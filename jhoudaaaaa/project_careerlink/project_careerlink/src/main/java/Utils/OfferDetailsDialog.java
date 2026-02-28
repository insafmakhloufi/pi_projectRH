package Utils;

import Entities.Offre.OffreEmploi;
import Services.Ai.TranslationService;
import Services.Offre.OfrreEmploiServices;
import Utils.Session;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.net.URL;

public final class OfferDetailsDialog {

    private OfferDetailsDialog() {
    }

    public static void show(Window owner, OffreEmploi offre, String stylesheetResourcePath) {
        show(owner, offre, stylesheetResourcePath, true);
    }

    public static void show(Window owner, OffreEmploi offre, String stylesheetResourcePath, boolean countView) {
        if (offre == null) {
            return;
        }

        OffreEmploi displayOffre = offre;
        try {
            OfrreEmploiServices service = new OfrreEmploiServices();
            OffreEmploi full = service.getOffreById(offre.getId());
            if (full != null) {
                // Injecter seulement les champs textuels manquants (car la version light ne les contient pas)
                if (isBlank(displayOffre.getNiveauExperience())) {
                    displayOffre.setNiveauExperience(full.getNiveauExperience());
                }
                if (isBlank(displayOffre.getNiveauEtudes())) {
                    displayOffre.setNiveauEtudes(full.getNiveauEtudes());
                }
                if (isBlank(displayOffre.getLanguesRequises())) {
                    displayOffre.setLanguesRequises(full.getLanguesRequises());
                }
                if (displayOffre.getNombrePoste() == null) {
                    displayOffre.setNombrePoste(full.getNombrePoste());
                }
                if (displayOffre.getDateLimiteCandidature() == null) {
                    displayOffre.setDateLimiteCandidature(full.getDateLimiteCandidature());
                }
                if (displayOffre.getTeletravail() == null) {
                    displayOffre.setTeletravail(full.getTeletravail());
                }
                if (displayOffre.getIdEntreprise() == null) {
                    displayOffre.setIdEntreprise(full.getIdEntreprise());
                }
            }
        } catch (Exception ignored) {
        }

        if (countView) {
            incrementerVues(displayOffre.getId());
        }
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'offre");
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("offer-details-dialog");
        pane.setPadding(new Insets(0));
        pane.getButtonTypes().add(ButtonType.CLOSE);

        if (stylesheetResourcePath != null && !stylesheetResourcePath.isBlank()) {
            URL css = OfferDetailsDialog.class.getResource(stylesheetResourcePath);
            if (css == null && stylesheetResourcePath.startsWith("/Offre/")) {
                css = OfferDetailsDialog.class.getResource(stylesheetResourcePath.replaceFirst("^/Offre/", "/"));
            }
            if (css != null) {
                pane.getStylesheets().add(css.toExternalForm());
            }
        }

        VBox root = new VBox(0);
        root.getStyleClass().add("offer-details-root");

        HBox header = new HBox(12);
        header.getStyleClass().add("offer-details-header");

        VBox headerText = new VBox(4);
        headerText.getStyleClass().add("offer-details-header-text");

        final Label[] titleLabel = new Label[1];
        final Label[] descValueLabel = new Label[1];
        final TranslationService translationService = new TranslationService();

        Label title = new Label(safe(displayOffre != null ? displayOffre.getTitre() : null, "Sans titre"));
        title.getStyleClass().add("offer-details-title");
        title.setWrapText(true);
        titleLabel[0] = title;

        Label subtitle = new Label("Détails de l'offre");
        subtitle.getStyleClass().add("offer-details-subtitle");

        headerText.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label status = new Label(safe(displayOffre != null ? displayOffre.getStatus() : null, "Non défini"));
        status.getStyleClass().add("offer-details-status");

        header.getChildren().addAll(headerText, spacer, status);

        VBox body = new VBox(12);
        body.getStyleClass().add("offer-details-body");

        // Barre de traduction - placée directement dans le body en haut
        HBox translationBar = new HBox(10);
        translationBar.setAlignment(javafx.geometry.Pos.CENTER);
        translationBar.setPadding(new Insets(10, 15, 10, 15));
        translationBar.setStyle("-fx-background-color: #dbeafe; -fx-border-color: #2563eb; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label tradLabel = new Label("🌐 Traduire cette offre:");
        tradLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e40af; -fx-font-weight: bold;");
        
        Button btnFr = new Button("🇫🇷 FR");
        Button btnEn = new Button("🇬🇧 EN");
        Button btnAr = new Button("🇸🇦 AR");
        
        String btnStyle = "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-weight: bold; -fx-padding: 6 15; -fx-min-width: 60; -fx-cursor: hand;";
        btnFr.setStyle(String.format(btnStyle, "#2563eb", "white"));
        btnEn.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
        btnAr.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
        
        btnFr.setOnAction(e -> {
            traduireOffre(displayOffre, "fr", translationService, titleLabel[0], descValueLabel[0], owner);
            btnFr.setStyle(String.format(btnStyle, "#2563eb", "white"));
            btnEn.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
            btnAr.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
        });
        btnEn.setOnAction(e -> {
            traduireOffre(displayOffre, "en", translationService, titleLabel[0], descValueLabel[0], owner);
            btnFr.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
            btnEn.setStyle(String.format(btnStyle, "#2563eb", "white"));
            btnAr.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
        });
        btnAr.setOnAction(e -> {
            traduireOffre(displayOffre, "ar", translationService, titleLabel[0], descValueLabel[0], owner);
            btnFr.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
            btnEn.setStyle(String.format(btnStyle, "#ffffff", "#2563eb") + " -fx-border-color: #2563eb; -fx-border-width: 2;");
            btnAr.setStyle(String.format(btnStyle, "#2563eb", "white"));
        });
        
        Region barSpacer = new Region();
        HBox.setHgrow(barSpacer, Priority.ALWAYS);
        
        translationBar.getChildren().addAll(tradLabel, barSpacer, btnFr, btnEn, btnAr);

        VBox rows = new VBox(10);
        rows.getStyleClass().add("offer-details-rows");

        rows.getChildren().add(createRow("Description", safe(displayOffre != null ? displayOffre.getDescription() : null, ""), descValueLabel));
        rows.getChildren().add(createRow("Niveau d'expérience", safe(displayOffre != null ? displayOffre.getNiveauExperience() : null, "Non spécifié"), null));
        rows.getChildren().add(createRow("Niveau d'études", safe(displayOffre != null ? displayOffre.getNiveauEtudes() : null, "Non spécifié"), null));
        rows.getChildren().add(createRow("Langues requises", safe(displayOffre != null ? displayOffre.getLanguesRequises() : null, "Non spécifié"), null));
        rows.getChildren().add(createRow("Nombre de postes", displayOffre != null && displayOffre.getNombrePoste() != null ? String.valueOf(displayOffre.getNombrePoste()) : "-", null));
        rows.getChildren().add(createRow("Date limite", displayOffre != null && displayOffre.getDateLimiteCandidature() != null ? String.valueOf(displayOffre.getDateLimiteCandidature()) : "-", null));
        rows.getChildren().add(createRow("Télétravail", displayOffre != null && Boolean.TRUE.equals(displayOffre.getTeletravail()) ? "Oui" : "Non", null));
        rows.getChildren().add(createRow("Localisation", safeLocation(displayOffre), null));
        rows.getChildren().add(createRow("Type de contrat", safeNom(displayOffre != null ? displayOffre.getTypeContrat() : null, "Non spécifié"), null));
        rows.getChildren().add(createRow("Type d'emploi", safeNom(displayOffre != null ? displayOffre.getTypeEmploi() : null, "Non spécifié"), null));
        rows.getChildren().add(createRow("Secteur", safeNom(displayOffre != null ? displayOffre.getSecteur() : null, "Non spécifié"), null));

        ScrollPane sp = new ScrollPane(rows);
        sp.getStyleClass().add("offer-details-scroll");
        sp.setFitToWidth(true);
        sp.setPrefViewportHeight(320);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(sp, Priority.ALWAYS);

        // Bouton Postuler (fonctionnalité à intégrer avec le module candidat)
        Button postulerBtn = new Button("Postuler à cette offre");
        postulerBtn.getStyleClass().addAll("btn-primary", "offer-postuler-btn");
        postulerBtn.setMaxWidth(Double.MAX_VALUE);
        postulerBtn.setOnAction(e -> {
            postuler(displayOffre);
            e.consume();
        });
        
        VBox postulerBox = new VBox(8, postulerBtn);
        postulerBox.setPadding(new Insets(10, 0, 0, 0));

        body.getChildren().addAll(translationBar, sp, postulerBox);
        root.getChildren().addAll(header, body);

        pane.setContent(root);
        
        pane.setPrefWidth(620);
        pane.setPrefHeight(520);
        pane.setMinHeight(Region.USE_PREF_SIZE);
        dialog.setResizable(true);

        Node closeBtn = pane.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.getStyleClass().addAll("btn-secondary");
        }

        dialog.showAndWait();
    }
    
    private static void incrementerVues(int offreId) {
        try {
            OfrreEmploiServices service = new OfrreEmploiServices();
            service.incrementerVues(offreId);
        } catch (Exception e) {
            System.err.println("[ERREUR] incrementerVues a échoué pour offre ID " + offreId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void postuler(OffreEmploi offre) {
        if (Session.getCurrentUser() == null) {
            new Alert(Alert.AlertType.WARNING, "Veuillez vous connecter pour postuler.").showAndWait();
            return;
        }
        
        // Fonctionnalité à intégrer avec le module candidat
        new Alert(Alert.AlertType.INFORMATION, 
            "La fonctionnalité de candidature sera intégrée avec le module candidat.\n\n" +
            "Offre: " + offre.getTitre()).showAndWait();
    }

    private static HBox createRow(String key, String value, Label[] valueLabelRef) {
        HBox row = new HBox(12);
        row.getStyleClass().add("offer-details-row");

        Label k = new Label(key);
        k.getStyleClass().add("offer-details-key");
        k.setMinWidth(150);

        Label v = new Label(value != null ? value : "");
        v.getStyleClass().add("offer-details-value");
        v.setWrapText(true);
        HBox.setHgrow(v, Priority.ALWAYS);
        
        // Stocker référence si demandé (pour mise à jour traduction)
        if (valueLabelRef != null && key.equals("Description")) {
            valueLabelRef[0] = v;
        }

        row.getChildren().addAll(k, v);
        return row;
    }
    
    private static void traduireOffre(OffreEmploi offre, String langueCible, 
                                     TranslationService service, Label titleLabel, 
                                     Label descLabel, Window owner) {
        if (offre == null) return;
        
        // Détecter langue source
        String langueSource = service.detecterLangue(offre.getTitre() + " " + offre.getDescription());
        if (langueSource.equals(langueCible)) {
            return; // Déjà dans cette langue
        }
        
        // Afficher loading
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(20, 20);
        
        Task<TranslationService.OffreTraduite> task = new Task<>() {
            @Override
            protected TranslationService.OffreTraduite call() {
                return service.traduireOffre(
                    offre.getTitre(), 
                    offre.getDescription(), 
                    langueSource, 
                    langueCible, 
                    "tunisia"
                );
            }
        };
        
        task.setOnSucceeded(e -> {
            TranslationService.OffreTraduite traduction = task.getValue();
            if (traduction != null && traduction.isSucces()) {
                Platform.runLater(() -> {
                    titleLabel.setText(traduction.getTitre());
                    if (descLabel != null) {
                        descLabel.setText(traduction.getDescription());
                    }
                });
            } else {
                new Alert(Alert.AlertType.WARNING, "La traduction a échoué. Veuillez réessayer.").show();
            }
        });
        
        task.setOnFailed(e -> {
            new Alert(Alert.AlertType.ERROR, "Erreur de traduction: " + task.getException().getMessage()).show();
        });
        
        new Thread(task).start();
    }

    private static String safe(String s, String fallback) {
        if (s == null) {
            return fallback;
        }
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static String safeLocation(OffreEmploi offre) {
        if (offre == null || offre.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String adresse = offre.getLocalisation().getAdresse();
        String a = adresse != null ? adresse.trim() : "";
        if (!a.isEmpty()) {
            return a;
        }
        String ville = offre.getLocalisation().getVille();
        String pays = offre.getLocalisation().getPays();
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

    private static String safeNom(Object entity, String fallback) {
        if (entity == null) {
            return fallback;
        }
        try {
            Object r = entity.getClass().getMethod("getNom").invoke(entity);
            return safe(r != null ? String.valueOf(r) : null, fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
