package agentes.incidencias;

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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class AgenteIncidencias extends Agent {

    private static final String TOKEN_ENV = "token.env";
    private static final String ADMIN_ID_KEY = "ADMIN_DISCORD_ID";
    private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";

    private transient JDA jda;
    private String adminId;

    @Override
    protected void setup() {
        System.out.println("Hola! El agente [" + getAID().getName() + "] ya está despertando.");

        // Verificar si el archivo token.env existe
        File envFile = new File(System.getProperty("user.dir"), TOKEN_ENV);
        if (!envFile.exists() || !envFile.isFile()) {
            System.err.println("[AgenteIncidencias] ERROR: El archivo token.env no existe o no es válido.");
            doDelete();
            return;
        }

        // Cargar valores del archivo token.env
        String token = loadTokenFromFile(TOKEN_ENV);
        this.adminId = loadTokenFromFile(TOKEN_ENV);

        if (token == null || token.isBlank() || adminId == null || adminId.isBlank()) {
            System.err.println("[AgenteIncidencias] ERROR: Falta DISCORD_TOKEN o ADMIN_DISCORD_ID en token.env");
            doDelete();
            return;
        }

        // Conectar el agente a la API de Discord
        try {
            this.jda = JDABuilder.createDefault(token).build().awaitReady();
            System.out.println("[AgenteIncidencias] Conectado a Discord con éxito.");
        } catch (Exception e) {
            System.err.println("[AgenteIncidencias] No se pudo conectar a la API de Discord: " + e.getMessage());
            doDelete();
            return;
        }

        // Registrar el servicio en el DF
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
            doDelete();
            return;
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

    @Override
    protected void takeDown() {
        try {
            if (DFService.search(this, new DFAgentDescription()).length > 0) {
                DFService.deregister(this);
                System.out.println("Agente desregistrado del DF.");
            } else {
                System.out.println("[AgenteIncidencias] No estaba registrado en el DF.");
            }
            if (jda != null) {
                jda.shutdown();
            }
        } catch (FIPAException fe) {
            System.err.println("[AgenteIncidencias] Error al intentar desregistrar: " + fe.getMessage());
        }
    }
}