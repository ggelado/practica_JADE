package agentes.vision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import model.DiscordMessage;

public class AgenteVisualizador extends Agent {

  private static final String SERVICE_TYPE = "vision-safety";
  private static final String SERVICE_NAME = "Servicio-Vision-Seguridad";
  private static final Path PYTHON_SCRIPT = Path.of("visionModel", "predict_image.py");
  private static final String PYTHON_ENV_VAR = "VISION_PYTHON_PATH";

  // Valor resuelto en setup()
  private String pythonExecutable;

  @Override
  protected void setup() {
    System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

    Map<String, String> config = loadConfig(Path.of("token.env"));
    String py = config.get(PYTHON_ENV_VAR);
    if (py == null || py.isBlank()) {
      throw new IllegalStateException("Define VISION_PYTHON_PATH en token.env");
    }
    this.pythonExecutable = py;

    registerService(); // Registrar el servicio y darlo de alta

    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        // Espera mensajes, si no recibe ninguno se bloquea
        ACLMessage msg = receive();
        if (msg == null) {
          block(); // A la espera hasta recibir un mensaje.
          return; // JADE en el momento que recibe un mensaje desbloquea el agente, retorna y
                  // vuelve al bucle
        }

        try {
          DiscordMessage discordMessage = resolveDiscordMessage(msg);

          System.out.println("[AgenteVisualizador] Mensaje recibido -> id: " + discordMessage.getId()
              + " | mensaje(url): " + discordMessage.getMensaje()
              + " | detecciones-previas: " + discordMessage.getDetecciones());

          String result = analyzeWithVisionModel(discordMessage.getMensaje());
          List<DiscordMessage.Detecciones> detecciones = extractDetections(result);
          for (DiscordMessage.Detecciones deteccion : detecciones) {
            discordMessage.agregarDetecciones(deteccion);
          }
          sendToClassifier(discordMessage);


        } catch (Exception exception) {
          System.err.println("Error procesando mensaje: " + exception.getMessage());
        }
      }
    });
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
      System.out.println("Agente desregistrado del DF. Adiós!");
    } catch (FIPAException exception) {
      exception.printStackTrace();
    }
  }

  private void registerService() {
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());

    ServiceDescription sd = new ServiceDescription();
    sd.setType(SERVICE_TYPE);
    sd.setName(SERVICE_NAME);
    dfd.addServices(sd);

    try {
      DFService.register(this, dfd);
      System.out.println("Agente registrado correctamente en el DF como '" + SERVICE_TYPE + "'.");
    } catch (FIPAException exception) {
      throw new IllegalStateException("No se pudo registrar el agente de visión", exception);
    }
  }


  private DiscordMessage resolveDiscordMessage(ACLMessage msg) throws UnreadableException {
    if (msg.getContentObject() instanceof DiscordMessage discordMessage) {
      return discordMessage;
    }
    throw new IllegalArgumentException("El mensaje recibido no es válido");
  }

  private String analyzeWithVisionModel(String imageUrl) throws IOException, InterruptedException {

    Path scriptPath = PYTHON_SCRIPT;
    if (!scriptPath.isAbsolute()) { // Hacer que la ruta al script sea absoluta
      scriptPath = Path.of(System.getProperty("user.dir")).resolve(scriptPath);
    }

    ProcessBuilder processBuilder = new ProcessBuilder(
      this.pythonExecutable,
      scriptPath.toString(),
      "--url",
      imageUrl);


    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    StringBuilder output = new StringBuilder();

    try (InputStream inputStream = process.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inputStreamReader)) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line).append('\n');
      }
    }

    int exitCode = process.waitFor();
    String fullOutput = output.toString();
    String result = fullOutput.trim();

    // La última línea de la salida contiene los resultados
    String lastLine = null;
    String[] lines = fullOutput.split("\r?\n");
    for (int i = lines.length - 1; i >= 0; i--) {
      String l = lines[i].trim();
      if (!l.isEmpty()) {
        lastLine = l;
        break;
      }
    }
    if (lastLine != null) {
      result = lastLine;
    }
    if (exitCode != 0) {
      String msg = "No se pudo procesar la imagen (exitCode=" + exitCode + ") - salida: " + result;
      System.err.println("[AgenteVisualizador] " + msg);
      throw new IOException(msg);
    }

    return result;
  }

  private static List<DiscordMessage.Detecciones> extractDetections(String rawOutput) {
    if (rawOutput == null) {
      return List.of(); // Lista vacía
    }
    String rawCategories = rawOutput.trim(); // Sin espacios
    if (rawCategories.isEmpty()) {
      return List.of(); // Lista vacía
    }

    List<DiscordMessage.Detecciones> categories = new ArrayList<>();
    for (String token : rawCategories.split(",")) {
      String category = token.trim();
      DiscordMessage.Detecciones deteccion = DiscordMessage.Detecciones.fromLabel(category);
      if (deteccion != null) {
        categories.add(deteccion);
      }
    }

    return categories;
  }


  private static Map<String, String> loadConfig(Path file) {
    Map<String, String> values = new HashMap<>();

    if (!Files.exists(file)) {
      return values;
    }

    try {
      List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

      for (String line : lines) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
          continue;
        }

        String[] parts = trimmed.split("=", 2);
        values.put(parts[0].trim(), parts[1].trim());
      }
    } catch (IOException exception) {
      throw new IllegalStateException("No se pudo leer token.env", exception);
    }

    return values;
  }

  private void sendToClassifier(DiscordMessage discordMessage) throws IOException, FIPAException {
    
    System.out.println("[AgenteVisualizador] Contenido a enviar -> id: " + discordMessage.getId()
        + " | url: " + discordMessage.getMensaje()
        + " | detecciones: " + discordMessage.getDetecciones());

    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription service = new ServiceDescription();
    service.setType("clasificador");
    template.addServices(service);

    DFAgentDescription[] agents = DFService.search(this, template);
    if (agents.length == 0) {
      throw new RuntimeException("No se encontró ningún clasificador.");
    }

    // Log del contenido que se enviará
    System.out.println("[AgenteVisualizador] Preparando envío al clasificador: receiver=" + agents[0].getName());


    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    message.addReceiver(agents[0].getName());
    message.setContentObject(discordMessage);
    send(message);

    System.out.println("[AgenteVisualizador] Envío completado al clasificador (id: " + discordMessage.getId() + ")");
  }

}
