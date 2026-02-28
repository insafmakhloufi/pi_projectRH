package Controllers.evenement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class OpenAIEventDescriptionGenerator {

  
    private static final URI RESPONSES_ENDPOINT = URI.create("https://api.openai.com/v1/responses");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIEventDescriptionGenerator() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String generateDescription(EventInfo info) throws IOException, InterruptedException {
        if (info == null) {
            throw new IllegalArgumentException("Missing event info");
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.contains("PUT_YOUR_KEY_HERE")) {
            throw new IllegalStateException("OpenAI API key is missing. Set OPENAI_API_KEY or environment variable OPENAI_API_KEY.");
        }

        String requestBody = buildRequestBody(info);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(RESPONSES_ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        System.out.println("[OpenAIEventDescriptionGenerator] HTTP status: " + status);

        if (status != 200) {
            String body = response.body();
            System.out.println("[OpenAIEventDescriptionGenerator] Response body: " + body);
            throw new IllegalArgumentException("OpenAI API error: HTTP " + status);
        }

        return extractDescription(response.body());
    }

    private String buildRequestBody(EventInfo info) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "gpt-4.1-mini");

        ArrayNode input = root.putArray("input");
        ObjectNode msg = input.addObject();
        msg.put("role", "user");

        String prompt = buildPrompt(info);

        ArrayNode content = msg.putArray("content");
        content.addObject()
                .put("type", "input_text")
                .put("text", prompt);

        ObjectNode text = root.putObject("text");
        ObjectNode format = text.putObject("format");
        format.put("type", "json_object");

        return objectMapper.writeValueAsString(root);
    }

    private String extractDescription(String responsesApiJson) throws IOException {
        JsonNode root = objectMapper.readTree(responsesApiJson);
        JsonNode output = root.path("output");
        if (!output.isArray()) {
            throw new IOException("OpenAI response missing output array");
        }

        for (JsonNode outItem : output) {
            JsonNode content = outItem.path("content");
            if (!content.isArray()) continue;

            for (JsonNode c : content) {
                if (!"output_text".equals(c.path("type").asText())) continue;

                String text = c.path("text").asText(null);
                if (text == null || text.isBlank()) continue;

                JsonNode obj = objectMapper.readTree(text);
                JsonNode descNode = obj.get("description");
                if (descNode != null && descNode.isTextual()) {
                    String desc = descNode.asText("").trim();
                    if (!desc.isBlank()) return desc;
                }
            }
        }

        throw new IOException("OpenAI response did not contain a description field");
    }

    private static String buildPrompt(EventInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate an event description in French. ");
        sb.append("Return ONLY JSON with key 'description'. ");
        sb.append("Format: {\"description\":\"...\"}.\n\n");

        if (info.title != null && !info.title.isBlank()) sb.append("Title: ").append(info.title).append("\n");
        if (info.type != null && !info.type.isBlank()) sb.append("Type: ").append(info.type).append("\n");
        if (info.location != null && !info.location.isBlank()) sb.append("Location: ").append(info.location).append("\n");
        if (info.startDate != null && !info.startDate.isBlank()) sb.append("Start date: ").append(info.startDate).append("\n");
        if (info.endDate != null && !info.endDate.isBlank()) sb.append("End date: ").append(info.endDate).append("\n");
        if (info.capacity != null && !info.capacity.isBlank()) sb.append("Capacity: ").append(info.capacity).append("\n");
        if (info.status != null && !info.status.isBlank()) sb.append("Status: ").append(info.status).append("\n");

        sb.append("\nWrite a clear, attractive description (2-6 sentences). ");
        sb.append("Do not add markdown, do not add extra keys.");

        return sb.toString();
    }

    private static String resolveApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return OPENAI_API_KEY;
    }

    public static final class EventInfo {
        public final String title;
        public final String type;
        public final String location;
        public final String startDate;
        public final String endDate;
        public final String capacity;
        public final String status;

        public EventInfo(String title, String type, String location, String startDate, String endDate, String capacity, String status) {
            this.title = title;
            this.type = type;
            this.location = location;
            this.startDate = startDate;
            this.endDate = endDate;
            this.capacity = capacity;
            this.status = status;
        }
    }
}
