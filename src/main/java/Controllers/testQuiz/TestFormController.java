package Controllers.testQuiz;

import Entities.testQuiz.CandidatItem;
import Entities.testQuiz.Test;
import Services.testQuiz.TestService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class TestFormController implements Initializable {

    @FXML
    private ComboBox<CandidatItem> candidatComboBox;

    @FXML
    private TextField titreField;

    @FXML
    private ComboBox<String> typeComboBox;

    @FXML
    private TextField scoreMaxField;

    @FXML
    private TextField durationSecondsField;

    @FXML
    private TextField totalPointsField;

    @FXML
    private Label candidatValidationLabel;

    @FXML
    private Label titreValidationLabel;

    @FXML
    private Label typeValidationLabel;

    @FXML
    private Label scoreValidationLabel;

    @FXML
    private Label durationValidationLabel;

    @FXML
    private Label totalValidationLabel;

    @FXML
    private Button saveButton;

    private final TestService testService = new TestService();
    private static final String[] TEST_TYPES = new String[]{"QCM", "LOGIQUE", "CODING"};

    private Test test;
    private boolean saved;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeComboBox.getItems().setAll(TEST_TYPES);
        candidatComboBox.getItems().setAll(testService.getAllCandidats());
        installLiveValidation();
        validateFormAndToggleSave();
    }

    public void setTest(Test test) {
        this.test = test;

        selectCandidateById(test.getCandidatID());
        titreField.setText(test.getTitre());
        typeComboBox.setValue(test.getType());
        scoreMaxField.setText(String.valueOf(test.getScoreMax()));
        durationSecondsField.setText(String.valueOf(test.getDurationSeconds()));
        totalPointsField.setText(String.valueOf(test.getTotalPoints()));
        validateFormAndToggleSave();
    }

    public Test getTest() {
        return test;
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleEnregistrer() {
        try {
            CandidatItem candidat = candidatComboBox.getValue();
            if (candidat == null) {
                showError("Validation", "Le candidat est obligatoire.");
                return;
            }
            int candidatID = candidat.getId();
            String titre = titreField.getText() == null ? "" : titreField.getText().trim();
            String type = typeComboBox.getValue() == null ? "" : typeComboBox.getValue().trim();
            double scoreMax = Double.parseDouble(scoreMaxField.getText().trim());
            int durationSeconds = Integer.parseInt(durationSecondsField.getText().trim());
            int totalPoints = Integer.parseInt(totalPointsField.getText().trim());

            if (titre.length() < 3) {
                showError("Validation", "Le titre est obligatoire (min 3 caracteres).");
                return;
            }
            if (type.isEmpty()) {
                showError("Validation", "Le type est obligatoire.");
                return;
            }
            if (scoreMax < 0 || durationSeconds < 0 || totalPoints < 0) {
                showError("Validation", "scoreMax, durationSeconds et totalPoints doivent etre >= 0.");
                return;
            }
            if (!testService.candidatExists(candidatID)) {
                showError("Validation", "Le candidat selectionne n'existe pas dans la table user.");
                return;
            }

            int excludeId = test == null ? 0 : test.getId();
            if (testService.existsTitreForCandidat(candidatID, titre, excludeId)) {
                showError("Validation", "Ce candidat a deja un test avec ce titre.");
                return;
            }

            if (test == null) {
                test = new Test(candidatID, titre, type, scoreMax, durationSeconds, totalPoints);
            } else {
                test.setCandidatID(candidatID);
                test.setTitre(titre);
                test.setType(type);
                test.setScoreMax(scoreMax);
                test.setDurationSeconds(durationSeconds);
                test.setTotalPoints(totalPoints);
            }

            saved = true;
            closeStage();

        } catch (NumberFormatException e) {
            showError("Validation", "Les champs numeriques sont invalides.");
        }
    }

    @FXML
    private void handleAnnuler() {
        saved = false;
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) candidatComboBox.getScene().getWindow();
        stage.close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void installLiveValidation() {
        candidatComboBox.valueProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
        titreField.textProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
        typeComboBox.valueProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
        scoreMaxField.textProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
        durationSecondsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
        totalPointsField.textProperty().addListener((obs, o, n) -> validateFormAndToggleSave());
    }

    private void validateFormAndToggleSave() {
        boolean candidatOk = candidatComboBox.getValue() != null;
        boolean titreOk = valueOf(titreField).length() >= 3;
        boolean typeOk = typeComboBox.getValue() != null && !typeComboBox.getValue().isBlank();
        boolean scoreOk = isDoubleNonNegative(scoreMaxField);
        boolean durationOk = isIntNonNegative(durationSecondsField);
        boolean totalOk = isIntNonNegative(totalPointsField);

        updateValidationUI(candidatComboBox, candidatValidationLabel, candidatOk, "Candidat obligatoire");
        updateValidationUI(titreField, titreValidationLabel, titreOk, "Titre min 3 caracteres");
        updateValidationUI(typeComboBox, typeValidationLabel, typeOk, "Type obligatoire");
        updateValidationUI(scoreMaxField, scoreValidationLabel, scoreOk, "ScoreMax >= 0");
        updateValidationUI(durationSecondsField, durationValidationLabel, durationOk, "Duration >= 0");
        updateValidationUI(totalPointsField, totalValidationLabel, totalOk, "TotalPoints >= 0");

        boolean uniqueOk = true;
        CandidatItem candidat = candidatComboBox.getValue();
        if (candidatOk && titreOk && candidat != null) {
            int excludeId = test == null ? 0 : test.getId();
            uniqueOk = !testService.existsTitreForCandidat(candidat.getId(), valueOf(titreField), excludeId);
            if (!uniqueOk) {
                updateValidationUI(titreField, titreValidationLabel, false, "Ce candidat a deja un test avec ce titre.");
            }
        }

        boolean formOk = candidatOk && titreOk && typeOk && scoreOk && durationOk && totalOk && uniqueOk;
        if (saveButton != null) {
            saveButton.setDisable(!formOk);
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

    private String valueOf(TextField field) {
        return field.getText() == null ? "" : field.getText().trim();
    }

    private boolean isDoubleNonNegative(TextField field) {
        try {
            return Double.parseDouble(valueOf(field)) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isIntNonNegative(TextField field) {
        try {
            return Integer.parseInt(valueOf(field)) >= 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void selectCandidateById(int id) {
        for (CandidatItem item : candidatComboBox.getItems()) {
            if (item.getId() == id) {
                candidatComboBox.setValue(item);
                return;
            }
        }
    }
}
