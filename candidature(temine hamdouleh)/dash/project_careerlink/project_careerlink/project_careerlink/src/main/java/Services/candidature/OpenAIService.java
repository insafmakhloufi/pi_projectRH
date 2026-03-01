package Services.candidature;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class OpenAIService {

    private static final String API_URL = "";

    private final HttpClient httpClient;
    private final Gson gson;

    public OpenAIService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
    }

    public String chatCompletion(String systemPrompt, String userPrompt) throws Exception {
        OpenAIConfig cfg = OpenAIConfig.load();

        if (cfg.apiKey == null || cfg.apiKey.isBlank()) {
            throw new IllegalStateException("");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", cfg.model);

        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        payload.add("messages", messages);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(180))
                .header("Authorization", "Bearer " + cfg.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException("OpenAI HTTP " + resp.statusCode() + "\n" + resp.body());
        }

        JsonObject root = gson.fromJson(resp.body(), JsonObject.class);
        String content = root
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        return content == null ? "" : content.trim();
    }

    private JsonObject message(String role, String content) {
        JsonObject m = new JsonObject();
        m.addProperty("role", role);
        m.addProperty("content", content);
        return m;
    }

    static class OpenAIConfig {
        final String apiKey;
        final String model;

        OpenAIConfig(String apiKey, String model) {
            this.apiKey = apiKey;
            this.model = (model == null || model.isBlank()) ? "gpt-4o-mini" : model;
        }

        static OpenAIConfig load() {
            Properties p = new Properties();
            try (InputStream in = OpenAIService.class.getResourceAsStream("/config/openai.properties")) {
                if (in != null) {
                    p.load(in);
                }
            } catch (Exception ignored) {}

            String apiKey = p.getProperty("openai.api_key", "").trim();
            String model = p.getProperty("openai.model", "gpt-4o-mini").trim();

            String apiKeySys = System.getProperty("");
            if (apiKeySys != null && !apiKeySys.isBlank()) apiKey = apiKeySys.trim();
            String modelSys = System.getProperty("openai.model");
            if (modelSys != null && !modelSys.isBlank()) model = modelSys.trim();

            return new OpenAIConfig(apiKey, model);
        }
    }
}
