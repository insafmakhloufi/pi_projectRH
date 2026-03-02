package Controllers.candidature;

import Entities.candidature.Entretien;
import Entities.candidature.Candidature;
import Services.candidature.EntretienService;
import Services.candidature.CandidatureService;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.DateCell;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.function.UnaryOperator;

public class RH_EntretienFormController {

    @FXML private Label title;

    @FXML private Label lblCandidateName;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbHour;
    @FXML private ComboBox<String> cbMinute;
    @FXML private ComboBox<String> cbMode;

    @FXML private VBox boxPlatform;
    @FXML private ToggleButton tbMeet;
    @FXML private ToggleButton tbTeams;
    @FXML private ToggleButton tbZoom;
    private final ToggleGroup platformGroup = new ToggleGroup();

    @FXML private TextField tfMeetingLink;
    @FXML private VBox boxLieu;

    @FXML private Label lblRightTitle;

    @FXML private Button btnOpenPlatform;

    @FXML private Label errDate;
    @FXML private Label errTime;
    @FXML private Label errMode;
    @FXML private Label errNotes;
    @FXML private Label errPlatform;
    @FXML private Label errLink;
    @FXML private Label errLieu;

    @FXML private TextField tfLieu;
    @FXML private TextArea taNotes;

    @FXML private Button btnSave;

    private final EntretienService entretienService = new EntretienService();
    private final CandidatureService candidatureService = new CandidatureService();

    private static final String INVALID_CLASS = "rh-invalid";

    private static final List<String> BANNED_WORDS = Arrays.asList(
            "fuck",
            "putin",
            "merde"
    );

    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.+", Pattern.CASE_INSENSITIVE);
    private static final Pattern WIN_ABS_PATH_PATTERN = Pattern.compile("^[a-zA-Z]:\\\\.+");
    private static final Pattern WIN_UNC_PATH_PATTERN = Pattern.compile("^\\\\\\\\.+");

    private static final Pattern LETTERS_SPACE_PATTERN = Pattern.compile("^[\\p{L}\\s]*$");

    private Integer editingEntretienId = null;
    private Integer idCandidature = null;

    @FXML
    private void initialize() {
        

        if (dpDate != null) {
            dpDate.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setDisable(empty);
                        return;
                    }
                    setDisable(!item.isAfter(LocalDate.now()));
                }
            });
        }

        if (cbHour != null) {
            List<String> hours = new ArrayList<>();
            for (int i = 9; i <= 16; i++) hours.add(String.format("%02d", i));
            cbHour.getItems().setAll(hours);
            cbHour.getSelectionModel().selectFirst();
        }
        if (cbMinute != null) {
            cbMinute.getItems().setAll("00", "15", "30", "45");
            cbMinute.getSelectionModel().select("00");
        }
        if (cbMode != null) {
            cbMode.getItems().setAll("En ligne", "Présentiel");
            cbMode.getSelectionModel().selectFirst();
            cbMode.valueProperty().addListener((obs, o, n) -> {
                updateModeUI();
                validateModeLive();
                validatePlatformLive();
                validateLinkLive();
                validateLieuLive();
            });
        }

        if (tbMeet != null) {
            tbMeet.setToggleGroup(platformGroup);
            tbMeet.setUserData("Google Meet");
        }
        if (tbTeams != null) {
            tbTeams.setToggleGroup(platformGroup);
            tbTeams.setUserData("Microsoft Teams");
        }
        if (tbZoom != null) {
            tbZoom.setToggleGroup(platformGroup);
            tbZoom.setUserData("Zoom");
        }
        platformGroup.selectedToggleProperty().addListener((obs, o, n) -> markValid(tbMeet));

        if (taNotes != null) {
            // Message: pas de chiffres ni caractères spéciaux (lettres + espaces + retours ligne)
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String next = change.getControlNewText();
                if (next == null) return change;
                return LETTERS_SPACE_PATTERN.matcher(next).matches() ? change : null;
            };
            taNotes.setTextFormatter(new TextFormatter<>(filter));
        }

        if (tfLieu != null) {
            // Lieu: pas de chiffres ni caractères spéciaux (lettres + espaces)
            UnaryOperator<TextFormatter.Change> filter = change -> {
                String next = change.getControlNewText();
                if (next == null) return change;
                return LETTERS_SPACE_PATTERN.matcher(next).matches() ? change : null;
            };
            tfLieu.setTextFormatter(new TextFormatter<>(filter));
        }

        dpDate.valueProperty().addListener((obs, o, n) -> validateDateLive());
        if (cbHour != null) cbHour.valueProperty().addListener((obs, o, n) -> validateTimeLive());
        if (cbMinute != null) cbMinute.valueProperty().addListener((obs, o, n) -> validateTimeLive());
        if (tfLieu != null) tfLieu.textProperty().addListener((obs, o, n) -> validateLieuLive());
        if (taNotes != null) taNotes.textProperty().addListener((obs, o, n) -> validateNotesLive());
        if (tfMeetingLink != null) tfMeetingLink.textProperty().addListener((obs, o, n) -> validateLinkLive());
        platformGroup.selectedToggleProperty().addListener((obs, o, n) -> validatePlatformLive());

        updateModeUI();
    }

    public void openForCreate(int idCandidature) {
        this.idCandidature = idCandidature;
        this.editingEntretienId = null;
        updateCandidateUI();

        if (btnSave != null) {
            btnSave.setText("Ajouter");
        }

        if (cbMode != null) {
            cbMode.getSelectionModel().selectFirst();
            updateModeUI();
        }
    }

    public void openForEdit(Entretien e) {
        if (e == null) return;
        this.editingEntretienId = e.getId_entretien();
        this.idCandidature = e.getId_candidature();

        updateCandidateUI();

        if (btnSave != null) {
            btnSave.setText("Ajouter");
        }

        if (e.getDate_heure() != null) {
            dpDate.setValue(e.getDate_heure().toLocalDate());
            if (cbHour != null) cbHour.getSelectionModel().select(String.format("%02d", e.getDate_heure().getHour()));
            if (cbMinute != null) cbMinute.getSelectionModel().select(String.format("%02d", e.getDate_heure().getMinute()));
        }
        if (cbMode != null) {
            cbMode.getSelectionModel().select(safe(e.getMode()).isBlank() ? "En ligne" : e.getMode());
        }
        selectPlatformToggle(e.getPlatform());
        if (tfLieu != null) {
            tfLieu.setText(e.getLieu());
        }

        String rawNotes = safe(e.getNotes());
        String meetingLink = extractMeetingLink(rawNotes);
        if (tfMeetingLink != null) {
            tfMeetingLink.setText(meetingLink);
        }
        if (taNotes != null) {
            taNotes.setText(removeMeetingLinkLine(rawNotes));
        }

        updateModeUI();
    }

    private void updateCandidateUI() {
        String who = "";
        if (idCandidature != null) {
            try {
                Candidature c = candidatureService.getCandidatureById(idCandidature);
                if (c != null) {
                    who = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
                }
            } catch (Exception ignored) {
            }
        }

        if (lblCandidateName != null) {
            lblCandidateName.setText(who.isBlank() ? "Candidat" : who);
        }

        if (title != null) title.setText("Entretien");
    }

    private void updateModeUI() {
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        boolean online = "En ligne".equalsIgnoreCase(mode);

        if (lblRightTitle != null) {
            lblRightTitle.setText(online ? "Planifier la réunion" : "Lieu");
        }

        if (boxPlatform != null) {
            boxPlatform.setVisible(online);
            boxPlatform.setManaged(online);
        }
        if (boxLieu != null) {
            boxLieu.setVisible(!online);
            boxLieu.setManaged(!online);
        }

        if (online) {
            if (tfLieu != null) tfLieu.setText("");
        } else {
            platformGroup.selectToggle(null);
            if (tfMeetingLink != null) tfMeetingLink.setText("");
        }

        if (btnOpenPlatform != null) {
            btnOpenPlatform.setDisable(!online);
        }
    }

    @FXML
    private void openPlatform() {
        String link = (tfMeetingLink == null) ? "" : safe(tfMeetingLink.getText());
        String platform = selectedPlatform();
        String target = !link.isBlank() ? link : platformUrl(platform);
        if (safe(target).isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Veuillez d'abord choisir une plateforme ou saisir un lien.").show();
            return;
        }
        try {
            if (Desktop.isDesktopSupported()) {
                if (isWindowsPath(target)) {
                    Desktop.getDesktop().open(new File(target));
                } else {
                    String url = normalizeUrl(target);
                    Desktop.getDesktop().browse(new URI(url));
                }
            } else {
                new Alert(Alert.AlertType.INFORMATION, "Ouverture du navigateur non supportée.").show();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la plateforme.").show();
        }
    }

    private String platformUrl(String platform) {
        String p = safe(platform).toLowerCase(Locale.ROOT);
        if (p.contains("meet")) return "https://meet.google.com/";
        if (p.contains("teams")) return "https://teams.microsoft.com/";
        if (p.contains("zoom")) return "https://zoom.us/";
        return "";
    }

    private String normalizeUrl(String input) {
        String s = safe(input);
        if (s.isBlank()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://" + s;
    }

    private boolean isWindowsPath(String input) {
        String s = safe(input);
        return WIN_ABS_PATH_PATTERN.matcher(s).matches() || WIN_UNC_PATH_PATTERN.matcher(s).matches();
    }

    private boolean isValidUrlOrPath(String input) {
        String s = safe(input);
        if (s.isBlank()) return false;
        if (isWindowsPath(s)) {
            try {
                return new File(s).exists();
            } catch (Exception ignored) {
                return false;
            }
        }
        String url = normalizeUrl(s);
        return URL_PATTERN.matcher(url).matches();
    }

    private String selectedPlatform() {
        Toggle t = platformGroup.getSelectedToggle();
        return (t == null || t.getUserData() == null) ? "" : safe(String.valueOf(t.getUserData()));
    }

    private void selectPlatformToggle(String platform) {
        String p = safe(platform);
        if (p.isBlank()) {
            platformGroup.selectToggle(null);
            return;
        }
        if (tbMeet != null && p.equalsIgnoreCase("Google Meet")) platformGroup.selectToggle(tbMeet);
        else if (tbTeams != null && (p.equalsIgnoreCase("Microsoft Teams") || p.equalsIgnoreCase("Teams"))) platformGroup.selectToggle(tbTeams);
        else if (tbZoom != null && p.equalsIgnoreCase("Zoom")) platformGroup.selectToggle(tbZoom);
        else platformGroup.selectToggle(null);
    }

    private String extractMeetingLink(String notes) {
        String s = safe(notes);
        String key = "Lien:";
        int idx = s.toLowerCase(Locale.ROOT).indexOf(key.toLowerCase(Locale.ROOT));
        if (idx < 0) return "";
        String tail = s.substring(idx + key.length()).trim();
        int end = tail.indexOf('\n');
        if (end >= 0) tail = tail.substring(0, end).trim();
        return tail;
    }

    private String removeMeetingLinkLine(String notes) {
        String s = safe(notes);
        if (s.isBlank()) return "";

        StringBuilder out = new StringBuilder();
        String[] lines = s.split("\\R", -1);
        for (String line : lines) {
            String trimmed = (line == null) ? "" : line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("lien:")) {
                continue;
            }
            if (out.length() > 0) out.append("\n");
            out.append(line == null ? "" : line);
        }
        return out.toString().trim();
    }

    private String injectMeetingLink(String notes, String link) {
        String n = safe(notes);
        String l = safe(link);
        if (l.isBlank()) return n;
        String line = "Lien: " + l;
        if (n.isBlank()) return line;
        if (n.toLowerCase(Locale.ROOT).contains("lien:")) return n;
        return n + "\n" + line;
    }

    @FXML
    private void save() {
        clearValidation();
        validateAllLive();

        if (idCandidature == null) {
            new Alert(Alert.AlertType.ERROR, "Candidature manquante.").show();
            return;
        }

        LocalDate d = dpDate.getValue();
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        String lieu = safe(tfLieu.getText());
        String platform = selectedPlatform();
        String meetLink = (tfMeetingLink == null) ? "" : safe(tfMeetingLink.getText());

        List<String> errors = new ArrayList<>();

        if (!validateDateLive()) errors.add("- Date: invalide");

        LocalTime t = null;
        String h = (cbHour == null) ? "" : safe(cbHour.getValue());
        String m = (cbMinute == null) ? "" : safe(cbMinute.getValue());
        if (!validateTimeLive()) errors.add("- Heure du RDV: invalide");
        else {
            try {
                t = LocalTime.of(Integer.parseInt(h), Integer.parseInt(m));
            } catch (Exception ignored) {
            }
        }

        if (!validateModeLive()) errors.add("- Mode: obligatoire");

        boolean isOnline = "En ligne".equalsIgnoreCase(mode);
        if (isOnline) {
            if (!validatePlatformLive()) errors.add("- Plateforme: obligatoire");
            if (!validateLinkLive()) errors.add("- Lien / Path: obligatoire");
        } else {
            if (!validateLieuLive()) errors.add("- Lieu: obligatoire");
        }

        String statut = "PLANIFIE";

        String notesTxt = (taNotes == null) ? "" : safe(taNotes.getText());
        if (!validateNotesLive()) errors.add("- Message: invalide");

        if (!errors.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Veuillez corriger les champs:\n" + String.join("\n", errors)).show();
            return;
        }

        Entretien e = new Entretien();
        if (editingEntretienId != null) e.setId_entretien(editingEntretienId);
        e.setId_candidature(idCandidature);
        e.setDate(d);
        e.setHeure((t.getHour() * 100) + t.getMinute());
        e.setMode(mode);
        e.setStatut(statut);
        if (isOnline) {
            e.setPlatform(platform);
            e.setLieu(null);
        } else {
            e.setLieu(lieu);
            e.setPlatform(null);
        }
        e.setNotes(isOnline ? injectMeetingLink(notesTxt, meetLink) : notesTxt);

        try {
            if (editingEntretienId == null) {
                entretienService.ajouterEntretien(e);
            } else {
                entretienService.modifierEntretien(e);
            }
            new Alert(Alert.AlertType.INFORMATION, "Entretien enregistré.").show();
            back();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement.").show();
        }
    }

    @FXML
    private void back() {
        try {
            Parent root = dpDate.getScene().getRoot();
            var center = root.lookup("#adminContentArea");
            if (!(center instanceof javafx.scene.layout.Pane pane)) {
                return;
            }
            var url = getClass().getResource("/candidaturefxml/RH_Entretiens.fxml");
            if (url == null) return;
            Parent view = javafx.fxml.FXMLLoader.load(url);
            pane.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void clearValidation() {
        markValid(dpDate);
        markValid(cbHour);
        markValid(cbMinute);
        markValid(cbMode);
        markValid(tbMeet);
        markValid(tfMeetingLink);
        markValid(tfLieu);
        markValid(taNotes);
    }

    private void markInvalid(Control c) {
        if (c == null) return;
        if (!c.getStyleClass().contains(INVALID_CLASS)) {
            c.getStyleClass().add(INVALID_CLASS);
        }
    }

    private void markValid(Control c) {
        if (c == null) return;
        c.getStyleClass().remove(INVALID_CLASS);
    }

    private void setError(Label lbl, String msg) {
        if (lbl == null) return;
        String m = safe(msg);
        lbl.setText(m);
        boolean show = !m.isBlank();
        lbl.setVisible(show);
        lbl.setManaged(show);
    }

    private void validateAllLive() {
        validateDateLive();
        validateTimeLive();
        validateModeLive();
        validateNotesLive();
        validatePlatformLive();
        validateLinkLive();
        validateLieuLive();
    }

    private boolean validateDateLive() {
        if (dpDate == null) return true;
        LocalDate d = dpDate.getValue();
        if (d == null) {
            markInvalid(dpDate);
            setError(errDate, "Date obligatoire");
            return false;
        }
        if (!d.isAfter(LocalDate.now())) {
            markInvalid(dpDate);
            setError(errDate, "Choisissez une date à partir de demain");
            return false;
        }
        markValid(dpDate);
        setError(errDate, "");
        return true;
    }

    private boolean validateTimeLive() {
        String h = (cbHour == null) ? "" : safe(cbHour.getValue());
        String m = (cbMinute == null) ? "" : safe(cbMinute.getValue());
        if (h.isBlank() || m.isBlank()) {
            markInvalid(cbHour);
            markInvalid(cbMinute);
            setError(errTime, "Heure obligatoire");
            return false;
        }
        try {
            LocalTime t = LocalTime.of(Integer.parseInt(h), Integer.parseInt(m));
            if (t.getHour() < 9 || t.getHour() > 16) {
                markInvalid(cbHour);
                markInvalid(cbMinute);
                setError(errTime, "Entre 09:00 et 16:00");
                return false;
            }
        } catch (Exception ex) {
            markInvalid(cbHour);
            markInvalid(cbMinute);
            setError(errTime, "Heure invalide");
            return false;
        }
        markValid(cbHour);
        markValid(cbMinute);
        setError(errTime, "");
        return true;
    }

    private boolean validateModeLive() {
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        if (mode.isBlank()) {
            markInvalid(cbMode);
            setError(errMode, "Mode obligatoire");
            return false;
        }
        markValid(cbMode);
        setError(errMode, "");
        return true;
    }

    private boolean validateNotesLive() {
        if (taNotes == null) return true;
        String notes = safe(taNotes.getText());
        if (notes.isBlank()) {
            markInvalid(taNotes);
            setError(errNotes, "Message obligatoire");
            return false;
        }
        if (!LETTERS_SPACE_PATTERN.matcher(notes).matches()) {
            markInvalid(taNotes);
            setError(errNotes, "Pas de chiffres ni caractères spéciaux");
            return false;
        }
        if (containsBannedWords(notes)) {
            markInvalid(taNotes);
            setError(errNotes, "Pas d’insulte");
            return false;
        }
        markValid(taNotes);
        setError(errNotes, "");
        return true;
    }

    private boolean validatePlatformLive() {
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        if (!"En ligne".equalsIgnoreCase(mode)) {
            setError(errPlatform, "");
            markValid(tbMeet);
            markValid(tbTeams);
            markValid(tbZoom);
            return true;
        }
        if (selectedPlatform().isBlank()) {
            markInvalid(tbMeet);
            markInvalid(tbTeams);
            markInvalid(tbZoom);
            setError(errPlatform, "Choisissez une plateforme");
            return false;
        }
        markValid(tbMeet);
        markValid(tbTeams);
        markValid(tbZoom);
        setError(errPlatform, "");
        return true;
    }

    private boolean validateLinkLive() {
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        if (!"En ligne".equalsIgnoreCase(mode)) {
            if (tfMeetingLink != null) markValid(tfMeetingLink);
            setError(errLink, "");
            return true;
        }
        String link = (tfMeetingLink == null) ? "" : safe(tfMeetingLink.getText());
        if (link.isBlank()) {
            markInvalid(tfMeetingLink);
            setError(errLink, "Lien/Path obligatoire");
            return false;
        }
        if (containsBannedWords(link)) {
            markInvalid(tfMeetingLink);
            setError(errLink, "Pas d’insulte");
            return false;
        }
        if (!isValidUrlOrPath(link)) {
            markInvalid(tfMeetingLink);
            setError(errLink, "Lien/Path invalide");
            return false;
        }
        markValid(tfMeetingLink);
        setError(errLink, "");
        return true;
    }

    private boolean validateLieuLive() {
        String mode = (cbMode == null) ? "" : safe(cbMode.getValue());
        if (!"Présentiel".equalsIgnoreCase(mode)) {
            if (tfLieu != null) markValid(tfLieu);
            setError(errLieu, "");
            return true;
        }
        String lieu = safe(tfLieu.getText());
        if (lieu.isBlank()) {
            markInvalid(tfLieu);
            setError(errLieu, "Lieu obligatoire");
            return false;
        }
        if (!LETTERS_SPACE_PATTERN.matcher(lieu).matches()) {
            markInvalid(tfLieu);
            setError(errLieu, "Pas de chiffres ni caractères spéciaux");
            return false;
        }
        markValid(tfLieu);
        setError(errLieu, "");
        return true;
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private boolean containsBannedWords(String text) {
        String s = safe(text).toLowerCase(Locale.ROOT);
        for (String w : BANNED_WORDS) {
            if (s.contains(w.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
