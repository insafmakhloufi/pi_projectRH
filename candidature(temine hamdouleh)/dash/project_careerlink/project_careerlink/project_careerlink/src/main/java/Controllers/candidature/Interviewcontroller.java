package Controllers.candidature;


import Services.candidature.Cameraservice;
import Services.candidature.AvatarService;
import Services.candidature.AudioRecordService;
import Services.candidature.InterviewAnalyseService;
import Services.candidature.InterviewOpenAIService;
import Entities.candidature.InterviewResultat;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * InterviewController
 * Contrôleur principal de l'écran d'entretien IA.
 *
 * Étape 1 : Caméra live + UI de base
 * (Les services IA, STT, TTS seront ajoutés aux étapes suivantes)
 */
public class Interviewcontroller implements Initializable {

    // ── FXML Bindings ────────────────────────────────────────────────────────

    @FXML private ImageView cameraView;       // Affichage caméra
    @FXML private MediaView avatarMediaView;

    @FXML private Label  statusLabel;         // "Prêt / En cours / ..."
    @FXML private Label  questionLabel;       // Question posée par l'IA
    @FXML private Label  timerLabel;          // Compte à rebours réponse
    @FXML private TextArea transcriptArea;    // Transcription réponse candidat

    @FXML private Button btnStart;            // Démarrer l'entretien
    @FXML private Button btnRecord;           // Activer le micro
    @FXML private Button btnStop;             // Stopper l'enregistrement
    @FXML private Button btnReport;           // Générer le rapport PDF
    @FXML private Button btnFinish;           // Terminer l'entretien

    @FXML private Circle recordingDot;        // Point rouge clignotant
    @FXML private Label  recordingLabel;      // Label "ENREGISTREMENT"

    // ── Services ─────────────────────────────────────────────────────────────
    private Cameraservice cameraService;
    private AvatarService avatarService;
    private InterviewOpenAIService openAIService;
    private AudioRecordService audioRecordService;
    private InterviewAnalyseService analyseService;

    private final List<InterviewResultat> resultats = new ArrayList<>();
    private String currentQuestion = "";

    private MediaPlayer audioPlayer;
    private int questionNumber = 0;
    private static final int MAX_QUESTIONS = 5;

    private Pane navigationContainer;

    public void setNavigationContainer(Pane navigationContainer) {
        this.navigationContainer = navigationContainer;
    }

    // ── Timer réponse ────────────────────────────────────────────────────────
    private Timeline countdownTimeline;
    private int      remainingSeconds;
    private static final int ANSWER_TIME_SECONDS = 60; // 1 minute max pour répondre

    // ── Animation clignotement ────────────────────────────────────────────────
    private Timeline blinkTimeline;

    // ── Initialisation ───────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // Lier la caméra à l'ImageView
        cameraService = new Cameraservice(cameraView);

        // Configurer l'ImageView caméra (plein écran, miroir déjà géré par FFmpeg)
        cameraView.setPreserveRatio(false);

        // Démarrer la caméra immédiatement au chargement de l'écran
        startCameraPreview();

        avatarService = new AvatarService(avatarMediaView);
        avatarService.playIdle();

        openAIService = new InterviewOpenAIService();

        analyseService = new InterviewAnalyseService();

        audioRecordService = new AudioRecordService();
        if (!audioRecordService.isMicrophoneAvailable()) {
            updateStatus("⚠️ Microphone non détecté — vérifiez vos périphériques");
        }
    }

    // ── Caméra ───────────────────────────────────────────────────────────────

    /**
     * Démarre la preview caméra en arrière-plan.
     * Le candidat se voit dès l'ouverture de l'écran.
     */
    private void startCameraPreview() {
        new Thread(() -> {
            cameraService.startCamera();
        }, "CameraStartThread").start();

        updateStatus("📷 Caméra active — Prêt à démarrer l'entretien");

        // Vérification (au cas où FFmpeg est introuvable / caméra bloquée)
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (cameraService == null) return;
            if (!cameraService.isRunning()) {
                String err = cameraService.getLastError();
                updateStatus("⚠ Caméra indisponible");
                if (err != null && !err.isBlank()) {
                    new Alert(Alert.AlertType.ERROR,
                            "La caméra n'a pas démarré.\n\nCause: " + err +
                                    "\n\nSolution: place ffmpeg.exe dans l'un de ces chemins :\n" +
                                    "- <projet>/ffmpeg/bin/ffmpeg.exe\n" +
                                    "- C:/ffmpeg/bin/ffmpeg.exe\n\n" +
                                    "Ou définis la variable d'environnement FFMPEG_PATH vers ffmpeg.exe"
                    ).show();
                }
            }
        }));
        t.setCycleCount(1);
        t.play();
    }

    // ── Actions boutons ───────────────────────────────────────────────────────

    /**
     * Bouton "Démarrer l'entretien"
     * Pour l'instant : affiche un message placeholder.
     * → Étape 3 : appellera l'IA pour générer la première question.
     */
    @FXML
    private void onStartInterview() {
        btnStart.setDisable(true);
        btnRecord.setDisable(true);
        btnStop.setDisable(true);
        updateStatus("⏳ Génération de la question...");

        new Thread(() -> {
            try {
                questionNumber++;

                String question = openAIService.generateQuestion(questionNumber);
                String audioPath = openAIService.textToSpeech(question);

                Platform.runLater(() -> {
                    questionLabel.setText(question);
                    currentQuestion = question;
                    updateStatus("🤖 Question " + questionNumber + "/" + MAX_QUESTIONS);

                    FadeTransition ft = new FadeTransition(Duration.millis(600), questionLabel);
                    ft.setFromValue(0);
                    ft.setToValue(1);
                    ft.play();

                    playQuestionAudio(audioPath);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    questionLabel.setText("Pouvez-vous vous présenter et expliquer votre parcours ?");
                    updateStatus("⚠️ IA indisponible — question de secours");
                    btnRecord.setDisable(false);
                    System.err.println("[Interviewcontroller] Erreur IA : " + e.getMessage());
                });
            }
        }, "AIQuestionThread").start();
    }

    private void playQuestionAudio(String audioPath) {
        try {
            if (audioPlayer != null) {
                audioPlayer.stop();
                audioPlayer.dispose();
                audioPlayer = null;
            }

            Media media = new Media(Path.of(audioPath).toUri().toString());
            audioPlayer = new MediaPlayer(media);

            audioPlayer.setOnError(() -> {
                System.err.println("[Interviewcontroller] MediaPlayer error: " + audioPlayer.getError());
                btnRecord.setDisable(false);
                if (avatarService != null) avatarService.playIdle();
            });

            if (avatarService != null) {
                avatarService.playTalking(true);
            }

            audioPlayer.setOnEndOfMedia(() -> {
                if (avatarService != null) {
                    avatarService.stopTalking();
                }
                btnRecord.setDisable(false);
                updateStatus("🎤 À vous ! Cliquez sur Répondre quand vous êtes prêt");
            });

            audioPlayer.play();
            System.out.println("[Interviewcontroller] Audio question en cours...");

        } catch (Exception e) {
            System.err.println("[Interviewcontroller] Erreur audio : " + e.getMessage());
            btnRecord.setDisable(false);
            if (avatarService != null) avatarService.playIdle();
        }
    }

    /**
     * Bouton "🎤 Répondre" — Lance l'enregistrement audio
     * → Étape 4 : déclenchera AudioRecordService
     */
    @FXML
    private void onRecord() {
        btnRecord.setDisable(true);
        btnStop.setDisable(false);

        if (audioRecordService == null) {
            updateStatus("⚠️ Service microphone indisponible");
            btnRecord.setDisable(false);
            btnStop.setDisable(true);
            return;
        }

        audioRecordService.startRecording();

        if (!audioRecordService.isRecording()) {
            updateStatus("⚠️ Impossible d'accéder au microphone");
            btnRecord.setDisable(false);
            btnStop.setDisable(true);
            return;
        }

        updateStatus("🎙️ Enregistrement en cours... Parlez maintenant !");
        showRecordingIndicator(true);
        startCountdown();

        System.out.println("[Interviewcontroller] Enregistrement micro démarré");
    }

    /**
     * Bouton "⏹ Stop" — Stoppe l'enregistrement
     */
    @FXML
    private void onStopRecording() {
        stopRecordingUI();

        String wavPath = (audioRecordService != null) ? audioRecordService.stopRecording() : null;

        if (wavPath == null) {
            String err = (audioRecordService != null) ? audioRecordService.getLastError() : "Service micro indisponible";
            updateStatus("⚠️ Erreur enregistrement : " + err);
            transcriptArea.setText("Erreur lors de l'enregistrement.");
            btnRecord.setDisable(false);
            return;
        }

        updateStatus("⏳ Transcription et analyse en cours...");
        transcriptArea.setText("⏳ Analyse de votre réponse...");

        final String questionActuelle = currentQuestion;
        final int numQuestion = questionNumber;

        new Thread(() -> {
            try {
                if (analyseService == null) {
                    throw new IllegalStateException("AnalyseService indisponible");
                }

                String texte = analyseService.transcribe(wavPath);

                InterviewResultat resultat = new InterviewResultat(numQuestion, questionActuelle);
                resultat.setReponseTranscrite(texte);

                analyseService.analyseReponse(resultat);
                resultats.add(resultat);

                Platform.runLater(() -> {
                    transcriptArea.setText("✅ " + texte);

                    String fb = resultat.getFeedback();
                    if (fb == null) fb = "";
                    String fbPreview = fb.substring(0, Math.min(60, fb.length()));
                    updateStatus("📊 Score : " + resultat.getScore() + "/100 — " + fbPreview + (fb.length() > 60 ? "..." : ""));

                    if (questionNumber >= MAX_QUESTIONS) {
                        btnReport.setDisable(false);
                        updateStatus("🏁 Entretien terminé ! Consultez vos résultats.");
                        afficherPageResultats();
                    } else {
                        btnStart.setDisable(false);
                        btnStart.setText("▶  Question suivante (" + (questionNumber + 1) + "/" + MAX_QUESTIONS + ")");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    transcriptArea.setText("⚠️ Erreur analyse : " + e.getMessage());
                    updateStatus("⚠️ Erreur — réessayez");
                    btnRecord.setDisable(false);
                    System.err.println("[Interviewcontroller] Erreur étape 5 : " + e.getMessage());
                });
            }
        }, "AnalyseThread").start();

        System.out.println("[Interviewcontroller] Enregistrement arrêté");
    }

    private void afficherPageResultats() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/candidaturefxml/interview_resultats.fxml")
            );
            Parent view = loader.load();

            InterviewResultatsController ctrl = loader.getController();
            ctrl.setResultats(resultats);

            cleanup();

            if (navigationContainer != null) {
                navigationContainer.getChildren().setAll(view);
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Entretien terminé. (Conteneur navigation introuvable)").show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur affichage résultats : " + e.getMessage()).show();
        }
    }

    /**
     * Bouton "📄 Rapport PDF"
     * → Étape 6 : appellera ReportService
     */
    @FXML
    private void onGenerateReport() {
        updateStatus("📄 Génération du rapport PDF... (à intégrer étape 6)");
        System.out.println("[Controller] Génération rapport PDF");
        // TODO Étape 6 : reportService.generatePdf(session);
    }

    @FXML
    private void onFinishInterview() {
        try {
            cleanup();
        } catch (Exception ignored) {
        }

        if (navigationContainer == null) {
            new Alert(Alert.AlertType.INFORMATION, "Entretien terminé.").show();
            return;
        }

        try {
            var url = getClass().getResource("/candidaturefxml/MesCandidatures.fxml");
            if (url == null) throw new RuntimeException("MesCandidatures.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            navigationContainer.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de retourner à Mes candidatures.").show();
        }
    }

    // ── Timer compte à rebours ────────────────────────────────────────────────

    private void startCountdown() {
        remainingSeconds = ANSWER_TIME_SECONDS;
        timerLabel.setText("⏱ " + remainingSeconds + "s");

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            remainingSeconds--;
            timerLabel.setText("⏱ " + remainingSeconds + "s");

            // Alerte rouge quand il reste 10 secondes
            if (remainingSeconds <= 10) {
                timerLabel.setStyle("-fx-text-fill: #ff4d6d; -fx-font-size: 14px; -fx-font-weight: bold;");
            }

            if (remainingSeconds <= 0) {
                stopRecordingUI();
                updateStatus("⏰ Temps écoulé — Analyse en cours...");
            }
        }));
        countdownTimeline.setCycleCount(ANSWER_TIME_SECONDS);
        countdownTimeline.play();
    }

    private void stopRecordingUI() {
        if (countdownTimeline != null) countdownTimeline.stop();

        btnStop.setDisable(true);
        btnRecord.setDisable(false);
        btnReport.setDisable(false);

        timerLabel.setText("");
        timerLabel.setStyle("-fx-text-fill: #ffd166; -fx-font-size: 14px; -fx-font-weight: bold;");

        showRecordingIndicator(false);
    }

    // ── Indicateur rouge clignotant ───────────────────────────────────────────

    private void showRecordingIndicator(boolean show) {
        recordingDot.setVisible(show);
        recordingLabel.setVisible(show);

        if (show) {
            // Animation clignotement
            blinkTimeline = new Timeline(
                    new KeyFrame(Duration.seconds(0.5), e -> recordingDot.setVisible(!recordingDot.isVisible()))
            );
            blinkTimeline.setCycleCount(Timeline.INDEFINITE);
            blinkTimeline.play();
        } else {
            if (blinkTimeline != null) blinkTimeline.stop();
            recordingDot.setVisible(false);
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    /**
     * Appelé quand la scène se ferme — important pour libérer la caméra !
     */
    public void cleanup() {
        if (audioRecordService != null && audioRecordService.isRecording()) {
            try {
                audioRecordService.stopRecording();
            } catch (Exception ignored) {
            }
        }

        if (audioPlayer != null) {
            try {
                audioPlayer.stop();
                audioPlayer.dispose();
            } catch (Exception ignored) {
            }
            audioPlayer = null;
        }

        if (cameraService != null) {
            cameraService.stopCamera();
        }

        if (avatarService != null) {
            avatarService.dispose();
        }

        if (countdownTimeline != null) countdownTimeline.stop();
        if (blinkTimeline != null) blinkTimeline.stop();
    }
}
