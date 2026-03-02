package Controllers.Dashboard;

import Controllers.candidature.CopiloteVocalController;
import Controllers.evenement.Nav;
import Controllers.evenement.Navigation;
import Controllers.evenement.NavigationService;
import Controllers.messagerie.MessagerieController;
import Utils.front.FrontNavigator;
import Utils.Session;
import Utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
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
    private Button btnTestQuiz;

    @FXML
    private Button btnLogout;

    @FXML
    private StackPane contentArea;

    @FXML
    private StackPane pageContainer;

    @FXML
    private VBox sidebar;
    @FXML
    private CopiloteVocalController copiloteVocalController;

    private MessagerieController messagerieCtrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initEventNavigationTarget();

        // Initialize sidebar logic
        sidebar.setPrefWidth(80);
        setButtonsContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        
        // Set default active button
        setActiveButton(btnDashboard);

        // Messagerie popup
        messagerieCtrl = new MessagerieController();
        messagerieCtrl.initPopup(contentArea);

        // Copilote vocal
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
        if (btnDashboard != null) btnDashboard.setContentDisplay(display);
        if (btnCandidature != null) btnCandidature.setContentDisplay(display);
        if (btnOffre != null) btnOffre.setContentDisplay(display);
        if (btnEvenement != null) btnEvenement.setContentDisplay(display);
        if (btnFormations != null) btnFormations.setContentDisplay(display);
        if (btnTest != null) btnTest.setContentDisplay(display);
        if (btnTestQuiz != null) btnTestQuiz.setContentDisplay(display);
        if (btnLogout != null) btnLogout.setContentDisplay(display);
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
        loadPage("/candidaturefxml/MesCandidatures.fxml");
    }

    @FXML
    private void handleOffre() {
        setActiveButton(btnOffre);
        loadPage("/Offre/AfficherOffresFront.fxml");
    }

    @FXML
    private void handleEvenement() {
        setActiveButton(btnEvenement);
        loadPage("/views_event/event_affichage.fxml"); // Mapping Evenement to Analytics
    }
    
    @FXML
    private void handleFormations() {
        setActiveButton(btnFormations);
        loadIntoContent("/Formation/FormationFront.fxml");
    }

    @FXML
    private void handleTest() {
        handleTestQuiz();
    }

    @FXML
    private void handleTestQuiz() {
        setActiveButton(btnTestQuiz != null ? btnTestQuiz : btnTest);
        if (pageContainer == null) {
            return;
        }
        FrontNavigator.configure(pageContainer);
        FrontNavigator.showTestsList();
    }

    private void loadIntoContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (root instanceof Region region) {
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(root);
            } else {
                contentArea.getChildren().setAll(root);
            }
            initEventNavigationTarget();
        } catch (IOException e) {
            Logger.getLogger(Dashboard_Front.class.getName()).log(Level.SEVERE, null, e);
        }
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
            String path = (fxmlFile != null && fxmlFile.startsWith("/"))
                    ? fxmlFile
                    : "/Dashboardfxml/" + fxmlFile;
            URL resource = getClass().getResource(path);
            if (resource == null) {
                System.out.println("FXML file not found: " + path);
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(root);
            } else {
                contentArea.getChildren().setAll(root);
            }
            initEventNavigationTarget();
        } catch (IOException e) {
            Logger.getLogger(Dashboard_Front.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    private void initEventNavigationTarget() {
        StackPane target = pageContainer != null ? pageContainer : contentArea;
        if (target == null) {
            return;
        }
        NavigationService.init(target);
        Nav.setDefaultContentArea(target);
        Navigation.setContentArea(target);
    }
}
