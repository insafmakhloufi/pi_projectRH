package Utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class AiConfig {

    private AiConfig() {
    }

    public static String getApiKey() {
        String fromEnv = trimToNull(System.getenv("OPENAI_API_KEY"));
        if (fromEnv != null) {
            return fromEnv;
        }

        Properties properties = loadLocalConfig();
        return trimToNull(properties.getProperty("OPENAI_API_KEY"));
    }

    public static String getModel() {
        String fromEnv = trimToNull(System.getenv("OPENAI_MODEL"));
        if (fromEnv != null) {
            return fromEnv;
        }

        Properties properties = loadLocalConfig();
        String fromFile = trimToNull(properties.getProperty("OPENAI_MODEL"));
        return fromFile == null ? "gpt-4o-mini" : fromFile;
    }

    public static String buildMissingApiKeyMessage() {
        return "OPENAI_API_KEY manquante. Configurez la variable d'environnement ou config.properties.";
    }

    private static Properties loadLocalConfig() {
        Properties properties = new Properties();
        Path configPath = Path.of("config.properties");
        if (!Files.exists(configPath)) {
            return properties;
        }

        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
        } catch (IOException ignored) {
            // Optional local config file.
        }
        return properties;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
