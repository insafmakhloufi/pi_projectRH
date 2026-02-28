package Controllers.Formation;

import Entities.Formation.Chapitre;
import Entities.Formation.Cour;
import Services.Formation.ChapitreServices;
import Services.Formation.CourServices;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class AjouterChapitre {

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    @FXML private Button btnRetour;
    @FXML private TextField tfTitre;
    @FXML private TextArea taContenu;
    @FXML private Button btnChoisirPdf;
    @FXML private Button btnEnregistrer;
    @FXML private Label lblMessage;

    private final ChapitreServices chapitreServices = new ChapitreServices();
    private final CourServices courServices = new CourServices();

    private Integer editChapitreId;

    @FXML
    void initialize() {
        if (btnRetour != null) {
            btnRetour.setOnAction(e -> retour());
        }
        if (btnEnregistrer != null) {
            btnEnregistrer.setOnAction(e -> enregistrer());
        }
        if (btnChoisirPdf != null) {
            btnChoisirPdf.setOnAction(e -> choisirPdf());
        }

        setupEditMode();
    }

    private void setupEditMode() {
        editChapitreId = UiState.selectedChapitreEditId;
        if (editChapitreId == null || editChapitreId <= 0) {
            return;
        }

        try {
            Chapitre ch = chapitreServices.getChapitreById(editChapitreId);
            if (ch == null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Chapitre introuvable");
                return;
            }

            UiState.selectedCourId = ch.getCourId();

            if (tfTitre != null) {
                tfTitre.setText(ch.getTitre() == null ? "" : ch.getTitre());
            }
            if (taContenu != null) {
                taContenu.setText(ch.getContenu() == null ? "" : ch.getContenu());
            }

            if (btnEnregistrer != null) {
                btnEnregistrer.setText("Modifier");
            }
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: #dc2626;");
            lblMessage.setText("Erreur: " + e.getMessage());
        }
    }

    private void enregistrer() {
        try {
            if (UiState.selectedCourId == null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Aucun cours sélectionné");
                return;
            }

            if (tfTitre == null || tfTitre.getText().isBlank()
                    || taContenu == null || taContenu.getText().isBlank()) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            int courId = UiState.selectedCourId;

            if (editChapitreId != null && editChapitreId > 0) {
                Chapitre updated = new Chapitre();
                updated.setId(editChapitreId);
                updated.setCourId(courId);
                updated.setTitre(tfTitre.getText().trim());
                updated.setContenu(taContenu.getText().trim());

                chapitreServices.modifierChapitre(updated);
                UiState.selectedChapitreEditId = null;
                editChapitreId = null;

                lblMessage.setStyle("-fx-text-fill: #16a34a;");
                lblMessage.setText("Chapitre modifié avec succès");

                WindowUtil.navigate(btnEnregistrer, "/Formation/AfficherChapitre.fxml", "Chapitres");
                return;
            }

            Cour cour = courServices.getCourById(courId);
            int max = (cour == null) ? 0 : cour.getNbChapitres();
            int current = chapitreServices.countByCour(courId);

            if (max <= 0) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Nombre de chapitres MAX invalide pour ce cours");
                return;
            }

            if (current >= max) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Limite atteinte: " + current + "/" + max);
                return;
            }

            Chapitre ch = new Chapitre();
            ch.setCourId(courId);
            ch.setTitre(tfTitre.getText().trim());
            ch.setContenu(taContenu.getText().trim());

            chapitreServices.ajouterChapitre(ch);

            lblMessage.setStyle("-fx-text-fill: #16a34a;");
            lblMessage.setText("Chapitre ajouté avec succès");

            WindowUtil.navigate(btnEnregistrer, "/Formation/AfficherChapitre.fxml", "Chapitres");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: #dc2626;");
            lblMessage.setText("Erreur: " + e.getMessage());
        }
    }

    private void choisirPdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        Stage stage = null;
        if (btnRetour != null && btnRetour.getScene() != null) {
            stage = (Stage) btnRetour.getScene().getWindow();
        }

        java.util.List<File> selectedFiles = chooser.showOpenMultipleDialog(stage);
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            return;
        }

        try {
            Path uploadsDir = Path.of(System.getProperty("user.home"), ".careerlink", "uploads", "chapitres");
            Files.createDirectories(uploadsDir);

            StringBuilder stored = new StringBuilder();
            if (selectedFiles.size() == 1) {
                File selected = selectedFiles.get(0);
                String baseName = selected.getName();
                String safeName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
                String uniqueName = System.currentTimeMillis() + "_" + safeName;
                Path dest = uploadsDir.resolve(uniqueName);
                Files.copy(selected.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                stored.append(PDF_PREFIX).append(dest.toAbsolutePath());
            } else {
                stored.append(PDFS_PREFIX);
                long baseTs = System.currentTimeMillis();
                for (int i = 0; i < selectedFiles.size(); i++) {
                    File f = selectedFiles.get(i);
                    if (f == null) {
                        continue;
                    }
                    String baseName = f.getName();
                    String safeName = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    String uniqueName = (baseTs + i) + "_" + safeName;
                    Path dest = uploadsDir.resolve(uniqueName);
                    Files.copy(f.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    if (stored.length() > PDFS_PREFIX.length()) {
                        stored.append(';');
                    }
                    stored.append(dest.toAbsolutePath());
                }
            }

            if (taContenu != null) {
                taContenu.setText(stored.toString());
            }
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #16a34a;");
                lblMessage.setText(selectedFiles.size() == 1 ? "PDF importé" : (selectedFiles.size() + " PDFs importés"));
            }
        } catch (Exception ex) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText("Import PDF impossible");
            error.setContentText(ex.getMessage());
            error.showAndWait();
        }
    }

    private void retour() {
        try {
            UiState.selectedChapitreEditId = null;
            editChapitreId = null;
            WindowUtil.navigate(btnRetour, "/Formation/AfficherChapitre.fxml", "Chapitres");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
