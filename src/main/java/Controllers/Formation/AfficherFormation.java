package Controllers.Formation;

import Entities.Formation.Formation;
import Services.Formation.CourServices;
import Services.Formation.FormationServices;
import Utils.UiState;
import Utils.Mydatabase;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import Utils.WindowUtil;
import java.text.Normalizer;
import javafx.util.Duration;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

public class AfficherFormation {

    private static final boolean DEBUG_CARD_CLICKS = false;

    @FXML private Button btnRetour;
    @FXML private Button btnTous;
    @FXML private Button btnShowPaidPurchases;
    @FXML private Label lblPaidPurchases;
    @FXML private ScrollPane cardsScroll;
    @FXML private FlowPane cardsFlow;
    @FXML private FlowPane chipsFlow;
    @FXML private TextField tfSearchDomaine;
    @FXML private Button btnSortDate;
    @FXML private PieChart chartPayanteStats;
    @FXML private VBox statsLegendBox;

    private boolean sortDateEnabled = false;
    private final ToggleGroup chipsGroup = new ToggleGroup();
    private boolean applyingChipSelection = false;

    private final FormationServices fs = new FormationServices();
    private final CourServices courServices = new CourServices();

    @FXML
    void initialize() {
        if (DEBUG_CARD_CLICKS && cardsFlow != null) {
            cardsFlow.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                Node n = (e.getPickResult() != null) ? e.getPickResult().getIntersectedNode() : null;
                System.out.println("[DEBUG] cardsFlow click x=" + e.getX() + " y=" + e.getY() + " target=" + (n != null ? n.getClass().getName() : "null"));
                int depth = 0;
                while (n != null && depth < 12) {
                    String id = (n.getId() != null) ? ("#" + n.getId()) : "";
                    String cls = (!n.getStyleClass().isEmpty()) ? ("." + String.join(".", n.getStyleClass())) : "";
                    System.out.println("  -> " + n.getClass().getSimpleName() + id + cls + " bounds=" + n.getBoundsInParent());
                    n = n.getParent();
                    depth++;
                }
            });
        }

        if (cardsScroll != null && cardsFlow != null) {
            cardsScroll.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
                if (newB != null) {
                    cardsFlow.setPrefWrapLength(Math.max(0, newB.getWidth()));
                }
            });
            Platform.runLater(() -> {
                Bounds vb = cardsScroll.getViewportBounds();
                if (vb != null) {
                    cardsFlow.setPrefWrapLength(Math.max(0, vb.getWidth()));
                }
                cardsFlow.applyCss();
                cardsFlow.layout();
            });
        }

        if (chartPayanteStats != null) {
            chartPayanteStats.setAnimated(true);
            chartPayanteStats.setLegendVisible(false);
            chartPayanteStats.setLabelsVisible(false);
            chartPayanteStats.setStartAngle(90);
        }

        if (tfSearchDomaine != null) {
            tfSearchDomaine.textProperty().addListener((obs, oldV, newV) -> refreshCards());
        }

        if (btnSortDate != null) {
            btnSortDate.setFocusTraversable(false);
            btnSortDate.setOnAction(e -> {
                sortDateEnabled = true;
                refreshCards();
            });
        }

        if (btnTous != null) {
            btnTous.setFocusTraversable(false);
            btnTous.setOnAction(e -> resetFilters());
        }

        if (btnShowPaidPurchases != null) {
            btnShowPaidPurchases.setOnAction(e -> generateDetailedReport());
        }

        btnRetour.setOnAction(event -> retour());

        refreshCards();
    }

    private void resetFilters() {
        sortDateEnabled = false;
        if (tfSearchDomaine != null) {
            tfSearchDomaine.clear();
        }

        if (chipsGroup != null) {
            Toggle t = null;
            for (Toggle tg : chipsGroup.getToggles()) {
                if (tg instanceof ToggleButton tb) {
                    if ("Tous".equalsIgnoreCase(tb.getText())) {
                        t = tg;
                        break;
                    }
                }
            }
            if (t != null) {
                chipsGroup.selectToggle(t);
            }
        }

        refreshCards();
    }

    private void buildDomainChips(List<Formation> allFormations) {
        if (chipsFlow == null) return;

        Set<String> domains = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (allFormations != null) {
            for (Formation f : allFormations) {
                if (f == null) continue;
                String dom = f.getDomaine();
                if (dom != null && !dom.isBlank()) domains.add(dom.trim());
            }
        }

        chipsFlow.getChildren().clear();

        // ── "Tous" chip
        ToggleButton allBtn = new ToggleButton("Tous");
        allBtn.getStyleClass().addAll("chip", "chip-active");   // ← chip-active au lieu de selected
        allBtn.setToggleGroup(chipsGroup);
        allBtn.setSelected(true);
        allBtn.selectedProperty().addListener((obs, was, is) -> {
            if (is) allBtn.getStyleClass().add("chip-active");
            else    allBtn.getStyleClass().remove("chip-active");
        });
        allBtn.setOnAction(e -> applyChipToSearch(""));
        chipsFlow.getChildren().add(allBtn);

        for (String dom : domains) {
            ToggleButton b = new ToggleButton(dom);
            b.getStyleClass().add("chip");
            b.setToggleGroup(chipsGroup);
            b.selectedProperty().addListener((obs, was, is) -> {
                if (is) b.getStyleClass().add("chip-active");
                else    b.getStyleClass().remove("chip-active");
            });
            b.setOnAction(e -> applyChipToSearch(dom));
            chipsFlow.getChildren().add(b);
        }
    }

    private void applyChipToSearch(String value) {
        if (tfSearchDomaine == null) return;
        applyingChipSelection = true;
        try {
            tfSearchDomaine.setText(value == null ? "" : value);
        } finally {
            applyingChipSelection = false;
        }
        refreshCards();
    }

    private void refreshCards() {
        if (cardsFlow == null) {
            return;
        }

        List<Formation> all = fs.afficherFormations();
        if (all == null) {
            all = java.util.Collections.emptyList();
        }
        buildDomainChips(all);

        updatePayanteStats(all);

        String q = tfSearchDomaine == null ? "" : tfSearchDomaine.getText();
        String query = (q == null) ? "" : q.trim().toLowerCase();

        ObservableList<Formation> observableList = FXCollections.observableList(all);
        if (!query.isEmpty()) {
            observableList = observableList.filtered(f -> {
                String dom = f.getDomaine();
                return dom != null && dom.toLowerCase().contains(query);
            });
        }

        if (sortDateEnabled) {
            observableList.sort((a, b) -> {
                java.sql.Date da = (a == null) ? null : a.getDateDebut();
                java.sql.Date db = (b == null) ? null : b.getDateDebut();
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return da.compareTo(db);
            });
        }

        cardsFlow.getChildren().clear();
        int index = 0;
        for (Formation f : observableList) {
            cardsFlow.getChildren().add(createCard(f, index++));
        }
    }

    private void updatePayanteStats(List<Formation> formations) {
        if (chartPayanteStats == null) {
            return;
        }

        int paid = 0;
        int free = 0;
        if (formations != null) {
            for (Formation f : formations) {
                if (f == null) {
                    continue;
                }
                if (f.isPayante()) {
                    paid++;
                } else {
                    free++;
                }
            }
        }

        System.out.println("[STATS] formations=" + (formations == null ? 0 : formations.size()) + " paid=" + paid + " free=" + free);

        final int paidFinal = paid;
        final int freeFinal = free;

        Runnable render = () -> {
            chartPayanteStats.getData().clear();
            
            PieChart.Data paidSlice = new PieChart.Data("Payantes", paidFinal);
            PieChart.Data freeSlice = new PieChart.Data("Gratuites", freeFinal);
            
            chartPayanteStats.getData().addAll(paidSlice, freeSlice);

            // Créer l'effet Donut
            chartPayanteStats.layout(); // Forcer le layout pour avoir les dimensions
            
            // Appliquer styles et tooltips
            applyPieStyle(paidSlice, "#6366f1"); // Indigo moderne
            applyPieStyle(freeSlice, "#10b981"); // Emeraude moderne

            int total = paidFinal + freeFinal;
            int pctPaid = total == 0 ? 0 : (int) Math.round((paidFinal * 100.0) / total);

            Circle innerCircle = new Circle(46);
            innerCircle.setFill(javafx.scene.paint.Color.WHITE);
            innerCircle.setStroke(javafx.scene.paint.Color.web("#eef2ff"));
            innerCircle.setStrokeWidth(2);

            Label centerTop = new Label(total + " total");
            centerTop.getStyleClass().add("donut-center-title");
            Label centerBottom = new Label(pctPaid + "% payantes");
            centerBottom.getStyleClass().add("donut-center-subtitle");
            VBox centerBox = new VBox(2, centerTop, centerBottom);
            centerBox.setAlignment(Pos.CENTER);
            centerBox.setMouseTransparent(true);
            centerBox.getStyleClass().add("donut-center");

            if (chartPayanteStats.getParent() instanceof StackPane) {
                StackPane container = (StackPane) chartPayanteStats.getParent();
                container.getChildren().removeIf(node -> (node instanceof Circle) || (node instanceof VBox vb && vb.getStyleClass().contains("donut-center")));
                container.getChildren().addAll(innerCircle, centerBox);
                StackPane.setAlignment(innerCircle, Pos.CENTER);
                StackPane.setAlignment(centerBox, Pos.CENTER);
            }

            renderLegend(total, paidFinal, freeFinal);

            chartPayanteStats.applyCss();
        };

        if (Platform.isFxApplicationThread()) {
            render.run();
        } else {
            Platform.runLater(render);
        }
    }

    private void applyPieStyle(PieChart.Data data, String color) {
        if (data.getNode() != null) {
            data.getNode().setStyle("-fx-pie-color: " + color + ";");
            Tooltip.install(data.getNode(), new Tooltip(data.getName() + ": " + (int)data.getPieValue()));
            attachSliceInteractions(data.getNode());
        } else {
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    newNode.setStyle("-fx-pie-color: " + color + ";");
                    Tooltip.install(newNode, new Tooltip(data.getName() + ": " + (int)data.getPieValue()));
                    attachSliceInteractions(newNode);
                }
            });
        }
    }

    private void attachSliceInteractions(Node node) {
        if (node == null) {
            return;
        }

        ScaleTransition st = new ScaleTransition(Duration.millis(180), node);
        node.setOnMouseEntered(e -> {
            st.stop();
            st.setToX(1.06);
            st.setToY(1.06);
            st.playFromStart();
        });
        node.setOnMouseExited(e -> {
            st.stop();
            st.setToX(1.0);
            st.setToY(1.0);
            st.playFromStart();
        });
    }

    private void renderLegend(int total, int paidCount, int freeCount) {
        if (statsLegendBox == null) {
            return;
        }

        statsLegendBox.getChildren().clear();

        int paidPct = total == 0 ? 0 : (int) Math.round((paidCount * 100.0) / total);
        int freePct = total == 0 ? 0 : (int) Math.round((freeCount * 100.0) / total);

        statsLegendBox.getChildren().add(buildLegendRow("Payantes", paidCount, paidPct, "#6366f1"));
        statsLegendBox.getChildren().add(buildLegendRow("Gratuites", freeCount, freePct, "#10b981"));
    }

    private HBox buildLegendRow(String label, int count, int pct, String color) {
        Circle dot = new Circle(5);
        dot.setFill(javafx.scene.paint.Color.web(color));

        Label name = new Label(label);
        name.getStyleClass().add("stats-legend-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label(count + "  (" + pct + "%)");
        value.getStyleClass().add("stats-legend-value");

        HBox row = new HBox(10, dot, name, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("stats-legend-row");
        return row;
    }

    private void generateDetailedReport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport des formations");
        fileChooser.setInitialFileName("Rapport_Formations_CareerLink.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try {
            PdfWriter writer = new PdfWriter(file.getAbsolutePath());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Titre
            document.add(new Paragraph("Rapport Détaillé des Formations")
                    .setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20));

            List<Formation> allFormations = fs.afficherFormations();
            
            // Section Formations Payantes
            document.add(new Paragraph("Formations Payantes")
                    .setBold().setFontSize(16).setFontColor(ColorConstants.BLUE).setMarginBottom(10));
            
            Table paidTable = new Table(UnitValue.createPointArray(new float[]{100, 100, 150, 100}));
            paidTable.addHeaderCell(new Cell().add(new Paragraph("Titre")).setBold());
            paidTable.addHeaderCell(new Cell().add(new Paragraph("Prix")).setBold());
            paidTable.addHeaderCell(new Cell().add(new Paragraph("Acheteurs")).setBold());
            paidTable.addHeaderCell(new Cell().add(new Paragraph("Date")).setBold());

            // Récupérer les acheteurs pour toutes les formations payantes en une fois (ou par formation)
            for (Formation f : allFormations) {
                if (f.isPayante()) {
                    List<String> buyers = getBuyersForFormation(f.getId());
                    paidTable.addCell(f.getTitre());
                    paidTable.addCell(String.format("%.2f DT", f.getPrix()));
                    paidTable.addCell(buyers.isEmpty() ? "Aucun achat" : String.join(", ", buyers));
                    paidTable.addCell(f.getDateDebut() != null ? f.getDateDebut().toString() : "-");
                }
            }
            document.add(paidTable);

            // Section Formations Gratuites
            document.add(new Paragraph("\nFormations Gratuites")
                    .setBold().setFontSize(16).setFontColor(ColorConstants.GREEN).setMarginTop(20).setMarginBottom(10));
            
            Table freeTable = new Table(UnitValue.createPointArray(new float[]{150, 150, 150}));
            freeTable.addHeaderCell(new Cell().add(new Paragraph("Titre")).setBold());
            freeTable.addHeaderCell(new Cell().add(new Paragraph("Domaine")).setBold());
            freeTable.addHeaderCell(new Cell().add(new Paragraph("Date")).setBold());

            for (Formation f : allFormations) {
                if (!f.isPayante()) {
                    freeTable.addCell(f.getTitre());
                    freeTable.addCell(f.getDomaine());
                    freeTable.addCell(f.getDateDebut() != null ? f.getDateDebut().toString() : "-");
                }
            }
            document.add(freeTable);

            document.close();
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText("Le rapport PDF a été généré avec succès !");
            alert.showAndWait();

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setContentText("Erreur lors de la génération du PDF : " + ex.getMessage());
            alert.showAndWait();
        }
    }

    private List<String> getBuyersForFormation(int formationId) {
        List<String> buyers = new ArrayList<>();
        String sql = "SELECT u.full_name FROM user u " +
                     "JOIN formation_purchase fp ON u.id = fp.user_id " +
                     "WHERE fp.formation_id = ?";
        try (java.sql.Connection con = Mydatabase.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    buyers.add(rs.getString("full_name"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buyers;
    }

    private void updatePaidPurchasesCount() {
        if (lblPaidPurchases == null) {
            return;
        }
        try {
            int count = 0;
            String sql = "SELECT COUNT(*) FROM formation_purchase";
            try (java.sql.Connection con = Mydatabase.getInstance().getConnection();
                 java.sql.PreparedStatement ps = con.prepareStatement(sql);
                 java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    count = rs.getInt(1);
                }
            }
            lblPaidPurchases.setText("Formations payées par les utilisateurs : " + count);
        } catch (Exception e) {
            lblPaidPurchases.setText("Erreur stats achats: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════
    //  createCard — seuls les getStyleClass() ont changé
    // ══════════════════════════════════════════════════════════
    private VBox createCard(Formation f, int index) {
        VBox card = new VBox(0);                                 // spacing 0 : géré par padding CSS
        card.getStyleClass().add("formation-card");

        // ── Cover dégradé (+ image si dispo)
        Region cover = new Region();
        // Couleur de dégradé selon l'index pour varier
        String[] coverClasses = {"formation-cover", "course-cover", "cover-purple", "cover-teal"};
        cover.getStyleClass().addAll("formation-cover", coverClasses[index % coverClasses.length]);
        cover.setPrefHeight(120);
        applyDomainCover(cover, f.getDomaine());

        // ── Domaine (petit texte violet majuscule)
        Label code = new Label(f.getDomaine() != null ? f.getDomaine().toUpperCase() : "");
        code.getStyleClass().add("formation-domain");            // ← était "formation-code"

        // ── Titre (gros texte sombre)
        Label title = new Label(f.getTitre());
        title.getStyleClass().add("formation-title");
        title.setWrapText(true);

        // ── Badge statut vert arrondi
        Label status = new Label("● Ouvert");
        status.getStyleClass().add("formation-status-badge");    // ← était "formation-status"

        VBox body = new VBox(6);
        body.getStyleClass().add("formation-body");
        body.getChildren().addAll(code, title, status);

        // ── Editor inline (inchangé)
        VBox editor = new VBox(10);
        editor.getStyleClass().add("formation-body");
        editor.setVisible(false);
        editor.setManaged(false);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField tfTitre       = new TextField(f.getTitre());
        TextField tfLieu        = new TextField(f.getLieu());
        TextField tfDomaine     = new TextField(f.getDomaine());
        CheckBox cbPayante = new CheckBox();
        cbPayante.setSelected(f.isPayante());

        TextField tfPrix = new TextField(String.valueOf(f.getPrix()));
        tfPrix.setPromptText("Prix (DT)");
        tfPrix.setDisable(!cbPayante.isSelected());
        cbPayante.selectedProperty().addListener((obs, oldV, newV) -> {
            tfPrix.setDisable(!newV);
            if (!newV) {
                tfPrix.setText("0");
            }
        });

        TextArea  taDescription = new TextArea(f.getDescription());
        taDescription.setPrefRowCount(3);
        DatePicker dpDateDebut = new DatePicker();
        if (f.getDateDebut() != null) dpDateDebut.setValue(f.getDateDebut().toLocalDate());
        DatePicker dpDateFin = new DatePicker();
        if (f.getDateFin() != null) dpDateFin.setValue(f.getDateFin().toLocalDate());

        grid.add(new Label("Titre"),       0, 0); grid.add(tfTitre,       1, 0);
        grid.add(new Label("Lieu"),        0, 1); grid.add(tfLieu,         1, 1);
        grid.add(new Label("Domaine"),     0, 2); grid.add(tfDomaine,      1, 2);
        grid.add(new Label("Date début"),  0, 3); grid.add(dpDateDebut,     1, 3);
        grid.add(new Label("Date fin"),    0, 4); grid.add(dpDateFin,       1, 4);
        grid.add(new Label("Payante"),     0, 5); grid.add(cbPayante,      1, 5);
        grid.add(new Label("Prix"),        0, 6); grid.add(tfPrix,         1, 6);
        grid.add(new Label("Description"), 0, 7); grid.add(taDescription,  1, 7);

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

        card.setOnMouseClicked(e -> {
            if (editor.isVisible()) return;
            ouvrirCours(f);
        });

        Region divider = new Region();
        divider.getStyleClass().add("formation-divider");
        divider.setPrefHeight(1);

        // ── Footer
        String dateInfo = "";
        if (f.getDateDebut() != null) {
            dateInfo = String.valueOf(f.getDateDebut());
            if (f.getDateFin() != null) {
                dateInfo += " → " + f.getDateFin();
            }
        }
        Label meta = new Label(f.getLieu() + " • " + dateInfo);
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button fav = new Button("···");
        fav.getStyleClass().add("formation-fav");
        fav.setFocusTraversable(false);

        // Bouton Modifier — violet dégradé
        SVGPath editIcon = new SVGPath();
        editIcon.getStyleClass().add("card-edit-svg");           // ← était "btn-svg btn-svg-edit"
        editIcon.setContent("M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zm2.92 2.33H5v-.92l9.06-9.06.92.92L5.92 19.58zM20.71 7.04a1.003 1.003 0 0 0 0-1.42l-2.34-2.34a1.003 1.003 0 0 0-1.42 0l-1.83 1.83 3.75 3.75 1.84-1.82z");

        Button modifier = new Button();
        modifier.getStyleClass().add("btn-edit-card");           // ← était "btn-primary btn-icon"
        modifier.setGraphic(editIcon);
        modifier.setTooltip(new Tooltip("Modifier"));
        modifier.setFocusTraversable(false);
        modifier.setOnMouseClicked(e -> e.consume());
        modifier.setOnAction(e -> {
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

        Button supprimer = new Button();
        supprimer.getStyleClass().add("btn-delete-card");        // ← était "btn-danger btn-icon"
        supprimer.setGraphic(deleteIcon);
        supprimer.setTooltip(new Tooltip("Supprimer"));
        supprimer.setFocusTraversable(false);
        supprimer.setOnMouseClicked(e -> e.consume());
        supprimer.setOnAction(e -> supprimerFormation(f));

        btnCancel.setOnAction(e -> {
            lblEditMsg.setText("");
            editor.setVisible(false);
            editor.setManaged(false);
            body.setVisible(true);
            body.setManaged(true);
        });

        btnSave.setOnAction(e -> {
            try {
                if (tfTitre.getText().isBlank() || tfLieu.getText().isBlank()
                        || tfDomaine.getText().isBlank() || taDescription.getText().isBlank()
                        || dpDateDebut.getValue() == null || dpDateFin.getValue() == null) {
                    lblEditMsg.setText("Veuillez remplir tous les champs");
                    return;
                }
                LocalDate dateDebutValue = dpDateDebut.getValue();
                LocalDate dateFinValue = dpDateFin.getValue();
                if (dateDebutValue.isBefore(LocalDate.now())) {
                    lblEditMsg.setText("La date début doit être aujourd'hui ou plus");
                    return;
                }
                if (dateFinValue.isBefore(dateDebutValue)) {
                    lblEditMsg.setText("La date fin doit être après la date début");
                    return;
                }

                boolean payante = cbPayante.isSelected();
                float prix = 0f;
                if (payante) {
                    if (tfPrix.getText() == null || tfPrix.getText().isBlank()) {
                        lblEditMsg.setText("Veuillez saisir le prix de la formation");
                        return;
                    }
                    try {
                        prix = Float.parseFloat(tfPrix.getText().trim());
                    } catch (NumberFormatException ex) {
                        lblEditMsg.setText("Prix invalide");
                        return;
                    }
                    if (prix <= 0f) {
                        lblEditMsg.setText("Prix invalide");
                        return;
                    }
                }

                Formation updated = new Formation(
                        f.getId(), tfLieu.getText().trim(), tfTitre.getText().trim(),
                        taDescription.getText().trim(), tfDomaine.getText().trim(),
                        Date.valueOf(dpDateDebut.getValue()),
                        Date.valueOf(dpDateFin.getValue()),
                        payante,
                        prix
                );
                fs.modifierFormation(updated);
                refreshCards();
            } catch (Exception ex) {
                lblEditMsg.setText("Erreur: " + ex.getMessage());
            }
        });

        HBox footer = new HBox(8);
        footer.getStyleClass().add("formation-footer");
        footer.getChildren().addAll(meta, spacer, fav, modifier, supprimer);

        card.getChildren().addAll(cover, body, editor, divider, footer);
        return card;
    }

    private void applyDomainCover(Region cover, String domaine) {
        if (cover == null) return;
        String slug = slugify(domaine);
        if (slug.isEmpty()) return;

        java.net.URL url = getClass().getResource("/images/domains/" + slug + ".png");
        if (url == null) url = getClass().getResource("/images/domains/" + slug + ".jpg");
        if (url == null) url = getClass().getResource("/images/domains/" + slug + ".jpeg");
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

    private void supprimerFormation(Formation f) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer la formation");
        confirm.setContentText("Cette action est définitive.\n\nFormation : \"" + f.getTitre() + "\"");

        ButtonType deleteType = new ButtonType("Supprimer", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(deleteType, cancelType);

        StackPane graphic = new StackPane();
        graphic.getStyleClass().add("confirm-delete-graphic");
        graphic.setMinSize(44, 44);
        graphic.setPrefSize(44, 44);
        graphic.setMaxSize(44, 44);
        Label mark = new Label("!");
        mark.getStyleClass().add("confirm-delete-mark");
        graphic.getChildren().add(mark);
        StackPane.setAlignment(mark, Pos.CENTER);
        confirm.setGraphic(graphic);

        try {
            String css = getClass().getResource("/Dashboardcss/back.css").toExternalForm();
            confirm.getDialogPane().getStylesheets().add(css);
        } catch (Exception ignored) {
        }
        confirm.getDialogPane().getStyleClass().add("confirm-delete-dialog");
        confirm.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

        Button deleteBtn = (Button) confirm.getDialogPane().lookupButton(deleteType);
        deleteBtn.getStyleClass().add("danger-button");
        deleteBtn.setDefaultButton(false);

        Button cancelBtn = (Button) confirm.getDialogPane().lookupButton(cancelType);
        cancelBtn.getStyleClass().add("secondary-button");
        cancelBtn.setCancelButton(true);

        ButtonType result = confirm.showAndWait().orElse(cancelType);
        if (result != deleteType) return;
        try {
            fs.supprimerFormation(f.getId());
            refreshCards();
        } catch (Exception ex) {
            String msg = ex.getMessage();
            Throwable cause = ex;
            while (cause.getCause() != null) cause = cause.getCause();
            if (cause instanceof SQLException sqlEx) {
                String sqlState = sqlEx.getSQLState();
                if ((sqlState != null && sqlState.startsWith("08")) ||
                        (msg != null && msg.toLowerCase().contains("communications link failure"))) {
                    msg = "Connexion MySQL indisponible. Vérifie que le serveur MySQL est démarré.";
                }
            } else if (msg != null && msg.toLowerCase().contains("communications link failure")) {
                msg = "Connexion MySQL indisponible. Vérifie que le serveur MySQL est démarré.";
            }
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Suppression impossible");
            error.setContentText(msg);
            error.showAndWait();
        }
    }

    private void ouvrirCours(Formation f) {
        try {
            UiState.selectedFormationId = f.getId();
            UiState.selectedFormationTitre = f.getTitre();
            WindowUtil.navigate(btnRetour, "/Formation/AfficherCour.fxml", "Cours de la formation");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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