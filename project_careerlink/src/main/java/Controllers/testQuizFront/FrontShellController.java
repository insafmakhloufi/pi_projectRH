package Controllers.testQuizFront;

import Utils.front.FrontNavigator;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class FrontShellController implements Initializable {

    @FXML
    private VBox sidebar;
    @FXML
    private StackPane contentArea;
    @FXML
    private Button btnHome;
    @FXML
    private Button btnTests;

    private Button activeButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sidebar.setPrefWidth(82);
        setButtonsDisplay(ContentDisplay.GRAPHIC_ONLY);
        FrontNavigator.configure(contentArea);
        setActive(btnHome);
        showHome();
    }

    @FXML
    private void handleSidebarEntered() {
        sidebar.setPrefWidth(236);
        setButtonsDisplay(ContentDisplay.LEFT);
    }

    @FXML
    private void handleSidebarExited() {
        sidebar.setPrefWidth(82);
        setButtonsDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @FXML
    private void handleHome() {
        setActive(btnHome);
        showHome();
    }

    @FXML
    private void handleTests() {
        setActive(btnTests);
        FrontNavigator.showTestsList();
    }

    private void showHome() {
        contentArea.getChildren().clear();
    }

    private void setButtonsDisplay(ContentDisplay display) {
        if (btnHome != null) btnHome.setContentDisplay(display);
        if (btnTests != null) btnTests.setContentDisplay(display);
    }

    private void setActive(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        if (activeButton != null && !activeButton.getStyleClass().contains("active")) {
            activeButton.getStyleClass().add("active");
            ScaleTransition pulse = new ScaleTransition(Duration.millis(140), activeButton);
            pulse.setFromX(0.97);
            pulse.setFromY(0.97);
            pulse.setToX(1.0);
            pulse.setToY(1.0);
            pulse.play();
        }
    }
}
