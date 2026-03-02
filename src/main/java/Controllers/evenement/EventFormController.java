package Controllers.evenement;

import Entities.evenement.Evenement;
import Services.evenement.EvenementService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class EventFormController {

    @FXML private BorderPane rootPane;

    @FXML private Button btnBack;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;
    @FXML private Button btnChooseImage;
    @FXML private Button btnRemoveImage;
    @FXML private Button btnGenerateDescription;

    @FXML private Label lblFormTitle;

    @FXML private TextField tfTitre;
    @FXML private TextArea taDescription;
    @FXML private DatePicker dpDateDebut;
    @FXML private DatePicker dpDateFin;
    @FXML private TextField tfLieu;
    @FXML private ComboBox<String> tfType;
    @FXML private TextField tfCapacite;
    @FXML private ComboBox<String> cbStatut;

    @FXML private ImageView imgPreview;
    @FXML private VBox boxImagePlaceholder;
    @FXML private Label lblImagePath;

    private final EvenementService evenementService = new EvenementService();

    private final OpenAIEventDescriptionGenerator descriptionGenerator = new OpenAIEventDescriptionGenerator();
    private volatile Task<String> runningDescriptionTask;

    private String imageUrl;

    private Integer editingEventId;

    @FXML
    public void initialize() {
        cbStatut.getItems().setAll("BROUILLON", "PUBLIE", "ANNULE", "TERMINE");
        cbStatut.setValue("BROUILLON");

        if (tfType != null) {
            tfType.getItems().setAll("REUNION", "FORMATION", "SEMINAIRE", "CONFERENCE", "ATELIER");
        }

        if (dpDateDebut != null) {
            final LocalDate today = LocalDate.now();
            dpDateDebut.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        return;
                    }

                    boolean notAllowed = !item.isAfter(today);
                    setDisable(notAllowed);
                    if (notAllowed) {
                        setStyle("-fx-background-color: #ffe5e5;");
                    }
                }
            });
        }

        if (dpDateFin != null) {
            dpDateFin.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        return;
                    }

                    LocalDate start = dpDateDebut != null ? dpDateDebut.getValue() : null;
                    boolean notAllowed = (start != null) ? !item.isAfter(start) : false;
                    setDisable(notAllowed);
                    if (notAllowed) {
                        setStyle("-fx-background-color: #ffe5e5;");
                    }
                }
            });
        }

        if (dpDateDebut != null && dpDateFin != null) {
            dpDateDebut.valueProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    LocalDate fin = dpDateFin.getValue();
                    if (fin != null && !fin.isAfter(n)) {
                        dpDateFin.setValue(null);
                    }
                }
                dpDateFin.setDayCellFactory(Objects.requireNonNullElseGet(dpDateFin.getDayCellFactory(), () -> p -> new DateCell()));
            });
        }

        imgPreview.setImage(null);
        setPlaceholderVisible(true);
        lblImagePath.setText("");
        updateImageButtons();

        playOpenFade();
    }

    public void setEvent(Evenement event) {
        if (event == null) return;

        editingEventId = event.getIdEvenement();

        if (lblFormTitle != null) {
            lblFormTitle.setText("Modifier un Évènement");
        }
        if (btnSave != null) {
            btnSave.setText("Mettre à jour");
        }

        tfTitre.setText(event.getTitre());
        taDescription.setText(event.getDescription());
        tfLieu.setText(event.getLieu());
        if (tfType != null) {
            String type = event.getType();
            if (type != null) {
                tfType.setValue(type);
                if (tfType.isEditable() && tfType.getEditor() != null) {
                    tfType.getEditor().setText(type);
                }
            } else {
                tfType.setValue(null);
                if (tfType.isEditable() && tfType.getEditor() != null) {
                    tfType.getEditor().setText("");
                }
            }
        }
        tfCapacite.setText(String.valueOf(event.getCapaciteMax()));

        if (event.getStatut() != null) {
            cbStatut.setValue(event.getStatut());
        }

        if (event.getDateDebut() != null) {
            dpDateDebut.setValue(event.getDateDebut().toLocalDate());
        } else {
            dpDateDebut.setValue(null);
        }

        if (event.getDateFin() != null) {
            dpDateFin.setValue(event.getDateFin().toLocalDate());
        } else {
            dpDateFin.setValue(null);
        }

        imageUrl = event.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                imgPreview.setImage(new Image(imageUrl, true));
                setPlaceholderVisible(false);
                lblImagePath.setText(imageUrl);
            } catch (Exception e) {
                imageUrl = null;
                imgPreview.setImage(null);
                setPlaceholderVisible(true);
                lblImagePath.setText("");
            }
        } else {
            imgPreview.setImage(null);
            setPlaceholderVisible(true);
            lblImagePath.setText("");
        }

        updateImageButtons();
    }

    private void playOpenFade() {
        if (rootPane == null) return;
        rootPane.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(220), rootPane);
        ft.setToValue(1);
        ft.play();
    }

    @FXML
    private void handleChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(getStage());
        if (file == null) return;

        imageUrl = file.toURI().toString();
        try {
            Image image = new Image(imageUrl, true);
            imgPreview.setImage(image);
            setPlaceholderVisible(false);
            lblImagePath.setText(file.getName());
            updateImageButtons();
        } catch (Exception e) {
            imageUrl = null;
            imgPreview.setImage(null);
            setPlaceholderVisible(true);
            lblImagePath.setText("");
            updateImageButtons();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Image invalide.");
        }
    }

    @FXML
    private void handleRemoveImage() {
        imageUrl = null;
        imgPreview.setImage(null);
        setPlaceholderVisible(true);
        lblImagePath.setText("");
        updateImageButtons();
    }

    private void updateImageButtons() {
        boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();
        if (btnRemoveImage != null) btnRemoveImage.setDisable(!hasImage);
    }

    @FXML
    private void handleGenerateDescription() {
        if (taDescription == null) return;

        if (btnGenerateDescription != null) {
            btnGenerateDescription.setDisable(true);
            btnGenerateDescription.setText("Génération...");
        }

        Task<String> previous = runningDescriptionTask;
        if (previous != null) {
            previous.cancel(true);
        }

        OpenAIEventDescriptionGenerator.EventInfo info = new OpenAIEventDescriptionGenerator.EventInfo(
                safeTrim(tfTitre != null ? tfTitre.getText() : null),
                readTypeValue(),
                safeTrim(tfLieu != null ? tfLieu.getText() : null),
                dpDateDebut != null && dpDateDebut.getValue() != null ? dpDateDebut.getValue().toString() : "",
                dpDateFin != null && dpDateFin.getValue() != null ? dpDateFin.getValue().toString() : "",
                safeTrim(tfCapacite != null ? tfCapacite.getText() : null),
                cbStatut != null && cbStatut.getValue() != null ? cbStatut.getValue() : ""
        );

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return descriptionGenerator.generateDescription(info);
            }
        };
        runningDescriptionTask = task;

        task.setOnSucceeded(evt -> {
            String desc = task.getValue();
            Platform.runLater(() -> {
                if (desc != null && !desc.isBlank()) {
                    taDescription.setText(desc);
                }
                resetGenerateButton();
            });
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> {
                resetGenerateButton();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Génération impossible: " + (ex != null ? ex.getMessage() : "Erreur inconnue"));
                if (ex != null) {
                    ex.printStackTrace();
                }
            });
        });

        task.setOnCancelled(evt -> Platform.runLater(this::resetGenerateButton));

        Thread t = new Thread(task, "openai-event-description");
        t.setDaemon(true);
        t.start();
    }

    private void resetGenerateButton() {
        if (btnGenerateDescription != null) {
            btnGenerateDescription.setDisable(false);
            btnGenerateDescription.setText("Générer");
        }
    }

    private String readTypeValue() {
        if (tfType == null) return "";
        if (tfType.isEditable() && tfType.getEditor() != null) {
            return safeTrim(tfType.getEditor().getText());
        }
        return safeTrim(tfType.getValue());
    }

    private void setPlaceholderVisible(boolean visible) {
        if (boxImagePlaceholder != null) {
            boxImagePlaceholder.setVisible(visible);
            boxImagePlaceholder.setManaged(visible);
        }
    }

    @FXML
    private void handleSave() {
        try {
            String titre = safeTrim(tfTitre.getText());
            String description = safeTrim(taDescription.getText());
            String lieu = safeTrim(tfLieu.getText());
            String type;
            if (tfType == null) {
                type = "";
            } else if (tfType.isEditable() && tfType.getEditor() != null) {
                type = safeTrim(tfType.getEditor().getText());
            } else {
                type = safeTrim(tfType.getValue());
            }
            String statut = cbStatut.getValue();

            if (titre.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le titre est obligatoire.");
                return;
            }
            if (lieu.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le lieu est obligatoire.");
                return;
            }
            if (type.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Le type est obligatoire.");
                return;
            }

            int capaciteMax;
            try {
                capaciteMax = Integer.parseInt(safeTrim(tfCapacite.getText()));
            } catch (Exception ex) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Capacité max invalide.");
                return;
            }

            LocalDateTime dateDebut = toStartOfDay(dpDateDebut.getValue());
            LocalDateTime dateFin = toStartOfDay(dpDateFin.getValue());

            if (dpDateDebut.getValue() != null && !dpDateDebut.getValue().isAfter(LocalDate.now())) {
                showAlert("La date début doit être après aujourd'hui.");
                return;
            }

            if (dateDebut != null && dateFin != null && !dateFin.isAfter(dateDebut)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "La date fin doit être strictement après la date début.");
                return;
            }

            Evenement e = new Evenement(
                    titre,
                    description,
                    dateDebut,
                    dateFin,
                    lieu,
                    type,
                    capaciteMax,
                    statut,
                    imageUrl
            );

            if (editingEventId != null) {
                e.setIdEvenement(editingEventId);
                evenementService.modifierEvenement(e);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Évènement modifié avec succès.");
            } else {
                evenementService.ajouterEvenement(e);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Évènement enregistré avec succès.");
            }

            goBackToListing();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'enregistrer: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        goBackToListing();
    }

    @FXML
    private void handleBack() {
        goBackToListing();
    }

    private void goBackToListing() {
        try {
            // Try NavigationService first (for backend navigation)
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/event_affichage_backend.fxml");
                return;
            }

            // Try Nav (alternative navigation service)
            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_affichage_backend.fxml");
                return;
            }

            // Fallback to regular navigation
            Parent root = FXMLLoader.load(getClass().getResource("/views_event/event_affichage_backend.fxml"));
            Stage stage = getStage();
            Scene current = stage.getScene();
            if (current == null) {
                stage.setScene(new Scene(root, 1300, 780));
            } else {
                current.setRoot(root);
            }

            stage.setTitle("Backend • Gestion des Évènements");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de revenir à la liste: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Stage getStage() {
        return (Stage) rootPane.getScene().getWindow();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    private static LocalDateTime toStartOfDay(LocalDate date) {
        if (date == null) return null;
        return LocalDateTime.of(date, LocalTime.of(0, 0));
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        showAlert(Alert.AlertType.WARNING, "Validation", message);
    }
}
