package Controllers.testQuiz;

import Entities.testQuiz.Proposition;
import Services.testQuiz.PropositionService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class PropositionFormController {

    @FXML
    private TextArea contenuArea;

    @FXML
    private CheckBox estCorrectCheckBox;

    @FXML
    private Label contenuValidationLabel;

    @FXML
    private Label correctValidationLabel;

    @FXML
    private Button saveButton;

    private final PropositionService propositionService = new PropositionService();

    private int questionId;
    private Proposition proposition;
    private boolean saved;

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
        setupLiveValidation();
        validateFormAndToggleActions();
    }

    public void setProposition(Proposition proposition) {
        this.proposition = proposition;

        contenuArea.setText(proposition.getContenu());
        estCorrectCheckBox.setSelected(proposition.isEstCorrect());
        validateFormAndToggleActions();
    }

    public Proposition getProposition() {
        return proposition;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleEnregistrer() {
        String contenu = contenuArea.getText() == null ? "" : contenuArea.getText().trim();
        boolean estCorrect = estCorrectCheckBox.isSelected();

        if (contenu.isEmpty()) {
            showError("Validation", "Le contenu est obligatoire.");
            return;
        }

        if (!propositionService.questionExists(questionId)) {
            showError("Validation", "La question selectionnee n'existe plus.");
            return;
        }

        if (proposition == null) {
            proposition = new Proposition(questionId, contenu, estCorrect);
        } else {
            proposition.setQuestionID(questionId);
            proposition.setContenu(contenu);
            proposition.setEstCorrect(estCorrect);
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
        Stage stage = (Stage) contenuArea.getScene().getWindow();
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
        contenuArea.textProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
        estCorrectCheckBox.selectedProperty().addListener((obs, o, n) -> validateFormAndToggleActions());
    }

    private void validateFormAndToggleActions() {
        boolean contenuOk = contenuArea.getText() != null && !contenuArea.getText().trim().isEmpty();
        boolean checkOk = true;

        updateValidationUI(contenuArea, contenuValidationLabel, contenuOk, "Contenu obligatoire");
        updateValidationUI(estCorrectCheckBox, correctValidationLabel, checkOk, "OK");

        if (saveButton != null) {
            saveButton.setDisable(!contenuOk || !checkOk);
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
