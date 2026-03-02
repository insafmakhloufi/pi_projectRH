package Controllers.evenement;

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

public final class OpenAIClient {

    private static final URI CHAT_COMPLETIONS_ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");

    private static final String DEV_FALLBACK_API_KEY = "";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String runAdminCommand(String userCommand) throws IOException, InterruptedException {
        if (userCommand == null || userCommand.isBlank()) {
            throw new IllegalArgumentException("Missing command");
        }

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing OPENAI_API_KEY environment variable");
        }

        String requestBody = buildChatCompletionsBody(userCommand.trim());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(CHAT_COMPLETIONS_ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        String body = response.body();

        if (status != 200) {
            throw new IOException("OpenAI API error HTTP " + status + (body != null && !body.isBlank() ? ": " + safeTrim(body) : ""));
        }

        return extractAssistantText(body);
    }

    public String planDbQuestion(String userQuestion, String schemaContext) throws IOException, InterruptedException {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("Missing question");
        }
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing OPENAI_API_KEY environment variable");
        }

        String sys = AiPlannerPrompt.plannerSystemPrompt();
        String user = (schemaContext != null ? schemaContext.trim() + "\n\n" : "")
                + "USER_QUESTION: " + userQuestion.trim();

        return chatCompletion(apiKey, sys, user, 500);
    }

    public String finalAnswerDbQuestion(String userQuestion, String planText, String executionResults) throws IOException, InterruptedException {
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("Missing question");
        }
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing OPENAI_API_KEY environment variable");
        }

        String sys = AiPlannerPrompt.finalAnswerSystemPrompt();
        String user = "USER_QUESTION:\n" + userQuestion.trim()
                + "\n\nPLAN:\n" + (planText != null ? planText.trim() : "")
                + "\n\nEXECUTION_RESULTS:\n" + (executionResults != null ? executionResults.trim() : "");

        return chatCompletion(apiKey, sys, user, 600);
    }

    private String buildChatCompletionsBody(String command) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "gpt-4o-mini");

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "You are an assistant for an Event Management admin dashboard.\n" +
                        "First line must start with ACTION:\n" +
                        "Possible actions:\n" +
                        "- delete_event id=<number>\n" +
                        "- open_event id=<number>\n" +
                        "- open_add_form\n" +
                        "- most_participants\n" +
                        "- count_status\n" +
                        "If unclear, use ACTION: ask_clarification\n\n" +
                        "Rules:\n" +
                        "- The first line must be exactly one ACTION line.\n" +
                        "- After the ACTION line, explain briefly what you did.\n" +
                        "- If the user requests deletion and no id is provided, ask for clarification.\n");

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", command);

        root.put("temperature", 0.2);
        root.put("max_tokens", 300);

        return objectMapper.writeValueAsString(root);
    }

    private String buildChatCompletionsBody(String systemPrompt, String userPrompt, int maxTokens) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "gpt-4o-mini");

        ArrayNode messages = root.putArray("messages");

        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);

        root.put("temperature", 0.2);
        root.put("max_tokens", Math.max(50, Math.min(maxTokens, 1200)));

        return objectMapper.writeValueAsString(root);
    }

    private String chatCompletion(String apiKey, String systemPrompt, String userPrompt, int maxTokens)
            throws IOException, InterruptedException {
        String requestBody = buildChatCompletionsBody(systemPrompt, userPrompt, maxTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(CHAT_COMPLETIONS_ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        int status = response.statusCode();
        String body = response.body();

        if (status != 200) {
            throw new IOException("OpenAI API error HTTP " + status + (body != null && !body.isBlank() ? ": " + safeTrim(body) : ""));
        }

        return extractAssistantText(body);
    }

    private String extractAssistantText(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IOException("Empty OpenAI response");
        }

        JsonNode root = objectMapper.readTree(json);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IOException("OpenAI response missing choices");
        }

        JsonNode message = choices.get(0).path("message");
        String content = message.path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new IOException("OpenAI response missing message.content");
        }
        return content.trim();
    }

    private static String resolveApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();

        String sys = System.getProperty("OPENAI_API_KEY");
        if (sys != null && !sys.isBlank()) return sys.trim();

        if (DEV_FALLBACK_API_KEY != null && !DEV_FALLBACK_API_KEY.isBlank()) {
            return DEV_FALLBACK_API_KEY.trim();
        }

        return null;
    }

    private static String safeTrim(String s) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() > 600 ? t.substring(0, 600) + "..." : t;
    }
}

