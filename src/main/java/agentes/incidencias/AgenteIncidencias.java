package agentes.incidencias;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import model.DiscordMessage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class AgenteIncidencias extends Agent {

    private static final String TOKEN_ENV = "token.env";
    private static final String ADMIN_ID_KEY = "ADMIN_DISCORD_ID";
    private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";
    private static final String SAFE_IMAGE_URL_KEY = "SAFE_IMAGE_URL";
    private static final String DEFAULT_SAFE_IMAGE_URL = "https://images.openai.com/static-rsc-4/KHGu71UfH_dMNlhUal-896Wz58oxmodI2Ho_tcS5ZNy-Sz9Dr8KWUF1rB3eATUCb8RcTaMOtcKAZ7lR6-U_2Zw8oTCgajaJj4iIQ2z6mASBFylLLi7Z3XfgTXnIp2XvSRtKaJMbiQvbnasXApZ_kILMSBhVkexXMhI2EK2Z_vjvBL_qVafG7L3MxpDlYmbsD?purpose=fullsize";

    private transient JDA jda;
    private String adminId;
    private String safeImageUrl;

    @Override
    protected void setup() {
        System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

        String token = loadValueFromEnv(DISCORD_TOKEN_KEY);
        this.adminId = loadValueFromEnv(ADMIN_ID_KEY);
        this.safeImageUrl = firstNonBlank(loadValueFromEnv(SAFE_IMAGE_URL_KEY), DEFAULT_SAFE_IMAGE_URL);

        if (token == null || token.isBlank()) {
            System.err.println("[AgenteIncidencias] ERROR: Falta DISCORD_TOKEN en token.env");
            return;
        }

        if (adminId == null || adminId.isBlank()) {
            System.out.println("[AgenteIncidencias] AVISO: Falta ADMIN_DISCORD_ID en token.env; no se enviarán alertas privadas al admin.");
        }

        if (safeImageUrl == null || safeImageUrl.isBlank()) {
            System.err.println("[AgenteIncidencias] AVISO: Falta SAFE_IMAGE_URL en token.env. No se enviará imagen bonita.");
        }

        // Conectar el agente a la API de Discord
        try {
            this.jda = JDABuilder.createDefault(token).build().awaitReady();
            System.out.println("[AgenteIncidencias] Conectado a Discord con éxito.");
        } catch (Exception e) {
            System.err.println("[AgenteIncidencias] No se pudo conectar a la API de Discord: " + e.getMessage());
            return;
        }

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID()); 
        ServiceDescription sd = new ServiceDescription();
        sd.setType("gestion-incidencias"); 
        sd.setName("Servicio-Alertas-Seguridad");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            System.out.println("Agente registrado correctamente en el DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Filtro Bloqueante
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    try {
                        DiscordMessage discordMsg = (DiscordMessage) msg.getContentObject();
                        
                        System.out.println("\n[AgenteIncidencias] ¡Alerta grave detectada en el sistema!");
                        System.out.println("Contenido peligroso: " + discordMsg.getDetecciones());

                        enviarImagenBonitaAlCanal(discordMsg);
                        enviarMensajePrivadoAdmin(discordMsg);

                    } catch (UnreadableException e) {
                        System.err.println("Error al decodificar el objeto DiscordMessage: " + e.getMessage());
                    }
                } else {
                    block();
                }
            }
        });
    }

    /**
     * Buscar al adminy mandarle un mensaje
     */
    private void enviarMensajePrivadoAdmin(DiscordMessage info) {
        if (jda == null || adminId == null) return;

        // Buscamos al usuario
        String textoAlerta = "🚨 **[ALERTA DE SEGURIDAD]** 🚨\n"
                + "Se ha detectado una infracción en el servidor.\n"
                + "**ID del Mensaje:** " + info.getId() + "\n"
                + "**Tipos de contenido detectados:** " + info.getDetecciones() + "\n"
                + "Por favor, revisa el canal de moderación.";

        // Intentar como canal (ID de canal)
        TextChannel channel = jda.getTextChannelById(adminId);
        if (channel != null) {
            channel.sendMessage(textoAlerta).queue(
                success -> System.out.println("[AgenteIncidencias] Mensaje de alerta enviado al canal (ID " + adminId + ") con éxito."),
                error -> System.err.println("[AgenteIncidencias] Error al enviar el mensaje al canal: " + error.getMessage())
            );
            return;
        }

        // Si no es canal, intentar como usuario (DM)
        jda.retrieveUserById(adminId).queue(
            user -> {
                user.openPrivateChannel().queue(
                    privateChannel -> {
                        privateChannel.sendMessage(textoAlerta).queue(
                            success -> System.out.println("[AgenteIncidencias] Mensaje de alerta enviado al administrador por Discord con éxito."),
                            error -> System.err.println("[AgenteIncidencias] Error al enviar el DM: " + error.getMessage())
                        );
                    },
                    error -> System.err.println("[AgenteIncidencias] Error abriendo canal privado: " + error.getMessage())
                );
            },
            error -> System.err.println("[AgenteIncidencias] No se pudo encontrar al usuario administrador ni al canal con el ID proporcionado.")
        );
    }

    private void enviarImagenBonitaAlCanal(DiscordMessage info) {
        if (jda == null || info == null || info.getChannelId() == null || info.getChannelId().isBlank()) {
            return;
        }

        if (safeImageUrl == null || safeImageUrl.isBlank()) {
            System.err.println("[AgenteIncidencias] SAFE_IMAGE_URL no está configurada; se omite la respuesta visual.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(info.getChannelId());
        if (channel == null) {
            System.err.println("[AgenteIncidencias] No se encontró el canal original para enviar la imagen bonita: " + info.getChannelId());
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Contenido retirado")
                .setDescription("Se detectó contenido no permitido. Aquí tienes algo bonito mientras tanto.")
                .setImage(safeImageUrl);

        channel.sendMessageEmbeds(embed.build()).queue(
                success -> System.out.println("[AgenteIncidencias] Imagen bonita enviada al canal original (" + info.getChannelId() + ")."),
                error -> System.err.println("[AgenteIncidencias] Error enviando la imagen bonita al canal: " + error.getMessage())
        );
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }

    /**
     * Función para leer token.env
     */
    private String loadValueFromEnv(String key) {
        File env = new File(System.getProperty("user.dir"), TOKEN_ENV);
        if (!env.exists()) return null;

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

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            if (jda != null) {
                jda.shutdown();
            }
            System.out.println("Agente desregistrado del DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}