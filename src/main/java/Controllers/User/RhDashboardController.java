package Controllers;

import Entities.User;
import Utils.Session;
import Utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class RhDashboardController {

    @FXML private Label helloLabel;

    @FXML
    public void initialize() {
        User current = Session.getCurrentUser();
        if (current == null) {
            goToLogin();
            return;
        }

        if (helloLabel != null) {
            helloLabel.setText("Hello " + current.getFullName());
        }
    }

    @FXML
    public void logout() {
        Session.clear();
        goToLogin();
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);

            Stage stage = null;
            if (helloLabel != null && helloLabel.getScene() != null) {
                stage = (Stage) helloLabel.getScene().getWindow();
            }
            if (stage == null) return;

            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
