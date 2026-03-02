package Services.candidature;

import Entities.candidature.InterviewResultat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.*;
import java.util.Properties;

/**
 * InterviewAnalyseService
 * ────────────────────────
 * 1. transcribe(wavPath)         → Whisper API → texte
 * 2. analyseReponse(resultat)    → GPT → score + feedback + points forts/faibles + conseils
 */
public class InterviewAnalyseService {

    private static final String WHISPER_URL  = "https://api.openai.com/v1/audio/transcriptions";
    private static final String GPT_URL      = "https://api.openai.com/v1/chat/completions";
    private static final String GPT_MODEL    = "gpt-4o-mini";

    private final HttpClient httpClient;
    private final Gson       gson;
    private final String     apiKey;

    public InterviewAnalyseService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson       = new Gson();
        this.apiKey     = loadApiKey();
    }

    private String loadApiKey() {
        String key = System.getProperty("openai.api_key");
        if (key != null && !key.isBlank()) return key.trim();

        Properties p = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/config/openai.properties")) {
            if (in != null) p.load(in);
        } catch (Exception ignored) {
        }

        key = p.getProperty("openai.api_key", "").trim();
        if (key.isBlank()) {
            throw new IllegalStateException("openai.api_key est vide. Renseigne src/main/resources/config/openai.properties ou -Dopenai.api_key=...");
        }
        return key;
    }

    // ══════════════════════════════════════════════════════════════
    // 1. TRANSCRIPTION WHISPER
    // ══════════════════════════════════════════════════════════════

    /**
     * Transcrit le fichier WAV en texte via Whisper API.
     * @param wavPath chemin du fichier WAV
     * @return texte transcrit
     */
    public String transcribe(String wavPath) throws Exception {

        byte[] audioBytes = Files.readAllBytes(Path.of(wavPath));

        // Construire le multipart/form-data manuellement
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(body));

        // Champ model
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        writer.append("whisper-1\r\n");

        // Champ language
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        writer.append("fr\r\n");

        // Champ file
        writer.append("--").append(boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"answer.wav\"\r\n");
        writer.append("Content-Type: audio/wav\r\n\r\n");
        writer.flush();

        body.write(audioBytes);

        writer.append("\r\n--").append(boundary).append("--\r\n");
        writer.flush();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WHISPER_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Whisper erreur " + response.statusCode() + " : " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String texte = json.get("text").getAsString().trim();

        System.out.println("[InterviewAnalyseService] Transcription : " + texte);
        return texte;
    }

    // ══════════════════════════════════════════════════════════════
    // 2. ANALYSE GPT (score + feedback + points forts/faibles)
    // ══════════════════════════════════════════════════════════════

    /**
     * Analyse la réponse transcrite et remplit l'objet InterviewResultat.
     * @param resultat objet avec question + reponseTranscrite déjà remplis
     */
    public void analyseReponse(InterviewResultat resultat) throws Exception {

        String prompt = String.format("""
                Tu es un expert RH qui évalue des réponses d'entretien.
                
                Question posée : "%s"
                Réponse du candidat : "%s"
                
                Analyse cette réponse et retourne UNIQUEMENT un JSON valide avec exactement cette structure :
                {
                  "score": <nombre entre 0 et 100>,
                  "feedback": "<analyse générale en 2-3 phrases>",
                  "points_forts": "<ce que le candidat a bien fait, en 1-2 phrases>",
                  "points_faibles": "<ce qui manque ou peut être amélioré, en 1-2 phrases>",
                  "conseils": "<conseils concrets pour progresser, en 2-3 phrases>"
                }
                
                Règles :
                - Langue : français
                - Ton : constructif et bienveillant
                - Retourne UNIQUEMENT le JSON, aucun texte avant ou après
                """,
                resultat.getQuestion(),
                resultat.getReponseTranscrite()
        );

        JsonObject body = new JsonObject();
        body.addProperty("model", GPT_MODEL);
        body.addProperty("max_tokens", 500);
        body.addProperty("temperature", 0.5);

        com.google.gson.JsonArray messages = new com.google.gson.JsonArray();
        JsonObject msg = new JsonObject();
        msg.addProperty("role", "user");
        msg.addProperty("content", prompt);
        messages.add(msg);
        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GPT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GPT analyse erreur " + response.statusCode());
        }

        // Parser la réponse GPT
        JsonObject json     = gson.fromJson(response.body(), JsonObject.class);
        String     content  = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

        // Nettoyer si GPT ajoute des backticks markdown
        content = content.replace("```json", "").replace("```", "").trim();

        // Parser le JSON retourné par GPT
        JsonObject analyse = gson.fromJson(content, JsonObject.class);

        resultat.setScore(         analyse.get("score").getAsInt());
        resultat.setFeedback(      analyse.get("feedback").getAsString());
        resultat.setPointsForts(   analyse.get("points_forts").getAsString());
        resultat.setPointsFaibles( analyse.get("points_faibles").getAsString());
        resultat.setConseils(      analyse.get("conseils").getAsString());

        System.out.println("[InterviewAnalyseService] Score : " + resultat.getScore());
    }
}
