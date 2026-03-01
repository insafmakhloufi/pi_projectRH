package Controllers.Dashboard;

import Controllers.candidature.CopiloteVocalController;
import Controllers.evenement.Nav;
import Controllers.evenement.Navigation;
import Controllers.evenement.NavigationService;
import Controllers.messagerie.MessagerieController;
import Utils.Session;
import Utils.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.event.ActionEvent;

import java.net.URL;

public class Dashboard_Admin {

    @FXML private StackPane adminContentArea;
    @FXML private StackPane adminCenter;
    @FXML private CopiloteVocalController copiloteVocalController;

    @FXML private VBox sidebar;
    @FXML private Label lblDash;
    @FXML private Label lblCand;
    @FXML private Label lblEnt;
    @FXML private Label lblTests;
    @FXML private Label lblPubs;
    @FXML private Label lblForms;
    @FXML private Label lblOffres;
    @FXML private Label lblEntreprise;
    @FXML private Label lblLogout;

    private static final double SIDEBAR_COLLAPSED_W = 72;
    private static final double SIDEBAR_EXPANDED_W = 220;
    private Timeline sidebarAnim;
    private MessagerieController messagerieCtrl;

    @FXML
    private void initialize() {
        initEventNavigationTarget();

        if (sidebar != null) {
            sidebar.getStyleClass().add("expanded");
            sidebar.setPrefWidth(SIDEBAR_EXPANDED_W);
            sidebar.setMinWidth(SIDEBAR_EXPANDED_W);
            sidebar.setMaxWidth(SIDEBAR_EXPANDED_W);
        }
        setLabelsVisible(true);

        // Messagerie popup
        messagerieCtrl = new MessagerieController();
        messagerieCtrl.initPopup(adminCenter);

        // Copilote vocal
        if (copiloteVocalController != null) {
            copiloteVocalController.setNavigationContainer(adminContentArea);
            copiloteVocalController.setContexte("Dashboard RH Admin");
        }

        openDashboard(null);

        Platform.runLater(() -> {
            if (sidebar != null && sidebar.getScene() != null) {
                Stage currentStage = (Stage) sidebar.getScene().getWindow();
                if (currentStage != null) {
                    Utils.EntrepriseRequiredCheck.checkAndRedirect(currentStage);
                }
            }
        });
    }

    @FXML
    private void openCandidatures(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/candidaturefxml/RH_Candidatures.fxml");
    }

    @FXML
    private void openEntretiens(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/candidaturefxml/RH_Entretiens.fxml");
    }

    @FXML
    private void openDashboard(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/Dashboardfxml/Profile.fxml");
    }

    @FXML
    private void openTests(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/testQuiz/fxml/test_list.fxml");
    }

    @FXML
    private void openPublications(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/views_event/event_affichage_backend.fxml");
    }

    @FXML
    private void openFormations(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/Formation/AjouterFormation.fxml");
    }

    @FXML
    private void openFormations() {
        openFormations(null);
    }

    @FXML
    private void openOffres(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/Offre/AfficherOffre.fxml");
    }

    @FXML
    private void openEntreprise(ActionEvent e) {
        setActiveFromEvent(e);
        loadCenter("/Entreprise/MonEntreprise.fxml");
    }

    @FXML
    private void logout(ActionEvent e) {
        setActiveFromEvent(e);
        Session.clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            Stage stage = null;
            if (adminContentArea != null && adminContentArea.getScene() != null) {
                stage = (Stage) adminContentArea.getScene().getWindow();
            } else if (sidebar != null && sidebar.getScene() != null) {
                stage = (Stage) sidebar.getScene().getWindow();
            }
            if (stage == null) return;

            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void expandSidebar(MouseEvent e) {
        if (sidebar == null) return;
        sidebar.getStyleClass().add("expanded");
        animateSidebar(SIDEBAR_EXPANDED_W, true);
    }

    @FXML
    private void collapseSidebar(MouseEvent e) {
        if (sidebar == null) return;

        // init (appel depuis initialize)
        if (e == null) {
            sidebar.getStyleClass().remove("expanded");
            sidebar.setPrefWidth(SIDEBAR_COLLAPSED_W);
            sidebar.setMinWidth(SIDEBAR_COLLAPSED_W);
            sidebar.setMaxWidth(SIDEBAR_COLLAPSED_W);
            setLabelsVisible(false);
            return;
        }

        sidebar.getStyleClass().remove("expanded");
        animateSidebar(SIDEBAR_COLLAPSED_W, false);
    }

    private void animateSidebar(double targetW, boolean expanding) {
        if (sidebarAnim != null) sidebarAnim.stop();

        if (expanding) setLabelsVisible(true);

        sidebarAnim = new Timeline(
                new KeyFrame(Duration.millis(220),
                        new KeyValue(sidebar.prefWidthProperty(), targetW),
                        new KeyValue(sidebar.minWidthProperty(),  targetW),
                        new KeyValue(sidebar.maxWidthProperty(),  targetW)
                )
        );

        sidebarAnim.setOnFinished(ev -> {
            if (!expanding) setLabelsVisible(false);
        });

        sidebarAnim.play();
    }


    private void setLabelsVisible(boolean visible) {
        Label[] labels = new Label[]{lblDash, lblCand, lblEnt, lblTests, lblPubs, lblForms, lblOffres, lblEntreprise, lblLogout};
        for (Label l : labels) {
            if (l == null) continue;
            l.setVisible(visible);
            l.setManaged(visible);
        }
    }

    private void setActiveFromEvent(ActionEvent e) {
        if (e == null) return;
        Object src = e.getSource();
        if (!(src instanceof Button btn)) return;
        setActive(btn);
    }

    private void setActive(Button active) {
        if (sidebar == null || active == null) return;
        for (var n : sidebar.getChildren()) {
            if (n instanceof Button b) {
                b.getStyleClass().remove("admin-rail-active");
            }
        }
        if (!active.getStyleClass().contains("admin-rail-active")) {
            active.getStyleClass().add("admin-rail-active");
        }
    }

    private void loadPlaceholder(String title) {
        Label l = new Label(title);
        l.getStyleClass().add("admin-error");
        adminContentArea.getChildren().setAll(l);
        StackPane.setAlignment(l, Pos.TOP_LEFT);
    }

    private void loadCenter(String fxmlPath) {
        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                loadPlaceholder("Écran introuvable: " + fxmlPath);
                return;
            }

            Parent view = FXMLLoader.load(url);
            if (view instanceof Region region) {
                region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            }
            StackPane.setAlignment(view, Pos.TOP_LEFT);
            adminContentArea.getChildren().setAll(view);
            initEventNavigationTarget();
        } catch (Exception e) {
            e.printStackTrace();

            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String rootMsg = root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage());

            Label fallback = new Label("Erreur de chargement: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            fallback.getStyleClass().add("admin-error");
            adminContentArea.getChildren().setAll(fallback);
            StackPane.setAlignment(fallback, Pos.TOP_LEFT);

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible de charger l'écran RH");
            alert.setContentText(rootMsg);
            alert.show();
        }
    }

    private void initEventNavigationTarget() {
        if (adminContentArea == null) {
            return;
        }
        NavigationService.init(adminContentArea);
        Nav.setDefaultContentArea(adminContentArea);
        Navigation.setContentArea(adminContentArea);
    }
}
