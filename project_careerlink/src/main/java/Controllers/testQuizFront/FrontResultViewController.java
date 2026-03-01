package Controllers.testQuizFront;

import Entites.Formation.Formation;
import Services.Formation.AiFormationRecommenderService;
import Services.Formation.FormationServices;
import Utils.UiState;
import Utils.WindowUtil;
import Utils.front.FrontNavigator;
import Utils.front.FrontQuizResult;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class FrontResultViewController implements Initializable {

    @FXML
    private Label titleLabel;
    @FXML
    private Label percentLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private Label wrongLabel;
    @FXML
    private Label unansweredLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Arc scoreArc;
    @FXML
    private VBox recoSection;
    @FXML
    private Label recoLoadingLabel;
    @FXML
    private FlowPane recoCardsFlow;

    private FrontQuizResult result;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        scoreArc.setLength(0);
    }

    public void setResult(FrontQuizResult result) {
        this.result = result;
        titleLabel.setText("Resultat - " + result.getTest().getTitre());
        messageLabel.setText(result.gradeMessage());
        animateRingAndNumbers();
        maybeLoadRecommendations();
    }

    @FXML
    private void handleReplay() {
        FrontNavigator.showQuiz(result.getTest());
    }

    @FXML
    private void handleBackToTests() {
        FrontNavigator.showTestsList();
    }

    private void animateRingAndNumbers() {
        int targetPercent = result.percent();
        int targetScore = result.getScore();
        int maxScore = result.getMaxScore();

        Timeline ring = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scoreArc.lengthProperty(), 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(scoreArc.lengthProperty(), -360.0 * targetPercent / 100.0))
        );
        ring.play();

        Timeline numbers = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    percentLabel.setText("0%");
                    scoreLabel.setText("0 / " + maxScore);
                }),
                new KeyFrame(Duration.millis(900), e -> {
                    percentLabel.setText(targetPercent + "%");
                    scoreLabel.setText(targetScore + " / " + maxScore);
                })
        );
        numbers.play();

        correctLabel.setText(String.valueOf(result.getCorrect()));
        wrongLabel.setText(String.valueOf(result.getWrong()));
        unansweredLabel.setText(String.valueOf(result.getUnanswered()));
        timeLabel.setText(formatTime(result.getTimeSpentSeconds()));
    }

    private String formatTime(int seconds) {
        int s = Math.max(0, seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void maybeLoadRecommendations() {
        if (result == null) {
            return;
        }

        boolean needsRecommendation = result.getScore() < (result.getMaxScore() / 2.0);
        if (!needsRecommendation) {
            setVisibleManaged(recoSection, false);
            return;
        }

        setVisibleManaged(recoSection, true);
        setVisibleManaged(recoLoadingLabel, true);
        if (recoCardsFlow != null) {
            recoCardsFlow.getChildren().clear();
        }

        Task<List<Formation>> task = new Task<>() {
            @Override
            protected List<Formation> call() {
                List<Formation> all = new FormationServices().afficherFormations();
                return new AiFormationRecommenderService().recommend(
                        result.getTest() == null ? "" : result.getTest().getTitre(),
                        result.getTest() == null ? "" : result.getTest().getType(),
                        result.getScore(),
                        result.getMaxScore(),
                        all
                );
            }
        };

        task.setOnSucceeded(e -> {
            setVisibleManaged(recoLoadingLabel, false);
            List<Formation> recos = task.getValue();
            if (recoCardsFlow == null || recos == null || recos.isEmpty()) {
                return;
            }
            for (Formation f : recos) {
                recoCardsFlow.getChildren().add(createRecoCard(f, recoCardsFlow));
            }
        });

        task.setOnFailed(e -> setVisibleManaged(recoLoadingLabel, false));

        Thread t = new Thread(task, "ai-formation-reco");
        t.setDaemon(true);
        t.start();
    }

    private VBox createRecoCard(Formation f, Node anyNode) {
        VBox card = new VBox(10);
        card.getStyleClass().add("formation-card");

        Region cover = new Region();
        cover.getStyleClass().add("formation-cover");
        cover.setPrefHeight(100);

        Label code = new Label(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "");
        code.getStyleClass().add("formation-code");

        Label title = new Label(f.getTitre() == null ? "" : f.getTitre());
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);

        String statusText = f.isPayante() ? "Payante - " + f.getPrix() + " DT" : "Ouvert";
        Label status = new Label(statusText);
        status.getStyleClass().add("formation-status");

        VBox body = new VBox(6, code, title, status);
        body.getStyleClass().add("formation-body");

        Region divider = new Region();
        divider.getStyleClass().add("formation-divider");

        String lieu = f.getLieu() == null ? "" : f.getLieu();
        String date = f.getDateDebut() == null ? "-" : f.getDateDebut().toString();
        Label meta = new Label(lieu + " - " + date);
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button fav = new Button("-> Voir");
        fav.getStyleClass().add("formation-fav");
        fav.setFocusTraversable(false);

        HBox footer = new HBox(10, meta, spacer, fav);
        footer.getStyleClass().add("formation-footer");

        card.getChildren().addAll(cover, body, divider, footer);
        card.setPrefWidth(300);
        card.setMaxWidth(300);

        card.setOnMouseClicked(e -> openRecoFormation(f, anyNode));
        fav.setOnAction(e -> openRecoFormation(f, anyNode));

        return card;
    }

    private void openRecoFormation(Formation f, Node anyNode) {
        UiState.selectedFormationId = f.getId();
        UiState.selectedFormationTitre = f.getTitre();
        WindowUtil.navigate(anyNode, "/Formation/CourFront.fxml", "Cours de la formation");
    }

    private void setVisibleManaged(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
