package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Entretien;
import Services.candidature.EntretienService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Pane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CandidateEntretienDetailsController {

    @FXML private Label lblSubtitle;
    @FXML private Label chipStatus;

    @FXML private Label lblDate;
    @FXML private Label lblTime;
    @FXML private Label lblMode;
    @FXML private Label lblPlace;

    @FXML private TextField tfLink;
    @FXML private Button btnCopy;
    @FXML private TextArea taNotes;

    private final EntretienService entretienService = new EntretienService();

    private Candidature candidature;
    private Entretien entretien;

    public void setCandidature(Candidature c) {
        this.candidature = c;
        loadAndRender();
    }

    private void loadAndRender() {
        if (candidature == null) {
            entretien = null;
            render();
            return;
        }

        try {
            entretien = entretienService.getLatestEntretienByCandidature(candidature.getIDCandidat());
        } catch (Exception ex) {
            ex.printStackTrace();
            entretien = null;
        }

        render();
    }

    private void render() {
        String subtitle = candidature == null ? "" : (safe(candidature.getPrenom()) + " " + safe(candidature.getNom())).trim();
        if (lblSubtitle != null) lblSubtitle.setText(subtitle);

        if (entretien == null) {
            set(chipStatus, "Aucun entretien");
            set(lblDate, "-");
            set(lblTime, "-");
            set(lblMode, "-");
            set(lblPlace, "-");
            if (tfLink != null) tfLink.setText("");
            if (taNotes != null) taNotes.setText("-");
            if (btnCopy != null) btnCopy.setDisable(true);
            return;
        }

        set(chipStatus, prettyStatus(entretien.getStatut()));

        LocalDate d = entretien.getDate();
        if (d != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
            set(lblDate, d.format(fmt));
        } else {
            set(lblDate, "-");
        }

        LocalDateTime dh = entretien.getDate_heure();
        if (dh != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);
            set(lblTime, dh.format(fmt));
        } else {
            set(lblTime, "-");
        }

        String mode = safe(entretien.getMode());
        set(lblMode, mode.isBlank() ? "-" : mode);

        String place;
        if ("En ligne".equalsIgnoreCase(mode)) {
            place = safe(entretien.getPlatform());
        } else {
            place = safe(entretien.getLieu());
        }
        set(lblPlace, place.isBlank() ? "-" : place);

        String notes = safe(entretien.getNotes());
        if (taNotes != null) taNotes.setText(stripMeetingLinkLine(notes).isBlank() ? "-" : stripMeetingLinkLine(notes));

        String link = extractMeetingLink(notes);
        if (tfLink != null) tfLink.setText(link);
        if (btnCopy != null) btnCopy.setDisable(link.isBlank());
    }

    @FXML
    private void onCopyLink() {
        if (tfLink == null) return;
        String link = safe(tfLink.getText());
        if (link.isBlank()) return;
        ClipboardContent content = new ClipboardContent();
        content.putString(link);
        Clipboard.getSystemClipboard().setContent(content);
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

    private Pane findCenterWrap() {
        if (lblDate == null || lblDate.getScene() == null) return null;
        Parent root = lblDate.getScene().getRoot();
        var page = root.lookup("#pageContainer");
        if (page instanceof Pane pane) return pane;
        var content = root.lookup("#contentArea");
        if (content instanceof Pane pane) return pane;
        return null;
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

    private void set(Label l, String v) {
        if (l != null) l.setText(v == null ? "" : v);
    }

    private String prettyStatus(String s) {
        String x = safe(s);
        if (x.isBlank()) return "Planifié";
        return x;
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
