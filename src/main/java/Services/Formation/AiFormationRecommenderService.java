package Services.Formation;

import Entites.Formation.Formation;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AiFormationRecommenderService {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(45);

    public List<Formation> recommend(String testTitre, String testType, int score, int maxScore, List<Formation> allFormations) {
        if (allFormations == null || allFormations.isEmpty()) {
            return List.of();
        }

        try {
            String apiKey = AiConfig.getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return List.of();
            }

            String model = AiConfig.getModel();
            String body = buildRequestBody(testTitre, testType, score, maxScore, allFormations, model);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(HTTP_TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            String assistantContent = extractAssistantContent(response.body());
            if (assistantContent == null || assistantContent.isBlank()) {
                return List.of();
            }

            List<Integer> ids = parseRecommendedIds(assistantContent);
            if (ids.isEmpty()) {
                return List.of();
            }

            Map<Integer, Formation> byId = new LinkedHashMap<>();
            for (Formation f : allFormations) {
                if (f != null) {
                    byId.put(f.getId(), f);
                }
            }

            List<Formation> out = new ArrayList<>();
            for (Integer id : ids) {
                Formation f = byId.get(id);
                if (f != null) {
                    out.add(f);
                }
            }
            return out;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private String buildRequestBody(String testTitre, String testType, int score, int maxScore, List<Formation> formations, String model) {
        StringBuilder sb = new StringBuilder();
        for (Formation f : formations) {
            if (f == null) {
                continue;
            }
            sb.append("- ID:").append(f.getId())
                    .append(" | Titre:").append(nullToEmpty(f.getTitre()))
                    .append(" | Domaine:").append(nullToEmpty(f.getDomaine()))
                    .append(" | Description:").append(nullToEmpty(f.getDescription()))
                    .append("\n");
        }

        int pct = (maxScore <= 0) ? 0 : (int) Math.round(score * 100.0 / maxScore);

        String systemPrompt = "Tu es un conseiller en formation professionnelle. "
                + "Ton rôle est de recommander des formations pertinentes à un candidat "
                + "qui a échoué à un test. Réponds UNIQUEMENT avec un tableau JSON "
                + "contenant les IDs des formations recommandées, format exact : "
                + "[1, 5, 12] "
                + "Recommande entre 1 et 3 formations maximum. "
                + "Ne mets aucun texte avant ou après le tableau JSON.";

        String userPrompt = "Le candidat a passé le test \"" + nullToEmpty(testTitre) + "\" (type: " + nullToEmpty(testType) + "). "
                + "Son score est " + score + "/" + maxScore + " (" + pct + "%). "
                + "Voici les formations disponibles :\n" + sb + "\n"
                + "Quelles formations recommandes-tu pour combler ses lacunes ?";

        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"temperature\":0.2,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + jsonEscape(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(userPrompt) + "\"}"
                + "]"
                + "}";
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantContent(String rawResponse) {
        Object rootObj = new JsonParser(rawResponse).parseValue();
        if (!(rootObj instanceof Map<?, ?> root)) {
            return null;
        }
        Object choicesObj = root.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object first = choices.get(0);
        if (!(first instanceof Map<?, ?> choiceMap)) {
            return null;
        }
        Object messageObj = choiceMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object contentObj = messageMap.get("content");
        if (contentObj instanceof String s) {
            return s.trim();
        }
        if (contentObj instanceof List<?> parts && !parts.isEmpty()) {
            Object p0 = parts.get(0);
            if (p0 instanceof Map<?, ?> partMap) {
                Object txt = partMap.get("text");
                if (txt instanceof String s) {
                    return s.trim();
                }
            }
        }
        return null;
    }

    private List<Integer> parseRecommendedIds(String content) {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();

        String trimmed = content.trim();
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int end = trimmed.indexOf(']');
            String array = trimmed.substring(0, end + 1);
            addIdsFromArrayText(array, ids);
        } else {
            Matcher m = Pattern.compile("\\[[^\\]]*\\]").matcher(trimmed);
            if (m.find()) {
                addIdsFromArrayText(m.group(), ids);
            }
        }

        List<Integer> out = new ArrayList<>();
        for (Integer id : ids) {
            if (id != null && id > 0) {
                out.add(id);
            }
            if (out.size() == 3) {
                break;
            }
        }
        return out;
    }

    private void addIdsFromArrayText(String arrayText, LinkedHashSet<Integer> ids) {
        if (arrayText == null || arrayText.isBlank()) {
            return;
        }
        String clean = arrayText.trim();
        if (clean.startsWith("[")) {
            clean = clean.substring(1);
        }
        if (clean.endsWith("]")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        if (clean.isBlank()) {
            return;
        }
        String[] parts = clean.split(",");
        for (String p : parts) {
            try {
                ids.add(Integer.parseInt(p.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed IDs from model output.
            }
        }
    }

    private String jsonEscape(String input) {
        if (input == null) {
            return "";
        }
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
                    if (c < 32) {
                        sb.append(' ');
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private String nullToEmpty(String v) {
        return v == null ? "" : v;
    }

    private static final class JsonParser {
        private final String source;
        private int index = 0;

        JsonParser(String source) {
            this.source = source == null ? "" : source;
        }

        Object parseValue() {
            skipWs();
            if (index >= source.length()) {
                throw new RuntimeException("JSON vide.");
            }
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
                skipWs();
                String key = parseString();
                skipWs();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
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
                Object value = parseValue();
                list.add(value);
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
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (index >= source.length()) {
                        break;
                    }
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
                        default -> throw new RuntimeException("Escape JSON invalide.");
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new RuntimeException("String JSON invalide.");
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
            throw new RuntimeException("Boolean JSON invalide.");
        }

        private Object parseNull() {
            if (source.startsWith("null", index)) {
                index += 4;
                return null;
            }
            throw new RuntimeException("Null JSON invalide.");
        }

        private Number parseNumber() {
            int start = index;
            if (peekRaw('-')) index++;
            while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            if (peekRaw('.')) {
                index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            }
            String num = source.substring(start, index);
            if (num.contains(".")) {
                return Double.parseDouble(num);
            }
            return Long.parseLong(num);
        }

        private void skipWs() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    index++;
                } else {
                    break;
                }
            }
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
    }
}
