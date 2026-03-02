package Services.candidature;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

/**
 * AvatarService
 * Gère l'avatar animé (idle / talking) via deux vidéos MP4.
 *
 * idle    → boucle en permanence (avatar au repos)
 * talking → joue une fois puis retour automatique en idle
 */
public class AvatarService {

    private final MediaView mediaView;

    private MediaPlayer idlePlayer;
    private MediaPlayer talkingPlayer;

    private static final String IDLE_PATH    = "/Images/avatar_idle.mp4";
    private static final String TALKING_PATH = "/Images/avatar_talking.mp4";

    // ── Constructeur ─────────────────────────────────────────────────────────
    public AvatarService(MediaView mediaView) {
        this.mediaView = mediaView;
        initPlayers();
    }

    // ── Initialisation des deux players ──────────────────────────────────────
    private void initPlayers() {
        try {
            // ── Player IDLE (boucle infinie) ──
            var idleUrl = getClass().getResource(IDLE_PATH);
            if (idleUrl == null) throw new RuntimeException("Fichier introuvable : " + IDLE_PATH);

            idlePlayer = new MediaPlayer(new Media(idleUrl.toExternalForm()));
            idlePlayer.setCycleCount(MediaPlayer.INDEFINITE); // boucle infinie
            idlePlayer.setVolume(0);                          // pas de son
            idlePlayer.setOnError(() -> {
                System.err.println("[AvatarService] Erreur idlePlayer: " + idlePlayer.getError());
            });
            idlePlayer.setOnReady(() -> {
                System.out.println("[AvatarService] idle ready");
            });

            // ── Player TALKING (joue une fois → retour idle) ──
            var talkingUrl = getClass().getResource(TALKING_PATH);
            if (talkingUrl == null) throw new RuntimeException("Fichier introuvable : " + TALKING_PATH);

            talkingPlayer = new MediaPlayer(new Media(talkingUrl.toExternalForm()));
            talkingPlayer.setCycleCount(MediaPlayer.INDEFINITE); // boucle pendant que le TTS parle
            talkingPlayer.setVolume(0);                          // pas de son (le son vient du TTS)

            talkingPlayer.setOnError(() -> {
                System.err.println("[AvatarService] Erreur talkingPlayer: " + talkingPlayer.getError());
            });
            talkingPlayer.setOnReady(() -> {
                System.out.println("[AvatarService] talking ready");
            });

            // Quand talking se termine → retour idle automatique
            talkingPlayer.setOnEndOfMedia(this::playIdle);

            System.out.println("[AvatarService] Players initialisés.");

        } catch (Exception e) {
            System.err.println("[AvatarService] Erreur init : " + e.getMessage());
        }
    }

    // ── Jouer IDLE ────────────────────────────────────────────────────────────
    public void playIdle() {
        if (idlePlayer == null) return;

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::playIdle);
            return;
        }

        // Arrêter talking si en cours
        if (talkingPlayer != null) {
            talkingPlayer.stop();
        }

        mediaView.setMediaPlayer(idlePlayer);
        idlePlayer.stop();
        idlePlayer.seek(Duration.ZERO);
        idlePlayer.play();

        System.out.println("[AvatarService] → idle");
    }

    // ── Jouer TALKING ─────────────────────────────────────────────────────────
    /**
     * Lance la vidéo talking.
     * @param looping true = boucle pendant que le TTS parle
     *                false = joue une fois puis retour idle
     */
    public void playTalking(boolean looping) {
        if (talkingPlayer == null) return;

        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> playTalking(looping));
            return;
        }

        // Arrêter idle
        if (idlePlayer != null) {
            idlePlayer.stop();
        }

        talkingPlayer.setCycleCount(looping ? MediaPlayer.INDEFINITE : 1);

        // Si joue une seule fois → retour idle à la fin
        if (!looping) {
            talkingPlayer.setOnEndOfMedia(this::playIdle);
        } else {
            talkingPlayer.setOnEndOfMedia(null);
        }

        mediaView.setMediaPlayer(talkingPlayer);
        talkingPlayer.stop();
        talkingPlayer.seek(Duration.ZERO);
        talkingPlayer.play();

        System.out.println("[AvatarService] → talking (looping=" + looping + ")");
    }

    // ── Arrêter talking manuellement (appelé quand TTS termine) ──────────────
    public void stopTalking() {
        if (talkingPlayer != null) {
            talkingPlayer.stop();
        }
        playIdle();
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    public void dispose() {
        if (idlePlayer    != null) idlePlayer.dispose();
        if (talkingPlayer != null) talkingPlayer.dispose();
        System.out.println("[AvatarService] Disposed.");
    }
}
