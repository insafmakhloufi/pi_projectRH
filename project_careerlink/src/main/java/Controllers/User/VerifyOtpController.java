package Controllers.User;

import Services.User.PasswordResetService;
import Utils.PasswordResetContext;
import Utils.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class VerifyOtpController {

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{6,}$");

    @FXML private Label hintLabel;

    @FXML private TextField otp1;
    @FXML private TextField otp2;
    @FXML private TextField otp3;
    @FXML private TextField otp4;
    @FXML private TextField otp5;
    @FXML private TextField otp6;

    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordVisibleField;
    @FXML private ToggleButton showNewPasswordToggle;

    @FXML private Label otpError;
    @FXML private Label passwordError;
    @FXML private Label statusLabel;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox contentBox;

    private final PasswordResetService resetService = new PasswordResetService();

    private final List<TextField> otpFields = new ArrayList<>();

    @FXML
    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                contentBox.setPrefWidth(newVal.getWidth());
                contentBox.setPrefHeight(newVal.getHeight());
            }
        });

        String channel = PasswordResetContext.getChannel() == null ? "" : PasswordResetContext.getChannel().name();
        if (!channel.isBlank()) {
            String display = "WHATSAPP".equalsIgnoreCase(channel) ? "WhatsApp" : channel;
            hintLabel.setText("Enter the code sent via " + display);
        }

        otpFields.clear();
        otpFields.add(otp1);
        otpFields.add(otp2);
        otpFields.add(otp3);
        otpFields.add(otp4);
        otpFields.add(otp5);
        otpFields.add(otp6);
        installOtpBehavior();
        bindPasswordToggle();
    }

    private void bindPasswordToggle() {
        if (newPasswordVisibleField != null) {
            newPasswordVisibleField.textProperty().bindBidirectional(newPasswordField.textProperty());
        }

        if (showNewPasswordToggle != null) {
            showNewPasswordToggle.selectedProperty().addListener((obs, was, isSelected) -> {
                if (newPasswordVisibleField != null) {
                    newPasswordVisibleField.setVisible(isSelected);
                    newPasswordVisibleField.setManaged(isSelected);
                }
                newPasswordField.setVisible(!isSelected);
                newPasswordField.setManaged(!isSelected);

                updateEyeIcon();
            });

            updateEyeIcon();
        }
    }

    private void updateEyeIcon() {
        if (showNewPasswordToggle == null) return;
        boolean showing = showNewPasswordToggle.isSelected();

        SVGPath icon = new SVGPath();
        icon.setContent(showing
                ? "M2 4 L18 20 M22 4 L4 22 M1 12 C4 6 8 3 12 3 C16 3 20 6 23 12 C20 18 16 21 12 21 C8 21 4 18 1 12 Z"
                : "M1 12 C4 6 8 3 12 3 C16 3 20 6 23 12 C20 18 16 21 12 21 C8 21 4 18 1 12 Z M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8 Z");
        icon.setFill(Color.TRANSPARENT);
        icon.setStroke(Color.web("#374151"));
        icon.setStrokeWidth(2);

        showNewPasswordToggle.setText("");
        showNewPasswordToggle.setGraphic(icon);
    }

    private void installOtpBehavior() {
        for (int i = 0; i < otpFields.size(); i++) {
            TextField field = otpFields.get(i);
            if (field == null) continue;

            final int index = i;
            field.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change -> {
                String text = change.getControlNewText();
                if (text.isEmpty()) return change;
                if (!text.matches("\\d{0,1}")) {
                    return null;
                }
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

        if (otp1 != null) {
            javafx.application.Platform.runLater(() -> otp1.requestFocus());
        }
    }

    private void focusNext(int index) {
        int next = index + 1;
        if (next >= 0 && next < otpFields.size()) {
            TextField f = otpFields.get(next);
            if (f != null) {
                f.requestFocus();
                f.selectAll();
            }
        }
    }

    private void focusPrev(int index) {
        int prev = index - 1;
        if (prev >= 0 && prev < otpFields.size()) {
            TextField f = otpFields.get(prev);
            if (f != null) {
                f.requestFocus();
                f.selectAll();
            }
        }
    }

    private String readOtp() {
        StringBuilder sb = new StringBuilder();
        for (TextField f : otpFields) {
            String t = f == null ? "" : f.getText();
            sb.append(t == null ? "" : t.trim());
        }
        return sb.toString();
    }

    @FXML
    public void handleResetPassword() {
        clearErrors();

        String identifier = PasswordResetContext.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            otpError.setText("Missing identifier. Please restart.");
            return;
        }

        String otp = readOtp();
        String newPassword = newPasswordField.getText() == null ? "" : newPasswordField.getText().trim();

        boolean valid = true;
        if (!otp.matches("\\d{6}")) {
            otpError.setText("OTP must be 6 digits");
            valid = false;
        }
        if (newPassword.isEmpty()) {
            passwordError.setText("Password is required");
            valid = false;
        } else if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            passwordError.setText("Password must have 1 uppercase, 1 lowercase, 1 digit, 1 special char");
            valid = false;
        }
        if (!valid) return;

        try {
            resetService.verifyOtpAndResetPassword(identifier, otp, newPassword);
        } catch (Exception e) {
            otpError.setText(e.getMessage() == null ? "Error" : e.getMessage());
            return;
        }

        PasswordResetContext.clear();
        switchSceneWithAnimation("/User/fxml/login.fxml");
    }

    @FXML
    public void goToForgot() {
        switchSceneWithAnimation("/User/fxml/forgot_password.fxml");
    }

    private void clearErrors() {
        otpError.setText("");
        passwordError.setText("");
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
