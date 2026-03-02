package Services.candidature;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.*;

/**
 * AudioRecordService
 * ──────────────────
 * Enregistre le microphone en WAV via Java Sound API (pas de dépendance externe).
 *
 * Utilisation :
 *   startRecording()  → lance l'enregistrement dans un thread séparé
 *   stopRecording()   → arrête et sauvegarde le fichier WAV
 *   getOutputPath()   → retourne le chemin du fichier WAV sauvegardé
 */
public class AudioRecordService {

    // ── Config audio ──────────────────────────────────────────────────────────
    private static final float  SAMPLE_RATE   = 16000.0f; // 16kHz (optimal pour Whisper STT)
    private static final int    SAMPLE_SIZE   = 16;        // 16 bits
    private static final int    CHANNELS      = 1;         // mono
    private static final boolean SIGNED       = true;
    private static final boolean BIG_ENDIAN   = false;

    // ── Chemin de sortie (dans le dossier du projet) ──────────────────────────
    private static final String OUTPUT_DIR  = "recordings";
    private static final String OUTPUT_FILE = "answer.wav";

    // ── État interne ──────────────────────────────────────────────────────────
    private TargetDataLine microphone;
    private Thread         recordingThread;
    private volatile boolean recording = false;
    private String         lastSavedPath;
    private String         lastError;

    // ── Démarrer l'enregistrement ─────────────────────────────────────────────
    public void startRecording() {
        if (recording) return;

        try {
            // Format audio
            AudioFormat format = new AudioFormat(
                SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, SIGNED, BIG_ENDIAN
            );

            // Ouvrir le microphone
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                lastError = "Microphone non supporté sur ce système.";
                System.err.println("[AudioRecordService] " + lastError);
                return;
            }

            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();

            recording = true;

            // Créer le dossier recordings s'il n'existe pas
            Path dir = Path.of(OUTPUT_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }

            // Lancer l'enregistrement dans un thread séparé
            recordingThread = new Thread(() -> captureAudio(format), "MicRecorderThread");
            recordingThread.setDaemon(true);
            recordingThread.start();

            System.out.println("[AudioRecordService] Enregistrement démarré...");

        } catch (Exception e) {
            lastError = e.getMessage();
            recording = false;
            System.err.println("[AudioRecordService] Erreur démarrage : " + e.getMessage());
        }
    }

    // ── Capture audio dans un thread ──────────────────────────────────────────
    private void captureAudio(AudioFormat format) {
        File outputFile = new File(OUTPUT_DIR + "/" + OUTPUT_FILE);

        try (AudioInputStream ais = new AudioInputStream(microphone)) {
            // AudioSystem.write bloque jusqu'à fermeture de la ligne
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outputFile);
            lastSavedPath = outputFile.getAbsolutePath();
            System.out.println("[AudioRecordService] Fichier sauvegardé : " + lastSavedPath);

        } catch (Exception e) {
            if (recording) { // ignorer l'erreur si on a volontairement arrêté
                lastError = e.getMessage();
                System.err.println("[AudioRecordService] Erreur capture : " + e.getMessage());
            }
        }
    }

    // ── Arrêter l'enregistrement ──────────────────────────────────────────────
    public String stopRecording() {
        if (!recording) return null;

        recording = false;

        if (microphone != null) {
            microphone.stop();
            microphone.close(); // déclenche la fin de AudioSystem.write → sauvegarde le WAV
        }

        // Attendre que le thread finisse d'écrire le fichier
        if (recordingThread != null) {
            try {
                recordingThread.join(3000); // max 3 secondes d'attente
            } catch (InterruptedException ignored) {}
        }

        System.out.println("[AudioRecordService] Enregistrement arrêté.");
        return lastSavedPath;
    }

    // ── Vérifications ─────────────────────────────────────────────────────────
    public boolean isRecording() {
        return recording;
    }

    public boolean isMicrophoneAvailable() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, SIGNED, BIG_ENDIAN);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            return AudioSystem.isLineSupported(info);
        } catch (Exception e) {
            return false;
        }
    }

    public String getLastSavedPath() { return lastSavedPath; }
    public String getLastError()     { return lastError; }
}
