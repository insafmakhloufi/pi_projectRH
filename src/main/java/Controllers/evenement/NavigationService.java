package Controllers.evenement;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

public final class NavigationService {

    private static StackPane contentArea;

    private NavigationService() {
    }

    public static void init(StackPane area) {
        contentArea = area;
    }

    public static boolean hasContentArea() {
        return contentArea != null;
    }

    public static StackPane getContentArea() {
        return contentArea;
    }

    public static void go(String fxmlPath, Consumer<Object> initController) {
        if (contentArea == null) {
            throw new IllegalStateException("NavigationService not initialized: contentArea is null");
        }

        try {
            URL resource = NavigationService.class.getResource(fxmlPath);
            if (resource == null) {
                throw new IllegalArgumentException("FXML introuvable: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Node root = loader.load();
            Object controller = loader.getController();

            if (initController != null) {
                initController.accept(controller);
            }

            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlPath + ": " + e.getMessage(), e);
        }
    }

    public static void go(String fxmlPath) {
        go(fxmlPath, null);
    }
}
