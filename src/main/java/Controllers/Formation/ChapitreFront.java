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
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class ChapitreFront {

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    @FXML private Button btnRetour;
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
        btnVoir.setOnAction(e -> ouvrirAfficherChapitre(ch));

        HBox footer = new HBox(10, meta, spacer, btnVoir);
        footer.getStyleClass().add("formation-footer");

        VBox body = new VBox(6, title);
        body.getStyleClass().add("formation-body");

        card.getChildren().addAll(body, footer);
        return card;
    }

    private void ouvrirAfficherChapitre(Chapitre ch) {
        if (ch == null || ch.getId() <= 0) {
            return;
        }
        try {
            UiState.selectedChapitreId = ch.getId();
            UiState.selectedChapitreReturnFxml = "/Formation/ChapitreFront.fxml";
            WindowUtil.navigate(btnRetour, "/Formation/DetailChapitre.fxml", "Chapitre");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/CourFront.fxml", "Cours");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
