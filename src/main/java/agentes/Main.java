package agentes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) throws Exception {
        Map<String, String> config = loadConfig(Path.of("token.env")); // Para enmascarar token bot
        String token = firstNonBlank(
                config.get("DISCORD_TOKEN"),
                System.getenv("DISCORD_TOKEN"));

        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Define DISCORD_TOKEN en token.env");
        }

        // Crea la conexión con Discord, habilita el acceso al contenido de mensajes
        // y registra el listener que recibe los eventos del servidor
        JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new AgentePerceptor())
                .build()
                .awaitReady();

        // Se ejecuta cuando el bot ya está conectado y listo para escuchar mensajes.
        System.out.println("Bot iniciado y escuchando mensajes");
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

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }

        if (second != null && !second.isBlank()) {
            return second;
        }

        return null;
    }
}
