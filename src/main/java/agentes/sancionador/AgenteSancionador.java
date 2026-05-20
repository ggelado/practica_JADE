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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class AgenteSancionador extends Agent {
	
    private static final String TOKEN_ENV       = "token.env";
    private static final String DISCORD_TOKEN_KEY = "DISCORD_TOKEN";
    
    private transient JDA jda;

	@Override
	protected void setup() {
		System.out.println("[Agenet Sancionador] Arrancando:" + getAID().getName());
		//Obtener token e id del canal
		String token = loadValueFromEnv(DISCORD_TOKEN_KEY);
		
		if(token == null || token.isBlank()) {
			System.err.println("[Agente Sancionador] ERROR: Falta el token de Discord");
			return;
		}
		// Conectarse el agente a discord
		try {
			this.jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .build()
                    .awaitReady();
			System.out.println("[Agente Sancionador] Conectado a Discord");
		} catch(Exception e) {
			System.err.println("[Agente Sancionador] No se pudo conectar a Discord");
			e.printStackTrace();
			return;
		}
		
		// Comportamiento para eliminar el mensaje:
		addBehaviour(new CyclicBehaviour() {
			@Override
			public void action() {
				ACLMessage msg = receive();
				if(msg!=null) {
					try {
						DiscordMessage discordMsg = (DiscordMessage) msg.getContentObject();
						System.out.println("[Agente Sancionador] Mensaje obtenido" + discordMsg.getId()
						+ " | Detecciones: " + discordMsg.getDetecciones());
						//Eliminar el mensaje
						eliminarMensaje(discordMsg);
					} catch(UnreadableException e) {
						System.err.println("[Agente Sancionador] Error leyendo mensaje");
					}
				} else block();
			}
		});
	}
	
	
	// Función para leer token.env
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
   
    // Buscar el mensaje de discord por id y eliminarlo
    private void eliminarMensaje(DiscordMessage msg) {
    	if(jda == null) return;
    	String msgId = msg.getId();
    	String chId = msg.getChannelId();
    	
    	if(chId == null || chId.isBlank()) {
    		System.err.println("[Agente Sancionador] El mensaje no tiene channelId -> id: " + msgId);
            return;
    	}
    	
    	var channel = jda.getTextChannelById(chId);
        if (channel == null) {
            System.err.println("[Agente Sancionador] Canal no encontrado: " + chId);
            return;
        }
        
        // Se elimina el mensaje del canal
        channel.deleteMessageById(msgId).queue(
        		success -> System.out.println("[Agente Sancionador] Mensaje eliminado -> id: " + msgId),
        		error -> System.err.println("[Agente Sancionador] Error al eliminar mensaje -> id: " 
        					+ msgId + " | Causa: " + error.getMessage())
        );
    }
}
