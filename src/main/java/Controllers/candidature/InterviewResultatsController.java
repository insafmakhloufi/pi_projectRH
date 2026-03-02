package Controllers.candidature;

import Entities.candidature.InterviewResultat;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;

/**
 * InterviewResultatsController
 * Affiche les résultats complets de l'entretien.
 * Reçoit la liste des résultats depuis InterviewController via setResultats().
 */
public class InterviewResultatsController {

    @FXML private Label       scoreGlobalLabel;
    @FXML private ProgressBar scoreGlobalBar;
    @FXML private VBox        resultsContainer;

    private List<InterviewResultat> resultats;

    // ── Initialisation avec les données ──────────────────────────────────────

    /**
     * Appelé depuis InterviewController pour passer les données.
     */
    public void setResultats(List<InterviewResultat> resultats) {
        this.resultats = resultats;
        afficherResultats();
    }

    // ── Affichage ─────────────────────────────────────────────────────────────

    private void afficherResultats() {
        if (resultats == null || resultats.isEmpty()) return;

        // Calcul score global (moyenne)
        int scoreMoyen = (int) resultats.stream()
                .mapToInt(InterviewResultat::getScore)
                .average()
                .orElse(0);

        // Afficher score global
        scoreGlobalLabel.setText(scoreMoyen + "");
        scoreGlobalLabel.setStyle(
            "-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " +
            getCouleurScore(scoreMoyen) + ";"
        );
        scoreGlobalBar.setProgress(scoreMoyen / 100.0);
        scoreGlobalBar.setStyle("-fx-accent: " + getCouleurScore(scoreMoyen) + ";");

        // Créer une carte pour chaque question
        for (InterviewResultat r : resultats) {
            resultsContainer.getChildren().add(creerCarteQuestion(r));
        }
    }

    // ── Créer une carte de résultat pour une question ─────────────────────────

    private VBox creerCarteQuestion(InterviewResultat r) {

        VBox carte = new VBox(12);
        carte.setStyle("""
                -fx-background-color: #161b22;
                -fx-border-color: #30363d;
                -fx-border-radius: 10;
                -fx-background-radius: 10;
                -fx-padding: 18;
                """);

        // ── En-tête : numéro + score ──
        HBox header = new HBox();
        header.setSpacing(10);

        Label numLabel = new Label("Question " + r.getNumeroQuestion());
        numLabel.setStyle("-fx-text-fill: #a0c4ff; -fx-font-size: 13px; -fx-font-weight: bold;");
        HBox.setHgrow(numLabel, Priority.ALWAYS);

        Label scoreLabel = new Label(r.getScore() + " / 100");
        scoreLabel.setStyle("-fx-text-fill: " + r.getCouleurScore() + "; " +
                            "-fx-font-size: 16px; -fx-font-weight: bold;");

        ProgressBar bar = new ProgressBar(r.getScore() / 100.0);
        bar.setPrefWidth(100);
        bar.setStyle("-fx-accent: " + r.getCouleurScore() + ";");

        header.getChildren().addAll(numLabel, bar, scoreLabel);

        // ── Question posée ──
        Label questionLabel = new Label("❓ " + r.getQuestion());
        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        // ── Réponse transcrite ──
        VBox reponseBox = creerSection("💬 Votre réponse",
                r.getReponseTranscrite() != null ? r.getReponseTranscrite() : "—",
                "#8b949e", "#1c2128");

        // ── Feedback général ──
        VBox feedbackBox = creerSection("📝 Analyse",
                r.getFeedback(), "#c9d1d9", "#161b22");

        // ── Points forts ──
        VBox fortsBox = creerSection("✅ Points forts",
                r.getPointsForts(), "#52b788", "#0d2818");

        // ── Points faibles ──
        VBox faiblesBox = creerSection("⚠️ Points à améliorer",
                r.getPointsFaibles(), "#ffd166", "#2a1f00");

        // ── Conseils ──
        VBox conseilsBox = creerSection("💡 Conseils",
                r.getConseils(), "#a0c4ff", "#0d1a2e");

        carte.getChildren().addAll(
                header, questionLabel,
                reponseBox, feedbackBox,
                fortsBox, faiblesBox, conseilsBox
        );

        return carte;
    }

    // ── Créer une section colorée (titre + texte) ─────────────────────────────

    private VBox creerSection(String titre, String contenu,
                               String couleurTitre, String bgColor) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: " + bgColor + "; " +
                     "-fx-background-radius: 6; -fx-padding: 10;");

        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-text-fill: " + couleurTitre + "; " +
                            "-fx-font-size: 12px; -fx-font-weight: bold;");

        Label contenuLabel = new Label(contenu);
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-text-fill: #c9d1d9; -fx-font-size: 13px;");

        box.getChildren().addAll(titreLabel, contenuLabel);
        return box;
    }

    // ── Couleur selon score ───────────────────────────────────────────────────

    private String getCouleurScore(int score) {
        if (score >= 70) return "#52b788";
        if (score >= 40) return "#ffd166";
        return "#ff4d6d";
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    @FXML
    private void onNouvelEntretien() {
        // Recharger la page entretien dans le même conteneur
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/candidaturefxml/interview.fxml")
            );
            javafx.scene.Parent view = loader.load();

            // Remonter jusqu'au conteneur parent
            javafx.scene.layout.Pane parent =
                (javafx.scene.layout.Pane) resultsContainer.getScene()
                    .getRoot().lookup("#contentArea");

            if (parent != null) {
                parent.getChildren().setAll(view);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onTelechargerPDF() {
        // TODO Étape 6 : générer le PDF
        System.out.println("[ResultatsController] PDF → étape 6");
    }
}
