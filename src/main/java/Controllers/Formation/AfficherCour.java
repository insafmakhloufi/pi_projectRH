package Controllers.Formation;

import Entities.Formation.Cour;
import Entities.Formation.Formation;
import Services.Formation.CourServices;
import Services.Formation.FormationServices;
import Services.Formation.PredictiveCourseAnalyzer;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.SVGPath;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class AfficherCour {

    private static final String PDF_PREFIX = "pdf:";

    @FXML private Button btnRetour;
    @FXML private Button btnAjouter;
    @FXML private Label lblTitre;
    @FXML private FlowPane cardsFlow;
    @FXML private FlowPane chipsFlow;
    @FXML private TextField tfSearchChap;
    @FXML private Button btnSortDuree;
    @FXML private Canvas canvasComplexity;
    @FXML private Label lblStatsTotal;
    @FXML private Label lblStatsSub;
    @FXML private VBox statsLegend;

    private boolean sortDureeEnabled = false;
    private final ToggleGroup chipsGroup = new ToggleGroup();
    private boolean applyingChipSelection = false;

    private final CourServices courServices = new CourServices();
    private final FormationServices formationServices = new FormationServices();
    private final PredictiveCourseAnalyzer courseAnalyzer = new PredictiveCourseAnalyzer();

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

        updateStatsComplexity(cours);

        cardsFlow.getChildren().clear();
        int index = 0;
        for (Cour c : cours) {
            cardsFlow.getChildren().add(createCard(c, index++));
        }
    }

    private void updateStatsComplexity(ObservableList<Cour> cours) {
        if (canvasComplexity == null) return;

        int easy = 0;
        int medium = 0;
        int hard = 0;

        if (cours != null) {
            for (Cour c : cours) {
                if (c == null) continue;
                double score = courseAnalyzer.getComplexityScore(c);
                if (score < 2.5) {
                    easy++;
                } else if (Math.abs(score - 2.5) < 1e-9) {
                    medium++;
                } else {
                    hard++;
                }
            }
        }

        int total = easy + medium + hard;

        if (lblStatsTotal != null) {
            lblStatsTotal.setText(total + " cours");
        }

        if (lblStatsSub != null) {
            if (total <= 0) {
                lblStatsSub.setText("");
            } else {
                int topV = easy;
                String topName = "Facile";
                if (medium > topV) {
                    topV = medium;
                    topName = "Moyen";
                }
                if (hard > topV) {
                    topV = hard;
                    topName = "Complexe";
                }
                double pctTop = (topV * 100.0 / total);
                lblStatsSub.setText(String.format(Locale.US, "%.0f%% %s", pctTop, topName));
            }
        }

        if (statsLegend != null) {
            statsLegend.getChildren().clear();
        }

        double[] values = new double[] { easy, medium, hard };
        String[] labels = new String[] { "Facile", "Moyen", "Complexe" };
        String[] colorsHex = new String[] { "#10b981", "#f59e0b", "#ef4444" };

        drawDonutOnCanvas(canvasComplexity, values, colorsHex);

        if (statsLegend != null) {
            for (int i = 0; i < values.length; i++) {
                double v = values[i];
                double pct = (total <= 0) ? 0.0 : (v * 100.0 / total);

                Region dot = new Region();
                dot.getStyleClass().add("stats-dot");
                dot.setStyle("-fx-background-color: " + colorsHex[i] + ";");

                Label name = new Label(labels[i]);
                name.getStyleClass().add("stats-legend-title");

                Label val = new Label(String.format(Locale.US, "%.0f (%.0f%%)", v, pct));
                val.getStyleClass().add("stats-legend-value");

                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);

                HBox row = new HBox(10, dot, name, spacer, val);
                row.getStyleClass().add("stats-legend-row");
                statsLegend.getChildren().add(row);
            }
        }
    }

    private void drawDonutOnCanvas(Canvas canvas, double[] values, String[] colorsHex) {
        if (canvas == null) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        if (gc == null) return;

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (w <= 0 || h <= 0) return;

        gc.clearRect(0, 0, w, h);

        double total = 0.0;
        if (values != null) {
            for (double v : values) total += Math.max(0.0, v);
        }

        double cx = w / 2.0;
        double cy = h / 2.0;

        double min = Math.min(w, h);
        double ring = Math.max(12.0, min * 0.14);
        double padding = 10.0;
        double radius = (min / 2.0) - padding - (ring / 2.0);
        if (radius <= 0) return;
        double diameter = radius * 2.0;
        double x = cx - radius;
        double y = cy - radius;

        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        gc.setLineWidth(ring);
        gc.setStroke(Color.web("#e5e7eb", 0.85));
        gc.strokeArc(x, y, diameter, diameter, 90, -360, ArcType.OPEN);

        if (total <= 0.0) {
            gc.setStroke(Color.web("#e5e7eb"));
            gc.strokeArc(x, y, diameter, diameter, 90, -360, ArcType.OPEN);
            return;
        }

        double start = 90.0;
        double gap = 2.5;
        for (int i = 0; i < values.length; i++) {
            double v = Math.max(0.0, values[i]);
            if (v <= 0.0) continue;
            double rawAngle = (v * 360.0 / total);
            double drawAngle = Math.max(0.0, rawAngle - gap);
            double signed = -drawAngle;
            double startAdj = start - (gap / 2.0);

            String hex = (colorsHex == null || i >= colorsHex.length) ? "#6366f1" : colorsHex[i];

            gc.setLineWidth(ring + 4.0);
            gc.setStroke(Color.web(hex, 0.20));
            gc.strokeArc(x, y, diameter, diameter, startAdj, signed, ArcType.OPEN);

            gc.setLineWidth(ring);
            gc.setStroke(Color.web(hex));
            gc.strokeArc(x, y, diameter, diameter, startAdj, signed, ArcType.OPEN);

            start -= rawAngle;
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
        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        pane.getStyleClass().add("modern-dialog");

        java.net.URL css = getClass().getResource("/Formation/appAdmin.css");
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }

        String headerText = (c == null || c.getNomFormateur() == null || c.getNomFormateur().isBlank()) ? "Cours" : c.getNomFormateur();
        Label title = new Label("Détails du cours");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label(headerText);
        subtitle.getStyleClass().add("dialog-subtitle");
        VBox header = new VBox(2, title, subtitle);
        header.getStyleClass().add("dialog-header");
        pane.setHeader(header);

        String formateur = (c == null || c.getNomFormateur() == null) ? "" : c.getNomFormateur();
        String origine = (c == null || c.getOrigine() == null) ? "" : c.getOrigine();
        String desc = (c == null || c.getDescription() == null) ? "" : c.getDescription();
        String duree = (c == null) ? "" : (c.getDure() + " h");
        String nbCh = (c == null) ? "" : String.valueOf(c.getNbChapitres());

        Label badgeOrigine = new Label(origine == null || origine.isBlank() ? "Sans origine" : origine);
        badgeOrigine.getStyleClass().add("dialog-badge");
        Label badgeDuree = new Label(duree == null || duree.isBlank() ? "Durée inconnue" : duree);
        badgeDuree.getStyleClass().add("dialog-badge");
        Label badgeCh = new Label((nbCh == null || nbCh.isBlank() ? "0" : nbCh) + " chapitres");
        badgeCh.getStyleClass().add("dialog-badge");

        HBox badges = new HBox(10, badgeOrigine, badgeDuree, badgeCh);
        badges.getStyleClass().add("dialog-badges");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("dialog-grid");
        grid.setHgap(14);
        grid.setVgap(12);

        Label l1 = new Label("Formateur");
        l1.getStyleClass().add("dialog-field-label");
        Label v1 = new Label(formateur);
        v1.getStyleClass().add("dialog-field-value");
        v1.setWrapText(true);

        Label l2 = new Label("Description");
        l2.getStyleClass().add("dialog-field-label");
        Label v2 = new Label(desc);
        v2.getStyleClass().add("dialog-field-value");
        v2.setWrapText(true);

        grid.add(l1, 0, 0);
        grid.add(v1, 1, 0);
        grid.add(l2, 0, 1);
        grid.add(v2, 1, 1);

        VBox content = new VBox(14, badges, grid);
        content.getStyleClass().add("dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(620);
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