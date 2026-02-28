package Controllers.Dashboard;

import Utils.Session;
import Utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.layout.VBox;

public class Dashboard_Front implements Initializable {

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnCandidature;

    @FXML
    private Button btnOffre;

    @FXML
    private Button btnEvenement;

    @FXML
    private Button btnFormations;

    @FXML
    private Button btnTest;

    @FXML
    private Button btnLogout;

    @FXML
    private StackPane contentArea;

    @FXML
    private StackPane pageContainer;

    @FXML
    private VBox sidebar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize sidebar logic
        sidebar.setPrefWidth(80);
        setButtonsContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        // Set default active button
        setActiveButton(btnDashboard);
    }
    @FXML
    private void openFormations() {
        loadCenter("/Formation/FormationFront.fxml");
    }

    private void loadCenter(String s) {
        try {
            URL resource = getClass().getResource(s);
            if (resource == null) {
                System.out.println("FXML file not found: " + s);
                return;
            }
            Parent root = new FXMLLoader(resource).load();
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(root);
            } else if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            Logger.getLogger(Dashboard_Front.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    @FXML
    private void handleSidebarEntered() {
        animateSidebar(250);
        setButtonsContentDisplay(ContentDisplay.LEFT);
    }

    @FXML
    private void handleSidebarExited() {
        animateSidebar(80);
        setButtonsContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    private void animateSidebar(double targetWidth) {
        // Simple animation using Timeline could go here, or just setPrefWidth for now for responsiveness
        // For smoother animation, we'd use a Transition. Let's stick to logic first.
        sidebar.setPrefWidth(targetWidth);
    }

    private void setButtonsContentDisplay(ContentDisplay display) {
        btnDashboard.setContentDisplay(display);
        btnCandidature.setContentDisplay(display);
        btnOffre.setContentDisplay(display);
        btnEvenement.setContentDisplay(display);
        btnFormations.setContentDisplay(display);
        btnTest.setContentDisplay(display);
        btnLogout.setContentDisplay(display);
    }
    
    private Button activeButton;

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        activeButton.getStyleClass().add("active");
    }

    @FXML
    private void handleDashboard() {
        setActiveButton(btnDashboard);
        loadPage("Dashboard.fxml");
    }

    @FXML
    private void handleCandidature() {
        setActiveButton(btnCandidature);
        // loadPage("Candidature.fxml"); // Use placeholder for now if file doesn't exist
        loadPage("Candidatures.fxml");
    }

    @FXML
    private void handleOffre() {
        setActiveButton(btnOffre);
        loadPage("offre.fxml"); // Mapping Offre to Portfolio
    }

    @FXML
    private void handleEvenement() {
        setActiveButton(btnEvenement);
        loadPage("Evenement.fxml"); // Mapping Evenement to Analytics
    }
    
    @FXML
    private void handleFormations() {
        setActiveButton(btnFormations);
        loadCenter("/Formation/FormationFront.fxml");
    }

    @FXML
    private void handleTest() {
        setActiveButton(btnTest);
        loadPage("Test.fxml"); // Mapping Test to Ideas
    }

    @FXML
    private void handleLogout() {
        Session.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            Stage stage = null;
            if (btnLogout != null && btnLogout.getScene() != null) {
                stage = (Stage) btnLogout.getScene().getWindow();
            } else if (pageContainer != null && pageContainer.getScene() != null) {
                stage = (Stage) pageContainer.getScene().getWindow();
            } else if (contentArea != null && contentArea.getScene() != null) {
                stage = (Stage) contentArea.getScene().getWindow();
            }
            if (stage == null) return;

            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (IOException e) {
            Logger.getLogger(Dashboard_Front.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void loadPage(String fxmlFile) {
        try {
            URL resource = getClass().getResource("/Dashboardfxml/" + fxmlFile);
            if (resource == null) {
                System.out.println("FXML file not found: " + fxmlFile);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(root);
            } else {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            Logger.getLogger(Dashboard_Front.class.getName()).log(Level.SEVERE, null, e);
        }
    }
}
