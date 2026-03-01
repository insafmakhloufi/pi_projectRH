package Controllers.testQuizFront;

import Entities.testQuiz.Test;
import Utils.front.FrontDifficultyUtil;
import Utils.front.FrontNavigator;
import Utils.front.FrontTestQuizAdapter;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FrontTestsListController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private FlowPane cardsFlow;
    @FXML
    private Label emptyLabel;

    private final FrontTestQuizAdapter adapter = new FrontTestQuizAdapter();
    private final List<Test> allTests = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        searchField.textProperty().addListener((obs, o, n) -> renderFiltered());
        loadTests();
    }

    @FXML
    private void handleRefresh() {
        loadTests();
    }

    private void loadTests() {
        Task<List<Test>> task = new Task<>() {
            @Override
            protected List<Test> call() {
                return adapter.loadTests();
            }
        };

        task.setOnSucceeded(e -> {
            allTests.clear();
            List<Test> loaded = task.getValue() == null ? List.of() : task.getValue();
            loaded.sort(Comparator.comparingInt(Test::getId).reversed());
            allTests.addAll(loaded);
            renderFiltered();
        });
        task.setOnFailed(e -> {
            allTests.clear();
            renderFiltered();
        });

        Thread t = new Thread(task, "front-tests-loader");
        t.setDaemon(true);
        t.start();
    }

    private void renderFiltered() {
        String q = normalize(searchField.getText());
        List<Test> filtered = allTests.stream()
                .filter(t -> q.isBlank()
                        || normalize(t.getTitre()).contains(q)
                        || normalize(t.getType()).contains(q))
                .collect(Collectors.toList());

        cardsFlow.getChildren().clear();
        emptyLabel.setVisible(filtered.isEmpty());
        emptyLabel.setManaged(filtered.isEmpty());
        if (filtered.isEmpty()) return;

        for (int i = 0; i < filtered.size(); i++) {
            VBox card = buildCard(filtered.get(i));
            cardsFlow.getChildren().add(card);
            playCardEnter(card, i);
        }
    }

    private VBox buildCard(Test test) {
        Label icon = new Label();
        icon.setGraphic(icon("M12 2 2 7l10 5 10-5-10-5zm0 7-10-5v13l10 5 10-5V4l-10 5z"));
        icon.getStyleClass().add("card-small-icon");

        Label title = new Label(safe(test.getTitre()));
        title.getStyleClass().add("test-card-title-front");
        title.setWrapText(true);
        title.setMaxWidth(260);

        Label type = chip(safe(test.getType()).toUpperCase(Locale.ROOT), "chip-type");
        Label diff = chip(FrontDifficultyUtil.difficultyOf(test), "chip-difficulty");
        HBox chips = new HBox(8, type, diff);

        Label metaA = new Label("Duree: " + Math.max(0, test.getDurationSeconds()) + "s");
        metaA.getStyleClass().add("test-card-meta-front");
        Label metaB = new Label("Points: " + Math.max(0, test.getTotalPoints()));
        metaB.getStyleClass().add("test-card-meta-front");

        Button start = new Button("Demarrer");
        start.getStyleClass().add("btn-primary-front");
        start.setOnAction(e -> FrontNavigator.showQuiz(test));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(10, icon, spacer, chips);
        top.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, top, title, metaA, metaB, start);
        card.getStyleClass().add("test-card-front");
        card.setPadding(new Insets(14));
        card.setPrefWidth(320);
        installHover(card);
        return card;
    }

    private Label chip(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().addAll("chip-front", style);
        return l;
    }

    private void playCardEnter(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(18);
        FadeTransition fade = new FadeTransition(Duration.millis(260), card);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setDelay(Duration.millis(index * 70L));
        TranslateTransition slide = new TranslateTransition(Duration.millis(280), card);
        slide.setFromY(18);
        slide.setToY(0);
        slide.setDelay(Duration.millis(index * 70L));
        fade.play();
        slide.play();
    }

    private void installHover(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(140), card);
            s.setToX(1.02);
            s.setToY(1.02);
            s.play();
            TranslateTransition up = new TranslateTransition(Duration.millis(140), card);
            up.setToY(-3);
            up.play();
        });
        card.setOnMouseExited(e -> {
            ScaleTransition s = new ScaleTransition(Duration.millis(140), card);
            s.setToX(1);
            s.setToY(1);
            s.play();
            TranslateTransition down = new TranslateTransition(Duration.millis(140), card);
            down.setToY(0);
            down.play();
        });
    }

    private SVGPath icon(String path) {
        SVGPath p = new SVGPath();
        p.setContent(path);
        p.getStyleClass().add("svg-icon-front");
        return p;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
