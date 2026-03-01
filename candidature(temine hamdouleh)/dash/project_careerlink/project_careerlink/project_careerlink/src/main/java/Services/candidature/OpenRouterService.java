package Services.candidature;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class OpenRouterService {

    private static final String API_URL = "";

    private final HttpClient httpClient;
    private final Gson gson;

    public OpenRouterService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    public String chatCompletion(String systemPrompt, String userPrompt) throws Exception {
        OpenRouterConfig cfg = OpenRouterConfig.load();

        if (cfg.apiKey == null || cfg.apiKey.isBlank()) {
            throw new IllegalStateException("openrouter.api_key est vide. Renseigne src/main/resources/config/openrouter.properties");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", cfg.model);

        var messages = gson.toJsonTree(new Object[] {
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        }).getAsJsonArray();
        payload.add("messages", messages);

        JsonObject reasoning = new JsonObject();
        reasoning.addProperty("enabled", true);
        payload.add("reasoning", reasoning);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(180))
                .header("Authorization", "Bearer " + cfg.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("OpenRouter HTTP " + resp.statusCode() + "\n" + resp.body());
        }

        JsonObject root = gson.fromJson(resp.body(), JsonObject.class);
        String content = root
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        return content == null ? "" : content.trim();
    }

    private record Message(String role, String content) {}

    static class OpenRouterConfig {
        final String apiKey;
        final String model;

        OpenRouterConfig(String apiKey, String model) {
            this.apiKey = apiKey;
            this.model = (model == null || model.isBlank()) ? "openai/o3" : model;
        }

        static OpenRouterConfig load() {
            Properties p = new Properties();
            try (InputStream in = OpenRouterService.class.getResourceAsStream("/config/openrouter.properties")) {
                if (in != null) {
                    p.load(in);
                }
            } catch (Exception ignored) {}

            String apiKey = p.getProperty("openrouter.api_key", "").trim();
            String model = p.getProperty("openrouter.model", "openai/o3").trim();

            // override via JVM properties if you prefer
            String apiKeySys = System.getProperty("openrouter.api_key");
            if (apiKeySys != null && !apiKeySys.isBlank()) apiKey = apiKeySys.trim();
            String modelSys = System.getProperty("openrouter.model");
            if (modelSys != null && !modelSys.isBlank()) model = modelSys.trim();

            return new OpenRouterConfig(apiKey, model);
        }
    }
}
