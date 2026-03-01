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
import java.util.Locale;
import java.util.Map;

public class AiTestGeneratorService {

    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(45);

    public GeneratedTestPayload generate(GenerationRequest request) {
        validateInputRequest(request);
        ensureSafePromptInput(request.description());

        String apiKey = AiConfig.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("OPENAI_API_KEY manquante. Configurez la variable d'environnement ou config.properties.");
        }

        String model = AiConfig.getModel();
        String body = buildRequestBody(request, model);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(HTTP_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Echec appel IA: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Echec appel IA: " + e.getMessage(), e);
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Erreur API IA (HTTP " + response.statusCode() + ").");
        }

        String assistantJson = extractAssistantJsonContent(response.body());
        GeneratedTestPayload payload = parseGeneratedPayload(assistantJson);
        validatePayload(payload, request);
        return payload;
    }

    private void validateInputRequest(GenerationRequest request) {
        if (request == null) throw new IllegalArgumentException("Parametres generation invalides.");
        if (request.candidatId() <= 0) throw new IllegalArgumentException("Candidat obligatoire.");
        if (request.type() == null || request.type().isBlank()) throw new IllegalArgumentException("Type obligatoire.");
        if (request.nombreQuestions() < 5 || request.nombreQuestions() > 30) {
            throw new IllegalArgumentException("nombreQuestions doit etre entre 5 et 30.");
        }
        if (request.difficulte() == null || request.difficulte().isBlank()) {
            throw new IllegalArgumentException("Difficulte obligatoire.");
        }
        if (request.description() == null || request.description().trim().length() < 10) {
            throw new IllegalArgumentException("Description obligatoire (min 10 caracteres).");
        }
    }

    private String buildRequestBody(GenerationRequest request, String model) {
        String prompt = buildPrompt(request);
        return "{"
                + "\"model\":\"" + jsonEscape(model) + "\","
                + "\"temperature\":0.3,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are a safe assistant for educational/professional test generation. Return ONLY valid JSON, no markdown.\"},"
                + "{\"role\":\"user\",\"content\":\"" + jsonEscape(prompt) + "\"}"
                + "]"
                + "}";
    }

    private String buildPrompt(GenerationRequest request) {
        return ""
                + "Generate a strict JSON object only. No prose, no markdown, no explanations.\n"
                + "Context:\n"
                + "- type=" + request.type() + "\n"
                + "- nombreQuestions=" + request.nombreQuestions() + "\n"
                + "- difficulte=" + request.difficulte() + "\n"
                + "- description=" + request.description() + "\n"
                + "Required output schema:\n"
                + "{\n"
                + "  \"test\": {\"titre\":\"...\", \"type\":\"...\", \"durationSeconds\":600, \"totalPoints\":10, \"scoreMax\":10},\n"
                + "  \"questions\": [\n"
                + "    {\n"
                + "      \"question\":\"...\",\n"
                + "      \"points\":1,\n"
                + "      \"reponse\":\"optional\",\n"
                + "      \"propositions\":[{\"contenu\":\"...\",\"estCorrect\":false}]\n"
                + "    }\n"
                + "  ]\n"
                + "}\n"
                + "Rules:\n"
                + "- Keep exactly requested test type: " + request.type() + ".\n"
                + "- Create exactly " + request.nombreQuestions() + " questions.\n"
                + "- If type is QCM: each question MUST include at least 4 propositions and exactly 1 proposition with estCorrect=true.\n"
                + "- If type is not QCM: do not include propositions (or empty array), reponse can be provided when relevant.\n"
                + "- Content must be safe, non-violent, non-dangerous, no illegal instructions.\n"
                + "- Return strict valid JSON only.";
    }

    @SuppressWarnings("unchecked")
    private String extractAssistantJsonContent(String rawResponse) {
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
            throw new RuntimeException("Reponse IA invalide (choice).");
        }

        Object messageObj = firstMap.get("message");
        if (!(messageObj instanceof Map<?, ?> messageMap)) {
            throw new RuntimeException("Reponse IA invalide (message).");
        }

        Object content = messageMap.get("content");
        if (content instanceof String s) {
            return s.trim();
        }
        if (content instanceof List<?> parts && !parts.isEmpty()) {
            Object p0 = parts.get(0);
            if (p0 instanceof Map<?, ?> partMap) {
                Object txt = partMap.get("text");
                if (txt instanceof String s) return s.trim();
            }
        }

        throw new RuntimeException("Reponse IA sans contenu JSON.");
    }

    @SuppressWarnings("unchecked")
    private GeneratedTestPayload parseGeneratedPayload(String json) {
        Object parsed = new JsonParser(json).parseValue();
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new RuntimeException("JSON IA invalide (racine).");
        }

        Map<String, Object> testMap = castMap(root.get("test"), "test");
        GeneratedTest test = new GeneratedTest(
                asString(testMap.get("titre"), "test.titre"),
                asString(testMap.get("type"), "test.type"),
                asInt(testMap.get("durationSeconds"), "test.durationSeconds"),
                asInt(testMap.get("totalPoints"), "test.totalPoints"),
                asDouble(testMap.get("scoreMax"), "test.scoreMax")
        );

        Object questionsObj = root.get("questions");
        if (!(questionsObj instanceof List<?> qList)) {
            throw new RuntimeException("JSON IA invalide: questions manquant.");
        }
        List<GeneratedQuestion> questions = new ArrayList<>();
        for (Object obj : qList) {
            Map<String, Object> qMap = castMap(obj, "questions[]");
            String questionText = asString(qMap.get("question"), "questions[].question");
            int points = asInt(qMap.get("points"), "questions[].points");
            String reponse = optString(qMap.get("reponse"));

            List<GeneratedProposition> propositions = new ArrayList<>();
            Object propsObj = qMap.get("propositions");
            if (propsObj instanceof List<?> propsList) {
                for (Object propObj : propsList) {
                    Map<String, Object> pMap = castMap(propObj, "questions[].propositions[]");
                    propositions.add(new GeneratedProposition(
                            asString(pMap.get("contenu"), "propositions[].contenu"),
                            asBoolean(pMap.get("estCorrect"), "propositions[].estCorrect")
                    ));
                }
            }
            questions.add(new GeneratedQuestion(questionText, points, reponse, propositions));
        }

        return new GeneratedTestPayload(test, questions);
    }

    private void validatePayload(GeneratedTestPayload payload, GenerationRequest request) {
        if (payload == null || payload.test() == null) {
            throw new RuntimeException("Payload IA vide.");
        }
        if (payload.questions() == null || payload.questions().isEmpty()) {
            throw new RuntimeException("Payload IA sans questions.");
        }
        if (payload.questions().size() != request.nombreQuestions()) {
            throw new RuntimeException("L'IA n'a pas respecte le nombre exact de questions.");
        }

        GeneratedTest test = payload.test();
        if (test.titre() == null || test.titre().trim().length() < 3) {
            throw new RuntimeException("Titre genere invalide.");
        }

        String requestedType = request.type().trim();
        String generatedType = test.type() == null ? "" : test.type().trim();
        if (!requestedType.equalsIgnoreCase(generatedType)) {
            throw new RuntimeException("Type genere invalide. Attendu: " + requestedType + ".");
        }
        if (test.durationSeconds() <= 0 || test.totalPoints() <= 0 || test.scoreMax() <= 0) {
            throw new RuntimeException("Meta test generee invalide.");
        }

        boolean qcm = requestedType.equalsIgnoreCase("QCM");
        ensureSafePromptInput(test.titre());

        for (GeneratedQuestion question : payload.questions()) {
            if (question.question() == null || question.question().trim().isEmpty()) {
                throw new RuntimeException("Question vide detectee.");
            }
            if (question.points() < 1) {
                throw new RuntimeException("Points invalides dans une question.");
            }
            ensureSafePromptInput(question.question());
            if (question.reponse() != null) {
                ensureSafePromptInput(question.reponse());
            }

            if (qcm) {
                List<GeneratedProposition> props = question.propositions();
                if (props == null || props.size() < 4) {
                    throw new RuntimeException("QCM invalide: chaque question doit avoir au moins 4 propositions.");
                }
                int correctCount = 0;
                for (GeneratedProposition proposition : props) {
                    if (proposition.contenu() == null || proposition.contenu().trim().isEmpty()) {
                        throw new RuntimeException("Proposition vide detectee.");
                    }
                    ensureSafePromptInput(proposition.contenu());
                    if (proposition.estCorrect()) correctCount++;
                }
                if (correctCount != 1) {
                    throw new RuntimeException("QCM invalide: il faut exactement 1 proposition correcte par question.");
                }
            }
        }
    }

    private void ensureSafePromptInput(String text) {
        if (text == null) return;
        String lower = text.toLowerCase(Locale.ROOT);
        String[] blocked = new String[]{
                "bombe", "bomb", "explosif", "explosive", "arme", "weapon",
                "terror", "drugs", "drogue", "kill", "suicide", "malware", "piratage"
        };
        for (String token : blocked) {
            if (lower.contains(token)) {
                throw new RuntimeException("Contenu potentiellement dangereux detecte.");
            }
        }
    }

    private Map<String, Object> castMap(Object obj, String name) {
        if (obj instanceof Map<?, ?> in) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : in.entrySet()) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return out;
        }
        throw new RuntimeException("JSON invalide: " + name);
    }

    private String asString(Object obj, String field) {
        if (obj instanceof String s && !s.trim().isEmpty()) {
            return s.trim();
        }
        throw new RuntimeException("Champ manquant/invalide: " + field);
    }

    private String optString(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) {
            String t = s.trim();
            return t.isEmpty() ? null : t;
        }
        return null;
    }

    private int asInt(Object obj, String field) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new RuntimeException("Champ numerique invalide: " + field);
    }

    private double asDouble(Object obj, String field) {
        if (obj instanceof Number n) return n.doubleValue();
        if (obj instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        throw new RuntimeException("Champ numerique invalide: " + field);
    }

    private boolean asBoolean(Object obj, String field) {
        if (obj instanceof Boolean b) return b;
        if (obj instanceof String s) {
            if ("true".equalsIgnoreCase(s)) return true;
            if ("false".equalsIgnoreCase(s)) return false;
        }
        throw new RuntimeException("Champ booleen invalide: " + field);
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
                default -> {
                    if (c < 32) sb.append(' ');
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    public record GenerationRequest(
            int candidatId,
            String type,
            int nombreQuestions,
            String difficulte,
            String description
    ) {
    }

    public record GeneratedTestPayload(GeneratedTest test, List<GeneratedQuestion> questions) {
    }

    public record GeneratedTest(
            String titre,
            String type,
            int durationSeconds,
            int totalPoints,
            double scoreMax
    ) {
    }

    public record GeneratedQuestion(
            String question,
            int points,
            String reponse,
            List<GeneratedProposition> propositions
    ) {
    }

    public record GeneratedProposition(String contenu, boolean estCorrect) {
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
            throw new RuntimeException("JSON invalide autour de: " + c);
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
                    if (index >= source.length()) break;
                    char esc = source.charAt(index++);
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (index + 4 > source.length()) throw new RuntimeException("Unicode escape invalide.");
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
                return Boolean.TRUE;
            }
            if (source.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
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
            if (peekRaw('e') || peekRaw('E')) {
                index++;
                if (peekRaw('+') || peekRaw('-')) index++;
                while (index < source.length() && Character.isDigit(source.charAt(index))) index++;
            }
            String num = source.substring(start, index).trim();
            if (num.contains(".") || num.contains("e") || num.contains("E")) {
                return Double.parseDouble(num);
            }
            try {
                return Integer.parseInt(num);
            } catch (NumberFormatException ex) {
                return Long.parseLong(num);
            }
        }

        private void skipWs() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') index++;
                else break;
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
