package Controllers.candidature;

import Entities.candidature.Candidature;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.text.SimpleDateFormat;

public class CandidatureDetailsController {

    @FXML private ImageView imgBanner;

    @FXML private Label lblBannerSubtitle;

    @FXML private Label lblInitials;
    @FXML private Label lblName;
    @FXML private Label lblEmail;
    @FXML private Label lblPhone;

    @FXML private Label chipStatus;
    @FXML private Label chipPrivacy;

    @FXML private Label lblAdresse;
    @FXML private Label lblVille;
    @FXML private Label lblBirth;
    @FXML private Label lblDegree;
    @FXML private Label lblInstitution;

    @FXML private Label lblMotivation;

    @FXML private FlowPane skillsWrap;
    @FXML private Label lblSkillsRaw;

    @FXML private Label lblRightTitle;

    @FXML private Button btnTabMotivation;
    @FXML private Button btnTabSkills;

    @FXML private VBox paneMotivation;
    @FXML private VBox paneSkills;

    @FXML private Button btnOpenFiles;
    @FXML private Button btnOpenVideo;

    private Candidature candidature;

    public void setCandidature(Candidature c) {
        this.candidature = c;
        refreshUI();
    }

    @FXML
    private void initialize() {
        try {
            var url = getClass().getResource("/Images/emp.jpg");
            if (url != null && imgBanner != null) {
                imgBanner.setImage(new Image(url.toExternalForm()));
            }
        } catch (Exception ignored) {
        }

        if (skillsWrap != null) {
            skillsWrap.setVisible(false);
            skillsWrap.setManaged(false);
        }

        showMotivation();
        refreshUI();
    }

    @FXML
    private void showMotivation() {
        setPaneVisible(paneMotivation, true);
        setPaneVisible(paneSkills, false);
        setTabActive(btnTabMotivation, true);
        setTabActive(btnTabSkills, false);
        setText(lblRightTitle, "Lettre motivation");
    }

    @FXML
    private void showSkills() {
        setPaneVisible(paneMotivation, false);
        setPaneVisible(paneSkills, true);
        setTabActive(btnTabMotivation, false);
        setTabActive(btnTabSkills, true);
        setText(lblRightTitle, "Compétences");
    }

    @FXML
    private void onBack() {
        try {
            Pane pane = findCenterWrap();
            if (pane == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/MesCandidatures.fxml");
            if (url == null) throw new RuntimeException("MesCandidatures.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de revenir à la liste des candidatures.").show();
        }
    }

    @FXML
    private void onOpenPieces() {
        if (candidature == null) return;
        String p = safe(candidature.getPieces_jointes());
        if (p.isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Aucune pièce jointe.").show();
            return;
        }
        openPath(p);
    }

    @FXML
    private void onOpenVideo() {
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

        if (btnOpenFiles != null) btnOpenFiles.setDisable(!has);
        if (btnOpenVideo != null) btnOpenVideo.setDisable(!has);

        if (!has) {
            setText(lblBannerSubtitle, "");
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
            setText(lblSkillsRaw, "");
            if (skillsWrap != null) skillsWrap.getChildren().clear();
            if (chipStatus != null) chipStatus.setText("");
            if (chipPrivacy != null) chipPrivacy.setText("");
            return;
        }

        String fullName = (safe(candidature.getPrenom()) + " " + safe(candidature.getNom())).trim();
        if (fullName.isBlank()) fullName = "Candidature";

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

        if (chipStatus != null) {
            String st = safe(candidature.getStatut());
            if (st.isBlank()) st = "EN COURS";
            chipStatus.setText(st);
        }

        if (chipPrivacy != null) {
            boolean ok = candidature.isAccept_privacy();
            chipPrivacy.setText(ok ? "Privacy acceptée" : "Privacy non acceptée");
        }

        String subtitle = subtitleOf();
        setText(lblBannerSubtitle, subtitle);

        String motivation = safe(candidature.getLettre_motivation());
        if (motivation.isBlank()) motivation = safe(candidature.getSkills_text());
        if (motivation.isBlank()) motivation = "-";
        setText(lblMotivation, motivation);

        String skills = safe(candidature.getSkills_text());
        setText(lblSkillsRaw, skills.isBlank() ? "-" : skills);
        renderSkillsChips(skills);
    }

    private String subtitleOf() {
        String deg = safe(candidature.getHighest_degree());
        String inst = safe(candidature.getInstitution());
        if (!deg.isBlank() && !inst.isBlank()) return deg + " • " + inst;
        if (!deg.isBlank()) return deg;
        if (!inst.isBlank()) return inst;
        return "";
    }

    private void renderSkillsChips(String skillsRaw) {
        if (skillsWrap == null) return;
        skillsWrap.getChildren().clear();

        String s = safe(skillsRaw);
        if (s.isBlank()) return;

        String[] parts = s.split("[,;\\n]");
        int added = 0;
        for (String p : parts) {
            String t = safe(p);
            if (t.isBlank()) continue;

            Label chip = new Label(t);
            chip.getStyleClass().add("cd-chip");
            skillsWrap.getChildren().add(chip);

            added++;
            if (added >= 18) break;
        }
    }

    private Pane findCenterWrap() {
        if (lblName == null || lblName.getScene() == null) return null;
        Parent root = lblName.getScene().getRoot();
        var page = root.lookup("#pageContainer");
        if (page instanceof Pane pane) return pane;
        var content = root.lookup("#contentArea");
        if (content instanceof Pane pane) return pane;
        return null;
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

    private void setPaneVisible(VBox pane, boolean visible) {
        if (pane == null) return;
        pane.setVisible(visible);
        pane.setManaged(visible);
    }

    private void setTabActive(Button b, boolean active) {
        if (b == null) return;
        b.getStyleClass().remove("cd-tab-btn-active");
        if (active) b.getStyleClass().add("cd-tab-btn-active");
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
