package Controllers.Offre;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class MainLayout {

    @FXML
    private StackPane contentPane;

    @FXML
    private BorderPane rootPane;

    @FXML
    private Button btnDashboard;

    @FXML
    private Button btnUtilisateurs;

    @FXML
    private Button btnAjouterOffre;

    @FXML
    private Button btnOffres;

    @FXML
    private Button btnCandidatures;

    @FXML
    private Button btnEntretiens;

    @FXML
    private Button btnPublications;

    @FXML
    private Button btnMessagerie;

    @FXML
    private Button btnEntreprise;

    @FXML
    private Button btnLogout;

    @FXML
    private VBox sidebarRail;

    private List<Button> navButtons;

    private static final double DRAWER_COLLAPSED_WIDTH = 76;
    private static final double DRAWER_EXPANDED_WIDTH = 220;
    private static final Duration DRAWER_ANIM_DURATION = Duration.millis(180);
    private Timeline drawerTimeline;

    private boolean darkMode;

    @FXML
    void initialize() {
        navButtons = List.of(
                btnDashboard,
                btnUtilisateurs,
                btnAjouterOffre,
                btnOffres,
                btnCandidatures,
                btnEntretiens,
                btnPublications,
                btnMessagerie,
                btnEntreprise,
                btnLogout
        );

        setupDrawer();

        goAjouterOffre(null);
    }

    @FXML
    void toggleDarkMode(ActionEvent event) {
        darkMode = !darkMode;
        if (rootPane == null) {
            return;
        }
        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark")) {
                rootPane.getStyleClass().add("dark");
            }
        } else {
            rootPane.getStyleClass().remove("dark");
        }
    }

    @FXML
    void expandSidebar() {
        expandDrawer();
    }

    @FXML
    void collapseSidebar() {
        collapseDrawer();
    }

    private void setupDrawer() {
        if (sidebarRail == null) {
            return;
        }

        sidebarRail.setPrefWidth(DRAWER_COLLAPSED_WIDTH);
        sidebarRail.setMinWidth(DRAWER_COLLAPSED_WIDTH);
        sidebarRail.setMaxWidth(DRAWER_COLLAPSED_WIDTH);
        if (!sidebarRail.getStyleClass().contains("collapsed")) {
            sidebarRail.getStyleClass().add("collapsed");
        }

        sidebarRail.setOnMouseEntered(e -> expandDrawer());
        sidebarRail.setOnMouseExited(e -> collapseDrawer());
    }

    private void animateDrawerTo(double targetWidth, boolean collapsed) {
        if (sidebarRail == null) {
            return;
        }

        if (drawerTimeline != null) {
            drawerTimeline.stop();
        }

        drawerTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(sidebarRail.prefWidthProperty(), sidebarRail.getPrefWidth(), Interpolator.EASE_BOTH)
                ),
                new KeyFrame(DRAWER_ANIM_DURATION,
                        new KeyValue(sidebarRail.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH)
                )
        );
        drawerTimeline.currentTimeProperty().addListener((obs, o, n) -> {
            double w = sidebarRail.getPrefWidth();
            sidebarRail.setMinWidth(w);
            sidebarRail.setMaxWidth(w);
        });
        drawerTimeline.setOnFinished(evt -> {
            sidebarRail.setMinWidth(targetWidth);
            sidebarRail.setMaxWidth(targetWidth);
            if (collapsed) {
                if (!sidebarRail.getStyleClass().contains("collapsed")) {
                    sidebarRail.getStyleClass().add("collapsed");
                }
            } else {
                sidebarRail.getStyleClass().remove("collapsed");
            }
        });
        drawerTimeline.playFromStart();
    }

    private void expandDrawer() {
        animateDrawerTo(DRAWER_EXPANDED_WIDTH, false);
    }

    private void collapseDrawer() {
        animateDrawerTo(DRAWER_COLLAPSED_WIDTH, true);
    }

    @FXML
    void goDashboard(ActionEvent event) {
        showPlaceholder("Dashboard");
        select(btnDashboard);
    }

    @FXML
    void goUtilisateurs(ActionEvent event) {
        showPlaceholder("Utilisateurs");
        select(btnUtilisateurs);
    }

    @FXML
    void goAjouterOffre(ActionEvent event) {
        loadPage("/Offre/AjouterOffre.fxml");
        select(btnAjouterOffre);
    }

    @FXML
    void goOffres(ActionEvent event) {
        loadPage("/Offre/AfficherOffre.fxml");
        select(btnOffres);
    }

    @FXML
    void goCandidatures(ActionEvent event) {
        showPlaceholder("Candidatures");
        select(btnCandidatures);
    }

    @FXML
    void goEntretiens(ActionEvent event) {
        showPlaceholder("Entretiens");
        select(btnEntretiens);
    }

    @FXML
    void goPublications(ActionEvent event) {
        showPlaceholder("Publications");
        select(btnPublications);
    }

    @FXML
    void goMessagerie(ActionEvent event) {
        showPlaceholder("Messagerie");
        select(btnMessagerie);
    }

    @FXML
    void goEntreprise(ActionEvent event) {
        loadPage("/Entreprise/MonEntreprise.fxml");
        select(btnEntreprise);
    }

    @FXML
    void logout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Logout");
        alert.setHeaderText(null);
        alert.setContentText("Logout");
        alert.showAndWait();
    }

    private void loadPage(String fxmlPath) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource(fxmlPath));
            contentPane.getChildren().setAll(page);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            String msg = e.getClass().getSimpleName() + ": " + String.valueOf(e.getMessage());
            if (root != null && root != e) {
                msg += "\nCause: " + root.getClass().getSimpleName() + ": " + String.valueOf(root.getMessage());
            }
            msg += "\nFXML: " + fxmlPath;
            alert.setContentText(msg);
            alert.showAndWait();
        }
    }

    private void showPlaceholder(String name) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(name);
        alert.setHeaderText(null);
        alert.setContentText("Page '" + name + "' non implémentée.");
        alert.showAndWait();
    }

    private void select(Button selected) {
        if (navButtons == null) {
            return;
        }
        for (Button b : navButtons) {
            if (b == null) {
                continue;
            }
            b.getStyleClass().remove("nav-selected");
            b.getStyleClass().remove("admin-rail-active");
        }
        if (selected != null && !selected.getStyleClass().contains("admin-rail-active")) {
            selected.getStyleClass().add("admin-rail-active");
        }
    }
}
