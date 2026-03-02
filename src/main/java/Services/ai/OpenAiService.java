package Services.Ai;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenAiService {

    private final HttpClient http = HttpClient.newHttpClient();
    private static final String ENDPOINT = "https://api.openai.com/v1/chat/completions";
    private static String cachedKey = null;

    private String apiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();

        String sys = System.getProperty("OPENAI_API_KEY");
        if (sys != null && !sys.isBlank()) return sys.trim();

        throw new IllegalStateException("Missing OPENAI_API_KEY environment variable");
    }

    public String generateOfferText(String poste, String niveau, String ville) {
        String prompt = "GÃ©nÃ¨re une offre d'emploi professionnelle en franÃ§ais avec:\n" +
                "1) Description professionnelle\n" +
                "2) Missions (liste)\n" +
                "3) CompÃ©tences requises (liste)\n" +
                "4) Profil recherchÃ©\n\n" +
                "Poste: " + safe(poste) + ", Niveau: " + safe(niveau) + ", Ville: " + safe(ville) + "\n" +
                "Style: professionnel, attractif, sans emojis.";
        return chat(prompt);
    }

    public String improveText(String rawText) {
        String prompt = "AmÃ©liore ce texte en franÃ§ais (corrige fautes, rend professionnel et attractif):\n\n" + safe(rawText);
        return chat(prompt);
    }

    public String generateText(String prompt) {
        return chat(prompt);
    }

    private String chat(String prompt) {
        String[] models = new String[]{"gpt-3.5-turbo", "gpt-4o-mini"};
        String env = System.getenv("OPENAI_MODEL");
        if (env != null && !env.isBlank()) models = new String[]{env.trim()};

        for (String model : models) {
            try {
                return callModel(prompt, model);
            } catch (RuntimeException ex) {
                String m = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                if (m.contains("model") && (m.contains("not found") || m.contains("not exist"))) {
                    continue;
                }
                throw ex;
            }
        }
        throw new RuntimeException("Aucun modÃ¨le OpenAI disponible");
    }

    private String callModel(String prompt, String model) {
        try {
            String key = apiKey();
            
            String body = "{"
                + "\"model\":" + json(model) + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + json("Tu es un assistant RH expert. Tu Ã©cris en franÃ§ais, style professionnel.") + "},"
                + "{\"role\":\"user\",\"content\":" + json(prompt) + "}"
                + "],"
                + "\"temperature\":0.7,\"max_tokens\":800"
                + "}";

            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Authorization", "Bearer " + key)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                String errorBody = res.body();
                throw new RuntimeException("HTTP " + res.statusCode() + ": " + errorBody);
            }
            String result = extract(res.body());
            return result;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Erreur rÃ©seau: " + e.getMessage(), e);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String json(String s) {
        if (s == null) return "\"\"";
        String out = s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "")
            .replace("\t", "\\t");
        return "\"" + out + "\"";
    }

    private static String extract(String json) {
        if (json == null) return "";
        String marker = "\"content\":";
        int i = json.indexOf(marker);
        if (i < 0) return json;
        int start = i + marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return json;
        start++;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int j = start; j < json.length(); j++) {
            char c = json.charAt(j);
            if (esc) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    default: sb.append(c);
                }
                esc = false;
                continue;
            }
            if (c == '\\') { esc = true; continue; }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString().trim();
    }
}

