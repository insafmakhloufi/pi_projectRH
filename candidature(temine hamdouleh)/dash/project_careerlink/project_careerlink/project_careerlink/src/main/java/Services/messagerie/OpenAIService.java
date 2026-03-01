package Services.messagerie;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class OpenAIService {

    private static final String API_KEY = ""; // ← remplace par ta vraie clé
    private static final String API_URL = "";

    public String obtenirReponse(String question) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String systemPrompt = "Tu es un assistant RH professionnel. " +
                    "Tu reponds aux questions basiques des candidats concernant " +
                    "les postes, contrats, salaires, entretiens et processus de recrutement. " +
                    "Sois concis, professionnel et bienveillant.";

            String body = "{"
                    + "\"model\": \"gpt-3.5-turbo\","
                    + "\"messages\": ["
                    + "  {\"role\": \"system\", \"content\": \"" + systemPrompt + "\"},"
                    + "  {\"role\": \"user\", \"content\": \"" + question.replace("\"", "'") + "\"}"
                    + "],"
                    + "\"max_tokens\": 300"
                    + "}";

            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("utf-8"));
            os.flush();

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "utf-8")
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            // ✅ Parser correctement la réponse JSON
            String json = response.toString();

            // Chercher "content" dans les choices
            int contentIndex = json.indexOf("\"content\":");
            if (contentIndex == -1) {
                return "Je suis désolé, je ne peux pas répondre pour le moment.";
            }

            // Avancer après "content":
            int start = contentIndex + 10;

            // Passer les espaces et le guillemet ouvrant
            while (start < json.length() &&
                    (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
                start++;
            }

            // Chercher la fin du contenu (guillemet fermant non échappé)
            StringBuilder content = new StringBuilder();
            while (start < json.length()) {
                char c = json.charAt(start);
                if (c == '\\') {
                    // Caractère échappé
                    start++;
                    if (start < json.length()) {
                        char next = json.charAt(start);
                        if (next == 'n') content.append('\n');
                        else if (next == 't') content.append('\t');
                        else if (next == '"') content.append('"');
                        else if (next == '\\') content.append('\\');
                        else content.append(next);
                    }
                } else if (c == '"') {
                    // Fin du contenu
                    break;
                } else {
                    content.append(c);
                }
                start++;
            }

            String result = content.toString().trim();
            return result.isEmpty()
                    ? "Je suis désolé, je ne peux pas répondre pour le moment."
                    : result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Je suis désolé, je ne peux pas répondre pour le moment.";
        }
    }
}
