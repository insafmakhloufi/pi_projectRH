package Controllers.User;

import Services.User.FaceAuthService;
import Utils.OpenCvCascadeLoader;
import Utils.Session;
import Utils.ThemeManager;
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
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.rectangle;

public class FaceEnrollController {

    @FXML private ImageView cameraView;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;

    private VideoCapture capture;
    private volatile boolean running;
    private Thread cameraThread;

    private final List<Mat> samplesGray = new ArrayList<>();

    private CascadeClassifier faceCascade;
    private final FaceAuthService faceAuthService = new FaceAuthService();

    @FXML
    public void initialize() {
        String cascadePath = OpenCvCascadeLoader.loadFrontalFaceCascadePath();
        System.out.println("[FaceEnrollController] Cascade path: " + cascadePath);
        if (cascadePath != null && !cascadePath.isBlank()) {
            try {
                faceCascade = new CascadeClassifier(cascadePath);
                if (faceCascade.empty()) {
                    System.err.println("[FaceEnrollController] Cascade classifier is empty - file may be corrupt");
                    faceCascade = null;
                    if (errorLabel != null) errorLabel.setText("Face detection model failed to load");
                } else {
                    System.out.println("[FaceEnrollController] Cascade loaded successfully");
                }
            } catch (Exception e) {
                System.err.println("[FaceEnrollController] Error loading cascade: " + e.getMessage());
                faceCascade = null;
                if (errorLabel != null) errorLabel.setText("Face detection error: " + e.getMessage());
            }
        } else {
            System.err.println("[FaceEnrollController] No cascade path available");
            if (errorLabel != null) errorLabel.setText("Face detection model not found");
        }
        if (statusLabel != null) statusLabel.setText("Samples: 0");
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
        statusLabel.setText("Camera started. Samples: " + samplesGray.size());
    }

    @FXML
    public void captureSample() {
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

        samplesGray.add(resized);
        statusLabel.setText("Samples: " + samplesGray.size());
    }

    @FXML
    public void saveModel() {
        clearErrors();
        var current = Session.getCurrentUser();
        if (current == null) {
            errorLabel.setText("Not logged in");
            return;
        }
        if (samplesGray.size() < 5) {
            errorLabel.setText("Capture at least 5 samples");
            return;
        }

        org.bytedeco.opencv.opencv_core.MatVector faces = new org.bytedeco.opencv.opencv_core.MatVector(samplesGray.size());
        for (int i = 0; i < samplesGray.size(); i++) {
            faces.put(i, samplesGray.get(i));
        }

        Mat labels = FaceAuthService.buildLabels(samplesGray.size());
        LBPHFaceRecognizer recognizer = FaceAuthService.trainPerUser(faces, labels);

        faceAuthService.saveModel(current.getId(), recognizer);
        faceAuthService.enable(current.getId());
        statusLabel.setText("Face registered and enabled");
        samplesGray.clear();
    }

    @FXML
    public void close() {
        stopCamera();
        javafx.application.Platform.runLater(() -> {
            if (cameraView != null && cameraView.getScene() != null) {
                javafx.stage.Window window = cameraView.getScene().getWindow();
                if (window instanceof Stage) {
                    Stage stage = (Stage) window;
                    System.out.println("[FaceEnrollController] Close clicked. stage=" + stage + ", owner=" + stage.getOwner());
                    // If opened as a dialog, it will have an owner. Just hide/close the dialog.
                    if (stage.getOwner() != null) {
                        stage.hide();
                        return;
                    }

                    // If this screen was loaded into the main window by mistake, do NOT close the app.
                    // Navigate back to the dashboard instead.
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/admin_dashboard.fxml"));
                        Scene scene = new Scene(loader.load());
                        ThemeManager.applyTheme(scene);
                        stage.setScene(scene);
                        stage.setMaximized(true);
                    } catch (Exception e) {
                        System.err.println("[FaceEnrollController] Failed to navigate back to dashboard on close");
                        e.printStackTrace();
                        if (errorLabel != null) {
                            errorLabel.setText("Unable to close this screen. Check console logs.");
                        }
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
        if (faceCascade == null) return null;
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
    }
}
