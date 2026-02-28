package Controllers.evenement;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.function.Consumer;

public final class Nav {

    private static StackPane defaultContentArea;

    private Nav() {
    }

    public static void setDefaultContentArea(StackPane contentArea) {
        defaultContentArea = contentArea;
    }

    public static StackPane getDefaultContentArea() {
        return defaultContentArea;
    }

    public static void go(StackPane contentArea, String fxml, Consumer<Object> init) {
        if (contentArea == null) {
            throw new IllegalStateException("contentArea is null");
        }
        try {
            FXMLLoader loader = new FXMLLoader(Nav.class.getResource(fxml));
            Node root = loader.load();
            Object controller = loader.getController();
            if (init != null) {
                init.accept(controller);
            }
            contentArea.getChildren().setAll(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxml + ": " + e.getMessage(), e);
        }
    }

    public static void go(String fxml, Consumer<Object> init) {
        if (defaultContentArea == null) {
            throw new IllegalStateException("Default contentArea not set");
        }
        go(defaultContentArea, fxml, init);
    }

    public static void go(String fxml) {
        go(fxml, null);
    }
}
