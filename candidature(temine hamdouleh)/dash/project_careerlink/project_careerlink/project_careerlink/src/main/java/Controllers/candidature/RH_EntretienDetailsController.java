package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Entretien;
import Services.candidature.CandidatureService;
import Services.candidature.EntretienService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class RH_EntretienDetailsController {

    @FXML private Label lblAvatar;
    @FXML private Label lblCandidat;
    @FXML private Label lblRole;
    @FXML private Label lblDateHeure;
    @FXML private Label lblDuration;
    @FXML private Label lblPlatform;
    @FXML private ImageView imgPlatform;
    @FXML private StackPane platformIconWrap;
    @FXML private Label lblNotes;

    @FXML private Label lblStars;
    @FXML private Label lblRating;

    @FXML private TextField tfLink;
    @FXML private Button btnCopy;

    @FXML private Button btnClose;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final EntretienService entretienService = new EntretienService();
    private final CandidatureService candidatureService = new CandidatureService();

    private Entretien entretien;

    public void setEntretien(Entretien e) {
        this.entretien = e;
        render();
    }

    private void applyPlatformBranding(String mode, String platformOrLieu) {
        if (imgPlatform == null) return;

        String m = safe(mode);
        String p = safe(platformOrLieu);
        String key = ("En ligne".equalsIgnoreCase(m) ? p : "Présentiel");
        if (key.isBlank()) key = m;

        String path = null;
        String brandClass = null;
        String k = key.toLowerCase(Locale.ROOT);
        if (k.contains("meet")) {
            path = "/images/googlemeet.png";
            brandClass = "rh-cap-platform-meet";
        } else if (k.contains("team")) {
            path = "/images/teams.png";
            brandClass = "rh-cap-platform-teams";
        } else if (k.contains("zoom")) {
            path = "/images/zoom.jpg";
            brandClass = "rh-cap-platform-zoom";
        } else if (k.contains("présentiel") || k.contains("presentiel") || k.contains("sur place") || k.contains("bureau") || k.contains("sur site")) {
            path = "/images/ordi.png";
            brandClass = "rh-cap-platform-onsite";
        } else if ("en ligne".equalsIgnoreCase(m)) {
            path = "/images/ordi.png";
            brandClass = "rh-cap-platform-online";
        }

        if (platformIconWrap != null) {
            platformIconWrap.getStyleClass().removeAll(
                    "rh-cap-platform-meet",
                    "rh-cap-platform-teams",
                    "rh-cap-platform-zoom",
                    "rh-cap-platform-onsite",
                    "rh-cap-platform-online"
            );
            if (brandClass != null) platformIconWrap.getStyleClass().add(brandClass);
        }

        if (path == null) {
            imgPlatform.setImage(null);
            imgPlatform.setVisible(false);
            imgPlatform.setManaged(false);
            return;
        }

        try {
            Image img = new Image(getClass().getResourceAsStream(path));
            imgPlatform.setImage(img);
            imgPlatform.setVisible(true);
            imgPlatform.setManaged(true);
        } catch (Exception e) {
            imgPlatform.setImage(null);
            imgPlatform.setVisible(false);
            imgPlatform.setManaged(false);
        }
    }

    private void render() {
        if (entretien == null) {
            if (lblAvatar != null) lblAvatar.setText("?");
            lblCandidat.setText("-");
            if (lblRole != null) lblRole.setText("");
            lblDateHeure.setText("-");
            if (lblDuration != null) lblDuration.setText("-");
            if (lblPlatform != null) lblPlatform.setText("-");
            lblNotes.setText("-");
            if (tfLink != null) tfLink.setText("");
            btnEdit.setDisable(true);
            btnDelete.setDisable(true);
            if (btnCopy != null) btnCopy.setDisable(true);
            return;
        }

        Candidature c = null;
        try {
            c = candidatureService.getCandidatureById(entretien.getId_candidature());
        } catch (Exception ignored) {
        }

        String who;
        String role = "";
        if (c == null) {
            who = "Candidature ID: " + entretien.getId_candidature();
        } else {
            who = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
            if (who.isBlank()) who = "Candidature ID: " + entretien.getId_candidature();
            role = !safe(c.getHighest_degree()).isBlank() ? safe(c.getHighest_degree()) : safe(c.getInstitution());
        }

        lblCandidat.setText(who);
        if (lblRole != null) lblRole.setText(role);
        if (lblAvatar != null) lblAvatar.setText(initialsFromName(who));

        if (entretien.getDate_heure() != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRENCH);
            lblDateHeure.setText(entretien.getDate_heure().format(fmt));
        } else {
            lblDateHeure.setText("-");
        }

        if (lblDuration != null) lblDuration.setText("60 minutes");

        String mode = safe(entretien.getMode());
        String platformOrLieu;
        if ("En ligne".equalsIgnoreCase(mode)) {
            platformOrLieu = safe(entretien.getPlatform());
        } else {
            platformOrLieu = safe(entretien.getLieu());
        }
        if (lblPlatform != null) {
            String p = platformOrLieu;
            if (p.isBlank()) p = mode;
            lblPlatform.setText(p);
        }

        applyPlatformBranding(mode, platformOrLieu);

        String notes = safe(entretien.getNotes());
        lblNotes.setText(stripMeetingLinkLine(notes));

        String link = extractMeetingLink(notes);
        if (tfLink != null) tfLink.setText(link);
        if (btnCopy != null) btnCopy.setDisable(link.isBlank());

        if (lblStars != null && safe(lblStars.getText()).isBlank()) lblStars.setText("★★★★★");
        if (lblRating != null && safe(lblRating.getText()).isBlank()) lblRating.setText("5");
    }

    @FXML
    private void copyLink() {
        if (tfLink == null) return;
        String link = safe(tfLink.getText());
        if (link.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(link);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private String extractMeetingLink(String notes) {
        String s = safe(notes);
        if (s.isBlank()) return "";
        String key = "Lien:";
        int idx = s.toLowerCase(Locale.ROOT).indexOf(key.toLowerCase(Locale.ROOT));
        if (idx < 0) return "";
        String tail = s.substring(idx + key.length()).trim();
        int end = tail.indexOf('\n');
        if (end >= 0) tail = tail.substring(0, end).trim();
        return tail;
    }

    private String stripMeetingLinkLine(String notes) {
        String s = safe(notes);
        if (s.isBlank()) return "";
        String key = "Lien:";
        String[] lines = s.split("\\R");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            if (safe(line).toLowerCase(Locale.ROOT).startsWith(key.toLowerCase(Locale.ROOT))) continue;
            if (out.length() > 0) out.append("\n");
            out.append(line);
        }
        return out.toString().trim();
    }

    private String initialsFromName(String fullName) {
        String s = safe(fullName);
        if (s.isBlank() || s.startsWith("Candidature")) return "?";
        String[] parts = s.split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        String a = parts[0].isBlank() ? "" : parts[0].substring(0, 1);
        String b = parts[parts.length - 1].isBlank() ? "" : parts[parts.length - 1].substring(0, 1);
        String res = (a + b).toUpperCase(Locale.ROOT);
        return res.isBlank() ? "?" : res;
    }

    @FXML
    private void back() {
        try {
            if (lblCandidat == null || lblCandidat.getScene() == null) return;
            Parent root = lblCandidat.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof Pane pane)) return;

            var url = getClass().getResource("/candidaturefxml/RH_Entretiens.fxml");
            if (url == null) return;

            Parent view = FXMLLoader.load(url);
            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void edit() {
        if (entretien == null) return;
        try {
            if (lblCandidat == null || lblCandidat.getScene() == null) return;
            Parent root = lblCandidat.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof Pane pane)) return;

            var url = getClass().getResource("/candidaturefxml/RH_EntretienForm.fxml");
            if (url == null) return;

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienFormController ctrl = loader.getController();
            if (ctrl != null) {
                // Recharge depuis DB si possible (évite d'avoir un objet partiel)
                Entretien fresh = null;
                try {
                    fresh = entretienService.getEntretienById(entretien.getId_entretien());
                } catch (Exception ignored) {
                }
                ctrl.openForEdit(fresh != null ? fresh : entretien);
            }

            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void delete() {
        if (entretien == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer cet entretien ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    entretienService.supprimerEntretien(entretien.getId_entretien());
                    back();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
