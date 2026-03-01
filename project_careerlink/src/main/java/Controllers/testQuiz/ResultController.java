package Controllers.testQuiz;
// BACKEND - NE PAS MODIFIER POUR LE FRONT

import Entities.testQuiz.Test;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;

public class ResultController {

    @FXML
    private BorderPane rootPane;
    @FXML
    private VBox resultCard;
    @FXML
    private Label titleLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label ratioLabel;
    @FXML
    private ProgressBar resultProgressBar;
    @FXML
    private Button retryButton;

    private Test test;
    private Integer candidatId;

    public void setResultData(Test test, Integer candidatId, int score, int total, double ratio) {
        this.test = test;
        this.candidatId = candidatId;

        titleLabel.setText(test.getTitre());
        scoreLabel.setText(score + " / " + total);
        ratioLabel.setText((int) Math.round(ratio * 100) + "%");
        messageLabel.setText(messageByRatio(ratio));
        resultProgressBar.setProgress(0);
        retryButton.setDisable(test == null);

        FadeTransition fade = new FadeTransition(Duration.millis(300), resultCard);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), resultCard);
        scale.setFromX(0.95);
        scale.setFromY(0.95);
        scale.setToX(1.0);
        scale.setToY(1.0);

        Timeline progressAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(resultProgressBar.progressProperty(), 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(resultProgressBar.progressProperty(), ratio))
        );

        Platform.runLater(() -> {
            new ParallelTransition(fade, scale).playFromStart();
            progressAnim.playFromStart();
        });
    }

    @FXML
    private void handleBackToList() {
        navigateTo("/testQuiz/fxml/test_list.fxml", null);
    }

    @FXML
    private void handleRetry() {
        if (test == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/test_player.fxml"));
            Parent view = loader.load();
            TestPlayerController controller = loader.getController();
            controller.setContext(test, candidatId);
            navigateWithFade(view);
        } catch (IOException ignored) {
        }
    }

    private void navigateTo(String path, Object ignored) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource(path));
            navigateWithFade(view);
        } catch (IOException ignoredException) {
        }
    }

    private void navigateWithFade(Parent view) {
        if (!(rootPane.getParent() instanceof StackPane parent)) {
            return;
        }
        parent.getChildren().setAll(view);
        FadeTransition fade = new FadeTransition(Duration.millis(220), view);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.playFromStart();
    }

    private String messageByRatio(double ratio) {
        if (ratio >= 0.85) {
            return "Excellent";
        }
        if (ratio >= 0.6) {
            return "Bien";
        }
        return "A ameliorer";
    }
}
