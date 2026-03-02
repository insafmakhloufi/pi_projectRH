package Services.candidature;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.nio.file.*;
import java.util.Base64;
import javax.sound.sampled.*;

public class CopiloteVocalService {

    private static final String API_KEY = System.getenv("OPENAI_API_KEY");
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
            throw new RuntimeException("Microphone non supportÃ© !");
        }

        microLine = (TargetDataLine) AudioSystem.getLine(info);
        microLine.open(format);
        microLine.start();

        audioFile = File.createTempFile("copilote_audio", ".wav");

        // Enregistrement dans un thread sÃ©parÃ©
        new Thread(() -> {
            try (AudioInputStream ais = new AudioInputStream(microLine)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("ðŸŽ¤ Enregistrement dÃ©marrÃ©...");
    }

    public File arreterEnregistrement() throws Exception {
        if (microLine != null) {
            microLine.stop();
            microLine.close();
            System.out.println("ðŸŽ¤ Enregistrement arrÃªtÃ© : " + audioFile.getAbsolutePath());
        }
        return audioFile;
    }

    // ============ WHISPER : Audio â†’ Texte ============

    public String transcrireAudio(File audioFile) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

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

        // Partie modÃ¨le
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
                .header("Authorization", "Bearer " + API_KEY)
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

    // ============ GPT : Texte â†’ Action JSON ============

    public String comprendreCommande(String texte, String contexte) throws Exception {
        String systemPrompt = """
    Tu es un copilote vocal pour CareerLink, une application de gestion de candidatures.
    Tu reÃ§ois une commande en franÃ§ais et tu retournes UNIQUEMENT un JSON.
    
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
    - {"action": "FILL_FIELD", "field": "taMotivationLetter", "value": "Je suis motivÃ©..."}
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
    
    RÃ¨gles:
    - "navigue vers", "aller Ã ", "ouvre", "va sur" â†’ NAVIGATE
    - "Ã©tape suivante", "suivant", "continuer", "next" â†’ NEXT_STEP
    - "Ã©tape prÃ©cÃ©dente", "retour", "prÃ©cÃ©dent" â†’ PREV_STEP
    - "soumettre", "envoyer", "submit" â†’ SUBMIT_FORM
    - "dans le champ prÃ©nom Ã©cris X" â†’ FILL_FIELD tfPrenom
    - "dans le champ nom Ã©cris X" â†’ FILL_FIELD tfNom
    - "dans le champ email Ã©cris X" â†’ FILL_FIELD tfEmail
    - "dans le champ adresse Ã©cris X" â†’ FILL_FIELD tfAdresse
    - "dans le champ ville Ã©cris X" â†’ FILL_FIELD tfVille
    - "dans le champ tÃ©lÃ©phone Ã©cris X" â†’ FILL_FIELD tfTelLocal
    - "dans le champ compÃ©tences Ã©cris X" â†’ FILL_FIELD tfSkills
    - "dans le champ lettre de motivation Ã©cris X" â†’ FILL_FIELD taMotivationLetter
    - "date de naissance X" â†’ FILL_DATE dpNaissance
    - "mon diplÃ´me est bachelor", "j'ai un bachelor" â†’ SELECT_RADIO rbBachelor
    - "mon diplÃ´me est engineering" â†’ SELECT_RADIO rbEngineering
    - "mon diplÃ´me est master", "j'ai un master" â†’ SELECT_RADIO rbMasters
    - "mon diplÃ´me est doctorat", "j'ai un phd" â†’ SELECT_RADIO rbPhd
    - "mon Ã©tablissement est X" â†’ SELECT_COMBO cbInstitution
    - "indicatif X" â†’ SELECT_COMBO cbIndicatif
    - "commencer enregistrement", "start video", "enregistrer" â†’ START_VIDEO
    - "arrÃªter enregistrement", "stop video" â†’ STOP_VIDEO
    - "je confirme les informations" â†’ CHECK_CONFIRM
    - "j'accepte la politique" â†’ CHECK_PRIVACY
    - "recherche X", "cherche X" â†’ SEARCH
    
    Contexte actuel: """ + contexte + """
    
    RÃ©ponds UNIQUEMENT avec le JSON, rien d'autre.
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GPT_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
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

    // ============ TTS : Texte â†’ Voix ============

    public void parler(String texte) throws Exception {
        String body = """
            {
              "model": "tts-1",
              "input": %s,
              "voice": "nova"
            }
            """.formatted(toJsonString(texte));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TTS_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .POST(BodyPublishers.ofString(body))
                .build();

        HttpResponse<byte[]> response = client.send(request,
                HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Erreur TTS: " + response.statusCode());
        }

        // Jouer l'audio mp3 reÃ§u
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
}
