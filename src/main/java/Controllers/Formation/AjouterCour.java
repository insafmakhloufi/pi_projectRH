package Controllers.Formation;

import Entites.Formation.Cour;
import Entites.Formation.Formation;
import Services.Formation.CourServices;
import Services.Formation.FormationServices;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class AjouterCour {

    private static final String PDF_PREFIX = "pdf:";

    @FXML
    private Button btnRetour;

    @FXML
    private ComboBox<Formation> cbFormation;

    @FXML
    private TextField tfNomFormateur;

    @FXML
    private TextField tfComplexite;

    @FXML
    private TextArea taDescription;

    @FXML
    private TextField tfOrigine;

    @FXML
    private TextField tfDuree;

    @FXML
    private TextField tfNbChapitres;

    @FXML
    private Button btnEnregistrer;

    @FXML
    private Label lblMessage;

    private final FormationServices formationServices = new FormationServices();
    private final CourServices courServices = new CourServices();

    @FXML
    void initialize() {
        btnRetour.setOnAction(e -> retour());
        btnEnregistrer.setOnAction(e -> enregistrer());

        List<Formation> formations = formationServices.afficherFormations();
        ObservableList<Formation> items = FXCollections.observableArrayList(formations);
        cbFormation.setItems(items);

        cbFormation.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });
        cbFormation.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Formation item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitre());
            }
        });

        if (UiState.selectedFormationId != null) {
            for (Formation f : items) {
                if (f.getId() == UiState.selectedFormationId) {
                    cbFormation.getSelectionModel().select(f);
                    break;
                }
            }
        }
    }

    private void enregistrer() {
        try {
            if (cbFormation.getValue() == null
                    || tfNomFormateur.getText().isBlank()
                    || tfComplexite.getText().isBlank()
                    || taDescription.getText().isBlank()
                    || tfOrigine.getText().isBlank()
                    || tfDuree.getText().isBlank()
                    || tfNbChapitres.getText().isBlank()) {
                lblMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            int complexite = Integer.parseInt(tfComplexite.getText().trim());
            float duree = Float.parseFloat(tfDuree.getText().trim());
            int nbChapitres;
            try {
                nbChapitres = Integer.parseInt(tfNbChapitres.getText().trim());
            } catch (NumberFormatException e) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Nombre de chapitres invalide");
                return;
            }

            if (nbChapitres <= 0) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Nombre de chapitres doit être > 0");
                return;
            }

            if (duree <= 0) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Durée invalide");
                return;
            }

            Formation formation = cbFormation.getValue();

            Cour c = new Cour(
                    tfNomFormateur.getText().trim(),
                    complexite,
                    taDescription.getText().trim(),
                    tfOrigine.getText().trim(),
                    duree,
                    nbChapitres
            );
            c.setFormationId(formation.getId());

            courServices.ajouterCour(c);

            lblMessage.setStyle("-fx-text-fill: #16a34a;");
            lblMessage.setText("Cours ajouté avec succès");

            // retour vers la liste des cours de la formation
            WindowUtil.navigate(btnEnregistrer, "/Formation/AfficherCour.fxml", "Cours de la formation");
        } catch (NumberFormatException e) {
            lblMessage.setStyle("-fx-text-fill: #dc2626;");
            lblMessage.setText("Complexité/Durée invalides");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: #dc2626;");
            lblMessage.setText("Erreur: " + e.getMessage());
        }
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/AjouterFormation.fxml", "Ajouter une formation");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
