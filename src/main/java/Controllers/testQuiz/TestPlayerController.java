package Controllers.testQuiz;
// BACKEND - NE PAS MODIFIER POUR LE FRONT

import Entities.testQuiz.Proposition;
import Entities.testQuiz.Question;
import Entities.testQuiz.Test;
import Services.testQuiz.PropositionService;
import Services.testQuiz.QuestionService;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class TestPlayerController implements Initializable {

    @FXML
    private BorderPane rootPane;
    @FXML
    private Label titleLabel;
    @FXML
    private Label typeLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private ProgressBar timerProgressBar;
    @FXML
    private Label questionCountLabel;
    @FXML
    private ProgressBar questionProgressBar;
    @FXML
    private Label questionLabel;
    @FXML
    private VBox questionCard;
    @FXML
    private VBox qcmContainer;
    @FXML
    private TextArea logicAnswerArea;
    @FXML
    private Label scorePreviewLabel;
    @FXML
    private Button previousButton;
    @FXML
    private Button nextButton;
    @FXML
    private Label loadingLabel;

    private final QuestionService questionService = new QuestionService();
    private final PropositionService propositionService = new PropositionService();
    private final ToggleGroup toggleGroup = new ToggleGroup();

    private final Map<Integer, List<Proposition>> propositionsByQuestion = new HashMap<>();
    private final Map<Integer, Integer> selectedProposition = new HashMap<>();
    private final Map<Integer, String> textAnswers = new HashMap<>();

    private Test test;
    private Integer candidatId;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int remainingSeconds = 0;
    private int initialDurationSeconds = 0;
    private int previewScore = 0;
    private Timeline timer;
    private boolean finished = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        toggleGroup.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if (isQcm()) {
                nextButton.setDisable(newV == null);
            }
        });
    }

    public void setContext(Test test, Integer candidatId) {
        this.test = test;
        this.candidatId = candidatId;
        titleLabel.setText(test.getTitre());
        typeLabel.setText(test.getType() == null ? "N/A" : test.getType().toUpperCase(Locale.ROOT));
        loadQuestions();
    }

    @FXML
    private void handleQuit() {
        stopTimer();
        goToList();
    }

    @FXML
    private void handlePrevious() {
        if (currentIndex <= 0 || questions.isEmpty()) {
            return;
        }
        saveCurrentAnswer();
        currentIndex--;
        showCurrentQuestion(true);
    }

    @FXML
    private void handleNext() {
        if (questions.isEmpty() || finished) {
            return;
        }

        saveCurrentAnswer();
        updatePreviewScore();

        if (currentIndex >= questions.size() - 1) {
            finishTest();
            return;
        }
        currentIndex++;
        showCurrentQuestion(true);
    }

    private void loadQuestions() {
        loadingLabel.setVisible(true);
        loadingLabel.setManaged(true);
        nextButton.setDisable(true);
        previousButton.setDisable(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                questions = questionService.getByTestId(test.getId());
                if (isQcm()) {
                    for (Question question : questions) {
                        propositionsByQuestion.put(question.getId(), propositionService.getByQuestionId(question.getId()));
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            if (questions == null || questions.isEmpty()) {
                goToList();
                return;
            }
            currentIndex = 0;
            initialDurationSeconds = Math.max(0, test.getDurationSeconds());
            remainingSeconds = initialDurationSeconds;
            updateTimerUi();
            startTimer();
            showCurrentQuestion(false);
        });

        task.setOnFailed(e -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            goToList();
        });

        Thread thread = new Thread(task, "test-player-questions");
        thread.setDaemon(true);
        thread.start();
    }

    private void showCurrentQuestion(boolean animate) {
        Question question = questions.get(currentIndex);
        questionCountLabel.setText("Question " + (currentIndex + 1) + "/" + questions.size());
        questionProgressBar.setProgress((double) (currentIndex + 1) / questions.size());
        questionLabel.setText(question.getQuestion() == null ? "" : question.getQuestion().trim());
        previousButton.setDisable(currentIndex == 0);
        nextButton.setText(currentIndex == questions.size() - 1 ? "Terminer" : "Suivant");

        if (isQcm()) {
            renderQcm(question);
        } else {
            renderLogic(question);
        }

        updatePreviewScore();

        if (animate) {
            FadeTransition fade = new FadeTransition(Duration.millis(180), questionCard);
            fade.setFromValue(0.4);
            fade.setToValue(1.0);
            Timeline slide = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(questionCard.translateXProperty(), 16)),
                    new KeyFrame(Duration.millis(180), new KeyValue(questionCard.translateXProperty(), 0))
            );
            new ParallelTransition(fade, slide).playFromStart();
        }
    }

    private void renderQcm(Question question) {
        qcmContainer.setVisible(true);
        qcmContainer.setManaged(true);
        logicAnswerArea.setVisible(false);
        logicAnswerArea.setManaged(false);
        qcmContainer.getChildren().clear();
        toggleGroup.selectToggle(null);

        List<Proposition> options = propositionsByQuestion.getOrDefault(question.getId(), List.of());
        for (Proposition option : options) {
            RadioButton rb = new RadioButton(option.getContenu());
            rb.getStyleClass().add("answer-choice");
            rb.setWrapText(true);
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(option);
            qcmContainer.getChildren().add(rb);
        }

        Integer selectedId = selectedProposition.get(question.getId());
        if (selectedId != null) {
            for (Toggle toggle : toggleGroup.getToggles()) {
                Proposition option = (Proposition) toggle.getUserData();
                if (option != null && option.getId() == selectedId) {
                    toggleGroup.selectToggle(toggle);
                    break;
                }
            }
        }
        nextButton.setDisable(toggleGroup.getSelectedToggle() == null);
    }

    private void renderLogic(Question question) {
        qcmContainer.setVisible(false);
        qcmContainer.setManaged(false);
        logicAnswerArea.setVisible(true);
        logicAnswerArea.setManaged(true);
        logicAnswerArea.setText(textAnswers.getOrDefault(question.getId(), ""));
        nextButton.setDisable(false);
    }

    private void saveCurrentAnswer() {
        Question question = questions.get(currentIndex);
        if (isQcm()) {
            Toggle selected = toggleGroup.getSelectedToggle();
            if (selected != null) {
                Proposition selectedOption = (Proposition) selected.getUserData();
                if (selectedOption != null) {
                    selectedProposition.put(question.getId(), selectedOption.getId());
                }
            }
            return;
        }
        String answer = logicAnswerArea.getText() == null ? "" : logicAnswerArea.getText().trim();
        textAnswers.put(question.getId(), answer);
    }

    private void updatePreviewScore() {
        previewScore = computeScore();
        scorePreviewLabel.setText("Score courant: " + previewScore + " / " + computeTotalPoints());
    }

    private void finishTest() {
        if (finished) {
            return;
        }
        finished = true;
        stopTimer();
        int score = computeScore();
        int total = computeTotalPoints();
        double ratio = total == 0 ? 0 : (double) score / total;
        openResultView(score, total, ratio);
    }

    private int computeScore() {
        int totalScore = 0;
        for (Question question : questions) {
            int points = Math.max(0, question.getPoints());
            if (isQcm()) {
                Integer selectedId = selectedProposition.get(question.getId());
                if (selectedId == null) {
                    continue;
                }
                for (Proposition option : propositionsByQuestion.getOrDefault(question.getId(), List.of())) {
                    if (option.getId() == selectedId && option.isEstCorrect()) {
                        totalScore += points;
                        break;
                    }
                }
            } else {
                String expected = normalize(question.getReponse());
                String provided = normalize(textAnswers.get(question.getId()));
                if (!expected.isEmpty() && expected.equals(provided)) {
                    totalScore += points;
                }
            }
        }
        return totalScore;
    }

    private int computeTotalPoints() {
        int total = 0;
        for (Question question : questions) {
            total += Math.max(0, question.getPoints());
        }
        return total;
    }

    private void startTimer() {
        stopTimer();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (finished) {
                return;
            }
            remainingSeconds = Math.max(0, remainingSeconds - 1);
            updateTimerUi();
            if (remainingSeconds == 0) {
                saveCurrentAnswer();
                updatePreviewScore();
                finishTest();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.playFromStart();
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    private void updateTimerUi() {
        int min = remainingSeconds / 60;
        int sec = remainingSeconds % 60;
        timerLabel.setText(String.format("%02d:%02d", min, sec));
        timerProgressBar.setProgress(initialDurationSeconds <= 0 ? 0 : (double) remainingSeconds / initialDurationSeconds);
    }

    private void openResultView(int score, int total, double ratio) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/testQuiz/fxml/result_view.fxml"));
            Parent view = loader.load();
            ResultController controller = loader.getController();
            controller.setResultData(test, candidatId, score, total, ratio);
            navigateWithFade(view);
        } catch (IOException e) {
            goToList();
        }
    }

    private void goToList() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/testQuiz/fxml/test_list.fxml"));
            navigateWithFade(view);
        } catch (IOException ignored) {
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

    private boolean isQcm() {
        return test != null && normalize(test.getType()).equals("qcm");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
