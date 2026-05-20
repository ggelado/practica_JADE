package agentes.incidencias;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import model.DiscordMessage;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class AgenteIncidencias extends Agent {

    private static final String TOKEN_ENV = "token.env";
    private static final String ADMIN_ID_KEY = "ADMIN_DISCORD_ID";
    private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";

    private transient JDA jda;
    private String adminId;

    @Override
    protected void setup() {
        System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

        String token = loadValueFromEnv(DISCORD_TOKEN_KEY);
        this.adminId = loadValueFromEnv(ADMIN_ID_KEY);

        if (token == null || token.isBlank() || adminId == null || adminId.isBlank()) {
            System.err.println("[AgenteIncidencias] ERROR: Falta DISCORD_TOKEN o ADMIN_DISCORD_ID en token.env");
            return;
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
        jda.retrieveUserById(adminId).queue(
            user -> {
                user.openPrivateChannel().queue(
                    channel -> {
                        String textoAlerta = "🚨 **[ALERTA DE SEGURIDAD]** 🚨\n"
                                + "Se ha detectado una infracción en el servidor.\n"
                                + "**ID del Mensaje:** " + info.getId() + "\n"
                                + "**Tipos de contenido detectados:** " + info.getDetecciones() + "\n"
                                + "Por favor, revisa el canal de moderación.";
                        
                        // Enviamos el mensaje a Ds
                        channel.sendMessage(textoAlerta).queue(
                            success -> System.out.println("[AgenteIncidencias] Mensaje de alerta enviado al administrador por Discord con éxito."),
                            error -> System.err.println("[AgenteIncidencias] Error al enviar el DM: " + error.getMessage())
                        );
                    }
                );
            },
            error -> System.err.println("[AgenteIncidencias] No se pudo encontrar al usuario administrador con el ID proporcionado.")
        );
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