package Services.candidature;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.*;
import java.util.Base64;
import javax.sound.sampled.*;

public class CopiloteVocalService {

    private static final String WHISPER_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String GPT_URL = "https://api.openai.com/v1/chat/completions";
    private static final String TTS_URL = "https://api.openai.com/v1/audio/speech";

    private TargetDataLine microLine;
    private File audioFile;

    // ============ ENREGISTREMENT MICRO ============

    public void demarrerEnregistrement() throws Exception {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new RuntimeException("Microphone non supporté !");
        }

        microLine = (TargetDataLine) AudioSystem.getLine(info);
        microLine.open(format);
        microLine.start();

        audioFile = File.createTempFile("copilote_audio", ".wav");

        // Enregistrement dans un thread séparé
        new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(microLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println(" Enregistrement démarré...");
    }

    public File arreterEnregistrement() throws Exception {
        if (microLine != null) {
            microLine.stop();
            microLine.close();
            System.out.println(" Enregistrement arrêté : " + audioFile.getAbsolutePath());
        }
        return audioFile;
    }

    // ============ WHISPER : Audio → Texte ============

    public String transcrireAudio(File audioFile) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String apiKey = getApiKey();

        // Multipart boundary
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        byte[] fileBytes = Files.readAllBytes(audioFile.toPath());

        // Construire le body multipart manuellement
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Partie fichier
        String filePart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n"
                + "Content-Type: audio/wav\r\n\r\n";
        baos.write(filePart.getBytes());
        baos.write(fileBytes);
        baos.write("\r\n".getBytes());

        // Partie modèle
        String modelPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"model\"\r\n\r\n"
                + "whisper-1\r\n";
        baos.write(modelPart.getBytes());

        // Partie langue
        String langPart = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"language\"\r\n\r\n"
                + "fr\r\n";
        baos.write(langPart.getBytes());

        // Fin boundary
        baos.write(("--" + boundary + "--\r\n").getBytes());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WHISPER_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofByteArray(baos.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println("Whisper response: " + response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur Whisper: " + response.body());
        }

        // Parse { "text": "..." }
        String body = response.body();
        int start = body.indexOf("\"text\":") + 9;
        int end = body.lastIndexOf("\"");
        return body.substring(start, end).trim();
    }

    // ============ GPT : Texte → Action JSON ============

    public String comprendreCommande(String texte, String contexte) throws Exception {
        String systemPrompt = """
    Tu es un copilote vocal pour CareerLink, une application de gestion de candidatures.
    Tu reçois une commande en français et tu retournes UNIQUEMENT un JSON.
    
    Actions disponibles:
    - {"action": "NAVIGATE", "page": "MES_CANDIDATURES"}
    - {"action": "NAVIGATE", "page": "AJOUTER_CANDIDATURE"}
    - {"action": "NAVIGATE", "page": "OFFRES"}
    - {"action": "NAVIGATE", "page": "EVENEMENTS"}
    - {"action": "NAVIGATE", "page": "FORMATIONS"}
    - {"action": "NEXT_STEP"}
    - {"action": "PREV_STEP"}
    - {"action": "SUBMIT_FORM"}
    - {"action": "FILL_FIELD", "field": "tfPrenom", "value": "Ahmed"}
    - {"action": "FILL_FIELD", "field": "tfNom", "value": "Ben Salah"}
    - {"action": "FILL_FIELD", "field": "tfEmail", "value": "ahmed@gmail.com"}
    - {"action": "FILL_FIELD", "field": "tfAdresse", "value": "12 rue Paris"}
    - {"action": "FILL_FIELD", "field": "tfVille", "value": "Tunis"}
    - {"action": "FILL_FIELD", "field": "tfTelLocal", "value": "55123456"}
    - {"action": "FILL_FIELD", "field": "tfSkills", "value": "Java, Python"}
    - {"action": "FILL_FIELD", "field": "taMotivationLetter", "value": "Je suis motivé..."}
    - {"action": "FILL_DATE", "field": "dpNaissance", "value": "2000-01-15"}
    - {"action": "SELECT_RADIO", "field": "rbBachelor"}
    - {"action": "SELECT_RADIO", "field": "rbEngineering"}
    - {"action": "SELECT_RADIO", "field": "rbMasters"}
    - {"action": "SELECT_RADIO", "field": "rbPhd"}
    - {"action": "SELECT_COMBO", "field": "cbInstitution", "value": "ESPRIT"}
    - {"action": "SELECT_COMBO", "field": "cbIndicatif", "value": "+216"}
    - {"action": "START_VIDEO"}
    - {"action": "STOP_VIDEO"}
    - {"action": "CHECK_CONFIRM"}
    - {"action": "CHECK_PRIVACY"}
    - {"action": "SEARCH", "query": "Ahmed"}
    - {"action": "UNKNOWN"}
    
    Règles:
    - "navigue vers", "aller à", "ouvre", "va sur" → NAVIGATE
    - "étape suivante", "suivant", "continuer", "next" → NEXT_STEP
    - "étape précédente", "retour", "précédent" → PREV_STEP
    - "soumettre", "envoyer", "submit" → SUBMIT_FORM
    - "dans le champ prénom écris X" → FILL_FIELD tfPrenom
    - "dans le champ nom écris X" → FILL_FIELD tfNom
    - "dans le champ email écris X" → FILL_FIELD tfEmail
    - "dans le champ adresse écris X" → FILL_FIELD tfAdresse
    - "dans le champ ville écris X" → FILL_FIELD tfVille
    - "dans le champ téléphone écris X" → FILL_FIELD tfTelLocal
    - "dans le champ compétences écris X" → FILL_FIELD tfSkills
    - "dans le champ lettre de motivation écris X" → FILL_FIELD taMotivationLetter
    - "date de naissance X" → FILL_DATE dpNaissance
    - "mon diplôme est bachelor", "j'ai un bachelor" → SELECT_RADIO rbBachelor
    - "mon diplôme est engineering" → SELECT_RADIO rbEngineering
    - "mon diplôme est master", "j'ai un master" → SELECT_RADIO rbMasters
    - "mon diplôme est doctorat", "j'ai un phd" → SELECT_RADIO rbPhd
    - "mon établissement est X" → SELECT_COMBO cbInstitution
    - "indicatif X" → SELECT_COMBO cbIndicatif
    - "commencer enregistrement", "start video", "enregistrer" → START_VIDEO
    - "arrêter enregistrement", "stop video" → STOP_VIDEO
    - "je confirme les informations" → CHECK_CONFIRM
    - "j'accepte la politique" → CHECK_PRIVACY
    - "recherche X", "cherche X" → SEARCH
    
    Contexte actuel: """ + contexte + """
    
    Réponds UNIQUEMENT avec le JSON, rien d'autre.
    """;

        String body = """
            {
              "model": "gpt-4o-mini",
              "messages": [
                {"role": "system", "content": %s},
                {"role": "user", "content": %s}
              ],
              "max_tokens": 150,
              "temperature": 0.1
            }
            """.formatted(toJsonString(systemPrompt), toJsonString(texte));

        HttpClient client = HttpClient.newHttpClient();
        String apiKey = getApiKey();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GPT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur GPT: " + response.body());
        }

        // Extraire le contenu
        String resp = response.body();
        String marker = "\"content\": \"";
        int start = resp.indexOf(marker) + marker.length();
        StringBuilder sb = new StringBuilder();
        int i = start;
        while (i < resp.length()) {
            char c = resp.charAt(i);
            if (c == '\\' && i + 1 < resp.length()) {
                char next = resp.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i += 2; }
                    case '"' -> { sb.append('"'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    default -> { sb.append(c); i++; }
                }
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
                i++;
            }
        }

        System.out.println("GPT action: " + sb.toString().trim());
        return sb.toString().trim();
    }

    // ============ TTS : Texte → Voix ============

    public void parler(String texte) throws Exception {
        String body = """
            {
              "model": "tts-1",
              "input": %s,
              "voice": "nova"
            }
            """.formatted(toJsonString(texte));

        HttpClient client = HttpClient.newHttpClient();
        String apiKey = getApiKey();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(BodyPublishers.ofString(body))
                .build();

        HttpResponse<byte[]> response = client.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur TTS: " + response.statusCode());
        }

        // Jouer l'audio mp3 reçu
        File tempMp3 = File.createTempFile("copilote_tts", ".mp3");
        Files.write(tempMp3.toPath(), response.body());

        // Jouer via JavaFX Media
        javafx.application.Platform.runLater(() -> {
            try {
                javafx.scene.media.Media media = new javafx.scene.media.Media(tempMp3.toURI().toString());
                javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                player.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String toJsonString(String s) {
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private String getApiKey() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty("openai.api_key");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY manquante (variable d'environnement) ou propriété JVM -Dopenai.api_key");
        }
        return apiKey.trim();
    }
}