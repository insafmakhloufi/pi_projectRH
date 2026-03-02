package Services.candidature;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAILettreMotivationService {

    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL   = "gpt-4o-mini"; // ou "gpt-3.5-turbo"

    /**
     * GÃ©nÃ¨re une lettre de motivation via OpenAI GPT.
     */
    public String genererLettreMotivation(String prompt) throws Exception {
        String body = """
            {
              "model": "%s",
              "messages": [{"role": "user", "content": %s}],
              "max_tokens": 800,
              "temperature": 0.7
            }
            """.formatted(MODEL, toJsonString(prompt));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));


        System.out.println("DEBUG STATUS: " + response.statusCode());
        System.out.println("DEBUG BODY: " + response.body().substring(0, Math.min(300, response.body().length())));
        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur OpenAI: " + response.statusCode() + " - " + response.body());
        }

        // Parse JSON simple sans librairie externe
        String resp = response.body();
        int contentStart = resp.indexOf("\"content\":") + 11;
        int contentEnd   = resp.indexOf("\"", contentStart);
        // Cherche la vraie fin (peut contenir des \\n etc)
        String extracted = extractContent(resp);
        return extracted;
    }

    private String extractContent(String json) {
        // La rÃ©ponse OpenAI ressemble Ã  :
        // {"choices":[{"message":{"role":"assistant","content":"LA LETTRE ICI"}}]}

        try {
            // Cherche "content":"
            String marker = "\"content\": \"";
            int start = json.indexOf(marker);
            if (start < 0) {
                System.out.println("DEBUG JSON COMPLET: " + json);
                return "Erreur: contenu introuvable dans la rÃ©ponse";
            }
            start += marker.length();

            // Parcourt caractÃ¨re par caractÃ¨re pour gÃ©rer les Ã©chappements
            StringBuilder sb = new StringBuilder();
            int i = start;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n'  -> { sb.append('\n'); i += 2; }
                        case 'r'  -> { sb.append('\r'); i += 2; }
                        case 't'  -> { sb.append('\t'); i += 2; }
                        case '"'  -> { sb.append('"');  i += 2; }
                        case '\\' -> { sb.append('\\'); i += 2; }
                        default   -> { sb.append(c);    i++;    }
                    }
                } else if (c == '"') {
                    break; // fin du contenu
                } else {
                    sb.append(c);
                    i++;
                }
            }

            String result = sb.toString().trim();
            System.out.println("DEBUG LETTRE GENEREE: " + result.substring(0, Math.min(100, result.length())));
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur parsing: " + e.getMessage();
        }
    }

    private String toJsonString(String s) {
        // Transforme une String Java en valeur JSON correctement Ã©chappÃ©e
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }
}
