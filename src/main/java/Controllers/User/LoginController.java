package Controllers.User;

import Entities.User.User;
import Services.User.EmailVerificationService;
import Services.User.UserService;
import Utils.EmailVerificationContext;
import Utils.Session;
import Utils.ThemeManager;
import Utils.TotpLoginContext;
import Controllers.User.FaceLoginController;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.regex.Pattern;

public class LoginController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?\\d{8,15}");

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordToggle;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox contentBox;

    @FXML private Label emailError;
    @FXML private Label passwordError;
    @FXML private Label lockCountdownLabel;
    @FXML private Label successMessage;
    @FXML private Hyperlink resendVerificationLink;

    private final UserService userService = new UserService();

    private Timeline lockCountdownTimeline;

    @FXML
    public void initialize() {
        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                contentBox.setPrefWidth(newVal.getWidth());
                contentBox.setPrefHeight(newVal.getHeight());
            }
        });

        bindPasswordToggle();

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            stopLockCountdown();
            validateIdentifier(newVal);
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validatePassword(newVal);
        });
    }

    private void bindPasswordToggle() {
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        showPasswordToggle.selectedProperty().addListener((obs, was, isSelected) -> {
            passwordVisibleField.setVisible(isSelected);
            passwordVisibleField.setManaged(isSelected);

            passwordField.setVisible(!isSelected);
            passwordField.setManaged(!isSelected);

            updateEyeIcon();
        });

        updateEyeIcon();
    }

    private void updateEyeIcon() {
        boolean showing = showPasswordToggle != null && showPasswordToggle.isSelected();

        SVGPath icon = new SVGPath();
        icon.setContent(showing
                ? "M2 4 L18 20 M22 4 L4 22 M1 12 C4 6 8 3 12 3 C16 3 20 6 23 12 C20 18 16 21 12 21 C8 21 4 18 1 12 Z"
                : "M1 12 C4 6 8 3 12 3 C16 3 20 6 23 12 C20 18 16 21 12 21 C8 21 4 18 1 12 Z M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8 Z");
        icon.setFill(Color.TRANSPARENT);
        icon.setStroke(Color.web("#374151"));
        icon.setStrokeWidth(2);

        showPasswordToggle.setText("");
        showPasswordToggle.setGraphic(icon);
    }

    @FXML
    public void handleLogin() {
        clearErrors();

        String identifier = emailField.getText() == null ? "" : emailField.getText().trim();
        String password;
        if (showPasswordToggle != null && showPasswordToggle.isSelected()) {
            password = passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText().trim();
        } else {
            password = passwordField.getText() == null ? "" : passwordField.getText().trim();
        }

        boolean valid = true;
        if (!validateIdentifier(identifier)) valid = false;
        if (!validatePassword(password)) valid = false;
        if (!valid) return;

        User user;
        try {
            user = userService.loginWithIdentifier(identifier, password);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() == null ? "Login error" : ex.getMessage();
            if (msg.startsWith("LOCKED|")) {
                passwordError.setText("");
                startLockCountdown(msg);
            } else if (msg.startsWith("TOTP_REQUIRED|")) {
                try {
                    int userId = Integer.parseInt(msg.substring("TOTP_REQUIRED|".length()).trim());
                    TotpLoginContext.setUserId(userId);
                    switchSceneWithAnimation("/User/fxml/verify_totp_login.fxml");
                } catch (Exception e) {
                    passwordError.setText("Login error");
                }
            } else if (msg.toLowerCase().contains("verify") && msg.toLowerCase().contains("email")) {
                passwordError.setText(msg);
                if (resendVerificationLink != null) {
                    resendVerificationLink.setVisible(true);
                    resendVerificationLink.setManaged(true);
                }
            } else {
                passwordError.setText(msg);
            }
            return;
        }
        if (user == null) {
            passwordError.setText("Invalid credentials");
            return;
        }

        Session.setCurrentUser(user);

        String role = user.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            goToDashboard("/User/fxml/admin_dashboard.fxml");
        } else if ("CANDIDAT".equalsIgnoreCase(role)) {
            goToDashboard("/Dashboardfxml/Dashboard_Front.fxml");
        } else if ("MANAGER_RH".equalsIgnoreCase(role) || "MANAGERRH".equalsIgnoreCase(role)) {
            goToDashboard("/Dashboardfxml/Dashboard_Admin.fxml");
        } else {
            successMessage.setText("Logged in successfully!");
        }
    }

    private void goToDashboard(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) scrollPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void goToSignup() {
        switchSceneWithAnimation("/User/fxml/ajouter.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        switchSceneWithAnimation("/User/fxml/forgot_password.fxml");
    }

    @FXML
    public void goToFaceLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/face_login.fxml"));
            Parent root = loader.load();
            try {
                FaceLoginController controller = loader.getController();
                String identifier = emailField == null ? "" : (emailField.getText() == null ? "" : emailField.getText().trim());
                controller.setIdentifier(identifier);
            } catch (Exception ignored) {
            }

            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) scrollPane.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void clearErrors() {
        emailError.setText("");
        passwordError.setText("");
        if (lockCountdownLabel != null) {
            lockCountdownLabel.setText("");
        }
        successMessage.setText("");

        stopLockCountdown();
    }

    private void stopLockCountdown() {
        if (lockCountdownTimeline != null) {
            lockCountdownTimeline.stop();
            lockCountdownTimeline = null;
        }
        if (lockCountdownLabel != null) {
            lockCountdownLabel.setText("");
        }
    }

    private void startLockCountdown(String lockedMessage) {
        if (lockCountdownLabel == null) {
            passwordError.setText("Account locked. Try again later.");
            return;
        }

        long untilMillis;
        try {
            untilMillis = Long.parseLong(lockedMessage.substring("LOCKED|".length()));
        } catch (Exception e) {
            lockCountdownLabel.setText("Account locked. Try again later.");
            return;
        }

        Runnable update = () -> {
            long now = System.currentTimeMillis();
            long diff = untilMillis - now;
            if (diff <= 0) {
                lockCountdownLabel.setText("");
                if (lockCountdownTimeline != null) {
                    lockCountdownTimeline.stop();
                }
                return;
            }
            long totalSec = diff / 1000;
            long mm = totalSec / 60;
            long ss = totalSec % 60;
            lockCountdownLabel.setText(String.format("Account locked. Try again in %02d:%02d", mm, ss));
        };

        update.run();
        lockCountdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), evt -> update.run()));
        lockCountdownTimeline.setCycleCount(Timeline.INDEFINITE);
        lockCountdownTimeline.play();
    }

    private boolean validateIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.isEmpty()) {
            emailError.setText("Identifier is required");
            return false;
        }

        if (value.contains("@")) {
            if (!EMAIL_PATTERN.matcher(value).matches()) {
                emailError.setText("Invalid email format");
                return false;
            }
        } else if (PHONE_PATTERN.matcher(value).matches()) {
            // ok
        } else {
            if (value.length() < 3) {
                emailError.setText("Username must be at least 3 characters");
                return false;
            }
        }

        emailError.setText("");
        return true;
    }

    private boolean validatePassword(String password) {
        String value = password == null ? "" : password.trim();
        if (value.isEmpty()) {
            passwordError.setText("Password is required");
            return false;
        }
        passwordError.setText("");
        return true;
    }

    @FXML
    public void resendVerificationEmail() {
        String identifier = emailField.getText() == null ? "" : emailField.getText().trim();
        if (identifier.isEmpty()) {
            passwordError.setText("Please enter your email first");
            return;
        }

        try {
            EmailVerificationService service = new EmailVerificationService();
            service.sendVerificationCode(identifier, null);
            EmailVerificationContext.setEmail(identifier);
            switchSceneWithAnimation("/User/fxml/verify_email.fxml");
        } catch (IllegalArgumentException e) {
            passwordError.setText(e.getMessage());
        } catch (IllegalStateException e) {
            passwordError.setText("Email service not configured. Check Gmail SMTP settings.");
        } catch (Exception e) {
            passwordError.setText("Failed to send verification email: " + e.getMessage());
        }
    }
}
