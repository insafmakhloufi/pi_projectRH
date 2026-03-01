package Controllers.testQuiz;

import Entities.testQuiz.Question;
import Services.testQuiz.QuestionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class QuestionFormController {

    @FXML
    private TextArea questionArea;

    @FXML
    private TextField reponseField;

    @FXML
    private TextField pointsField;

    @FXML
    private Label questionValidationLabel;

    @FXML
    private Label pointsValidationLabel;

    @FXML
    private Button saveButton;

    private final QuestionService questionService = new QuestionService();

    private int testId;
    private Question question;
    private boolean saved;

    public void setTestId(int testId) {
        this.testId = testId;
        setupLiveValidation();
        validateFormAndToggleActions();
    }

    public void setQuestion(Question question) {
        this.question = question;

        questionArea.setText(question.getQuestion());
        reponseField.setText(question.getReponse());
        pointsField.setText(String.valueOf(question.getPoints()));
        validateFormAndToggleActions();
    }

    public Question getQuestion() {
        return question;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleEnregistrer() {
        String questionText = questionArea.getText() == null ? "" : questionArea.getText().trim();
        String reponse = reponseField.getText() == null ? "" : reponseField.getText().trim();

        if (questionText.isEmpty()) {
            showError("Validation", "La question est obligatoire.");
            return;
        }

        if (!questionService.testExists(testId)) {
            showError("Validation", "Le test selectionne n'existe plus.");
            return;
        }

        int points;
        try {
            points = Integer.parseInt(pointsField.getText().trim());
        } catch (NumberFormatException e) {
            showError("Validation", "Le champ points doit etre numerique.");
            return;
        }

        if (points < 1) {
            showError("Validation", "Le champ points doit etre >= 1.");
            return;
        }

        if (question == null) {
            question = new Question(testId, questionText, reponse.isEmpty() ? null : reponse, points);
        } else {
            question.setTestID(testId);
            question.setQuestion(questionText);
            question.setReponse(reponse.isEmpty() ? null : reponse);
            question.setPoints(points);
        }

        saved = true;
        closeStage();
    }

    @FXML
    private void handleAnnuler() {
        saved = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) questionArea.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void setupLiveValidation() {
        questionArea.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        pointsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
    }

    private void validateFormAndToggleActions() {
        boolean questionOk = questionArea.getText() != null && !questionArea.getText().trim().isEmpty();
        boolean pointsOk = false;
        try {
            pointsOk = Integer.parseInt(pointsField.getText() == null ? "" : pointsField.getText().trim()) >= 1;
        } catch (Exception ignored) {
            pointsOk = false;
        }

        updateValidationUI(questionArea, questionValidationLabel, questionOk, "Question obligatoire");
        updateValidationUI(pointsField, pointsValidationLabel, pointsOk, "Points >= 1");

        if (saveButton != null) {
            saveButton.setDisable(!(questionOk && pointsOk));
        }
    }

    private void updateValidationUI(javafx.scene.Node field, Label messageLabel, boolean ok, String errMsg) {
        field.getStyleClass().removeAll("field-error", "field-ok");
        messageLabel.getStyleClass().removeAll("error-text", "ok-text");
        if (ok) {
            field.getStyleClass().add("field-ok");
            messageLabel.setText("OK");
            messageLabel.getStyleClass().add("ok-text");
        } else {
            field.getStyleClass().add("field-error");
            messageLabel.setText(errMsg);
            messageLabel.getStyleClass().add("error-text");
        }
    }
}
