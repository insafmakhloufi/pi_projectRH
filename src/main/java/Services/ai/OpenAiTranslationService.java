package Services.ai;

import Utils.AiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiTranslationService {

    private static final String RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final Pattern OUTPUT_TEXT_PATTERN = Pattern.compile(
            "\\\"type\\\"\\s*:\\s*\\\"output_text\\\"[\\s\\S]*?\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"",
            Pattern.DOTALL
    );
    private static final Pattern OUTPUT_ARRAY_TEXT_PATTERN = Pattern.compile(
            "\\\"output\\\"\\s*:\\s*\\[[\\s\\S]*?\\\"content\\\"\\s*:\\s*\\[[\\s\\S]*?\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"",
            Pattern.DOTALL
    );
    private static final Pattern OUTPUT_TEXT_FALLBACK_PATTERN = Pattern.compile(
            "\\\"output_text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"",
            Pattern.DOTALL
    );
    private static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile(
            "\\\"message\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\""
    );

    private final HttpClient client;

    public OpenAiTranslationService() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public String translate(String text, String sourceLang, String targetLang) {
        String input = text == null ? "" : text.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException("Le texte source est obligatoire.");
        }

        String normalizedSource = normalizeSourceLang(sourceLang);
        String normalizedTarget = normalizeTargetLang(targetLang);

        String apiKey = AiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(AiConfig.buildMissingApiKeyMessage());
        }

        String model = AiConfig.getModel();
        if (model == null || model.isBlank()) {
            model = "gpt-4o-mini";
        }

        String instruction = buildInstruction(normalizedSource, normalizedTarget);
        String body = "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"input\":["
                + "{\"role\":\"system\",\"content\":[{\"type\":\"input_text\",\"text\":\"" + jsonEscape(instruction) + "\"}]},"
                + "{\"role\":\"user\",\"content\":[{\"type\":\"input_text\",\"text\":\"" + jsonEscape(input) + "\"}]}"
                + "]"
                + "}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(RESPONSES_ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Requete de traduction interrompue.", e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur reseau/timeout pendant la traduction.", e);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw mapApiError(status, response.body());
        }

        String translated = extractOutputText(response.body());
        if (translated == null || translated.isBlank()) {
            throw new RuntimeException("La reponse de traduction est vide.");
        }
        return translated.trim();
    }

    private String buildInstruction(String sourceLang, String targetLang) {
        String sourceLabel = sourceLang.equals("AUTO") ? "auto-detected language" : langLabel(sourceLang);
        String targetLabel = langLabel(targetLang);
        String base = "Translate the text from " + sourceLabel + " to " + targetLabel + ". "
                + "Return only the translated text. Keep names, numbers, punctuation. "
                + "Do not add explanations. Do not paraphrase.";
        if ("AR".equals(targetLang)) {
            return base + " Keep natural Arabic RTL text direction.";
        }
        return base;
    }

    private String normalizeSourceLang(String sourceLang) {
        String s = sourceLang == null ? "AUTO" : sourceLang.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "AUTO", "FR", "EN", "AR" -> s;
            default -> throw new IllegalArgumentException("Langue source invalide: " + sourceLang);
        };
    }

    private String normalizeTargetLang(String targetLang) {
        String t = targetLang == null ? "" : targetLang.trim().toUpperCase(Locale.ROOT);
        return switch (t) {
            case "FR", "EN", "AR" -> t;
            default -> throw new IllegalArgumentException("Langue cible invalide. Choisissez FR, EN ou AR.");
        };
    }

    private String langLabel(String code) {
        return switch (code) {
            case "FR" -> "French";
            case "EN" -> "English";
            case "AR" -> "Arabic";
            default -> code;
        };
    }

    private String extractOutputText(String payload) {
        String safe = payload == null ? "" : payload;

        Matcher m1 = OUTPUT_TEXT_PATTERN.matcher(safe);
        if (m1.find()) {
            return unescapeJson(m1.group(1));
        }

        Matcher m2 = OUTPUT_ARRAY_TEXT_PATTERN.matcher(safe);
        if (m2.find()) {
            return unescapeJson(m2.group(1));
        }

        Matcher m3 = OUTPUT_TEXT_FALLBACK_PATTERN.matcher(safe);
        if (m3.find()) {
            return unescapeJson(m3.group(1));
        }

        return null;
    }

    private RuntimeException mapApiError(int status, String payload) {
        if (status == 401) {
            return new RuntimeException("Cle API OpenAI invalide (401).");
        }
        if (status == 429) {
            return new RuntimeException("Quota OpenAI atteint ou limite de requetes (429).");
        }
        if (status == 408) {
            return new RuntimeException("Timeout de la requete OpenAI (408).");
        }

        String apiMessage = extractErrorMessage(payload);
        if (apiMessage != null && !apiMessage.isBlank()) {
            return new RuntimeException("Erreur OpenAI traduction (HTTP " + status + "): " + apiMessage);
        }
        return new RuntimeException("Erreur OpenAI traduction (HTTP " + status + ").");
    }

    private String extractErrorMessage(String payload) {
        String safe = payload == null ? "" : payload;
        Matcher matcher = ERROR_MESSAGE_PATTERN.matcher(safe);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }

    private String jsonEscape(String input) {
        if (input == null) return "";
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
