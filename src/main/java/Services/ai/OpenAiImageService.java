package Services.ai;

import Utils.AiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiImageService {

    private static final String IMAGES_ENDPOINT = "https://api.openai.com/v1/images/generations";
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final Pattern B64_PATTERN = Pattern.compile("\"b64_json\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile("\"message\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"");

    private final HttpClient client;

    public OpenAiImageService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(12))
                .build();
    }

    public byte[] generateImagePng(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new IllegalArgumentException("Le prompt image est obligatoire.");
        }

        String apiKey = AiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(AiConfig.buildMissingApiKeyMessage());
        }

        String normalizedPrompt = prompt.trim();

        // Primary: gpt-image-1.
        String primaryBody = "{"
                + "\"model\":\"gpt-image-1\","
                + "\"prompt\":\"" + escapeJson(normalizedPrompt) + "\","
                + "\"size\":\"1024x1024\","
                + "\"n\":1,"
                + "\"response_format\":\"b64_json\""
                + "}";
        HttpResponse<String> primaryResponse = sendImageRequest(apiKey, primaryBody);
        if (isSuccess(primaryResponse.statusCode())) {
            return parseImagePayload(primaryResponse.body());
        }

        // Fallback for projects where gpt-image-1 is unavailable/restricted.
        String fallbackBody = "{"
                + "\"model\":\"dall-e-3\","
                + "\"prompt\":\"" + escapeJson(normalizedPrompt) + "\","
                + "\"size\":\"1024x1024\","
                + "\"quality\":\"standard\","
                + "\"response_format\":\"url\""
                + "}";
        HttpResponse<String> fallbackResponse = sendImageRequest(apiKey, fallbackBody);
        if (isSuccess(fallbackResponse.statusCode())) {
            return parseImagePayload(fallbackResponse.body());
        }

        throw buildImageApiException(fallbackResponse);
    }

    private byte[] downloadImage(String url) {
        HttpRequest downloadRequest = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = client.send(downloadRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Impossible de telecharger l'image generee (HTTP " + response.statusCode() + ").");
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Telechargement image interrompu.", e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur reseau pendant telechargement image.", e);
        }
    }

    private String extractFirst(String payload, Pattern pattern) {
        Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private HttpResponse<String> sendImageRequest(String apiKey, String body) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(IMAGES_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Generation image interrompue.", e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur reseau/timeout pendant la generation image.", e);
        }
    }

    private boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    private byte[] parseImagePayload(String payload) {
        String safePayload = payload == null ? "" : payload;
        String b64 = extractFirst(safePayload, B64_PATTERN);
        if (b64 != null) {
            try {
                return Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Reponse image invalide (base64).", e);
            }
        }

        String url = extractFirst(safePayload, URL_PATTERN);
        if (url != null) {
            return downloadImage(url);
        }
        throw new RuntimeException("Aucune image trouvee dans la reponse OpenAI.");
    }

    private RuntimeException buildImageApiException(HttpResponse<String> response) {
        int status = response == null ? -1 : response.statusCode();
        String payload = response == null || response.body() == null ? "" : response.body();

        if (status == 401) {
            return new RuntimeException("Cle OpenAI invalide (401).");
        }
        if (status == 429) {
            return new RuntimeException("Quota OpenAI atteint ou limite de requetes (429).");
        }

        String apiMessage = extractFirst(payload, ERROR_MESSAGE_PATTERN);
        if (apiMessage != null && !apiMessage.isBlank()) {
            return new RuntimeException("Erreur OpenAI image (HTTP " + status + "): " + unescapeJson(apiMessage));
        }
        return new RuntimeException("Erreur OpenAI image (HTTP " + status + ").");
    }

    private String escapeJson(String input) {
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 32 ? ' ' : c);
            }
        }
        return sb.toString();
    }

    private String unescapeJson(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c != '\\' || i + 1 >= input.length()) {
                out.append(c);
                continue;
            }
            char n = input.charAt(++i);
            switch (n) {
                case '"', '\\', '/' -> out.append(n);
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> {
                    if (i + 4 < input.length()) {
                        String hex = input.substring(i + 1, i + 5);
                        try {
                            out.append((char) Integer.parseInt(hex, 16));
                            i += 4;
                        } catch (NumberFormatException e) {
                            out.append("\\u").append(hex);
                            i += 4;
                        }
                    } else {
                        out.append("\\u");
                    }
                }
                default -> out.append(n);
            }
        }
        return out.toString();
    }
}
