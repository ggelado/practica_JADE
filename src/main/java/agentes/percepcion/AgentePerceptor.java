package agentes.percepcion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import model.DiscordMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class AgentePerceptor extends Agent {

    private static final String TOKEN_ENV = "token.env";
    private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";
    private static final String VISION_SERVICE_TYPE = "vision-safety";

    private transient JDA jda;

    @Override
    protected void setup() {
        System.out.println("Agente Perceptor JADE arrancando: " + getAID().getName());

        String token = loadTokenFromFile(TOKEN_ENV);
        if (token == null || token.isBlank()) {
            System.err.println("No se encontró token de Discord. JDA no se iniciará.");
            return;
        }

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new ListenerAdapter() {
                        @Override
                        public void onMessageReceived(MessageReceivedEvent event) {
                            // Reutilizamos la lógica del perceptor existente
                            if (event.getAuthor().isBot()) {
                                return;
                            }

                            String autor = event.getAuthor().getAsTag();
                            String canal = event.getChannel().getName();
                            String contenido = null;
                            try {
                                contenido = event.getMessage().getContentRaw();
                            } catch (Exception ignore) {
                            }

                            System.out.println("[Perceptor] Mensaje recibido en #" + canal
                                    + " de " + autor + ": " + (contenido == null ? "" : contenido));

                            event.getMessage().getAttachments().forEach(attachment -> {
                                if (attachment.isImage()) {
                                    String imageUrl = attachment.getUrl();
                                    System.out.println("[Perceptor] Imagen detectada en #" + canal
                                            + " de " + autor + ": " + imageUrl);
                                    try {
                                        sendImageToVisionAgent(imageUrl, event.getMessageId(), event.getChannel().getId());
                                    } catch (Exception e) {
                                        System.err.println("Error enviando imagen a AgenteVisualizador: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    })
                    .build();
        } catch (Exception e) {
            System.err.println("Error iniciando JDA en AgentePerceptor: " + e.getMessage());
        }
    }

    @Override
    protected void takeDown() {
        if (jda != null) {
            jda.shutdown();
        }
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            // Ignorar
        }
        System.out.println("AgentePerceptor detenido.");
    }

    private String loadTokenFromFile(String filename) {
        File env = new File(filename);
        if (!env.exists()) {
            File alt = new File(getProjectRoot(), filename);
            if (alt.exists()) {
                env = alt;
            } else {
                return null;
            }
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(env))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(DISCORD_TOKEN_KEY + "=")) {
                    return line.substring((DISCORD_TOKEN_KEY + "=").length()).trim();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private File getProjectRoot() {
        return new File(System.getProperty("user.dir"));
    }

    private void sendImageToVisionAgent(String imageUrl, String messageId, String channelId) throws IOException, FIPAException {
        DiscordMessage discordMessage = new DiscordMessage(imageUrl, messageId, channelId);

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType(VISION_SERVICE_TYPE);
        template.addServices(sd);

        DFAgentDescription[] agents = DFService.search(this, template);
        if (agents == null || agents.length == 0) {
            throw new RuntimeException("No se encontró AgenteVisualizador con el servicio '" + VISION_SERVICE_TYPE + "'.");
        }

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(agents[0].getName());
        msg.setContentObject(discordMessage);
        send(msg);
        System.out.println("[Perceptor] Enviado mensaje a AgenteVisualizador: " + imageUrl);
    }

}