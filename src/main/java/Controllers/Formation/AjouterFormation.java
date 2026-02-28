package Controllers.Formation;

import Entities.Formation.Formation;
import Services.Formation.FormationServices;
import Services.Formation.FormationPurchaseService;
import Services.User.UserService;
import Utils.EmailService;
import Utils.UiState;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import Utils.WindowUtil;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import Entities.User;

public class AjouterFormation {
    
    @FXML
    private TextField tfLieu;

    @FXML
    private Button btnCarte;
    
    @FXML
    private TextField tfTitre;
    
    @FXML
    private TextArea tfDescription;
    
    @FXML
    private TextField tfDomaine;
    
    @FXML
    private DatePicker dpDateDebut;

    @FXML
    private DatePicker dpDateFin;

    @FXML
    private CheckBox cbPayante;

    @FXML
    private TextField tfPrix;
    
    @FXML
    private Button btnAjouter;

    @FXML
    private Button btnAjouterCour;
    
    @FXML
    private Button btnAnnuler;

    @FXML
    private Button btnAfficher;
    
    @FXML
    private Label lblMessage;

    @FXML
    private VBox cardAdd;

    @FXML
    private VBox titleBox;

    @FXML
    private GridPane formGrid;

    @FXML
    private HBox actionBar;
    
    private FormationServices formationServices;
    
    public void initialize() {
        formationServices = new FormationServices();

        if (UiState.selectedLieu != null && !UiState.selectedLieu.isBlank()) {
            tfLieu.setText(UiState.selectedLieu);
        }
        
        // Actions des boutons
        btnAjouter.setOnAction(event -> ajouterFormation());
        btnAnnuler.setOnAction(event -> annuler());
        btnAfficher.setOnAction(event -> afficherFormations());
        btnCarte.setOnAction(event -> ouvrirCarte());
        if (btnAjouterCour != null) {
            btnAjouterCour.setOnAction(event -> ouvrirAjouterCour());
        }

        playIntroAnimation();

        dpDateDebut.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(empty);
                    return;
                }
                setDisable(date.isBefore(LocalDate.now()));
            }
        });

        dpDateFin.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setDisable(empty);
                    return;
                }
                setDisable(date.isBefore(LocalDate.now()));
            }
        });
    }

    private void playIntroAnimation() {
        if (cardAdd == null) {
            return;
        }

        animateNode(cardAdd, 360, 14);

        if (titleBox == null || formGrid == null || actionBar == null) {
            return;
        }

        titleBox.setOpacity(0);
        titleBox.setTranslateY(10);
        formGrid.setOpacity(0);
        formGrid.setTranslateY(10);
        actionBar.setOpacity(0);
        actionBar.setTranslateY(10);

        PauseTransition p1 = new PauseTransition(javafx.util.Duration.millis(120));
        PauseTransition p2 = new PauseTransition(javafx.util.Duration.millis(120));

        ParallelTransition t1 = buildFadeSlide(titleBox, 260, 10);
        ParallelTransition t2 = buildFadeSlide(formGrid, 280, 10);
        ParallelTransition t3 = buildFadeSlide(actionBar, 240, 10);

        SequentialTransition seq = new SequentialTransition(p1, t1, p2, t2, t3);
        seq.play();
    }

    private void animateNode(VBox node, int durationMs, double fromY) {
        node.setOpacity(0);
        node.setTranslateY(fromY);

        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(durationMs), node);
        tt.setFromY(fromY);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(ft, tt).play();
    }

    private ParallelTransition buildFadeSlide(javafx.scene.Node node, int durationMs, double fromY) {
        FadeTransition ft = new FadeTransition(javafx.util.Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition tt = new TranslateTransition(javafx.util.Duration.millis(durationMs), node);
        tt.setFromY(fromY);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        return new ParallelTransition(ft, tt);
    }

    private void ouvrirCarte() {
        try {
            WindowUtil.navigate(btnCarte, "/Formation/mapPicker.fxml", "Choisir le lieu");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Erreur d'ouverture: " + e.getMessage());
        }
    }
    
    private void ajouterFormation() {
        try {
            // Validation des champs
            if (tfLieu.getText().isEmpty() || tfTitre.getText().isEmpty() || 
                tfDescription.getText().isEmpty() || tfDomaine.getText().isEmpty() ||
                dpDateDebut.getValue() == null || dpDateFin.getValue() == null) {
                
                lblMessage.setText("Veuillez remplir tous les champs");
                return;
            }

            if (dpDateDebut.getValue().isBefore(LocalDate.now())) {
                lblMessage.setStyle("-fx-text-fill: red;");
                lblMessage.setText("La date début doit être aujourd'hui ou une date future");
                return;
            }

            if (dpDateFin.getValue().isBefore(dpDateDebut.getValue())) {
                lblMessage.setStyle("-fx-text-fill: red;");
                lblMessage.setText("La date fin doit être après la date début");
                return;
            }
            
            // Conversion des données
            String lieu = tfLieu.getText();
            String titre = tfTitre.getText();
            String description = tfDescription.getText();
            String domaine = tfDomaine.getText();
            Date dateDebut = Date.valueOf(dpDateDebut.getValue());
            Date dateFin = Date.valueOf(dpDateFin.getValue());

            boolean payante = cbPayante != null && cbPayante.isSelected();
            float prix = 0f;
            if (payante) {
                if (tfPrix == null || tfPrix.getText().isBlank()) {
                    lblMessage.setStyle("-fx-text-fill: red;");
                    lblMessage.setText("Veuillez saisir le prix de la formation");
                    return;
                }
                try {
                    prix = Float.parseFloat(tfPrix.getText().trim());
                } catch (NumberFormatException ex) {
                    lblMessage.setStyle("-fx-text-fill: red;");
                    lblMessage.setText("Prix invalide");
                    return;
                }
                if (prix <= 0f) {
                    lblMessage.setStyle("-fx-text-fill: red;");
                    lblMessage.setText("Prix invalide");
                    return;
                }
            }

            Formation formation = new Formation(lieu, titre, description, domaine, dateDebut, dateFin, payante, prix);
            int formationId = formationServices.ajouterFormationAndReturnId(formation);

            final boolean payanteFinal = payante;
            final float prixFinal = prix;
            final int formationIdFinal = formationId;
            final String lieuFinal = lieu;
            final String titreFinal = titre;
            final String domaineFinal = domaine;
            final LocalDate dateDebutFinal = dpDateDebut.getValue();
            final LocalDate dateFinFinal = dpDateFin.getValue();

            new Thread(() -> {
                try {
                    UserService userService = new UserService();
                    List<User> candidats = userService.getUsersByRole("CANDIDAT");

                    String subject = "Nouvelle formation: " + titreFinal;
                    String safeTitre = escapeHtml(titreFinal);
                    String safeDomaine = escapeHtml(domaineFinal);
                    String safeLieu = escapeHtml(lieuFinal);
                    String safeDateDebut = escapeHtml(String.valueOf(dateDebutFinal));
                    String safeDateFin = escapeHtml(String.valueOf(dateFinFinal));

                    String paySection = "";
                    if (payanteFinal) {
                        paySection = "" +
                                "<div style='margin-top:16px;'>" +
                                "  <div style='font-size:14px;color:#374151;margin-bottom:12px;'>Prix: <b>" + escapeHtml(String.valueOf(prixFinal)) + " DT</b></div>" +
                                "  <div style='font-size:12px;color:#6b7280;margin-bottom:14px;'>Le lien de paiement est personnel (lié à votre compte).</div>" +
                                "</div>";
                    }

                    String baseHtml = "" +
                            "<!doctype html>" +
                            "<html lang='fr'>" +
                            "<head><meta charset='utf-8'><meta name='viewport' content='width=device-width, initial-scale=1'></head>" +
                            "<body style='margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#111827;'>" +
                            "<div style='padding:24px 12px;'>" +
                            "  <div style='max-width:640px;margin:0 auto;background:#ffffff;border-radius:14px;overflow:hidden;border:1px solid #e5e7eb;'>" +
                            "    <div style='background:linear-gradient(135deg,#4f46e5,#06b6d4);padding:22px 22px;'>" +
                            "      <div style='font-size:14px;opacity:0.95;color:#e0f2fe;'>CareerLink</div>" +
                            "      <div style='font-size:22px;font-weight:700;line-height:1.25;color:#ffffff;margin-top:6px;'>Nouvelle formation disponible</div>" +
                            "    </div>" +
                            "    <div style='padding:22px 22px 8px 22px;'>" +
                            "      <div style='font-size:16px;line-height:1.5;margin:0 0 14px 0;color:#111827;'>Une nouvelle formation vient d'être ajoutée.</div>" +
                            "      <div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:12px;padding:16px;'>" +
                            "        <div style='font-size:18px;font-weight:700;color:#111827;margin-bottom:10px;'>" + safeTitre + "</div>" +
                            "        <table role='presentation' cellpadding='0' cellspacing='0' style='width:100%;border-collapse:collapse;font-size:14px;color:#374151;'>" +
                            "          <tr><td style='padding:6px 0;width:140px;color:#6b7280;'>Domaine</td><td style='padding:6px 0;font-weight:600;'>" + safeDomaine + "</td></tr>" +
                            "          <tr><td style='padding:6px 0;width:140px;color:#6b7280;'>Lieu</td><td style='padding:6px 0;font-weight:600;'>" + safeLieu + "</td></tr>" +
                            "          <tr><td style='padding:6px 0;width:140px;color:#6b7280;'>Date début</td><td style='padding:6px 0;font-weight:600;'>" + safeDateDebut + "</td></tr>" +
                            "          <tr><td style='padding:6px 0;width:140px;color:#6b7280;'>Date fin</td><td style='padding:6px 0;font-weight:600;'>" + safeDateFin + "</td></tr>" +
                            "        </table>" +
                            paySection +
                            "{{PAY_BUTTON}}" +
                            "      </div>" +
                            "    </div>" +
                            "    <div style='padding:0 22px 22px 22px;color:#6b7280;font-size:12px;line-height:1.4;'>" +
                            "      Vous recevez cet email car vous êtes inscrit en tant que candidat sur CareerLink." +
                            "    </div>" +
                            "  </div>" +
                            "</div>" +
                            "</body></html>";

                    if (!payanteFinal) {
                        java.util.ArrayList<String> emails = new java.util.ArrayList<>();
                        for (User u : candidats) {
                            if (u != null && u.getEmail() != null && !u.getEmail().isBlank()) {
                                emails.add(u.getEmail().trim());
                            }
                        }
                        EmailService.sendBccHtml(emails, subject, baseHtml.replace("{{PAY_BUTTON}}", ""));
                        return;
                    }

                    FormationPurchaseService purchaseService = new FormationPurchaseService();
                    for (User u : candidats) {
                        if (u == null || u.getEmail() == null || u.getEmail().isBlank()) {
                            continue;
                        }
                        int userId = u.getId();
                        if (userId <= 0 || formationIdFinal <= 0) {
                            continue;
                        }

                        try {
                            // On ne met PLUS le lien Stripe direct dans le mail.
                            // On met un lien vers notre serveur qui VERIFIE avant de rediriger.
                            String redirectUrl = "http://localhost:4242/pay-redirect?user_id=" + userId + "&formation_id=" + formationIdFinal;
                            
                            String button = "" +
                                    "<div style='margin-top:18px;text-align:center;'>" +
                                    "  <a href='" + escapeHtml(redirectUrl) + "' style='display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 18px;border-radius:10px;font-weight:700;font-size:14px;'>Payer maintenant</a>" +
                                    "</div>";

                            String html = baseHtml.replace("{{PAY_BUTTON}}", button);
                            EmailService.sendToHtml(u.getEmail().trim(), subject, html);
                        } catch (Exception ex) {
                            System.out.println("[WARN] Email to " + u.getEmail() + " failed: " + ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("[WARN] Email notification failed: " + ex.getMessage());
                }
            }).start();
            
            // Message de succès
            lblMessage.setStyle("-fx-text-fill: green;");
            lblMessage.setText("Formation ajoutée avec succès!");
            
            // Vider les champs après ajout
            if (cbPayante != null) cbPayante.setSelected(false);
            if (tfPrix != null) tfPrix.clear();

            
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Erreur lors de l'ajout: " + e.getMessage());
        }
    }
    
    private void annuler() {
        tfLieu.clear();
        tfTitre.clear();
        tfDescription.clear();
        tfDomaine.clear();
        dpDateDebut.setValue(null);
        dpDateFin.setValue(null);
        if (cbPayante != null) cbPayante.setSelected(false);
        if (tfPrix != null) tfPrix.clear();
    }

    private void afficherFormations() {
        try {
            WindowUtil.navigate(btnAfficher, "/Formation/afficherFormation.fxml", "Liste des formations");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Erreur d'ouverture: " + e.getMessage());
        }
    }

    private void ouvrirAjouterCour() {
        try {
            WindowUtil.navigate(btnAjouterCour, "/Formation/AjouterCour.fxml", "Ajouter un cours");
        } catch (Exception e) {
            lblMessage.setStyle("-fx-text-fill: red;");
            lblMessage.setText("Erreur d'ouverture: " + e.getMessage());
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
