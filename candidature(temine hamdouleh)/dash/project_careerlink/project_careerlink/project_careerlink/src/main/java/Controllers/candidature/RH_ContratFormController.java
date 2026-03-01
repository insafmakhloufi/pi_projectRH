package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Contrat;
import Services.candidature.ContratService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.sql.Date;

public class RH_ContratFormController {

    @FXML private TextField tfNomCandidat;
    @FXML private TextField tfEmailCandidat;

    @FXML private ComboBox<String> cbTypeContrat;
    @FXML private ComboBox<String> cbTypeEmploi;
    @FXML private TextField tfSecteur;
    @FXML private TextField tfLocalisation;
    @FXML private TextField tfNomEntreprise;
    @FXML private ComboBox<String> cbModeTravail;

    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfSalaire;
    @FXML private TextField tfHoraire;
    @FXML private TextField tfPeriodeEssai;
    @FXML private TextField tfAvantages;

    @FXML private Label lblError;
    @FXML private Button btnBack;

    @FXML private ComboBox<String> cbHoraire;


    private Candidature candidature;
    private final ContratService contratService = new ContratService();

    @FXML
    private void initialize() {
        cbTypeContrat.getItems().setAll("CDI", "CDD", "Stage", "Freelance", "Alternance");
        cbTypeContrat.setValue("CDI");

        cbTypeEmploi.getItems().setAll("Temps plein", "Temps partiel", "Remote");
        cbTypeEmploi.setValue("Temps plein");

        cbModeTravail.getItems().setAll("Présentiel", "Hybride", "Remote");
        cbModeTravail.setValue("Présentiel");

        // Remplace tfHoraire TextField par ComboBox dans le FXML
        // Pour l'instant on remplit via code
        cbHoraire.getItems().setAll(
                "35h/semaine - 9h à 17h",
                "40h/semaine - 8h à 17h",
                "40h/semaine - 9h à 18h",
                "45h/semaine - 8h à 18h30",
                "Temps partiel - 20h/semaine",
                "Flexible - Horaires libres"
        );
        cbHoraire.setValue("40h/semaine - 8h à 17h");

        // listener d'abord
        cbTypeContrat.valueProperty().addListener((obs, o, n) -> updateDateFinState(n));

       // ensuite état initial
        updateDateFinState(cbTypeContrat.getValue());
    }

    private void updateDateFinState(String typeContrat) {
        boolean needDateFin = "CDD".equals(typeContrat)
                || "Stage".equals(typeContrat)
                || "Alternance".equals(typeContrat);

        dpDateFin.setDisable(!needDateFin);
        dpDateFin.setEditable(needDateFin);
        dpDateFin.setOpacity(needDateFin ? 1.0 : 0.5);

        if (!needDateFin) {
            dpDateFin.setValue(null);
        }
    }
    public void initContrat(Candidature c) {
        this.candidature = c;

        // Pré-remplir infos candidat (readonly)
        String nom = (c.getPrenom() + " " + c.getNom()).trim();
        tfNomCandidat.setText(nom);
        tfEmailCandidat.setText(c.getEmail() != null ? c.getEmail() : "");

        // ⚠️ EN DUR pour l'instant — à remplacer par offre réelle lors de l'intégration
        tfSecteur.setText("Informatique");
        tfLocalisation.setText("Tunis, Tunisie");
        tfNomEntreprise.setText("CareerLink");
    }

    @FXML
    private void genererContrat() {
        lblError.setText("");

        // Validation
        if (dpDateDebut.getValue() == null) {
            lblError.setText("La date de début est obligatoire.");
            return;
        }

        // Salaire
        Double salaire = null;
        if (!tfSalaire.getText().isBlank()) {
            try {
                salaire = Double.parseDouble(tfSalaire.getText().trim());
                if (salaire <= 0) {
                    lblError.setText("Le salaire doit être un nombre positif.");
                    return;
                }
            } catch (NumberFormatException e) {
                lblError.setText("Le salaire doit être un nombre valide. Ex: 1500.00");
                return;
            }
        }

// Entreprise obligatoire
        if (tfNomEntreprise.getText().isBlank()) {
            lblError.setText("Le nom de l'entreprise est obligatoire.");
            return;
        }

// Secteur obligatoire
        if (tfSecteur.getText().isBlank()) {
            lblError.setText("Le secteur est obligatoire.");
            return;
        }

// Horaire obligatoire
        if (cbHoraire.getValue() == null || cbHoraire.getValue().isBlank()) {
            lblError.setText("L'horaire de travail est obligatoire.");
            return;
        }

        boolean needDateFin = "CDD".equals(cbTypeContrat.getValue())
                || "Stage".equals(cbTypeContrat.getValue())
                || "Alternance".equals(cbTypeContrat.getValue());

        if (needDateFin && dpDateFin.getValue() == null) {
            lblError.setText("La date de fin est obligatoire pour un " + cbTypeContrat.getValue() + ".");
            return;
        }

        if (dpDateFin.getValue() != null && dpDateDebut.getValue().isAfter(dpDateFin.getValue())) {
            lblError.setText("La date de début ne peut pas être après la date de fin.");
            return;
        }



        // Construire le contrat
        Contrat contrat = new Contrat();
        contrat.setCandidature_id(candidature.getIDCandidat());

        if (candidature.getUserId() != null) {
            contrat.setCandidat_id(candidature.getUserId());
        }

        // ⚠️ EN DUR pour l'instant
        contrat.setType_contrat(cbTypeContrat.getValue());
        contrat.setType_emploi(cbTypeEmploi.getValue());
        contrat.setSecteur(tfSecteur.getText());
        contrat.setLocalisation(tfLocalisation.getText());
        contrat.setNom_entreprise(tfNomEntreprise.getText());
        contrat.setMode_travail(cbModeTravail.getValue());

        contrat.setDate_debut(Date.valueOf(dpDateDebut.getValue()));
        if (dpDateFin.getValue() != null) {
            contrat.setDate_fin(Date.valueOf(dpDateFin.getValue()));
        }

        contrat.setSalaire(salaire);
        contrat.setHoraire_travail(cbHoraire.getValue());
        contrat.setPeriode_essai(tfPeriodeEssai.getText());
        contrat.setAvantages(tfAvantages.getText());
        contrat.setStatut("GENERATED");

        // Générer QR hash simple
        String hash = "CTRK-" + candidature.getIDCandidat() + "-" + System.currentTimeMillis();
        contrat.setQr_hash(hash);

        // Sauvegarder en DB
        contratService.ajouterContrat(contrat);

        // Confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Contrat généré");
        alert.setHeaderText(null);
        alert.setContentText("Le contrat a été généré avec succès !\nRéférence : " + hash);
        alert.showAndWait();

        // Retour à la liste
        retour();
    }

    @FXML
    private void retour() {
        try {
            Parent root = btnBack.getScene().getRoot();
            Pane pane = (Pane) root.lookup("#adminContentArea");
            if (pane == null) return;

            var url = getClass().getResource("/candidaturefxml/RH_Candidatures.fxml");
            if (url == null) return;

            Parent view = FXMLLoader.load(url);
            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}