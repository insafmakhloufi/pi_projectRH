package Services.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GreenApiWhatsAppSender implements PasswordResetService.SmsSender {

    private final String apiUrl;
    private final String instanceId;
    private final String token;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public GreenApiWhatsAppSender() {
        this.apiUrl = getRequiredEnv("GREEN_API_URL");
        this.instanceId = getRequiredEnv("GREEN_API_INSTANCE_ID");
        this.token = getRequiredEnv("GREEN_API_TOKEN");
    }

    @Override
    public void send(String toPhoneE164, String textContent) {
        String digits = normalizeToDigits(toPhoneE164);
        String chatId = digits + "@c.us";
        String message = textContent == null ? "" : textContent;

        String endpoint = normalizeBaseUrl(apiUrl) + "/waInstance" + instanceId + "/sendMessage/" + token;

        String bodyJson = "{" +
                "\"chatId\":\"" + escapeJson(chatId) + "\"," +
                "\"message\":\"" + escapeJson(message) + "\"" +
                "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Green-API request failed", e);
        }

        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("Green-API error (to=" + digits + "): " + code + " - " + (response.body() == null ? "" : response.body()));
        }

        if (response.body() != null && !response.body().isBlank()) {
            System.out.println("Green-API success (to=" + digits + "): " + response.body());
        }
    }

    private static String normalizeBaseUrl(String url) {
        String v = url == null ? "" : url.trim();
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }

    private static String normalizeToDigits(String phone) {
        if (phone == null) {
            throw new IllegalArgumentException("Phone is required");
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.isBlank()) {
            throw new IllegalArgumentException("Invalid phone");
        }
        return digits;
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String getRequiredEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return v;
    }
}
