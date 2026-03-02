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
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javafx.stage.FileChooser;

public class AfficherCour {

    private static final String PDF_PREFIX = "pdf:";

    @FXML private Button btnRetour;
    @FXML private Button btnAjouter;
    @FXML private Label lblTitre;
    @FXML private FlowPane cardsFlow;
    @FXML private FlowPane chipsFlow;
    @FXML private TextField tfSearchChap;
    @FXML private Button btnSortDuree;
    @FXML private PieChart chartDureeByChap;

    private boolean sortDureeEnabled = false;
    private final ToggleGroup chipsGroup = new ToggleGroup();
    private boolean applyingChipSelection = false;

    private final CourServices courServices = new CourServices();
    private final FormationServices formationServices = new FormationServices();

    private String coverSlug;

    @FXML
    void initialize() {
        refreshCards();
        btnRetour.setOnAction(e -> retour());
        if (btnAjouter != null) {
            btnAjouter.setOnAction(e -> ouvrirAjouter());
        }

        if (tfSearchChap != null) {
            tfSearchChap.textProperty().addListener((obs, oldV, newV) -> refreshCards());
        }

        if (btnSortDuree != null) {
            btnSortDuree.setFocusTraversable(false);
            btnSortDuree.setOnAction(e -> {
                sortDureeEnabled = true;
                refreshCards();
            });
        }

        String titre = UiState.selectedFormationTitre;
        if (titre != null && !titre.isBlank()) {
            lblTitre.setText("Cours - " + titre);
        }

        if (UiState.selectedFormationId == null) return;
        refreshCards();
    }

    private void buildChapitreChips(List<Cour> allCours, String currentQuery) {
        if (chipsFlow == null) return;

        Set<String> origines = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (allCours != null) {
            for (Cour c : allCours) {
                if (c == null) continue;
                String origine = c.getOrigine();
                if (origine != null && !origine.isBlank()) origines.add(origine.trim());
            }
        }

        chipsFlow.getChildren().clear();

        // ── "Tous" chip
        ToggleButton allBtn = new ToggleButton("Tous");
        allBtn.getStyleClass().addAll("chip", "chip-active");    // ← chip-active dès le départ
        allBtn.setToggleGroup(chipsGroup);
        allBtn.setSelected(true);
        allBtn.selectedProperty().addListener((obs, was, is) -> {
            if (is) allBtn.getStyleClass().add("chip-active");
            else    allBtn.getStyleClass().remove("chip-active");
        });
        allBtn.setOnAction(e -> applyChipToSearch(""));
        chipsFlow.getChildren().add(allBtn);

        ToggleButton toSelect = null;
        String q = currentQuery == null ? "" : currentQuery.trim().toLowerCase();

        for (String origine : origines) {
            ToggleButton b = new ToggleButton(origine);
            b.getStyleClass().add("chip");
            b.setToggleGroup(chipsGroup);
            b.selectedProperty().addListener((obs, was, is) -> {
                if (is) b.getStyleClass().add("chip-active");
                else    b.getStyleClass().remove("chip-active");
            });
            b.setOnAction(e -> applyChipToSearch(origine));
            chipsFlow.getChildren().add(b);
            if (!q.isEmpty() && origine.toLowerCase().contains(q)) toSelect = b;
        }

        if (toSelect != null) toSelect.setSelected(true);
        else                  allBtn.setSelected(true);
    }

    private void applyChipToSearch(String value) {
        if (tfSearchChap == null) return;
        applyingChipSelection = true;
        try {
            tfSearchChap.setText(value == null ? "" : value);
        } finally {
            applyingChipSelection = false;
        }
        refreshCards();
    }

    private void refreshCards() {
        if (coverSlug == null) {
            Formation f = formationServices.getFormationById(UiState.selectedFormationId);
            coverSlug = slugify(f == null ? null : f.getDomaine());
        }

        String q = tfSearchChap == null ? "" : tfSearchChap.getText();
        String query = (q == null) ? "" : q.trim().toLowerCase();

        List<Cour> all = courServices.afficherCoursParFormation(UiState.selectedFormationId);
        buildChapitreChips(all, q);

        ObservableList<Cour> cours = FXCollections.observableList(all);
        if (!query.isEmpty()) {
            cours = cours.filtered(c -> {
                String origine = c.getOrigine();
                return origine != null && origine.toLowerCase().contains(query);
            });
        }

        if (sortDureeEnabled) {
            cours.sort((a, b) -> {
                float da = (a == null) ? Float.NaN : a.getDure();
                float db = (b == null) ? Float.NaN : b.getDure();
                if (Float.isNaN(da) && Float.isNaN(db)) return 0;
                if (Float.isNaN(da)) return 1;
                if (Float.isNaN(db)) return -1;
                return Float.compare(da, db);
            });
        }

        updateStatsDureeByChapitre(cours);

        cardsFlow.getChildren().clear();
        int index = 0;
        for (Cour c : cours) {
            cardsFlow.getChildren().add(createCard(c, index++));
        }
    }

    private void updateStatsDureeByChapitre(ObservableList<Cour> cours) {
        if (chartDureeByChap == null) return;

        Map<String, Double> sumByOrigine = new TreeMap<>();
        if (cours != null) {
            for (Cour c : cours) {
                if (c == null) continue;
                String origine = c.getOrigine();
                if (origine == null || origine.isBlank()) origine = "(Sans origine)";
                double d = c.getDure();
                sumByOrigine.put(origine, sumByOrigine.getOrDefault(origine, 0.0) + d);
            }
        }

        List<Map.Entry<String, Double>> ordered = new ArrayList<>(sumByOrigine.entrySet());
        ordered.sort(Map.Entry.comparingByValue());

        double total = 0.0;
        for (Map.Entry<String, Double> e : ordered) total += e.getValue();

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<String, Double> e : ordered) {
            data.add(new PieChart.Data(e.getKey(), e.getValue()));
        }
        chartDureeByChap.setData(data);

        final double totalFinal = total;
        for (PieChart.Data d : data) {
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) return;
                double value = d.getPieValue();
                double pct = (totalFinal <= 0.0) ? 0.0 : (value * 100.0 / totalFinal);
                Tooltip.install(newNode, new Tooltip(
                        String.format(Locale.US, "%.0f min (%.1f%%)", value, pct)));
            });
        }
    }

    // ══════════════════════════════════════════════════════════
    //  createCard — seuls les getStyleClass() ont changé
    // ══════════════════════════════════════════════════════════
    private VBox createCard(Cour c, int index) {
        VBox card = new VBox(0);
        card.getStyleClass().add("formation-card");

        // ── Cover dégradé (+ image si dispo)
        Region cover = new Region();
        String[] coverClasses = {"formation-cover", "course-cover", "cover-purple", "cover-teal"};
        cover.getStyleClass().addAll("formation-cover", coverClasses[index % coverClasses.length]);
        cover.setPrefHeight(120);
        applyCoverSlug(cover, coverSlug);

        // ── Chapitre (petit texte violet majuscule)
        String origineTxt = c.getOrigine() != null ? c.getOrigine() : "";
        Label chap = new Label(origineTxt.toUpperCase());
        chap.getStyleClass().add("formation-domain");            // ← était "formation-code"

        // ── Nom formateur (titre principal)
        Label title = new Label(c.getNomFormateur());
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);

        // ── Complexité
        Label status = new Label("⚡ Complexité : " + c.getComplexite());
        status.getStyleClass().add("formation-complexity");      // ← était "formation-status"

        VBox body = new VBox(6);
        body.getStyleClass().add("formation-body");
        body.getChildren().addAll(chap, title, status);

        // ── Editor inline (inchangé)
        VBox editor = new VBox(10);
        editor.getStyleClass().add("formation-body");
        editor.setVisible(false);
        editor.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField tfFormateur   = new TextField(c.getNomFormateur());
        TextField tfComplexite  = new TextField(String.valueOf(c.getComplexite()));
        TextField tfOrigine     = new TextField(c.getOrigine());
        TextField tfDuree       = new TextField(String.valueOf(c.getDure()));
        TextArea  taDescription = new TextArea(c.getDescription());
        taDescription.setPrefRowCount(3);

        grid.add(new Label("Formateur"),   0, 0); grid.add(tfFormateur,    1, 0);
        grid.add(new Label("Complexité"),  0, 1); grid.add(tfComplexite,   1, 1);
        grid.add(new Label("Origine"),     0, 2); grid.add(tfOrigine,      1, 2);
        grid.add(new Label("Durée"),       0, 3); grid.add(tfDuree,        1, 3);
        grid.add(new Label("Description"), 0, 4); grid.add(taDescription,  1, 4);

        Label lblEditMsg = new Label();
        lblEditMsg.getStyleClass().add("message-error");

        Button btnSave   = new Button("💾 Enregistrer");
        btnSave.getStyleClass().add("btn-save");                 // ← était "btn-success"
        btnSave.setFocusTraversable(false);

        Button btnCancel = new Button("✕ Annuler");
        btnCancel.getStyleClass().add("btn-cancel");             // ← était "btn-ghost"
        btnCancel.setFocusTraversable(false);

        HBox actions = new HBox(10, btnCancel, btnSave);
        editor.getChildren().addAll(grid, lblEditMsg, actions);

        Region divider = new Region();
        divider.getStyleClass().add("formation-divider");
        divider.setPrefHeight(1);

        // ── Footer
        Label meta = new Label(c.getOrigine() + " • " + c.getDure() + " h");
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton Voir — blanc avec bordure indigo
        Button btnVoir = new Button("Voir");
        btnVoir.getStyleClass().add("btn-view-card");            // ← était "btn-ghost"
        btnVoir.setFocusTraversable(false);
        btnVoir.setOnAction(e -> afficherContenu(c));

        Button btnChapitres = new Button("Chapitres");
        btnChapitres.getStyleClass().add("btn-secondary");
        btnChapitres.setFocusTraversable(false);
        btnChapitres.setOnAction(e -> {
            UiState.selectedCourId = (c == null) ? null : c.getId();
            UiState.selectedCourTitre = (c == null) ? null : c.getNomFormateur();
            WindowUtil.navigate(btnChapitres, "/Formation/AfficherChapitre.fxml", "Chapitres");
        });

        // Bouton Modifier — violet dégradé
        SVGPath editIcon = new SVGPath();
        editIcon.getStyleClass().add("card-edit-svg");           // ← était "btn-svg btn-svg-edit"
        editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm2.92 2.33H5v-.92l9.06-9.06.92.92L5.92 19.58zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z");

        Button btnModifier = new Button();
        btnModifier.getStyleClass().add("btn-edit-card");        // ← était "btn-primary btn-icon"
        btnModifier.setGraphic(editIcon);
        btnModifier.setTooltip(new Tooltip("Modifier"));
        btnModifier.setFocusTraversable(false);
        btnModifier.setOnAction(e -> {
            lblEditMsg.setText("");
            body.setVisible(false);
            body.setManaged(false);
            editor.setVisible(true);
            editor.setManaged(true);
        });

        // Bouton Supprimer — rouge dégradé
        SVGPath deleteIcon = new SVGPath();
        deleteIcon.getStyleClass().add("card-delete-svg");       // ← était "btn-svg btn-svg-delete"
        deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");

        Button btnSupprimer = new Button();
        btnSupprimer.getStyleClass().add("btn-delete-card");     // ← était "btn-danger btn-icon"
        btnSupprimer.setGraphic(deleteIcon);
        btnSupprimer.setTooltip(new Tooltip("Supprimer"));
        btnSupprimer.setFocusTraversable(false);
        btnSupprimer.setOnAction(e -> supprimerCour(c));

        btnCancel.setOnAction(e -> {
            lblEditMsg.setText("");
            editor.setVisible(false);
            editor.setManaged(false);
            body.setVisible(true);
            body.setManaged(true);
        });

        btnSave.setOnAction(e -> {
            try {
                if (tfFormateur.getText().isBlank()
                        || tfComplexite.getText().isBlank() || tfOrigine.getText().isBlank()
                        || tfDuree.getText().isBlank() || taDescription.getText().isBlank()) {
                    lblEditMsg.setText("Veuillez remplir tous les champs");
                    return;
                }
                int complexite;
                float duree;
                try { complexite = Integer.parseInt(tfComplexite.getText().trim()); }
                catch (NumberFormatException ex) { lblEditMsg.setText("Complexité invalide"); return; }
                try { duree = Float.parseFloat(tfDuree.getText().trim()); }
                catch (NumberFormatException ex) { lblEditMsg.setText("Durée invalide"); return; }
                if (duree <= 0) { lblEditMsg.setText("Durée invalide"); return; }

                Cour updated = new Cour(
                        c.getId(), c.getFormationId(), tfFormateur.getText().trim(),
                        complexite, taDescription.getText().trim(),
                        tfOrigine.getText().trim(), duree
                );
                courServices.modifierCour(updated);
                refreshCards();
            } catch (Exception ex) {
                lblEditMsg.setText("Erreur: " + ex.getMessage());
            }
        });

        HBox footer = new HBox(8);
        footer.getStyleClass().add("formation-footer");
        footer.getChildren().addAll(meta, spacer, btnChapitres, btnVoir, btnModifier, btnSupprimer);

        card.getChildren().addAll(cover, body, editor, divider, footer);
        return card;
    }

    private void applyCoverSlug(Region cover, String slug) {
        if (cover == null || slug == null || slug.isBlank()) return;
        java.net.URL url = getClass().getResource("/Images/domains/" + slug + ".png");
        if (url == null) url = getClass().getResource("/Images/domains/" + slug + ".jpg");
        if (url == null) url = getClass().getResource("/Images/domains/" + slug + ".jpeg");
        if (url == null) return;
        String cssUrl = url.toExternalForm();
        cover.setStyle(
                "-fx-background-image: url('" + cssUrl + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );
    }

    private String slugify(String s) {
        if (s == null) return "";
        String v = s.trim().toLowerCase();
        if (v.isEmpty()) return "";
        v = Normalizer.normalize(v, Normalizer.Form.NFD);
        v = v.replaceAll("\\p{M}", "");
        v = v.replaceAll("[^a-z0-9]+", "-");
        v = v.replaceAll("(^-+)|(-+$)", "");
        return v;
    }

    private void supprimerCour(Cour c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le cours ?");
        String titre = (c == null || c.getNomFormateur() == null || c.getNomFormateur().isBlank()) ? "Cours" : c.getNomFormateur();
        confirm.setContentText("\"" + titre + "\" sera supprimé.");
        ButtonType result = confirm.showAndWait().orElse(ButtonType.CANCEL);
        if (result != ButtonType.OK) return;
        try {
            courServices.supprimerCour(c.getId());
            refreshCards();
        } catch (Exception ex) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Suppression impossible");
            error.setContentText(ex.getMessage());
            error.showAndWait();
        }
    }

    private void afficherContenu(Cour c) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du cours");
        String header = (c == null || c.getNomFormateur() == null || c.getNomFormateur().isBlank()) ? "Cours" : c.getNomFormateur();
        dialog.setHeaderText(header);
        
        VBox content = new VBox(10);
        content.setPrefWidth(400);
        content.getChildren().addAll(
            new Label("Formateur: " + c.getNomFormateur()),
            new Label("Origine: " + c.getOrigine()),
            new Label("Durée: " + c.getDure() + " h"),
            new Label("Nombre de chapitres: " + c.getNbChapitres()),
            new Label("Description:"),
            new Label(c.getDescription())
        );
        ((Label)content.getChildren().get(5)).setWrapText(true);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void choisirPdfEtRemplirContenu(TextArea taContenu) {
        // Méthode obsolète car le champ contenu a été supprimé
    }

    private void ouvrirAjouter() {
        try {
            WindowUtil.navigate(btnAjouter, "/Formation/AfficherCour.fxml", "Cours de la formation");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/AfficherFormation.fxml", "Liste des formations");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}