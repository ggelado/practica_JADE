package agentes.analista;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import model.DiscordMessage;

public class AgenteAnalista extends Agent {

  private static final String SERVICE_TYPE = "analista-texto";
  private static final String SERVICE_NAME = "Servicio-Analista-Texto";
  private static final String GEMINI_KEY_VAR = "GEMINI_API_KEY";
  private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=";
  private String geminiApiKey;

  @Override
  protected void setup() {
    System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

    Map<String, String> config = loadConfig(Path.of("token.env"));
    this.geminiApiKey = config.getOrDefault(GEMINI_KEY_VAR, "");

    if (this.geminiApiKey.isBlank()) {
      System.out.println("[AgenteAnalista] GEMINI_API_KEY no encontrada en token.env; las llamadas a la API fallarán.");
    }

    registerService();

    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        ACLMessage msg = receive();
        if (msg == null) {
          block();
          return;
        }

        try {
          DiscordMessage discordMessage = resolveDiscordMessage(msg);

          String textoResumido = discordMessage.getMensaje().length() > 80
              ? discordMessage.getMensaje().substring(0, 80) + "..."
              : discordMessage.getMensaje();

          System.out.println(
              "[AgenteAnalista] Mensaje recibido -> id: " + discordMessage.getId() + " | texto: " + textoResumido);

          String rawLabels = analyzeText(discordMessage.getMensaje());

          System.out.println("[AgenteAnalista] Gemini devolvió: " + rawLabels);

          List<DiscordMessage.Detecciones> detecciones = extractDetections(rawLabels);
          for (DiscordMessage.Detecciones deteccion : detecciones) {
            discordMessage.agregarDetecciones(deteccion);
          }

          sendToClassifier(discordMessage);

        } catch (Exception e) {
          System.err.println("[AgenteAnalista] Error procesando mensaje: " + e.getMessage());
        }
      }
    });
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
      System.out.println("[AgenteAnalista] Agente desregistrado del DF. Adiós!");
    } catch (FIPAException e) {
      e.printStackTrace();
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
      System.out.println("[AgenteAnalista] Registrado en el DF como '" + SERVICE_TYPE + "'.");
    } catch (FIPAException e) {
      throw new IllegalStateException("No se pudo registrar el AgenteAnalista", e);
    }
  }

  private DiscordMessage resolveDiscordMessage(ACLMessage msg) throws UnreadableException {
    if (msg.getContentObject() instanceof DiscordMessage discordMessage) {
      return discordMessage;
    }
    throw new IllegalArgumentException("El mensaje recibido no es un DiscordMessage válido");
  }

  private String analyzeText(String text) throws IOException, InterruptedException {
    String prompt = "Classify the following Discord message and reply ONLY with a comma-separated "
        + "list of applicable labels. Choose exclusively from: "
        + "toxic, spam, scam, discrimination, depression, anxiety, self_harm, loneliness, "
        + "help, positive, greeting, question, link, mention. " + "If none apply, reply with the single word: safe. "
        + "Do NOT explain. Do NOT add punctuation other than commas. " + "Message: \"" + escapeJson(text) + "\"";

    String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]}";

    // Configuración de la cola de espera
    int maxRetries = 3;
    int tiempoEsperaMs = 15000; // 15 segundos de espera para que se recargue la cuota por minuto de Gemini

    for (int intento = 1; intento <= maxRetries; intento++) {
      try {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(GEMINI_URL + geminiApiKey))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // MANEJO DE LÍMITE (COLA DE ESPERA)
        if (response.statusCode() == 429) {
          System.err.println("[AgenteAnalista] Error API Saturada (429). Intento " + intento + "/" + maxRetries
              + ". Esperando " + (tiempoEsperaMs / 1000) + " segundos...");

          // Pausamos el agente. Los nuevos mensajes se irán acumulando en el buzón
          // interno de JADE
          Thread.sleep(tiempoEsperaMs);
          continue; // Volvemos a lanzar el bucle para intentar llamar a la API de nuevo
        }

        else if (response.statusCode() != 200) {
          System.err.println("[AgenteAnalista] Error de conexión con la API (" + response.statusCode()
              + "). Dejando pasar el mensaje como 'safe'.");
          return "safe";
        }

        // Si todo va bien (HTTP 200), extraemos la etiqueta navegando por el JSON con
        // Gson
        try {
          JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
          String extractedText = jsonResponse.getAsJsonArray("candidates").get(0).getAsJsonObject()
              .getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString();

          return extractedText.trim();
        } catch (Exception e) {
          System.err.println("[AgenteAnalista] Error navegando el JSON con Gson. Dejando pasar como 'safe'.");
          return "safe";
        }

      } catch (Exception e) {
        // Si no hay internet o falla la librería HttpClient
        System.err.println("[AgenteAnalista] Excepción llamando a Gemini: " + e.getMessage()
            + ". Dejando pasar el mensaje como 'safe'.");
        return "safe";
      }
    }

    // Si después de los 3 intentos sigue dando error 429, dejamos pasar el mensaje
    System.err
        .println("[AgenteAnalista] Se agotaron los reintentos tras la espera. Dejando pasar el mensaje como 'safe'.");
    return "safe";

  }

  private static String escapeJson(String text) {
    return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
        "\\t");
  }

  private static List<DiscordMessage.Detecciones> extractDetections(String rawOutput) {
    if (rawOutput == null) {
      return List.of();
    }
    String rawCategories = rawOutput.trim();
    if (rawCategories.isEmpty()) {
      return List.of();
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

  private void sendToClassifier(DiscordMessage discordMessage) throws IOException, FIPAException {
    System.out.println("[AgenteAnalista] Contenido a enviar -> id: " + discordMessage.getId() + " | detecciones: "
        + discordMessage.getDetecciones());

    DFAgentDescription template = new DFAgentDescription();
    ServiceDescription service = new ServiceDescription();
    service.setType("clasificador");
    template.addServices(service);

    DFAgentDescription[] agents = DFService.search(this, template);
    if (agents.length == 0) {
      throw new RuntimeException("No se encontró ningún clasificador.");
    }

    System.out.println("[AgenteAnalista] Preparando envío al clasificador: receiver=" + agents[0].getName());

    ACLMessage message = new ACLMessage(ACLMessage.INFORM);
    message.addReceiver(agents[0].getName());
    message.setContentObject(discordMessage);
    send(message);

    System.out.println("[AgenteAnalista] Envío completado al clasificador (id: " + discordMessage.getId() + ")");
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
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo leer token.env", e);
    }

    return values;
  }
}
