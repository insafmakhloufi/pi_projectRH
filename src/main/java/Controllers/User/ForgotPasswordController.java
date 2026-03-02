package Controllers.User;

import Services.User.PasswordResetService;
import Utils.PasswordResetContext;
import Utils.PhoneUtil;
import Utils.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private ComboBox<String> phoneCountryCodeCombo;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> channelCombo;

    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label channelError;
    @FXML private Label statusLabel;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox contentBox;

    private final PasswordResetService resetService = new PasswordResetService();

    @FXML
    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                contentBox.setPrefWidth(newVal.getWidth());
                contentBox.setPrefHeight(newVal.getHeight());
            }
        });

        channelCombo.getItems().addAll("EMAIL", "WHATSAPP");
        channelCombo.setValue("EMAIL");

        if (phoneCountryCodeCombo != null) {
            phoneCountryCodeCombo.getItems().addAll(
                    "🇹🇳 Tunisia (+216)",
                    "🇫🇷 France (+33)",
                    "🇩🇿 Algeria (+213)",
                    "🇩🇪 Germany (+49)",
                    "🇮🇹 Italy (+39)",
                    "🇪🇬 Egypt (+20)"
            );
            phoneCountryCodeCombo.setValue("🇹🇳 Tunisia (+216)");
        }

        channelCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateChannelUI());
        updateChannelUI();
    }

    private void updateChannelUI() {
        String channelStr = channelCombo.getValue();
        boolean whatsapp = "WHATSAPP".equalsIgnoreCase(channelStr);

        if (emailField != null) {
            emailField.setVisible(!whatsapp);
            emailField.setManaged(!whatsapp);
        }
        if (phoneField != null) {
            phoneField.setVisible(whatsapp);
            phoneField.setManaged(whatsapp);
        }
        if (phoneCountryCodeCombo != null) {
            phoneCountryCodeCombo.setVisible(whatsapp);
            phoneCountryCodeCombo.setManaged(whatsapp);
        }
    }

    @FXML
    public void handleSendCode() {
        clearErrors();

        String channelStr = channelCombo.getValue();

        boolean whatsapp = "WHATSAPP".equalsIgnoreCase(channelStr);
        String identifier;
        if (whatsapp) {
            String dial = extractDialCode(phoneCountryCodeCombo == null ? null : phoneCountryCodeCombo.getValue());
            String rawPhone = phoneField == null || phoneField.getText() == null ? "" : phoneField.getText().trim();
            if (rawPhone.isEmpty()) {
                phoneError.setText("Phone is required");
                return;
            }
            try {
                identifier = PhoneUtil.normalizeToE164(dial, rawPhone);
            } catch (IllegalArgumentException e) {
                phoneError.setText("Invalid phone number");
                return;
            }
        } else {
            identifier = emailField == null || emailField.getText() == null ? "" : emailField.getText().trim();
        }

        boolean valid = true;
        if (identifier.isEmpty()) {
            if (whatsapp) {
                phoneError.setText("Phone is required");
            } else {
                emailError.setText("Email is required");
            }
            valid = false;
        }
        if (channelStr == null || channelStr.isBlank()) {
            channelError.setText("Channel is required");
            valid = false;
        }
        if (!valid) return;

        PasswordResetService.Channel channel = PasswordResetService.Channel.valueOf(channelStr);

        try {
            resetService.requestOtp(identifier, channel);
        } catch (Exception e) {
            statusLabel.setText("");
            if (whatsapp) {
                phoneError.setText(e.getMessage() == null ? "Error" : e.getMessage());
            } else {
                emailError.setText(e.getMessage() == null ? "Error" : e.getMessage());
            }
            return;
        }

        PasswordResetContext.setIdentifier(identifier);
        PasswordResetContext.setChannel(channel);

        switchSceneWithAnimation("/User/fxml/verify_otp.fxml");
    }

    @FXML
    public void goToLogin() {
        PasswordResetContext.clear();
        switchSceneWithAnimation("/User/fxml/login.fxml");
    }

    private void clearErrors() {
        emailError.setText("");
        phoneError.setText("");
        channelError.setText("");
        statusLabel.setText("");
    }

    private String extractDialCode(String value) {
        if (value == null || value.isBlank()) return "+216";
        int start = value.indexOf('(');
        int end = value.indexOf(')');
        if (start >= 0 && end > start) {
            String inside = value.substring(start + 1, end).trim();
            if (!inside.isBlank()) return inside;
        }
        return value.trim();
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
