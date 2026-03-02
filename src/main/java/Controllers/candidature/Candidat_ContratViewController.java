package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Contrat;
import Services.candidature.ContratService;
import Services.candidature.EmailService;
import Utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.sql.Timestamp;
import java.time.LocalDateTime;


import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javafx.scene.control.ScrollPane;

public class Candidat_ContratViewController {

    @FXML private TextField tfNomCandidat, tfEmailCandidat;
    @FXML private TextField tfTypeContrat, tfTypeEmploi;
    @FXML private TextField tfEntreprise, tfSecteur;
    @FXML private TextField tfLocalisation, tfModeTravail;
    @FXML private TextField tfDateDebut, tfDateFin;
    @FXML private TextField tfSalaire, tfHoraire;
    @FXML private TextField tfPeriodeEssai, tfAvantages;

    @FXML private Label lblStatutContrat;

    @FXML private VBox sectionSignature;
    @FXML private VBox stepEnvoyer;
    @FXML private VBox stepSaisirCode;
    @FXML private VBox stepSigne;

    @FXML private Button btnEnvoyerCode;
    @FXML private TextField tfCode;
    @FXML private Label lblCodeInfo;
    @FXML private Label lblCodeError;
    @FXML private Label lblSignedAt;
    @FXML private Button btnBack;


    @FXML private Canvas signatureCanvas;
    @FXML private VBox stepSignature;
    @FXML private Label lblSignatureError;

    private Candidature candidature;
    private Contrat contrat;

    private final ContratService contratService = new ContratService();
    private final EmailService emailService = new EmailService();

    public void initContrat(Candidature c, Contrat ct) {
        this.candidature = c;
        this.contrat = ct;

        // Infos candidat
        tfNomCandidat.setText((safe(c.getPrenom()) + " " + safe(c.getNom())).trim());
        tfEmailCandidat.setText(safe(c.getEmail()));

        // Infos offre
        tfTypeContrat.setText(safe(ct.getType_contrat()));
        tfTypeEmploi.setText(safe(ct.getType_emploi()));
        tfEntreprise.setText(safe(ct.getNom_entreprise()));
        tfSecteur.setText(safe(ct.getSecteur()));
        tfLocalisation.setText(safe(ct.getLocalisation()));
        tfModeTravail.setText(safe(ct.getMode_travail()));

        // Détails contrat
        tfDateDebut.setText(ct.getDate_debut() != null ? ct.getDate_debut().toString() : "-");
        tfDateFin.setText(ct.getDate_fin() != null ? ct.getDate_fin().toString() : "CDI - Pas de date fin");
        tfSalaire.setText(ct.getSalaire() != null ? ct.getSalaire() + " DT/mois" : "Non précisé");
        tfHoraire.setText(safe(ct.getHoraire_travail()));
        tfPeriodeEssai.setText(safe(ct.getPeriode_essai()).isBlank() ? "Aucune" : safe(ct.getPeriode_essai()));
        tfAvantages.setText(safe(ct.getAvantages()).isBlank() ? "Aucun" : safe(ct.getAvantages()));

        // Statut
        updateStatutUI();
    }

    private void updateStatutUI() {
        String statut = safe(contrat.getStatut()).toUpperCase();

        if ("SIGNED".equals(statut)) {
            lblStatutContrat.setText("✅ CONTRAT SIGNE");
            lblStatutContrat.setStyle("-fx-font-size:15px; -fx-font-weight:bold; " +
                    "-fx-background-color:#dcfce7; -fx-text-fill:#16a34a; " +
                    "-fx-background-radius:20; -fx-padding: 8 20;");

            // Montrer section signé
            stepEnvoyer.setVisible(false);
            stepEnvoyer.setManaged(false);
            stepSaisirCode.setVisible(false);
            stepSaisirCode.setManaged(false);
            stepSigne.setVisible(true);
            stepSigne.setManaged(true);

            if (contrat.getSigned_at() != null) {
                lblSignedAt.setText("Signé le : " + contrat.getSigned_at().toString());
            }

        } else {
            lblStatutContrat.setText("⏳ EN ATTENTE DE SIGNATURE");
            lblStatutContrat.setStyle("-fx-font-size:15px; -fx-font-weight:bold; " +
                    "-fx-background-color:#ede9fe; -fx-text-fill:#4f46e5; " +
                    "-fx-background-radius:20; -fx-padding: 8 20;");

            stepEnvoyer.setVisible(true);
            stepEnvoyer.setManaged(true);
            stepSaisirCode.setVisible(false);
            stepSaisirCode.setManaged(false);
            stepSigne.setVisible(false);
            stepSigne.setManaged(false);
        }
    }

    @FXML
    private void envoyerCode() {
        String email = safe(candidature.getEmail());
        if (email.isBlank()) {
            new Alert(Alert.AlertType.ERROR, "Email du candidat introuvable.").show();
            return;
        }

        btnEnvoyerCode.setDisable(true);
        btnEnvoyerCode.setText("⏳ Envoi en cours...");

        new Thread(() -> {
            try {
                String code = EmailService.genererCode();

                // Expiration : 15 minutes
                Timestamp expiresAt = Timestamp.valueOf(LocalDateTime.now().plusMinutes(15));
                contratService.sauvegarderCode(contrat.getId(), code, expiresAt);

                String nomCandidat = (safe(candidature.getPrenom()) + " " + safe(candidature.getNom())).trim();
                emailService.envoyerCodeSignature(email, code, nomCandidat);

                Platform.runLater(() -> {
                    btnEnvoyerCode.setText("📧 Envoyer le code par email");
                    btnEnvoyerCode.setDisable(false);

                    // Passer à l'étape saisir code
                    stepEnvoyer.setVisible(false);
                    stepEnvoyer.setManaged(false);
                    stepSaisirCode.setVisible(true);
                    stepSaisirCode.setManaged(true);

                    lblCodeInfo.setText("Code envoye a : " + email + "\n(Valable 15 minutes)");
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    btnEnvoyerCode.setText("📧 Envoyer le code par email");
                    btnEnvoyerCode.setDisable(false);
                    new Alert(Alert.AlertType.ERROR,
                            "Impossible d'envoyer l'email.\n" + ex.getMessage()).show();
                });
            }
        }).start();
    }


    /*

    @FXML
    private void validerEtSigner() {
        lblCodeError.setText("");
        String codeEntré = tfCode.getText().trim();

        if (codeEntré.isBlank()) {
            lblCodeError.setText("Veuillez entrer le code recu par email.");
            return;
        }

        boolean valide = contratService.validerCode(contrat.getId(), codeEntré);

        if (!valide) {
            lblCodeError.setText("Code invalide ou expire. Veuillez renvoyer un nouveau code.");
            return;
        }

        // Signer le contrat
        contratService.signerContrat(contrat.getId());
        contrat.setStatut("SIGNED");
        contrat.setSigned_at(Timestamp.valueOf(LocalDateTime.now()));

        // Mettre à jour UI
        stepSaisirCode.setVisible(false);
        stepSaisirCode.setManaged(false);
        stepSigne.setVisible(true);
        stepSigne.setManaged(true);

        lblStatutContrat.setText("✅ CONTRAT SIGNE");
        lblStatutContrat.setStyle("-fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-background-color:#dcfce7; -fx-text-fill:#16a34a; " +
                "-fx-background-radius:20; -fx-padding: 8 20;");

        lblSignedAt.setText("Signe le : " + contrat.getSigned_at().toString());

        new Alert(Alert.AlertType.INFORMATION,
                "Contrat signe avec succes ! Bienvenue dans l'equipe !").show();
    }
    */

    //jdiddd

    @FXML
    private void validerCode() {
        lblCodeError.setText("");
        String codeEntré = tfCode.getText().trim();

        if (codeEntré.isBlank()) {
            lblCodeError.setText("Veuillez entrer le code recu par email.");
            return;
        }

        boolean valide = contratService.validerCode(contrat.getId(), codeEntré);

        if (!valide) {
            lblCodeError.setText("Code invalide ou expire. Veuillez renvoyer un nouveau code.");
            return;
        }

        // Code valide → passer à l'étape signature
        stepSaisirCode.setVisible(false);
        stepSaisirCode.setManaged(false);
        stepSignature.setVisible(true);
        stepSignature.setManaged(true);

        // Initialiser le canvas
        initSignatureCanvas();
    }

   /* private void initSignatureCanvas() {
        GraphicsContext gc = signatureCanvas.getGraphicsContext2D();

        // Fond blanc
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());

        // Ligne guide
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1);
        gc.strokeLine(20, signatureCanvas.getHeight() - 40,
                signatureCanvas.getWidth() - 20,
                signatureCanvas.getHeight() - 40);

        // Texte guide
        gc.setFill(Color.web("#cbd5e1"));
        gc.fillText("Signez ici...", 20, signatureCanvas.getHeight() - 50);

        // Dessin à la souris
        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        signatureCanvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        signatureCanvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            gc.moveTo(e.getX(), e.getY());
        });

        signatureCanvas.setOnMouseReleased(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
            gc.closePath();
        });
    }
*/
    //jdiddd
   private GraphicsContext gc;

    private void initSignatureCanvas() {
        gc = signatureCanvas.getGraphicsContext2D();

        // Fond blanc
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, signatureCanvas.getWidth(), signatureCanvas.getHeight());

        // Ligne guide
        gc.setStroke(Color.web("#e2e8f0"));
        gc.setLineWidth(1);
        gc.strokeLine(20, signatureCanvas.getHeight() - 40,
                signatureCanvas.getWidth() - 20,
                signatureCanvas.getHeight() - 40);

        // Texte guide
        gc.setFill(Color.web("#cbd5e1"));
        gc.fillText("Signez ici...", 20, signatureCanvas.getHeight() - 50);

        // Style du trait
        gc.setStroke(Color.web("#1e293b"));
        gc.setLineWidth(2.5);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    @FXML
    private void onCanvasMousePressed(javafx.scene.input.MouseEvent e) {
        e.consume();
        gc.beginPath();
        gc.moveTo(e.getX(), e.getY());
        gc.stroke();
    }

    @FXML
    private void onCanvasMouseDragged(javafx.scene.input.MouseEvent e) {
        e.consume();
        gc.lineTo(e.getX(), e.getY());
        gc.stroke();
        gc.moveTo(e.getX(), e.getY());
    }

    @FXML
    private void onCanvasMouseReleased(javafx.scene.input.MouseEvent e) {
        e.consume();
        gc.lineTo(e.getX(), e.getY());
        gc.stroke();
        gc.closePath();
    }




    @FXML
    private void effacerSignature() {
        initSignatureCanvas();
        lblSignatureError.setText("");
    }


    @FXML
    private void confirmerSignature() {
        lblSignatureError.setText("");

        // Vérifier que le canvas n'est pas vide (fond blanc seulement)
        WritableImage snapshot = signatureCanvas.snapshot(null, null);
        if (isCanvasVide(snapshot)) {
            lblSignatureError.setText("Veuillez dessiner votre signature avant de confirmer.");
            return;
        }

        // Convertir canvas en Base64
        String signatureBase64 = null;
        try {
            WritableImage image = signatureCanvas.snapshot(null, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", baos);
            signatureBase64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            lblSignatureError.setText("Erreur lors de la capture de la signature.");
            return;
        }

        // Signer le contrat avec la signature
        contratService.signerContrat(contrat.getId(), signatureBase64);
        contrat.setStatut("SIGNED");
        contrat.setSigned_at(Timestamp.valueOf(LocalDateTime.now()));
        contrat.setSignature_image(signatureBase64);

        // Mettre à jour UI
        stepSignature.setVisible(false);
        stepSignature.setManaged(false);
        stepSigne.setVisible(true);
        stepSigne.setManaged(true);

        lblStatutContrat.setText("✅ CONTRAT SIGNE");
        lblStatutContrat.setStyle("-fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-background-color:#dcfce7; -fx-text-fill:#16a34a; " +
                "-fx-background-radius:20; -fx-padding: 8 20;");

        lblSignedAt.setText("Signe le : " + contrat.getSigned_at().toString());

        new Alert(Alert.AlertType.INFORMATION,
                "Contrat signe avec succes ! Bienvenue dans l'equipe !").show();
    }


    private boolean isCanvasVide(WritableImage image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        var reader = image.getPixelReader();

        // Vérifie un échantillon de pixels — si tous blancs = vide
        int pixelsNonBlancs = 0;
        for (int x = 0; x < width; x += 5) {
            for (int y = 0; y < height; y += 5) {
                Color color = reader.getColor(x, y);
                if (color.getRed() < 0.99 || color.getGreen() < 0.99 || color.getBlue() < 0.99) {
                    pixelsNonBlancs++;
                }
            }
        }
        return pixelsNonBlancs < 10;
    }




    @FXML
    private void retour() {
        try {
            if (btnBack.getScene() == null) return;
            Parent root = btnBack.getScene().getRoot();
            Pane pane = (Pane) root.lookup("#pageContainer");
            if (pane == null) pane = (Pane) root.lookup("#contentArea");
            if (pane == null) return;

            Parent view = FXMLLoader.load(
                    getClass().getResource("/candidaturefxml/MesCandidatures.fxml")
            );
            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}