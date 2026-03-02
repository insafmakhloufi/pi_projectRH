package Controllers.evenement;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class Navigation {

    private static StackPane contentArea;

    public static void setContentArea(StackPane area) {
        contentArea = area;
    }

    public static boolean hasContentArea() {
        return contentArea != null;
    }

    public static void setContent(Parent root) {
        if (contentArea == null) {
            throw new IllegalStateException("Navigation content area not set.");
        }

        contentArea.getChildren().setAll(root);

        FadeTransition fade = new FadeTransition(Duration.millis(180), root);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    public static void navigate(String fxmlPath) {
        if (contentArea == null) {
            throw new IllegalStateException("Navigation content area not set.");
        }

        try {
            FXMLLoader loader = new FXMLLoader(Navigation.class.getResource(fxmlPath));
            Parent root = loader.load();

            setContent(root);

        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Erreur");
            a.setHeaderText("Impossible de charger: " + fxmlPath);
            a.setContentText(String.valueOf(e.getMessage()));
            a.showAndWait();
            throw new RuntimeException("Failed to navigate to " + fxmlPath + ": " + e.getMessage(), e);
        }
    }
}
