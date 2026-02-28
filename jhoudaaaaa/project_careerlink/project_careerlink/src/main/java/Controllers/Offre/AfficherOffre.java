package Controllers.Offre;

import Entities.Offre.OffreEmploi;
import Entities.Entreprise.Entreprise;
import Services.Offre.OfrreEmploiServices;
import Services.Offre.SecteurActiviteService;
import Services.Entreprise.EntrepriseService;
import Utils.Session;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import Utils.OfferDetailsDialog;
import javafx.concurrent.Task;

import javafx.scene.Node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AfficherOffre {

    @FXML
    private FlowPane offersFlowPane;

    @FXML
    private ListView<OffreEmploi> offersListView;

    @FXML
    private ScrollPane offersScrollPane;

    @FXML
    private PieChart statusChart;

    @FXML
    private PieChart secteurChart;

    @FXML
    private PieChart typeContratChart;

    @FXML
    private Label statusTotalLabel;

    @FXML
    private Label secteurTotalLabel;

    @FXML
    private Label contratTotalLabel;

    @FXML
    private VBox statusLegendBox;

    @FXML
    private VBox secteurLegendBox;

    @FXML
    private VBox contratLegendBox;

    @FXML
    private ComboBox<String> secteurFilter;

    @FXML
    private VBox locationListBox;

    @FXML
    private HBox positionChipsBox;

    private final OfrreEmploiServices service = new OfrreEmploiServices();
    private final SecteurActiviteService secteurService = new SecteurActiviteService();
    private final EntrepriseService entrepriseService = new EntrepriseService();
    private ObservableList<OffreEmploi> master;
    private FilteredList<OffreEmploi> filtered;
    private ObservableList<VBox> selectedCards = FXCollections.observableArrayList();
    private final ObservableList<OffreEmploi> selectedOffers = FXCollections.observableArrayList();

    private String selectedLocationKey;

    @FXML
    private StackPane dashCenter;

    @FXML
    private Button addBtn;

    private ProgressIndicator loadingIndicator;

    @FXML
    void initialize() {
        setupSecteurFilter();
        reloadData();

        setupResponsiveGrid();

        setupCharts();

        setupOffersListView();

        playEntryAnimation(dashCenter);
    }

    private void setupOffersListView() {
        if (offersListView == null) {
            return;
        }

        offersListView.setFocusTraversable(false);
        offersListView.setCache(true);
        offersListView.setCacheShape(true);
        offersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        offersListView.setCellFactory(lv -> new OfferCardCell());

        offersListView.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<OffreEmploi>) c -> {
            selectedOffers.setAll(offersListView.getSelectionModel().getSelectedItems());
        });
    }

    private final class OfferCardCell extends ListCell<OffreEmploi> {
        private final Label titleLabel = new Label();
        private final Label statusLabel = new Label();
        private final Label descriptionLabel = new Label();
        private final VBox detailsBox = new VBox(6);

        private final Label locValue = new Label();
        private final Label contratValue = new Label();
        private final Label secteurValue = new Label();
        private final Label emploiValue = new Label();
        private final Label expValue = new Label();
        private final Label postesValue = new Label();
        private final Label deadlineValue = new Label();
        private final Button detailsBtn = new Button("Détails");
        private final VBox card;
        private OffreEmploi current;

        private OfferCardCell() {
            HBox header = new HBox();
            header.getStyleClass().add("offer-card-header");

            titleLabel.getStyleClass().add("offer-card-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            titleLabel.setWrapText(false);
            titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

            statusLabel.getStyleClass().add("offer-card-status");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().addAll(titleLabel, spacer, statusLabel);

            descriptionLabel.getStyleClass().add("offer-card-description");
            descriptionLabel.setWrapText(true);

            detailsBox.getStyleClass().add("offer-card-details");

            detailsBox.getChildren().addAll(
                    createDetailRow("📍", locValue),
                    createDetailRow("📋", contratValue),
                    createDetailRow("🏢", secteurValue),
                    createDetailRow("💼", emploiValue),
                    createDetailRow("⭐", expValue),
                    createDetailRow("👥", postesValue),
                    createDetailRow("⏳", deadlineValue)
            );

            detailsBtn.getStyleClass().addAll("btn-secondary");
            detailsBtn.setOnAction(e -> {
                OffreEmploi o = current;
                if (o == null) {
                    return;
                }
                if (detailsBtn.getScene() != null) {
                    OfferDetailsDialog.show(detailsBtn.getScene().getWindow(), o, "/cssOffre/backoffice_dashboard.css", false);
                } else {
                    OfferDetailsDialog.show(null, o, "/cssOffre/backoffice_dashboard.css", false);
                }
                e.consume();
            });

            HBox actions = new HBox(10);
            actions.getStyleClass().add("offer-card-footer");
            actions.getChildren().add(detailsBtn);

            VBox content = new VBox(8);
            content.getStyleClass().add("offer-card-content");
            content.getChildren().addAll(descriptionLabel, detailsBox, actions);

            card = new VBox(0, header, content);
            card.getStyleClass().addAll("offer-card", "offer-card-back");
            card.setPadding(new Insets(0));
            card.setCache(true);
            card.setCacheShape(true);
            card.setCacheHint(javafx.scene.CacheHint.SPEED);

            setText(null);
            setGraphic(card);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            // La largeur de la carte suit la largeur du ListView.
            // Important: listView peut être null au moment du constructeur.
            listViewProperty().addListener((obs, oldLv, newLv) -> {
                if (newLv != null) {
                    card.prefWidthProperty().bind(newLv.widthProperty().subtract(30));
                    card.maxWidthProperty().bind(newLv.widthProperty().subtract(30));
                }
            });
        }

        private HBox createDetailRow(String icon, Label valueLabel) {
            HBox box = new HBox(8);
            box.getStyleClass().add("offer-card-detail");

            Label iconLabel = new Label(icon);
            iconLabel.getStyleClass().add("offer-card-icon");

            valueLabel.getStyleClass().add("offer-card-value");
            valueLabel.setWrapText(true);
            valueLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(valueLabel, Priority.ALWAYS);

            box.getChildren().addAll(iconLabel, valueLabel);
            return box;
        }

        @Override
        protected void updateItem(OffreEmploi offre, boolean empty) {
            super.updateItem(offre, empty);
            current = empty ? null : offre;
            if (empty || offre == null) {
                setGraphic(null);
                return;
            }

            setGraphic(card);

            titleLabel.setText(offre.getTitre() != null ? offre.getTitre() : "Sans titre");
            statusLabel.setText(offre.getStatus() != null ? offre.getStatus() : "N/A");

            String desc = offre.getDescription();
            if (desc != null && desc.length() > 80) {
                desc = desc.substring(0, 77) + "...";
            }
            descriptionLabel.setText(desc != null ? desc : "Aucune description");

            String locText;
            if (offre.getLocalisation() == null) {
                locText = "Non spécifiée";
            } else {
                String a = offre.getLocalisation().getAdresse() != null ? offre.getLocalisation().getAdresse().trim() : "";
                if (!a.isEmpty()) {
                    locText = a;
                } else {
                    String v = offre.getLocalisation().getVille();
                    String p = offre.getLocalisation().getPays();
                    String vv = v != null ? v.trim() : "";
                    String pp = p != null ? p.trim() : "";
                    if (vv.isEmpty() && pp.isEmpty()) {
                        locText = "Non spécifiée";
                    } else if (vv.isEmpty()) {
                        locText = pp;
                    } else if (pp.isEmpty()) {
                        locText = vv;
                    } else {
                        locText = vv + ", " + pp;
                    }
                }
            }

            locValue.setText(locText);
            contratValue.setText(offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : "Non spécifié");
            secteurValue.setText(offre.getSecteur() != null ? offre.getSecteur().getNom() : "Non spécifié");
            emploiValue.setText(offre.getTypeEmploi() != null ? offre.getTypeEmploi().getNom() : "Non spécifié");
            expValue.setText(offre.getNiveauExperience() != null && !offre.getNiveauExperience().trim().isEmpty()
                    ? offre.getNiveauExperience()
                    : "Exp. non définie");
            postesValue.setText(offre.getNombrePoste() != null ? (offre.getNombrePoste() + " poste(s)") : "Postes: -");
            deadlineValue.setText("Limite: " + (offre.getDateLimiteCandidature() != null ? String.valueOf(offre.getDateLimiteCandidature()) : "-")
                    + " · " + (Boolean.TRUE.equals(offre.getTeletravail()) ? "Télétravail" : "Présentiel"));

            // Style sélection
            if (getListView() != null && getListView().getSelectionModel().getSelectedItems().contains(offre)) {
                if (!card.getStyleClass().contains("selected")) {
                    card.getStyleClass().add("selected");
                }
            } else {
                card.getStyleClass().remove("selected");
            }
        }
    }

    @FXML
    private void openAjouterOffre(ActionEvent event) {
        try {
            Parent page = FXMLLoader.load(getClass().getResource("/Offre/AjouterOffre.fxml"));

            Node source = event != null ? (Node) event.getSource() : null;
            Scene scene = source != null ? source.getScene() : (dashCenter != null ? dashCenter.getScene() : null);
            if (scene != null) {
                Node host = scene.lookup("#adminContentArea");
                if (host instanceof StackPane sp) {
                    sp.getChildren().setAll(page);
                    return;
                }
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Ajouter offre");
            stage.setScene(new Scene(page));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir AjouterOffre.fxml: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupCharts() {
        setupPie(statusChart);
        setupPie(secteurChart);
        setupPie(typeContratChart);
    }

    private void setupPie(PieChart chart) {
        if (chart == null) {
            return;
        }
        chart.setTitle(null);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(false);
        chart.setClockwise(true);
        chart.setStartAngle(90);
    }

    private void setTotal(Label label, long total) {
        if (label == null) {
            return;
        }
        label.setText("Total\n" + total);
    }

    private void renderLegend(VBox legendBox, List<PieChart.Data> data) {
        if (legendBox == null) {
            return;
        }

        legendBox.getChildren().clear();
        if (data == null || data.isEmpty()) {
            return;
        }

        double sum = data.stream().mapToDouble(PieChart.Data::getPieValue).sum();
        int i = 0;
        for (PieChart.Data d : data) {
            String name = d.getName() != null ? d.getName() : "";
            long value = (long) d.getPieValue();
            int pct = sum <= 0 ? 0 : (int) Math.round((d.getPieValue() * 100.0) / sum);

            Region dot = new Region();
            dot.getStyleClass().addAll("legend-dot", "legend-color-" + (i % 6));
            dot.setMinSize(10, 10);
            dot.setPrefSize(10, 10);
            dot.setMaxSize(10, 10);

            Label left = new Label(name);
            left.getStyleClass().add("legend-name");
            left.setTextOverrun(OverrunStyle.ELLIPSIS);
            left.setMaxWidth(Double.MAX_VALUE);

            Label right = new Label(pct + "% (" + value + ")");
            right.getStyleClass().add("legend-pct");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(8, dot, left, spacer, right);
            row.getStyleClass().add("legend-row");

            legendBox.getChildren().add(row);
            i++;
        }
    }

    private void installSliceTooltips(PieChart chart) {
        if (chart == null || chart.getData() == null) {
            return;
        }
        double sum = chart.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();
        for (PieChart.Data d : chart.getData()) {
            String name = d.getName() != null ? d.getName() : "";
            long value = (long) d.getPieValue();
            int pct = sum <= 0 ? 0 : (int) Math.round((d.getPieValue() * 100.0) / sum);
            Tooltip tooltip = new Tooltip(name + ": " + value + " (" + pct + "%)");
            tooltip.setStyle("-fx-font-size: 12px;");
            if (d.getNode() != null) {
                Tooltip.install(d.getNode(), tooltip);
            }
        }
    }

    private void updateCharts() {
        if (filtered == null) {
            return;
        }
        
        // Limiter les mises à jour fréquentes - si peu d'offres, mettre à jour directement
        // sinon utiliser runLater pour grouper les mises à jour
        int size = filtered.size();
        if (size > 100) {
            // Pour les grandes listes, ne pas mettre à jour les graphiques automatiquement
            // pour éviter le lag
            return;
        }

        if (statusChart != null) {
            Map<String, Long> byStatus = filtered.stream()
                    .collect(Collectors.groupingBy(o -> {
                        if (o == null || o.getStatus() == null) {
                            return "Non défini";
                        }
                        String s = o.getStatus().trim();
                        return s.isEmpty() ? "Non défini" : s;
                    }, LinkedHashMap::new, Collectors.counting()));

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            for (Map.Entry<String, Long> e : byStatus.entrySet()) {
                data.add(new PieChart.Data(e.getKey(), e.getValue()));
            }
            statusChart.setData(data);
            setTotal(statusTotalLabel, data.stream().mapToLong(d -> (long) d.getPieValue()).sum());
            renderLegend(statusLegendBox, data);
            installSliceTooltips(statusChart);
        }

        if (secteurChart != null) {
            Map<String, Long> bySecteur = filtered.stream()
                    .collect(Collectors.groupingBy(o -> {
                        if (o == null || o.getSecteur() == null || o.getSecteur().getNom() == null) {
                            return "Non défini";
                        }
                        String s = o.getSecteur().getNom().trim();
                        return s.isEmpty() ? "Non défini" : s;
                    }, LinkedHashMap::new, Collectors.counting()));

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            for (Map.Entry<String, Long> e : bySecteur.entrySet()) {
                data.add(new PieChart.Data(e.getKey(), e.getValue()));
            }
            secteurChart.setData(data);
            setTotal(secteurTotalLabel, data.stream().mapToLong(d -> (long) d.getPieValue()).sum());
            renderLegend(secteurLegendBox, data);
            installSliceTooltips(secteurChart);
        }

        if (typeContratChart != null) {
            Map<String, Long> byContrat = filtered.stream()
                    .collect(Collectors.groupingBy(o -> {
                        if (o == null || o.getTypeContrat() == null || o.getTypeContrat().getNom() == null) {
                            return "Non défini";
                        }
                        String s = o.getTypeContrat().getNom().trim();
                        return s.isEmpty() ? "Non défini" : s;
                    }, LinkedHashMap::new, Collectors.counting()));

            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
            for (Map.Entry<String, Long> e : byContrat.entrySet()) {
                data.add(new PieChart.Data(e.getKey(), e.getValue()));
            }
            typeContratChart.setData(data);
            setTotal(contratTotalLabel, data.stream().mapToLong(d -> (long) d.getPieValue()).sum());
            renderLegend(contratLegendBox, data);
            installSliceTooltips(typeContratChart);
        }
    }

    private void updateLocationMap() {
        if (locationListBox == null || filtered == null) {
            return;
        }

        Map<String, Long> rawByLocation = getCountsByLocation(getOffersForLocationUi());
        if (rawByLocation.isEmpty()) {
            locationListBox.getChildren().clear();
            return;
        }

        long max = rawByLocation.values().stream().mapToLong(Long::longValue).max().orElse(1);
        long total = rawByLocation.values().stream().mapToLong(Long::longValue).sum();

        locationListBox.getChildren().clear();

        VBox allRow = createLocationRow("Toutes les positions", total, max, null);
        if (selectedLocationKey == null) {
            allRow.getStyleClass().add("selected");
        }
        allRow.setOnMouseClicked(e -> {
            selectedLocationKey = null;
            applyFilter();
        });
        locationListBox.getChildren().add(allRow);

        for (Map.Entry<String, Long> e : rawByLocation.entrySet()) {
            String name = e.getKey();
            long value = e.getValue();
            VBox row = createLocationRow(name, value, max, name);
            if (name != null && name.equals(selectedLocationKey)) {
                row.getStyleClass().add("selected");
            }
            row.setOnMouseClicked(ev -> {
                if (name != null && name.equals(selectedLocationKey)) {
                    selectedLocationKey = null;
                } else {
                    selectedLocationKey = name;
                }
                applyFilter();
            });
            locationListBox.getChildren().add(row);
        }
    }

    private void updatePositionSelector() {
        if (positionChipsBox == null) {
            return;
        }

        Map<String, Long> counts = getCountsByLocation(getOffersForLocationUi());
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        positionChipsBox.getChildren().clear();

        Button all = new Button("Toutes les positions (" + total + ")");
        all.getStyleClass().add("pos-chip");
        if (selectedLocationKey == null) {
            all.getStyleClass().add("selected");
        }
        all.setOnAction(e -> {
            selectedLocationKey = null;
            applyFilter();
        });
        positionChipsBox.getChildren().add(all);

        for (Map.Entry<String, Long> e : counts.entrySet()) {
            String key = e.getKey();
            long value = e.getValue();
            Button chip = new Button(key + " (" + value + ")");
            chip.getStyleClass().add("pos-chip");
            if (key != null && key.equals(selectedLocationKey)) {
                chip.getStyleClass().add("selected");
            }
            chip.setOnAction(ev -> {
                if (key != null && key.equals(selectedLocationKey)) {
                    selectedLocationKey = null;
                } else {
                    selectedLocationKey = key;
                }
                applyFilter();
            });
            positionChipsBox.getChildren().add(chip);
        }
    }

    private List<OffreEmploi> getOffersForLocationUi() {
        if (master == null) {
            return List.of();
        }

        String secteurSelected = secteurFilter != null ? secteurFilter.getValue() : null;
        return master.stream().filter(o -> {
            if (secteurSelected == null || secteurSelected.equals("Tous")) {
                return true;
            }
            if (o == null || o.getSecteur() == null || o.getSecteur().getNom() == null) {
                return false;
            }
            return secteurSelected.equalsIgnoreCase(o.getSecteur().getNom());
        }).toList();
    }

    private Map<String, Long> getCountsByLocation(List<OffreEmploi> offers) {
        if (offers == null || offers.isEmpty()) {
            return new LinkedHashMap<>();
        }

        return offers.stream()
                .collect(Collectors.groupingBy(this::getLocationKey, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));
    }

    private VBox createLocationRow(String titleText, long value, long max, String key) {
        Label icon = new Label("📍");
        icon.getStyleClass().add("loc-row-icon");

        Label title = new Label(titleText != null ? titleText : "");
        title.getStyleClass().add("loc-row-title");
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setMaxWidth(Double.MAX_VALUE);

        Label badge = new Label(value + " offre" + (value > 1 ? "s" : ""));
        badge.getStyleClass().add("loc-row-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox top = new HBox(10, icon, title, spacer, badge);
        top.getStyleClass().add("loc-row-top");

        Region fill = new Region();
        fill.getStyleClass().add("loc-row-bar-fill");
        fill.setMinHeight(6);
        fill.setPrefHeight(6);
        fill.setMaxHeight(6);

        StackPane bar = new StackPane(fill);
        bar.getStyleClass().add("loc-row-bar");
        bar.setMinHeight(6);
        bar.setPrefHeight(6);
        bar.setMaxHeight(6);
        StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);

        double pct = max <= 0 ? 0 : Math.max(0, Math.min(1, value * 1.0 / max));
        fill.prefWidthProperty().bind(bar.widthProperty().multiply(pct));

        VBox row = new VBox(8, top, bar);
        row.getStyleClass().add("loc-row");
        row.setUserData(key);
        Tooltip.install(row, new Tooltip((titleText != null ? titleText : "") + " : " + value));
        return row;
    }

    private String getLocationKey(OffreEmploi o) {
        if (o == null || o.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String ville = o.getLocalisation().getVille();
        String pays = o.getLocalisation().getPays();
        String v = ville != null ? ville.trim() : "";
        String p = pays != null ? pays.trim() : "";
        if (v.isEmpty() && p.isEmpty()) {
            return "Non spécifiée";
        }
        if (v.isEmpty()) {
            return p;
        }
        if (p.isEmpty()) {
            return v;
        }
        return v + ", " + p;
    }

    private void setupResponsiveGrid() {
        // ListView est virtualisé, pas besoin de gérer wrapLength
    }

    @FXML
    void showStats(ActionEvent event) {
        if (master == null) {
            reloadData();
        }
        if (master == null || master.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Statistiques");
            alert.setHeaderText(null);
            alert.setContentText("Aucune offre à analyser.");
            alert.showAndWait();
            return;
        }

        Map<String, Long> counts = master.stream()
                .collect(Collectors.groupingBy(o -> {
                    if (o == null || o.getTypeContrat() == null || o.getTypeContrat().getNom() == null) {
                        return "Non défini";
                    }
                    String n = o.getTypeContrat().getNom();
                    return n == null || n.trim().isEmpty() ? "Non défini" : n.trim();
                }, LinkedHashMap::new, Collectors.counting()));

        PieChart chart = new PieChart();
        chart.setTitle("Répartition des offres par type de contrat");
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            chart.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        }

        Label title = new Label("Statistiques");
        title.getStyleClass().add("stats-title");

        javafx.scene.control.Button close = new javafx.scene.control.Button("Fermer");
        close.getStyleClass().add("secondary-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        HBox header = new HBox(10, title, spacer, close);
        header.getStyleClass().add("stats-header");

        VBox root = new VBox(14, header, chart);
        root.getStyleClass().addAll("card", "stats-card");

        Scene scene = new Scene(root, 720, 520);
        try {
            scene.getStylesheets().add(getClass().getResource("/cssOffre/carrerlink.css").toExternalForm());
        } catch (Exception ignored) {
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Statistiques offres");
        stage.setScene(scene);

        close.setOnAction(e -> stage.close());

        stage.showAndWait();
    }

    @FXML
    void editSelected(ActionEvent event) {
        if (selectedOffers == null || selectedOffers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Modification");
            alert.setHeaderText(null);
            alert.setContentText("Sélectionne une offre à modifier.");
            alert.showAndWait();
            return;
        }
        if (selectedOffers.size() != 1) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Modification");
            alert.setHeaderText(null);
            alert.setContentText("Pour modifier, sélectionne une seule offre.");
            alert.showAndWait();
            return;
        }

        OffreEmploi toEdit = selectedOffers.get(0);
        if (toEdit == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Offre/AjouterOffre.fxml"));
            Parent root = loader.load();

            AjouterOffre controller = loader.getController();
            if (controller != null) {
                controller.setEditingOffre(toEdit);
            }

            Scene scene = new Scene(root);
            try {
                scene.getStylesheets().add(getClass().getResource("/cssOffre/carrerlink.css").toExternalForm());
            } catch (Exception ignored) {
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier offre");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.showAndWait();

            reloadData();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void refresh(ActionEvent event) {
        reloadData();
    }

    @FXML
    void deleteSelected(ActionEvent event) {
        if (selectedOffers == null || selectedOffers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Suppression");
            alert.setHeaderText(null);
            alert.setContentText("Sélectionne une ou plusieurs offres à supprimer.");
            alert.showAndWait();
            return;
        }

        List<OffreEmploi> selected = new java.util.ArrayList<>(selectedOffers);

        String titles = selected.stream()
                .limit(5)
                .map(o -> o != null ? o.getTitre() : "")
                .collect(Collectors.joining("\n- ", "- ", selected.size() > 5 ? "\n..." : ""));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + selected.size() + " offre(s) ?\n\n" + titles);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            for (OffreEmploi o : selected) {
                if (o != null) {
                    service.supprimerOffreEmploi(o.getId());
                }
            }
            reloadData();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void setupSecteurFilter() {
        if (secteurFilter == null) {
            return;
        }

        secteurFilter.getItems().clear();
        secteurFilter.getItems().add("Tous");
        try {
            secteurFilter.getItems().addAll(
                    secteurService.getAll().stream().map(s -> s != null ? s.getNom() : "").toList()
            );
        } catch (Exception ignored) {
        }
        secteurFilter.getSelectionModel().select("Tous");

        secteurFilter.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter());
    }

    private void reloadData() {
        showLoading(true);

        Task<List<OffreEmploi>> task = new Task<>() {
            @Override
            protected List<OffreEmploi> call() {
                if (Session.getCurrentUser() == null) {
                    return java.util.Collections.emptyList();
                }

                Entreprise e = entrepriseService.getByUserId(Session.getCurrentUser().getId());
                if (e == null) {
                    return java.util.Collections.emptyList();
                }

                // Utiliser la version allégée pour de meilleures performances (filtrée par entreprise)
                return service.afficherOfrreEmploiLightByEntrepriseId(e.getIdEntreprise());
            }
        };

        task.setOnSucceeded(e -> {
            List<OffreEmploi> res = task.getValue();
            master = FXCollections.observableArrayList(res != null ? res : Collections.emptyList());
            filtered = new FilteredList<>(master, o -> true);
            applyFilter();
            updatePositionSelector();
            showLoading(false);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors du chargement des offres");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "load-offres");
        th.setDaemon(true);
        th.start();
    }

    private void applyFilter() {
        if (filtered == null) {
            return;
        }

        String secteurSelected = secteurFilter != null ? secteurFilter.getValue() : null;
        String locationSelected = selectedLocationKey;
        
        filtered.setPredicate(o -> {
            boolean secteurOk;
            if (secteurSelected == null || secteurSelected.equals("Tous")) {
                secteurOk = true;
            } else if (o == null || o.getSecteur() == null || o.getSecteur().getNom() == null) {
                secteurOk = false;
            } else {
                secteurOk = secteurSelected.equalsIgnoreCase(o.getSecteur().getNom());
            }

            boolean locationOk;
            if (locationSelected == null || locationSelected.isEmpty()) {
                locationOk = true;
            } else {
                String key = getLocationKey(o);
                locationOk = locationSelected.equals(key);
            }

            return secteurOk && locationOk;
        });

        // Rafraîchir les cartes après le filtrage (une seule fois)
        refreshCardsAsync();
        updateCharts();
        updateLocationMap();
    }

    private void refreshCardsAsync() {
        if (offersListView == null) {
            return;
        }

        selectedOffers.clear();
        offersListView.getSelectionModel().clearSelection();

        if (filtered == null) {
            offersListView.setItems(FXCollections.observableArrayList());
            return;
        }

        // ListView est virtualisé: on peut binder directement la liste filtrée
        offersListView.setItems(filtered);
        offersListView.refresh();
    }

    private void refreshCards() {
        refreshCardsAsync();
    }

    private void showLoading(boolean show) {
        if (dashCenter == null) {
            return;
        }
        if (loadingIndicator == null) {
            loadingIndicator = new ProgressIndicator();
            loadingIndicator.setMaxSize(80, 80);
        }
        if (show) {
            if (!dashCenter.getChildren().contains(loadingIndicator)) {
                dashCenter.getChildren().add(loadingIndicator);
            }
        } else {
            dashCenter.getChildren().remove(loadingIndicator);
        }
    }

    private VBox createOfferCard(OffreEmploi offre) {
        // Header avec titre et status
        HBox header = new HBox();
        header.getStyleClass().add("offer-card-header");
        
        Label titleLabel = new Label(offre.getTitre() != null ? offre.getTitre() : "Sans titre");
        titleLabel.getStyleClass().add("offer-card-title");
        titleLabel.setMaxWidth(200);
        titleLabel.setWrapText(false);
        titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        
        Label statusLabel = new Label(offre.getStatus() != null ? offre.getStatus() : "N/A");
        statusLabel.getStyleClass().add("offer-card-status");
        
        header.getChildren().addAll(titleLabel, new Region(), statusLabel);
        HBox.setHgrow(header.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);

        // Description
        Label descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("offer-card-description");
        String desc = offre.getDescription();
        if (desc != null && desc.length() > 80) {
            desc = desc.substring(0, 77) + "...";
        }
        descriptionLabel.setText(desc != null ? desc : "Aucune description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(60);

        // Détails
        VBox detailsBox = new VBox(6);
        detailsBox.getStyleClass().add("offer-card-details");

        // Localisation
        String locText;
        if (offre.getLocalisation() == null) {
            locText = "Non spécifiée";
        } else {
            String a = offre.getLocalisation().getAdresse() != null ? offre.getLocalisation().getAdresse().trim() : "";
            if (!a.isEmpty()) {
                locText = a;
            } else {
                String v = offre.getLocalisation().getVille();
                String p = offre.getLocalisation().getPays();
                String vv = v != null ? v.trim() : "";
                String pp = p != null ? p.trim() : "";
                if (vv.isEmpty() && pp.isEmpty()) {
                    locText = "Non spécifiée";
                } else if (vv.isEmpty()) {
                    locText = pp;
                } else if (pp.isEmpty()) {
                    locText = vv;
                } else {
                    locText = vv + ", " + pp;
                }
            }
        }
        HBox localisationBox = createDetailBox("📍", 
            locText);

        // Type de contrat
        HBox contratBox = createDetailBox("📋", 
            offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : "Non spécifié");

        // Secteur
        HBox secteurBox = createDetailBox("🏢", 
            offre.getSecteur() != null ? offre.getSecteur().getNom() : "Non spécifié");

        // Type d'emploi
        HBox emploiBox = createDetailBox("💼", 
            offre.getTypeEmploi() != null ? offre.getTypeEmploi().getNom() : "Non spécifié");

        // Niveau d'expérience
        HBox expBox = createDetailBox("⭐",
            offre.getNiveauExperience() != null && !offre.getNiveauExperience().trim().isEmpty() ? offre.getNiveauExperience() : "Exp. non définie");

        // Nombre de postes
        HBox postesBox = createDetailBox("👥",
            offre.getNombrePoste() != null ? (offre.getNombrePoste() + " poste(s)") : "Postes: -");

        // Deadline / télétravail
        String deadline = offre.getDateLimiteCandidature() != null ? String.valueOf(offre.getDateLimiteCandidature()) : "-";
        String remote = Boolean.TRUE.equals(offre.getTeletravail()) ? "Télétravail" : "Présentiel";
        HBox dlBox = createDetailBox("⏳", "Limite: " + deadline + " · " + remote);

        detailsBox.getChildren().addAll(localisationBox, contratBox, secteurBox, emploiBox, expBox, postesBox, dlBox);

        // Assemblage de la carte
        VBox content = new VBox(8);

        VBox card = new VBox(0, header, content);
        card.getStyleClass().addAll("offer-card", "offer-card-back");
        card.setUserData(offre);
        card.setPadding(new Insets(0));

        card.setCache(true);
        card.setCacheShape(true);
        card.setCacheHint(javafx.scene.CacheHint.SPEED);

        // La sélection est gérée par ListView
        return card;
    }

    private HBox createDetailBox(String icon, String value) {
        HBox box = new HBox(8);
        box.getStyleClass().add("offer-card-detail");
        
        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("offer-card-icon");
        
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("offer-card-value");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(200);
        
        box.getChildren().addAll(iconLabel, valueLabel);
        return box;
    }

    private void playEntryAnimation(javafx.scene.Node node) {
        // no-op (animations disabled for performance)
    }

    private void installHoverAnimation(javafx.scene.Node node) {
        return;
    }
}
