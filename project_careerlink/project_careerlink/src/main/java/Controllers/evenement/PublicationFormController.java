package Controllers.evenement;

import Entities.evenement.Publication;
import Entities.User.User;
import Services.evenement.PublicationService;
import Utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;

public class PublicationFormController {

    @FXML private Label lblEventId;
    @FXML private TextField tfTitre;
    @FXML private TextArea taContenu;
    @FXML private TextField tfAuteurId;
    @FXML private ComboBox<String> cbType;
    @FXML private ComboBox<String> cbStatut;
    @FXML private ComboBox<String> cbVisibilite;
    @FXML private TextField tfImageUrl;
    @FXML private TextField tfVideoUrl;
    @FXML private TextField tfPieceJointeUrl;
    @FXML private ImageView ivPhoto;
    @FXML private Label lblAnalysis;
    @FXML private Button btnPublish;
    @FXML private Button btnCancel;
    @FXML private Button btnChooseImage;
    @FXML private Button btnChooseVideo;
    @FXML private Button btnChooseAttachment;

    private final PublicationService publicationService = new PublicationService();
    private Publication publicationToEdit;
    private int evenementId;
    private String returnViewFxml;

    private final OpenAIImageAnalyzer imageAnalyzer = new OpenAIImageAnalyzer();
    private volatile Task<OpenAIImageAnalyzer.AnalysisResult> runningAnalysis;

    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    public void setEvenementId(int evenementId) {
        this.evenementId = evenementId;
        if (lblEventId != null) {
            lblEventId.setText("Event: #" + evenementId);
        }
    }

    public void setEventId(int eventId) {
        setEvenementId(eventId);
    }

    public void setReturnView(String fxmlPath) {
        this.returnViewFxml = fxmlPath;
    }

    public void setModeAdd() {
        this.publicationToEdit = null;
        if (btnPublish != null) {
            btnPublish.setText("Publier");
        }
    }

    public void setModeEdit(Publication p) {
        setPublicationToEdit(p);
    }

    public void setPublicationToEdit(Publication p) {
        this.publicationToEdit = p;
        
        if (p == null) return;
        
        if (tfTitre != null) tfTitre.setText(p.getTitre());
        if (taContenu != null) taContenu.setText(p.getContenu());
        if (tfAuteurId != null) tfAuteurId.setText(String.valueOf(p.getAuteurId()));
        if (cbType != null) cbType.setValue(p.getType());
        if (cbStatut != null) cbStatut.setValue(p.getStatut());
        if (cbVisibilite != null) cbVisibilite.setValue(p.getVisibilite());
        if (tfImageUrl != null) tfImageUrl.setText(p.getImageUrl());
        if (tfVideoUrl != null) tfVideoUrl.setText(p.getVideoUrl());
        if (tfPieceJointeUrl != null) tfPieceJointeUrl.setText(p.getPieceJointeUrl());
        
        if (btnPublish != null) btnPublish.setText("Update");
        
        refreshImagePreview();
    }

    @FXML
    public void initialize() {
        // Hide auteurId field since it's now automatic
        if (tfAuteurId != null) {
            tfAuteurId.setVisible(false);
            tfAuteurId.setManaged(false);
            
            // Pre-populate with current user ID for reference
            User currentUser = Session.getCurrentUser();
            if (currentUser != null) {
                tfAuteurId.setText(String.valueOf(currentUser.getId()));
            }
        }
        
        if (cbType != null) {
            cbType.setItems(FXCollections.observableArrayList("POST", "ANNONCE", "INFO"));
            cbType.setValue("POST");
        }

        if (cbStatut != null) {
            cbStatut.setItems(FXCollections.observableArrayList("PUBLIE", "BROUILLON"));
            cbStatut.setValue("PUBLIE");
        }

        if (cbVisibilite != null) {
            cbVisibilite.setItems(FXCollections.observableArrayList("PUBLIC", "PRIVE"));
            cbVisibilite.setValue("PUBLIC");
        }

        if (tfImageUrl != null) {
            tfImageUrl.textProperty().addListener((obs, o, n) -> refreshImagePreview());
        }

        if (lblAnalysis != null) {
            lblAnalysis.setText("");
        }
    }

    @FXML
    private void onChooseImage() {
        File f = chooseFile("Choisir une image", "Images", "*.jpg", "*.jpeg", "*.png", "*.webp");
        if (f == null) return;

        long fileSize = f.length();
        System.out.println("[PublicationFormController] Selected image: " + f.getName());
        System.out.println("[PublicationFormController] File size: " + fileSize + " bytes");

        if (fileSize > MAX_IMAGE_BYTES) {
            if (lblAnalysis != null) {
                lblAnalysis.setText("Image too large");
                lblAnalysis.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: 700;");
            }
            if (btnPublish != null) {
                btnPublish.setDisable(true);
            }
            return;
        }

        OpenAIImageAnalyzer.MimeValidation validation = OpenAIImageAnalyzer.validateImageFile(f);
        if (!validation.isValid()) {
            String msg = validation.message();
            if (msg == null || msg.isBlank()) {
                msg = "Invalid image type";
            }
            if (lblAnalysis != null) {
                lblAnalysis.setText(msg);
                lblAnalysis.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: 700;");
            }
            if (btnPublish != null) {
                btnPublish.setDisable(true);
            }
            return;
        }

        System.out.println("[PublicationFormController] Detected MIME: " + validation.mime());

        if (tfImageUrl != null) {
            tfImageUrl.setText(f.toURI().toString());
        }

        setPreviewFromFile(f);
        runModerationAsync(f);
    }

    @FXML
    private void chooseImage() {
        onChooseImage();
    }

    @FXML
    private void chooseVideo() {
        File f = chooseFile("Choisir une vidéo", "Vidéos", "*.mp4", "*.mov", "*.mkv", "*.avi");
        if (f != null && tfVideoUrl != null) {
            tfVideoUrl.setText(f.toURI().toString());
        }
    }

    @FXML
    private void chooseAttachment() {
        File f = chooseFile("Choisir une pièce jointe", "Fichiers", "*.pdf", "*.doc", "*.docx", "*.txt");
        if (f != null && tfPieceJointeUrl != null) {
            tfPieceJointeUrl.setText(f.toURI().toString());
        }
    }

    @FXML
    private void save() {
        try {
            if (evenementId <= 0) {
                showAlert("Erreur", "evenementId manquant.");
                return;
            }

            String titre = tfTitre != null ? tfTitre.getText().trim() : null;
            if (titre == null || titre.isEmpty()) {
                showAlert("Validation", "Veuillez saisir un titre.");
                return;
            }

            String contenu = taContenu != null ? taContenu.getText().trim() : null;
            if (contenu == null || contenu.isEmpty()) {
                showAlert("Validation", "Veuillez saisir un contenu.");
                return;
            }

            // Automatically use current user's ID
            int auteurId;
            User currentUser = Session.getCurrentUser();
            if (currentUser != null) {
                auteurId = currentUser.getId();
            } else {
                showAlert("Erreur", "Aucun utilisateur connecté. Veuillez vous reconnecter.");
                return;
            }

            String type = cbType != null ? cbType.getValue() : null;
            String statut = cbStatut != null ? cbStatut.getValue() : null;
            String visibilite = cbVisibilite != null ? cbVisibilite.getValue() : null;
            String imageUrl = tfImageUrl != null ? emptyToNull(tfImageUrl.getText()) : null;
            String videoUrl = tfVideoUrl != null ? emptyToNull(tfVideoUrl.getText()) : null;
            String pieceJointeUrl = tfPieceJointeUrl != null ? emptyToNull(tfPieceJointeUrl.getText()) : null;

            Publication p = new Publication(
                titre,
                contenu,
                LocalDateTime.now(),
                auteurId,
                type,
                statut,
                visibilite,
                imageUrl,
                videoUrl,
                pieceJointeUrl,
                evenementId
            );

            if (publicationToEdit != null) {
                p.setIdPublication(publicationToEdit.getIdPublication());
                publicationService.modifierPublication(p);
                showAlert("Succès", "Publication mise à jour avec succès.");
            } else {
                publicationService.ajouterPublication(p);
                showAlert("Succès", "Publication créée avec succès.");
            }

            finishAndReturn();

        } catch (Exception e) {
            showAlert("Erreur", "Sauvegarde impossible: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        finishAndReturn();
    }

    private void finishAndReturn() {
        if (NavigationService.hasContentArea()) {
            String target = (returnViewFxml == null || returnViewFxml.isBlank())
                    ? "/views/event_publications.fxml"
                    : returnViewFxml;

            NavigationService.go(target, ctrl -> {
                if (ctrl instanceof EventPublicationsController) {
                    ((EventPublicationsController) ctrl).setEventId(evenementId);
                } else if (ctrl instanceof EventPublicationsBackendController) {
                    ((EventPublicationsBackendController) ctrl).setEventId(evenementId);
                }
            });
            return;
        }

        if (Nav.getDefaultContentArea() != null) {
            String target = (returnViewFxml == null || returnViewFxml.isBlank())
                    ? "/views_event/event_publications.fxml"
                    : returnViewFxml;
            Nav.go(target, ctrl -> {
                if (ctrl instanceof EventPublicationsController) {
                    ((EventPublicationsController) ctrl).setEventId(evenementId);
                } else if (ctrl instanceof EventPublicationsBackendController) {
                    ((EventPublicationsBackendController) ctrl).setEventId(evenementId);
                }
            });
            return;
        }
 
        // Fallback: close modal stage (if this form is ever opened as a separate window)
        try {
            if (tfTitre != null && tfTitre.getScene() != null) {
                Stage stage = (Stage) tfTitre.getScene().getWindow();
                stage.close();
            }
        } catch (Exception ignored) {
        }
    }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void refreshImagePreview() {
        if (ivPhoto == null || tfImageUrl == null) return;
        
        String url = tfImageUrl.getText();
        if (url == null || url.trim().isEmpty()) {
            ivPhoto.setImage(null);
            return;
        }

        try {
            Image img = new Image(url, true);
            ivPhoto.setImage(img);
        } catch (Exception ignored) {
            ivPhoto.setImage(null);
        }
    }

    private void setPreviewFromFile(File f) {
        if (ivPhoto == null || f == null) return;
        try {
            Image img = new Image(f.toURI().toString(), true);
            ivPhoto.setImage(img);
        } catch (Exception ignored) {
            ivPhoto.setImage(null);
        }
    }

    private void runModerationAsync(File imageFile) {
        if (lblAnalysis != null) {
            lblAnalysis.setText("Analyzing...");
            lblAnalysis.setStyle("-fx-text-fill: #334155;");
        }
        if (btnPublish != null) {
            btnPublish.setDisable(true);
        }

        Task<OpenAIImageAnalyzer.AnalysisResult> previous = runningAnalysis;
        if (previous != null) {
            previous.cancel(true);
        }

        Task<OpenAIImageAnalyzer.AnalysisResult> task = new Task<>() {
            @Override
            protected OpenAIImageAnalyzer.AnalysisResult call() throws Exception {
                return imageAnalyzer.analyzeImageForHate(imageFile);
            }
        };
        runningAnalysis = task;

        task.setOnSucceeded(evt -> {
            OpenAIImageAnalyzer.AnalysisResult r = task.getValue();
            Platform.runLater(() -> applyModerationResult(r));
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                if (lblAnalysis != null) {
                    String msg = "Analysis failed.";
                    if (ex instanceof IllegalArgumentException && ex.getMessage() != null && !ex.getMessage().isBlank()) {
                        msg = ex.getMessage();
                    }
                    lblAnalysis.setText(msg);
                    lblAnalysis.setStyle("-fx-text-fill: #B45309; -fx-font-weight: 700;");
                }
                if (btnPublish != null) {
                    btnPublish.setDisable(true);
                }
                if (ex != null) {
                    ex.printStackTrace();
                }
            });
        });

        task.setOnCancelled(evt -> Platform.runLater(() -> {
            if (lblAnalysis != null && "Analyzing...".equals(lblAnalysis.getText())) {
                lblAnalysis.setText("");
            }
        }));

        Thread t = new Thread(task, "openai-image-moderation");
        t.setDaemon(true);
        t.start();
    }

    private void applyModerationResult(OpenAIImageAnalyzer.AnalysisResult r) {
        if (r == null) {
            if (lblAnalysis != null) {
                lblAnalysis.setText("Analysis failed.");
                lblAnalysis.setStyle("-fx-text-fill: #B45309;");
            }
            if (btnPublish != null) {
                btnPublish.setDisable(true);
            }
            return;
        }

        if (r.isHate()) {
            if (lblAnalysis != null) {
                lblAnalysis.setText("Not good — if you publish you will be banned." + (r.reason().isBlank() ? "" : "\n" + r.reason()));
                lblAnalysis.setStyle("-fx-text-fill: #B91C1C; -fx-font-weight: 700;");
            }
            if (btnPublish != null) {
                btnPublish.setDisable(true);
            }
        } else {
            if (lblAnalysis != null) {
                lblAnalysis.setText("Good — content is safe." + (r.reason().isBlank() ? "" : "\n" + r.reason()));
                lblAnalysis.setStyle("-fx-text-fill: #15803D; -fx-font-weight: 700;");
            }
            if (btnPublish != null) {
                btnPublish.setDisable(false);
            }
        }
    }

    private File chooseFile(String title, String desc, String... exts) {
        try {
            if (tfTitre == null || tfTitre.getScene() == null) return null;
            
            Stage stage = (Stage) tfTitre.getScene().getWindow();
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            
            FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(desc, exts);
            fc.getExtensionFilters().add(filter);
            
            return fc.showOpenDialog(stage);
        } catch (Exception e) {
            return null;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}