package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Services.candidature.CertificationService;
import Services.candidature.ExperienceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class RH_CandidatureDetailsController {

    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    @FXML private Button btnOpenFiles;
    @FXML private Button btnOpenVideo;
    @FXML private Button btnEntretien;

    @FXML private Label lblInitials;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;

    @FXML private Label chipConfirmed;
    @FXML private Label chipPrivacy;

    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblBirth;
    @FXML private Label lblDegree;
    @FXML private Label lblInstitution;

    @FXML private Label lblMotivation;

    @FXML private FlowPane skillsWrap;
    @FXML private Label lblSkillsRaw;

    @FXML private VBox experiencesBox;
    @FXML private Label lblNoExperiences;

    @FXML private VBox certificationsBox;
    @FXML private Label lblNoCertifications;

    private final ExperienceService experienceService = new ExperienceService();
    private final CertificationService certificationService = new CertificationService();

    private Candidature candidature;

    public void setCandidature(Candidature c) {
        this.candidature = c;
        refreshUI();
    }

    @FXML
    private void initialize() {
        refreshUI();
    }

    @FXML
    private void goBack() {
        try {
            Parent root = lblTitle.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/RH_Candidatures.fxml");
            if (url == null) throw new RuntimeException("RH_Candidatures.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de revenir à la liste des candidatures.").show();
        }
    }

    @FXML
    private void openEntretien() {
        if (candidature == null) return;

        try {
            Parent root = lblTitle.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/RH_EntretienForm.fxml");
            if (url == null) throw new RuntimeException("RH_EntretienForm.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienFormController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.openForCreate(candidature.getIDCandidat());
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire d'entretien.").show();
        }
    }

    @FXML
    private void openPieces() {
        if (candidature == null) return;

        String p = safe(candidature.getPieces_jointes());
        if (p.isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune pièce jointe.").show();
            return;
        }
        openPath(p);
    }

    @FXML
    private void openVideo() {
        if (candidature == null) return;

        String p = safe(candidature.getVideo_path());
        if (p.isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune vidéo.").show();
            return;
        }
        openPath(p);
    }

    private void refreshUI() {
        boolean has = (candidature != null);

        if (btnEntretien != null) btnEntretien.setDisable(!has);
        if (btnOpenFiles != null) btnOpenFiles.setDisable(!has);
        if (btnOpenVideo != null) btnOpenVideo.setDisable(!has);

        if (!has) {
            setText(lblTitle, "Détails candidature");
            setText(lblSubtitle, "");
            setText(lblInitials, "");
            setText(lblName, "");
            setText(lblEmail, "");
            setText(lblPhone, "");
            setText(lblAdresse, "");
            setText(lblVille, "");
            setText(lblBirth, "");
            setText(lblDegree, "");
            setText(lblInstitution, "");
            setText(lblMotivation, "");
            if (skillsWrap != null) skillsWrap.getChildren().clear();
            setText(lblSkillsRaw, "");
            if (experiencesBox != null) experiencesBox.getChildren().clear();
            if (certificationsBox != null) certificationsBox.getChildren().clear();
            if (lblNoExperiences != null) lblNoExperiences.setVisible(true);
            if (lblNoCertifications != null) lblNoCertifications.setVisible(true);
            return;
        }

        String fullName = (safe(candidature.getPrenom()) + " " + safe(candidature.getNom())).trim();
        if (fullName.isBlank()) fullName = "Candidature";

        setText(lblTitle, fullName);
        if (lblSubtitle != null) {
            lblSubtitle.setText("");
            lblSubtitle.setVisible(false);
            lblSubtitle.setManaged(false);
        }
        setText(lblName, fullName);

        setText(lblInitials, initialsOf(safe(candidature.getPrenom()), safe(candidature.getNom())));
        setText(lblEmail, "✉  " + safe(candidature.getEmail()));

        String phone = (safe(candidature.getIndicatif()) + " " + safe(candidature.getTel())).trim();
        setText(lblPhone, "☎  " + (phone.isBlank() ? "-" : phone));

        setText(lblAdresse, safeOrDash(candidature.getAdresse()));
        setText(lblVille, safeOrDash(candidature.getVille()));
        setText(lblDegree, safeOrDash(candidature.getHighest_degree()));
        setText(lblInstitution, safeOrDash(candidature.getInstitution()));

        if (lblBirth != null) {
            String d = (candidature.getDate_naissance() == null) ? "-" : new SimpleDateFormat("dd/MM/yyyy").format(candidature.getDate_naissance());
            lblBirth.setText(d);
        }

        if (chipConfirmed != null) {
            boolean ok = candidature.isConfirm_info();
            chipConfirmed.setText(ok ? "Infos confirmées" : "Infos non confirmées");
            chipConfirmed.getStyleClass().removeAll("rh-chip-success", "rh-chip-warn");
            chipConfirmed.getStyleClass().add(ok ? "rh-chip-success" : "rh-chip-warn");
        }
        if (chipPrivacy != null) {
            boolean ok = candidature.isAccept_privacy();
            chipPrivacy.setText(ok ? "Privacy acceptée" : "Privacy non acceptée");
            chipPrivacy.getStyleClass().removeAll("rh-chip-success", "rh-chip-warn");
            chipPrivacy.getStyleClass().add(ok ? "rh-chip-success" : "rh-chip-warn");
        }

        String motivation = safe(candidature.getLettre_motivation());
        if (motivation.isBlank()) motivation = "-";
        setText(lblMotivation, motivation);

        String skills = safe(candidature.getSkills_text());
        setText(lblSkillsRaw, skills.isBlank() ? "-" : skills);
        renderSkillsChips(skills);

        renderExperiences();
        renderCertifications();
    }

    private void renderSkillsChips(String skillsRaw) {
        if (skillsWrap == null) return;
        skillsWrap.getChildren().clear();

        String s = (skillsRaw == null) ? "" : skillsRaw.trim();
        if (s.isBlank()) return;

        String[] parts = s.split("[,;\\n]");
        int added = 0;
        for (String p : parts) {
            String t = safe(p);
            if (t.isBlank()) continue;

            Label chip = new Label(t);
            chip.getStyleClass().add("rh-chip-neutral");
            skillsWrap.getChildren().add(chip);

            added++;
            if (added >= 18) break;
        }
    }

    private void renderExperiences() {
        if (experiencesBox == null) return;
        experiencesBox.getChildren().clear();

        List<Experience> list = new ArrayList<>();
        try {
            list = experienceService.afficherExperiencesParCandidature(candidature.getIDCandidat());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        boolean empty = (list == null || list.isEmpty());
        if (lblNoExperiences != null) lblNoExperiences.setVisible(empty);
        if (empty) return;

        for (Experience e : list) {
            experiencesBox.getChildren().add(buildExperienceRow(e));
        }
    }

    private void renderCertifications() {
        if (certificationsBox == null) return;
        certificationsBox.getChildren().clear();

        List<Certification> list = new ArrayList<>();
        try {
            list = certificationService.afficherCertificationsParCandidature(candidature.getIDCandidat());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        boolean empty = (list == null || list.isEmpty());
        if (lblNoCertifications != null) lblNoCertifications.setVisible(empty);
        if (empty) return;

        for (Certification c : list) {
            certificationsBox.getChildren().add(buildCertificationRow(c));
        }
    }

    private HBox buildExperienceRow(Experience e) {
        HBox row = new HBox(10);
        row.getStyleClass().add("rh-cand-details-row");

        Label left = new Label("💼");
        left.getStyleClass().add("rh-cand-details-row-ico");

        VBox mid = new VBox(2);
        Label title = new Label(safeOrDash(e.getPoste()));
        title.getStyleClass().add("rh-cand-details-row-title");

        String company = safeOrDash(e.getEntreprise());
        String date = formatPeriod(e.getDate_debut(), e.getDate_fin());
        Label sub = new Label(company + (date.isBlank() ? "" : " • " + date));
        sub.getStyleClass().add("rh-muted");

        mid.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(left, mid, spacer);
        return row;
    }

    private HBox buildCertificationRow(Certification c) {
        HBox row = new HBox(10);
        row.getStyleClass().add("rh-cand-details-row");

        Label left = new Label("🏅");
        left.getStyleClass().add("rh-cand-details-row-ico");

        VBox mid = new VBox(2);
        Label title = new Label(safeOrDash(c.getNom_certification()));
        title.getStyleClass().add("rh-cand-details-row-title");

        String org = safeOrDash(c.getOrganisme());
        String obt = (c.getDate_obtention() == null) ? "" : new SimpleDateFormat("dd/MM/yyyy").format(c.getDate_obtention());
        String exp = (c.getDate_expiration() == null) ? "" : new SimpleDateFormat("dd/MM/yyyy").format(c.getDate_expiration());

        String dates = obt.isBlank() ? "" : ("Obtenue: " + obt + (exp.isBlank() ? "" : " • Expire: " + exp));

        String num = (c.getNumero() == null) ? "" : (" • N° " + c.getNumero());
        Label sub = new Label(org + num + (dates.isBlank() ? "" : " • " + dates));
        sub.getStyleClass().add("rh-muted");

        mid.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(left, mid, spacer);
        return row;
    }

    private String formatPeriod(java.sql.Date start, java.sql.Date end) {
        if (start == null && end == null) return "";
        SimpleDateFormat df = new SimpleDateFormat("MM/yyyy");
        String a = (start == null) ? "" : df.format(start);
        String b = (end == null) ? "" : df.format(end);
        if (!a.isBlank() && !b.isBlank()) return a + " - " + b;
        if (!a.isBlank()) return "Depuis " + a;
        if (!b.isBlank()) return "Jusqu'à " + b;
        return "";
    }

    private void openPath(String path) {
        try {
            if (!Desktop.isDesktopSupported()) {
                new Alert(Alert.AlertType.INFORMATION, "Ouverture de fichier non supportée sur cette machine.").show();
                return;
            }

            File f = new File(path);
            if (f.exists()) {
                Desktop.getDesktop().open(f);
                return;
            }

            new Alert(Alert.AlertType.INFORMATION, "Chemin introuvable: " + path).show();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir: " + path).show();
        }
    }

    private void setText(Label l, String v) {
        if (l != null) l.setText(v == null ? "" : v);
    }

    private String initialsOf(String prenom, String nom) {
        String p = (prenom == null) ? "" : prenom.trim();
        String n = (nom == null) ? "" : nom.trim();
        String a = p.isEmpty() ? "" : p.substring(0, 1).toUpperCase();
        String b = n.isEmpty() ? "" : n.substring(0, 1).toUpperCase();
        String r = (a + b).trim();
        return r.isEmpty() ? "?" : r;
    }

    private String safeOrDash(String s) {
        String x = safe(s);
        return x.isBlank() ? "-" : x;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
