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
import javafx.scene.control.Label;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.layout.VBox;



//jdoddddd notification
import Controllers.messagerie.MessagerieController;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.scene.shape.Circle;



//jdiddd copilottt
import Controllers.candidature.CopiloteVocalController;

//jdidddd
import javafx.scene.layout.Pane;



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

    //notificationnnn
    private MessagerieController messagerieCtrl;

    // Ajoute ce champ avec les autres @FXML copilottt jdiddddddd
    @FXML private CopiloteVocalController copiloteVocalController;



    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize sidebar logic
        sidebar.setPrefWidth(80);
        setButtonsContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        // Set default active button
        setActiveButton(btnDashboard);

        //notification
        // Initialiser la messagerie popup
        messagerieCtrl = new MessagerieController();
        messagerieCtrl.initPopup(contentArea);

        //copiloteee jdiddd

        // Connecter copilote au conteneur de navigation
        if (copiloteVocalController != null) {
            Pane container = pageContainer != null ? pageContainer : contentArea;
            copiloteVocalController.setNavigationContainer(container);
            copiloteVocalController.setContexte("Dashboard Candidat");
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
        showMissingView("Dashboard", "Dashboard.fxml introuvable dans /Dashboardfxml");
    }

    @FXML
    private void handleCandidature() {
        setActiveButton(btnCandidature);
        loadView("/candidaturefxml/MesCandidatures.fxml");
    }

    @FXML
    private void handleOffre() {
        setActiveButton(btnOffre);
        loadPage("offre.fxml"); // Mapping Offre to Portfolio
    }

    @FXML
    private void handleEvenement() {
        setActiveButton(btnEvenement);
        loadView("/candidaturefxml/Evenement.fxml");
    }
    
    @FXML
    private void handleFormations() {
        setActiveButton(btnFormations);
        loadPage("Formation.fxml"); // Mapping Formations to Documents
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
            URL resource = fxmlFile != null && fxmlFile.startsWith("/")
                    ? getClass().getResource(fxmlFile)
                    : getClass().getResource("/Dashboardfxml/" + fxmlFile);
            if (resource == null) {
                showMissingView("Navigation", "FXML file not found: " + fxmlFile);
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
            showMissingView("Navigation", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void loadView(String resourcePath) {
        try {
            URL resource = getClass().getResource(resourcePath);
            if (resource == null) {
                showMissingView("Navigation", "FXML not found: " + resourcePath);
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
            showMissingView("Navigation", "Erreur de chargement: " + e.getMessage());
        }
    }

    private void showMissingView(String title, String details) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        Label d = new Label(details);
        d.setWrapText(true);
        d.setStyle("-fx-text-fill: #334155;");

        VBox box = new VBox(8, t, d);
        box.setPadding(new Insets(18));
        box.setAlignment(Pos.TOP_LEFT);
        box.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: rgba(15,23,42,0.12);");

        if (pageContainer != null) {
            pageContainer.getChildren().setAll(box);
        } else if (contentArea != null) {
            contentArea.getChildren().setAll(box);
        }
    }
}
