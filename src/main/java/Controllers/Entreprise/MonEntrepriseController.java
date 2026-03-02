package Controllers.Entreprise;

import Entities.Entreprise.Entreprise;
import Entities.Offre.OffreEmploi;
import Entities.OffreStatistiques;
import Services.Entreprise.EntrepriseService;
import Services.Offre.OfrreEmploiServices;
import Services.Offre.OfrreEmploiServices.OffreVueDetails;
import Utils.Session;
import Utils.ThemeManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.application.Platform;
import javafx.scene.chart.*;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Comparator;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class MonEntrepriseController {

    @FXML private ImageView ivLogo;
    @FXML private Label lblNom;
    @FXML private Label lblEmail;

    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblTelephone;
    @FXML private Label lblEmailContact;
    @FXML private Label lblDescription;
    @FXML private Label lblTotalOffres;
    @FXML private Label lblTotalCandidatures;
    @FXML private Label lblTotalFiliales;
    @FXML private Label lblTauxMarche;
    @FXML private Label lblTotalVues;
    @FXML private Label lblTauxConversion;
    @FXML private Label lblVilleBadge;
    @FXML private Label lblTelephoneBadge;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    @FXML private PieChart secteurPieChart;
    @FXML private Label lblSecteurPrincipal;
    
    // Liste des filiales (remplace le BarChart)
    @FXML private VBox filialesListContainer;

    private final EntrepriseService entrepriseService = new EntrepriseService();
    private final OfrreEmploiServices offreService = new OfrreEmploiServices();

    private Entreprise entreprise;

    @FXML
    private void initialize() {
        System.out.println("[DEBUG] initialize() - lblTotalOffres: " + (lblTotalOffres != null ? "OK" : "NULL"));
        reload();
    }

    private void reload() {
        if (Session.getCurrentUser() == null) {
            new Alert(Alert.AlertType.ERROR, "Utilisateur non connecté.").show();
            return;
        }
        entreprise = entrepriseService.getByUserId(Session.getCurrentUser().getId());
        if (entreprise == null) {
            redirectToAjouterEntreprise();
            return;
        }

        if (lblNom != null) lblNom.setText(nvl(entreprise.getNomEntreprise()));
        if (lblEmail != null) lblEmail.setText(nvl(entreprise.getEmailContact()));

        if (lblAdresse != null) lblAdresse.setText(nvl(entreprise.getAdresse()));
        if (lblVille != null) lblVille.setText(nvl(entreprise.getVille()));
        if (lblTelephone != null) lblTelephone.setText(nvl(entreprise.getTelephone()));
        if (lblEmail != null) lblEmail.setText(nvl(entreprise.getEmailContact()));
        if (lblEmailContact != null) lblEmailContact.setText(nvl(entreprise.getEmailContact()));
        if (lblDescription != null) lblDescription.setText(nvl(entreprise.getDescription()));
        
        if (lblVilleBadge != null) lblVilleBadge.setText(nvl(entreprise.getVille()));
        if (lblTelephoneBadge != null) lblTelephoneBadge.setText(nvl(entreprise.getTelephone()));

        // Charger les statistiques globales
        loadStatistics();
        
        // Charger les données des graphiques - async pour éviter la lenteur
        // Charger les données des graphiques
        loadCharts();

        refreshLogo();
    }
    
    private void loadStatistics() {
        try {
            int idEntreprise = entreprise.getIdEntreprise();
            
            // Charger les offres de l'entreprise
            List<OffreEmploi> offres = offreService.afficherOfrreEmploiLightByEntrepriseId(idEntreprise);
            int totalOffres = (offres != null) ? offres.size() : 0;
            
            // Calculer le nombre de filiales uniques
            int totalFiliales = (offres != null) ? offreService.countFilialesUniques(idEntreprise) : 0;
            
            // Calculer le taux du marché
            double tauxMarche = (offres != null) ? offreService.getTauxMarcheEntreprise(idEntreprise) : 0;
            
            System.out.println("[DEBUG] Entreprise " + idEntreprise + " - Offres trouvées: " + totalOffres + " - Filiales: " + totalFiliales + " - Taux marché: " + String.format("%.1f%%", tauxMarche));
            
            // Mettre à jour directement
            if (lblTotalOffres != null) {
                lblTotalOffres.setText(String.valueOf(totalOffres));
                // Force refresh
                lblTotalOffres.setVisible(false);
                lblTotalOffres.setVisible(true);
                if (lblTotalOffres.getParent() != null) {
                    lblTotalOffres.getParent().layout();
                }
            }
            
            if (lblTotalFiliales != null) lblTotalFiliales.setText(String.valueOf(totalFiliales));
            if (lblTauxMarche != null) lblTauxMarche.setText(String.format("%.1f%%", tauxMarche));
            
            // Stats globales (candidatures, vues)
            var stats = offreService.getStatistiquesGlobales(idEntreprise);
            if (lblTotalCandidatures != null) lblTotalCandidatures.setText(String.valueOf(stats.getTotalCandidatures()));
            if (lblTotalVues != null) lblTotalVues.setText(String.valueOf(stats.getTotalVues()));
            if (lblTauxConversion != null) lblTauxConversion.setText(String.format("%.1f%%", stats.getTauxConversionGlobal()));
            
        } catch (Exception e) {
            e.printStackTrace();
            if (lblTotalOffres != null) lblTotalOffres.setText("0");
            if (lblTotalFiliales != null) lblTotalFiliales.setText("0");
            if (lblTauxMarche != null) lblTauxMarche.setText("0%");
            if (lblTotalVues != null) lblTotalVues.setText("0");
        }
    }
    
    private void loadCharts() {
        int idEntreprise = entreprise.getIdEntreprise();
        
        try {
            Map<String, Integer> secteurStats = offreService.getOffresParSecteur(idEntreprise);

            if (secteurPieChart != null) {
                secteurPieChart.getData().clear();

                if (secteurStats == null || secteurStats.isEmpty()) {
                    if (lblSecteurPrincipal != null) lblSecteurPrincipal.setText("");
                } else {
                    // Calculer le total pour les pourcentages
                    int total = secteurStats.values().stream().mapToInt(Integer::intValue).sum();
                    
                    // Trier par valeur décroissante
                    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(secteurStats.entrySet());
                    sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    
                    // Limiter à 5 secteurs principaux, le reste = "Autres"
                    List<Map.Entry<String, Integer>> topSectors = sorted.size() > 5 ? sorted.subList(0, 5) : sorted;
                    int topTotal = topSectors.stream().mapToInt(Map.Entry::getValue).sum();
                    int autres = total - topTotal;
                    
                    // Couleurs modernes pour le donut
                    String[] colors = {"#6366f1", "#8b5cf6", "#ec4899", "#f43f5e", "#10b981", "#f59e0b"};
                    int colorIdx = 0;
                    
                    // Ajouter les données au PieChart
                    for (Map.Entry<String, Integer> entry : topSectors) {
                        String nom = entry.getKey() != null && !entry.getKey().trim().isEmpty() 
                            ? entry.getKey() 
                            : "Non classé";
                        int count = entry.getValue();
                        double pct = (count * 100.0) / total;
                        
                        PieChart.Data data = new PieChart.Data(nom + " (" + String.format("%.0f%%", pct) + ")", count);
                        secteurPieChart.getData().add(data);
                        
                        // Appliquer la couleur via style inline (sera stylisé par CSS)
                        colorIdx++;
                    }
                    
                    // Ajouter "Autres" si nécessaire
                    if (autres > 0 && sorted.size() > 5) {
                        double pctAutres = (autres * 100.0) / total;
                        PieChart.Data data = new PieChart.Data("Autres (" + String.format("%.0f%%", pctAutres) + ")", autres);
                        secteurPieChart.getData().add(data);
                    }
                    
                    // Mettre à jour le label central avec le secteur principal
                    if (lblSecteurPrincipal != null && !sorted.isEmpty()) {
                        String topSector = sorted.get(0).getKey();
                        int topCount = sorted.get(0).getValue();
                        double topPct = (topCount * 100.0) / total;
                        lblSecteurPrincipal.setText(String.format("%.0f%%", topPct));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur secteurs: " + e.getMessage());
        }
        
        try {
            Map<String, Integer> locStats = offreService.getOffresParLocalisation(idEntreprise);
            
            if (filialesListContainer != null) {
                filialesListContainer.getChildren().clear();
                
                if (locStats.isEmpty()) {
                    filialesListContainer.getChildren().add(new Label("Aucune donnée"));
                } else {
                    int max = locStats.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                    int total = locStats.values().stream().mapToInt(Integer::intValue).sum();

                    List<Map.Entry<String, Integer>> sorted = new ArrayList<>(locStats.entrySet());
                    sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
                    if (sorted.size() > 10) {
                        sorted = sorted.subList(0, 10);
                    }

                    // Header with total
                    HBox header = new HBox(10);
                    header.setAlignment(Pos.CENTER_LEFT);
                    Label totalLabel = new Label("📍 " + sorted.size() + " sites actifs");
                    totalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b; -fx-font-weight: 600;");
                    header.getChildren().add(totalLabel);
                    filialesListContainer.getChildren().add(header);

                    // Separator
                    Region sep = new Region();
                    sep.setMinHeight(1);
                    sep.setStyle("-fx-background-color: #e2e8f0;");
                    sep.setMaxWidth(Double.MAX_VALUE);
                    filialesListContainer.getChildren().add(sep);

                    // Color palette for cities
                    String[] colors = {"#6366f1", "#8b5cf6", "#ec4899", "#f43f5e", "#10b981", "#3b82f6", "#f59e0b", "#06b6d4"};
                    int colorIdx = 0;

                    for (Map.Entry<String, Integer> entry : sorted) {
                        String ville = entry.getKey() != null ? entry.getKey() : "Non spécifiée";
                        int count = entry.getValue() != null ? entry.getValue() : 0;
                        double percentage = (count * 100.0) / max;
                        double totalPercentage = (count * 100.0) / total;
                        String color = colors[colorIdx % colors.length];

                        // Modern city card
                        VBox cityCard = new VBox(8);
                        cityCard.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 12; " +
                                         "-fx-border-color: #f1f5f9; -fx-border-radius: 12; -fx-border-width: 1;");

                        // Top row: icon + city name + count badge
                        HBox topRow = new HBox(10);
                        topRow.setAlignment(Pos.CENTER_LEFT);

                        // City icon with colored background
                        Label iconLabel = new Label("🏢");
                        iconLabel.setStyle("-fx-font-size: 16px; -fx-background-color: " + color + "20; " +
                                          "-fx-padding: 8; -fx-background-radius: 10;");

                        Label villeLabel = new Label(ville);
                        villeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
                        HBox.setHgrow(villeLabel, Priority.ALWAYS);

                        // Count badge
                        Label countBadge = new Label(String.valueOf(count));
                        countBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                                            "-fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 4 12; " +
                                            "-fx-background-radius: 20px;");

                        topRow.getChildren().addAll(iconLabel, villeLabel, countBadge);

                        // Progress bar showing relative size
                        Region progressBg = new Region();
                        progressBg.setMinHeight(6);
                        progressBg.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 3;");
                        progressBg.setMaxWidth(Double.MAX_VALUE);

                        StackPane progressBar = new StackPane();
                        progressBar.setMaxWidth(Double.MAX_VALUE);
                        progressBar.setAlignment(Pos.CENTER_LEFT);

                        Region progressFill = new Region();
                        progressFill.setMinHeight(6);
                        progressFill.setPrefWidth(percentage); // Percentage of max width
                        progressFill.setMaxWidth(Double.MAX_VALUE);
                        progressFill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");

                        progressBar.getChildren().addAll(progressBg, progressFill);
                        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);

                        // Percentage label
                        Label pctLabel = new Label(String.format("%.0f%% des offres", totalPercentage));
                        pctLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 500;");

                        cityCard.getChildren().addAll(topRow, progressBar, pctLabel);
                        filialesListContainer.getChildren().add(cityCard);
                        
                        colorIdx++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur villes: " + e.getMessage());
        }
    }

    private void refreshLogo() {
        if (ivLogo == null) return;
        String v = entreprise != null && entreprise.getLogo() != null ? entreprise.getLogo().trim() : "";
        if (v.isBlank()) {
            ivLogo.setImage(null);
            return;
        }
        try {
            Image img;
            if (v.startsWith("http://") || v.startsWith("https://") || v.startsWith("file:")) {
                img = new Image(v, true);
            } else {
                img = new Image(new File(v).toURI().toString(), true);
            }
            ivLogo.setImage(img);
        } catch (Exception ex) {
            ivLogo.setImage(null);
        }
    }

    @FXML
    private void showVuesDetails() {
        try {
            // Rafraîchir les statistiques avant d'afficher les détails
            loadStatistics();
            
            List<OffreVueDetails> details = offreService.getDetailsVuesParOffre(entreprise.getIdEntreprise());
            
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Détails des Vues");
            dialog.setHeaderText("Offres et leurs vues par utilisateur");
            
            // Create content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            content.setPrefWidth(600);
            content.setPrefHeight(400);
            
            if (details.isEmpty()) {
                Label emptyLabel = new Label("Aucune vue enregistrée.");
                emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
                content.getChildren().add(emptyLabel);
            } else {
                // Group by offer
                Map<String, List<OffreVueDetails>> byOffre = details.stream()
                    .collect(Collectors.groupingBy(OffreVueDetails::getOffreTitre));
                
                // Couleurs pour les badges
                String[] badgeColors = {"#6366f1", "#8b5cf6", "#ec4899", "#f43f5e", "#10b981", "#3b82f6"};
                int colorIndex = 0;
                
                for (Map.Entry<String, List<OffreVueDetails>> entry : byOffre.entrySet()) {
                    VBox offreBox = new VBox(8);
                    offreBox.setStyle("-fx-background-color: white; " +
                                      "-fx-padding: 20; " +
                                      "-fx-background-radius: 12; " +
                                      "-fx-border-color: #e2e8f0; " +
                                      "-fx-border-radius: 12;");
                    
                    // Header avec titre et badge
                    HBox header = new HBox(15);
                    header.setAlignment(Pos.CENTER_LEFT);
                    
                    Label titreLabel = new Label(entry.getKey());
                    titreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1e293b;");
                    HBox.setHgrow(titreLabel, Priority.ALWAYS);
                    
                    int viewCount = entry.getValue().size();
                    Label countBadge = new Label(viewCount + " vue" + (viewCount > 1 ? "s" : ""));
                    String badgeColor = badgeColors[colorIndex % badgeColors.length];
                    countBadge.setStyle("-fx-background-color: " + badgeColor + "; " +
                                        "-fx-text-fill: white; " +
                                        "-fx-font-size: 12px; " +
                                        "-fx-font-weight: 700; " +
                                        "-fx-padding: 4 12 4 12; " +
                                        "-fx-background-radius: 20px;");
                    
                    header.getChildren().addAll(titreLabel, countBadge);
                    offreBox.getChildren().add(header);
                    
                    // Separator
                    Region separator = new Region();
                    separator.setMinHeight(1);
                    separator.setStyle("-fx-background-color: #f1f5f9;");
                    separator.setMaxWidth(Double.MAX_VALUE);
                    offreBox.getChildren().add(separator);
                    
                    // Add user details
                    VBox usersBox = new VBox(6);
                    for (OffreVueDetails detail : entry.getValue()) {
                        HBox userBox = new HBox(12);
                        userBox.setAlignment(Pos.CENTER_LEFT);
                        userBox.setStyle("-fx-padding: 5 0 5 5;");
                        
                        Label iconLabel = new Label("👤");
                        iconLabel.setStyle("-fx-font-size: 14px; -fx-opacity: 0.7;");
                        
                        Label userLabel = new Label(detail.getUserNom());
                        userLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13; -fx-font-weight: 500;");
                        
                        Label dateLabel = new Label("• " + detail.getDateVue().substring(0, 19));
                        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12;");
                        
                        userBox.getChildren().addAll(iconLabel, userLabel, dateLabel);
                        usersBox.getChildren().add(userBox);
                    }
                    offreBox.getChildren().add(usersBox);
                    
                    content.getChildren().add(offreBox);
                    colorIndex++;
                }
            }
            
            ScrollPane scrollPane = new ScrollPane(content);
            scrollPane.setFitToWidth(true);
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des détails: " + e.getMessage()).show();
        }
    }

    @FXML
    private void edit(ActionEvent e) {
        redirectToAjouterEntreprise();
    }

    @FXML
    private void delete(ActionEvent e) {
        if (Session.getCurrentUser() == null) {
            new Alert(Alert.AlertType.ERROR, "Utilisateur non connecté.").show();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer votre entreprise supprimera aussi toutes les offres liées. Continuer ?");

        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            entrepriseService.deleteByUserIdCascade(Session.getCurrentUser().getId());
            // Redirection complète vers le formulaire d'entreprise (pas dans la zone interne)
            redirectToEntrepriseFullScreen();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
        }
    }

    private void redirectToEntrepriseFullScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Entreprise/AjouterEntreprise.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) btnDelete.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);

            // Afficher un message informatif
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Entreprise supprimée");
            info.setHeaderText(null);
            info.setContentText("Votre entreprise a été supprimée. Vous devez créer une nouvelle entreprise pour continuer.");
            info.show();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de redirection vers le formulaire entreprise.").show();
        }
    }

    private void redirectToAjouterEntreprise() {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/Entreprise/AjouterEntreprise.fxml"));
            StackPane host = findAdminContentArea();
            if (host == null) {
                new Alert(Alert.AlertType.ERROR, "Zone d'affichage introuvable.").show();
                return;
            }
            host.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur de navigation.").show();
        }
    }

    private StackPane findAdminContentArea() {
        if (btnEdit != null && btnEdit.getScene() != null) {
            var n = btnEdit.getScene().lookup("#adminContentArea");
            if (n instanceof StackPane sp) {
                return sp;
            }
        }
        if (btnDelete != null && btnDelete.getScene() != null) {
            var n = btnDelete.getScene().lookup("#adminContentArea");
            if (n instanceof StackPane sp) {
                return sp;
            }
        }
        return null;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
