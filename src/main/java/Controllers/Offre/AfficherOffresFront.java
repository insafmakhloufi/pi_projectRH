package Controllers.Offre;

import Controllers.candidature.AjouterCandidature;
import Entities.Offre.OffreCompatibilite;
import Entities.Offre.OffreEmploi;
import Services.Offre.FavorisOffreService;
import Services.Offre.OfrreEmploiServices;
import Services.Offre.SecteurActiviteService;
import Utils.Session;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import Services.Offre.JobHeatMapService;
import Entities.Offre.HeatScore;
import Services.Ai.TranslationService;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AfficherOffresFront {

    @FXML
    private FlowPane offersFlowPane;

    @FXML
    private ComboBox<String> secteurFilter;

    @FXML
    private ComboBox<String> dateFilter;

    @FXML
    private HBox positionChipsBox;

    @FXML
    private VBox locationListBox;

    @FXML
    private AreaChart<String, Number> secteurAreaChart;

    @FXML
    private CategoryAxis secteurChartXAxis;

    @FXML
    private NumberAxis secteurChartYAxis;

    @FXML
    private Button refreshBtn;

    @FXML
    private Button btnRecommandees;

    @FXML
    private Button btnFavoris;

    @FXML
    private Button btnChaudes;

    @FXML
    private VBox statsPanel;

    @FXML
    private VBox detailsPanel;

    @FXML
    private VBox offerDetailsContent;

    private OffreEmploi currentSelectedOffer;

    private ProgressIndicator loadingIndicator;

    private Timeline cardBuildTimeline;

    private Timeline filterDebounceTimeline;
    private String lastSecteurForStats;

    private final OfrreEmploiServices service = new OfrreEmploiServices();
    private final SecteurActiviteService secteurService = new SecteurActiviteService();

    private ObservableList<OffreEmploi> master;
    private FilteredList<OffreEmploi> filtered;
    private String selectedLocationKey;

    /**
     * Affiche les offres triées par score de chaleur (Heat-Map)
     * Les offres les plus urgentes/rares/fraîches apparaissent en premier
     */
    @FXML
    void afficherOffresChaudes(ActionEvent event) {
        showLoading(true);

        Task<List<OffreEmploi>> task = new Task<>() {
            @Override
            protected List<OffreEmploi> call() throws Exception {
                // Charger toutes les offres actives
                List<OffreEmploi> allOffers = service.afficherOfrreEmploiLight();
                
                // Filtrer uniquement les offres actives avec un score >= 30
                JobHeatMapService heatService = new JobHeatMapService();
                return allOffers.stream()
                    .filter(o -> "active".equalsIgnoreCase(o.getStatus()))
                    .filter(o -> {
                        HeatScore score = heatService.calculerHeatScore(o);
                        return score != null && score.getScoreGlobal() >= 30;
                    })
                    .sorted((o1, o2) -> {
                        HeatScore h1 = heatService.calculerHeatScore(o1);
                        HeatScore h2 = heatService.calculerHeatScore(o2);
                        if (h1 == null && h2 == null) return 0;
                        if (h1 == null) return 1;
                        if (h2 == null) return -1;
                        return Integer.compare(h2.getScoreGlobal(), h1.getScoreGlobal());
                    })
                    .limit(10)
                    .collect(Collectors.toList());
            }
        };

        task.setOnSucceeded(e -> {
            List<OffreEmploi> offresChaudes = task.getValue();
            if (offresChaudes == null || offresChaudes.isEmpty()) {
                offersFlowPane.getChildren().clear();
                VBox empty = new VBox(10);
                empty.getStyleClass().addAll("wizard-card");
                empty.setPadding(new Insets(18));
                Label t = new Label("🔥 Aucune offre 'chaude' pour le moment");
                t.getStyleClass().add("hero-title");
                Label s = new Label("Les offres chaudes sont celles qui ferment bientôt ou ont peu de postes disponibles.");
                s.getStyleClass().add("hero-subtitle");
                empty.getChildren().addAll(t, s);
                offersFlowPane.getChildren().add(empty);
            } else {
                // Créer le panneau de statistiques Heat-Map professionnel
                VBox heatMapStatsPanel = createHeatMapStatsPanel(offresChaudes);
                
                // Afficher les offres triées par chaleur
                offersFlowPane.getChildren().clear();
                offersFlowPane.getChildren().add(heatMapStatsPanel);
                for (OffreEmploi o : offresChaudes) {
                    offersFlowPane.getChildren().add(createCard(o));
                }
            }
            showLoading(false);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors du chargement des offres chaudes");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "load-offres-chaudes");
        th.setDaemon(true);
        th.start();
    }

    /**
     * Crée un panneau de statistiques Heat-Map PROFESSIONNEL avec texte visible
     */
    private VBox createHeatMapStatsPanel(List<OffreEmploi> offres) {
        JobHeatMapService heatService = new JobHeatMapService();
        
        int countUltra = 0, countHigh = 0, countMedium = 0;
        double avgScore = 0;
        int totalScore = 0;
        int count = 0;
        
        for (OffreEmploi o : offres) {
            HeatScore score = heatService.calculerHeatScore(o);
            if (score != null) {
                if (score.getScoreGlobal() >= 85) countUltra++;
                else if (score.getScoreGlobal() >= 70) countHigh++;
                else countMedium++;
                totalScore += score.getScoreGlobal();
                count++;
            }
        }
        
        avgScore = count > 0 ? (double) totalScore / count : 0;
        int total = offres.size();
        int pctUltra = total > 0 ? countUltra * 100 / total : 0;
        int pctHigh = total > 0 ? countHigh * 100 / total : 0;
        int pctMedium = total > 0 ? countMedium * 100 / total : 0;
        
        // Panneau principal - FOND BLANC avec bordure grise
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(25));
        panel.setMaxWidth(Double.MAX_VALUE);
        panel.setStyle("-fx-background-color: #ffffff; " +
                      "-fx-background-radius: 16; " +
                      "-fx-border-color: #d1d5db; " +
                      "-fx-border-width: 2; " +
                      "-fx-border-radius: 16; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8);");
        
        // TITRE PRINCIPAL avec icône
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label fireIcon = new Label("🔥");
        fireIcon.setStyle("-fx-font-size: 28px;");
        
        VBox titleBox = new VBox(3);
        Label mainTitle = new Label("HEAT-MAP DES OPPORTUNITÉS");
        mainTitle.setStyle("-fx-text-fill: #1f2937; -fx-font-size: 18px; -fx-font-weight: 800;");
        
        Label subtitle = new Label(String.format("Score moyen: %.0f%% | %d offre%s analysée%s", 
            avgScore, total, total > 1 ? "s" : "", total > 1 ? "s" : ""));
        subtitle.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 13px; -fx-font-weight: 500;");
        
        titleBox.getChildren().addAll(mainTitle, subtitle);
        header.getChildren().addAll(fireIcon, titleBox);
        
        // SECTION CHALEUR GLOBALE
        Label globalLabel = new Label("📊 CHALEUR GLOBALE");
        globalLabel.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-weight: 700; " +
                            "-fx-padding: 0 0 5 0;");
        
        // Barre principale avec pourcentage
        HBox mainBarBox = new HBox(10);
        mainBarBox.setAlignment(Pos.CENTER_LEFT);
        
        String mainColor = avgScore >= 85 ? "#dc2626" : avgScore >= 70 ? "#ea580c" : "#16a34a";
        
        // Conteneur barre
        StackPane mainBarContainer = new StackPane();
        mainBarContainer.setMinHeight(20);
        mainBarContainer.setMaxHeight(20);
        mainBarContainer.setMaxWidth(Double.MAX_VALUE);
        mainBarContainer.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 10;");
        HBox.setHgrow(mainBarContainer, Priority.ALWAYS);
        
        // Barre remplie
        Region mainFill = new Region();
        mainFill.setMinHeight(20);
        mainFill.setMaxHeight(20);
        double mainWidth = Math.max(avgScore * 4, 50); // 4px par %, min 50px
        mainFill.setMinWidth(mainWidth);
        mainFill.setMaxWidth(mainWidth);
        mainFill.setStyle("-fx-background-color: " + mainColor + "; -fx-background-radius: 10;");
        StackPane.setAlignment(mainFill, Pos.CENTER_LEFT);
        
        mainBarContainer.getChildren().addAll(mainFill);
        
        // Pourcentage en gros
        Label mainPct = new Label(String.format("%.0f%%", avgScore));
        mainPct.setMinWidth(60);
        mainPct.setStyle("-fx-text-fill: " + mainColor + "; -fx-font-size: 20px; -fx-font-weight: 800;");
        
        mainBarBox.getChildren().addAll(mainBarContainer, mainPct);
        
        // SÉPARATEUR
        Region sep = new Region();
        sep.setMinHeight(2);
        sep.setMaxHeight(2);
        sep.setStyle("-fx-background-color: #e5e7eb;");
        sep.setPadding(new Insets(8, 0, 8, 0));
        
        // SECTION RÉPARTITION
        Label repartTitle = new Label("📈 RÉPARTITION PAR NIVEAU");
        repartTitle.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px; -fx-font-weight: 700; " +
                            "-fx-padding: 0 0 10 0;");
        
        // Barres individuelles
        VBox barsBox = new VBox(10);
        
        // Rouge - Ultra urgentes
        barsBox.getChildren().add(createStatRow("🔥 Ultra-urgentes (≥85%)", countUltra, pctUltra, "#dc2626"));
        // Orange - Haute priorité  
        barsBox.getChildren().add(createStatRow("⚡ Haute priorité (70-84%)", countHigh, pctHigh, "#ea580c"));
        // Vert - Opportunités
        barsBox.getChildren().add(createStatRow("✨ Opportunités (50-69%)", countMedium, pctMedium, "#16a34a"));
        
        // BADGES COMPTAGE en bas
        HBox badges = new HBox(15);
        badges.setAlignment(Pos.CENTER);
        badges.setPadding(new Insets(15, 0, 0, 0));
        
        badges.getChildren().addAll(
            createCountBadge("🔥", countUltra, "#dc2626"),
            createCountBadge("⚡", countHigh, "#ea580c"),
            createCountBadge("✨", countMedium, "#16a34a")
        );
        
        // SECTION EXPLICATIVE - Ce qu'est le Heat-Map
        VBox explanationBox = new VBox(8);
        explanationBox.setPadding(new Insets(0, 0, 10, 0));
        explanationBox.setStyle("-fx-background-color: #f3f4f6; -fx-background-radius: 12; -fx-padding: 15;");
        
        Label whatIsTitle = new Label("❓ Qu'est-ce que le Heat-Map ?");
        whatIsTitle.setStyle("-fx-text-fill: #1f2937; -fx-font-size: 13px; -fx-font-weight: 700;");
        
        Label whatIsText = new Label("Le Heat-Map analyse les offres et attribue un score de \"chaleur\" (0-100%) basé sur 3 critères :");
        whatIsText.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px; -fx-wrap-text: true;");
        whatIsText.setWrapText(true);
        
        Label criteria1 = new Label("🔥 Urgence : Offres qui ferment bientôt (moins de jours = plus urgent)");
        criteria1.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 10px;");
        
        Label criteria2 = new Label("💎 Rareté : Offres avec peu de postes disponibles (1-2 postes = très exclusif)");
        criteria2.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 10px;");
        
        Label criteria3 = new Label("✨ Fraîcheur : Offres récemment publiées (moins de 7 jours = nouvelle)");
        criteria3.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 10px;");
        
        explanationBox.getChildren().addAll(whatIsTitle, whatIsText, criteria1, criteria2, criteria3);
        
        // Assembler tout avec explication en premier
        panel.getChildren().addAll(
            header, 
            explanationBox,
            sep,
            globalLabel, mainBarBox,
            repartTitle, barsBox,
            badges
        );
        
        return panel;
    }
    
    /**
     * Crée une ligne de statistique avec barre
     */
    private HBox createStatRow(String label, int count, int percentage, String color) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        
        // Label
        Label lbl = new Label(label);
        lbl.setMinWidth(200);
        lbl.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 12px; -fx-font-weight: 600;");
        
        // Barre
        StackPane bar = new StackPane();
        bar.setMinHeight(12);
        bar.setMaxHeight(12);
        bar.setMinWidth(150);
        bar.setMaxWidth(150);
        bar.setStyle("-fx-background-color: #e5e7eb; -fx-background-radius: 6;");
        
        if (count > 0) {
            Region fill = new Region();
            fill.setMinHeight(12);
            fill.setMaxHeight(12);
            double width = Math.max(percentage * 1.5, count > 0 ? 20 : 0);
            fill.setMinWidth(width);
            fill.setMaxWidth(width);
            fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            bar.getChildren().add(fill);
        }
        
        // Compteur
        Label cnt = new Label(String.valueOf(count));
        cnt.setMinWidth(30);
        cnt.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-font-weight: 800;");
        
        row.getChildren().addAll(lbl, bar, cnt);
        return row;
    }
    
    /**
     * Crée un badge de comptage coloré
     */
    private Label createCountBadge(String emoji, int count, String color) {
        Label badge = new Label(emoji + " " + count);
        badge.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: 700; " +
                      "-fx-background-color: " + color + "; " +
                      "-fx-padding: 10 18; " +
                      "-fx-background-radius: 25; " +
                      "-fx-effect: dropshadow(gaussian, " + color + "66, 10, 0, 0, 4);");
        return badge;
    }

    private String resolveToUri(String path) {
        String p = path.trim();
        if (p.isEmpty()) return "";

        try {
            if (p.startsWith("file:") || p.startsWith("http://") || p.startsWith("https://")) {
                return p;
            }

            File f = new File(p);
            if (!f.isAbsolute()) {
                f = new File(System.getProperty("user.dir"), p);
            }
            if (f.exists()) {
                return f.toURI().toString();
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private Image loadImageOrNull(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        String uri = resolveToUri(path);
        if (uri == null || uri.trim().isEmpty()) return null;
        Image img = new Image(uri, false);
        return img.isError() ? null : img;
    }

    private HBox buildEntrepriseRow(OffreEmploi o) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        // Pas de logo pour l'instant - trop lent
        String nom = safe(o != null ? o.getNomEntreprise() : null, "Entreprise");
        Label nomLbl = new Label("🏢 " + nom);
        nomLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 700;");
        nomLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
        nomLbl.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(nomLbl, Priority.ALWAYS);

        row.getChildren().add(nomLbl);
        return row;
    }

    @FXML
    void initialize() {
        setupSecteurFilter();
        setupDateFilter();
        setupChartAxis();
        refresh(null);
    }

    private void setupChartAxis() {
        if (secteurChartYAxis == null) {
            return;
        }
        secteurChartYAxis.setAutoRanging(false);
        secteurChartYAxis.setLowerBound(0);
        secteurChartYAxis.setUpperBound(100);
        secteurChartYAxis.setTickUnit(20);
        secteurChartYAxis.setForceZeroInRange(true);
        secteurChartYAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                if (object == null) {
                    return "0%";
                }
                return String.format("%d%%", Math.round(object.doubleValue()));
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void setupSecteurFilter() {
        if (secteurFilter == null) {
            return;
        }

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("Tous");
        try {
            secteurService.getAll().forEach(s -> {
                if (s != null && s.getNom() != null && !s.getNom().trim().isEmpty()) {
                    items.add(s.getNom().trim());
                }
            });
        } catch (Exception ignored) {
        }

        secteurFilter.setItems(items);
        secteurFilter.setValue("Tous");
        secteurFilter.valueProperty().addListener((obs, o, n) -> scheduleApplyFilter());
    }

    private void setupDateFilter() {
        if (dateFilter == null) {
            return;
        }
        ObservableList<String> items = FXCollections.observableArrayList(
            "Toutes les dates",
            "📅 Plus récentes",
            "📅 Plus anciennes",
            "📅 Aujourd'hui",
            "📅 Cette semaine",
            "📅 Ce mois"
        );
        dateFilter.setItems(items);
        dateFilter.setValue("Toutes les dates");
        dateFilter.valueProperty().addListener((obs, o, n) -> scheduleApplyFilter());
    }

    @FXML
    void refresh(ActionEvent event) {
        // Réinitialisation UI (filtres, panels, sélection comparaison)
        try {
            selectedOffersForComparison.clear();
        } catch (Exception ignored) {
        }
        comparisonMode = false;
        updateSelectionCount();

        selectedLocationKey = null;
        lastSecteurForStats = null;
        currentSelectedOffer = null;

        if (secteurFilter != null) {
            secteurFilter.setValue("Tous");
        }

        // Revenir au panneau stats si on était sur les détails
        if (detailsPanel != null) {
            detailsPanel.setVisible(false);
            detailsPanel.setManaged(false);
        }
        if (statsPanel != null) {
            statsPanel.setVisible(true);
            statsPanel.setManaged(true);
        }

        if (offerDetailsContent != null) {
            offerDetailsContent.getChildren().clear();
        }

        if (positionChipsBox != null) {
            positionChipsBox.getChildren().clear();
        }

        if (locationListBox != null) {
            locationListBox.getChildren().clear();
        }

        showLoading(true);

        Task<List<OffreEmploi>> task = new Task<>() {
            @Override
            protected List<OffreEmploi> call() {
                // Version allégée pour meilleures performances
                return service.afficherOfrreEmploiLight();
            }
        };

        task.setOnSucceeded(e -> {
            List<OffreEmploi> res = task.getValue();
            master = FXCollections.observableArrayList(res != null ? res : Collections.emptyList());
            filtered = new FilteredList<>(master, o -> true);
            selectedLocationKey = null;
            lastSecteurForStats = null;
            applyFilter();
            // Forcer la re-génération des composants UI après reset
            updatePositionSelector();
            updateLocationMap();
            updateSecteurStatsChart();
            updateSelectionCount();
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

        Thread th = new Thread(task, "load-offres-front");
        th.setDaemon(true);
        th.start();
    }

    private void showLoading(boolean show) {
        if (offersFlowPane == null) {
            return;
        }
        if (loadingIndicator == null) {
            loadingIndicator = new ProgressIndicator();
            loadingIndicator.setMaxSize(70, 70);
        }
        if (show) {
            offersFlowPane.getChildren().setAll(loadingIndicator);
        } else {
            if (offersFlowPane.getChildren().size() == 1 && offersFlowPane.getChildren().get(0) == loadingIndicator) {
                offersFlowPane.getChildren().clear();
            }
        }
    }

    private void render(List<OffreEmploi> offres) {
        if (offersFlowPane == null) {
            return;
        }
        offersFlowPane.getChildren().clear();
        if (offres == null || offres.isEmpty()) {
            VBox empty = new VBox(10);
            empty.getStyleClass().addAll("wizard-card");
            empty.setPadding(new Insets(18));
            Label t = new Label("Aucune offre disponible pour le moment");
            t.getStyleClass().add("hero-title");
            Label s = new Label("Reviens plus tard ou actualise la page");
            s.getStyleClass().add("hero-subtitle");
            empty.getChildren().addAll(t, s);
            offersFlowPane.getChildren().add(empty);
            return;
        }

        // Stop any previous incremental rendering
        if (cardBuildTimeline != null) {
            cardBuildTimeline.stop();
            cardBuildTimeline = null;
        }

        // Prepare the list of offers in background (no JavaFX Node creation here)
        Task<java.util.List<OffreEmploi>> task = new Task<>() {
            @Override
            protected java.util.List<OffreEmploi> call() {
                java.util.List<OffreEmploi> base = new java.util.ArrayList<>();
                int count = 0;
                for (OffreEmploi o : offres) {
                    if (o != null) {
                        base.add(o);
                        count++;
                        // Limit to 10 cards MAXIMUM for front performance
                        if (count >= 10) {
                            break;
                        }
                    }
                }
                return base;
            }
        };

        task.setOnSucceeded(e -> {
            java.util.List<OffreEmploi> base = task.getValue();
            Platform.runLater(() -> renderCardsIncrementally(base));
        });

        task.setOnFailed(e -> Platform.runLater(() -> {
            offersFlowPane.getChildren().clear();
            Label err = new Label("Erreur lors du rendu des offres");
            err.getStyleClass().add("hero-subtitle");
            offersFlowPane.getChildren().add(err);
        }));

        Thread th = new Thread(task, "offer-list-prep-front");
        th.setDaemon(true);
        th.start();
    }

    private void renderCardsIncrementally(java.util.List<OffreEmploi> base) {
        if (offersFlowPane == null) {
            return;
        }

        offersFlowPane.getChildren().clear();
        if (base == null || base.isEmpty()) {
            return;
        }

        // Rendu direct sans animation pour meilleures performances
        for (OffreEmploi o : base) {
            offersFlowPane.getChildren().add(createCard(o));
        }
    }

    private void applyFilter() {
        if (filtered == null) {
            return;
        }

        String secteurSelected = secteurFilter != null ? secteurFilter.getValue() : null;
        String locationSelected = selectedLocationKey;
        String dateSelected = dateFilter != null ? dateFilter.getValue() : "Toutes les dates";

        boolean secteurChanged;
        if (secteurSelected == null) {
            secteurChanged = lastSecteurForStats != null;
        } else {
            secteurChanged = !secteurSelected.equals(lastSecteurForStats);
        }
        lastSecteurForStats = secteurSelected;

        // Filtre par date
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = null;
        java.time.LocalDate endDate = null;
        boolean sortRecentFirst = true;

        if (dateSelected != null) {
            switch (dateSelected) {
                case "📅 Aujourd'hui":
                    startDate = today;
                    endDate = today;
                    break;
                case "📅 Cette semaine":
                case "🏷️ Nouveau":
                    startDate = today.minusDays(7);
                    endDate = today;
                    break;
                case "📅 Ce mois":
                    startDate = today.withDayOfMonth(1);
                    endDate = today;
                    break;
                case "📅 Plus anciennes":
                    sortRecentFirst = false;
                    break;
                case "📅 Plus récentes":
                    sortRecentFirst = true;
                    break;
                case "Toutes les dates":
                default:
                    // Pas de filtrage par date
                    break;
            }
        }

        final java.time.LocalDate finalStartDate = startDate;
        final java.time.LocalDate finalEndDate = endDate;

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
                locationOk = locationSelected.equals(getLocationKey(o));
            }

            boolean dateOk;
            if (finalStartDate == null || finalEndDate == null) {
                dateOk = true;
            } else if (o == null || o.getDatePublication() == null) {
                dateOk = true; // Inclure si pas de date
            } else {
                dateOk = !o.getDatePublication().isBefore(finalStartDate) && 
                         !o.getDatePublication().isAfter(finalEndDate);
            }

            return secteurOk && locationOk && dateOk;
        });

        // Trier par date si nécessaire - créer une liste triée
        List<OffreEmploi> sortedList = new ArrayList<>(filtered);
        if (dateSelected != null && !dateSelected.equals("Toutes les dates")) {
            if (sortRecentFirst && 
                (dateSelected.contains("Plus récentes") || dateSelected.contains("Cette semaine") || 
                 dateSelected.contains("Aujourd'hui") || dateSelected.contains("Ce mois") ||
                 dateSelected.contains("Nouveau"))) {
                sortedList.sort((o1, o2) -> {
                    if (o1.getDatePublication() == null && o2.getDatePublication() == null) return 0;
                    if (o1.getDatePublication() == null) return 1;
                    if (o2.getDatePublication() == null) return -1;
                    return o2.getDatePublication().compareTo(o1.getDatePublication());
                });
            } else if (!sortRecentFirst && dateSelected.contains("Plus anciennes")) {
                sortedList.sort((o1, o2) -> {
                    if (o1.getDatePublication() == null && o2.getDatePublication() == null) return 0;
                    if (o1.getDatePublication() == null) return -1;
                    if (o2.getDatePublication() == null) return 1;
                    return o1.getDatePublication().compareTo(o2.getDatePublication());
                });
            }
        }

        render(sortedList);
        if (secteurChanged) {
            updatePositionSelector();
            updateLocationMap();
            updateSecteurStatsChart();
        }
    }

    private void scheduleApplyFilter() {
        if (filterDebounceTimeline != null) {
            filterDebounceTimeline.stop();
            filterDebounceTimeline = null;
        }
        filterDebounceTimeline = new Timeline(new KeyFrame(Duration.millis(120), e -> applyFilter()));
        filterDebounceTimeline.setCycleCount(1);
        filterDebounceTimeline.playFromStart();
    }

    private void updateSecteurStatsChart() {
        if (secteurAreaChart == null) {
            return;
        }

        // Réactiver le chart
        secteurAreaChart.setVisible(true);
        secteurAreaChart.setManaged(true);

        List<OffreEmploi> base = (filtered != null) ? new ArrayList<>(filtered) : (master != null ? master : Collections.emptyList());

        Map<String, Long> bySecteur = base.stream()
                .collect(Collectors.groupingBy(o -> {
                    if (o == null || o.getSecteur() == null || o.getSecteur().getNom() == null) {
                        return "Non défini";
                    }
                    String s = o.getSecteur().getNom().trim();
                    return s.isEmpty() ? "Non défini" : s;
                }, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(8)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (x, y) -> x,
                        LinkedHashMap::new
                ));

        long total = bySecteur.values().stream().mapToLong(Long::longValue).sum();

        secteurAreaChart.getData().clear();
        if (bySecteur.isEmpty()) {
            return;
        }

        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Map.Entry<String, Long> e : bySecteur.entrySet()) {
            double pct = total <= 0 ? 0 : (e.getValue() * 100.0 / total);
            s.getData().add(new XYChart.Data<>(e.getKey(), pct));
        }

        secteurAreaChart.getData().add(s);
    }

    private void updatePositionSelector() {
        if (positionChipsBox == null || master == null) {
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
            scheduleApplyFilter();
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
                scheduleApplyFilter();
            });
            positionChipsBox.getChildren().add(chip);
        }
    }

    private void updateLocationMap() {
        if (locationListBox == null || master == null) {
            return;
        }

        Map<String, Long> counts = getCountsByLocation(getOffersForLocationUi());
        if (counts.isEmpty()) {
            locationListBox.getChildren().clear();
            return;
        }

        long max = counts.values().stream().mapToLong(Long::longValue).max().orElse(1);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        locationListBox.getChildren().clear();

        VBox allRow = createLocationRow("Toutes les positions", total, max, null);
        if (selectedLocationKey == null) {
            allRow.getStyleClass().add("selected");
        }
        allRow.setOnMouseClicked(e -> {
            selectedLocationKey = null;
            scheduleApplyFilter();
        });
        locationListBox.getChildren().add(allRow);

        for (Map.Entry<String, Long> e : counts.entrySet()) {
            String key = e.getKey();
            long value = e.getValue();
            VBox row = createLocationRow(key, value, max, key);
            if (key != null && key.equals(selectedLocationKey)) {
                row.getStyleClass().add("selected");
            }
            row.setOnMouseClicked(ev -> {
                if (key != null && key.equals(selectedLocationKey)) {
                    selectedLocationKey = null;
                } else {
                    selectedLocationKey = key;
                }
                scheduleApplyFilter();
            });
            locationListBox.getChildren().add(row);
        }
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

    private List<OffreEmploi> getOffersForLocationUi() {
        if (master == null) {
            return Collections.emptyList();
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

    private VBox createCard(OffreEmploi o) {
        // Carte moderne avec style pro - TAILLE FIXE
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setPrefWidth(320);
        card.setMinWidth(320);
        card.setMaxWidth(320);
        card.setPrefHeight(280);
        card.setMinHeight(280);
        card.setMaxHeight(280);
        card.setStyle("-fx-background-color: #ffffff; " +
                     "-fx-background-radius: 16; " +
                     "-fx-border-color: #e5e7eb; " +
                     "-fx-border-width: 1; " +
                     "-fx-border-radius: 16; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        // === LIGNE 1: Checkbox + Entreprise + Favoris ===
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        
        // Checkbox pour comparaison
        javafx.scene.control.CheckBox compareCheck = new javafx.scene.control.CheckBox();
        compareCheck.setSelected(selectedOffersForComparison.contains(o));
        compareCheck.setStyle("-fx-padding: 2;");
        compareCheck.setOnAction(e -> {
            if (compareCheck.isSelected()) {
                if (selectedOffersForComparison.size() >= 3) {
                    compareCheck.setSelected(false);
                    showAlert(Alert.AlertType.WARNING, "Limite atteinte", 
                        "Vous ne pouvez sélectionner que 3 offres maximum pour la comparaison.");
                    return;
                }
                selectedOffersForComparison.add(o);
            } else {
                selectedOffersForComparison.remove(o);
            }
            updateSelectionCount();
        });
        
        // Nom entreprise
        String companyName = safe(o != null ? o.getNomEntreprise() : null, "Entreprise");
        Label companyLabel = new Label("🏢 " + companyName);
        companyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px; -fx-font-weight: 600;");
        
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        
        // Badge NOUVEAU
        Label badgeNouveau = null;
        if (o != null && o.isNouveau()) {
            badgeNouveau = new Label("✨ NOUVEAU");
            badgeNouveau.setStyle("-fx-background-color: #8b5cf6; " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 3 8; " +
                    "-fx-background-radius: 10; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 9px;");
        }
        
        // Bouton Favoris - COEUR qui fonctionne
        javafx.scene.control.ToggleButton favorisBtn = new javafx.scene.control.ToggleButton("♥");
        favorisBtn.setSelected(false);
        favorisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #d1d5db; " +
                "-fx-font-size: 20px; -fx-padding: 0 2; -fx-cursor: hand;");
        
        // Vérifier si déjà favoris et mettre à jour le style
        FavorisOffreService favorisService = new FavorisOffreService();
        if (Session.getCurrentUser() != null && o != null && o.getId() > 0) {
            boolean estFavoris = favorisService.estFavoris(Session.getCurrentUser().getId(), o.getId());
            if (estFavoris) {
                favorisBtn.setSelected(true);
                favorisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                        "-fx-font-size: 20px; -fx-padding: 0 2;");
            }
        }
        
        // Action du bouton favoris - FONCTIONNEL
        favorisBtn.setOnAction(e -> {
            if (Session.getCurrentUser() == null) {
                showAlert(Alert.AlertType.WARNING, "Connexion requise", 
                    "Veuillez vous connecter pour ajouter des favoris.");
                favorisBtn.setSelected(false);
                return;
            }
            
            Integer userId = Session.getCurrentUser().getId();
            Integer offreId = o.getId();
            
            if (favorisBtn.isSelected()) {
                // Ajouter aux favoris
                boolean success = favorisService.ajouterAuxFavoris(userId, offreId);
                if (success) {
                    favorisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                            "-fx-font-size: 20px; -fx-padding: 0 2;");
                } else {
                    favorisBtn.setSelected(false);
                }
            } else {
                // Retirer des favoris
                boolean success = favorisService.retirerDesFavoris(userId, offreId);
                if (success) {
                    favorisBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #d1d5db; " +
                            "-fx-font-size: 20px; -fx-padding: 0 2;");
                } else {
                    favorisBtn.setSelected(true);
                }
            }
        });
        
        topRow.getChildren().addAll(compareCheck, companyLabel, spacer1);
        if (badgeNouveau != null) {
            topRow.getChildren().add(badgeNouveau);
        }
        topRow.getChildren().add(favorisBtn);
        
        // === TITRE + HEAT-MAP ===
        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(safe(o != null ? o.getTitre() : null, "Offre sans titre"));
        title.setStyle("-fx-text-fill: #111827; -fx-font-size: 15px; -fx-font-weight: 700;");
        title.setWrapText(true);
        title.setMaxWidth(240);
        
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);
        
        // Badge Heat-Map
        Label badgeHeat = null;
        if (o != null) {
            JobHeatMapService heatService = new JobHeatMapService();
            HeatScore heat = heatService.calculerHeatScore(o);
            if (heat != null && heat.getScoreGlobal() >= 30) {
                String emoji = heat.getScoreGlobal() >= 85 ? "🔥" : heat.getScoreGlobal() >= 70 ? "⚡" : "✨";
                String bgColor = heat.getScoreGlobal() >= 85 ? "#dc2626" : heat.getScoreGlobal() >= 70 ? "#ea580c" : "#16a34a";
                
                badgeHeat = new Label(emoji + " " + heat.getScoreGlobal() + "%");
                badgeHeat.setStyle("-fx-background-color: " + bgColor + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 4 8; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 11px;");
            }
        }
        
        titleRow.getChildren().addAll(title, spacer2);
        if (badgeHeat != null) {
            titleRow.getChildren().add(badgeHeat);
        }
        
        // === DÉTAILS ===
        VBox detailsBox = new VBox(5);
        detailsBox.setPadding(new Insets(4, 0, 4, 0));
        
        String locText;
        if (o == null || o.getLocalisation() == null) {
            locText = "Non spécifié";
        } else {
            String v = o.getLocalisation().getVille();
            String p = o.getLocalisation().getPays();
            String vv = v != null ? v.trim() : "";
            String pp = p != null ? p.trim() : "";
            if (vv.isEmpty() && pp.isEmpty()) locText = "Non spécifié";
            else if (vv.isEmpty()) locText = pp;
            else if (pp.isEmpty()) locText = vv;
            else locText = vv + ", " + pp;
        }
        
        Label locationLabel = new Label("📍 " + locText + "   💼 " + 
            safe(o != null && o.getTypeContrat() != null ? o.getTypeContrat().getNom() : null, "Contrat"));
        locationLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px;");
        
        String dateStr = "🗓️ Date limite: ";
        String dateColor = "#4b5563";
        if (o != null && o.getDateLimiteCandidature() != null) {
            dateStr += o.getDateLimiteCandidature().toString();
            long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(
                java.time.LocalDate.now(), o.getDateLimiteCandidature());
            if (joursRestants <= 3 && joursRestants >= 0) {
                dateStr += " (⚠️ " + joursRestants + "j)";
                dateColor = "#dc2626";
            } else if (joursRestants <= 7 && joursRestants >= 0) {
                dateStr += " (📅 " + joursRestants + "j)";
                dateColor = "#ea580c";
            }
        } else {
            dateStr += "Non spécifiée";
        }
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: " + dateColor + "; -fx-font-size: 11px;");
        
        HBox postesRow = new HBox(15);
        postesRow.setAlignment(Pos.CENTER_LEFT);
        int nbPostes = o != null && o.getNombrePoste() != null ? o.getNombrePoste() : 1;
        Label postesLabel = new Label("👥 " + nbPostes + " poste" + (nbPostes > 1 ? "s" : ""));
        postesLabel.setStyle("-fx-text-fill: #4b5563; -fx-font-size: 11px;");
        postesRow.getChildren().add(postesLabel);
        
        if (o != null && o.getTeletravail() != null && o.getTeletravail()) {
            Label remoteLabel = new Label("🏠 Télétravail");
            remoteLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; " +
                    "-fx-padding: 2 6; -fx-background-radius: 6; -fx-font-size: 10px;");
            postesRow.getChildren().add(remoteLabel);
        }
        
        detailsBox.getChildren().addAll(locationLabel, dateLabel, postesRow);
        
        // === SÉPARATEUR ===
        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color: #e5e7eb;");
        VBox.setVgrow(sep, Priority.ALWAYS);
        
        // === BOUTONS ===
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        
        Button detailsBtn = new Button("Détails");
        detailsBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; " +
                "-fx-font-weight: 600; -fx-padding: 8 16; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12px;");
        detailsBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(detailsBtn, Priority.ALWAYS);
        detailsBtn.setOnAction(e -> showOfferDetailsPanel(o));
        
        Button postulerBtn = new Button("Postuler");
        postulerBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                "-fx-font-weight: 600; -fx-padding: 8 16; " +
                "-fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 12px;");
        postulerBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(postulerBtn, Priority.ALWAYS);
        postulerBtn.setOnAction(e -> postuler(o));
        
        actions.getChildren().addAll(detailsBtn, postulerBtn);
        
        // Assembler
        card.getChildren().addAll(topRow, titleRow, detailsBox, sep, actions);
        
        return card;
    }
    
    /**
     * Crée un pill (badge arrondi) avec couleur personnalisée
     */
    private Label createPill(String text, String bgColor, String textColor) {
        Label pill = new Label(text);
        pill.setStyle("-fx-background-color: " + bgColor + "; " +
                     "-fx-text-fill: " + textColor + "; " +
                     "-fx-padding: 4 10; " +
                     "-fx-background-radius: 12; " +
                     "-fx-font-size: 11px; " +
                     "-fx-font-weight: 600;");
        return pill;
    }



    //jdiddd candidatureee

    private void postuler(OffreEmploi o) {
        try {
            Parent root = offersFlowPane.getScene().getRoot();

            // AVANT - cherchait adminContentArea, contentArea, pageContainer
            // APRÈS - cherche directement pageContainer
            Pane contentArea = (Pane) root.lookup("#pageContainer");
            if (contentArea == null) contentArea = (Pane) root.lookup("#contentArea");
            if (contentArea == null) throw new RuntimeException("Conteneur introuvable");

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/candidaturefxml/Ajouter.fxml")
            );
            Parent view = loader.load();

            AjouterCandidature ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setOffre(o);
            }

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire : " + e.getMessage());
        }
    }


    private String safe(String s, String fallback) {
        if (s == null) {
            return fallback;
        }
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    /**
     * Affiche uniquement les offres récentes (moins de 7 jours)
     */
    @FXML
    void afficherOffresRecentes(ActionEvent event) {
        showLoading(true);

        Task<List<OffreEmploi>> task = new Task<>() {
            @Override
            protected List<OffreEmploi> call() throws Exception {
                return service.getOffresRecentes();
            }
        };

        task.setOnSucceeded(e -> {
            List<OffreEmploi> recentes = task.getValue();
            if (recentes == null || recentes.isEmpty()) {
                // Aucune offre récente
                offersFlowPane.getChildren().clear();
                VBox empty = new VBox(10);
                empty.getStyleClass().addAll("wizard-card");
                empty.setPadding(new Insets(18));
                Label t = new Label("🏷️ Aucune offre récente");
                t.getStyleClass().add("hero-title");
                Label s = new Label("Il n'y a pas d'offres publiées dans les 7 derniers jours.");
                s.getStyleClass().add("hero-subtitle");
                empty.getChildren().addAll(t, s);
                offersFlowPane.getChildren().add(empty);
            } else {
                // Afficher les offres récentes
                offersFlowPane.getChildren().clear();
                for (OffreEmploi o : recentes) {
                    offersFlowPane.getChildren().add(createCard(o));
                }
            }
            showLoading(false);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors du chargement des offres récentes");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "load-offres-recentes");
        th.setDaemon(true);
        th.start();
    }

    /**
     * Affiche les offres favorites de l'utilisateur connecté
     */
    @FXML
    void afficherMesFavoris(ActionEvent event) {
        if (Session.getCurrentUser() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Connexion requise");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez vous connecter pour voir vos favoris.");
            alert.showAndWait();
            return;
        }

        showLoading(true);
        
        Task<List<OffreEmploi>> task = new Task<>() {
            @Override
            protected List<OffreEmploi> call() throws Exception {
                // Charger les IDs des favoris
                FavorisOffreService favorisService = new FavorisOffreService();
                List<Integer> favorisIds = favorisService.getFavorisByUser(Session.getCurrentUser().getId())
                    .stream()
                    .map(f -> f.getOffreId())
                    .collect(Collectors.toList());
                
                // Charger les offres complètes
                List<OffreEmploi> allOffers = service.afficherOfrreEmploiLight();
                return allOffers.stream()
                    .filter(o -> favorisIds.contains(o.getId()))
                    .collect(Collectors.toList());
            }
        };
        
        task.setOnSucceeded(e -> {
            List<OffreEmploi> favoris = task.getValue();
            if (favoris == null || favoris.isEmpty()) {
                // Aucun favori
                offersFlowPane.getChildren().clear();
                VBox empty = new VBox(10);
                empty.getStyleClass().addAll("wizard-card");
                empty.setPadding(new Insets(18));
                Label t = new Label("❤️ Vous n'avez pas encore de favoris");
                t.getStyleClass().add("hero-title");
                Label s = new Label("Cliquez sur le cœur ♥ sur une offre pour l'ajouter à vos favoris");
                s.getStyleClass().add("hero-subtitle");
                empty.getChildren().addAll(t, s);
                offersFlowPane.getChildren().add(empty);
            } else {
                // Afficher les favoris
                offersFlowPane.getChildren().clear();
                for (OffreEmploi o : favoris) {
                    offersFlowPane.getChildren().add(createCard(o));
                }
            }
            showLoading(false);
        });
        
        task.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de charger vos favoris: " + (ex != null ? ex.getMessage() : "erreur inconnue"));
            alert.showAndWait();
        });
        
        Thread th = new Thread(task, "load-favoris");
        th.setDaemon(true);
        th.start();
    }

    /**
     * Affiche les offres recommandées (les plus compatibles avec le profil du candidat)
     * TEMPORAIREMENT DESACTIVE POUR TESTS
     */
    @FXML
    void afficherOffresRecommandees(ActionEvent event) {
        if (Session.getCurrentUser() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Connexion requise");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez vous connecter pour voir les offres recommandées.");
            alert.showAndWait();
            return;
        }

        showLoading(true);

        Task<List<OffreCompatibilite>> task = new Task<>() {
            @Override
            protected List<OffreCompatibilite> call() throws Exception {
                String titre = Session.getCurrentUser().getTitre();
                System.out.println("[DEBUG] Titre utilisateur: " + titre);
                
                if (titre != null && !titre.trim().isEmpty()) {
                    System.out.println("[DEBUG] Recherche par titre: " + titre.trim());
                    return service.getOffresRecommandeesParTitre(titre.trim());
                } else {
                    System.out.println("[DEBUG] Recherche par user ID: " + Session.getCurrentUser().getId());
                    return service.getOffresCompatibles(Session.getCurrentUser().getId());
                }
            }
        };

        task.setOnSucceeded(e -> {
            List<OffreCompatibilite> compatibles = task.getValue();
            System.out.println("[DEBUG] Offres recommandées trouvées: " + (compatibles != null ? compatibles.size() : 0));
            renderOffresCompatibles(compatibles);
            showLoading(false);
        });

        task.setOnFailed(e -> {
            showLoading(false);
            Throwable ex = task.getException();
            System.err.println("[ERROR] Erreur recommandation: " + (ex != null ? ex.getMessage() : "inconnue"));
            if (ex != null) {
                ex.printStackTrace();
            }
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors du chargement des offres recommandées");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "load-offres-recommandees");
        th.setDaemon(true);
        th.start();
    }

    /**
     * Affiche les offres avec leur score de compatibilité
     */
    private void renderOffresCompatibles(List<OffreCompatibilite> offres) {
        if (offersFlowPane == null) {
            return;
        }

        // Stop any previous incremental rendering
        if (cardBuildTimeline != null) {
            cardBuildTimeline.stop();
            cardBuildTimeline = null;
        }

        offersFlowPane.getChildren().clear();

        if (offres == null || offres.isEmpty()) {
            VBox empty = new VBox(10);
            empty.getStyleClass().addAll("wizard-card");
            empty.setPadding(new Insets(18));
            Label t = new Label("Aucune offre recommandée pour le moment");
            t.getStyleClass().add("hero-title");
            Label s = new Label("Complétez votre profil en postulant à une offre pour obtenir des recommandations personnalisées.");
            s.getStyleClass().add("hero-subtitle");
            empty.getChildren().addAll(t, s);
            offersFlowPane.getChildren().add(empty);
            return;
        }

        // Limiter à 10 MAXIMUM pour les performances
        List<OffreCompatibilite> limited = offres.size() > 10 ? offres.subList(0, 10) : offres;

        // Rendu direct sans Timeline - beaucoup plus rapide
        for (OffreCompatibilite oc : limited) {
            offersFlowPane.getChildren().add(createCompatibleCard(oc));
        }
    }

    /**
     * Crée une carte d'offre avec indication de compatibilité
     */
    private VBox createCompatibleCard(OffreCompatibilite oc) {
        OffreEmploi o = oc.getOffre();

        VBox card = new VBox(12);
        card.getStyleClass().addAll("wizard-card", "offer-front-card");
        card.setPadding(new Insets(16));

        HBox entrepriseRow = buildEntrepriseRow(o);

        // Score badge (coloré selon le score)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label scoreBadge = new Label(oc.getScore() + "% - " + oc.getScoreLabel());
        scoreBadge.setStyle("-fx-background-color: " + oc.getScoreColor() + "; " +
                "-fx-text-fill: white; " +
                "-fx-padding: 6 14; " +
                "-fx-background-radius: 20; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 12px;");

        header.getChildren().add(scoreBadge);

        // Badge "NOUVEAU" pour les offres récentes (moins de 7 jours)
        if (o.isNouveau()) {
            Label badgeNouveau = new Label("🏷️ NOUVEAU");
            badgeNouveau.setStyle("-fx-background-color: linear-gradient(135deg, #ff4757 0%, #ff6b81 100%); " +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 4 12; " +
                    "-fx-background-radius: 12; " +
                    "-fx-font-weight: bold; " +
                    "-fx-font-size: 11px;");
            header.getChildren().add(badgeNouveau);
        }

        // Titre
        Label title = new Label(safe(o.getTitre(), "Offre sans titre"));
        title.getStyleClass().add("offer-front-title");
        title.setWrapText(true);

        // Meta info simplifiée
        VBox meta = new VBox(4);

        String locText;
        if (o.getLocalisation() == null) {
            locText = "Localisation";
        } else {
            String v = o.getLocalisation().getVille();
            String p = o.getLocalisation().getPays();
            String vv = v != null ? v.trim() : "";
            String pp = p != null ? p.trim() : "";
            if (vv.isEmpty() && pp.isEmpty()) {
                locText = "Localisation";
            } else if (vv.isEmpty()) {
                locText = pp;
            } else if (pp.isEmpty()) {
                locText = vv;
            } else {
                locText = vv + ", " + pp;
            }
        }

        Label loc = new Label("📍 " + locText + " | " + 
            safe(o.getTypeContrat() != null ? o.getTypeContrat().getNom() : null, "Contrat"));
        loc.getStyleClass().add("offer-front-meta");

        meta.getChildren().add(loc);

        // Actions
        Button details = new Button("Détails");
        details.getStyleClass().addAll("btn-secondary", "offer-front-btn");
        details.setMaxWidth(Double.MAX_VALUE);
        details.setOnAction(e -> {
            showOfferDetailsPanel(o);
            e.consume();
        });

        Button apply = new Button("Postuler");
        apply.getStyleClass().addAll("btn-primary", "offer-front-btn");
        apply.setMaxWidth(Double.MAX_VALUE);
        apply.setOnAction(e -> postuler(o));

        HBox actions = new HBox(10, details, apply);
        HBox.setHgrow(details, Priority.ALWAYS);
        HBox.setHgrow(apply, Priority.ALWAYS);

        // Carte simplifiée sans description et matchingBox
        card.getChildren().addAll(header, entrepriseRow, title, meta, actions);
        return card;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen - 3) + "...";
    }

    /**
     * Affiche les détails de l'offre dans le panneau de droite
     */
    private void showOfferDetailsPanel(OffreEmploi o) {
        if (o == null || offerDetailsContent == null) {
            return;
        }
        currentSelectedOffer = o;

        // Incrémenter le nombre de vues pour cette offre
        if (service != null && o.getId() > 0) {
            service.incrementerVues(o.getId());
        }

        // Hide stats panel, show details panel
        if (statsPanel != null) {
            statsPanel.setVisible(false);
            statsPanel.setManaged(false);
        }
        if (detailsPanel != null) {
            detailsPanel.setVisible(true);
            detailsPanel.setManaged(true);
        }

        // Clear and build new content
        offerDetailsContent.getChildren().clear();

        // Header with title and company - Style moderne avec logo
        HBox headerWithLogo = new HBox(12);
        headerWithLogo.setAlignment(Pos.CENTER_LEFT);

        // Logo entreprise
        ImageView logoView = new ImageView();
        logoView.setFitWidth(56);
        logoView.setFitHeight(56);
        logoView.setPreserveRatio(true);
        logoView.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 12;");

        String logoPath = o.getLogoEntreprise();
        if (logoPath != null && !logoPath.trim().isEmpty()) {
            Image logo = loadImageOrNull(logoPath);
            if (logo != null) {
                logoView.setImage(logo);
                logoView.setStyle("-fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
            } else {
                // Placeholder avec initiales
                logoView.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-background-radius: 12;");
            }
        } else {
            // Placeholder avec initiales
            logoView.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-background-radius: 12;");
        }

        VBox headerText = new VBox(6);
        headerText.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(safe(o.getTitre(), "Sans titre"));
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        titleLabel.setWrapText(true);

        Label companyLabel = new Label(safe(o.getNomEntreprise(), "Entreprise"));
        companyLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #6366f1;");

        headerText.getChildren().addAll(titleLabel, companyLabel);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        headerWithLogo.getChildren().addAll(logoView, headerText);

        // Status badge - Style moderne avec effet glassmorphism
        Label statusBadge = new Label(safe(o.getStatus(), "N/A").toUpperCase());
        if ("active".equalsIgnoreCase(o.getStatus())) {
            statusBadge.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #34d399); -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 24; -fx-font-weight: 800; -fx-font-size: 11px; -fx-effect: dropshadow(gaussian, rgba(16, 185, 129, 0.3), 8, 0, 0, 2);");
        } else {
            statusBadge.setStyle("-fx-background-color: linear-gradient(to right, #ef4444, #f87171); -fx-text-fill: white; -fx-padding: 8 18; -fx-background-radius: 24; -fx-font-weight: 800; -fx-font-size: 11px; -fx-effect: dropshadow(gaussian, rgba(239, 68, 68, 0.3), 8, 0, 0, 2);");
        }

        HBox statusBox = new HBox(10, statusBadge);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        // Description section - Style moderne
        VBox descSection = new VBox(10);
        descSection.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #f1f5f9); -fx-background-radius: 16; -fx-padding: 18;");

        Label descTitle = new Label("📝 Description");
        descTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #3b82f6;");

        Label descContent = new Label(safe(o.getDescription(), "Aucune description disponible"));
        descContent.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569; -fx-line-spacing: 2;");
        descContent.setWrapText(true);

        descSection.getChildren().addAll(descTitle, descContent);

        // Offres similaires (compact) - affichage horizontal sous la description
        List<OffreEmploi> similaires = findSimilarOffers(o, 6);
        if (!similaires.isEmpty()) {
            Label similarTitle = new Label("🎯 Vous aimerez aussi...");
            similarTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");

            HBox similarCardsBox = new HBox(10);
            similarCardsBox.setPadding(new Insets(2, 0, 0, 0));

            for (OffreEmploi similar : similaires) {
                int score = calculateSimilarityScore(o, similar);
                VBox c = createSimilarOfferCard(similar, score);
                c.setMinWidth(290);
                c.setPrefWidth(290);
                c.setMaxWidth(290);
                similarCardsBox.getChildren().add(c);
            }

            ScrollPane similarScroll = new ScrollPane(similarCardsBox);
            similarScroll.setFitToHeight(true);
            similarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            similarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            similarScroll.setPannable(true);
            similarScroll.setPrefViewportHeight(160);
            similarScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

            descSection.getChildren().addAll(similarTitle, similarScroll);
        }

        // Barre de traduction (FR/EN/AR)
        HBox translateBar = new HBox(10);
        translateBar.setAlignment(Pos.CENTER_LEFT);
        translateBar.setStyle("-fx-background-color: #eff6ff; -fx-padding: 10 12; -fx-background-radius: 12; -fx-border-color: #2563eb; -fx-border-radius: 12; -fx-border-width: 1;");

        Label translateLabel = new Label("🌐 Traduire:");
        translateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #1e40af; -fx-font-weight: 800;");

        Button btnFr = new Button("FR");
        Button btnEn = new Button("EN");
        Button btnAr = new Button("AR");

        String btnStyleOn = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10;";
        String btnStyleOff = "-fx-background-color: white; -fx-text-fill: #2563eb; -fx-font-weight: 800; -fx-padding: 6 12; -fx-background-radius: 10; -fx-border-color: #2563eb; -fx-border-radius: 10; -fx-border-width: 1;";

        // Langue affichée actuellement (simple)
        final String[] langShown = {"fr"};

        btnFr.setStyle(btnStyleOn);
        btnEn.setStyle(btnStyleOff);
        btnAr.setStyle(btnStyleOff);

        Runnable setActiveFr = () -> {
            btnFr.setStyle(btnStyleOn);
            btnEn.setStyle(btnStyleOff);
            btnAr.setStyle(btnStyleOff);
        };
        Runnable setActiveEn = () -> {
            btnFr.setStyle(btnStyleOff);
            btnEn.setStyle(btnStyleOn);
            btnAr.setStyle(btnStyleOff);
        };
        Runnable setActiveAr = () -> {
            btnFr.setStyle(btnStyleOff);
            btnEn.setStyle(btnStyleOff);
            btnAr.setStyle(btnStyleOn);
        };

        TranslationService translationService = new TranslationService();

        java.util.function.Consumer<String> doTranslate = targetLang -> {
            // Pas de traduction si déjà dans la langue cible
            if (targetLang != null && targetLang.equalsIgnoreCase(langShown[0])) {
                return;
            }

            Task<TranslationService.OffreTraduite> t = new Task<>() {
                @Override
                protected TranslationService.OffreTraduite call() {
                    String src = translationService.detecterLangue(safe(o.getTitre(), "") + " " + safe(o.getDescription(), ""));
                    return translationService.traduireOffre(
                            o.getTitre(),
                            o.getDescription(),
                            src,
                            targetLang,
                            "tunisia"
                    );
                }
            };

            t.setOnSucceeded(ev -> {
                TranslationService.OffreTraduite res = t.getValue();
                if (res != null && res.isSucces()) {
                    langShown[0] = targetLang;
                    titleLabel.setText(res.getTitre());
                    descContent.setText(res.getDescription());
                } else {
                    showAlert(Alert.AlertType.WARNING, "Traduction", "Traduction indisponible");
                }
            });

            t.setOnFailed(ev -> showAlert(Alert.AlertType.ERROR, "Traduction", "Erreur traduction"));

            Thread th = new Thread(t, "translate-offre");
            th.setDaemon(true);
            th.start();
        };

        btnFr.setOnAction(e -> {
            setActiveFr.run();
            doTranslate.accept("fr");
        });
        btnEn.setOnAction(e -> {
            setActiveEn.run();
            doTranslate.accept("en");
        });
        btnAr.setOnAction(e -> {
            setActiveAr.run();
            doTranslate.accept("ar");
        });

        Region translateSpacer = new Region();
        HBox.setHgrow(translateSpacer, Priority.ALWAYS);
        translateBar.getChildren().addAll(translateLabel, translateSpacer, btnFr, btnEn, btnAr);

        // Info grid - Style moderne
        VBox infoSection = new VBox(10);
        infoSection.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16; -fx-padding: 18; -fx-spacing: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label infoTitle = new Label("📊 Informations clés");
        infoTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #3b82f6;");
        infoSection.getChildren().add(infoTitle);

        infoSection.getChildren().addAll(
            createModernInfoRow("📍", "Localisation", getLocationText(o)),
            createModernInfoRow("📋", "Contrat", safe(o.getTypeContrat() != null ? o.getTypeContrat().getNom() : null, "Non spécifié")),
            createModernInfoRow("🏢", "Secteur", safe(o.getSecteur() != null ? o.getSecteur().getNom() : null, "Non spécifié")),
            createModernInfoRow("⭐", "Expérience", safe(o.getNiveauExperience(), "Non spécifié")),
            createModernInfoRow("🎓", "Études", safe(o.getNiveauEtudes(), "Non spécifié")),
            createModernInfoRow("🌐", "Langues", safe(o.getLanguesRequises(), "Non spécifiées")),
            createModernInfoRow("👥", "Postes", o.getNombrePoste() != null ? o.getNombrePoste() + " poste(s)" : "Non spécifié"),
            createModernInfoRow("📅", "Date limite", o.getDateLimiteCandidature() != null ? o.getDateLimiteCandidature().toString() : "Non spécifiée"),
            createModernInfoRow("🏠", "Télétravail", Boolean.TRUE.equals(o.getTeletravail()) ? "Oui ✓" : "Non ✗")
        );

        // Boutons d'export - PDF, QR Code et Virtual Tour 3D
        HBox exportButtons = new HBox(10);
        exportButtons.setAlignment(Pos.CENTER);
        
        Button pdfBtn = new Button("📄 Exporter PDF");
        pdfBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 12 20; -fx-background-radius: 10;");
        pdfBtn.setCursor(javafx.scene.Cursor.HAND);
        pdfBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(pdfBtn, Priority.ALWAYS);
        pdfBtn.setOnAction(e -> exportOfferToPdf(o));
        
        Button qrBtn = new Button("📱 QR Code");
        qrBtn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13px; -fx-padding: 12 20; -fx-background-radius: 10;");
        qrBtn.setCursor(javafx.scene.Cursor.HAND);
        qrBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(qrBtn, Priority.ALWAYS);
        qrBtn.setOnAction(e -> showQrCodeForScanning(o));
        
        exportButtons.getChildren().addAll(pdfBtn, qrBtn);

        // Postuler button - Style moderne avec gradient
        Button postulerBtn = new Button("💼 Postuler à cette offre");
        postulerBtn.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 14px; -fx-padding: 16 28; -fx-background-radius: 14; -fx-effect: dropshadow(gaussian, rgba(102, 126, 234, 0.4), 12, 0, 0, 4);");
        postulerBtn.setCursor(javafx.scene.Cursor.HAND);
        postulerBtn.setMaxWidth(Double.MAX_VALUE);
        postulerBtn.setOnAction(e -> postuler(o));

        // Add all to content
        offerDetailsContent.getChildren().addAll(
            statusBox,
            headerWithLogo,
            descSection,
            translateBar,
            infoSection,
            exportButtons,
            postulerBtn
        );
    }

    private VBox createDetailSection(String title, String content) {
        VBox section = new VBox(8);
        section.getStyleClass().add("offer-detail-section");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("offer-detail-section-title");

        if (!content.isEmpty()) {
            Label contentLabel = new Label(content);
            contentLabel.getStyleClass().add("offer-detail-section-content");
            contentLabel.setWrapText(true);
            section.getChildren().addAll(titleLabel, contentLabel);
        } else {
            section.getChildren().add(titleLabel);
        }

        return section;
    }

    private HBox createModernInfoRow(String icon, String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");

        VBox textBox = new VBox(2);
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-weight: 500;");
        valueLabel.setWrapText(true);
        textBox.getChildren().addAll(labelLabel, valueLabel);

        HBox.setHgrow(textBox, Priority.ALWAYS);
        row.getChildren().addAll(iconLabel, textBox);
        return row;
    }

    private HBox createInfoRow(String icon, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("offer-detail-info-value");
        valueLabel.setWrapText(true);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);

        row.getChildren().addAll(iconLabel, valueLabel);
        return row;
    }

    private String getLocationText(OffreEmploi o) {
        if (o == null || o.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String v = o.getLocalisation().getVille();
        String p = o.getLocalisation().getPays();
        String a = o.getLocalisation().getAdresse();
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.trim().isEmpty()) sb.append(a).append(", ");
        if (v != null && !v.trim().isEmpty()) sb.append(v).append(", ");
        if (p != null && !p.trim().isEmpty()) sb.append(p);
        String result = sb.toString().trim();
        return result.isEmpty() ? "Non spécifiée" : result;
    }

    // ==================== OFFRES SIMILAIRES ====================

    /**
     * Trouve les offres similaires basées sur secteur, type de contrat ou entreprise
     */
    private List<OffreEmploi> findSimilarOffers(OffreEmploi reference, int maxResults) {
        if (master == null || reference == null) {
            return Collections.emptyList();
        }

        String refSecteur = reference.getSecteur() != null ? reference.getSecteur().getNom() : null;
        String refContrat = reference.getTypeContrat() != null ? reference.getTypeContrat().getNom() : null;
        String refEntreprise = reference.getNomEntreprise();
        String refVille = reference.getLocalisation() != null ? reference.getLocalisation().getVille() : null;

        // Calculer un score pour chaque offre
        Map<OffreEmploi, Integer> scoreMap = new LinkedHashMap<>();

        for (OffreEmploi offre : master) {
            // Ne pas inclure l'offre de référence
            if (offre.getId() == reference.getId()) {
                continue;
            }

            int score = 0;

            // Même secteur = +3 points
            String secteur = offre.getSecteur() != null ? offre.getSecteur().getNom() : null;
            if (refSecteur != null && secteur != null && refSecteur.equalsIgnoreCase(secteur)) {
                score += 3;
            }

            // Même type de contrat = +2 points
            String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
            if (refContrat != null && contrat != null && refContrat.equalsIgnoreCase(contrat)) {
                score += 2;
            }

            // Même entreprise = +2 points
            String entreprise = offre.getNomEntreprise();
            if (refEntreprise != null && entreprise != null && refEntreprise.equalsIgnoreCase(entreprise)) {
                score += 2;
            }

            // Même ville = +1 point
            String ville = offre.getLocalisation() != null ? offre.getLocalisation().getVille() : null;
            if (refVille != null && ville != null && refVille.equalsIgnoreCase(ville)) {
                score += 1;
            }

            // Ne garder que les offres avec au moins un critère commun
            if (score > 0) {
                scoreMap.put(offre, score);
            }
        }

        // Trier par score décroissant et limiter les résultats
        return scoreMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(maxResults)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Crée une carte compacte pour une offre similaire
     */
    private VBox createSimilarOfferCard(OffreEmploi o, int score) {
        VBox card = new VBox(8);
        card.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 14; " +
            "-fx-border-color: #e2e8f0; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1; " +
            "-fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);"
        );
        
        // Titre
        Label title = new Label(safe(o.getTitre(), "Offre"));
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #1e293b;");
        title.setWrapText(true);
        
        // Entreprise
        Label company = new Label(safe(o.getNomEntreprise(), ""));
        company.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 600;");
        
        // Localisation | Contrat
        String loc = o.getLocalisation() != null && o.getLocalisation().getVille() != null 
            ? o.getLocalisation().getVille() : "";
        String contrat = o.getTypeContrat() != null ? o.getTypeContrat().getNom() : "";
        Label meta = new Label("📍 " + (loc.isEmpty() ? "-" : loc) + " | " + (contrat.isEmpty() ? "-" : contrat));
        meta.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        // Score badge
        Label scoreBadge = new Label("★ " + score + " match");
        scoreBadge.setStyle(
            "-fx-background-color: #dbeafe; " +
            "-fx-text-fill: #3b82f6; " +
            "-fx-padding: 3 10; " +
            "-fx-background-radius: 10; " +
            "-fx-font-size: 11px; " +
            "-fx-font-weight: 700;"
        );
        
        card.getChildren().addAll(title, company, meta, scoreBadge);
        
        // Click handler
        card.setOnMouseClicked(e -> showOfferDetailsPanel(o));
        
        // Hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 12; " +
                "-fx-border-color: #3b82f6; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 2; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(59, 130, 246, 0.15), 6, 0, 0, 2);"
            );
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: white; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 12; " +
                "-fx-border-color: #e2e8f0; " +
                "-fx-border-radius: 12; " +
                "-fx-border-width: 1; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);"
            );
        });
        
        return card;
    }

    /**
     * Calcule le score de similarité entre deux offres
     */
    private int calculateSimilarityScore(OffreEmploi ref, OffreEmploi other) {
        int score = 0;
        
        // Même secteur = +3 points
        String refSecteur = ref.getSecteur() != null ? ref.getSecteur().getNom() : null;
        String otherSecteur = other.getSecteur() != null ? other.getSecteur().getNom() : null;
        if (refSecteur != null && otherSecteur != null && refSecteur.equalsIgnoreCase(otherSecteur)) {
            score += 3;
        }
        
        // Même type de contrat = +2 points
        String refContrat = ref.getTypeContrat() != null ? ref.getTypeContrat().getNom() : null;
        String otherContrat = other.getTypeContrat() != null ? other.getTypeContrat().getNom() : null;
        if (refContrat != null && otherContrat != null && refContrat.equalsIgnoreCase(otherContrat)) {
            score += 2;
        }
        
        // Même entreprise = +2 points
        String refEntreprise = ref.getNomEntreprise();
        String otherEntreprise = other.getNomEntreprise();
        if (refEntreprise != null && otherEntreprise != null && refEntreprise.equalsIgnoreCase(otherEntreprise)) {
            score += 2;
        }
        
        // Même ville = +1 point
        String refVille = ref.getLocalisation() != null ? ref.getLocalisation().getVille() : null;
        String otherVille = other.getLocalisation() != null ? other.getLocalisation().getVille() : null;
        if (refVille != null && otherVille != null && refVille.equalsIgnoreCase(otherVille)) {
            score += 1;
        }
        
        return score;
    }

    // ==================== COMPARAISON D'OFFRES ====================

    private final java.util.Set<OffreEmploi> selectedOffersForComparison = new java.util.HashSet<>();
    private boolean comparisonMode = false;

    @FXML
    private Button btnComparer;

    @FXML
    void activerModeComparaison(ActionEvent event) {
        comparisonMode = !comparisonMode;
        if (!comparisonMode) {
            selectedOffersForComparison.clear();
        }
        updateSelectionCount();
        refresh(null);
    }

    private void updateSelectionCount() {
        if (btnComparer != null) {
            btnComparer.setText("⚖️ Comparer (" + selectedOffersForComparison.size() + "/3)");
        }
    }
    
    /**
     * Compare la sélection actuelle
     */
    @FXML
    void comparerSelection(ActionEvent event) {
        if (selectedOffersForComparison.size() < 2) {
            showAlert(Alert.AlertType.WARNING, 
                "Sélection insuffisante", 
                "Veuillez sélectionner au moins 2 offres pour la comparaison (maximum 3).\n\n" +
                "Offres actuellement sélectionnées : " + selectedOffersForComparison.size());
            return;
        }
        
        if (selectedOffersForComparison.size() > 3) {
            showAlert(Alert.AlertType.WARNING, 
                "Trop d'offres", 
                "Vous ne pouvez comparer que 3 offres maximum.\n\n" +
                "Veuillez décocher certaines offres.");
            return;
        }
        
        ouvrirComparaison();
    }
    
    /**
     * Ouvre le modal de comparaison avec les offres sélectionnées
     */
    private void ouvrirComparaison() {
        if (selectedOffersForComparison.size() < 2) {
            showAlert(Alert.AlertType.WARNING, 
                "Sélection insuffisante", 
                "Veuillez sélectionner au moins 2 offres pour la comparaison (maximum 3).");
            return;
        }
        
        if (selectedOffersForComparison.size() > 3) {
            showAlert(Alert.AlertType.WARNING, 
                "Trop d'offres", 
                "Vous ne pouvez comparer que 3 offres maximum. Veuillez décocher certaines offres.");
            return;
        }
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/Offre/ComparerOffres.fxml")
            );
            javafx.scene.Parent root = loader.load();
            
            // Passer les offres sélectionnées au controller
            ComparerOffresController controller = loader.getController();
            controller.setOffres(new java.util.ArrayList<>(selectedOffersForComparison));
            controller.setCanAddOffer(selectedOffersForComparison.size() < 3);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Comparaison d'Offres");
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            stage.setMaximized(true);
            
            if (offersFlowPane != null && offersFlowPane.getScene() != null 
                    && offersFlowPane.getScene().getWindow() != null) {
                stage.initOwner(offersFlowPane.getScene().getWindow());
            }
            
            controller.setOnAddOfferRequested(() -> {
                showAlert(Alert.AlertType.INFORMATION,
                        "Ajouter une offre",
                        "Sélectionnez une autre offre (maximum 3) puis cliquez à nouveau sur 'Comparer'.");
                updateSelectionCount();
            });

            stage.show();
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, 
                "Erreur", 
                "Impossible d'ouvrir la comparaison: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void updateCompareButtonState() {
        // Cette méthode est appelée quand la sélection change
        // Elle peut être utilisée pour mettre à jour l'UI en temps réel
    }

    /**
     * Ferme le panneau de détails et revient aux statistiques
     */
    @FXML
    void closeDetailsPanel() {
        if (detailsPanel != null) {
            detailsPanel.setVisible(false);
            detailsPanel.setManaged(false);
        }
        if (statsPanel != null) {
            statsPanel.setVisible(true);
            statsPanel.setManaged(true);
        }
        currentSelectedOffer = null;
    }

    /**
     * Ouvre la carte interactive des offres dans une fenêtre modale
     */
    @FXML
    void ouvrirCarteOffres(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/Offre/OffresMap.fxml")
            );
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Carte des Offres d'Emploi");
            stage.setScene(new javafx.scene.Scene(root, 1000, 700));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            // Centrer la fenêtre par rapport à la fenêtre principale
            if (offersFlowPane != null && offersFlowPane.getScene() != null 
                    && offersFlowPane.getScene().getWindow() != null) {
                stage.initOwner(offersFlowPane.getScene().getWindow());
            }

            stage.show();

        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir la carte: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }
    
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // ==================== EXPORT PDF & QR CODE ====================

    private void exportOfferToPdf(OffreEmploi offre) {
        try {
            Services.Export.PdfExportService pdfService = new Services.Export.PdfExportService();

            // Choix du fichier de sortie
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Enregistrer l'offre en PDF");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PDF files", "*.pdf")
            );

            // Nom par défaut
            String safeTitle = offre.getTitre() != null ? offre.getTitre().replaceAll("[^a-zA-Z0-9\\s-]", "") : "offre";
            fileChooser.setInitialFileName("CareerLink_" + safeTitle.replaceAll("\\s+", "_") + ".pdf");

            javafx.stage.Window window = offerDetailsContent.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(window);

            if (file != null) {
                pdfService.exportOffreToPdf(offre, file);
                showAlert(Alert.AlertType.INFORMATION, "Export réussi",
                    "L'offre a été exportée en PDF :\n" + file.getAbsolutePath());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur export",
                "Impossible d'exporter en PDF : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showQrCodeDialog(OffreEmploi offre) {
        try {
            Services.Export.QrCodeService qrService = new Services.Export.QrCodeService();

            // Générer un texte structuré avec les détails de l'offre
            String qrContent = buildOfferQrContent(offre);
            java.awt.image.BufferedImage qrImage = qrService.generateQrCodeImage(qrContent, 300, 300);

            // Convertir en Image JavaFX
            javafx.scene.image.WritableImage fxImage = convertToFxImage(qrImage);

            // Créer dialog
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("QR Code - " + offre.getTitre());
            dialog.setHeaderText("Scannez ce QR Code pour voir les détails de l'offre");

            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
            imageView.setPreserveRatio(true);

            // Info label
            javafx.scene.control.Label infoLabel = new javafx.scene.control.Label(
                "Ce QR Code contient les informations de l'offre.\n" +
                "Scannez-le avec n'importe quel lecteur QR pour voir les détails."
            );
            infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-text-alignment: center;");
            infoLabel.setWrapText(true);
            infoLabel.setAlignment(javafx.geometry.Pos.CENTER);

            VBox content = new VBox(15, imageView, infoLabel);
            content.setAlignment(javafx.geometry.Pos.CENTER);
            content.setPadding(new javafx.geometry.Insets(20));

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            // Sauvegarder bouton
            ButtonType saveBtn = new ButtonType("💾 Sauvegarder", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(0, saveBtn);

            dialog.showAndWait().ifPresent(result -> {
                if (result == saveBtn) {
                    saveQrCodeToFile(qrImage, offre);
                }
            });

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur QR Code",
                "Impossible de générer le QR Code : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Construit un contenu texte pour le QR Code au format propre et moderne
     * Évite les caractères d'échappement visibles
     */
    private String buildOfferQrContent(OffreEmploi offre) {
        StringBuilder sb = new StringBuilder();
        
        // En-tête moderne avec emoji
        sb.append("📋 OFFRE CAREERLINK\n");
        sb.append("━━━━━━━━━━━━━━━━━━━\n\n");
        
        // Titre du poste
        sb.append("💼 ");
        sb.append(offre.getTitre() != null ? offre.getTitre() : "Poste");
        sb.append("\n\n");
        
        // Entreprise
        String company = offre.getNomEntreprise();
        if (company != null && !company.isBlank()) {
            sb.append("🏢 ");
            sb.append(company);
            sb.append("\n");
        }
        
        // Localisation
        String loc = getLocationText(offre);
        if (!loc.equals("Non spécifiée")) {
            sb.append("📍 ");
            sb.append(loc);
            sb.append("\n");
        }
        
        // Type contrat
        String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
        if (contrat != null) {
            sb.append("📝 ");
            sb.append(contrat);
            sb.append("\n");
        }
        
        // Expérience
        String exp = offre.getNiveauExperience();
        if (exp != null && !exp.isBlank()) {
            sb.append("⭐ ");
            sb.append(exp);
            sb.append("\n");
        }
        
        sb.append("\n━━━━━━━━━━━━━━━━━━━\n");
        
        // Description
        String desc = offre.getDescription();
        if (desc != null && !desc.isBlank()) {
            String truncated = desc.length() > 150 ? desc.substring(0, 150) + "..." : desc;
            sb.append(truncated);
            sb.append("\n\n");
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━\n");
        
        // Date limite
        if (offre.getDateLimiteCandidature() != null) {
            sb.append("⏰ Date limite: ");
            sb.append(offre.getDateLimiteCandidature().toString());
            sb.append("\n");
        }
        
        // ID pour référence
        sb.append("🔹 Réf: ");
        sb.append(offre.getId());
        
        return sb.toString();
    }

    private javafx.scene.image.WritableImage convertToFxImage(java.awt.image.BufferedImage awtImage) {
        javafx.scene.image.WritableImage fxImage = new javafx.scene.image.WritableImage(
            awtImage.getWidth(), awtImage.getHeight());
        javafx.scene.image.PixelWriter pw = fxImage.getPixelWriter();

        for (int x = 0; x < awtImage.getWidth(); x++) {
            for (int y = 0; y < awtImage.getHeight(); y++) {
                int argb = awtImage.getRGB(x, y);
                pw.setArgb(x, y, argb);
            }
        }
        return fxImage;
    }

    private void saveQrCodeToFile(java.awt.image.BufferedImage qrImage, OffreEmploi offre) {
        try {
            // Créer le dossier CareerLink_QR_Codes dans le dossier utilisateur
            String userHome = System.getProperty("user.home");
            java.io.File qrFolder = new java.io.File(userHome, "CareerLink_QR_Codes");
            if (!qrFolder.exists()) {
                qrFolder.mkdirs();
            }
            
            // Créer sous-dossier par entreprise
            String companyName = offre.getNomEntreprise();
            if (companyName == null || companyName.isBlank()) {
                companyName = "Autres";
            }
            String safeCompany = companyName.replaceAll("[^a-zA-Z0-9\\s-]", "").trim();
            if (safeCompany.isBlank()) {
                safeCompany = "Autres";
            }
            
            java.io.File companyFolder = new java.io.File(qrFolder, safeCompany);
            if (!companyFolder.exists()) {
                companyFolder.mkdirs();
            }
            
            // Générer nom de fichier de base
            String safeTitle = offre.getTitre() != null ? 
                offre.getTitre().replaceAll("[^a-zA-Z0-9\\s-]", "").trim() : "offre";
            if (safeTitle.length() > 30) {
                safeTitle = safeTitle.substring(0, 30);
            }
            
            String dateStr = java.time.LocalDate.now().toString();
            String baseFileName = String.format("QR_%s_%s_%s", 
                offre.getId(), 
                dateStr,
                safeTitle.replaceAll("\\s+", "_")
            );
            
            // Sauvegarder l'image QR Code
            java.io.File imageFile = new java.io.File(companyFolder, baseFileName + ".png");
            javax.imageio.ImageIO.write(qrImage, "PNG", imageFile);
            
            // Créer fichier texte avec les détails complets
            java.io.File textFile = new java.io.File(companyFolder, baseFileName + ".txt");
            java.nio.file.Files.write(textFile.toPath(), buildOfferTextFile(offre).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            // Créer fichier HTML moderne
            java.io.File htmlFile = new java.io.File(companyFolder, baseFileName + ".html");
            java.nio.file.Files.write(htmlFile.toPath(), buildOfferHtmlFile(offre).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            showAlert(Alert.AlertType.INFORMATION, "✅ Fichiers sauvegardés",
                "📁 Emplacement : " + companyFolder.getAbsolutePath() + "\n\n" +
                "📄 Fichiers créés :\n" +
                "  • " + imageFile.getName() + " (QR Code)\n" +
                "  • " + textFile.getName() + " (Détails texte)\n" +
                "  • " + htmlFile.getName() + " (Page web moderne)\n\n" +
                "Ouvrez le fichier .html dans votre navigateur pour une belle présentation !");
                
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "❌ Erreur sauvegarde",
                "Impossible de sauvegarder : " + e.getMessage());
        }
    }

    /**
     * Construit le contenu du fichier texte avec les détails de l'offre
     */
    private String buildOfferTextFile(OffreEmploi offre) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("  📋 OFFRE D'EMPLOI - CareerLink\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        sb.append("💼 POSTE :\n");
        sb.append("   ").append(offre.getTitre() != null ? offre.getTitre() : "Non spécifié").append("\n\n");
        
        String company = offre.getNomEntreprise();
        if (company != null && !company.isBlank()) {
            sb.append("🏢 ENTREPRISE :\n");
            sb.append("   ").append(company).append("\n\n");
        }
        
        String loc = getLocationText(offre);
        if (!loc.equals("Non spécifiée")) {
            sb.append("📍 LOCALISATION :\n");
            sb.append("   ").append(loc).append("\n\n");
        }
        
        String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
        if (contrat != null) {
            sb.append("📋 TYPE DE CONTRAT :\n");
            sb.append("   ").append(contrat).append("\n\n");
        }
        
        String secteur = offre.getSecteur() != null ? offre.getSecteur().getNom() : null;
        if (secteur != null) {
            sb.append("🏢 SECTEUR :\n");
            sb.append("   ").append(secteur).append("\n\n");
        }
        
        String exp = offre.getNiveauExperience();
        if (exp != null && !exp.isBlank()) {
            sb.append("⭐ EXPÉRIENCE REQUISE :\n");
            sb.append("   ").append(exp).append("\n\n");
        }
        
        String etudes = offre.getNiveauEtudes();
        if (etudes != null && !etudes.isBlank()) {
            sb.append("🎓 NIVEAU D'ÉTUDES :\n");
            sb.append("   ").append(etudes).append("\n\n");
        }
        
        String langues = offre.getLanguesRequises();
        if (langues != null && !langues.isBlank()) {
            sb.append("🌐 LANGUES :\n");
            sb.append("   ").append(langues).append("\n\n");
        }
        
        Integer postes = offre.getNombrePoste();
        if (postes != null) {
            sb.append("👥 NOMBRE DE POSTES :\n");
            sb.append("   ").append(postes).append("\n\n");
        }
        
        Boolean teletravail = offre.getTeletravail();
        if (teletravail != null && teletravail) {
            sb.append("🏠 TÉLÉTRAVAIL :\n");
            sb.append("   ✅ Oui\n\n");
        }
        
        if (offre.getDateLimiteCandidature() != null) {
            sb.append("📅 DATE LIMITE :\n");
            sb.append("   ").append(offre.getDateLimiteCandidature().toString()).append("\n\n");
        }
        
        sb.append("═══════════════════════════════════════════════════\n");
        sb.append("📝 DESCRIPTION DU POSTE :\n");
        sb.append("═══════════════════════════════════════════════════\n\n");
        
        String desc = offre.getDescription();
        if (desc != null && !desc.isBlank()) {
            // Formater la description avec retours à la ligne
            String[] lines = desc.split("\\n");
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        } else {
            sb.append("Aucune description disponible.\n");
        }
        
        sb.append("\n═══════════════════════════════════════════════════\n");
        sb.append("🔹 Référence offre : ").append(offre.getId()).append("\n");
        sb.append("📅 Fichier généré le : ").append(java.time.LocalDate.now().toString()).append("\n");
        sb.append("═══════════════════════════════════════════════════\n");
        
        return sb.toString();
    }

    /**
     * Construit le contenu du fichier HTML moderne
     */
    private String buildOfferHtmlFile(OffreEmploi offre) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"fr\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(escapeHtml(offre.getTitre())).append(" - CareerLink</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }\n");
        html.append("        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 20px; overflow: hidden; box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25); }\n");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 40px 30px; text-align: center; color: white; }\n");
        html.append("        .header h1 { font-size: 28px; font-weight: 700; margin-bottom: 10px; }\n");
        html.append("        .header .company { font-size: 18px; opacity: 0.9; }\n");
        html.append("        .content { padding: 30px; }\n");
        html.append("        .badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: 600; text-transform: uppercase; margin-bottom: 20px; }\n");
        html.append("        .badge.active { background: #dcfce7; color: #166534; }\n");
        html.append("        .badge.inactive { background: #fee2e2; color: #991b1b; }\n");
        html.append("        .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 25px 0; }\n");
        html.append("        .info-item { background: #f8fafc; padding: 15px; border-radius: 12px; }\n");
        html.append("        .info-item .label { font-size: 11px; color: #64748b; text-transform: uppercase; font-weight: 600; margin-bottom: 5px; }\n");
        html.append("        .info-item .value { font-size: 14px; color: #0f172a; font-weight: 600; }\n");
        html.append("        .description { background: #f8fafc; padding: 25px; border-radius: 12px; margin: 25px 0; }\n");
        html.append("        .description h3 { color: #0f172a; font-size: 16px; margin-bottom: 15px; }\n");
        html.append("        .description p { color: #475569; line-height: 1.7; }\n");
        html.append("        .footer { background: #f8fafc; padding: 20px 30px; text-align: center; color: #64748b; font-size: 12px; }\n");
        html.append("        .date-limit { background: #fef3c7; color: #92400e; padding: 15px 20px; border-radius: 10px; margin: 20px 0; text-align: center; font-weight: 600; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        html.append("    <div class=\"container\">\n");
        
        // Header
        html.append("        <div class=\"header\">\n");
        html.append("            <h1>💼 ").append(escapeHtml(offre.getTitre())).append("</h1>\n");
        String company = offre.getNomEntreprise();
        if (company != null && !company.isBlank()) {
            html.append("            <div class=\"company\">🏢 ").append(escapeHtml(company)).append("</div>\n");
        }
        html.append("        </div>\n");
        
        html.append("        <div class=\"content\">\n");
        
        // Badge status
        String status = offre.getStatus();
        if ("active".equalsIgnoreCase(status)) {
            html.append("            <span class=\"badge active\">● Active</span>\n");
        } else {
            html.append("            <span class=\"badge inactive\">● Non active</span>\n");
        }
        
        // Infos grid
        html.append("            <div class=\"info-grid\">\n");
        
        String loc = getLocationText(offre);
        if (!loc.equals("Non spécifiée")) {
            html.append("                <div class=\"info-item\">\n");
            html.append("                    <div class=\"label\">📍 Localisation</div>\n");
            html.append("                    <div class=\"value\">").append(escapeHtml(loc)).append("</div>\n");
            html.append("                </div>\n");
        }
        
        String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
        if (contrat != null) {
            html.append("                <div class=\"info-item\">\n");
            html.append("                    <div class=\"label\">📋 Contrat</div>\n");
            html.append("                    <div class=\"value\">").append(escapeHtml(contrat)).append("</div>\n");
            html.append("                </div>\n");
        }
        
        String exp = offre.getNiveauExperience();
        if (exp != null && !exp.isBlank()) {
            html.append("                <div class=\"info-item\">\n");
            html.append("                    <div class=\"label\">⭐ Expérience</div>\n");
            html.append("                    <div class=\"value\">").append(escapeHtml(exp)).append("</div>\n");
            html.append("                </div>\n");
        }
        
        String etudes = offre.getNiveauEtudes();
        if (etudes != null && !etudes.isBlank()) {
            html.append("                <div class=\"info-item\">\n");
            html.append("                    <div class=\"label\">🎓 Études</div>\n");
            html.append("                    <div class=\"value\">").append(escapeHtml(etudes)).append("</div>\n");
            html.append("                </div>\n");
        }
        
        html.append("            </div>\n");
        
        // Description
        String desc = offre.getDescription();
        if (desc != null && !desc.isBlank()) {
            html.append("            <div class=\"description\">\n");
            html.append("                <h3>📝 Description du poste</h3>\n");
            html.append("                <p>").append(escapeHtml(desc).replace("\n", "<br>")).append("</p>\n");
            html.append("            </div>\n");
        }
        
        // Date limite
        if (offre.getDateLimiteCandidature() != null) {
            html.append("            <div class=\"date-limit\">\n");
            html.append("                ⏰ Date limite de candidature : ").append(offre.getDateLimiteCandidature().toString()).append("\n");
            html.append("            </div>\n");
        }
        
        html.append("        </div>\n");
        
        // Footer
        html.append("        <div class=\"footer\">\n");
        html.append("            © 2025 CareerLink - Réf. ").append(offre.getId()).append("<br>\n");
        html.append("            Fichier généré le ").append(java.time.LocalDate.now().toString()).append("\n");
        html.append("        </div>\n");
        
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        return html.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * Affiche le QR Code pour scanning, avec option de sauvegarde
     */
    private void showQrCodeForScanning(OffreEmploi offre) {
        try {
            Services.Export.QrCodeService qrService = new Services.Export.QrCodeService();
            
            // Générer le QR Code
            String qrContent = buildOfferQrContent(offre);
            java.awt.image.BufferedImage qrImage = qrService.generateQrCodeImage(qrContent, 400, 400);
            
            // Convertir pour JavaFX
            javafx.scene.image.WritableImage fxImage = convertToFxImage(qrImage);
            
            // Créer le dialog
            javafx.scene.control.Dialog<ButtonType> dialog = new javafx.scene.control.Dialog<>();
            dialog.setTitle("📱 QR Code - " + offre.getTitre());
            dialog.setHeaderText("Scannez ce QR Code avec votre smartphone");
            
            // Image QR
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(fxImage);
            imageView.setPreserveRatio(true);
            
            // Instructions
            javafx.scene.control.Label infoLabel = new javafx.scene.control.Label(
                "1. Ouvrez l'appareil photo ou un lecteur QR sur votre téléphone\n" +
                "2. Pointez vers ce code pour voir les détails de l'offre\n" +
                "3. Cliquez sur \"💾 Sauvegarder\" pour enregistrer le fichier"
            );
            infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-text-alignment: center;");
            infoLabel.setWrapText(true);
            infoLabel.setAlignment(javafx.geometry.Pos.CENTER);
            
            // Layout
            VBox content = new VBox(20, imageView, infoLabel);
            content.setAlignment(javafx.geometry.Pos.CENTER);
            content.setPadding(new javafx.geometry.Insets(30));
            
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);
            
            // Bouton Sauvegarder
            ButtonType saveBtn = new ButtonType("💾 Sauvegarder", ButtonBar.ButtonData.LEFT);
            dialog.getDialogPane().getButtonTypes().add(0, saveBtn);
            
            // Style du dialog
            dialog.getDialogPane().setStyle("-fx-background-color: white;");
            
            dialog.showAndWait().ifPresent(result -> {
                if (result == saveBtn) {
                    saveQrCodeToFile(qrImage, offre);
                }
            });
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur QR Code",
                "Impossible d'afficher le QR Code : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Génère et sauvegarde automatiquement le QR Code (sans affichage)
     */
    private void generateAndSaveQrCode(OffreEmploi offre) {
        try {
            Services.Export.QrCodeService qrService = new Services.Export.QrCodeService();
            String qrContent = buildOfferQrContent(offre);
            java.awt.image.BufferedImage qrImage = qrService.generateQrCodeImage(qrContent, 400, 400);
            
            saveQrCodeToFile(qrImage, offre);
            
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                "Impossible de générer le QR Code : " + e.getMessage());
        }
    }
}
