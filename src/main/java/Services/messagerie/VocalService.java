package Services.messagerie;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;

public class VocalService {

    private TargetDataLine microphone;
    private boolean enregistrement = false;
    private String dernierFichier;

    private static final AudioFormat FORMAT = new AudioFormat(
            44100, 16, 1, true, false
    );

    // ── Démarrer l'enregistrement ────────────────────
    public boolean demarrerEnregistrement() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT);
            if (!AudioSystem.isLineSupported(info)) {
                System.out.println("Microphone non supporté !");
                return false;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(FORMAT);
            microphone.start();
            enregistrement = true;

            // Générer nom fichier unique
            String fileName = "vocal_" + System.currentTimeMillis() + ".wav";
            Path uploadsDir = Paths.get("uploads");
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
            }
            dernierFichier = uploadsDir.resolve(fileName).toString();

            // Enregistrer dans un thread séparé
            new Thread(() -> {
                try {
                    AudioInputStream audioStream = new AudioInputStream(microphone);
                    AudioSystem.write(
                            audioStream,
                            AudioFileFormat.Type.WAVE,
                            new File(dernierFichier)
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Arrêter l'enregistrement ─────────────────────
    public String arreterEnregistrement() {
        if (microphone != null && enregistrement) {
            enregistrement = false;
            microphone.stop();
            microphone.close();
            return dernierFichier;
        }
        return null;
    }

    // ── Lire un vocal ────────────────────────────────
    public void lireVocal(String filePath) {
        new Thread(() -> {
            try {
                File audioFile = new File(filePath);
                if (!audioFile.exists()) {
                    System.out.println("Fichier audio introuvable : " + filePath);
                    return;
                }

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(info);

                speakers.open(format);
                speakers.start();

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    speakers.write(buffer, 0, bytesRead);
                }

                speakers.drain();
                speakers.close();
                audioStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean isEnregistrement() {
        return enregistrement;
    }
}