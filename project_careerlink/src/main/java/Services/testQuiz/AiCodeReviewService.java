package Services.testQuiz;

import Utils.AiConfig;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiCodeReviewService {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";

    public AiCodeResult reviewCode(String questionText, String userCode, String expectedAnswer, String language) {
        String apiKey = AiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(AiConfig.buildMissingApiKeyMessage());
        }

        String model = AiConfig.getModel();
        String systemPrompt = "Tu es un correcteur de code expert. Evalue si le code soumis par l'etudiant "
                + "resout correctement le probleme pose. Reponds UNIQUEMENT en JSON avec ce format exact : "
                + "{\"correct\": true/false, \"score\": 0-100, \"feedback\": \"explication courte en francais\"}";
        String userPrompt = "Question : " + safe(questionText) + "\n"
                + "Langage : " + safe(language) + "\n"
                + "Solution attendue : " + safe(expectedAnswer) + "\n"
                + "Code soumis par l'etudiant :\n" + safe(userCode);

        String body = "{"
                + "\"model\":\"" + escape(model) + "\","
                + "\"temperature\":0.1,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escape(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escape(userPrompt) + "\"}"
                + "]"
                + "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(50))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Appel IA interrompu.", e);
        } catch (IOException e) {
            throw new RuntimeException("Erreur appel IA: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Erreur API IA (HTTP " + response.statusCode() + ").");
        }

        String assistantContent = extractAssistantContent(response.body());
        return parseAiCodeResult(assistantContent);
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantContent(String rawResponse) {
        Object root = new JsonParser(rawResponse).parseValue();
        if (!(root instanceof Map<?, ?> map)) {
            throw new RuntimeException("Reponse IA invalide.");
        }
        Object choicesObj = map.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            throw new RuntimeException("Reponse IA sans choix.");
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> firstMap)) {
            throw new RuntimeException("Reponse IA invalide.");
        }
        Object msg = firstMap.get("message");
        if (!(msg instanceof Map<?, ?> messageMap)) {
            throw new RuntimeException("Reponse IA invalide.");
        }
        Object content = messageMap.get("content");
        if (content instanceof String s) return s.trim();
        if (content instanceof List<?> parts && !parts.isEmpty()) {
            Object p0 = parts.get(0);
            if (p0 instanceof Map<?, ?> partMap) {
                Object t = partMap.get("text");
                if (t instanceof String s) return s.trim();
            }
        }
        throw new RuntimeException("Reponse IA sans contenu.");
    }

    @SuppressWarnings("unchecked")
    private AiCodeResult parseAiCodeResult(String json) {
        Object parsed = new JsonParser(json).parseValue();
        if (!(parsed instanceof Map<?, ?> in)) {
            throw new RuntimeException("JSON IA invalide.");
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            map.put(String.valueOf(e.getKey()), e.getValue());
        }

        boolean correct = asBoolean(map.get("correct"));
        int score = clampScore(asInt(map.get("score")));
        String feedback = asString(map.get("feedback"));
        return new AiCodeResult(correct, score, feedback);
    }

    private boolean asBoolean(Object obj) {
        if (obj instanceof Boolean b) return b;
        if (obj instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        throw new RuntimeException("Champ correct invalide.");
    }

    private int asInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) return Integer.parseInt(s.trim());
        throw new RuntimeException("Champ score invalide.");
    }

    private String asString(Object obj) {
        if (obj instanceof String s) return s.trim();
        return "";
    }

    private int clampScore(int score) {
        if (score < 0) return 0;
        return Math.min(score, 100);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String escape(String input) {
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
                default -> {
                    if (c < 32) sb.append(' ');
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    private static final class JsonParser {
        private final String source;
        private int index = 0;

        JsonParser(String source) {
            this.source = source == null ? "" : source;
        }

        Object parseValue() {
            skipWs();
            if (index >= source.length()) throw new RuntimeException("JSON vide.");
            char c = source.charAt(index);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBoolean();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumber();
            throw new RuntimeException("JSON invalide.");
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWs();
            Map<String, Object> map = new LinkedHashMap<>();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                String key = parseString();
                expect(':');
                map.put(key, parseValue());
                skipWs();
                if (peek('}')) {
                    expect('}');
                    break;
                }
                expect(',');
            }
            return map;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWs();
            List<Object> list = new ArrayList<>();
            if (peek(']')) {
                expect(']');
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWs();
                if (peek(']')) {
                    expect(']');
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    char esc = source.charAt(index++);
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            String hex = source.substring(index, index + 4);
                            index += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                        }
                        default -> throw new RuntimeException("Escape invalide.");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("String invalide.");
        }

        private Boolean parseBoolean() {
            if (source.startsWith("true", index)) {
                index += 4;
                return true;
            }
            if (source.startsWith("false", index)) {
                index += 5;
                return false;
            }
            throw new RuntimeException("Boolean invalide.");
        }

        private Object parseNull() {
            if (source.startsWith("null", index)) {
                index += 4;
                return null;
            }
            throw new RuntimeException("Null invalide.");
        }

        private Number parseNumber() {
            int start = index;
            if (peekRaw('-')) index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            if (peekRaw('.')) {
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            }
            String token = source.substring(start, index);
            if (token.contains(".")) return Double.parseDouble(token);
            return Integer.parseInt(token);
        }

        private void expect(char c) {
            skipWs();
            if (index >= source.length() || source.charAt(index) != c) {
                throw new RuntimeException("JSON attendu: " + c);
            }
            index++;
        }

        private boolean peek(char c) {
            skipWs();
            return index < source.length() && source.charAt(index) == c;
        }

        private boolean peekRaw(char c) {
            return index < source.length() && source.charAt(index) == c;
        }

        private void skipWs() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') index++;
                else break;
            }
        }
    }
}
