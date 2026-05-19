package agentes.vision;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
  private static final String PYTHON_EXECUTABLE = "python"; // podría ser python3, actualizar en consecuencia

  @Override
  protected void setup() {
    System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

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
    ProcessBuilder processBuilder = new ProcessBuilder(
        PYTHON_EXECUTABLE,
        PYTHON_SCRIPT.toString(),
        "--url",
        imageUrl);


    Process process = processBuilder.start();
    StringBuilder output = new StringBuilder();

    try (InputStream inputStream = process.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inputStreamReader)) {
      String line;
      while ((line = reader.readLine()) != null) {
        output.append(line);
      }
    }

    int exitCode = process.waitFor();
    String result = output.toString();
    if (exitCode != 0) {
      throw new IOException("No se pudo procesar la imagen");
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
}
