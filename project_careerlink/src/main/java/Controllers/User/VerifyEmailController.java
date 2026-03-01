package Controllers.User;

import Services.User.EmailVerificationService;
import Utils.EmailVerificationContext;
import Utils.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class VerifyEmailController {

    @FXML private Label hintLabel;

    @FXML private TextField c1;
    @FXML private TextField c2;
    @FXML private TextField c3;
    @FXML private TextField c4;
    @FXML private TextField c5;
    @FXML private TextField c6;

    @FXML private Label codeError;
    @FXML private Label statusLabel;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox contentBox;

    private final EmailVerificationService verificationService = new EmailVerificationService();
    private final List<TextField> codeFields = new ArrayList<>();

    @FXML
    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                contentBox.setPrefWidth(newVal.getWidth());
                contentBox.setPrefHeight(newVal.getHeight());
            }
        });

        String email = EmailVerificationContext.getEmail();
        if (email != null && !email.isBlank()) {
            hintLabel.setText("Enter the code we sent to " + email);
        }

        codeFields.clear();
        codeFields.add(c1);
        codeFields.add(c2);
        codeFields.add(c3);
        codeFields.add(c4);
        codeFields.add(c5);
        codeFields.add(c6);
        installCodeBehavior();
    }

    private void installCodeBehavior() {
        for (int i = 0; i < codeFields.size(); i++) {
            TextField field = codeFields.get(i);
            if (field == null) continue;

            final int index = i;
            field.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change -> {
                String text = change.getControlNewText();
                if (text.isEmpty()) return change;
                if (!text.matches("\\d{0,1}")) return null;
                return change;
            }));

            field.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.length() == 1) {
                    focusNext(index);
                }
            });

            field.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case BACK_SPACE:
                        if (field.getText() == null || field.getText().isEmpty()) {
                            focusPrev(index);
                        }
                        break;
                    case LEFT:
                        focusPrev(index);
                        break;
                    case RIGHT:
                        focusNext(index);
                        break;
                    default:
                        break;
                }
            });
        }

        if (c1 != null) {
            javafx.application.Platform.runLater(() -> c1.requestFocus());
        }
    }

    private void focusNext(int index) {
        int next = index + 1;
        if (next >= 0 && next < codeFields.size()) {
            TextField f = codeFields.get(next);
            if (f != null) {
                f.requestFocus();
                f.selectAll();
            }
        }
    }

    private void focusPrev(int index) {
        int prev = index - 1;
        if (prev >= 0 && prev < codeFields.size()) {
            TextField f = codeFields.get(prev);
            if (f != null) {
                f.requestFocus();
                f.selectAll();
            }
        }
    }

    private String readCode() {
        StringBuilder sb = new StringBuilder();
        for (TextField f : codeFields) {
            String t = f == null ? "" : f.getText();
            sb.append(t == null ? "" : t.trim());
        }
        return sb.toString();
    }

    @FXML
    public void handleVerify() {
        clearErrors();

        String email = EmailVerificationContext.getEmail();
        if (email == null || email.isBlank()) {
            codeError.setText("Missing email. Please sign up again.");
            return;
        }

        String code = readCode();
        if (!code.matches("\\d{6}")) {
            codeError.setText("Code must be 6 digits");
            return;
        }

        try {
            verificationService.verifyCode(email, code);
        } catch (Exception e) {
            codeError.setText(e.getMessage() == null ? "Error" : e.getMessage());
            return;
        }

        statusLabel.setText("Email verified. You can login now.");
        EmailVerificationContext.clear();
        switchSceneWithAnimation("/User/fxml/login.fxml");
    }

    @FXML
    public void handleResend() {
        clearErrors();
        String email = EmailVerificationContext.getEmail();
        if (email == null || email.isBlank()) {
            codeError.setText("Missing email. Please sign up again.");
            return;
        }

        try {
            verificationService.sendVerificationCode(email, null);
        } catch (Exception e) {
            codeError.setText(e.getMessage() == null ? "Error" : e.getMessage());
            return;
        }

        statusLabel.setText("Verification code resent.");
    }

    @FXML
    public void goToLogin() {
        EmailVerificationContext.clear();
        switchSceneWithAnimation("/User/fxml/login.fxml");
    }

    private void clearErrors() {
        codeError.setText("");
        statusLabel.setText("");
    }

    private void switchSceneWithAnimation(String fxmlPath) {
        Scene currentScene = scrollPane.getScene();
        if (currentScene == null) return;

        Stage stage = (Stage) currentScene.getWindow();

        Parent currentRoot = currentScene.getRoot();
        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), currentRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(evt -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent newRoot = loader.load();
                Scene newScene = new Scene(newRoot);
                ThemeManager.applyTheme(newScene);

                stage.setScene(newScene);
                stage.setMaximized(true);

                newRoot.setOpacity(0.0);
                newRoot.setTranslateY(14);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), newRoot);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.setInterpolator(Interpolator.EASE_OUT);

                TranslateTransition slideIn = new TranslateTransition(Duration.millis(220), newRoot);
                slideIn.setFromY(14);
                slideIn.setToY(0);
                slideIn.setInterpolator(Interpolator.EASE_OUT);

                new ParallelTransition(fadeIn, slideIn).play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        fadeOut.play();
    }
}
