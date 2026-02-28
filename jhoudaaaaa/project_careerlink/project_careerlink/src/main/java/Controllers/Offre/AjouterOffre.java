package Controllers.Offre;

import Entities.Offre.OffreEmploi;
import Entities.Offre.Localisation;
import Entities.Offre.SecteurActivite;
import Entities.Offre.TypeContrat;
import Entities.Offre.TypeEmploi;
import Entities.Entreprise.Entreprise;
import Services.Entreprise.EntrepriseService;
import Services.Offre.OfrreEmploiServices;
import Services.Offre.LocalisationService;
import Services.Offre.SecteurActiviteService;
import Services.Offre.TypeContratService;
import Services.Offre.TypeEmploiService;
import Services.Ai.GeminiAiService;
import Services.Ai.OpenAiService;
import Utils.Session;
import Utils.OsmLocationPickerDialog;
import javafx.application.Platform;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

public class AjouterOffre implements Initializable {

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageSubtitleLabel;

    @FXML
    private Button frontOfficeBtn;

    @FXML
    private TextField titreid;

    @FXML
    private TextField descriptionid;

    @FXML
    private ComboBox<String> statusid;

    @FXML
    private ComboBox<TypeContrat> typeContratid;

    @FXML
    private ComboBox<TypeEmploi> typeEmploiid;

    @FXML
    private ComboBox<SecteurActivite> secteurid;

    @FXML
    private ComboBox<Localisation> localisationid;

    @FXML
    private Label coordAddressLabel;

    @FXML
    private Label coordLatLabel;

    @FXML
    private Label coordLonLabel;

    @FXML
    private Button pickMapBtn;

    @FXML
    private Button aiGenerateBtn;

    @FXML
    private Button aiImproveBtn;

    @FXML
    private TextField niveauExperienceField;

    @FXML
    private TextField niveauEtudesField;

    @FXML
    private TextField languesRequisesField;

    @FXML
    private TextField nombrePosteField;

    @FXML
    private DatePicker dateLimitePicker;

    @FXML
    private CheckBox teletravailCheck;

    @FXML
    private Button submitBtn;

    @FXML
    private Button cancelBtn;

    @FXML
    private StackPane dashCenter;

    @FXML
    private Label errTitre;

    @FXML
    private Label errDescription;

    @FXML
    private Label errStatus;

    @FXML
    private Label errTypeContrat;

    @FXML
    private Label errTypeEmploi;

    @FXML
    private Label errSecteur;

    @FXML
    private Label errLocalisation;

    @FXML
    private Label errNiveauExperience;

    @FXML
    private Label errNiveauEtudes;

    @FXML
    private Label errLanguesRequises;

    @FXML
    private Label errNombrePoste;

    @FXML
    private Label errDateLimite;

    @FXML
    private Label errTeletravail;

    @FXML
    private ImageView entrepriseLogo;

    @FXML
    private Label entrepriseNameLabel;

    @FXML
    private Label entrepriseAddressLabel;

    @FXML
    private Label entrepriseCityLabel;

    @FXML
    private Label entreprisePhoneLabel;

    @FXML
    private Label entrepriseEmailLabel;

    private OffreEmploi editingOffre;
    private OffreEmploi originalSnapshot;

    private Integer entrepriseId;
    private String entrepriseName;
    private String entrepriseLogoValue;

    private final OfrreEmploiServices service = new OfrreEmploiServices();
    private final EntrepriseService entrepriseService = new EntrepriseService();
    private final TypeContratService typeContratService = new TypeContratService();
    private final TypeEmploiService typeEmploiService = new TypeEmploiService();
    private final SecteurActiviteService secteurActiviteService = new SecteurActiviteService();
    private final LocalisationService localisationService = new LocalisationService();
    
    private GeminiAiService geminiAiService;
    private OpenAiService openAiService;
    
    private GeminiAiService getGeminiService() {
        if (geminiAiService == null) geminiAiService = new GeminiAiService();
        return geminiAiService;
    }
    
    private OpenAiService getOpenAiService() {
        if (openAiService == null) openAiService = new OpenAiService();
        return openAiService;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadEntrepriseContext();
        if (typeContratid != null) {
            typeContratid.getItems().setAll(typeContratService.getAll());
        }
        if (typeEmploiid != null) {
            typeEmploiid.getItems().setAll(typeEmploiService.getAll());
        }
        if (secteurid != null) {
            secteurid.getItems().setAll(secteurActiviteService.getAll());
        }
        if (localisationid != null) {
            localisationid.getItems().setAll(localisationService.getAll());
        }

        if (localisationid != null) {
            localisationid.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateCoordCard(newV));
            updateCoordCard(localisationid.getSelectionModel().getSelectedItem());
        } else {
            updateCoordCard(null);
        }

        if (statusid != null) {
            statusid.getItems().setAll("active", "non active");
        }

        UnaryOperator<TextFormatter.Change> maxTitre = change -> {
            String newText = change.getControlNewText();
            if (newText == null) {
                return change;
            }
            if (newText.length() > 80) {
                return null;
            }
            // Interdire les chiffres dans le titre
            if (newText.matches(".*\\d.*")) {
                return null;
            }
            return change;
        };
        UnaryOperator<TextFormatter.Change> maxDescription = change -> {
            String newText = change.getControlNewText();
            return newText != null && newText.length() <= 1000 ? change : null;
        };

        UnaryOperator<TextFormatter.Change> maxNiveau = change -> {
            String newText = change.getControlNewText();
            return newText != null && newText.length() <= 50 ? change : null;
        };

        UnaryOperator<TextFormatter.Change> maxEtudes = change -> {
            String newText = change.getControlNewText();
            return newText != null && newText.length() <= 100 ? change : null;
        };

        UnaryOperator<TextFormatter.Change> maxLangues = change -> {
            String newText = change.getControlNewText();
            return newText != null && newText.length() <= 255 ? change : null;
        };

        UnaryOperator<TextFormatter.Change> onlyInt = change -> {
            String newText = change.getControlNewText();
            if (newText == null || newText.isBlank()) {
                return change;
            }
            return newText.matches("\\d{0,3}") ? change : null;
        };

        if (titreid != null) {
            titreid.setTextFormatter(new TextFormatter<>(maxTitre));
        }
        if (descriptionid != null) {
            descriptionid.setTextFormatter(new TextFormatter<>(maxDescription));
        }

        if (niveauExperienceField != null) {
            niveauExperienceField.setTextFormatter(new TextFormatter<>(maxNiveau));
        }
        if (niveauEtudesField != null) {
            niveauEtudesField.setTextFormatter(new TextFormatter<>(maxEtudes));
        }
        if (languesRequisesField != null) {
            languesRequisesField.setTextFormatter(new TextFormatter<>(maxLangues));
        }
        if (nombrePosteField != null) {
            nombrePosteField.setTextFormatter(new TextFormatter<>(onlyInt));
        }

        if (titreid != null) {
            titreid.textProperty().addListener((obs, oldVal, newVal) -> clearError(titreid, errTitre));
        }
        if (descriptionid != null) {
            descriptionid.textProperty().addListener((obs, oldVal, newVal) -> clearError(descriptionid, errDescription));
        }
        if (statusid != null) {
            statusid.valueProperty().addListener((obs, oldVal, newVal) -> clearError(statusid, errStatus));
        }
        if (typeContratid != null) {
            typeContratid.valueProperty().addListener((obs, oldVal, newVal) -> clearError(typeContratid, errTypeContrat));
        }
        if (typeEmploiid != null) {
            typeEmploiid.valueProperty().addListener((obs, oldVal, newVal) -> clearError(typeEmploiid, errTypeEmploi));
        }
        if (secteurid != null) {
            secteurid.valueProperty().addListener((obs, oldVal, newVal) -> clearError(secteurid, errSecteur));
        }
        if (localisationid != null) {
            localisationid.valueProperty().addListener((obs, oldVal, newVal) -> clearError(localisationid, errLocalisation));
        }

        if (niveauExperienceField != null) {
            niveauExperienceField.textProperty().addListener((obs, oldVal, newVal) -> clearError(niveauExperienceField, errNiveauExperience));
        }
        if (niveauEtudesField != null) {
            niveauEtudesField.textProperty().addListener((obs, oldVal, newVal) -> clearError(niveauEtudesField, errNiveauEtudes));
        }
        if (languesRequisesField != null) {
            languesRequisesField.textProperty().addListener((obs, oldVal, newVal) -> clearError(languesRequisesField, errLanguesRequises));
        }
        if (nombrePosteField != null) {
            nombrePosteField.textProperty().addListener((obs, oldVal, newVal) -> clearError(nombrePosteField, errNombrePoste));
        }
        if (dateLimitePicker != null) {
            dateLimitePicker.valueProperty().addListener((obs, oldVal, newVal) -> clearError(dateLimitePicker, errDateLimite));
        }
        if (teletravailCheck != null) {
            teletravailCheck.selectedProperty().addListener((obs, oldVal, newVal) -> clearError(teletravailCheck, errTeletravail));
        }

        applyEditingIfNeeded();

        playEntryAnimation(dashCenter);
        Platform.runLater(() -> {
            if (dashCenter != null) {
                dashCenter.lookupAll(".button").forEach(this::installHoverAnimation);
            }
        });
    }

    public void setEditingOffre(OffreEmploi offre) {
        this.editingOffre = offre;
        this.originalSnapshot = cloneOffre(offre);
        applyEditingIfNeeded();
    }

    private void loadEntrepriseContext() {
        try {
            if (Session.getCurrentUser() == null) return;
            Entreprise e = entrepriseService.getByUserId(Session.getCurrentUser().getId());
            if (e == null) return;
            entrepriseId = e.getIdEntreprise();
            entrepriseName = e.getNomEntreprise();
            entrepriseLogoValue = e.getLogo();

            if (entrepriseAddressLabel != null) {
                String a = e.getAdresse() != null ? e.getAdresse().trim() : "";
                entrepriseAddressLabel.setText(a.isEmpty() ? "—" : a);
            }
            if (entrepriseCityLabel != null) {
                String v = e.getVille() != null ? e.getVille().trim() : "";
                entrepriseCityLabel.setText(v.isEmpty() ? "Ville: —" : "Ville: " + v);
            }
            if (entreprisePhoneLabel != null) {
                String t = e.getTelephone() != null ? e.getTelephone().trim() : "";
                entreprisePhoneLabel.setText(t.isEmpty() ? "Tél: —" : "Tél: " + t);
            }
            if (entrepriseEmailLabel != null) {
                String m = e.getEmailContact() != null ? e.getEmailContact().trim() : "";
                entrepriseEmailLabel.setText(m.isEmpty() ? "Email: —" : "Email: " + m);
            }

            if (entrepriseNameLabel != null && entrepriseName != null && !entrepriseName.isBlank()) {
                entrepriseNameLabel.setText(entrepriseName);
            }
            refreshEntrepriseLogo();
        } catch (Exception ignored) {
        }
    }

    @FXML
    void generateOfferWithAi(ActionEvent event) {
        System.out.println("[DEBUG] generateOfferWithAi clicked");
        
        String poste = titreid != null ? titreid.getText() : null;
        String niveau = niveauExperienceField != null ? niveauExperienceField.getText() : null;
        
        System.out.println("[DEBUG] poste=" + poste + ", niveau=" + niveau);
        
        String ville;
        if (localisationid != null && localisationid.getValue() != null) {
            Localisation l = localisationid.getValue();
            String a = l.getAdresse() != null ? l.getAdresse().trim() : "";
            if (!a.isEmpty()) {
                ville = a;
            } else {
                String v = l.getVille();
                String p = l.getPays();
                String vv = v != null ? v.trim() : "";
                String pp = p != null ? p.trim() : "";
                ville = (!vv.isEmpty() && !pp.isEmpty()) ? vv + ", " + pp : (!vv.isEmpty() ? vv : pp);
            }
        } else {
            ville = "";
        }
        
        System.out.println("[DEBUG] ville=" + ville);
        System.out.println("[DEBUG] openAiService=" + getOpenAiService());
        System.out.println("[DEBUG] geminiAiService=" + getGeminiService());

        setAiButtonsDisabled(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                System.out.println("[DEBUG] Task started");
                // Essayer OpenAI d'abord
                try {
                    System.out.println("[DEBUG] Calling OpenAI...");
                    String result = getOpenAiService().generateOfferText(poste, niveau, ville);
                    System.out.println("[DEBUG] OpenAI success, result length=" + (result != null ? result.length() : 0));
                    return result;
                } catch (RuntimeException ex) {
                    System.err.println("[AI] OpenAI failed: " + ex.getMessage());
                    ex.printStackTrace();
                    // Si c'est une erreur de clé/manque de crédits/quota, on fait fallback sur Gemini
                    String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                    if (msg.contains("api_key") || msg.contains("authentication") || msg.contains("401") || msg.contains("403")) {
                        // Clé invalide - essayer Gemini quand même
                        System.out.println("[DEBUG] Clé OpenAI invalide, fallback sur Gemini...");
                    } else if (msg.contains("429") || msg.contains("quota") || msg.contains("insufficient_quota")) {
                        System.out.println("[DEBUG] OpenAI quota dépassé (429), fallback sur Gemini...");
                    } else {
                        System.out.println("[DEBUG] OpenAI erreur, fallback sur Gemini...");
                    }
                    try {
                        return getGeminiService().generateOfferText(poste, niveau, ville);
                    } catch (RuntimeException geminiEx) {
                        System.err.println("[AI] Gemini aussi failed: " + geminiEx.getMessage());
                        // Si Gemini échoue aussi, on retourne un message local
                        throw new RuntimeException("Les services IA sont temporairement indisponibles (quota dépassé). " +
                                "Veuillez réessayer plus tard ou saisir manuellement la description.");
                    }
                }
            }
        };

        task.setOnSucceeded(e -> {
            System.out.println("[DEBUG] Task succeeded");
            setAiButtonsDisabled(false);
            String txt = task.getValue();
            System.out.println("[DEBUG] Got text, length=" + (txt != null ? txt.length() : 0));
            if (descriptionid != null && txt != null && !txt.isBlank()) {
                // Tronquer à 1000 caractères (limite du TextFormatter)
                final String truncatedTxt = txt.length() > 1000 ? txt.substring(0, 997) + "..." : txt;
                // Forcer la mise à jour sur le thread JavaFX
                javafx.application.Platform.runLater(() -> {
                    descriptionid.setText(truncatedTxt.trim());
                    System.out.println("[DEBUG] Text set to description field (truncated to " + truncatedTxt.length() + "): " + truncatedTxt.substring(0, Math.min(50, truncatedTxt.length())) + "...");
                    // Forcer le refresh UI
                    descriptionid.requestFocus();
                    descriptionid.positionCaret(truncatedTxt.length());
                    descriptionid.layout();
                    if (descriptionid.getParent() != null) {
                        descriptionid.getParent().layout();
                        if (descriptionid.getParent().getParent() != null) {
                            descriptionid.getParent().getParent().layout();
                        }
                    }
                    // Scroller pour voir le champ description
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                    pause.setOnFinished(evt -> {
                        descriptionid.requestFocus();
                        // Scroller vers le bas pour voir la description
                        if (dashCenter != null) {
                            javafx.scene.control.ScrollPane scrollPane = (javafx.scene.control.ScrollPane) dashCenter.lookup(".scroll-pane");
                            if (scrollPane != null) {
                                scrollPane.setVvalue(0.8); // Scroller vers le bas
                            }
                        }
                    });
                    pause.play();
                });
            } else {
                System.out.println("[DEBUG] Cannot set text: descriptionid=" + descriptionid + ", txt=" + (txt != null ? txt.substring(0, Math.min(50, txt.length())) : "null"));
            }
        });

        task.setOnFailed(e -> {
            System.err.println("[DEBUG] Task failed");
            setAiButtonsDisabled(false);
            Throwable ex = task.getException();
            System.err.println("[DEBUG] Exception: " + (ex != null ? ex.getMessage() : "null"));
            if (ex != null) ex.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur IA");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors de la génération");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "ai-generate");
        th.setDaemon(true);
        th.start();
        System.out.println("[DEBUG] Thread started");
    }

    @FXML
    void improveTextWithAi(ActionEvent event) {
        String raw = descriptionid != null ? descriptionid.getText() : null;
        if (raw == null || raw.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("IA");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez saisir une description avant d'améliorer le texte.");
            alert.showAndWait();
            return;
        }

        setAiButtonsDisabled(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    return getOpenAiService().improveText(raw);
                } catch (RuntimeException ex) {
                    System.err.println("[AI] OpenAI failed: " + ex.getMessage());
                    String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
                    if (msg.contains("api_key") || msg.contains("authentication") || msg.contains("401") || msg.contains("403")) {
                        System.out.println("[DEBUG] Clé OpenAI invalide, fallback sur Gemini...");
                    } else if (msg.contains("429") || msg.contains("quota") || msg.contains("insufficient_quota")) {
                        System.out.println("[DEBUG] OpenAI quota dépassé (429), fallback sur Gemini...");
                    } else {
                        System.out.println("[DEBUG] OpenAI erreur, fallback sur Gemini...");
                    }
                    try {
                        return getGeminiService().improveText(raw);
                    } catch (RuntimeException geminiEx) {
                        System.err.println("[AI] Gemini aussi failed: " + geminiEx.getMessage());
                        throw new RuntimeException("Les services IA sont temporairement indisponibles (quota dépassé). " +
                                "Veuillez réessayer plus tard ou saisir manuellement la description.");
                    }
                }
            }
        };

        task.setOnSucceeded(e -> {
            setAiButtonsDisabled(false);
            String txt = task.getValue();
            if (descriptionid != null && txt != null && !txt.isBlank()) {
                descriptionid.setText(txt.trim());
            }
        });

        task.setOnFailed(e -> {
            setAiButtonsDisabled(false);
            Throwable ex = task.getException();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur IA");
            alert.setHeaderText(null);
            alert.setContentText(ex != null ? ex.getMessage() : "Erreur lors de l'amélioration");
            alert.showAndWait();
        });

        Thread th = new Thread(task, "gemini-improve");
        th.setDaemon(true);
        th.start();
    }

    private void setAiButtonsDisabled(boolean disabled) {
        if (aiGenerateBtn != null) {
            aiGenerateBtn.setDisable(disabled);
        }
        if (aiImproveBtn != null) {
            aiImproveBtn.setDisable(disabled);
        }
    }

    @FXML
    void pickLocationOnMap(ActionEvent event) {
        try {
            Stage owner = null;
            if (dashCenter != null && dashCenter.getScene() != null && dashCenter.getScene().getWindow() instanceof Stage st) {
                owner = st;
            }
            OsmLocationPickerDialog.LocationResult res = OsmLocationPickerDialog.showAndWait(owner);
            if (res == null) {
                return;
            }

            Localisation loc = localisationService.findOrCreateByCoordinates(
                    res.getAdresse(),
                    res.getVille(),
                    res.getPays(),
                    res.getLatitude(),
                    res.getLongitude()
            );
            if (loc == null) {
                return;
            }

            if (localisationid != null) {
                localisationid.getItems().setAll(localisationService.getAll());
                Localisation target = localisationid.getItems().stream()
                        .filter(l -> l != null && l.getId() == loc.getId())
                        .findFirst().orElse(loc);
                localisationid.getSelectionModel().select(target);
                clearError(localisationid, errLocalisation);
                updateCoordCard(target);
            } else {
                updateCoordCard(loc);
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void updateCoordCard(Localisation loc) {
        if (coordAddressLabel == null && coordLatLabel == null && coordLonLabel == null) {
            return;
        }

        if (loc == null) {
            if (coordAddressLabel != null) coordAddressLabel.setText("Sélectionnez une localisation…");
            if (coordLatLabel != null) coordLatLabel.setText("Lat: —");
            if (coordLonLabel != null) coordLonLabel.setText("Lon: —");
            return;
        }

        String adresse = loc.getAdresse();
        if (adresse == null || adresse.isBlank()) {
            String v = loc.getVille() != null ? loc.getVille().trim() : "";
            String p = loc.getPays() != null ? loc.getPays().trim() : "";
            if (!v.isEmpty() && !p.isEmpty()) {
                adresse = v + ", " + p;
            } else if (!v.isEmpty()) {
                adresse = v;
            } else {
                adresse = p;
            }
        }
        if (coordAddressLabel != null) coordAddressLabel.setText(adresse == null || adresse.isBlank() ? "Localisation" : adresse);

        if (coordLatLabel != null) {
            Double lat = loc.getLatitude();
            coordLatLabel.setText(lat == null ? "Lat: —" : String.format(java.util.Locale.US, "Lat: %.5f", lat));
        }
        if (coordLonLabel != null) {
            Double lon = loc.getLongitude();
            coordLonLabel.setText(lon == null ? "Lon: —" : String.format(java.util.Locale.US, "Lon: %.5f", lon));
        }
    }

    private void refreshEntrepriseLogo() {
        if (entrepriseLogo == null) {
            return;
        }
        String v = entrepriseLogoValue != null ? entrepriseLogoValue.trim() : "";
        if (v.isBlank()) {
            entrepriseLogo.setImage(null);
            return;
        }
        try {
            Image img;
            if (v.startsWith("http://") || v.startsWith("https://")) {
                img = new Image(v, true);
            } else {
                img = new Image(new File(v).toURI().toString(), true);
            }
            entrepriseLogo.setImage(img);
        } catch (Exception ex) {
            entrepriseLogo.setImage(null);
        }
    }

    private OffreEmploi cloneOffre(OffreEmploi src) {
        if (src == null) {
            return null;
        }
        OffreEmploi copy = new OffreEmploi(src.getId(), src.getTitre(), src.getDescription(), src.getStatus());
        copy.setNiveauExperience(src.getNiveauExperience());
        copy.setNiveauEtudes(src.getNiveauEtudes());
        copy.setLanguesRequises(src.getLanguesRequises());
        copy.setNombrePoste(src.getNombrePoste());
        copy.setDateLimiteCandidature(src.getDateLimiteCandidature());
        copy.setTeletravail(src.getTeletravail());
        copy.setIdEntreprise(src.getIdEntreprise());
        copy.setTypeContrat(src.getTypeContrat());
        copy.setTypeEmploi(src.getTypeEmploi());
        copy.setSecteur(src.getSecteur());
        copy.setLocalisation(src.getLocalisation());
        return copy;
    }

    private void applyEditingIfNeeded() {
        if (editingOffre == null) {
            if (pageTitleLabel != null) {
                pageTitleLabel.setText("Créer une offre");
            }
            if (pageSubtitleLabel != null) {
                String base = "Renseigne les détails de l'offre";
                if (entrepriseName != null && !entrepriseName.isBlank()) {
                    base = base + " · " + entrepriseName;
                }
                pageSubtitleLabel.setText(base);
            }
            if (submitBtn != null) {
                submitBtn.setText("➕ Ajouter");
            }
            if (frontOfficeBtn != null) {
                frontOfficeBtn.setVisible(true);
                frontOfficeBtn.setManaged(true);
            }
            if (cancelBtn != null) {
                cancelBtn.setVisible(false);
                cancelBtn.setManaged(false);
            }
            return;
        }

        if (pageTitleLabel != null) {
            pageTitleLabel.setText("Modifier une offre");
        }
        if (pageSubtitleLabel != null) {
            pageSubtitleLabel.setText("Modifie les détails de l'offre");
        }
        if (submitBtn != null) {
            submitBtn.setText("💾 Enregistrer");
        }
        if (frontOfficeBtn != null) {
            frontOfficeBtn.setVisible(false);
            frontOfficeBtn.setManaged(false);
        }
        if (cancelBtn != null) {
            cancelBtn.setVisible(true);
            cancelBtn.setManaged(true);
        }
        if (titreid != null) {
            titreid.setText(editingOffre.getTitre());
        }
        if (descriptionid != null) {
            descriptionid.setText(editingOffre.getDescription());
        }
        if (statusid != null) {
            statusid.getSelectionModel().select(editingOffre.getStatus());
        }

        if (niveauExperienceField != null) {
            niveauExperienceField.setText(editingOffre.getNiveauExperience());
        }
        if (niveauEtudesField != null) {
            niveauEtudesField.setText(editingOffre.getNiveauEtudes());
        }
        if (languesRequisesField != null) {
            languesRequisesField.setText(editingOffre.getLanguesRequises());
        }
        if (nombrePosteField != null) {
            Integer np = editingOffre.getNombrePoste();
            nombrePosteField.setText(np != null ? String.valueOf(np) : "");
        }
        if (dateLimitePicker != null) {
            dateLimitePicker.setValue(editingOffre.getDateLimiteCandidature());
        }
        if (teletravailCheck != null) {
            Boolean t = editingOffre.getTeletravail();
            teletravailCheck.setSelected(t != null && t);
        }

        if (typeContratid != null && editingOffre.getTypeContrat() != null) {
            TypeContrat target = typeContratid.getItems().stream()
                    .filter(tc -> tc != null && tc.getId() == editingOffre.getTypeContrat().getId())
                    .findFirst().orElse(null);
            typeContratid.getSelectionModel().select(target);
        }
        if (typeEmploiid != null && editingOffre.getTypeEmploi() != null) {
            TypeEmploi target = typeEmploiid.getItems().stream()
                    .filter(te -> te != null && te.getId() == editingOffre.getTypeEmploi().getId())
                    .findFirst().orElse(null);
            typeEmploiid.getSelectionModel().select(target);
        }
        if (secteurid != null && editingOffre.getSecteur() != null) {
            SecteurActivite target = secteurid.getItems().stream()
                    .filter(s -> s != null && s.getId() == editingOffre.getSecteur().getId())
                    .findFirst().orElse(null);
            secteurid.getSelectionModel().select(target);
        }
        if (localisationid != null && editingOffre.getLocalisation() != null) {
            Localisation target = localisationid.getItems().stream()
                    .filter(l -> l != null && l.getId() == editingOffre.getLocalisation().getId())
                    .findFirst().orElse(null);
            localisationid.getSelectionModel().select(target);
        }
    }

    @FXML
    void resetForm(ActionEvent event) {
        clearError(titreid, errTitre);
        clearError(descriptionid, errDescription);
        clearError(statusid, errStatus);
        clearError(typeContratid, errTypeContrat);
        clearError(typeEmploiid, errTypeEmploi);
        clearError(secteurid, errSecteur);
        clearError(localisationid, errLocalisation);
        clearError(niveauExperienceField, errNiveauExperience);
        clearError(niveauEtudesField, errNiveauEtudes);
        clearError(languesRequisesField, errLanguesRequises);
        clearError(nombrePosteField, errNombrePoste);
        clearError(dateLimitePicker, errDateLimite);
        clearError(teletravailCheck, errTeletravail);

        if (editingOffre != null) {
            OffreEmploi src = originalSnapshot != null ? originalSnapshot : editingOffre;
            if (titreid != null) {
                titreid.setText(src.getTitre());
            }
            if (descriptionid != null) {
                descriptionid.setText(src.getDescription());
            }
            if (statusid != null) {
                String s = src.getStatus();
                if (s == null) {
                    statusid.getSelectionModel().clearSelection();
                } else {
                    String wanted = s.trim();
                    String found = statusid.getItems().stream()
                            .filter(it -> it != null && it.trim().equalsIgnoreCase(wanted))
                            .findFirst().orElse(null);
                    statusid.getSelectionModel().select(found);
                }
            }

            if (niveauExperienceField != null) {
                niveauExperienceField.setText(src.getNiveauExperience());
            }
            if (niveauEtudesField != null) {
                niveauEtudesField.setText(src.getNiveauEtudes());
            }
            if (languesRequisesField != null) {
                languesRequisesField.setText(src.getLanguesRequises());
            }
            if (nombrePosteField != null) {
                Integer np = src.getNombrePoste();
                nombrePosteField.setText(np != null ? String.valueOf(np) : "");
            }
            if (dateLimitePicker != null) {
                dateLimitePicker.setValue(src.getDateLimiteCandidature());
            }
            if (teletravailCheck != null) {
                Boolean t = src.getTeletravail();
                teletravailCheck.setSelected(t != null && t);
            }

            if (typeContratid != null) {
                TypeContrat tcSrc = src.getTypeContrat();
                TypeContrat target = tcSrc == null ? null : typeContratid.getItems().stream()
                        .filter(tc -> tc != null && tc.getId() == tcSrc.getId())
                        .findFirst().orElse(null);
                if (target == null) {
                    typeContratid.getSelectionModel().clearSelection();
                } else {
                    typeContratid.getSelectionModel().select(target);
                }
            }
            if (typeEmploiid != null) {
                TypeEmploi teSrc = src.getTypeEmploi();
                TypeEmploi target = teSrc == null ? null : typeEmploiid.getItems().stream()
                        .filter(te -> te != null && te.getId() == teSrc.getId())
                        .findFirst().orElse(null);
                if (target == null) {
                    typeEmploiid.getSelectionModel().clearSelection();
                } else {
                    typeEmploiid.getSelectionModel().select(target);
                }
            }
            if (secteurid != null) {
                SecteurActivite sSrc = src.getSecteur();
                SecteurActivite target = sSrc == null ? null : secteurid.getItems().stream()
                        .filter(s -> s != null && s.getId() == sSrc.getId())
                        .findFirst().orElse(null);
                if (target == null) {
                    secteurid.getSelectionModel().clearSelection();
                } else {
                    secteurid.getSelectionModel().select(target);
                }
            }
            if (localisationid != null) {
                Localisation lSrc = src.getLocalisation();
                Localisation target = lSrc == null ? null : localisationid.getItems().stream()
                        .filter(l -> l != null && l.getId() == lSrc.getId())
                        .findFirst().orElse(null);
                if (target == null) {
                    localisationid.getSelectionModel().clearSelection();
                } else {
                    localisationid.getSelectionModel().select(target);
                }
            }
        } else {
            clearForm();
        }
    }

    @FXML
    void cancelEdit(ActionEvent event) {
        if (editingOffre != null) {
            if (titreid != null && titreid.getScene() != null) {
                titreid.getScene().getWindow().hide();
            }
        } else {
            resetForm(event);
        }
    }

    @FXML
    void openFrontOffice(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Offre/AfficherOffresFront.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root, 1200, 720);
            try {
                scene.getStylesheets().add(getClass().getResource("/cssOffre/wizard.css").toExternalForm());
            } catch (Exception ignored) {
            }

            Stage stage = new Stage();
            stage.initModality(Modality.NONE);
            stage.setTitle("Front Office - Offres");
            stage.setScene(scene);
            stage.setMinWidth(1100);
            stage.setMinHeight(650);
            stage.show();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    void ajouterOffre(ActionEvent event) {
        String titre = titreid != null ? titreid.getText() : null;
        String description = descriptionid != null ? descriptionid.getText() : null;
        String status = statusid != null ? statusid.getValue() : null;

        String niveauExperience = niveauExperienceField != null ? niveauExperienceField.getText() : null;
        String niveauEtudes = niveauEtudesField != null ? niveauEtudesField.getText() : null;
        String languesRequises = languesRequisesField != null ? languesRequisesField.getText() : null;
        String nombrePosteRaw = nombrePosteField != null ? nombrePosteField.getText() : null;
        LocalDate dateLimite = dateLimitePicker != null ? dateLimitePicker.getValue() : null;
        Boolean teletravail = teletravailCheck != null ? teletravailCheck.isSelected() : null;

        titre = titre != null ? titre.trim().replaceAll("\\s+", " ") : null;
        description = description != null ? description.trim() : null;

        niveauExperience = niveauExperience != null ? niveauExperience.trim().replaceAll("\\s+", " ") : null;
        niveauEtudes = niveauEtudes != null ? niveauEtudes.trim().replaceAll("\\s+", " ") : null;
        languesRequises = languesRequises != null ? languesRequises.trim().replaceAll("\\s+", " ") : null;
        nombrePosteRaw = nombrePosteRaw != null ? nombrePosteRaw.trim() : null;

        boolean valid = true;

        if (entrepriseId == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Aucune entreprise associée à votre compte.");
            alert.showAndWait();
            return;
        }

        if (titre == null || titre.isEmpty()) {
            showError(titreid, errTitre, "Titre obligatoire");
            valid = false;
        } else {
            if (titre.length() < 3) {
                showError(titreid, errTitre, "Minimum 3 caractères");
                valid = false;
            }
            if (titre.length() > 80) {
                showError(titreid, errTitre, "Maximum 80 caractères");
                valid = false;
            }
            if (titre.matches(".*\\d.*")) {
                showError(titreid, errTitre, "Ne doit pas contenir de chiffres");
                valid = false;
            }
            if (!titre.matches("[A-Za-zÀ-ÿ' -]+")) {
                showError(titreid, errTitre, "Caractères invalides");
                valid = false;
            }
        }

        if (description == null || description.isEmpty()) {
            showError(descriptionid, errDescription, "Description obligatoire");
            valid = false;
        } else if (description.length() < 10) {
            showError(descriptionid, errDescription, "Minimum 10 caractères");
            valid = false;
        } else if (description.length() > 1000) {
            showError(descriptionid, errDescription, "Maximum 1000 caractères");
            valid = false;
        }

        if (status == null || status.trim().isEmpty()) {
            showError(statusid, errStatus, "Status obligatoire");
            valid = false;
        } else {
            String normalized = status.trim().toLowerCase();
            boolean allowed = normalized.equals("active") || normalized.equals("non active");
            if (!allowed) {
                showError(statusid, errStatus, "Status invalide");
                valid = false;
            }
        }
        if (typeContratid == null || typeContratid.getValue() == null) {
            showError(typeContratid, errTypeContrat, "Type de contrat obligatoire");
            valid = false;
        }
        if (typeEmploiid == null || typeEmploiid.getValue() == null) {
            showError(typeEmploiid, errTypeEmploi, "Type d'emploi obligatoire");
            valid = false;
        }
        if (secteurid == null || secteurid.getValue() == null) {
            showError(secteurid, errSecteur, "Secteur obligatoire");
            valid = false;
        }
        if (localisationid == null || localisationid.getValue() == null) {
            showError(localisationid, errLocalisation, "Localisation obligatoire - Utilisez la carte");
            valid = false;
        } else {
            // Vérifier que la localisation a des coordonnées GPS valides
            Localisation loc = localisationid.getValue();
            Double lat = loc.getLatitude();
            Double lon = loc.getLongitude();
            if (lat == null || lon == null || lat == 0 || lon == 0) {
                showError(localisationid, errLocalisation, "La localisation doit avoir des coordonnées GPS - Utilisez '🗺 Choisir sur la carte'");
                valid = false;
            }
        }

        if (niveauExperience == null || niveauExperience.isEmpty()) {
            showError(niveauExperienceField, errNiveauExperience, "Niveau d'expérience obligatoire");
            valid = false;
        } else if (niveauExperience.length() < 2) {
            showError(niveauExperienceField, errNiveauExperience, "Minimum 2 caractères");
            valid = false;
        } else if (niveauExperience.length() > 50) {
            showError(niveauExperienceField, errNiveauExperience, "Maximum 50 caractères");
            valid = false;
        }

        if (niveauEtudes == null || niveauEtudes.isEmpty()) {
            showError(niveauEtudesField, errNiveauEtudes, "Niveau d'études obligatoire");
            valid = false;
        } else if (niveauEtudes.length() < 2) {
            showError(niveauEtudesField, errNiveauEtudes, "Minimum 2 caractères");
            valid = false;
        } else if (niveauEtudes.length() > 100) {
            showError(niveauEtudesField, errNiveauEtudes, "Maximum 100 caractères");
            valid = false;
        }

        if (languesRequises == null || languesRequises.isEmpty()) {
            showError(languesRequisesField, errLanguesRequises, "Langues requises obligatoires");
            valid = false;
        } else if (languesRequises.length() < 2) {
            showError(languesRequisesField, errLanguesRequises, "Minimum 2 caractères");
            valid = false;
        } else if (languesRequises.length() > 255) {
            showError(languesRequisesField, errLanguesRequises, "Maximum 255 caractères");
            valid = false;
        }

        Integer nombrePoste = null;
        if (nombrePosteRaw == null || nombrePosteRaw.isEmpty()) {
            showError(nombrePosteField, errNombrePoste, "Nombre de postes obligatoire");
            valid = false;
        } else {
            try {
                nombrePoste = Integer.parseInt(nombrePosteRaw);
                if (nombrePoste <= 0) {
                    showError(nombrePosteField, errNombrePoste, "Doit être > 0");
                    valid = false;
                } else if (nombrePoste > 999) {
                    showError(nombrePosteField, errNombrePoste, "Trop grand");
                    valid = false;
                }
            } catch (Exception ex) {
                showError(nombrePosteField, errNombrePoste, "Nombre invalide");
                valid = false;
            }
        }

        if (dateLimite == null) {
            showError(dateLimitePicker, errDateLimite, "Date limite obligatoire");
            valid = false;
        } else {
            LocalDate today = LocalDate.now();
            if (dateLimite.isBefore(today)) {
                showError(dateLimitePicker, errDateLimite, "Doit être >= aujourd'hui");
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        // Vérifier que l'entreprise est bien chargée
        if (entrepriseId == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Impossible de créer l'offre : aucune entreprise associée à votre compte.");
            alert.showAndWait();
            return;
        }

        try {
            OffreEmploi o = new OffreEmploi(titre, description, status.trim());
            if (editingOffre != null) {
                o.setId(editingOffre.getId());
            }

            o.setNiveauExperience(niveauExperience);
            o.setNiveauEtudes(niveauEtudes);
            o.setLanguesRequises(languesRequises);
            o.setNombrePoste(nombrePoste);
            o.setDateLimiteCandidature(dateLimite);
            o.setTeletravail(teletravail);

            if (entrepriseId != null) {
                o.setIdEntreprise(entrepriseId);
            }

            if (typeContratid != null) {
                o.setTypeContrat(typeContratid.getValue());
            }
            if (typeEmploiid != null) {
                o.setTypeEmploi(typeEmploiid.getValue());
            }
            if (secteurid != null) {
                o.setSecteur(secteurid.getValue());
            }
            if (localisationid != null) {
                o.setLocalisation(localisationid.getValue());
            }

            if (editingOffre != null) {
                service.modifierOffreEmploi(o);
            } else {
                service.ajouterOffreEmploi(o);
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Succès");
            alert.setHeaderText(null);
            alert.setContentText(editingOffre != null ? "Offre modifiée" : "Offre ajoutée");
            alert.showAndWait();

            if (navigateToOffresList()) {
                return;
            }

            if (editingOffre != null) {
                if (titreid != null && titreid.getScene() != null) {
                    titreid.getScene().getWindow().hide();
                }
            } else {
                clearForm();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private boolean navigateToOffresList() {
        try {
            Scene scene = null;
            if (dashCenter != null) {
                scene = dashCenter.getScene();
            }
            if (scene == null && titreid != null) {
                scene = titreid.getScene();
            }
            if (scene == null) {
                return false;
            }

            Node host = scene.lookup("#adminContentArea");
            if (!(host instanceof StackPane sp)) {
                return false;
            }

            Parent listPage = FXMLLoader.load(getClass().getResource("/Offre/AfficherOffre.fxml"));
            sp.getChildren().setAll(listPage);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private void clearForm() {
        if (titreid != null) {
            titreid.clear();
        }
        if (descriptionid != null) {
            descriptionid.clear();
        }
        if (statusid != null) {
            statusid.getSelectionModel().clearSelection();
        }
        if (typeContratid != null) {
            typeContratid.getSelectionModel().clearSelection();
        }
        if (typeEmploiid != null) {
            typeEmploiid.getSelectionModel().clearSelection();
        }
        if (secteurid != null) {
            secteurid.getSelectionModel().clearSelection();
        }
        if (localisationid != null) {
            localisationid.getSelectionModel().clearSelection();
        }

        if (niveauExperienceField != null) {
            niveauExperienceField.clear();
        }
        if (niveauEtudesField != null) {
            niveauEtudesField.clear();
        }
        if (languesRequisesField != null) {
            languesRequisesField.clear();
        }
        if (nombrePosteField != null) {
            nombrePosteField.clear();
        }
        if (dateLimitePicker != null) {
            dateLimitePicker.setValue(null);
        }
        if (teletravailCheck != null) {
            teletravailCheck.setSelected(false);
        }
    }

    private void setError(javafx.scene.control.Control control) {
        if (control == null) {
            return;
        }
        if (!control.getStyleClass().contains("error")) {
            control.getStyleClass().add("error");
        }
    }

    private void showError(javafx.scene.control.Control control, Label label, String message) {
        setError(control);
        if (label == null) {
            return;
        }
        label.setText(message != null ? message : "");
        label.setVisible(true);
        label.setManaged(true);
    }

    private void clearError(javafx.scene.control.Control control, Label label) {
        if (control != null) {
            control.getStyleClass().remove("error");
        }
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    private void playEntryAnimation(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        node.setOpacity(0);
        node.setTranslateY(10);

        FadeTransition fade = new FadeTransition(Duration.millis(260), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), node);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    private void installHoverAnimation(javafx.scene.Node node) {
        if (node == null) {
            return;
        }

        ScaleTransition enter = new ScaleTransition(Duration.millis(120), node);
        enter.setToX(1.02);
        enter.setToY(1.02);
        enter.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition exit = new ScaleTransition(Duration.millis(140), node);
        exit.setToX(1.0);
        exit.setToY(1.0);
        exit.setInterpolator(Interpolator.EASE_OUT);

        node.setOnMouseEntered(e -> {
            exit.stop();
            enter.playFromStart();
        });

        node.setOnMouseExited(e -> {
            enter.stop();
            exit.playFromStart();
        });
    }
}
