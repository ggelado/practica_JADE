package agentes.percepcion;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class AgentePerceptor extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignora los mensajes enviados por otros bots 
        if (event.getAuthor().isBot()) {
            return;
        }

        // Extrae la información básica del mensaje recibido
        String autor = event.getAuthor().getAsTag();
        String canal = event.getChannel().getName();
        String contenido = event.getMessage().getContentRaw();

        // Imprime en consola quién habló, en qué canal y qué escribió
        System.out.println("[Perceptor] Mensaje recibido en #" + canal
                + " de " + autor + ": " + contenido);

        // Detecion de imagenes
        event.getMessage().getAttachments().forEach(attachment -> {
            if (attachment.isImage()) {
                String imageUrl = attachment.getUrl();
                System.out.println("[Perceptor] Imagen detectada en #" + canal
                        + " de " + autor + ": " + imageUrl);
            }
        });
    }
}