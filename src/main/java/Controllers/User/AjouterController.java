package Controllers.User;

import Entities.User.User;
import Services.User.EmailVerificationService;
import Services.User.UserService;
import Utils.PhoneUtil;
import Utils.EmailVerificationContext;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import Utils.ThemeManager;

import java.io.File;
import java.util.regex.Pattern;

public class AjouterController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{6,}$");

    @FXML private TextField nameField;
    @FXML private TextField titreField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private ComboBox<String> phoneCountryCodeCombo;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private ToggleButton showPasswordToggle;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private ImageView photoPreview;

    @FXML private ScrollPane scrollPane;
    @FXML private VBox contentBox;

    @FXML private Label nameError;
    @FXML private Label emailError;
    @FXML private Label phoneError;
    @FXML private Label passwordError;
    @FXML private Label roleError;
    @FXML private Label successMessage;
    @FXML private Label photoLabel;

    private final UserService userService = new UserService();
    private final EmailVerificationService verificationService = new EmailVerificationService();
    private String selectedPhotoPath = null;

    private final PauseTransition nameDebounce = new PauseTransition(Duration.millis(350));
    private final PauseTransition emailDebounce = new PauseTransition(Duration.millis(350));
    private final PauseTransition phoneDebounce = new PauseTransition(Duration.millis(350));

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("MANAGER_RH", "CANDIDAT");

        if (phoneCountryCodeCombo != null) {
            phoneCountryCodeCombo.getItems().addAll(
                    "🇹🇳 Tunisia (+216)",
                    "🇫🇷 France (+33)",
                    "🇩🇿 Algeria (+213)",
                    "🇪🇬 Egypt (+20)",
                    "🇩🇪 Germany (+49)",
                    "🇮🇹 Italy (+39)"
            );
            phoneCountryCodeCombo.setValue("🇹🇳 Tunisia (+216)");
        }

        scrollPane.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                contentBox.setPrefWidth(newVal.getWidth());
                contentBox.setPrefHeight(newVal.getHeight());
            }
        });

        bindPasswordToggle();

        installRealtimeValidation();
    }

    private void bindPasswordToggle() {
        if (passwordVisibleField != null) {
            passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        }

        if (showPasswordToggle != null) {
            showPasswordToggle.selectedProperty().addListener((obs, was, isSelected) -> {
                if (passwordVisibleField != null) {
                    passwordVisibleField.setVisible(isSelected);
                    passwordVisibleField.setManaged(isSelected);
                }
                passwordField.setVisible(!isSelected);
                passwordField.setManaged(!isSelected);

                updateEyeIcon();
            });

            updateEyeIcon();
        }
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
    public void goToLogin() {
        switchSceneWithAnimation("/User/fxml/login.fxml");
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

    private void installRealtimeValidation() {
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validateNameBasic(newVal);
            scheduleNameUniquenessCheck(newVal);
        });

        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validateEmailBasic(newVal);
            scheduleEmailUniquenessCheck(newVal);
        });

        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validatePhoneBasic(newVal);
            schedulePhoneUniquenessCheck(newVal);
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validatePassword(newVal);
        });

        roleComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            successMessage.setText("");
            validateRole(newVal);
        });
    }

    private boolean validateNameBasic(String name) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            nameError.setText("Full name is required");
            return false;
        }
        if (value.length() < 3) {
            nameError.setText("Minimum 3 characters");
            return false;
        }
        if ("Full name already exists".equals(nameError.getText())) {
            nameError.setText("");
        } else if (!nameError.getText().isEmpty() && !"Minimum 3 characters".equals(nameError.getText())) {
            nameError.setText("");
        }
        if (nameError.getText().isEmpty()) {
            nameError.setText("");
        }
        return true;
    }

    private void scheduleNameUniquenessCheck(String name) {
        nameDebounce.stop();
        if (!validateNameBasic(name)) {
            return;
        }

        String value = name.trim();
        nameDebounce.setOnFinished(e -> {
            if (userService.fullNameExists(value)) {
                nameError.setText("Full name already exists");
            } else if ("Full name already exists".equals(nameError.getText())) {
                nameError.setText("");
            }
        });
        nameDebounce.playFromStart();
    }

    private boolean validateEmailBasic(String email) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            emailError.setText("Email is required");
            return false;
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            emailError.setText("Invalid email format");
            return false;
        }

        if ("Email already exists".equals(emailError.getText())) {
            emailError.setText("");
        } else if (!emailError.getText().isEmpty() && !"Invalid email format".equals(emailError.getText())) {
            emailError.setText("");
        }
        return true;
    }

    private void scheduleEmailUniquenessCheck(String email) {
        emailDebounce.stop();
        if (!validateEmailBasic(email)) {
            return;
        }

        String value = email.trim();
        emailDebounce.setOnFinished(e -> {
            if (userService.emailExists(value)) {
                emailError.setText("Email already exists");
            } else if ("Email already exists".equals(emailError.getText())) {
                emailError.setText("");
            }
        });
        emailDebounce.playFromStart();
    }

    private boolean validatePhoneBasic(String phone) {
        String value = phone == null ? "" : phone.trim();
        if (value.isEmpty()) {
            phoneError.setText("Phone is required");
            return false;
        }

        String dial = extractDialCode(phoneCountryCodeCombo == null ? null : phoneCountryCodeCombo.getValue());
        try {
            PhoneUtil.normalizeToE164(dial, value);
        } catch (IllegalArgumentException e) {
            phoneError.setText("Invalid phone number");
            return false;
        }

        if ("Phone already exists".equals(phoneError.getText())) {
            phoneError.setText("");
        } else if (!phoneError.getText().isEmpty() && !"Invalid phone number".equals(phoneError.getText())) {
            phoneError.setText("");
        }
        return true;
    }

    private void schedulePhoneUniquenessCheck(String phone) {
        phoneDebounce.stop();
        if (!validatePhoneBasic(phone)) {
            return;
        }

        String dial = extractDialCode(phoneCountryCodeCombo == null ? null : phoneCountryCodeCombo.getValue());
        String value;
        try {
            value = PhoneUtil.normalizeToE164(dial, phone.trim());
        } catch (IllegalArgumentException e) {
            return;
        }
        phoneDebounce.setOnFinished(e -> {
            if (userService.phoneExists(value)) {
                phoneError.setText("Phone already exists");
            } else if ("Phone already exists".equals(phoneError.getText())) {
                phoneError.setText("");
            }
        });
        phoneDebounce.playFromStart();
    }

    private boolean validatePassword(String password) {
        String value = password == null ? "" : password.trim();
        if (value.isEmpty()) {
            passwordError.setText("Password is required");
            return false;
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            passwordError.setText("Password must have 1 uppercase, 1 lowercase, 1 digit, 1 special char");
            return false;
        }
        passwordError.setText("");
        return true;
    }

    private boolean validateRole(String role) {
        if (role == null) {
            roleError.setText("Role is required");
            return false;
        }
        roleError.setText("");
        return true;
    }

    @FXML
    public void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Photo");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(nameField.getScene().getWindow());
        if (file != null) {
            selectedPhotoPath = file.getAbsolutePath();
            photoLabel.setText(file.getName());
            photoPreview.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    public void handleSignup() {
        clearErrors();

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String titre = titreField == null || titreField.getText() == null ? "" : titreField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String phoneRaw = phoneField.getText() == null ? "" : phoneField.getText().trim();

        String password;
        if (showPasswordToggle != null && showPasswordToggle.isSelected() && passwordVisibleField != null) {
            password = passwordVisibleField.getText() == null ? "" : passwordVisibleField.getText();
        } else {
            password = passwordField.getText() == null ? "" : passwordField.getText();
        }

        String role = roleComboBox.getValue();

        boolean valid = true;
        if (!validateNameBasic(name)) valid = false;
        if (!validateEmailBasic(email)) valid = false;
        if (!validatePhoneBasic(phoneRaw)) valid = false;
        if (!validatePassword(password)) valid = false;
        if (!validateRole(role)) valid = false;
        if (!valid) return;

        String dial = extractDialCode(phoneCountryCodeCombo == null ? null : phoneCountryCodeCombo.getValue());
        String phone = PhoneUtil.normalizeToE164(dial, phoneRaw);

        User user = new User(name, titre, email, phone, selectedPhotoPath, password, role);

        boolean ok = userService.addUser(user);
        if (ok) {
            try {
                verificationService.sendVerificationCode(email, name);
            } catch (Exception e) {
                successMessage.setText("");
                emailError.setText(e.getMessage() == null ? "Error" : e.getMessage());
                return;
            }

            EmailVerificationContext.setEmail(email);
            successMessage.setText("Account created. Please verify your email");
            switchSceneWithAnimation("/User/fxml/verify_email.fxml");
        } else {
            successMessage.setText("");
        }
    }

    private void clearErrors() {
        nameError.setText(""); emailError.setText(""); phoneError.setText("");
        passwordError.setText(""); roleError.setText(""); successMessage.setText("");
    }

    private void clearFields() {
        nameField.clear(); emailField.clear(); phoneField.clear();
        passwordField.clear(); roleComboBox.setValue(null);
        photoLabel.setText("No file selected"); selectedPhotoPath = null;
        photoPreview.setImage(null);
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
}
