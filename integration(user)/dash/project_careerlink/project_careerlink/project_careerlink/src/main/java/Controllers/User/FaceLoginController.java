package Controllers.User;

import Entites.User.User;
import Services.User.EmailVerificationService;
import Services.User.FaceAuthService;
import Services.User.TotpService;
import Services.User.UserService;
import Utils.Session;
import Utils.ThemeManager;
import Utils.TotpLoginContext;
import Utils.Mydatabase;
import Utils.OpenCvCascadeLoader;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class FaceLoginController {

    @FXML private Label hintLabel;
    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private VideoCapture capture;
    private volatile boolean running;
    private Thread cameraThread;

    private CascadeClassifier faceCascade;

    private final FaceAuthService faceAuthService = new FaceAuthService();
    private final UserService userService = new UserService();
    private final TotpService totpService = new TotpService();
    private final EmailVerificationService emailVerificationService = new EmailVerificationService(Mydatabase.getInstance().getConnection());

    private String identifier;

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
        if (hintLabel != null) {
            hintLabel.setText("Looking for face match for: " + (identifier == null ? "" : identifier));
        }
    }

    @FXML
    public void initialize() {
        String cascadePath = OpenCvCascadeLoader.loadFrontalFaceCascadePath();
        if (cascadePath != null && !cascadePath.isBlank()) {
            faceCascade = new CascadeClassifier(cascadePath);
        }
        if (hintLabel != null) {
            if (identifier == null || identifier.isBlank()) {
                hintLabel.setText("Face ID: Just show your face and click Verify. Or enter your email first for specific account.");
            } else {
                hintLabel.setText("Looking for face match for: " + identifier);
            }
        }
    }

    @FXML
    public void startCamera() {
        clearErrors();
        if (running) return;

        capture = new VideoCapture(0);
        if (!capture.isOpened()) {
            errorLabel.setText("Cannot open camera");
            return;
        }

        running = true;
        cameraThread = new Thread(() -> {
            Mat frame = new Mat();
            while (running) {
                if (!capture.read(frame) || frame.empty()) continue;
                Mat preview = frame.clone();

                Rect face = detectLargestFace(preview);
                if (face != null) {
                    rectangle(preview, face, new org.bytedeco.opencv.opencv_core.Scalar(79, 70, 229, 0), 2, 8, 0);
                }

                Image fxImage = MatFx.toImage(preview);
                javafx.application.Platform.runLater(() -> cameraView.setImage(fxImage));
                try {
                    Thread.sleep(33);
                } catch (InterruptedException ignored) {
                }
            }
        });
        cameraThread.setDaemon(true);
        cameraThread.start();
        if (statusLabel != null) statusLabel.setText("Camera started");
    }

    @FXML
    public void verify() {
        clearErrors();
        if (capture == null || !capture.isOpened()) {
            errorLabel.setText("Start camera first");
            return;
        }
        if (faceCascade == null) {
            errorLabel.setText("Missing face cascade. Add /opencv/haarcascade_frontalface_default.xml to resources or set OPENCV_HAAR_CASCADE_PATH");
            return;
        }

        Mat frame = new Mat();
        if (!capture.read(frame) || frame.empty()) {
            errorLabel.setText("Camera frame not available");
            return;
        }

        Rect face = detectLargestFace(frame);
        if (face == null) {
            errorLabel.setText("No face detected");
            return;
        }

        Mat faceMat = new Mat(frame, face);
        Mat gray = new Mat();
        cvtColor(faceMat, gray, COLOR_BGR2GRAY);
        Mat resized = new Mat();
        resize(gray, resized, new Size(160, 160));

        User user = null;

        // If identifier provided, look up specific user (traditional flow)
        if (identifier != null && !identifier.isBlank()) {
            user = userService.findUserByIdentifier(identifier);
            if (user == null) {
                errorLabel.setText("User not found");
                return;
            }
            if (!faceAuthService.verify(user.getId(), resized)) {
                errorLabel.setText("Face not recognized");
                return;
            }
        } else {
            // Face ID mode: find user by face match across all registered users
            int matchedUserId = faceAuthService.findUserByFaceMatch(resized);
            if (matchedUserId == -1) {
                errorLabel.setText("Face not recognized. Try entering your email first.");
                return;
            }
            user = userService.getUserById(matchedUserId);
            if (user == null) {
                errorLabel.setText("User lookup failed");
                return;
            }
        }

        if (!emailVerificationService.isVerified(user.getId())) {
            errorLabel.setText("Please verify your email before logging in");
            return;
        }

        // If TOTP is enabled, require it after face match.
        if (totpService.isEnabled(user.getId())) {
            stopCamera();
            TotpLoginContext.setUserId(user.getId());
            switchScene("/User/fxml/verify_totp_login.fxml");
            return;
        }

        stopCamera();
        Session.setCurrentUser(user);
        goToRoleDashboard(user);
    }

    private void goToRoleDashboard(User user) {
        String role = user.getRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            switchScene("/User/fxml/admin_dashboard.fxml");
        } else if ("CANDIDAT".equalsIgnoreCase(role)) {
            switchScene("/Dashboardfxml/Dashboard_Front.fxml");
        } else if ("MANAGER_RH".equalsIgnoreCase(role) || "MANAGERRH".equalsIgnoreCase(role)) {
            switchScene("/Dashboardfxml/Dashboard_Admin.fxml");
        } else {
            if (statusLabel != null) statusLabel.setText("Logged in successfully");
        }
    }

    private void switchScene(String fxml) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxml));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void close() {
        stopCamera();
        javafx.application.Platform.runLater(() -> {
            if (cameraView != null && cameraView.getScene() != null) {
                javafx.stage.Window window = cameraView.getScene().getWindow();
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    // If opened as a dialog, it will have an owner. Just hide/close the dialog.
                    if (stage.getOwner() != null) {
                        stage.hide();
                        return;
                    }

                    // If this screen was loaded into the main window by mistake, do NOT close the app.
                    // Navigate back to the login screen instead.
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
                        Scene scene = new Scene(loader.load());
                        ThemeManager.applyTheme(scene);
                        stage.setScene(scene);
                        stage.setMaximized(true);
                    } catch (Exception e) {
                        System.err.println("[FaceLoginController] Failed to navigate back to login on close");
                        e.printStackTrace();
                        // As a last resort, hide the stage rather than killing the whole process
                        stage.hide();
                    }
                }
            }
        });
    }

    private void stopCamera() {
        running = false;
        if (capture != null) {
            try { capture.release(); } catch (Exception ignored) {}
        }
    }

    private Rect detectLargestFace(Mat bgrFrame) {
        Mat gray = new Mat();
        cvtColor(bgrFrame, gray, COLOR_BGR2GRAY);
        RectVector faces = new RectVector();
        faceCascade.detectMultiScale(gray, faces);
        long count = faces.size();
        if (count <= 0) return null;

        Rect best = faces.get(0);
        long bestArea = (long) best.width() * best.height();
        for (int i = 1; i < count; i++) {
            Rect r = faces.get(i);
            long area = (long) r.width() * r.height();
            if (area > bestArea) {
                bestArea = area;
                best = r;
            }
        }
        return best;
    }

    private void clearErrors() {
        if (errorLabel != null) errorLabel.setText("");
        if (statusLabel != null) statusLabel.setText("");
    }
}
