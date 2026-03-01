package Controllers.testQuiz;
// BACKEND - NE PAS MODIFIER POUR LE FRONT

import Services.ai.OpenAiTranslationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.NodeOrientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Optional;

public class TranslationDialogController {

    @FXML
    private TextArea sourceTextArea;
    @FXML
    private ComboBox<String> sourceLangCombo;
    @FXML
    private ComboBox<String> targetLangCombo;
    @FXML
    private Button translateButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private TextArea resultTextArea;
    @FXML
    private Button copyButton;
    @FXML
    private Button applyButton;
    @FXML
    private Button closeButton;
    @FXML
    private Label sourceValidationLabel;
    @FXML
    private Label targetValidationLabel;
    @FXML
    private Label statusLabel;

    private final OpenAiTranslationService translationService = new OpenAiTranslationService();

    private Stage dialogStage;
    private String appliedTranslation;

    @FXML
    private void initialize() {
        sourceLangCombo.getItems().setAll("AUTO", "FR", "EN", "AR");
        sourceLangCombo.setValue("AUTO");

        targetLangCombo.getItems().setAll("FR", "EN", "AR");

        resultTextArea.setEditable(false);
        resultTextArea.setWrapText(true);

        progressIndicator.setVisible(false);
        progressIndicator.setManaged(false);
        copyButton.setDisable(true);
        applyButton.setDisable(true);

        sourceTextArea.textProperty().addListener((obs, o, n) -> validateForm());
        targetLangCombo.valueProperty().addListener((obs, o, n) -> {
            updateResultOrientation();
            validateForm();
        });

        validateForm();
    }

    public static Optional<String> showAndWait(Window owner, String prefillText) {
        try {
            FXMLLoader loader = new FXMLLoader(TranslationDialogController.class.getResource("/testQuiz/fxml/translation_dialog.fxml"));
            Parent root = loader.load();
            TranslationDialogController controller = loader.getController();

            if (prefillText != null) {
                controller.sourceTextArea.setText(prefillText);
            }

            Stage dialog = new Stage();
            dialog.setTitle("Traduction");
            if (owner != null) {
                dialog.initOwner(owner);
                dialog.initModality(Modality.WINDOW_MODAL);
            }
            Scene scene = new Scene(root, 760, 620);
            scene.getStylesheets().add(TranslationDialogController.class.getResource("/testQuiz/css/style.css").toExternalForm());
            dialog.setScene(scene);
            controller.setDialogStage(dialog);

            dialog.showAndWait();
            return Optional.ofNullable(controller.appliedTranslation);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la fenetre de traduction.");
            alert.showAndWait();
            return Optional.empty();
        }
    }

    @FXML
    private void handleTranslate() {
        validateForm();
        if (translateButton.isDisable()) {
            return;
        }

        setBusy(true);
        statusLabel.setText("Traduction en cours...");

        String sourceText = sourceTextArea.getText().trim();
        String sourceLang = sourceLangCombo.getValue();
        String targetLang = targetLangCombo.getValue();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return translationService.translate(sourceText, sourceLang, targetLang);
            }
        };

        task.setOnSucceeded(event -> Platform.runLater(() -> {
            setBusy(false);
            String translated = task.getValue();
            resultTextArea.setText(translated == null ? "" : translated);
            copyButton.setDisable(resultTextArea.getText().isBlank());
            applyButton.setDisable(resultTextArea.getText().isBlank());
            statusLabel.setText("Traduction terminee.");
        }));

        task.setOnFailed(event -> Platform.runLater(() -> {
            setBusy(false);
            Throwable error = task.getException();
            String message = error == null || error.getMessage() == null || error.getMessage().isBlank()
                    ? "Echec de traduction. Verifiez votre connexion et la configuration API."
                    : error.getMessage();
            statusLabel.setText("Echec de traduction.");
            showError("Traduction", message);
        }));

        Thread thread = new Thread(task, "openai-translation-task");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCopy() {
        String text = resultTextArea.getText() == null ? "" : resultTextArea.getText();
        if (text.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard.getSystemClipboard().setContent(content);
        statusLabel.setText("Texte copie.");
    }

    @FXML
    private void handleApply() {
        String text = resultTextArea.getText() == null ? "" : resultTextArea.getText().trim();
        if (text.isBlank()) {
            return;
        }
        appliedTranslation = text;
        closeDialog();
    }

    @FXML
    private void handleClose() {
        closeDialog();
    }

    private void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void validateForm() {
        boolean sourceOk = sourceTextArea.getText() != null && !sourceTextArea.getText().trim().isEmpty();
        boolean targetOk = targetLangCombo.getValue() != null && !targetLangCombo.getValue().trim().isEmpty();

        updateValidation(sourceTextArea, sourceValidationLabel, sourceOk, "Texte source obligatoire");
        updateValidation(targetLangCombo, targetValidationLabel, targetOk, "Langue cible obligatoire");

        translateButton.setDisable(!(sourceOk && targetOk));
    }

    private void updateResultOrientation() {
        String target = targetLangCombo.getValue();
        if ("AR".equals(target)) {
            resultTextArea.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        } else {
            resultTextArea.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        }
    }

    private void updateValidation(javafx.scene.Node field, Label messageLabel, boolean ok, String errorMessage) {
        field.getStyleClass().removeAll("field-error", "field-ok");
        messageLabel.getStyleClass().removeAll("error-text", "ok-text");

        if (ok) {
            field.getStyleClass().add("field-ok");
            messageLabel.setText("OK");
            messageLabel.getStyleClass().add("ok-text");
        } else {
            field.getStyleClass().add("field-error");
            messageLabel.setText(errorMessage);
            messageLabel.getStyleClass().add("error-text");
        }
    }

    private void setBusy(boolean busy) {
        progressIndicator.setVisible(busy);
        progressIndicator.setManaged(busy);

        sourceTextArea.setDisable(busy);
        sourceLangCombo.setDisable(busy);
        targetLangCombo.setDisable(busy);
        copyButton.setDisable(busy || resultTextArea.getText().isBlank());
        applyButton.setDisable(busy || resultTextArea.getText().isBlank());
        closeButton.setDisable(busy);

        if (!busy) {
            validateForm();
        } else {
            translateButton.setDisable(true);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
