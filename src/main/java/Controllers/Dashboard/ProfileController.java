package Controllers.Dashboard;

import Entities.User.User;
import Services.User.FaceAuthService;
import Services.User.TotpService;
import Services.User.UserService;
import Utils.Session;
import Utils.ThemeManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.io.File;

public class ProfileController {

    @FXML private Circle avatarCircle;
    @FXML private ImageView avatarImage;
    @FXML private Label namePreviewLabel;
    @FXML private Label rolePreviewLabel;
    @FXML private Label statusLabel;

    @FXML private TextField fullNameField;
    @FXML private TextField titreField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;

    @FXML private ImageView photoPreview;
    @FXML private Label photoHintLabel;
    @FXML private TextField photoPathField;

    @FXML private Label totpStatusLabel;
    @FXML private ImageView totpQrImage;
    @FXML private TextField totpCodeField;
    @FXML private Label totpErrorLabel;
    @FXML private Label totpActionStatusLabel;
    @FXML private Button totpEnableButton;
    @FXML private Button totpDisableButton;

    @FXML private Label faceStatusLabel;
    @FXML private Button faceDisableButton;

    private final UserService userService = new UserService();
    private final TotpService totpService = new TotpService();
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    public void initialize() {
        loadFromSession();
        refreshTotpUi();
        refreshFaceUi();
    }

    private void loadFromSession() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        if (fullNameField != null) fullNameField.setText(nvl(current.getFullName()));
        if (titreField != null) titreField.setText(nvl(current.getTitre()));
        if (emailField != null) emailField.setText(nvl(current.getEmail()));
        if (phoneField != null) phoneField.setText(nvl(current.getPhone()));
        if (photoPathField != null) photoPathField.setText(nvl(current.getProfilePhoto()));

        if (namePreviewLabel != null) namePreviewLabel.setText(nvl(current.getFullName()));
        if (rolePreviewLabel != null) rolePreviewLabel.setText(nvl(current.getRole()));
        if (statusLabel != null) statusLabel.setText("");

        updatePhotoPreviewFromPath(nvl(current.getProfilePhoto()));
        updateAvatarImage(nvl(current.getProfilePhoto()));
    }

    private void updateAvatarImage(String path) {
        Image img = loadImageFromPath(path);
        if (avatarImage != null) avatarImage.setImage(img);
        if (avatarCircle != null) avatarCircle.setVisible(img == null);
    }

    private void updatePhotoPreviewFromPath(String path) {
        Image img = loadImageFromPath(path);
        if (photoPreview != null) photoPreview.setImage(img);
        if (photoHintLabel != null) {
            if (path == null || path.isBlank()) {
                photoHintLabel.setText("No file selected");
            } else {
                File f = new File(path);
                photoHintLabel.setText(f.exists() ? f.getName() : path);
            }
        }
    }

    private Image loadImageFromPath(String path) {
        try {
            if (path == null || path.isBlank()) return null;
            File f = new File(path);
            if (!f.exists()) return null;
            return new Image(f.toURI().toString(), false);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    public void choosePhoto() {
        Window owner = getWindow();
        if (owner == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose profile photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        File file = chooser.showOpenDialog(owner);
        if (file == null) return;

        if (photoPathField != null) photoPathField.setText(file.getAbsolutePath());
        updatePhotoPreviewFromPath(file.getAbsolutePath());
        updateAvatarImage(file.getAbsolutePath());
    }

    @FXML
    public void clearPhoto() {
        if (photoPathField != null) photoPathField.setText("");
        updatePhotoPreviewFromPath("");
        updateAvatarImage("");
    }

    @FXML
    public void saveProfile() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        String fullName = fullNameField != null ? fullNameField.getText() : current.getFullName();
        String titre = titreField != null ? titreField.getText() : current.getTitre();
        String email = emailField != null ? emailField.getText() : current.getEmail();
        String phone = phoneField != null ? phoneField.getText() : current.getPhone();
        String photo = photoPathField != null ? photoPathField.getText() : current.getProfilePhoto();

        User updated = new User(
                current.getId(),
                fullName,
                titre,
                email,
                phone,
                photo,
                current.getPassword(),
                current.getRole()
        );

        boolean ok = userService.updateUser(updated);
        if (!ok) {
            if (statusLabel != null) statusLabel.setText("Unable to save changes");
            return;
        }

        current.setFullName(fullName);
        current.setTitre(titre);
        current.setEmail(email);
        current.setPhone(phone);
        current.setProfilePhoto(photo);

        if (namePreviewLabel != null) namePreviewLabel.setText(nvl(current.getFullName()));
        if (rolePreviewLabel != null) rolePreviewLabel.setText(nvl(current.getRole()));
        updatePhotoPreviewFromPath(nvl(current.getProfilePhoto()));
        updateAvatarImage(nvl(current.getProfilePhoto()));
        if (statusLabel != null) statusLabel.setText("Saved successfully");
    }

    @FXML
    public void refreshTotpQr() {
        refreshTotpUi();
    }

    @FXML
    public void enableTotp() {
        clearTotpMessages();
        User current = Session.getCurrentUser();
        if (current == null) return;

        String code = totpCodeField == null ? "" : totpCodeField.getText();
        if (code == null || !code.trim().matches("\\d{6}")) {
            if (totpErrorLabel != null) totpErrorLabel.setText("Code must be 6 digits");
            return;
        }

        boolean ok = totpService.verifyCode(current.getId(), code);
        if (!ok) {
            if (totpErrorLabel != null) totpErrorLabel.setText("Invalid code");
            return;
        }

        totpService.enable(current.getId());
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("2FA enabled");
        refreshTotpUi();
    }

    @FXML
    public void disableTotp() {
        clearTotpMessages();
        User current = Session.getCurrentUser();
        if (current == null) return;

        totpService.disable(current.getId());
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("2FA disabled");
        refreshTotpUi();
    }

    private void refreshTotpUi() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        boolean enabled = totpService.isEnabled(current.getId());
        if (totpStatusLabel != null) totpStatusLabel.setText(enabled ? "Enabled" : "Not enabled");
        if (totpEnableButton != null) totpEnableButton.setDisable(enabled);
        if (totpDisableButton != null) totpDisableButton.setDisable(!enabled);
        if (totpCodeField != null) totpCodeField.setDisable(enabled);

        String secret = totpService.getOrCreateSecretBase32(current.getId());
        String uri = totpService.buildOtpAuthUri("CareerLink", current.getEmail(), secret);
        if (totpQrImage != null) totpQrImage.setImage(buildQrImage(uri, 220, 220));
    }

    private Image buildQrImage(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            return null;
        }
    }

    private void clearTotpMessages() {
        if (totpErrorLabel != null) totpErrorLabel.setText("");
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("");
    }

    @FXML
    public void openFaceEnroll() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/face_enroll.fxml"));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);

            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);

            Window owner = getWindow();
            if (owner != null) {
                dialog.initOwner(owner);
            }

            dialog.setTitle("Register Face");
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();

            refreshFaceUi();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("Face enroll failed");
        }
    }

    @FXML
    public void disableFaceLogin() {
        User current = Session.getCurrentUser();
        if (current == null) return;
        faceAuthService.disable(current.getId());
        refreshFaceUi();
    }

    private void refreshFaceUi() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        boolean enabled = faceAuthService.isEnabled(current.getId());
        boolean hasModel = faceAuthService.hasModel(current.getId());

        if (faceStatusLabel != null) {
            faceStatusLabel.setText(enabled && hasModel ? "Enabled" : (hasModel ? "Registered" : "Not enabled"));
        }
        if (faceDisableButton != null) {
            faceDisableButton.setDisable(!enabled);
        }
    }

    private Window getWindow() {
        if (fullNameField != null && fullNameField.getScene() != null) return fullNameField.getScene().getWindow();
        if (statusLabel != null && statusLabel.getScene() != null) return statusLabel.getScene().getWindow();
        return null;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
