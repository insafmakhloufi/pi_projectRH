package Controllers.Formation;

import Entites.Formation.Chapitre;
import Entites.Formation.Cour;
import Services.Formation.ChapitreServices;
import Services.Formation.CourServices;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class AfficherChapitre {

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    @FXML private Button btnRetour;
    @FXML private Button btnAjouter;
    @FXML private Label lblTitre;
    @FXML private Label lblCompteur;
    @FXML private FlowPane cardsFlow;

    private final ChapitreServices chapitreServices = new ChapitreServices();
    private final CourServices courServices = new CourServices();

    @FXML
    void initialize() {
        if (btnRetour != null) {
            btnRetour.setOnAction(e -> retour());
        }
        if (btnAjouter != null) {
            btnAjouter.setOnAction(e -> ouvrirAjouter());
        }
        refresh();
    }

    private void refresh() {
        if (UiState.selectedCourId == null) {
            if (cardsFlow != null) {
                cardsFlow.getChildren().clear();
            }
            if (lblCompteur != null) {
                lblCompteur.setText("Chapitres: 0/0");
            }
            if (btnAjouter != null) {
                btnAjouter.setDisable(true);
            }
            return;
        }

        int courId = UiState.selectedCourId;
        Cour cour = courServices.getCourById(courId);
        int max = (cour == null) ? 0 : cour.getNbChapitres();
        int current = chapitreServices.countByCour(courId);

        if (lblTitre != null) {
            String t = UiState.selectedCourTitre;
            if (t == null || t.isBlank()) {
                t = (cour == null) ? "Cours" : cour.getNomFormateur();
            }
            lblTitre.setText("Chapitres - " + t);
        }

        if (lblCompteur != null) {
            lblCompteur.setText("Chapitres: " + current + "/" + max);
        }

        if (btnAjouter != null) {
            btnAjouter.setDisable(max <= 0 || current >= max);
        }

        if (cardsFlow == null) {
            return;
        }

        List<Chapitre> all = chapitreServices.afficherChapitresParCour(courId);
        ObservableList<Chapitre> list = FXCollections.observableArrayList(all);

        cardsFlow.getChildren().clear();
        for (Chapitre ch : list) {
            cardsFlow.getChildren().add(createCard(ch));
        }
    }

    private VBox createCard(Chapitre ch) {
        VBox card = new VBox(10);
        card.getStyleClass().add("formation-card");

        Label title = new Label(ch.getOrdre() + ". " + (ch.getTitre() == null ? "" : ch.getTitre()));
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);

        String contenu = ch.getContenu();
        boolean isPdf = contenu != null && (contenu.startsWith(PDF_PREFIX) || contenu.startsWith(PDFS_PREFIX));
        Label meta = new Label(isPdf ? "PDF" : "Texte");
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnVoir = new Button("Voir");
        btnVoir.getStyleClass().add("btn-ghost");
        btnVoir.setFocusTraversable(false);
        btnVoir.setOnAction(e -> ouvrirDetail(ch));

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("btn-ghost");
        btnModifier.setFocusTraversable(false);
        btnModifier.setOnAction(e -> ouvrirModifier(ch));

        Button btnSupprimer = new Button("Supprimer");
        btnSupprimer.getStyleClass().add("btn-ghost");
        btnSupprimer.setFocusTraversable(false);
        btnSupprimer.setOnAction(e -> supprimer(ch));

        HBox footer = new HBox(10, meta, spacer, btnVoir, btnModifier, btnSupprimer);
        footer.getStyleClass().add("formation-footer");

        VBox body = new VBox(6, title);
        body.getStyleClass().add("formation-body");

        card.getChildren().addAll(body, footer);
        return card;
    }

    private void ouvrirModifier(Chapitre ch) {
        if (ch == null || ch.getId() <= 0) {
            return;
        }
        try {
            UiState.selectedChapitreEditId = ch.getId();
            WindowUtil.navigate(btnRetour, "/Formation/AjouterChapitre.fxml", "Modifier chapitre");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void supprimer(Chapitre ch) {
        if (ch == null || ch.getId() <= 0) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer ce chapitre ?");
        confirm.setContentText("Cette action est irréversible.");

        ButtonType ok = new ButtonType("Supprimer");
        ButtonType cancel = new ButtonType("Annuler", ButtonType.CANCEL.getButtonData());
        confirm.getButtonTypes().setAll(ok, cancel);

        confirm.showAndWait().ifPresent(type -> {
            if (type != ok) {
                return;
            }
            try {
                chapitreServices.supprimerChapitre(ch.getId());
                refresh();
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Erreur");
                err.setHeaderText("Suppression impossible");
                err.setContentText(e.getMessage());
                err.showAndWait();
            }
        });
    }

    private void ouvrirDetail(Chapitre ch) {
        if (ch == null || ch.getId() <= 0) {
            return;
        }
        try {
            UiState.selectedChapitreId = ch.getId();
            UiState.selectedChapitreReturnFxml = "/Formation/AfficherChapitre.fxml";
            WindowUtil.navigate(btnRetour, "/Formation/DetailChapitre.fxml", "Chapitre");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void ouvrirAjouter() {
        if (UiState.selectedCourId == null) {
            return;
        }
        try {
            WindowUtil.navigate(btnAjouter, "/Formation/AjouterChapitre.fxml", "Ajouter un chapitre");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/AfficherCour.fxml", "Cours");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
