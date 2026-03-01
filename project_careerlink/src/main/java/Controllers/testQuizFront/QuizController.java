package Controllers.testQuizFront;

import Entities.testQuiz.Test;
import Services.testQuiz.AiCodeResult;
import Services.testQuiz.AiCodeReviewService;
import Utils.front.FrontNavigator;
import Utils.front.FrontQuizQuestion;
import Utils.front.FrontQuizResult;
import Utils.front.FrontTestQuizAdapter;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.util.Duration;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

public class QuizController implements Initializable {

    @FXML
    private BorderPane quizRoot;
    @FXML
    private Label quizTitleLabel;
    @FXML
    private Label progressLabel;
    @FXML
    private Label questionBadge;
    @FXML
    private Label scoreLabel;
    @FXML
    private Label okLabel;
    @FXML
    private Label koLabel;
    @FXML
    private Label timerLabel;
    @FXML
    private Label arcLabel;
    @FXML
    private Arc progressArc;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private VBox questionCard;
    @FXML
    private Label questionLabel;
    @FXML
    private Button optionAButton;
    @FXML
    private Button optionBButton;
    @FXML
    private Button optionCButton;
    @FXML
    private Button optionDButton;
    @FXML
    private GridPane optionsGrid;
    @FXML
    private TextField openAnswerField;
    @FXML
    private VBox codingBlock;
    @FXML
    private TextArea codeEditorArea;
    @FXML
    private Label codingLangLabel;
    @FXML
    private HBox aiFeedbackBox;
    @FXML
    private Label aiFeedbackLabel;
    @FXML
    private Button btnSkip;
    @FXML
    private Button btnNext;

    private final FrontTestQuizAdapter adapter = new FrontTestQuizAdapter();
    private List<Button> optionButtons;

    private Test test;
    private List<FrontQuizQuestion> questions = List.of();
    private Integer[] selectedAnswers = new Integer[0];
    private String[] openAnswers = new String[0];
    private boolean[] answered = new boolean[0];
    private final Deque<Integer> skippedQueue = new ArrayDeque<>();
    private final Set<Integer> skippedSet = new HashSet<>();

    private int currentIndex = 0;
    private int remainingSeconds = 0;
    private int totalDuration = 0;
    private Timeline timer;
    private boolean finished = false;
    private int score = 0;
    private int correct = 0;
    private int wrong = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        optionButtons = Arrays.asList(optionAButton, optionBButton, optionCButton, optionDButton);
        installGlobalMouseDebug();
        // Ne PAS mettre pickOnBounds=false sur questionCard — ça bloquait les clics sur les boutons enfants
        optionAButton.setPickOnBounds(true);
        optionBButton.setPickOnBounds(true);
        optionCButton.setPickOnBounds(true);
        optionDButton.setPickOnBounds(true);
        btnSkip.setPickOnBounds(true);
        btnNext.setPickOnBounds(true);
        btnNext.setDisable(true);
        btnSkip.setDisable(false);
        installSkipNextMouseDebug();
        openAnswerField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (questions != null && !questions.isEmpty() && currentIndex >= 0 && currentIndex < questions.size()) {
                FrontQuizQuestion q = questions.get(currentIndex);
                if (q.isOpenQuestion() && !answered[currentIndex]) {
                    btnNext.setDisable(safe(newVal).trim().isBlank());
                }
            }
        });
        codeEditorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (questions != null && !questions.isEmpty() && currentIndex >= 0 && currentIndex < questions.size()) {
                FrontQuizQuestion q = questions.get(currentIndex);
                if (q.isCodingQuestion() && !answered[currentIndex]) {
                    btnNext.setDisable(safe(newVal).trim().isBlank());
                }
            }
        });
        refreshScoreLabel();
    }

    public void setTest(Test test) {
        this.test = test;
        this.questions = adapter.loadQuizQuestions(test);
        this.selectedAnswers = new Integer[questions.size()];
        this.openAnswers = new String[questions.size()];
        this.answered = new boolean[questions.size()];
        this.skippedQueue.clear();
        this.skippedSet.clear();
        this.currentIndex = 0;
        this.finished = false;
        this.score = 0;
        this.correct = 0;
        this.wrong = 0;
        this.totalDuration = adapter.inferDurationSeconds(test);
        this.remainingSeconds = totalDuration;

        quizTitleLabel.setText("Quiz: " + safe(test.getTitre()));
        refreshScoreLabel();
        updateTimer();

        if (questions.isEmpty()) {
            showEmpty();
            return;
        }
        renderQuestion(false);
        startTimer();
    }

    @FXML
    public void handleOptionA(ActionEvent e) {
        System.out.println("[QUIZ] click A");
        selectOption(0, optionAButton);
    }

    @FXML
    public void handleOptionB(ActionEvent e) {
        System.out.println("[QUIZ] click B");
        selectOption(1, optionBButton);
    }

    @FXML
    public void handleOptionC(ActionEvent e) {
        System.out.println("[QUIZ] click C");
        selectOption(2, optionCButton);
    }

    @FXML
    public void handleOptionD(ActionEvent e) {
        System.out.println("[QUIZ] click D");
        selectOption(3, optionDButton);
    }

    @FXML
    private void handleSkip(ActionEvent event) {
        System.out.println("[QUIZ] SKIP clicked");
        if (finished || questions.isEmpty() || answered[currentIndex]) return;

        // Zappe la question : pas de points, marquée comme répondue (skippée)
        selectedAnswers[currentIndex] = -1;
        answered[currentIndex] = true;
        skippedSet.remove(currentIndex);
        skippedQueue.remove(currentIndex);

        refreshScoreLabel();
        try {
            moveToNextTarget();
        } catch (Exception ex) {
            System.out.println("[QUIZ] SKIP error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleNextQuestion(ActionEvent event) {
        System.out.println("[QUIZ] NEXT clicked");
        if (finished || questions.isEmpty() || answered[currentIndex]) return;
        FrontQuizQuestion q = questions.get(currentIndex);
        boolean isCorrect;

        if (q.isCodingQuestion()) {
            String userCode = safe(codeEditorArea.getText());
            if (userCode.trim().isEmpty()) {
                System.out.println("[QUIZ] NEXT ignored: code vide");
                return;
            }

            btnNext.setDisable(true);
            btnSkip.setDisable(true);
            aiFeedbackLabel.setText("⏳ Correction par l'IA en cours...");
            aiFeedbackBox.getStyleClass().removeAll("ai-feedback-correct", "ai-feedback-wrong");
            aiFeedbackBox.setVisible(true);
            aiFeedbackBox.setManaged(true);

            Task<AiCodeResult> task = new Task<>() {
                @Override
                protected AiCodeResult call() {
                    AiCodeReviewService reviewer = new AiCodeReviewService();
                    return reviewer.reviewCode(
                            q.getText(),
                            userCode,
                            q.getCorrectAnswer(),
                            q.getCodingLanguage()
                    );
                }
            };

            task.setOnSucceeded(e -> {
                AiCodeResult result = task.getValue();
                aiFeedbackLabel.setText(result.getFeedback());
                aiFeedbackBox.getStyleClass().removeAll("ai-feedback-correct", "ai-feedback-wrong");
                aiFeedbackBox.getStyleClass().add(result.isCorrect() ? "ai-feedback-correct" : "ai-feedback-wrong");

                if (result.isCorrect()) {
                    correct++;
                    score += Math.max(1, q.getPoints());
                } else {
                    wrong++;
                }
                answered[currentIndex] = true;
                skippedSet.remove(currentIndex);
                skippedQueue.remove(currentIndex);
                refreshScoreLabel();

                PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
                pause.setOnFinished(ev -> moveToNextTarget());
                pause.play();
            });

            task.setOnFailed(e -> {
                wrong++;
                answered[currentIndex] = true;
                skippedSet.remove(currentIndex);
                skippedQueue.remove(currentIndex);
                aiFeedbackBox.getStyleClass().removeAll("ai-feedback-correct", "ai-feedback-wrong");
                aiFeedbackBox.getStyleClass().add("ai-feedback-wrong");
                aiFeedbackLabel.setText("Erreur de correction IA. Question marquée incorrecte.");
                refreshScoreLabel();
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(ev -> moveToNextTarget());
                pause.play();
            });

            Thread worker = new Thread(task, "quiz-code-review");
            worker.setDaemon(true);
            worker.start();
            return;
        } else if (q.isOpenQuestion()) {
            String input = safe(openAnswerField.getText()).trim();
            if (input.isBlank()) {
                System.out.println("[QUIZ] NEXT ignored: no open answer");
                return;
            }
            openAnswers[currentIndex] = input;
            isCorrect = normalize(input).equals(normalize(q.getCorrectAnswer()));
        } else {
            Integer selected = selectedAnswers[currentIndex];
            if (selected == null || selected < 0) {
                System.out.println("[QUIZ] NEXT ignored: no answer selected");
                return;
            }
            isCorrect = (selected == q.getCorrectIndex());
        }

        if (isCorrect) {
            correct++;
            score += Math.max(1, q.getPoints());
            if (!q.isOpenQuestion()) {
                Integer selected = selectedAnswers[currentIndex];
                if (selected != null && selected >= 0 && selected < optionButtons.size()) {
                    optionButtons.get(selected).getStyleClass().remove("choice-selected");
                    optionButtons.get(selected).getStyleClass().add("choice-correct");
                }
            }
        } else {
            wrong++;
            if (!q.isOpenQuestion()) {
                Integer selected = selectedAnswers[currentIndex];
                if (selected != null && selected >= 0 && selected < optionButtons.size()) {
                    optionButtons.get(selected).getStyleClass().remove("choice-selected");
                    optionButtons.get(selected).getStyleClass().add("choice-wrong");
                }
                int correctIdx = q.getCorrectIndex();
                if (correctIdx >= 0 && correctIdx < optionButtons.size()) {
                    optionButtons.get(correctIdx).getStyleClass().add("choice-correct");
                }
            }
        }

        answered[currentIndex] = true;
        skippedSet.remove(currentIndex);
        skippedQueue.remove(currentIndex);
        btnNext.setDisable(true);
        btnSkip.setDisable(true);
        refreshScoreLabel();

        if (q.isOpenQuestion()) {
            try {
                moveToNextTarget();
            } catch (Exception ex) {
                System.out.println("[QUIZ] NEXT error: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
            pause.setOnFinished(e -> {
                try {
                    moveToNextTarget();
                } catch (Exception ex) {
                    System.out.println("[QUIZ] NEXT error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
            pause.play();
        }
    }

    private void selectOption(int index, Button source) {
        if (finished || questions.isEmpty() || answered[currentIndex]) return;
        selectedAnswers[currentIndex] = index;
        for (int i = 0; i < optionButtons.size(); i++) {
            Button b = optionButtons.get(i);
            b.getStyleClass().remove("choice-selected");
            if (i == index) {
                b.getStyleClass().add("choice-selected");
            }
        }
        btnNext.setDisable(false);

        ScaleTransition pulse = new ScaleTransition(Duration.millis(110), source);
        pulse.setFromX(0.98);
        pulse.setFromY(0.98);
        pulse.setToX(1.0);
        pulse.setToY(1.0);
        pulse.play();
    }

    private void moveToNextTarget() {
        // Cherche la prochaine question non répondue (après currentIndex)
        int next = findNextUnansweredFrom(currentIndex + 1);
        if (next == -1) {
            // Plus aucune question non répondue → fin du quiz
            finishQuiz();
            return;
        }
        currentIndex = next;
        renderQuestion(true);
    }

    private int findNextUnansweredFrom(int start) {
        for (int i = Math.max(0, start); i < questions.size(); i++) {
            if (!answered[i]) {
                return i;
            }
        }
        return -1;
    }

    private int pollNextSkippedUnanswered() {
        while (!skippedQueue.isEmpty()) {
            int idx = skippedQueue.poll();
            skippedSet.remove(idx);
            if (idx >= 0 && idx < answered.length && !answered[idx]) {
                return idx;
            }
        }
        return -1;
    }

    private void renderQuestion(boolean animated) {
        FrontQuizQuestion q = questions.get(currentIndex);
        progressLabel.setText("Question " + (currentIndex + 1) + "/" + questions.size());
        if (questionBadge != null) {
            String num = String.format("Q %02d", currentIndex + 1);
            questionBadge.setText(num);
        }
        progressBar.setProgress((currentIndex + 1) / (double) Math.max(1, questions.size()));
        questionLabel.setText(safe(q.getText()));

        if (q.isCodingQuestion()) {
            optionsGrid.setVisible(false);
            optionsGrid.setManaged(false);
            openAnswerField.setVisible(false);
            openAnswerField.setManaged(false);

            codingBlock.setVisible(true);
            codingBlock.setManaged(true);
            codingLangLabel.setText(safe(q.getCodingLanguage()).toUpperCase());
            codeEditorArea.clear();

            aiFeedbackBox.setVisible(false);
            aiFeedbackBox.setManaged(false);
            aiFeedbackBox.getStyleClass().removeAll("ai-feedback-correct", "ai-feedback-wrong");
            aiFeedbackLabel.setText("");

            btnNext.setDisable(safe(codeEditorArea.getText()).trim().isBlank());
            codeEditorArea.requestFocus();
        } else if (q.isOpenQuestion()) {
            optionsGrid.setVisible(false);
            optionsGrid.setManaged(false);
            openAnswerField.setVisible(true);
            openAnswerField.setManaged(true);
            codingBlock.setVisible(false);
            codingBlock.setManaged(false);
            openAnswerField.setText(openAnswers[currentIndex] == null ? "" : openAnswers[currentIndex]);
            btnNext.setDisable(safe(openAnswerField.getText()).trim().isBlank());
            openAnswerField.requestFocus();
        } else {
            optionsGrid.setVisible(true);
            optionsGrid.setManaged(true);
            openAnswerField.setVisible(false);
            openAnswerField.setManaged(false);
            codingBlock.setVisible(false);
            codingBlock.setManaged(false);
            openAnswerField.clear();

            for (int i = 0; i < optionButtons.size(); i++) {
                Button b = optionButtons.get(i);
                b.getStyleClass().remove("choice-selected");
                b.getStyleClass().remove("choice-correct");
                b.getStyleClass().remove("choice-wrong");
                b.setDisable(false);
                String optionText = i < q.getOptions().size() ? safe(q.getOptions().get(i)) : "-";
                b.setText(letter(i) + "  " + optionText);
                if ("-".equals(optionText)) {
                    b.setDisable(true);
                }
            }

            Integer selected = selectedAnswers[currentIndex];
            if (selected != null && selected >= 0 && selected < optionButtons.size()) {
                optionButtons.get(selected).getStyleClass().add("choice-selected");
                btnNext.setDisable(false);
            } else {
                btnNext.setDisable(true);
            }
        }

        btnSkip.setDisable(false);

        if (animated) {
            FadeTransition fade = new FadeTransition(Duration.millis(220), questionCard);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            TranslateTransition slide = new TranslateTransition(Duration.millis(220), questionCard);
            slide.setFromY(14);
            slide.setToY(0);
            fade.play();
            slide.play();
        }
    }

    private void startTimer() {
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (finished) return;
            remainingSeconds--;
            updateTimer();
            if (remainingSeconds <= 0) {
                System.out.println("[QUIZ] timer finished");
                finishQuiz();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void finishQuiz() {
        if (finished) return;
        finished = true;
        if (timer != null) timer.stop();

        int unanswered = 0;
        int maxScore = 0;
        for (int i = 0; i < questions.size(); i++) {
            maxScore += Math.max(1, questions.get(i).getPoints());
            if (!answered[i]) {
                unanswered++;
            }
        }

        int spent = Math.max(0, totalDuration - Math.max(0, remainingSeconds));
        FrontQuizResult result = new FrontQuizResult(
                test,
                score,
                Math.max(1, maxScore),
                correct,
                wrong,
                unanswered,
                spent
        );
        FrontNavigator.showResult(result);
    }

    private void updateTimer() {
        int s = Math.max(0, remainingSeconds);
        timerLabel.setText(String.format("%02d:%02d", s / 60, s % 60));
    }

    private void refreshScoreLabel() {
        scoreLabel.setText(String.valueOf(score));
        if (okLabel != null) okLabel.setText(String.valueOf(correct));
        if (koLabel != null) koLabel.setText(String.valueOf(wrong));
        updateProgressArc();
    }

    private void showEmpty() {
        questionLabel.setText("No questions found for this test.");
        progressLabel.setText("Question 0/0");
        progressBar.setProgress(0);
        if (optionsGrid != null) {
            optionsGrid.setVisible(false);
            optionsGrid.setManaged(false);
        }
        if (openAnswerField != null) {
            openAnswerField.setVisible(false);
            openAnswerField.setManaged(false);
        }
        if (codingBlock != null) {
            codingBlock.setVisible(false);
            codingBlock.setManaged(false);
        }
        for (Button b : optionButtons) {
            b.setDisable(true);
            b.setText("-");
        }
        btnSkip.setDisable(true);
        btnNext.setDisable(true);
    }


    private void updateProgressArc() {
        int total = questions.size();
        int done = 0;
        for (boolean a : answered) if (a) done++;
        double pct = total > 0 ? (done * 100.0 / total) : 0;

        if (progressArc != null) {
            KeyValue kv = new KeyValue(progressArc.lengthProperty(), -360.0 * pct / 100.0);
            KeyFrame kf = new KeyFrame(Duration.millis(400), kv);
            new Timeline(kf).play();
        }
        if (arcLabel != null) arcLabel.setText((int) pct + "%");
    }

    private String letter(int i) {
        return switch (i) {
            case 0 -> "A";
            case 1 -> "B";
            case 2 -> "C";
            default -> "D";
        };
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String normalize(String value) {
        return safe(value).trim().toLowerCase();
    }

    private void installGlobalMouseDebug() {
        if (quizRoot == null) return;
        quizRoot.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node intersected = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            System.out.println("=== QUIZ MOUSE DEBUG START ===");
            System.out.println("sceneX=" + e.getSceneX() + ", sceneY=" + e.getSceneY());
            System.out.println("target=" + nodeInfo(e.getTarget()));
            System.out.println("source=" + nodeInfo(e.getSource()));
            System.out.println("intersected=" + nodeInfo(intersected));
            System.out.println("--- hierarchy from intersected ---");
            printHierarchy(intersected);
            System.out.println("=== QUIZ MOUSE DEBUG END ===");
        });
    }

    private void printHierarchy(Node from) {
        Node current = from;
        int depth = 0;
        while (current != null) {
            System.out.println("[" + depth + "] " + nodeInfo(current)
                    + " visible=" + current.isVisible()
                    + " managed=" + current.isManaged()
                    + " disabled=" + current.isDisable()
                    + " mouseTransparent=" + current.isMouseTransparent()
                    + " opacity=" + current.getOpacity());
            current = current.getParent();
            depth++;
        }
    }

    private String nodeInfo(Object obj) {
        if (!(obj instanceof Node n)) {
            return String.valueOf(obj);
        }
        String id = n.getId() == null ? "-" : n.getId();
        return n.getClass().getSimpleName() + "(id=" + id + ")";
    }

    private void installSkipNextMouseDebug() {
        btnSkip.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node hit = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            System.out.println("[QUIZ BTN DEBUG] SKIP pressed target=" + nodeInfo(e.getTarget())
                    + " intersected=" + nodeInfo(hit) + " disabled=" + btnSkip.isDisable());
        });
        btnNext.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node hit = e.getPickResult() == null ? null : e.getPickResult().getIntersectedNode();
            System.out.println("[QUIZ BTN DEBUG] NEXT pressed target=" + nodeInfo(e.getTarget())
                    + " intersected=" + nodeInfo(hit) + " disabled=" + btnNext.isDisable());
        });
    }
}
