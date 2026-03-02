package Controllers.Offre;

import Entities.Offre.OffreEmploi;
import Services.Ai.GeminiAiService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.util.List;

public class ComparerOffresController {
    
    @FXML private Label subtitleLabel;
    
    @FXML private Button ajouterOffreBtn;
    @FXML private Button annulerBtn;
    
    // Offre 1
    @FXML private VBox offre1Card;
    @FXML private Label titre1;
    @FXML private Label entreprise1;
    @FXML private Label localisation1;
    @FXML private Label contrat1;
    @FXML private Label experience1;
    @FXML private Label etudes1;
    @FXML private Label langues1;
    @FXML private Label secteur1;
    @FXML private Label postes1;
    @FXML private Label dateLimite1;
    @FXML private Label teletravail1;
    @FXML private Label status1;
    @FXML private Label salaire1;
    @FXML private Button btnEstimerSalaire1;
    
    // Offre 2
    @FXML private VBox offre2Card;
    @FXML private Label titre2;
    @FXML private Label entreprise2;
    @FXML private Label localisation2;
    @FXML private Label contrat2;
    @FXML private Label experience2;
    @FXML private Label etudes2;
    @FXML private Label langues2;
    @FXML private Label secteur2;
    @FXML private Label postes2;
    @FXML private Label dateLimite2;
    @FXML private Label teletravail2;
    @FXML private Label status2;
    @FXML private Label salaire2;
    @FXML private Button btnEstimerSalaire2;
    
    // Offre 3
    @FXML private VBox offre3Card;
    @FXML private Label titre3;
    @FXML private Label entreprise3;
    @FXML private Label localisation3;
    @FXML private Label contrat3;
    @FXML private Label experience3;
    @FXML private Label etudes3;
    @FXML private Label langues3;
    @FXML private Label secteur3;
    @FXML private Label postes3;
    @FXML private Label dateLimite3;
    @FXML private Label teletravail3;
    @FXML private Label status3;
    @FXML private Label salaire3;
    @FXML private Button btnEstimerSalaire3;
    
    private List<OffreEmploi> offresAComparer;
    private Runnable onAddOfferRequested;
    private GeminiAiService geminiService;
    
    private GeminiAiService getGeminiService() {
        if (geminiService == null) {
            geminiService = new GeminiAiService();
        }
        return geminiService;
    }
    
    public void setOffres(List<OffreEmploi> offres) {
        this.offresAComparer = offres;
        afficherComparaison();
        setCanAddOffer(offresAComparer.size() < 3);
    }
    
    public void setOnAddOfferRequested(Runnable r) {
        this.onAddOfferRequested = r;
    }
    
    public void setCanAddOffer(boolean canAdd) {
        if (ajouterOffreBtn != null) {
            ajouterOffreBtn.setVisible(canAdd);
            ajouterOffreBtn.setManaged(canAdd);
        }
    }
    
    private void afficherComparaison() {
        if (offresAComparer == null || offresAComparer.isEmpty()) {
            return;
        }
        
        subtitleLabel.setText(offresAComparer.size() + " offres sélectionnées");
        
        // Afficher Offre 1
        if (offresAComparer.size() >= 1) {
            remplirCarte(offresAComparer.get(0), 1);
        }
        
        // Afficher Offre 2
        if (offresAComparer.size() >= 2) {
            remplirCarte(offresAComparer.get(1), 2);
        }
        
        // Afficher Offre 3 (optionnel)
        if (offresAComparer.size() >= 3) {
            remplirCarte(offresAComparer.get(2), 3);
            offre3Card.setVisible(true);
            offre3Card.setManaged(true);
        }
    }
    
    private void remplirCarte(OffreEmploi o, int numero) {
        Label titre, entreprise, localisation, contrat, experience, etudes, 
              langues, secteur, postes, dateLimite, teletravail, status, salaire;
        
        switch (numero) {
            case 1:
                titre = titre1; entreprise = entreprise1; localisation = localisation1;
                contrat = contrat1; experience = experience1; etudes = etudes1;
                langues = langues1; secteur = secteur1; postes = postes1;
                dateLimite = dateLimite1; teletravail = teletravail1; status = status1;
                salaire = salaire1;
                break;
            case 2:
                titre = titre2; entreprise = entreprise2; localisation = localisation2;
                contrat = contrat2; experience = experience2; etudes = etudes2;
                langues = langues2; secteur = secteur2; postes = postes2;
                dateLimite = dateLimite2; teletravail = teletravail2; status = status2;
                salaire = salaire2;
                break;
            case 3:
                titre = titre3; entreprise = entreprise3; localisation = localisation3;
                contrat = contrat3; experience = experience3; etudes = etudes3;
                langues = langues3; secteur = secteur3; postes = postes3;
                dateLimite = dateLimite3; teletravail = teletravail3; status = status3;
                salaire = salaire3;
                break;
            default:
                return;
        }
        
        // Remplir les labels
        titre.setText(safe(o.getTitre(), "Sans titre"));
        entreprise.setText(safe(o.getNomEntreprise(), "Entreprise"));
        localisation.setText(getLocationText(o));
        contrat.setText(safe(o.getTypeContrat() != null ? o.getTypeContrat().getNom() : null, "Non spécifié"));
        experience.setText(safe(o.getNiveauExperience(), "Non spécifié"));
        etudes.setText(safe(o.getNiveauEtudes(), "Non spécifié"));
        langues.setText(safe(o.getLanguesRequises(), "Non spécifiées"));
        secteur.setText(safe(o.getSecteur() != null ? o.getSecteur().getNom() : null, "Non spécifié"));
        postes.setText(o.getNombrePoste() != null ? o.getNombrePoste() + " poste(s)" : "Non spécifié");
        dateLimite.setText(o.getDateLimiteCandidature() != null ? o.getDateLimiteCandidature().toString() : "Non spécifiée");
        teletravail.setText(Boolean.TRUE.equals(o.getTeletravail()) ? "Oui ✓" : "Non ✗");
        
        // Le salaire sera estimé uniquement lors du clic sur le bouton
        salaire.setText("");
        salaire.setVisible(false);
        salaire.setManaged(false);
        
        // Style du status - design moderne avec gradients premium
        status.setText(safe(o.getStatus(), "N/A").toUpperCase());
        String statusStyle = "-fx-background-radius: 10; -fx-padding: 12 24; -fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: white; ";
        if ("active".equalsIgnoreCase(o.getStatus())) {
            status.setStyle(statusStyle + "-fx-background-color: linear-gradient(135deg, #10b981 0%, #059669 100%);");
        } else if ("inactive".equalsIgnoreCase(o.getStatus()) || "ferme".equalsIgnoreCase(o.getStatus())) {
            status.setStyle(statusStyle + "-fx-background-color: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);");
        } else {
            status.setStyle(statusStyle + "-fx-background-color: linear-gradient(135deg, #64748b 0%, #475569 100%);");
        }
    }
    
    private void estimerSalaireAsync(OffreEmploi o, Label salaireLabel) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                try {
                    String secteur = o.getSecteur() != null ? o.getSecteur().getNom() : "";
                    String localisation = getLocationText(o);
                    String estimationIA = getGeminiService().estimateSalary(
                        o.getTitre(),
                        o.getDescription(),
                        secteur,
                        o.getNiveauExperience(),
                        localisation
                    );
                    // Si l'IA retourne N/A ou indisponible, utiliser le fallback local
                    if (estimationIA == null || estimationIA.equals("N/A") || estimationIA.contains("indisponible")) {
                        return estimerSalaireLocal(o);
                    }
                    return estimationIA;
                } catch (Exception e) {
                    // En cas d'erreur API, utiliser l'estimation locale
                    return estimerSalaireLocal(o);
                }
            }
        };
        
        task.setOnSucceeded(e -> {
            String estimation = task.getValue();
            Platform.runLater(() -> {
                if (estimation != null && !estimation.equals("N/A")) {
                    salaireLabel.setText("💰 " + estimation);
                    salaireLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: 700; -fx-background-color: #ecfdf5; -fx-padding: 4 8; -fx-background-radius: 6;");
                } else {
                    salaireLabel.setText("💰 Estimation non disponible");
                    salaireLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: 500;");
                }
            });
        });
        
        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                String fallback = estimerSalaireLocal(o);
                if (!fallback.equals("N/A")) {
                    salaireLabel.setText("💰 " + fallback);
                    salaireLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #059669; -fx-font-weight: 700; -fx-background-color: #ecfdf5; -fx-padding: 4 8; -fx-background-radius: 6;");
                } else {
                    salaireLabel.setText("💰 Estimation non disponible");
                    salaireLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af; -fx-font-weight: 500;");
                }
            });
        });
        
        new Thread(task).start();
    }
    
    /**
     * Estimation de salaire basée sur des règles locales (fallback quand l'IA n'est pas disponible)
     * Basée sur le secteur, niveau d'expérience, type de contrat et localisation
     */
    private String estimerSalaireLocal(OffreEmploi o) {
        try {
            String secteur = o.getSecteur() != null ? o.getSecteur().getNom().toLowerCase() : "";
            String niveau = o.getNiveauExperience() != null ? o.getNiveauExperience().toLowerCase() : "";
            String contrat = o.getTypeContrat() != null ? o.getTypeContrat().getNom().toLowerCase() : "";
            String titre = o.getTitre() != null ? o.getTitre().toLowerCase() : "";
            
            // Salaires de base par secteur (TND mensuel)
            int baseMin = 1200;
            int baseMax = 1800;
            
            // Ajustement par secteur
            if (secteur.contains("informatique") || secteur.contains("it") || secteur.contains("tech") || 
                titre.contains("développeur") || titre.contains("engineer") || titre.contains("data")) {
                baseMin = 2500; baseMax = 4500;
            } else if (secteur.contains("finance") || secteur.contains("banque") || secteur.contains("comptab")) {
                baseMin = 2000; baseMax = 3500;
            } else if (secteur.contains("marketing") || secteur.contains("communication")) {
                baseMin = 1500; baseMax = 2800;
            } else if (secteur.contains("rh") || secteur.contains("ressources humaines")) {
                baseMin = 1400; baseMax = 2500;
            } else if (secteur.contains("vente") || secteur.contains("commercial")) {
                baseMin = 1300; baseMax = 3500; // Variable avec commissions
            } else if (secteur.contains("éducation") || secteur.contains("formation")) {
                baseMin = 1200; baseMax = 2200;
            } else if (secteur.contains("santé") || secteur.contains("médical")) {
                baseMin = 1800; baseMax = 4000;
            }
            
            // Ajustement par niveau d'expérience
            if (niveau.contains("senior") || niveau.contains("expert") || niveau.contains("5+")) {
                baseMin *= 1.5; baseMax *= 1.6;
            } else if (niveau.contains("confirmé") || niveau.contains("mid") || niveau.contains("3-5")) {
                baseMin *= 1.2; baseMax *= 1.3;
            } else if (niveau.contains("junior") || niveau.contains("débutant") || niveau.contains("0-2")) {
                baseMin *= 0.8; baseMax *= 0.9;
            }
            
            // Ajustement par type de contrat
            if (contrat.contains("stage") || contrat.contains("internship")) {
                // Stage en informatique = mieux rémunéré
                if (secteur.contains("informatique") || secteur.contains("it") || secteur.contains("tech") ||
                    titre.contains("développeur") || titre.contains("ingénieur") || titre.contains("engineer") ||
                    titre.contains("data") || titre.contains("programmation") || titre.contains("node")) {
                    baseMin = 800; baseMax = 1500; // Stage tech
                } else {
                    baseMin = 500; baseMax = 800; // Stage standard
                }
            } else if (contrat.contains("cdd")) {
                baseMin *= 1.1; // Prime CDD
            }
            
            // Ajustement par localisation (Grand Tunis = plus élevé)
            String loc = getLocationText(o).toLowerCase();
            if (loc.contains("tunis") || loc.contains("ariana") || loc.contains("manouba") || 
                loc.contains("ben arous") || loc.contains("la marsa") || loc.contains("carthage")) {
                baseMin *= 1.15; baseMax *= 1.15;
            } else if (loc.contains("sfax") || loc.contains("sousse")) {
                baseMin *= 0.95; baseMax *= 0.95;
            }
            
            // Arrondir à la centaine
            baseMin = (baseMin / 100) * 100;
            baseMax = (baseMax / 100) * 100;
            
            return baseMin + "-" + baseMax + " TND";
        } catch (Exception e) {
            return "N/A";
        }
    }
    
    @FXML
    void estimerSalaire1() {
        if (offresAComparer != null && offresAComparer.size() >= 1) {
            btnEstimerSalaire1.setVisible(false);
            btnEstimerSalaire1.setManaged(false);
            salaire1.setVisible(true);
            salaire1.setManaged(true);
            estimerSalaireAsync(offresAComparer.get(0), salaire1);
        }
    }
    
    @FXML
    void estimerSalaire2() {
        if (offresAComparer != null && offresAComparer.size() >= 2) {
            btnEstimerSalaire2.setVisible(false);
            btnEstimerSalaire2.setManaged(false);
            salaire2.setVisible(true);
            salaire2.setManaged(true);
            estimerSalaireAsync(offresAComparer.get(1), salaire2);
        }
    }
    
    @FXML
    void estimerSalaire3() {
        if (offresAComparer != null && offresAComparer.size() >= 3) {
            btnEstimerSalaire3.setVisible(false);
            btnEstimerSalaire3.setManaged(false);
            salaire3.setVisible(true);
            salaire3.setManaged(true);
            estimerSalaireAsync(offresAComparer.get(2), salaire3);
        }
    }
    
    @FXML
    void fermer() {
        Stage stage = (Stage) titre1.getScene().getWindow();
        stage.close();
    }

    @FXML
    void annuler() {
        fermer();
    }

    @FXML
    void ajouterOffre() {
        if (onAddOfferRequested != null) {
            onAddOfferRequested.run();
        }
        fermer();
    }
    
    private String safe(String value, String defaultValue) {
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
    
    private String getLocationText(OffreEmploi o) {
        if (o == null || o.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String v = o.getLocalisation().getVille();
        String p = o.getLocalisation().getPays();
        StringBuilder sb = new StringBuilder();
        if (v != null && !v.trim().isEmpty()) sb.append(v);
        if (p != null && !p.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.length() > 0 ? sb.toString() : "Non spécifiée";
    }
}
