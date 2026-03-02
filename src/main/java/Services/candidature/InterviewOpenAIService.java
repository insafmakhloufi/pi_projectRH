package Services.candidature;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * InterviewOpenAIService
 * ─────────────
 * 1. generateQuestion()  → appelle GPT-4o-mini pour générer une question d'entretien
 * 2. textToSpeech()      → appelle OpenAI TTS pour convertir la question en fichier MP3
 */
public class InterviewOpenAIService {

    // ── Config ────────────────────────────────────────────────────────────────
    private static final String GPT_MODEL = "gpt-4o-mini";
    private static final String TTS_MODEL = "tts-1";
    private static final String TTS_VOICE = "echo";

    private static final String GPT_URL   = "https://api.openai.com/v1/chat/completions";
    private static final String TTS_URL   = "https://api.openai.com/v1/audio/speech";

    public static final String QUESTION_AUDIO = System.getProperty("java.io.tmpdir") + "/question.mp3";

    private final HttpClient httpClient;
    private final Gson gson;

    private final String apiKey;

    private final java.util.List<String> askedQuestions = new java.util.ArrayList<>();

    // ── Constructeur ──────────────────────────────────────────────────────────
    public InterviewOpenAIService() {
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

    // ══════════════════════════════════════════════════════════════════════════
    // 1. GÉNÉRER UNE QUESTION (GPT)
    // ══════════════════════════════════════════════════════════════════════════
    public String generateQuestion(int questionNumber) throws Exception {

        String historique = askedQuestions.isEmpty() ? "Aucune question posée encore." :
                "Questions déjà posées : " + String.join(" | ", askedQuestions);

        String systemPrompt = """
                Tu es un recruteur professionnel qui conduit un entretien d'embauche général.
                Tu poses des questions variées couvrant : motivation, personnalité, expérience,
                travail en équipe, gestion du stress, points forts/faibles, objectifs de carrière.
                
                Règles :
                - Une seule question courte et claire (max 2 phrases)
                - Langue : français
                - Ton : professionnel mais bienveillant
                - Ne répète jamais une question déjà posée
                - Ne numérote pas la question
                - Retourne UNIQUEMENT la question, rien d'autre
                """;

        String userPrompt = String.format(
                "Pose la question numéro %d de l'entretien. %s",
                questionNumber, historique
        );

        JsonObject body = new JsonObject();
        body.addProperty("model", GPT_MODEL);
        body.addProperty("max_tokens", 150);
        body.addProperty("temperature", 0.8);

        JsonArray messages = new JsonArray();

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", systemPrompt);
        messages.add(sysMsg);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userPrompt);
        messages.add(userMsg);

        body.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GPT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("GPT erreur " + response.statusCode() + " : " + response.body());
        }

        JsonObject json = gson.fromJson(response.body(), JsonObject.class);
        String question = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString()
                .trim();

        askedQuestions.add(question);
        System.out.println("[InterviewOpenAIService] Question générée : " + question);
        return question;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. TEXT TO SPEECH (TTS)
    // ══════════════════════════════════════════════════════════════════════════
    public String textToSpeech(String text) throws Exception {

        JsonObject body = new JsonObject();
        body.addProperty("model", TTS_MODEL);
        body.addProperty("voice", TTS_VOICE);
        body.addProperty("input", text);
        body.addProperty("speed", 0.95);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new RuntimeException("TTS erreur " + response.statusCode());
        }

        Path audioPath = Path.of(QUESTION_AUDIO);
        Files.write(audioPath, response.body());

        System.out.println("[InterviewOpenAIService] Audio TTS sauvegardé : " + QUESTION_AUDIO);
        return QUESTION_AUDIO;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────
    public void resetHistory() {
        askedQuestions.clear();
    }

    public int getQuestionsCount() {
        return askedQuestions.size();
    }
}