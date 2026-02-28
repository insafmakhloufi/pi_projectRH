package Services.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class WhatsAppCloudSender implements PasswordResetService.SmsSender {

    private final String accessToken;
    private final String phoneNumberId;
    private final String templateName;
    private final String templateLanguage;
    private final String templateParamMode;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public WhatsAppCloudSender() {
        this.accessToken = getRequiredEnv("WHATSAPP_ACCESS_TOKEN");
        this.phoneNumberId = getRequiredEnv("WHATSAPP_PHONE_NUMBER_ID");
        this.templateName = System.getenv("WHATSAPP_TEMPLATE_NAME");
        this.templateLanguage = System.getenv().getOrDefault("WHATSAPP_TEMPLATE_LANGUAGE", "en_US");
        this.templateParamMode = System.getenv().getOrDefault("WHATSAPP_TEMPLATE_PARAM_MODE", "body_text");
    }

    @Override
    public void send(String toPhoneE164, String textContent) {
        String to = normalizeToWhatsAppTo(toPhoneE164);
        String message = textContent == null ? "" : textContent;

        String bodyJson;
        if (templateName != null && !templateName.isBlank()) {
            String mode = templateParamMode == null ? "body_text" : templateParamMode.trim().toLowerCase();
            if (mode.equals("none")) {
                bodyJson = "{"
                        + "\"messaging_product\":\"whatsapp\","
                        + "\"to\":\"" + escapeJson(to) + "\","
                        + "\"type\":\"template\","
                        + "\"template\":{"
                        + "\"name\":\"" + escapeJson(templateName.trim()) + "\","
                        + "\"language\":{\"code\":\"" + escapeJson(templateLanguage) + "\"}"
                        + "}"
                        + "}";
            } else {
                bodyJson = "{"
                        + "\"messaging_product\":\"whatsapp\","
                        + "\"to\":\"" + escapeJson(to) + "\","
                        + "\"type\":\"template\","
                        + "\"template\":{"
                        + "\"name\":\"" + escapeJson(templateName.trim()) + "\","
                        + "\"language\":{\"code\":\"" + escapeJson(templateLanguage) + "\"},"
                        + "\"components\":[{"
                        + "\"type\":\"body\","
                        + "\"parameters\":[{\"type\":\"text\",\"text\":\"" + escapeJson(message) + "\"}]"
                        + "}]"
                        + "}"
                        + "}";
            }
        } else {
            bodyJson = "{"
                    + "\"messaging_product\":\"whatsapp\","
                    + "\"to\":\"" + escapeJson(to) + "\","
                    + "\"type\":\"text\","
                    + "\"text\":{\"preview_url\":false,\"body\":\"" + escapeJson(message) + "\"}"
                    + "}";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("WhatsApp Cloud API request failed", e);
        }

        int code = response.statusCode();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("WhatsApp Cloud API error (to=" + to + "): " + code + " - " + (response.body() == null ? "" : response.body()));
        }

        if (response.body() != null && !response.body().isBlank()) {
            System.out.println("WhatsApp Cloud API success (to=" + to + "): " + response.body());
        }
    }

    private static String normalizeToWhatsAppTo(String phone) {
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
