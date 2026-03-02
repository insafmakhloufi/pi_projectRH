package Utils;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.net.URL;

public final class WindowUtil {

    private WindowUtil() {
    }

    public static void applyFullScreen(Stage stage) {
        if (stage == null) {
            return;
        }
        Runnable r = () -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            }
            stage.setMaximized(true);
        };

        if (Platform.isFxApplicationThread()) {
            Platform.runLater(r);
        } else {
            Platform.runLater(r);
        }
    }

    public static boolean loadInDashboardContent(Node anyNode, String fxmlPath, String title) {
        if (anyNode == null || anyNode.getScene() == null) {
            return false;
        }

        Scene scene = anyNode.getScene();
        Node target = scene.lookup("#contentArea");
        if (target == null) {
            target = scene.lookup("#adminContentArea");
        }
        if (!(target instanceof StackPane contentArea)) {
            return false;
        }

        try {
            URL url = resolveResource(fxmlPath);
            if (url == null) {
                return false;
            }

            Parent view = FXMLLoader.load(url);
            if (view instanceof Region r) {
                r.setMaxWidth(980);
                r.setPrefWidth(Region.USE_COMPUTED_SIZE);
                r.setMinWidth(0);
            }

            StackPane.setAlignment(view, Pos.TOP_CENTER);
            contentArea.getChildren().setAll(view);

            if (title != null) {
                Stage stage = (Stage) scene.getWindow();
                if (stage != null) {
                    stage.setTitle(title);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void navigate(Node anyNode, String fxmlPath, String title) {
        if (loadInDashboardContent(anyNode, fxmlPath, title)) {
            return;
        }

        try {
            URL url = resolveResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("FXML introuvable: " + fxmlPath);
            }

            Parent root = FXMLLoader.load(url);
            Scene scene = new Scene(root);
            Stage stage = (Stage) anyNode.getScene().getWindow();
            if (stage != null && title != null) {
                stage.setTitle(title);
            }
            if (stage != null) {
                stage.setScene(scene);
                applyFullScreen(stage);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static URL resolveResource(String fxmlPath) {
        if (fxmlPath == null || fxmlPath.isBlank()) {
            return null;
        }

        String normalized = fxmlPath.startsWith("/") ? fxmlPath.substring(1) : fxmlPath;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = (cl != null) ? cl.getResource(normalized) : null;
        if (url != null) {
            return url;
        }
        return WindowUtil.class.getClassLoader().getResource(normalized);
    }
}
