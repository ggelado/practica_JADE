package agentes.sancionador;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import model.DiscordMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class AgenteSancionador extends Agent {

  private static final String SERVICE_TYPE = "sancionador";
  private static final String SERVICE_NAME = "Servicio-Sancionador";

  private static final String TOKEN_ENV = "token.env";
  private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";

  private transient JDA jda;
  private String safeImageUrl;

  @Override
  protected void setup() {
    System.out.println("[Agenet Sancionador] Arrancando:" + getAID().getName());
    // Obtener token e id del canal
    String token = loadValueFromEnv(DISCORD_TOKEN_KEY);
    this.safeImageUrl = loadValueFromEnv("SAFE_IMAGE_URL");
    if (this.safeImageUrl == null || this.safeImageUrl.isBlank()) {
      this.safeImageUrl = "https://images.openai.com/static-rsc-4/KHGu71UfH_dMNlhUal-896Wz58oxmodI2Ho_tcS5ZNy-Sz9Dr8KWUF1rB3eATUCb8RcTaMOtcKAZ7lR6-U_2Zw8oTCgajaJj4iIQ2z6mASBFylLLi7Z3XfgTXnIp2XvSRtKaJMbiQvbnasXApZ_kILMSBhVkexXMhI2EK2Z_vjvBL_qVafG7L3MxpDlYmbsD?purpose=fullsize";
    }

    if (token == null || token.isBlank()) {
      System.err.println("[Agente Sancionador] ERROR: Falta el token de Discord");
      return;
    }
    // Conectarse el agente a discord
    try {
      this.jda = JDABuilder.createDefault(token)
          .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT).build().awaitReady();
      System.out.println("[Agente Sancionador] Conectado a Discord");
    } catch (Exception e) {
      System.err.println("[Agente Sancionador] No se pudo conectar a Discord");
      e.printStackTrace();
      return;
    }

    registerService();

    // Comportamiento para eliminar el mensaje:
    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        ACLMessage msg = receive();
        if (msg != null) {
          try {
            DiscordMessage discordMsg = (DiscordMessage) msg.getContentObject();
            System.out.println("[Agente Sancionador] Mensaje obtenido" + discordMsg.getId() + " | Detecciones: "
                + discordMsg.getDetecciones());
            // Eliminar el mensaje y publicar imagen bonita
            eliminarMensaje(discordMsg);
          } catch (UnreadableException e) {
            System.err.println("[Agente Sancionador] Error leyendo mensaje");
          }
        } else
          block();
      }
    });
  }

  // Función para leer token.env
  private String loadValueFromEnv(String key) {
    File env = new File(System.getProperty("user.dir"), TOKEN_ENV);
    if (!env.exists())
      return null;

    try (BufferedReader reader = new BufferedReader(new FileReader(env))) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith(key + "=")) {
          return line.substring((key + "=").length()).trim();
        }
      }
    } catch (IOException e) {
      return null;
    }
    return null;
  }

  // Buscar el mensaje de discord por id y eliminarlo
  private void eliminarMensaje(DiscordMessage msg) {
    if (jda == null)
      return;
    String msgId = msg.getId();
    String chId = msg.getChannelId();

    if (chId == null || chId.isBlank()) {
      System.err.println("[Agente Sancionador] El mensaje no tiene channelId -> id: " + msgId);
      return;
    }

    var channel = jda.getTextChannelById(chId);
    if (channel == null) {
      System.err.println("[Agente Sancionador] Canal no encontrado: " + chId);
      return;
    }

    // Se elimina el mensaje del canal y luego publicamos imagen bonita
    channel.deleteMessageById(msgId).queue(success -> {
      System.out.println("[Agente Sancionador] Mensaje eliminado -> id: " + msgId);
      enviarImagenBonita(channel, chId);
    }, error -> {
      System.err.println(
          "[Agente Sancionador] Error al eliminar mensaje -> id: " + msgId + " | Causa: " + error.getMessage());
      // Intentar igualmente publicar la imagen aunque la eliminación falle
      enviarImagenBonita(channel, chId);
    });
  }

  private void enviarImagenBonita(TextChannel channel, String chId) {
    if (channel == null)
      return;
    if (safeImageUrl == null || safeImageUrl.isBlank()) {
      System.err.println("[Agente Sancionador] SAFE_IMAGE_URL no configurada; se omite imagen bonita.");
      return;
    }
    try {
      EmbedBuilder embed = new EmbedBuilder().setTitle("Contenido retirado")
          .setDescription("Se ha detectado contenido no permitido. Aquí tienes algo bonito mientras tanto.")
          .setImage(safeImageUrl);
      channel.sendMessageEmbeds(embed.build()).queue(
          s -> System.out.println("[Agente Sancionador] Imagen bonita enviada al canal " + chId),
          e -> System.err.println("[Agente Sancionador] Error enviando imagen: " + e.getMessage()));
    } catch (Exception e) {
      System.err.println("[Agente Sancionador] Error construyendo/enviando embed: " + e.getMessage());
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
      System.out.println("[AgenteSancionador] Registrado en el DF como '" + SERVICE_TYPE + "'.");
    } catch (FIPAException e) {
      throw new IllegalStateException("No se pudo registrar el AgenteSancionador", e);
    }
  }

  @Override
  protected void takeDown() {
    try {
      DFService.deregister(this);
      if (jda != null)
        jda.shutdown();
      System.out.println("[AgenteSancionador] Desregistrado del DF.");
    } catch (FIPAException e) {
      e.printStackTrace();
    }
  }
}
