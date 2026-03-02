package Controllers.candidature;

import Entities.Offre.OffreEmploi;
import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Services.candidature.CandidatureService;
import Services.candidature.CertificationService;
import Services.candidature.ExperienceService;
import Services.candidature.OpenAILettreMotivationService;

import Utils.Session;
import Entities.User.User;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class AjouterCandidature {

    @FXML private AnchorPane stepperPane;
    @FXML private Region progressLine;
    @FXML private ImageView runner;

    @FXML private Label progressPercentLabel;

    @FXML private VBox stepItem0, stepItem1, stepItem2, stepItem3, stepItem4;

    @FXML private VBox step0, step1, step2, step3, step4;

    @FXML private Button btnNext, btnBack;

    @FXML private Button btnFillFromAccount;

    @FXML private TextField tfPrenom, tfNom, tfEmail, tfAdresse, tfVille;

    @FXML private DatePicker dpNaissance;

    @FXML private ComboBox<String> cbIndicatif;
    @FXML private TextField tfTelLocal;

    @FXML private VBox experienceContainer;
    @FXML private VBox certificationContainer;
    @FXML private TextField tfSkills;

    @FXML private Button btnSelectFile;
    @FXML private Label lblSelectedFile;

    @FXML private Label reviewLabel;

    // STEP 2 - Formation
    @FXML private RadioButton rbBachelor, rbEngineering, rbMasters, rbPhd;
    @FXML private ComboBox<String> cbInstitution;
    @FXML private TextArea taMotivationLetter;

    @FXML private Label formTitle;
    @FXML private Label formSubtitle;

    @FXML private Label heroTitle;
    @FXML private Label heroSubtitle;

    private ToggleGroup tgDegree;

    private File selectedAttachment;

    private final List<Image> runnerFrames = new ArrayList<>();
    private Timeline walkFramesTimeline;
    private int idleFrameIndex = 0;

    private ParallelTransition runnerAll;
    private ChangeListener<Animation.Status> statusListener;

    private int currentStep = 0;
    private boolean isAnimating = false;

    private boolean editMode = false;
    private Integer editingId = null;

    //lettre motivation ai
    @FXML private Button btnGenerateAI;

    // STEP 3 - Motivation Video
    @FXML private Button btnRecordVideo, btnStopVideo, btnUploadVideo;
    @FXML private Label lblVideoStatus;

    @FXML private CheckBox cbConfirmInfo;
    @FXML private CheckBox cbAcceptPrivacy;

    private Integer offreId = null;
    private String offreTitre = null;


    private Process ffmpegProcess;
    private File recordedVideoFile;
    private File selectedVideoFile;

    private final String FFMPEG =
            "C:\\Users\\MSI\\Downloads\\ffmpeg-8.0.1-essentials_build\\ffmpeg-8.0.1-essentials_build\\bin\\ffmpeg.exe";

    private final String VIDEO_DEVICE = "HD Webcam";
    private final String AUDIO_DEVICE = "Réseau de microphones (Technologie Intel® Smart Sound pour microphones numériques)";

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern LETTERS_PATTERN = Pattern.compile("^[\\p{L}][\\p{L}\\s'\\-]*$");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[\\p{L}\\d\\s]+$");
    private static final List<String> BANNED_WORDS = Arrays.asList("putin", "merde", "fuck");

    public void setEditCandidature(Candidature cand) {
        if (cand == null) return;
        this.editMode = true;
        this.editingId = cand.getIDCandidat();

        if (heroTitle != null) heroTitle.setText("Modifier Candidature");
        if (heroSubtitle != null) heroSubtitle.setText("Mettez à jour vos informations");

        if (tfPrenom != null) tfPrenom.setText(safe(cand.getPrenom()));
        if (tfNom != null) tfNom.setText(safe(cand.getNom()));
        if (tfEmail != null) tfEmail.setText(safe(cand.getEmail()));
        if (tfAdresse != null) tfAdresse.setText(safe(cand.getAdresse()));
        if (tfVille != null) tfVille.setText(safe(cand.getVille()));

        if (dpNaissance != null) {
            if (cand.getDate_naissance() != null) dpNaissance.setValue(cand.getDate_naissance().toLocalDate());
            else dpNaissance.setValue(null);
        }

        if (cbIndicatif != null) cbIndicatif.setValue(safe(cand.getIndicatif()));
        if (tfTelLocal != null) tfTelLocal.setText(safe(cand.getTel()));

        selectDegree(cand.getHighest_degree());
        if (cbInstitution != null) cbInstitution.setValue(safe(cand.getInstitution()));
        if (taMotivationLetter != null) taMotivationLetter.setText(safe(cand.getLettre_motivation()));
        if (tfSkills != null) tfSkills.setText(safe(cand.getSkills_text()));

        String pj = safe(cand.getPieces_jointes()).trim();
        if (!pj.isBlank()) {
            File f = new File(pj);
            selectedAttachment = f.exists() ? f : null;
        } else {
            selectedAttachment = null;
        }
        if (lblSelectedFile != null) {
            lblSelectedFile.setText(selectedAttachment == null
                    ? "Aucun fichier sélectionné"
                    : "Fichier sélectionné : " + selectedAttachment.getName());
        }

        String vp = safe(cand.getVideo_path()).trim();
        if (!vp.isBlank()) {
            File vf = new File(vp);
            selectedVideoFile = vf.exists() ? vf : null;
        } else {
            selectedVideoFile = null;
        }
        if (lblVideoStatus != null) {
            lblVideoStatus.setText(selectedVideoFile == null ? "No video yet" : "Vidéo: " + selectedVideoFile.getPath());
        }

        if (cbConfirmInfo != null) cbConfirmInfo.setSelected(cand.isConfirm_info());
        if (cbAcceptPrivacy != null) cbAcceptPrivacy.setSelected(cand.isAccept_privacy());

        reloadExperiencesAndCertificationsForEdit(editingId);
        updateButtons();
    }

    @FXML
    private void fillFromConnectedUser(ActionEvent event) {
        User u = Session.getCurrentUser();
        if (u == null) {
            new Alert(Alert.AlertType.WARNING, "Aucun utilisateur connecté.").show();
            return;
        }

        String fullName = u.getFullName() != null ? u.getFullName().trim() : "";
        String email = u.getEmail() != null ? u.getEmail().trim() : "";
        String phone = u.getPhone() != null ? u.getPhone().trim() : "";

        String prenom = "";
        String nom = "";
        if (!fullName.isBlank()) {
            String[] parts = fullName.split("\\s+");
            if (parts.length == 1) {
                prenom = parts[0];
            } else {
                prenom = parts[0];
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < parts.length; i++) {
                    if (parts[i] == null || parts[i].isBlank()) continue;
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(parts[i]);
                }
                nom = sb.toString();
            }
        }

        if (tfPrenom != null && (tfPrenom.getText() == null || tfPrenom.getText().isBlank())) {
            tfPrenom.setText(prenom);
        }
        if (tfNom != null && (tfNom.getText() == null || tfNom.getText().isBlank())) {
            tfNom.setText(nom);
        }
        if (tfEmail != null && (tfEmail.getText() == null || tfEmail.getText().isBlank())) {
            tfEmail.setText(email);
        }
        if (tfTelLocal != null && (tfTelLocal.getText() == null || tfTelLocal.getText().isBlank())) {
            tfTelLocal.setText(phone);
        }
    }

    @FXML
    void submit(ActionEvent event) {
        System.out.println("SUBMIT appelé - editMode=" + editMode + " editingId=" + editingId);

        clearValidationUI();
        List<String> errors = new ArrayList<>();
        if (!validateAll(errors)) {
            showValidationSummary(errors);
            return;
        }

        CandidatureService cs = new CandidatureService();


        Candidature c = new Candidature();

        var currentUser = Session.getCurrentUser();
        if (currentUser != null) {
            c.setUserId(currentUser.getId());
        }

        c.setPrenom(tfPrenom.getText());
        c.setNom(tfNom.getText());

        if (currentUser != null && currentUser.getEmail() != null && !currentUser.getEmail().trim().isBlank()) {
            c.setEmail(currentUser.getEmail().trim());
        } else {
            c.setEmail(tfEmail.getText());
        }

        String ind = cbIndicatif.getValue();
        if (ind != null && ind.contains(" ")) ind = ind.split(" ")[0];
        c.setIndicatif(ind);

        c.setTel(tfTelLocal.getText());
        c.setAdresse(tfAdresse.getText());
        c.setVille(tfVille.getText());

        if (dpNaissance.getValue() != null) {
            c.setDate_naissance(java.sql.Date.valueOf(dpNaissance.getValue()));
        } else {
            c.setDate_naissance(null);
        }

        c.setHighest_degree(getSelectedDegree());

        c.setInstitution(cbInstitution != null ? cbInstitution.getValue() : null);
        c.setLettre_motivation(taMotivationLetter != null ? taMotivationLetter.getText() : null);

        c.setSkills_text(tfSkills != null ? tfSkills.getText() : null);

        c.setPieces_jointes(selectedAttachment != null ? selectedAttachment.getAbsolutePath() : null);

        File finalVideo = (selectedVideoFile != null) ? selectedVideoFile : recordedVideoFile;
        try {
            c.setVideo_path(persistMediaAndGetDbPath(finalVideo, "uploads/videos", "video"));
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Erreur", "Impossible de sauvegarder la vidéo localement.");
            return;
        }

        c.setConfirm_info(cbConfirmInfo != null && cbConfirmInfo.isSelected());
        c.setAccept_privacy(cbAcceptPrivacy != null && cbAcceptPrivacy.isSelected());

        int idCand;
        if (editMode && editingId != null) {
            c.setIDCandidat(editingId);
            cs.modifierCandidature(c);
            idCand = editingId;

            new ExperienceService().supprimerExperiencesParCandidature(idCand);
            new CertificationService().supprimerCertificationsParCandidature(idCand);

        } else {

            //jdidddddd
            System.out.println("DEBUG offreId = " + offreId);
            c.setOffreId(offreId); // ← ajouter cette ligne
            idCand = cs.ajouterCandidatureAndReturnId(c);
        }

        ExperienceService es = new ExperienceService();
        if (experienceContainer != null) {
            for (Node n : experienceContainer.getChildren()) {
                if (!(n instanceof VBox card)) continue;

                Object ud = card.getUserData();
                if (!(ud instanceof Object[] arr)) continue;

                TextField tfPoste = (TextField) arr[0];
                TextField tfEnt = (TextField) arr[1];
                DatePicker dpFrom = (DatePicker) arr[2];
                DatePicker dpTo = (DatePicker) arr[3];

                String poste = tfPoste.getText();
                String ent = tfEnt.getText();

                java.sql.Date debut = (dpFrom.getValue() != null) ? java.sql.Date.valueOf(dpFrom.getValue()) : null;
                java.sql.Date fin = (dpTo.getValue() != null) ? java.sql.Date.valueOf(dpTo.getValue()) : null;

                if ((poste == null || poste.isBlank()) && (ent == null || ent.isBlank())) continue;

                Experience exp = new Experience();
                exp.setId_candidature(idCand);
                exp.setPoste(poste);
                exp.setEntreprise(ent);
                exp.setDate_debut(debut);
                exp.setDate_fin(fin);

                es.ajouterExperience(exp);
            }
        }

        CertificationService certS = new CertificationService();
        if (certificationContainer != null) {
            for (Node n : certificationContainer.getChildren()) {
                if (!(n instanceof VBox card)) continue;

                Object ud = card.getUserData();
                if (!(ud instanceof Object[] arr) || arr.length < 5) continue;

                TextField tfOrg = (TextField) arr[0];
                TextField tfCertif = (TextField) arr[1];
                TextField tfNum = (TextField) arr[2];
                DatePicker dpIssued = (DatePicker) arr[3];
                DatePicker dpExpire = (DatePicker) arr[4];
                CheckBox cbNoExpire = (arr.length > 5 && arr[5] instanceof CheckBox) ? (CheckBox) arr[5] : null;

                java.sql.Date obt = (dpIssued.getValue() != null) ? java.sql.Date.valueOf(dpIssued.getValue()) : null;
                java.sql.Date expDate = null;
                if (cbNoExpire == null || !cbNoExpire.isSelected()) {
                    expDate = (dpExpire.getValue() != null) ? java.sql.Date.valueOf(dpExpire.getValue()) : null;
                }

                if ((tfOrg.getText() == null || tfOrg.getText().isBlank()) &&
                        (tfCertif.getText() == null || tfCertif.getText().isBlank())) continue;

                if (obt == null) {
                    showAlert("Champ obligatoire", "La date d'obtention est obligatoire pour une certification.");
                    return;
                }

                Integer numero = null;
                try {
                    if (tfNum.getText() != null && !tfNum.getText().isBlank()) {
                        numero = Integer.parseInt(tfNum.getText().trim());
                    }
                } catch (Exception ignored) {
                }

                Certification cert = new Certification();
                cert.setId_candidature(idCand);
                cert.setOrganisme(tfOrg.getText());
                cert.setNom_certification(tfCertif.getText());
                cert.setNumero(numero);
                cert.setDate_obtention(obt);
                cert.setDate_expiration(expDate);

                certS.ajouterCertification(cert);
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(editMode ? "Candidature modifiée" : "Candidature ajoutée");
        alert.setHeaderText(null);
        alert.setContentText(editMode
                ? "Votre candidature a été mise à jour avec succès."
                : "Votre candidature a été enregistrée avec succès.");
        alert.show();

        ouvrirMesCandidaturesDansDashboard();
    }

    private String persistMediaAndGetDbPath(File src, String relativeDir, String prefix) throws Exception {
        if (src == null) return null;
        if (!src.exists()) return null;

        String ext = "";
        String name = src.getName();
        int dot = name.lastIndexOf('.');
        if (dot >= 0 && dot < name.length() - 1) {
            ext = name.substring(dot);
        }

        Path dir = Paths.get(relativeDir);
        Files.createDirectories(dir);

        String fileName = prefix + "_" + System.currentTimeMillis() + ext;
        Path dest = dir.resolve(fileName);

        Files.copy(src.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
        return relativeDir.replace('\\', '/') + "/" + fileName;
    }

    private void clearValidationUI() {
        clearInvalid(tfPrenom);
        clearInvalid(tfNom);
        clearInvalid(tfEmail);
        clearInvalid(cbIndicatif);
        clearInvalid(tfTelLocal);
        clearInvalid(tfAdresse);
        clearInvalid(tfVille);
        clearInvalid(dpNaissance);
        clearInvalid(cbInstitution);
        clearInvalid(tfSkills);
        clearInvalid(taMotivationLetter);
    }

    private void selectDegree(String degree) {
        if (tgDegree == null) return;
        String d = safe(degree);
        for (Toggle t : tgDegree.getToggles()) {
            if (!(t instanceof RadioButton rb)) continue;
            Object ud = rb.getUserData();
            if (!(ud instanceof String s)) continue;
            if (s.equalsIgnoreCase(d)) {
                tgDegree.selectToggle(t);
                return;
            }
        }
    }

    private void reloadExperiencesAndCertificationsForEdit(int idCand) {
        try {
            if (experienceContainer != null) {
                experienceContainer.getChildren().clear();
                List<Experience> exps = new ExperienceService().afficherExperiencesParCandidature(idCand);
                if (exps != null) {
                    for (Experience e : exps) {
                        Node n = buildExperienceBlock();
                        if (n instanceof VBox card) fillExperienceBlock(card, e);
                        experienceContainer.getChildren().add(n);
                    }
                }
                if (experienceContainer.getChildren().isEmpty()) addExperience();
            }

            if (certificationContainer != null) {
                certificationContainer.getChildren().clear();
                List<Certification> certs = new CertificationService().afficherCertificationsParCandidature(idCand);
                if (certs != null) {
                    for (Certification c : certs) {
                        Node n = buildCertificationBlock();
                        if (n instanceof VBox card) fillCertificationBlock(card, c);
                        certificationContainer.getChildren().add(n);
                    }
                }
                if (certificationContainer.getChildren().isEmpty()) addCertification();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void fillExperienceBlock(VBox card, Experience e) {
        if (card == null || e == null) return;
        Object ud = card.getUserData();
        if (!(ud instanceof Object[] arr) || arr.length < 4) return;
        TextField poste = (TextField) arr[0];
        TextField ent = (TextField) arr[1];
        DatePicker from = (DatePicker) arr[2];
        DatePicker to = (DatePicker) arr[3];
        if (poste != null) poste.setText(safe(e.getPoste()));
        if (ent != null) ent.setText(safe(e.getEntreprise()));
        if (from != null) from.setValue(e.getDate_debut() == null ? null : e.getDate_debut().toLocalDate());
        if (to != null) to.setValue(e.getDate_fin() == null ? null : e.getDate_fin().toLocalDate());
    }

    private void fillCertificationBlock(VBox card, Certification c) {
        if (card == null || c == null) return;
        Object ud = card.getUserData();
        if (!(ud instanceof Object[] arr) || arr.length < 5) return;
        TextField org = (TextField) arr[0];
        TextField certif = (TextField) arr[1];
        TextField num = (TextField) arr[2];
        DatePicker issued = (DatePicker) arr[3];
        DatePicker expire = (DatePicker) arr[4];
        CheckBox noExpire = (arr.length > 5 && arr[5] instanceof CheckBox) ? (CheckBox) arr[5] : null;

        if (org != null) org.setText(safe(c.getOrganisme()));
        if (certif != null) certif.setText(safe(c.getNom_certification()));
        if (num != null) num.setText(c.getNumero() == null ? "" : String.valueOf(c.getNumero()));
        if (issued != null) issued.setValue(c.getDate_obtention() == null ? null : c.getDate_obtention().toLocalDate());
        if (expire != null) expire.setValue(c.getDate_expiration() == null ? null : c.getDate_expiration().toLocalDate());
        if (noExpire != null) {
            boolean noExp = c.getDate_expiration() == null;
            noExpire.setSelected(noExp);
            if (expire != null) expire.setDisable(noExp);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void clearInvalid(Control c) {
        if (c == null) return;
        c.getStyleClass().remove("field-invalid");
        c.setTooltip(null);
    }

    private void markInvalid(Control c, String message) {
        if (c == null) return;
        if (!c.getStyleClass().contains("field-invalid")) c.getStyleClass().add("field-invalid");
        Tooltip t = new Tooltip(message);
        t.getStyleClass().add("validation-tooltip");
        c.setTooltip(t);
    }

    private boolean validateAll(List<String> errors) {
        boolean ok = true;

        ok &= validateRequiredText(tfPrenom, "Prénom", errors);
        ok &= validateRequiredText(tfNom, "Nom", errors);
        ok &= validateRequiredText(tfEmail, "Email", errors);
        ok &= validateRequiredText(tfAdresse, "Adresse", errors);
        ok &= validateRequiredText(tfVille, "Ville", errors);

        if (tfPrenom != null && !isBlank(tfPrenom.getText()) && !LETTERS_PATTERN.matcher(tfPrenom.getText().trim()).matches()) {
            ok = false;
            errors.add("Prénom : uniquement des caractères");
            markInvalid(tfPrenom, "Uniquement des caractères");
        }
        if (tfNom != null && !isBlank(tfNom.getText()) && !LETTERS_PATTERN.matcher(tfNom.getText().trim()).matches()) {
            ok = false;
            errors.add("Nom : uniquement des caractères");
            markInvalid(tfNom, "Uniquement des caractères");
        }

        if (tfEmail != null && !isBlank(tfEmail.getText()) && !EMAIL_PATTERN.matcher(tfEmail.getText().trim()).matches()) {
            ok = false;
            errors.add("Email : format invalide");
            markInvalid(tfEmail, "Exemple: nom@domaine.com");
        }

        if (tfAdresse != null && !isBlank(tfAdresse.getText()) && !ADDRESS_PATTERN.matcher(tfAdresse.getText().trim()).matches()) {
            ok = false;
            errors.add("Adresse : pas de caractères spéciaux");
            markInvalid(tfAdresse, "Uniquement lettres/chiffres/espaces");
        }

        if (cbIndicatif == null || cbIndicatif.getValue() == null || cbIndicatif.getValue().isBlank()) {
            ok = false;
            errors.add("Téléphone : indicatif obligatoire");
            markInvalid(cbIndicatif, "Sélectionnez un indicatif");
        }

        if (tfTelLocal == null || isBlank(tfTelLocal.getText())) {
            ok = false;
            errors.add("Téléphone : numéro obligatoire");
            markInvalid(tfTelLocal, "Entrez votre numéro");
        } else {
            String tel = tfTelLocal.getText().trim().replaceAll("\\s+", "");
            if (!DIGITS_PATTERN.matcher(tel).matches()) {
                ok = false;
                errors.add("Téléphone : uniquement des chiffres");
                markInvalid(tfTelLocal, "Uniquement des chiffres");
            }
        }

        if (dpNaissance == null || dpNaissance.getValue() == null) {
            ok = false;
            errors.add("Date de naissance : obligatoire");
            markInvalid(dpNaissance, "Sélectionnez une date");
        } else {
            LocalDate birth = dpNaissance.getValue();
            LocalDate today = LocalDate.now();
            if (birth.isAfter(today)) {
                ok = false;
                errors.add("Date de naissance : ne peut pas être dans le futur");
                markInvalid(dpNaissance, "Date invalide");
            } else {
                int age = Period.between(birth, today).getYears();
                if (age < 18) {
                    ok = false;
                    errors.add("Date de naissance : vous devez avoir au moins 18 ans");
                    markInvalid(dpNaissance, "Âge minimum: 18 ans");
                }
            }
        }

        String degree = getSelectedDegree();
        if (degree == null || degree.isBlank()) {
            ok = false;
            errors.add("Formation : diplôme obligatoire");
            if (rbBachelor != null) markInvalid(rbBachelor, "Choisissez un diplôme");
            if (rbEngineering != null) markInvalid(rbEngineering, "Choisissez un diplôme");
            if (rbMasters != null) markInvalid(rbMasters, "Choisissez un diplôme");
            if (rbPhd != null) markInvalid(rbPhd, "Choisissez un diplôme");
        }

        if (cbInstitution == null || cbInstitution.getValue() == null || cbInstitution.getValue().isBlank()) {
            ok = false;
            errors.add("Formation : institution obligatoire");
            markInvalid(cbInstitution, "Sélectionnez une institution");
        }

        ok &= validateRequiredText(tfSkills, "Compétences", errors);
        if (tfSkills != null && !isBlank(tfSkills.getText())) {
            String skills = tfSkills.getText().trim();
            if (!skills.chars().allMatch(ch -> Character.isLetter(ch) || Character.isWhitespace(ch) || ch == ',' || ch == ';' || ch == '-' || ch == '_')) {
                ok = false;
                errors.add("Compétences : uniquement des caractères (séparateurs autorisés: , ; - _)");
                markInvalid(tfSkills, "Uniquement des caractères (+ séparateurs)");
            }
        }

        /*ok &= validateRequiredText(taMotivationLetter, "Lettre de motivation", errors);
        if (taMotivationLetter != null && !isBlank(taMotivationLetter.getText())) {
            String raw = taMotivationLetter.getText();
           /* if (!raw.matches("[\\p{L}\\s]+")) {
                ok = false;
                errors.add("Lettre de motivation : uniquement des lettres (pas de chiffres ni caractères spéciaux)");
                markInvalid(taMotivationLetter, "Uniquement des lettres");
            }
            //agent ia lettre de motivation
            if (raw.length() < 50) {
                errors.add("Lettre de motivation trop courte (minimum 50 caractères)");
                markInvalid(taMotivationLetter, "Lettre trop courte");
            }

            String txt = raw.toLowerCase();
            for (String w : BANNED_WORDS) {
                if (txt.contains(w)) {
                    ok = false;
                    errors.add("Lettre de motivation : langage inapproprié détecté");
                    markInvalid(taMotivationLetter, "Veuillez retirer les mots inappropriés");
                    break;
                }
            }
        }*/
        ok &= validateRequiredText(taMotivationLetter, "Lettre de motivation", errors);
        if (taMotivationLetter != null && !isBlank(taMotivationLetter.getText())) {
            String raw = taMotivationLetter.getText();

            // Vérifie longueur minimale
            if (raw.length() < 50) {
                ok = false;
                errors.add("Lettre de motivation trop courte (minimum 50 caractères)");
                markInvalid(taMotivationLetter, "Lettre trop courte");
            }

            // Vérifie chiffres et caractères spéciaux interdits
            if (raw.matches(".*[0-9+=*/_<>^§#~°²%$£µ;@!|{}\\[\\]\\\\].*")) {
                ok = false;
                errors.add("Lettre de motivation : chiffres et caractères spéciaux interdits");
                markInvalid(taMotivationLetter, "Chiffres et caractères spéciaux interdits");
            }

            // Vérifie insultes
            String txt = raw.toLowerCase();
            for (String w : BANNED_WORDS) {
                if (txt.contains(w)) {
                    ok = false;
                    errors.add("Lettre de motivation : langage inapproprié détecté");
                    markInvalid(taMotivationLetter, "Veuillez retirer les mots inappropriés");
                    break;
                }
            }
        }
        ok &= validateExperiences(errors);
        ok &= validateCertifications(errors);

        return ok;
    }

    private boolean validateRequiredText(TextInputControl c, String label, List<String> errors) {
        if (c == null) return true;
        if (isBlank(c.getText())) {
            errors.add(label + " : obligatoire");
            markInvalid(c, label + " obligatoire");
            return false;
        }
        return true;
    }

    private boolean validateRequiredText(Control c, String label, List<String> errors) {
        if (c == null) return true;
        if (c instanceof TextInputControl tic) return validateRequiredText(tic, label, errors);
        return true;
    }

    private boolean validateExperiences(List<String> errors) {
        if (experienceContainer == null) return true;
        boolean anyFilled = false;
        boolean ok = true;
        LocalDate today = LocalDate.now();

        for (Node n : experienceContainer.getChildren()) {
            if (!(n instanceof VBox card)) continue;
            Object ud = card.getUserData();
            if (!(ud instanceof Object[] arr) || arr.length < 4) continue;

            TextField tfPoste = (TextField) arr[0];
            TextField tfEnt = (TextField) arr[1];
            DatePicker dpFrom = (DatePicker) arr[2];
            DatePicker dpTo = (DatePicker) arr[3];

            String poste = tfPoste == null ? "" : tfPoste.getText();
            String ent = tfEnt == null ? "" : tfEnt.getText();
            LocalDate from = dpFrom == null ? null : dpFrom.getValue();
            LocalDate to = dpTo == null ? null : dpTo.getValue();

            boolean blockFilled = !isBlank(poste) || !isBlank(ent) || from != null || to != null;
            if (!blockFilled) continue;
            anyFilled = true;

            if (isBlank(poste)) {
                ok = false;
                errors.add("Expérience : poste obligatoire");
                markInvalid(tfPoste, "Poste obligatoire");
            } else if (!LETTERS_PATTERN.matcher(poste.trim()).matches()) {
                ok = false;
                errors.add("Expérience : poste uniquement des caractères");
                markInvalid(tfPoste, "Uniquement des caractères");
            }

            if (isBlank(ent)) {
                ok = false;
                errors.add("Expérience : entreprise obligatoire");
                markInvalid(tfEnt, "Entreprise obligatoire");
            } else if (!LETTERS_PATTERN.matcher(ent.trim()).matches()) {
                ok = false;
                errors.add("Expérience : entreprise uniquement des caractères");
                markInvalid(tfEnt, "Uniquement des caractères");
            }

            if (from == null) {
                ok = false;
                errors.add("Expérience : date début obligatoire");
                markInvalid(dpFrom, "Date début obligatoire");
            } else if (from.isAfter(today)) {
                ok = false;
                errors.add("Expérience : date début ne peut pas dépasser aujourd'hui");
                markInvalid(dpFrom, "Date future interdite");
            }

            if (to != null && to.isAfter(today)) {
                ok = false;
                errors.add("Expérience : date fin ne peut pas dépasser aujourd'hui");
                markInvalid(dpTo, "Date future interdite");
            }

            if (from != null && to != null && from.isAfter(to)) {
                ok = false;
                errors.add("Expérience : date début ne doit pas être après date fin");
                markInvalid(dpFrom, "Début <= Fin");
                markInvalid(dpTo, "Fin >= Début");
            }
        }

        return !anyFilled || ok;
    }

    private boolean validateCertifications(List<String> errors) {
        if (certificationContainer == null) return true;
        boolean anyFilled = false;
        boolean ok = true;
        LocalDate today = LocalDate.now();

        for (Node n : certificationContainer.getChildren()) {
            if (!(n instanceof VBox card)) continue;
            Object ud = card.getUserData();
            if (!(ud instanceof Object[] arr) || arr.length < 5) continue;

            TextField tfOrg = (TextField) arr[0];
            TextField tfCertif = (TextField) arr[1];
            DatePicker dpIssued = (DatePicker) arr[3];
            DatePicker dpExpire = (DatePicker) arr[4];
            CheckBox cbNoExpire = (arr.length > 5 && arr[5] instanceof CheckBox) ? (CheckBox) arr[5] : null;

            java.sql.Date obt = (dpIssued.getValue() != null) ? java.sql.Date.valueOf(dpIssued.getValue()) : null;
            LocalDate expire = (dpExpire == null) ? null : dpExpire.getValue();

            if ((tfOrg.getText() == null || tfOrg.getText().isBlank()) &&
                    (tfCertif.getText() == null || tfCertif.getText().isBlank())) continue;

            anyFilled = true;

            if (obt == null) {
                ok = false;
                errors.add("Certification : date d'obtention obligatoire");
                markInvalid(dpIssued, "Date obligatoire");
            } else if (obt.toLocalDate().isAfter(today)) {
                ok = false;
                errors.add("Certification : date d'obtention ne peut pas dépasser aujourd'hui");
                markInvalid(dpIssued, "Date future interdite");
            }

            if (cbNoExpire != null && cbNoExpire.isSelected()) {
                clearInvalid(dpExpire);
            } else if (expire != null) {
                if (obt != null && obt.toLocalDate().isAfter(expire)) {
                    ok = false;
                    errors.add("Certification : obtention ne doit pas être après expiration");
                    markInvalid(dpIssued, "Obtention <= Expiration");
                    markInvalid(dpExpire, "Expiration >= Obtention");
                }
            }
        }

        return !anyFilled || ok;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void showValidationSummary(List<String> errors) {
        String msg = String.join("\n", errors);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText("Veuillez corriger les champs invalides");
        alert.setContentText(msg);
        alert.show();
    }

    private void setupLiveValidation() {
        if (tfPrenom != null) {
            tfPrenom.textProperty().addListener((obs, o, n) -> validateLettersLive(tfPrenom, "Uniquement des caractères"));
        }
        if (tfNom != null) {
            tfNom.textProperty().addListener((obs, o, n) -> validateLettersLive(tfNom, "Uniquement des caractères"));
        }
        if (tfVille != null) {
            tfVille.textProperty().addListener((obs, o, n) -> validateLettersLive(tfVille, "Uniquement des caractères"));
        }
        if (tfEmail != null) {
            tfEmail.textProperty().addListener((obs, o, n) -> {
                if (isBlank(n)) {
                    clearInvalid(tfEmail);
                    return;
                }
                if (!EMAIL_PATTERN.matcher(n.trim()).matches()) {
                    markInvalid(tfEmail, "Format email invalide");
                } else {
                    clearInvalid(tfEmail);
                }
            });
        }

        if (tfAdresse != null) {
            tfAdresse.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                if (newText == null) return change;
                return ADDRESS_PATTERN.matcher(newText).matches() ? change : null;
            }));
            tfAdresse.textProperty().addListener((obs, o, n) -> {
                if (isBlank(n)) {
                    clearInvalid(tfAdresse);
                    return;
                }
                if (!ADDRESS_PATTERN.matcher(n.trim()).matches()) {
                    markInvalid(tfAdresse, "Uniquement lettres/chiffres/espaces");
                } else {
                    clearInvalid(tfAdresse);
                }
            });
        }

        if (tfSkills != null) {
            tfSkills.textProperty().addListener((obs, o, n) -> {
                if (isBlank(n)) {
                    clearInvalid(tfSkills);
                    return;
                }
                String skills = n.trim();
                boolean ok = skills.chars().allMatch(ch -> Character.isLetter(ch) || Character.isWhitespace(ch) || ch == ',' || ch == ';' || ch == '-' || ch == '_');
                if (!ok) {
                    markInvalid(tfSkills, "Uniquement lettres + séparateurs (, ; - _)");
                } else {
                    clearInvalid(tfSkills);
                }
            });
        }
       // j ai fait un commentaire ici pour l agent ia
        if (taMotivationLetter != null) {
            /* taMotivationLetter.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                if (newText == null) return change;
                return newText.matches("[\\p{L}\\s]*") ? change : null;
            })); */
            taMotivationLetter.textProperty().addListener((obs, o, n) -> {
                if (isBlank(n)) {
                    clearInvalid(taMotivationLetter);
                    return;
                }

                // APRÈS
                if (n.matches(".*[0-9+=*/_<>^§#~°²%$£µ;@!|{}\\[\\]\\\\].*")) {
                    markInvalid(taMotivationLetter, "Chiffres et caractères spéciaux interdits");
                    return;
                }

                String low = n.toLowerCase();
                boolean banned = false;
                for (String w : BANNED_WORDS) {
                    if (low.contains(w)) {
                        banned = true;
                        break;
                    }
                }
                if (banned) {
                    markInvalid(taMotivationLetter, "Langage inapproprié");
                } else {
                    clearInvalid(taMotivationLetter);
                }
            });
        }

        if (dpNaissance != null) {
            dpNaissance.valueProperty().addListener((obs, o, n) -> {
                if (n == null) {
                    clearInvalid(dpNaissance);
                    return;
                }
                LocalDate today = LocalDate.now();
                if (n.isAfter(today)) {
                    markInvalid(dpNaissance, "Date future interdite");
                    return;
                }
                int age = Period.between(n, today).getYears();
                if (age < 18) {
                    markInvalid(dpNaissance, "Âge minimum: 18 ans");
                } else {
                    clearInvalid(dpNaissance);
                }
            });
        }

        if (cbInstitution != null) {
            cbInstitution.valueProperty().addListener((obs, o, n) -> {
                if (n == null || n.isBlank()) clearInvalid(cbInstitution);
                else clearInvalid(cbInstitution);
            });
        }

        if (tgDegree != null) {
            tgDegree.selectedToggleProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    if (rbBachelor != null) clearInvalid(rbBachelor);
                    if (rbEngineering != null) clearInvalid(rbEngineering);
                    if (rbMasters != null) clearInvalid(rbMasters);
                    if (rbPhd != null) clearInvalid(rbPhd);
                }
            });
        }
    }

    private void validateLettersLive(TextField tf, String message) {
        if (tf == null) return;
        String v = tf.getText();
        if (isBlank(v)) {
            clearInvalid(tf);
            return;
        }
        if (!LETTERS_PATTERN.matcher(v.trim()).matches()) {
            markInvalid(tf, message);
        } else {
            clearInvalid(tf);
        }
    }

    private void attachExperienceLive(TextField poste, TextField ent, DatePicker from, DatePicker to) {
        if (poste != null) poste.textProperty().addListener((obs, o, n) -> validateLettersLive(poste, "Uniquement des caractères"));
        if (ent != null) ent.textProperty().addListener((obs, o, n) -> validateLettersLive(ent, "Uniquement des caractères"));
        if (from != null) from.valueProperty().addListener((obs, o, n) -> validateExperienceDatesLive(from, to));
        if (to != null) to.valueProperty().addListener((obs, o, n) -> validateExperienceDatesLive(from, to));
    }

    private void validateExperienceDatesLive(DatePicker from, DatePicker to) {
        LocalDate today = LocalDate.now();
        LocalDate f = from == null ? null : from.getValue();
        LocalDate t = to == null ? null : to.getValue();

        if (from != null) {
            if (f != null && f.isAfter(today)) markInvalid(from, "Date future interdite");
            else clearInvalid(from);
        }
        if (to != null) {
            if (t != null && t.isAfter(today)) markInvalid(to, "Date future interdite");
            else clearInvalid(to);
        }
        if (from != null && to != null && f != null && t != null && f.isAfter(t)) {
            markInvalid(from, "Début <= Fin");
            markInvalid(to, "Fin >= Début");
        }
    }

    private void attachCertificationLive(TextField org, TextField cert, DatePicker issued, DatePicker expire) {
        if (org != null) org.textProperty().addListener((obs, o, n) -> validateLettersLive(org, "Uniquement des caractères"));
        if (cert != null) cert.textProperty().addListener((obs, o, n) -> validateLettersLive(cert, "Uniquement des caractères"));
        if (issued != null) issued.valueProperty().addListener((obs, o, n) -> validateCertificationDatesLive(issued, expire));
        if (expire != null) expire.valueProperty().addListener((obs, o, n) -> validateCertificationDatesLive(issued, expire));
    }

    private void validateCertificationDatesLive(DatePicker issued, DatePicker expire) {
        LocalDate today = LocalDate.now();
        LocalDate i = issued == null ? null : issued.getValue();
        LocalDate e = expire == null ? null : expire.getValue();

        if (issued != null) {
            if (i != null && i.isAfter(today)) markInvalid(issued, "Date future interdite");
            else clearInvalid(issued);
        }
        if (expire != null) {
            clearInvalid(expire);
        }
        if (issued != null && expire != null && i != null && e != null && i.isAfter(e)) {
            markInvalid(issued, "Obtention <= Expiration");
            markInvalid(expire, "Expiration >= Obtention");
        }
    }

    private void ouvrirMesCandidaturesDansDashboard() {
        try {
            Parent dashboardRoot = stepperPane.getScene().getRoot();
            Pane pageContainer = (Pane) dashboardRoot.lookup("#pageContainer");
            Pane contentArea = (Pane) dashboardRoot.lookup("#contentArea");

            var url = getClass().getResource("/candidaturefxml/MesCandidatures.fxml");
            if (url == null) {
                throw new RuntimeException("MesCandidatures.fxml not found in resources root.");
            }

            Parent view = FXMLLoader.load(url);
            if (pageContainer != null) {
                pageContainer.getChildren().setAll(view);
            } else if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            } else {
                throw new RuntimeException("Neither #pageContainer nor #contentArea found.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startRecording() {
        try {
            recordedVideoFile = new File("motivation_" + System.currentTimeMillis() + ".mp4");

            List<String> cmd = List.of(
                    FFMPEG,
                    "-y",
                    "-f", "dshow",
                    "-i", "video=" + VIDEO_DEVICE + ":audio=" + AUDIO_DEVICE,
                    "-vcodec", "libx264",
                    "-preset", "veryfast",
                    "-pix_fmt", "yuv420p",
                    "-acodec", "aac",
                    "-b:a", "128k",
                    recordedVideoFile.getAbsolutePath()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            ffmpegProcess = pb.start();
            new Thread(() -> {
                try (var br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(ffmpegProcess.getInputStream()))) {
                    while (br.readLine() != null) {
                    }
                } catch (Exception ignored) {
                }
            }).start();

            if (btnRecordVideo != null) btnRecordVideo.setDisable(true);
            if (btnStopVideo != null) btnStopVideo.setDisable(false);
            if (btnUploadVideo != null) btnUploadVideo.setDisable(true);

            if (lblVideoStatus != null) lblVideoStatus.setText("Recording...");

        } catch (Exception e) {
            e.printStackTrace();
            if (lblVideoStatus != null) lblVideoStatus.setText("Error starting recording");
        }
    }

    @FXML
    private void stopRecording() {
        if (ffmpegProcess == null) return;

        if (lblVideoStatus != null) lblVideoStatus.setText("Arrêt en cours... sauvegarde...");
        if (btnStopVideo != null) btnStopVideo.setDisable(true);

        new Thread(() -> {
            try {
                var os = ffmpegProcess.getOutputStream();
                os.write("q\n".getBytes());
                os.flush();
                os.close();

                ffmpegProcess.waitFor();
                ffmpegProcess = null;

                Platform.runLater(() -> {
                    if (btnRecordVideo != null) btnRecordVideo.setDisable(false);
                    if (btnUploadVideo != null) btnUploadVideo.setDisable(false);
                    if (lblVideoStatus != null) {
                        lblVideoStatus.setText("Vidéo sauvegardée ✅ : " + recordedVideoFile.getAbsolutePath());
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();

                try {
                    if (ffmpegProcess != null) ffmpegProcess.destroy();
                } catch (Exception ignored) {
                }

                Platform.runLater(() -> {
                    if (lblVideoStatus != null) lblVideoStatus.setText("Erreur arrêt ❌");
                    if (btnStopVideo != null) btnStopVideo.setDisable(false);
                });
            }
        }).start();
    }

    @FXML
    private void chooseVideo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select a video");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video files", "*.mp4", "*.mov", "*.mkv", "*.webm", "*.avi")
        );

        Stage stage = (Stage) btnUploadVideo.getScene().getWindow();
        File f = fc.showOpenDialog(stage);
        if (f == null) return;

        selectedVideoFile = f;
        if (lblVideoStatus != null) lblVideoStatus.setText("Selected: " + f.getName());
    }

    @FXML
    private void initialize() {

        loadRunnerFrames();
        if (runner != null && !runnerFrames.isEmpty()) {
            idleFrameIndex = 0;
            runner.setImage(runnerFrames.get(idleFrameIndex));
            runner.setTranslateX(0);
        }

        if (tfTelLocal != null) {
            tfTelLocal.setTextFormatter(new TextFormatter<String>(change -> {
                String newText = change.getControlNewText();
                if (newText == null) return change;
                return newText.matches("\\d*") ? change : null;
            }));
        }

        if (stepperPane != null) {
            stepperPane.widthProperty().addListener((obs, o, n) -> layoutStepper());
        }

        layoutStepper();
        updateProgressInstant();
        updateStepperStyles(currentStep);
        showStep(currentStep);
        updateButtons();
        updatePercentLabel();

        if (experienceContainer != null && experienceContainer.getChildren().isEmpty()) addExperience();
        if (certificationContainer != null && certificationContainer.getChildren().isEmpty()) addCertification();

        if (cbIndicatif != null) {
            cbIndicatif.getItems().setAll(
                    "+216 (Tunisie)",
                    "+33 (France)",
                    "+49 (Allemagne)",
                    "+39 (Italie)",
                    "+1 (USA/Canada)"
            );
            cbIndicatif.getSelectionModel().selectFirst();
        }

        if (lblSelectedFile != null && (lblSelectedFile.getText() == null || lblSelectedFile.getText().isBlank())) {
            lblSelectedFile.setText("Aucun fichier sélectionné");
        }
        if (runner != null) {
            runner.setManaged(false);
            runner.setMouseTransparent(true);
        }

        tgDegree = new ToggleGroup();

        if (rbBachelor != null) rbBachelor.setToggleGroup(tgDegree);
        if (rbEngineering != null) rbEngineering.setToggleGroup(tgDegree);
        if (rbMasters != null) rbMasters.setToggleGroup(tgDegree);
        if (rbPhd != null) rbPhd.setToggleGroup(tgDegree);

        if (rbBachelor != null) {
            rbBachelor.setText("");
            rbBachelor.setUserData("Bachelor");
        }
        if (rbEngineering != null) {
            rbEngineering.setText("");
            rbEngineering.setUserData("Engineering");
        }
        if (rbMasters != null) {
            rbMasters.setText("");
            rbMasters.setUserData("Masters");
        }
        if (rbPhd != null) {
            rbPhd.setText("");
            rbPhd.setUserData("Ph.D.");
        }

        if (cbInstitution != null) {
            cbInstitution.getItems().setAll(
                    "ENIT",
                    "ESPRIT",
                    "INSAT",
                    "FST",
                    "ENSI",
                    "ISET",
                    "Autre"
            );
        }

        setupLiveValidation();

        // ✅ Fix DatePicker format jdiddaaaa
        if (dpNaissance != null) {
            dpNaissance.setConverter(new javafx.util.converter.LocalDateStringConverter(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
            ));
        }
    }

    private void layoutStepper() {
        if (runner == null || stepperPane == null) return;
        if (isAnimating) return;

        runner.setTranslateX(0);
        runner.setLayoutX(stepX(currentStep) - 20);
        updateProgressInstant();
    }

    private double stepX(int step) {
        double left = 22;
        double right = 22;

        double w = stepperPane.getWidth();
        if (w <= 0) return left;

        double trackW = w - left - right;
        return left + (trackW * step / 4.0);
    }

    private void updateProgressInstant() {
        if (progressLine == null) return;
        progressLine.setPrefWidth(stepX(currentStep) - 22);
    }

    @FXML
    private void next() {
        if (isAnimating && currentStep < 4) return;
        if (currentStep < 4) {
            animateMoveToStep(currentStep + 1);
        } else {
            if (cbConfirmInfo != null && cbAcceptPrivacy != null) {
                if (!cbConfirmInfo.isSelected() || !cbAcceptPrivacy.isSelected()) {
                    showAlert("Validation requise", "Veuillez confirmer les informations et accepter la politique.");
                    return;
                }
            }

            try {
                submit(new ActionEvent());
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible de mettre à jour la candidature. Vérifiez la console pour plus de détails.");
            }
        }

    }

    @FXML
    private void back() {
        if (isAnimating) return;
        if (currentStep > 0) {
            animateMoveToStep(currentStep - 1);
        }
    }

    private void animateMoveToStep(int targetStep) {
        if (runner == null || progressLine == null || stepperPane == null) return;

        hardStopRunnerAnimationsOnly();

        isAnimating = true;
        if (btnNext != null) btnNext.setDisable(true);
        if (btnBack != null) btnBack.setDisable(true);

        int direction = (targetStep > currentStep) ? 1 : -1;
        startWalkFramesSlow(direction);

        double finalLayoutX = stepX(targetStep) - 20;

        double fromLayoutX = runner.getLayoutX();
        double delta = finalLayoutX - fromLayoutX;

        runner.setTranslateX(0);

        TranslateTransition move = new TranslateTransition(Duration.millis(900), runner);
        move.setByX(delta);
        move.setInterpolator(Interpolator.EASE_BOTH);

        Timeline progressAnim = new Timeline(
                new KeyFrame(Duration.millis(900),
                        new KeyValue(progressLine.prefWidthProperty(), stepX(targetStep) - 22, Interpolator.EASE_BOTH)
                )
        );

        runnerAll = new ParallelTransition(move, progressAnim);

        runnerAll.setOnFinished(e -> {
            stopWalkFrames();

            runner.setLayoutX(finalLayoutX);
            runner.setTranslateX(0);

            if (!runnerFrames.isEmpty()) {
                idleFrameIndex = 0;
                runner.setImage(runnerFrames.get(idleFrameIndex));
            }

            currentStep = targetStep;

            updateStepperStyles(currentStep);
            showStep(currentStep);
            updateButtons();
            updatePercentLabel();

            isAnimating = false;
            if (btnNext != null) btnNext.setDisable(false);
            if (btnBack != null) btnBack.setDisable(false);

            Platform.runLater(this::scrollToTop);
        });

        runnerAll.play();
    }

    private void hardStopRunnerAnimationsOnly() {
        stopWalkFrames();

        if (runnerAll != null) {
            runnerAll.stop();
            runnerAll = null;
        }

        if (runner != null) {
            runner.setTranslateX(0);
            runner.setLayoutX(stepX(currentStep) - 20);
        }
    }

    private void scrollToTop() {
        if (stepperPane == null) return;

        Parent p = stepperPane.getParent();
        while (p != null && !(p instanceof ScrollPane)) {
            p = p.getParent();
        }
        if (p instanceof ScrollPane sp) {
            sp.setVvalue(0);
        }
    }

    private void startWalkFramesSlow(int direction) {
        stopWalkFrames();
        if (runnerFrames.isEmpty() || runner == null) return;

        walkFramesTimeline = new Timeline();
        walkFramesTimeline.setCycleCount(Animation.INDEFINITE);

        walkFramesTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(140), e -> {
                    idleFrameIndex += direction;

                    if (idleFrameIndex >= runnerFrames.size()) idleFrameIndex = 0;
                    if (idleFrameIndex < 0) idleFrameIndex = runnerFrames.size() - 1;

                    runner.setImage(runnerFrames.get(idleFrameIndex));
                })
        );

        walkFramesTimeline.play();
    }

    private void stopWalkFrames() {
        if (walkFramesTimeline != null) {
            walkFramesTimeline.stop();
            walkFramesTimeline = null;
        }
    }

    private void updateStepperStyles(int step) {
        VBox[] items = {stepItem0, stepItem1, stepItem2, stepItem3, stepItem4};
        for (int i = 0; i < items.length; i++) {
            if (items[i] == null) continue;
            items[i].getStyleClass().remove("active");
            if (i == step) items[i].getStyleClass().add("active");
        }
    }

    private void showStep(int index) {
        stopWalkFrames();

        if (runner != null && !runnerFrames.isEmpty()) {
            runner.setImage(runnerFrames.get(Math.max(0, Math.min(idleFrameIndex, runnerFrames.size() - 1))));
            runner.setTranslateX(0);
            runner.setLayoutX(stepX(currentStep) - 20);
        }

        VBox[] all = {step0, step1, step2, step3, step4};
        for (int i = 0; i < all.length; i++) {
            if (all[i] == null) continue;
            boolean show = (i == index);
            all[i].setVisible(show);
            all[i].setManaged(show);
        }

        if (index == 4 && reviewLabel != null) {
            reviewLabel.setText(buildReviewText());
        }
        updateFormHeader(index);

    }

    private void updateButtons() {
        if (btnBack != null) btnBack.setDisable(currentStep == 0);
        if (btnNext != null) {
            if (currentStep == 4) {
                btnNext.setText(editMode ? "Mettre à jour" : "Soumettre");
            } else {
                btnNext.setText("Suivant");
            }
        }
    }

    private void updatePercentLabel() {
        if (progressPercentLabel == null) return;
        int percent = (int) Math.round((currentStep / 4.0) * 100.0);
        progressPercentLabel.setText(percent + "% complété");
    }

    private String getTelephoneComplet() {
        String ind = (cbIndicatif == null || cbIndicatif.getValue() == null) ? "" : cbIndicatif.getValue();
        ind = ind.isBlank() ? "" : ind.split(" ")[0];
        String num = (tfTelLocal == null || tfTelLocal.getText() == null) ? "" : tfTelLocal.getText().trim();
        return (ind + " " + num).trim();
    }

    private String buildReviewText() {
        return "Prénom: " + txt(tfPrenom) + "\n"
                + "Nom: " + txt(tfNom) + "\n"
                + "Email: " + txt(tfEmail) + "\n"
                + "Téléphone: " + getTelephoneComplet() + "\n"
                + "Adresse: " + txt(tfAdresse) + "\n"
                + "Ville: " + txt(tfVille) + "\n"
                + "Date naissance: " + (dpNaissance == null || dpNaissance.getValue() == null ? "" : dpNaissance.getValue()) + "\n"
                + "Compétences: " + (tfSkills == null ? "" : tfSkills.getText()) + "\n"
                + "Fichier: " + (selectedAttachment == null ? "Aucun" : selectedAttachment.getName());
    }

    private String txt(TextField tf) {
        return (tf == null || tf.getText() == null) ? "" : tf.getText();
    }

    @FXML
    private void addExperience() {
        if (experienceContainer == null) return;
        experienceContainer.getChildren().add(buildExperienceBlock());
    }

    private Node buildExperienceBlock() {
        VBox card = new VBox(12);
        card.getStyleClass().add("exp-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(10);
        Label title = new Label("Expérience");
        title.getStyleClass().add("exp-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button remove = new Button("—");
        remove.getStyleClass().add("exp-remove");
        remove.setOnAction(e -> experienceContainer.getChildren().remove(card));

        header.getChildren().addAll(title, spacer, remove);

        GridPane gp = new GridPane();
        gp.setHgap(16);
        gp.setVgap(12);
        gp.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(c1, c2);

        TextField tfPosteExp = new TextField();
        tfPosteExp.setPromptText("Titre du poste");
        tfPosteExp.getStyleClass().add("field-input");
        tfPosteExp.setMaxWidth(Double.MAX_VALUE);

        TextField tfEntreprise = new TextField();
        tfEntreprise.setPromptText("Nom de l'entreprise");
        tfEntreprise.getStyleClass().add("field-input");
        tfEntreprise.setMaxWidth(Double.MAX_VALUE);

        DatePicker dpFrom = new DatePicker();
        dpFrom.getStyleClass().add("field-input");
        dpFrom.setMaxWidth(Double.MAX_VALUE);

        DatePicker dpTo = new DatePicker();
        dpTo.getStyleClass().add("field-input");
        dpTo.setMaxWidth(Double.MAX_VALUE);

        gp.add(fieldBox("Poste *", tfPosteExp), 0, 0);
        gp.add(fieldBox("Entreprise *", tfEntreprise), 1, 0);
        gp.add(fieldBox("De", dpFrom), 0, 1);
        gp.add(fieldBox("À", dpTo), 1, 1);

        card.getChildren().addAll(header, gp);
        card.setUserData(new Object[]{tfPosteExp, tfEntreprise, dpFrom, dpTo});

        attachExperienceLive(tfPosteExp, tfEntreprise, dpFrom, dpTo);

        return card;
    }

    @FXML
    private void addCertification() {
        if (certificationContainer == null) return;
        certificationContainer.getChildren().add(buildCertificationBlock());
    }

    private Node buildCertificationBlock() {
        VBox card = new VBox(12);
        card.getStyleClass().add("exp-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(10);
        Label title = new Label("Certification");
        title.getStyleClass().add("exp-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button remove = new Button("—");
        remove.getStyleClass().add("exp-remove");
        remove.setOnAction(e -> certificationContainer.getChildren().remove(card));

        header.getChildren().addAll(title, spacer, remove);

        GridPane gp = new GridPane();
        gp.setHgap(16);
        gp.setVgap(12);
        gp.setMaxWidth(Double.MAX_VALUE);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        gp.getColumnConstraints().addAll(c1, c2);

        TextField tfIssuer = new TextField();
        tfIssuer.setPromptText("Organisme (ex : Google, Microsoft, Coursera...)");
        tfIssuer.getStyleClass().add("field-input");
        tfIssuer.setMaxWidth(Double.MAX_VALUE);

        TextField tfCertif = new TextField();
        tfCertif.setPromptText("Nom de la certification");
        tfCertif.getStyleClass().add("field-input");
        tfCertif.setMaxWidth(Double.MAX_VALUE);

        TextField tfNumber = new TextField();
        tfNumber.setPromptText("Numéro (optionnel)");
        tfNumber.getStyleClass().add("field-input");
        tfNumber.setMaxWidth(Double.MAX_VALUE);
        tfNumber.setTextFormatter(new TextFormatter<String>(change -> {
            String newText = change.getControlNewText();
            if (newText == null) return change;
            return newText.matches("\\d*") ? change : null;
        }));

        DatePicker dpIssued = new DatePicker();
        dpIssued.getStyleClass().add("field-input");
        dpIssued.setMaxWidth(Double.MAX_VALUE);

        DatePicker dpExpire = new DatePicker();
        dpExpire.getStyleClass().add("field-input");
        dpExpire.setMaxWidth(Double.MAX_VALUE);

        CheckBox cbNoExpire = new CheckBox("Pas de date d'expiration");
        cbNoExpire.selectedProperty().addListener((obs, oldV, newV) -> {
            dpExpire.setDisable(newV);
            if (newV) {
                dpExpire.setValue(null);
                clearInvalid(dpExpire);
            }
        });

        gp.add(fieldBox("Organisme", tfIssuer), 0, 0);
        gp.add(fieldBox("Certification", tfCertif), 1, 0);
        gp.add(fieldBox("Numéro", tfNumber), 0, 1);
        gp.add(fieldBox("Date d'obtention", dpIssued), 1, 1);
        gp.add(fieldBox("Date d'expiration", dpExpire), 0, 2);
        gp.add(cbNoExpire, 1, 2);

        card.getChildren().addAll(header, gp);
        card.setUserData(new Object[]{tfIssuer, tfCertif, tfNumber, dpIssued, dpExpire, cbNoExpire});

        attachCertificationLive(tfIssuer, tfCertif, dpIssued, dpExpire);

        return card;
    }

    private VBox fieldBox(String label, Control field) {
        VBox box = new VBox(6);
        Label l = new Label(label);
        l.getStyleClass().add("field-caption");
        box.getChildren().addAll(l, field);
        return box;
    }

    @FXML
    private void chooseAttachment() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner un fichier (DOC, DOCX, HTML, PDF, TXT)");

        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.pdf", "*.txt", "*.html", "*.htm")
        );

        Stage stage = null;
        if (btnSelectFile != null && btnSelectFile.getScene() != null) {
            stage = (Stage) btnSelectFile.getScene().getWindow();
        } else if (stepperPane != null && stepperPane.getScene() != null) {
            stage = (Stage) stepperPane.getScene().getWindow();
        }

        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        long maxBytes = 1_000_000;
        if (file.length() > maxBytes) {
            showAlert("Fichier trop grand", "Le fichier sélectionné dépasse 1MB.");
            return;
        }

        String name = file.getName().toLowerCase();
        boolean allowed = name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".pdf")
                || name.endsWith(".txt") || name.endsWith(".html") || name.endsWith(".htm");

        if (!allowed) {
            showAlert("Format non supporté", "Formats acceptés : DOC, DOCX, PDF, TXT, HTML.");
            return;
        }

        selectedAttachment = file;
        if (lblSelectedFile != null) lblSelectedFile.setText("Fichier sélectionné : " + file.getName());
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadRunnerFrames() {
        runnerFrames.clear();
        for (int i = 1; i <= 8; i++) {
            String path = "/Images/run" + i + ".png";
            var is = getClass().getResourceAsStream(path);
            if (is == null) throw new RuntimeException("Image introuvable: " + path);
            runnerFrames.add(new Image(is));
        }
    }

    private void updateFormHeader(int step) {
        if (formTitle == null || formSubtitle == null) return;

        switch (step) {
            case 0 -> {
                formTitle.setText("Informations personnelles");
                formSubtitle.setText("Renseignez vos coordonnées");
            }
            case 1 -> {
                formTitle.setText("Expérience");
                formSubtitle.setText("Ajoutez vos expériences et certifications");
            }
            case 2 -> {
                formTitle.setText("Formation");
                formSubtitle.setText("Parlez de votre parcours académique");
            }
            case 3 -> {
                formTitle.setText("Motivation");
                formSubtitle.setText("Confirmez votre candidature");
            }
            case 4 -> {
                formTitle.setText("Finalisation");
                formSubtitle.setText("Confirmez et envoyez votre candidature");
            }
        }
    }

    private String getSelectedDegree() {
        if (tgDegree == null) return null;
        Toggle t = tgDegree.getSelectedToggle();
        if (t == null) return null;

        RadioButton rb = (RadioButton) t;
        Object ud = rb.getUserData();
        if (ud instanceof String s) return s;
        return null;
    }



    //generation de la lettre de motivation a l aide d un agent IA
    @FXML
    private void genererLettreIA() {
        // 1. Récupérer les données des étapes précédentes
        String prenom    = tfPrenom  != null ? tfPrenom.getText()  : "";
        String nom       = tfNom     != null ? tfNom.getText()     : "";
        String ville     = tfVille   != null ? tfVille.getText()   : "";
        String degree    = getSelectedDegree() != null ? getSelectedDegree() : "";
        String instit    = cbInstitution != null && cbInstitution.getValue() != null
                ? cbInstitution.getValue() : "";
        String skills    = tfSkills  != null ? tfSkills.getText()  : "";

        // 2. Récupérer les expériences depuis les blocs dynamiques
        StringBuilder expBuilder = new StringBuilder();
        if (experienceContainer != null) {
            for (Node n : experienceContainer.getChildren()) {
                if (!(n instanceof VBox card)) continue;
                Object ud = card.getUserData();
                if (!(ud instanceof Object[] arr) || arr.length < 2) continue;
                String poste = ((TextField) arr[0]).getText();
                String ent   = ((TextField) arr[1]).getText();
                if (!poste.isBlank() || !ent.isBlank()) {
                    expBuilder.append("- ").append(poste).append(" chez ").append(ent).append("\n");
                }
            }
        }

        // 3. Récupérer les certifications
        StringBuilder certBuilder = new StringBuilder();
        if (certificationContainer != null) {
            for (Node n : certificationContainer.getChildren()) {
                if (!(n instanceof VBox card)) continue;
                Object ud = card.getUserData();
                if (!(ud instanceof Object[] arr) || arr.length < 2) continue;
                String org   = ((TextField) arr[0]).getText();
                String certif = ((TextField) arr[1]).getText();
                if (!org.isBlank() || !certif.isBlank()) {
                    certBuilder.append("- ").append(certif).append(" (").append(org).append(")\n");
                }
            }
        }

        String nomOffre;
        if (offreTitre != null && !offreTitre.isBlank()) {
            nomOffre = offreTitre;
        } else if (offreId != null) {
            nomOffre = "l'offre sélectionnée";
        } else {
            nomOffre = "";
        }

        String prompt = """
                IMPORTANT: Ecris uniquement en francais sans aucun caractere accentue.
                Utilise e au lieu de e accent, a au lieu de a accent, u au lieu de u accent, etc.

                Tu es un expert en recrutement. Redige une lettre de motivation professionnelle en francais
                pour le candidat suivant qui postule au poste de : %s.

                Informations du candidat :
                - Nom complet : %s %s
                - Ville : %s
                - Diplome le plus eleve : %s
                - Institution : %s
                - Competences : %s

                Experiences professionnelles :
                %s

                Certifications :
                %s

                La lettre doit etre formelle, convaincante, structuree en 3 paragraphes (introduction,
                motivations & competences, conclusion), et faire environ 250 mots.
                Reponds UNIQUEMENT avec la lettre, sans titre ni commentaire.
                """.formatted(
                nomOffre, prenom, nom, ville, degree, instit, skills,
                expBuilder.length() > 0 ? expBuilder : "Aucune expérience renseignée",
                certBuilder.length() > 0 ? certBuilder : "Aucune certification renseignée"
        );

        // 6. Désactiver le bouton + afficher indicateur
        if (btnGenerateAI != null) {
            btnGenerateAI.setDisable(true);
            btnGenerateAI.setText("⏳ Génération...");
        }
        if (taMotivationLetter != null) {
            taMotivationLetter.setText("Génération en cours...");
        }

        // 7. Appel OpenAI en arrière-plan (Thread pour ne pas bloquer le UI)
        new Thread(() -> {
            try {
                OpenAILettreMotivationService aiService = new OpenAILettreMotivationService();
                String lettre = aiService.genererLettreMotivation(prompt);

                Platform.runLater(() -> {
                    if (taMotivationLetter != null) {
                        taMotivationLetter.setText(lettre);
                        // Désactiver la validation stricte (lettres uniquement)
                        // car la lettre générée peut contenir des virgules, points, etc.
                    }
                    if (btnGenerateAI != null) {
                        btnGenerateAI.setDisable(false);
                        btnGenerateAI.setText("✨ Générer avec l'IA");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (taMotivationLetter != null) taMotivationLetter.setText("");
                    if (btnGenerateAI != null) {
                        btnGenerateAI.setDisable(false);
                        btnGenerateAI.setText("✨ Générer avec l'IA");
                    }
                    showAlert("Erreur IA", "Impossible de générer la lettre : " + e.getMessage());
                });
            }
        }).start();
    }

    //jdidddddd offree integration
    public void setOffre(OffreEmploi offre) {
        if (offre == null) return;
        this.offreId = offre.getId();
        this.offreTitre = offre.getTitre();

        // Pré-remplir le nom de l'offre pour la lettre IA
        if (heroSubtitle != null) {
            heroSubtitle.setText("Vous postulez pour : " + offre.getTitre());
        }
    }
}
