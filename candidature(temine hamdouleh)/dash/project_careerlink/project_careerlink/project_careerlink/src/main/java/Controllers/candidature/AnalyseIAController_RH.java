package Controllers.candidature;

import Entities.candidature.AnalyseResult;
import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Services.candidature.AgentService;
import Services.candidature.CandidatureService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyseIAController_RH implements Initializable {

    // ── Top bar ───────────────────────────────────────────
    @FXML private Button btnRetour;

    // ── Carte candidat ────────────────────────────────────
    @FXML private Label lblInitiales;
    @FXML private Label lblNom;
    @FXML private Label lblOffre;
    @FXML private Label lblCompatibilite;
    @FXML private Label lblRecommandation;

    // ── Compétences techniques ────────────────────────────
    @FXML private Label       lblPctCompetences;
    @FXML private ProgressBar pbCompetences;
    @FXML private VBox        vboxCompetenceItems;
    @FXML private Label       lblTauxCouverture;
    @FXML private Label       lblCompetencesCommentaire;

    // ── Expérience gauche ─────────────────────────────────
    @FXML private Label       lblPctExperience;
    @FXML private ProgressBar pbExperience;
    @FXML private Label       lblAnneesCandidat;
    @FXML private Label       lblAnneesRequis;
    @FXML private Label       lblTypeExperience;
    @FXML private Label       lblExperienceCommentaire;
    @FXML private VBox        vboxExperienceDb;
    @FXML private PieChart    chartProbReussite;
    @FXML private LineChart<Number, Number> chartCourbeReussite;
    @FXML private Label       lblProbReussite;
    @FXML private Label       lblProbRisque;

    // ── Expérience droite ─────────────────────────────────
    @FXML private Label       lblPctExperienceR;
    // (contenu remplacé par graphique probabilité)

    // ── Diplôme & Certifications ──────────────────────────
    @FXML private Label       lblPctDiplome;
    @FXML private ProgressBar pbDiplome;
    @FXML private Circle      circleDiplome;
    @FXML private Label       lblDiplome;
    @FXML private Label       lblNiveauRequis;
    @FXML private VBox        vboxCertifications;
    @FXML private Label       lblDiplomeCommentaire;

    // ── Soft Skills ───────────────────────────────────────
    @FXML private Label       lblPctSoftSkills;
    @FXML private ProgressBar pbSoftSkills;
    @FXML private VBox        vboxSoftSkills;
    @FXML private Label       lblSoftSkillsCommentaire;

    // ── Score global (droite) ─────────────────────────────
    @FXML private Label  lblScoreGlobal;
    @FXML private Label  lblScoreCheck;
    @FXML private HBox   hboxBadge;
    @FXML private Label  lblBadgeCompatibilite;
    @FXML private Label  lblRecommandationDroite;
    @FXML private Label  lblOffreTitre;

    // ── Points forts / à améliorer ───────────────────────
    @FXML private VBox  vboxPointsForts;
    @FXML private VBox  vboxPointsAmeliorer;
    @FXML private Label lblClassement;
    @FXML private Label lblClassementSub;

    // ── Loading overlay ───────────────────────────────────
    @FXML private VBox        loadingOverlay;
    @FXML private ProgressBar pbLoading;
    @FXML private Label       lblLoadingStatus;

    // ── Données ───────────────────────────────────────────
    private Candidature candidature;
    private List<Experience> experiencesDb;
    private List<Certification> certificationsDb;

    private AnalyseResult lastAnalyse;

    private final CandidatureService candidatureService = new CandidatureService();

    private final AgentService    agentService = new AgentService();
    private final ExecutorService executor     = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("analyse-ia-worker");
        return t;
    });

    // ════════════════════════════════════════════════════════
    // Init
    // ════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        showLoading(false);
    }

    private void remplirGraphProbabilite(int scoreGlobal) {
        if (chartProbReussite == null) return;
        int success = Math.max(0, Math.min(100, scoreGlobal));
        int risk = 100 - success;

        chartProbReussite.getData().clear();
        PieChart.Data dSuccess = new PieChart.Data("Succès", success);
        PieChart.Data dRisk = new PieChart.Data("Risque", risk);
        chartProbReussite.getData().addAll(dSuccess, dRisk);

        setLabel(lblProbReussite, "Succès : " + success + "%");
        setLabel(lblProbRisque, "Risque : " + risk + "%");

        Platform.runLater(() -> {
            if (dSuccess.getNode() != null) dSuccess.getNode().setStyle("-fx-pie-color: #10B981;");
            if (dRisk.getNode() != null) dRisk.getNode().setStyle("-fx-pie-color: #EF4444;");
        });

        remplirCourbeProbabilite(success);
    }

    private void remplirCourbeProbabilite(int success) {
        if (chartCourbeReussite == null) return;

        chartCourbeReussite.setLegendVisible(false);
        chartCourbeReussite.setCreateSymbols(false);
        chartCourbeReussite.setAnimated(false);

        chartCourbeReussite.getData().clear();
        XYChart.Series<Number, Number> s = new XYChart.Series<>();

        // Courbe simple "tendance" vers la réussite (0..100)
        // Plus le score est élevé, plus la courbe monte vite.
        double k = 0.6 + (success / 100.0) * 0.8; // 0.6..1.4
        for (int x = 0; x <= 100; x += 10) {
            double t = x / 100.0;
            double y = 100.0 * Math.pow(t, 1.0 / k);
            // Clamper autour du score final
            y = Math.min(100.0, y * (0.6 + (success / 100.0) * 0.6));
            s.getData().add(new XYChart.Data<>(x, y));
        }
        // Point final à la valeur de succès
        s.getData().add(new XYChart.Data<>(100, success));

        chartCourbeReussite.getData().add(s);

        Platform.runLater(() -> {
            if (s.getNode() != null) {
                s.getNode().setStyle("-fx-stroke: #10B981; -fx-stroke-width: 3px;");
            }
        });
    }

    @FXML
    private void handleVoirAnalyseDetaillee() {
        String text = buildRhReport(lastAnalyse);
        if ((text == null || text.isBlank()) && lastAnalyse != null && lastAnalyse._raw_json != null) {
            text = lastAnalyse._raw_json;
        }
        if (text == null || text.isBlank()) text = "Analyse non disponible pour le moment.";

        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);

        BorderPane root = new BorderPane(area);
        root.setPadding(new Insets(12));

        Stage stage = new Stage();
        stage.setTitle("Analyse IA détaillée");
        stage.setScene(new Scene(root, 800, 600));
        stage.initOwner(btnRetour != null && btnRetour.getScene() != null ? btnRetour.getScene().getWindow() : null);
        stage.show();
    }

    private String buildRhReport(AnalyseResult r) {
        if (r == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Analyse IA – Rapport RH\n");
        sb.append("Candidat : ").append(safe(r._prenom)).append(" ").append(safe(r._nom)).append("\n");
        sb.append("Poste    : ").append(safe(r._offre_titre)).append("\n\n");

        sb.append("Résumé\n");
        sb.append("- Score global : ").append(r.score_global).append("%\n");
        sb.append("- Compatibilité : ").append(safe(r.compatibilite)).append("\n");
        sb.append("- Recommandation : ").append(safe(r.recommandation)).append("\n");
        sb.append("- Classement : ").append(safe(r.classement));
        if (r.classement_detail != null && !r.classement_detail.isBlank()) {
            sb.append(" ( ").append(r.classement_detail.trim()).append(" )");
        }
        sb.append("\n\n");

        sb.append("Compétences techniques\n");
        if (r.competences_techniques != null) {
            sb.append("- Score : ").append(r.competences_techniques.score).append("%\n");
            sb.append("- Taux de couverture : ").append(r.competences_techniques.taux_couverture).append("%\n");
            if (r.competences_techniques.items != null && !r.competences_techniques.items.isEmpty()) {
                for (AnalyseResult.CompetencesTechniques.Item it : r.competences_techniques.items) {
                    if (it == null || it.nom == null || it.nom.isBlank()) continue;
                    sb.append("  - ")
                            .append(it.present ? "✓ " : "✗ ")
                            .append(it.nom.trim());
                    if (it.niveau != null && !it.niveau.isBlank()) {
                        sb.append(" (niveau: ").append(it.niveau.trim()).append(")");
                    }
                    sb.append("\n");
                }
            }
            if (r.competences_techniques.commentaire != null && !r.competences_techniques.commentaire.isBlank()) {
                sb.append("- Avis IA : ").append(r.competences_techniques.commentaire.trim()).append("\n");
            }
        } else {
            sb.append("(Non disponible)\n");
        }
        sb.append("\n");

        sb.append("Expérience\n");
        if (r.experience != null) {
            sb.append("- Score : ").append(r.experience.score).append("%\n");
            sb.append("- Années : ").append(r.experience.annees_candidat)
                    .append(" (requis : ").append(r.experience.annees_requis).append(")\n");
            sb.append("- Type : ").append(safe(r.experience.type_experience)).append("\n");
            if (r.experience.commentaire != null && !r.experience.commentaire.isBlank()) {
                sb.append("- Avis IA : ").append(r.experience.commentaire.trim()).append("\n");
            }
        } else {
            sb.append("(Non disponible)\n");
        }
        sb.append("\n");

        sb.append("Diplôme & Certifications\n");
        if (r.diplome_certifications != null) {
            sb.append("- Score : ").append(r.diplome_certifications.score).append("%\n");
            sb.append("- Diplôme : ").append(safe(r.diplome_certifications.diplome)).append("\n");
            sb.append("- Niveau requis : ").append(safe(r.diplome_certifications.niveau_requis)).append("\n");
            sb.append("- Diplôme valide : ").append(r.diplome_certifications.diplome_valide ? "Oui" : "Non").append("\n");
            if (r.diplome_certifications.certifications != null && !r.diplome_certifications.certifications.isEmpty()) {
                sb.append("- Certifications (selon l'offre) :\n");
                for (AnalyseResult.DiplomeCertifications.Certification c : r.diplome_certifications.certifications) {
                    if (c == null || c.nom == null || c.nom.isBlank()) continue;
                    sb.append("  - ").append(c.presente ? "✓ " : "✗ ").append(c.nom.trim()).append("\n");
                }
            }
            if (r.diplome_certifications.commentaire != null && !r.diplome_certifications.commentaire.isBlank()) {
                sb.append("- Avis IA : ").append(r.diplome_certifications.commentaire.trim()).append("\n");
            }
        } else {
            sb.append("(Non disponible)\n");
        }
        sb.append("\n");

        sb.append("Soft skills\n");
        if (r.soft_skills != null) {
            sb.append("- Score : ").append(r.soft_skills.score).append("%\n");
            if (r.soft_skills.items != null && !r.soft_skills.items.isEmpty()) {
                sb.append("- Items : ");
                for (int i = 0; i < r.soft_skills.items.size(); i++) {
                    String s = r.soft_skills.items.get(i);
                    if (s == null || s.isBlank()) continue;
                    if (sb.charAt(sb.length() - 1) != ' ' && sb.charAt(sb.length() - 1) != ':') sb.append(", ");
                    sb.append(s.trim());
                }
                sb.append("\n");
            }
            if (r.soft_skills.commentaire != null && !r.soft_skills.commentaire.isBlank()) {
                sb.append("- Avis IA : ").append(r.soft_skills.commentaire.trim()).append("\n");
            }
        } else {
            sb.append("(Non disponible)\n");
        }
        sb.append("\n");

        sb.append("Points forts\n");
        if (r.points_forts != null && !r.points_forts.isEmpty()) {
            for (AnalyseResult.PointFort pf : r.points_forts) {
                if (pf == null) continue;
                String line = safe(pf.texte);
                if (line.isBlank()) line = safe(pf.mot_cle);
                if (line.isBlank()) continue;
                sb.append("- ").append(line.trim()).append("\n");
            }
        } else {
            sb.append("(Aucun)\n");
        }
        sb.append("\n");

        sb.append("Points à améliorer\n");
        if (r.points_ameliorer != null && !r.points_ameliorer.isEmpty()) {
            for (AnalyseResult.PointAmeliorer pa : r.points_ameliorer) {
                if (pa == null || pa.texte == null || pa.texte.isBlank()) continue;
                sb.append("- ").append(pa.texte.trim()).append("\n");
            }
        } else {
            sb.append("(Aucun)\n");
        }

        if (r.analyse_globale != null && !r.analyse_globale.isBlank()) {
            sb.append("\nAnalyse globale\n");
            sb.append(r.analyse_globale.trim()).append("\n");
        }

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════
    // Point d'entrée : appelé depuis la liste de candidatures
    // ════════════════════════════════════════════════════════

    /**
     * Injecter la candidature sélectionnée AVANT d'afficher la vue.
     *
     * Exemple d'utilisation :
     *   FXMLLoader loader = new FXMLLoader(getClass().getResource("/candidaturefxml/AnalyseIA_RH.fxml"));
     *   Parent view = loader.load();
     *   AnalyseIAController_RH ctrl = loader.getController();
     *   ctrl.setCandidature(candidatureSelectionnee);
     *   pane.getChildren().setAll(view);
     */
    public void setCandidature(Candidature candidature) {
        this.candidature = candidature;
        if (candidature == null) return;

        // Afficher immédiatement le nom depuis la BD (sans attendre l'IA)
        Platform.runLater(() -> {
            String prenom = safe(candidature.getPrenom());
            String nom    = safe(candidature.getNom());
            if (lblInitiales != null) lblInitiales.setText(initiales(nom, prenom));
            if (lblNom       != null) lblNom.setText((prenom + " " + nom).trim());
        });

        // Charger DB (expériences + certifications) et les afficher immédiatement
        this.experiencesDb = candidatureService.getExperiencesByCandidatureId(candidature.getIDCandidat());
        this.certificationsDb = candidatureService.getCertificationsByCandidatureId(candidature.getIDCandidat());
        Platform.runLater(() -> {
            remplirExperienceDb(this.experiencesDb);
            remplirCertificationsDb(this.certificationsDb);
        });

        lancerAnalyse();
    }

    // ════════════════════════════════════════════════════════
    // Lancement analyse (thread background)
    // ════════════════════════════════════════════════════════

    private void lancerAnalyse() {
        showLoading(true);
        updateStatus("Connexion à l'agent IA...");

        int idCandidat = candidature.getIDCandidat();

        executor.submit(() -> {
            try {
                updateStatus("Analyse en cours (peut prendre 20-60s)...");
                AnalyseResult result = agentService.analyser(idCandidat);
                updateStatus("Remplissage de l'interface...");
                Platform.runLater(() -> remplirUI(result));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showError(e.getMessage()));
            }
        });
    }

    // ════════════════════════════════════════════════════════
    // Remplissage complet de l'UI
    // ════════════════════════════════════════════════════════

    private void remplirUI(AnalyseResult r) {
        this.lastAnalyse = r;
        showLoading(false);

        // ── 1. Carte candidat ──────────────────────────────
        String nom    = safe(r._nom);
        String prenom = safe(r._prenom);
        if (lblInitiales    != null) lblInitiales.setText(initiales(nom, prenom));
        if (lblNom          != null) lblNom.setText((prenom + " " + nom).trim());
        if (lblOffre        != null) lblOffre.setText(safe(r._offre_titre));

        if (lblCompatibilite != null) {
            lblCompatibilite.setText(safe(r.compatibilite));
            colorerCompatibilite(lblCompatibilite, r.compatibilite);
        }
        if (lblRecommandation != null)
            lblRecommandation.setText(safe(r.recommandation));

        // ── 2. Compétences techniques ──────────────────────
        if (r.competences_techniques != null) {
            int sc = r.competences_techniques.score;

            setLabel(lblPctCompetences, sc + "%");
            colorerPct(lblPctCompetences, sc);
            colorerProgressBar(pbCompetences, sc);
            animer(pbCompetences, sc / 100.0);

            // ► Items avec NOM + indicateur vert/rouge
            if (vboxCompetenceItems != null) {
                vboxCompetenceItems.getChildren().clear();
                if (r.competences_techniques.items != null) {
                    for (AnalyseResult.CompetencesTechniques.Item item : r.competences_techniques.items) {
                        vboxCompetenceItems.getChildren().add(buildCompetenceRow(item));
                    }
                }
            }

            setLabel(lblTauxCouverture,
                    "✓ " + r.competences_techniques.taux_couverture
                            + "% des compétences requises sont présentes.");
        }

        // ── 3. Expérience ─────────────────────────────────
        if (r.experience != null) {
            int scExp = r.experience.score;

            String annees = r.experience.annees_candidat + " ans d'expérience";
            String requis = "(Requis : " + r.experience.annees_requis + " ans)";
            String typeExp = safe(r.experience.type_experience);

            // Gauche
            setLabel(lblPctExperience, scExp + "%");
            colorerPct(lblPctExperience, scExp);
            colorerProgressBar(pbExperience, scExp);
            animer(pbExperience, scExp / 100.0);
            setLabel(lblAnneesCandidat, annees);
            setLabel(lblAnneesRequis, requis);
            setLabel(lblTypeExperience, typeExp);

            setLabel(lblExperienceCommentaire, safe(r.experience.commentaire));
            remplirExperienceDb(this.experiencesDb);
        }

        // Card droite = probabilité (basée sur score global)
        setLabel(lblPctExperienceR, r.score_global + "%");
        colorerPct(lblPctExperienceR, r.score_global);
        remplirGraphProbabilite(r.score_global);

        // ── 4. Diplôme & Certifications ───────────────────
        if (r.diplome_certifications != null) {
            int sc = r.diplome_certifications.score;

            setLabel(lblPctDiplome, sc + "%");
            colorerPct(lblPctDiplome, sc);
            colorerProgressBar(pbDiplome, sc);
            animer(pbDiplome, sc / 100.0);

            setLabel(lblDiplome,     safe(r.diplome_certifications.diplome));
            setLabel(lblNiveauRequis,"Requis : " + safe(r.diplome_certifications.niveau_requis));

            if (circleDiplome != null)
                circleDiplome.setFill(r.diplome_certifications.diplome_valide
                        ? Color.web("#10B981") : Color.web("#EF4444"));

            if (vboxCertifications != null) {
                // Affichage DB des certifications
                remplirCertificationsDb(this.certificationsDb);
            }

            setLabel(lblDiplomeCommentaire, safe(r.diplome_certifications.commentaire));
        }

        // ── 5. Soft Skills ────────────────────────────────
        if (r.soft_skills != null) {
            int sc = r.soft_skills.score;

            setLabel(lblPctSoftSkills, sc + "%");
            colorerPct(lblPctSoftSkills, sc);
            colorerProgressBar(pbSoftSkills, sc);
            animer(pbSoftSkills, sc / 100.0);

            if (vboxSoftSkills != null) {
                vboxSoftSkills.getChildren().clear();
                List<String> items = r.soft_skills.items;
                if (items != null && !items.isEmpty()) {
                    // Afficher chaque skill sur sa propre ligne avec un dot
                    for (String skill : items) {
                        vboxSoftSkills.getChildren().add(buildSoftSkillRow(skill));
                    }
                }
            }

            setLabel(lblSoftSkillsCommentaire, safe(r.soft_skills.commentaire));
        }

        // ── 6. Score global (droite) ──────────────────────
        setLabel(lblScoreGlobal, r.score_global + "%");
        setLabel(lblScoreCheck,  r.score_global >= 60 ? "✓" : "✗");
        if (lblScoreCheck != null)
            lblScoreCheck.setStyle("-fx-font-size:24; -fx-text-fill:"
                    + (r.score_global >= 60 ? "#10B981" : "#EF4444") + ";");

        setLabel(lblBadgeCompatibilite,   safe(r.compatibilite));
        setLabel(lblRecommandationDroite, "→ " + safe(r.recommandation));
        setLabel(lblOffreTitre,           safe(r._offre_titre));

        // Couleur du badge selon compatibilité
        if (hboxBadge != null) {
            String bgColor, textColor;
            switch (safe(r.compatibilite)) {
                case "Élevée"  -> { bgColor = "#D1FAE5"; textColor = "#065F46"; }
                case "Moyenne" -> { bgColor = "#FEF3C7"; textColor = "#92400E"; }
                default        -> { bgColor = "#FEE2E2"; textColor = "#991B1B"; }
            }
            hboxBadge.setStyle("-fx-background-color:" + bgColor
                    + "; -fx-background-radius:20; -fx-padding:6 16 6 16;");
            if (lblBadgeCompatibilite != null)
                lblBadgeCompatibilite.setStyle(
                        "-fx-font-size:13; -fx-font-weight:bold; -fx-text-fill:" + textColor + ";");
            // Label "Compatibilité" fixe
            if (hboxBadge.getChildren().size() >= 1) {
                var first = hboxBadge.getChildren().get(0);
                if (first instanceof Label l)
                    l.setStyle("-fx-font-size:13; -fx-text-fill:" + textColor + ";");
            }
        }

        // ── 7. Points forts ───────────────────────────────
        if (vboxPointsForts != null) {
            vboxPointsForts.getChildren().clear();
            if (r.points_forts != null)
                for (AnalyseResult.PointFort pf : r.points_forts)
                    vboxPointsForts.getChildren().add(buildPointFortRow(pf));
        }

        // ── 8. Points à améliorer ─────────────────────────
        if (vboxPointsAmeliorer != null) {
            vboxPointsAmeliorer.getChildren().clear();
            if (r.points_ameliorer != null)
                for (AnalyseResult.PointAmeliorer pa : r.points_ameliorer)
                    vboxPointsAmeliorer.getChildren().add(buildPointAmeliorerRow(pa));
        }

        // ── 9. Classement ─────────────────────────────────
        setLabel(lblClassement, "→ " + safe(r.classement_detail));
        if (lblClassementSub != null) {
            String sub = r.score_global >= 80
                    ? "Fortement recommandé pour entretien technique."
                    : r.score_global >= 60
                    ? "Recommandé pour un entretien de présélection."
                    : "Profil à étudier plus attentivement.";
            lblClassementSub.setText(sub);
        }
    }

    // ════════════════════════════════════════════════════════
    // Builders de composants dynamiques
    // ════════════════════════════════════════════════════════

    /**
     * Ligne compétence :  ● Java         ✓ ✓
     *                     ● Docker       [Docker]  (badge rouge si absent)
     */
    private HBox buildCompetenceRow(AnalyseResult.CompetencesTechniques.Item item) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        boolean present = item != null && item.present;

        Label sign = new Label(present ? "✓" : "✗");
        sign.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-text-fill:" + (present ? "#10B981" : "#EF4444") + ";");

        Label nomLbl = new Label(safe(item.nom));
        nomLbl.getStyleClass().add("label-body");
        nomLbl.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (present) {
            Label ok = new Label("OK");
            ok.getStyleClass().add("label-green");
            row.getChildren().addAll(sign, nomLbl, spacer, ok);
        } else {
            Label ko = new Label("FAUX");
            ko.getStyleClass().add("label-red");
            row.getChildren().addAll(sign, nomLbl, spacer, ko);
        }

        return row;
    }

    private void remplirExperienceDb(List<Experience> exps) {
        if (vboxExperienceDb == null) return;
        vboxExperienceDb.getChildren().clear();
        if (exps == null || exps.isEmpty()) return;

        for (Experience e : exps) {
            vboxExperienceDb.getChildren().add(buildExperienceRow(e));
        }
    }

    private HBox buildExperienceRow(Experience e) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5, Color.web("#10B981"));

        String deb = e.getDate_debut() != null ? e.getDate_debut().toString() : "?";
        String fin = e.getDate_fin() != null ? e.getDate_fin().toString() : "Aujourd'hui";
        String txt = (safe(e.getPoste()) + " - " + safe(e.getEntreprise()) + " (" + deb + " → " + fin + ")").trim();

        Label lbl = new Label(txt);
        lbl.getStyleClass().add("label-body");
        lbl.setWrapText(true);

        row.getChildren().addAll(dot, lbl);
        return row;
    }

    private void remplirCertificationsDb(List<Certification> certs) {
        if (vboxCertifications == null) return;
        vboxCertifications.getChildren().clear();
        if (certs == null || certs.isEmpty()) return;

        for (Certification c : certs) {
            vboxCertifications.getChildren().add(buildCertificationDbRow(c));
        }
    }

    private HBox buildCertificationDbRow(Certification c) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5, Color.web("#10B981"));
        String obt = c.getDate_obtention() != null ? c.getDate_obtention().toString() : "?";
        String txt = (safe(c.getNom_certification()) + " - " + safe(c.getOrganisme()) + " (" + obt + ")").trim();

        Label lbl = new Label(txt);
        lbl.getStyleClass().add("label-body");
        lbl.setWrapText(true);

        row.getChildren().addAll(dot, lbl);
        return row;
    }

    /** Convertit le niveau IA en indicateur visuel */
    private String niveauToCheck(String niveau) {
        if (niveau == null) return "✓";
        return switch (niveau) {
            case "Confirmé"      -> "✓ ✓";
            case "Intermédiaire" -> "✓";
            case "Débutant"      -> "~";
            default              -> "✓";
        };
    }

    /** Ligne certification */
    private HBox buildCertRow(AnalyseResult.DiplomeCertifications.Certification cert) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5);
        dot.setFill(cert.presente ? Color.web("#10B981") : Color.web("#EF4444"));

        Label lbl = new Label(cert.presente
                ? safe(cert.nom)
                : "Aucune certification " + safe(cert.nom) + " trouvée");
        lbl.getStyleClass().add("label-body");
        lbl.setWrapText(true);

        row.getChildren().addAll(dot, lbl);
        return row;
    }

    /** Ligne soft skill avec dot vert */
    private HBox buildSoftSkillRow(String texte) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(5, Color.web("#10B981"));
        Label lbl  = new Label(safe(texte));
        lbl.getStyleClass().add("label-body");
        lbl.setWrapText(true);
        row.getChildren().addAll(dot, lbl);
        return row;
    }

    /** Ligne point fort : icône + mot-clé en gras + texte */
    private HBox buildPointFortRow(AnalyseResult.PointFort pf) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label icone = new Label(iconePointFort(pf.icone));
        icone.setStyle("-fx-font-size:16;");

        TextFlow tf = new TextFlow();
        tf.setPrefWidth(210);
        tf.setMaxWidth(210);

        Text motCle = new Text(safe(pf.mot_cle) + " ");
        motCle.setStyle("-fx-font-weight:bold; -fx-font-size:13;");

        Text suite = new Text(safe(pf.texte));
        suite.setStyle("-fx-font-size:13;");

        tf.getChildren().addAll(motCle, suite);
        row.getChildren().addAll(icone, tf);
        return row;
    }

    /** Ligne point à améliorer : ⚠ ou ❌ + texte */
    private HBox buildPointAmeliorerRow(AnalyseResult.PointAmeliorer pa) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        boolean warning = "warning".equals(pa.icone);
        Label icone = new Label(warning ? "⚠" : "❌");
        icone.setStyle("-fx-font-size:14;"
                + (warning ? "-fx-text-fill:#F59E0B;" : "-fx-text-fill:#EF4444;"));

        Label lbl = new Label(safe(pa.texte));
        lbl.getStyleClass().add("point-ameliorer-text");
        lbl.setWrapText(true);
        lbl.setPrefWidth(210);

        row.getChildren().addAll(icone, lbl);
        return row;
    }

    // ════════════════════════════════════════════════════════
    // Helpers : animation & couleurs
    // ════════════════════════════════════════════════════════

    /** Animation fluide 0 → target en 900ms */
    private void animer(ProgressBar pb, double target) {
        if (pb == null) return;
        pb.setProgress(0);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(pb.progressProperty(), 0)),
                new KeyFrame(Duration.millis(900), new KeyValue(pb.progressProperty(), target))
        );
        tl.play();
    }

    /** Couleur du label % selon le score */
    private void colorerPct(Label lbl, int score) {
        if (lbl == null) return;
        lbl.getStyleClass().removeAll("pct-green", "pct-orange", "pct-red");
        if      (score >= 80) lbl.getStyleClass().add("pct-green");
        else if (score >= 60) lbl.getStyleClass().add("pct-orange");
        else                  lbl.getStyleClass().add("pct-red");
    }

    /** Couleur de la barre de progression selon le score */
    private void colorerProgressBar(ProgressBar pb, int score) {
        if (pb == null) return;
        pb.getStyleClass().removeAll("progress-bar-green", "progress-bar-orange", "progress-bar-red");
        if      (score >= 80) pb.getStyleClass().add("progress-bar-green");
        else if (score >= 60) pb.getStyleClass().add("progress-bar-orange");
        else                  pb.getStyleClass().add("progress-bar-red");
    }

    /** Couleur du label compatibilité */
    private void colorerCompatibilite(Label lbl, String compat) {
        if (lbl == null) return;
        lbl.getStyleClass().removeAll("label-green", "label-orange", "label-red");
        if      ("Élevée".equals(compat))  lbl.getStyleClass().add("label-green");
        else if ("Moyenne".equals(compat)) lbl.getStyleClass().add("label-orange");
        else if (compat != null && !compat.isBlank()) lbl.getStyleClass().add("label-red");
    }

    /** Icône selon le type de point fort */
    private String iconePointFort(String type) {
        if (type == null) return "✅";
        return switch (type) {
            case "technique"  -> "🔧";
            case "experience" -> "💼";
            case "soft"       -> "✅";
            case "diplome"    -> "🎓";
            default           -> "✅";
        };
    }

    // ════════════════════════════════════════════════════════
    // Utilitaires
    // ════════════════════════════════════════════════════════

    private void setLabel(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    private String safe(String s) { return s != null ? s : ""; }

    private String initiales(String nom, String prenom) {
        String i1 = (prenom != null && !prenom.isEmpty()) ? String.valueOf(prenom.charAt(0)) : "";
        String i2 = (nom    != null && !nom.isEmpty())    ? String.valueOf(nom.charAt(0))    : "";
        return (i1 + i2).toUpperCase();
    }

    private void updateStatus(String msg) {
        Platform.runLater(() -> { if (lblLoadingStatus != null) lblLoadingStatus.setText(msg); });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(show);
            loadingOverlay.setManaged(show);
        }
    }

    private void showError(String msg) {
        showLoading(false);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur Agent IA");
        alert.setHeaderText("L'analyse a échoué");
        alert.setContentText(msg != null ? msg : "Erreur inconnue");
        alert.showAndWait();
    }

    // ════════════════════════════════════════════════════════
    // Navigation
    // ════════════════════════════════════════════════════════

    @FXML
    private void handleRetour() {
        executor.shutdownNow();
        try {
            Parent root = (btnRetour != null && btnRetour.getScene() != null)
                    ? btnRetour.getScene().getRoot() : null;
            if (root == null) return;

            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof Pane pane))
                throw new RuntimeException("adminContentArea not found");

            var url = getClass().getResource("/candidaturefxml/RH_Candidatures.fxml");
            if (url == null) throw new RuntimeException("RH_Candidatures.fxml introuvable");

            Parent view = FXMLLoader.load(url);
            pane.getChildren().setAll(view);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleTelecharger() {
        // TODO: export PDF
        System.out.println("Télécharger le rapport clicked");
    }
}