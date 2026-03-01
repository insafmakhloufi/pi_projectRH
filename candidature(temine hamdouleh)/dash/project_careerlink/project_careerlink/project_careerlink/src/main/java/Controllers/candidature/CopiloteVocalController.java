package Controllers.candidature;

import Services.candidature.CopiloteVocalService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import javafx.scene.layout.Pane;
import java.io.File;

public class CopiloteVocalController {

    @FXML private VBox copiloteRoot;
    @FXML private VBox popupCopilote;
    @FXML private Circle btnMicro;
    @FXML private Label lblStatut;
    @FXML private Label lblTexte;

    private final CopiloteVocalService service = new CopiloteVocalService();
    private boolean isRecording = false;
    private boolean isPopupVisible = false;
    private String contexteActuel = "Page principale";
    private Pane navigationContainer;
    private Timeline pulseAnimation;

    public void setNavigationContainer(Pane container) {
        this.navigationContainer = container;
    }

    public void setContexte(String contexte) {
        this.contexteActuel = contexte;
    }

    @FXML
    private void initialize() {
        if (popupCopilote != null) {
            popupCopilote.setVisible(false);
            popupCopilote.setManaged(false);
        }
    }

    @FXML
    private void togglePopup() {
        isPopupVisible = !isPopupVisible;
        if (popupCopilote != null) {
            popupCopilote.setVisible(isPopupVisible);
            popupCopilote.setManaged(isPopupVisible);
        }
        if (!isPopupVisible && isRecording) {
            stopRecording();
        }
    }

    @FXML
    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }



    //jdidaa
    // Ajoute ce champ
    private AjouterCandidature formulaireController;

    public void setFormulaireController(AjouterCandidature ctrl) {
        this.formulaireController = ctrl;
    }

    private void startRecording() {
        isRecording = true;
        lblStatut.setText("🎤 En écoute...");
        lblTexte.setText("");
        btnMicro.setFill(Color.web("#ef4444"));

        // Animation pulse
        startPulseAnimation();

        new Thread(() -> {
            try {
                service.demarrerEnregistrement();
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatut.setText("❌ Erreur micro");
                    isRecording = false;
                });
            }
        }).start();
    }

    private void stopRecording() {
        isRecording = false;
        stopPulseAnimation();
        btnMicro.setFill(Color.web("#4f46e5"));
        lblStatut.setText("⏳ Traitement...");

        new Thread(() -> {
            try {
                // 1. Arrêter enregistrement
                File audioFile = service.arreterEnregistrement();

                // 2. Whisper → texte
                Platform.runLater(() -> lblStatut.setText("📝 Transcription..."));
                String texte = service.transcrireAudio(audioFile);
                Platform.runLater(() -> lblTexte.setText("\"" + texte + "\""));

                // 3. GPT → action JSON
                Platform.runLater(() -> lblStatut.setText("🧠 Compréhension..."));
                String actionJson = service.comprendreCommande(texte, contexteActuel);

                // 4. Exécuter l'action
                Platform.runLater(() -> {
                    lblStatut.setText("⚡ Exécution...");
                    executerAction(actionJson, texte);
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatut.setText("❌ Erreur: " + e.getMessage());
                });
            }
        }).start();
    }

    /*
    private void executerAction(String actionJson, String texteOriginal) {
        try {
            // Parser le JSON simple
            if (actionJson.contains("\"NAVIGATE\"")) {
                String page = extraireValeur(actionJson, "page");
                naviguerVers(page);

            } else if (actionJson.contains("\"FILL_FIELD\"")) {
                String field = extraireValeur(actionJson, "field");
                String value = extraireValeur(actionJson, "value");
                remplirChamp(field, value);

            } else if (actionJson.contains("\"SEARCH\"")) {
                String query = extraireValeur(actionJson, "query");
                rechercherCandidature(query);

            } else {
                lblStatut.setText("❓ Commande non reconnue");
                parlerAsync("Je n'ai pas compris votre commande. Pouvez-vous répéter ?");
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblStatut.setText("❌ Erreur action");
        }
    }
     */

    private void executerAction(String actionJson, String texteOriginal) {
        try {
            if (actionJson.contains("\"NAVIGATE\"")) {
                String page = extraireValeur(actionJson, "page");
                naviguerVers(page);

            } else if (actionJson.contains("\"NEXT_STEP\"")) {
                cliquerBouton("btnNext", "btnSuivant", "btnNextStep");
                lblStatut.setText("✅ Étape suivante");
                parlerAsync("Passage à l'étape suivante");

            } else if (actionJson.contains("\"PREV_STEP\"")) {
                cliquerBouton("btnPrev", "btnPrecedent", "btnBack");
                lblStatut.setText("✅ Étape précédente");
                parlerAsync("Retour à l'étape précédente");

            } else if (actionJson.contains("\"SUBMIT_FORM\"")) {
                cliquerBouton("btnSubmit", "btnSoumettre");
                lblStatut.setText("✅ Formulaire soumis");
                parlerAsync("Formulaire soumis avec succès");

            } else if (actionJson.contains("\"FILL_FIELD\"")) {
                String field = extraireValeur(actionJson, "field");
                String value = extraireValeur(actionJson, "value");
                remplirChamp(field, value);

            } else if (actionJson.contains("\"FILL_DATE\"")) {
                String field = extraireValeur(actionJson, "field");
                String value = extraireValeur(actionJson, "value");
                remplirDate(field, value);

            } else if (actionJson.contains("\"SELECT_RADIO\"")) {
                String field = extraireValeur(actionJson, "field");
                selectionnerRadio(field);

            } else if (actionJson.contains("\"SELECT_COMBO\"")) {
                String field = extraireValeur(actionJson, "field");
                String value = extraireValeur(actionJson, "value");
                selectionnerCombo(field, value);

            } else if (actionJson.contains("\"START_VIDEO\"")) {
                cliquerBouton("btnRecord", "btnStartRecord");
                lblStatut.setText("✅ Enregistrement démarré");
                parlerAsync("Enregistrement vidéo démarré");

            } else if (actionJson.contains("\"STOP_VIDEO\"")) {
                cliquerBouton("btnStop", "btnStopRecord");
                lblStatut.setText("✅ Enregistrement arrêté");
                parlerAsync("Enregistrement vidéo arrêté");

            } else if (actionJson.contains("\"CHECK_CONFIRM\"")) {
                cocherCheckbox("cbConfirm", "checkConfirm");
                lblStatut.setText("✅ Confirmation cochée");
                parlerAsync("Informations confirmées");

            } else if (actionJson.contains("\"CHECK_PRIVACY\"")) {
                cocherCheckbox("cbPrivacy", "checkPrivacy");
                lblStatut.setText("✅ Politique acceptée");
                parlerAsync("Politique de confidentialité acceptée");

            } else if (actionJson.contains("\"SEARCH\"")) {
                String query = extraireValeur(actionJson, "query");
                rechercherCandidature(query);

            } else {
                lblStatut.setText("❓ Commande non reconnue");
                parlerAsync("Je n'ai pas compris. Pouvez-vous répéter ?");
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblStatut.setText("❌ Erreur");
        }
    }
    private void naviguerVers(String page) {
        if (navigationContainer == null) return;

        String fxmlPath = switch (page) {
            case "MES_CANDIDATURES" -> "/candidaturefxml/MesCandidatures.fxml";
            case "AJOUTER_CANDIDATURE" -> "/candidaturefxml/Ajouter.fxml";
            case "OFFRES" -> "/Dashboardfxml/offre.fxml";
            case "EVENEMENTS" -> "/candidaturefxml/Evenement.fxml";
            case "FORMATIONS" -> "/Dashboardfxml/Formation.fxml";
            default -> null;
        };

        if (fxmlPath == null) {
            lblStatut.setText("❓ Page inconnue");
            parlerAsync("Je ne connais pas cette page.");
            return;
        }

        try {
            Parent view = FXMLLoader.load(getClass().getResource(fxmlPath));
            navigationContainer.getChildren().setAll(view);

            String nomPage = page.replace("_", " ").toLowerCase();
            lblStatut.setText("✅ Navigation vers " + nomPage);
            parlerAsync("Navigation vers " + nomPage);

        } catch (Exception e) {
            e.printStackTrace();
            lblStatut.setText("❌ Page introuvable");
        }
    }

   /* private void remplirChamp(String field, String value) {
        if (navigationContainer == null) return;

        // Chercher le champ dans la vue actuelle
        javafx.scene.Node node = navigationContainer.lookup("#tf" +
                Character.toUpperCase(field.charAt(0)) + field.substring(1));

        if (node instanceof javafx.scene.control.TextField tf) {
            tf.setText(value);
            lblStatut.setText("✅ Champ " + field + " rempli");
            parlerAsync("J'ai rempli le champ " + field + " avec " + value);
        } else {
            lblStatut.setText("❓ Champ " + field + " introuvable");
            parlerAsync("Je n'ai pas trouvé le champ " + field);
        }
    }
*/
    private void rechercherCandidature(String query) {
        if (navigationContainer == null) return;

        javafx.scene.Node node = navigationContainer.lookup("#tfSearch");
        if (node instanceof javafx.scene.control.TextField tf) {
            tf.setText(query);
            lblStatut.setText("✅ Recherche : " + query);
            parlerAsync("Je recherche " + query);
        } else {
            lblStatut.setText("❓ Champ recherche introuvable");
        }
    }

    private void parlerAsync(String texte) {
        new Thread(() -> {
            try {
                service.parler(texte);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String extraireValeur(String json, String key) {
        String marker = "\"" + key + "\": \"";
        int start = json.indexOf(marker);
        if (start < 0) {
            marker = "\"" + key + "\":\"";
            start = json.indexOf(marker);
        }
        if (start < 0) return "";
        start += marker.length();
        int end = json.indexOf("\"", start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private void startPulseAnimation() {
        pulseAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(btnMicro.scaleXProperty(), 1.0),
                        new KeyValue(btnMicro.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(600),
                        new KeyValue(btnMicro.scaleXProperty(), 1.2),
                        new KeyValue(btnMicro.scaleYProperty(), 1.2)),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(btnMicro.scaleXProperty(), 1.0),
                        new KeyValue(btnMicro.scaleYProperty(), 1.0))
        );
        pulseAnimation.setCycleCount(Timeline.INDEFINITE);
        pulseAnimation.play();
    }

    private void stopPulseAnimation() {
        if (pulseAnimation != null) {
            pulseAnimation.stop();
            btnMicro.setScaleX(1.0);
            btnMicro.setScaleY(1.0);
        }
    }

    //jdiddddddddddddddddddddddddddddd

    private void remplirChamp(String fieldId, String value) {
        if (navigationContainer == null) return;

        javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);

        if (node instanceof javafx.scene.control.TextField tf) {
            tf.setText(value);
            lblStatut.setText("✅ " + fieldId + " = " + value);
            parlerAsync("J'ai rempli le champ avec " + value);
        } else if (node instanceof javafx.scene.control.TextArea ta) {
            ta.setText(value);
            lblStatut.setText("✅ " + fieldId + " = " + value);
            parlerAsync("J'ai rempli le champ avec " + value);
        } else {
            lblStatut.setText("❓ Champ " + fieldId + " introuvable");
            parlerAsync("Je n'ai pas trouvé le champ");
        }
    }

    private void remplirDate(String fieldId, String value) {
        if (navigationContainer == null) return;
        javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);
        if (node instanceof javafx.scene.control.DatePicker dp) {
            try {
                dp.setValue(java.time.LocalDate.parse(value));
                lblStatut.setText("✅ Date = " + value);
                parlerAsync("Date remplie : " + value);
            } catch (Exception e) {
                lblStatut.setText("❌ Format date invalide");
            }
        }
    }

    private void selectionnerRadio(String fieldId) {
        if (navigationContainer == null) return;
        javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);
        if (node instanceof javafx.scene.control.RadioButton rb) {
            rb.setSelected(true);
            lblStatut.setText("✅ " + fieldId + " sélectionné");
            parlerAsync("Diplôme sélectionné");
        } else {
            lblStatut.setText("❓ Option introuvable");
        }
    }

    private void selectionnerCombo(String fieldId, String value) {
        if (navigationContainer == null) return;
        javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);
        if (node instanceof javafx.scene.control.ComboBox cb) {
            cb.setValue(value);
            lblStatut.setText("✅ " + fieldId + " = " + value);
            parlerAsync("Sélection effectuée : " + value);
        }
    }

    private void cliquerBouton(String... fieldIds) {
        if (navigationContainer == null) return;
        for (String fieldId : fieldIds) {
            javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);
            if (node instanceof javafx.scene.control.Button btn) {
                btn.fire();
                return;
            }
        }
        lblStatut.setText("❓ Bouton introuvable");
    }

    private void cocherCheckbox(String... fieldIds) {
        if (navigationContainer == null) return;
        for (String fieldId : fieldIds) {
            javafx.scene.Node node = navigationContainer.lookup("#" + fieldId);
            if (node instanceof javafx.scene.control.CheckBox cb) {
                cb.setSelected(true);
                return;
            }
        }
        lblStatut.setText("❓ Checkbox introuvable");
    }




}