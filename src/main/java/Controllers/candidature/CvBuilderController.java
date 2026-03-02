package Controllers.candidature;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Transform;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.awt.image.BufferedImage;

public class CvBuilderController {

    @FXML private Label lblTemplate;
    @FXML private HBox templateRoot;

    @FXML private VBox tplSidebar;
    @FXML private VBox tplHeader;
    @FXML private HBox topContactRow;
    @FXML private VBox sectionSkillsBody;
    @FXML private VBox skillsSidebarBlock;

    @FXML private Button btnSecInfo;
    @FXML private Button btnSecExp;
    @FXML private Button btnSecProjects;
    @FXML private Button btnSecEdu;
    @FXML private Button btnSecSkills;
    @FXML private Button btnSecHobbies;
    @FXML private Button btnSecLang;

    @FXML private Label lblFormTitle;
    @FXML private Label lblFormTitleExp;

    @FXML private VBox cardInfo;
    @FXML private VBox cardExp;
    @FXML private VBox cardProjects;
    @FXML private VBox cardEdu;
    @FXML private VBox cardSkills;
    @FXML private VBox cardLang;
    @FXML private VBox cardHobbies;

    @FXML private TextField tfFirstName;
    @FXML private TextField tfLastName;
    @FXML private TextField tfTitle;
    @FXML private TextArea taSummary;
    @FXML private TextField tfPhone;
    @FXML private TextField tfEmail;
    @FXML private TextField tfAddress;

    @FXML private TextField tfDegree;
    @FXML private TextField tfSchool;
    @FXML private TextField tfEduStart;
    @FXML private TextField tfEduEnd;
    @FXML private TextField tfSkillName;
    @FXML private TextField tfSkillPct;
    @FXML private TextArea taLanguages;
    @FXML private TextArea taHobbies;

    @FXML private VBox skillPctBox;

    @FXML private TextField tfProjectName;
    @FXML private TextArea taProjectDesc;

    @FXML private Button btnChoosePhoto;

    @FXML private Label lblPreviewInitials;
    @FXML private ImageView imgPreviewPhoto;
    @FXML private Label lblPreviewName;
    @FXML private Label lblPreviewNameSide;
    @FXML private Label lblPreviewTitle;
    @FXML private Label lblPreviewSummary;
    @FXML private Label lblPreviewPhone;
    @FXML private Label lblPreviewEmail;
    @FXML private Label lblPreviewAddress;

    @FXML private Label lblPreviewJobTitle;
    @FXML private Label lblPreviewCompany;
    @FXML private Label lblPreviewExpDates;
    @FXML private Label lblPreviewExpDesc;

    @FXML private Label lblPreviewDegree;
    @FXML private Label lblPreviewSchool;
    @FXML private Label lblPreviewEduDates;

    @FXML private Label lblPreviewLang1;
    @FXML private Label lblPreviewLang2;

    @FXML private Label lblPreviewSkill1;
    @FXML private Label lblPreviewSkill2;
    @FXML private Label lblPreviewSkill3;

    @FXML private VBox previewExpList;
    @FXML private VBox previewProjectsList;
    @FXML private VBox previewEduList;
    @FXML private VBox previewLangList;
    @FXML private VBox previewSkillsList;
    @FXML private VBox previewSkillsSidebarList;
    @FXML private VBox previewHobbiesList;

    @FXML private Button btnAddExperience;
    @FXML private Button btnAddProject;
    @FXML private Button btnAddEducation;
    @FXML private Button btnAddLanguage;
    @FXML private Button btnAddSkill;
    @FXML private Button btnAddHobbies;
    @FXML private Button btnExportPdf;

    @FXML private TextField tfJobTitle;
    @FXML private TextField tfCompany;
    @FXML private TextField tfStart;
    @FXML private TextField tfEnd;
    @FXML private TextArea taExpDesc;

    private int templateId = 1;

    private VBox editingEduItem;
    private VBox editingExpItem;
    private VBox editingProjectItem;
    private Label editingLangLabel;
    private Label editingSkillLabel;
    private VBox editingSkillItem;

    public void initTemplate(int templateId) {
        this.templateId = templateId;
        if (lblTemplate != null) {
            lblTemplate.setText("Template " + templateId);
        }
        applyTemplateClass();
    }

    @FXML
    private void onExportPdf(ActionEvent e) {
        if (templateRoot == null) return;

        Window owner = null;
        if (templateRoot.getScene() != null) owner = templateRoot.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter CV en PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("cv.pdf");
        File out = fc.showSaveDialog(owner);
        if (out == null) return;
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getParentFile(), out.getName() + ".pdf");
        }

        try {
            SnapshotParameters params = new SnapshotParameters();
            params.setTransform(Transform.scale(3, 3));
            WritableImage fxImg = templateRoot.snapshot(params, null);

            BufferedImage bimg = new BufferedImage((int) fxImg.getWidth(), (int) fxImg.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < fxImg.getHeight(); y++) {
                for (int x = 0; x < fxImg.getWidth(); x++) {
                    bimg.setRGB(x, y, fxImg.getPixelReader().getArgb(x, y));
                }
            }

            try (PDDocument doc = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                doc.addPage(page);

                PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bimg);

                float pageW = page.getMediaBox().getWidth();
                float pageH = page.getMediaBox().getHeight();

                float imgW = pdImage.getWidth();
                float imgH = pdImage.getHeight();

                float scale = Math.min(pageW / imgW, pageH / imgH);
                float drawW = imgW * scale;
                float drawH = imgH * scale;
                float x = (pageW - drawW) / 2f;
                float y = (pageH - drawH) / 2f;

                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(pdImage, x, y, drawW, drawH);
                }

                doc.save(out);
            }

            new Alert(Alert.AlertType.INFORMATION, "PDF exporté: " + out.getAbsolutePath()).show();

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'exporter le PDF.").show();
        }
    }

    @FXML
    private void initialize() {
        setActive(btnSecInfo);
        showSection("info");
        if (lblTemplate != null && (lblTemplate.getText() == null || lblTemplate.getText().isBlank())) {
            lblTemplate.setText("Template " + templateId);
        }

        applyTemplateClass();

        setupPreviewBindings();
    }

    private void applyTemplateClass() {
        if (templateRoot == null) return;

        templateRoot.getStyleClass().removeAll("tpl-1", "tpl-2", "tpl-3");
        templateRoot.getStyleClass().remove("tpl-minimal");
        if (!templateRoot.getStyleClass().contains("tpl")) {
            templateRoot.getStyleClass().add("tpl");
        }
        switch (templateId) {
            case 2 -> templateRoot.getStyleClass().add("tpl-2");
            case 3 -> templateRoot.getStyleClass().add("tpl-3");
            default -> templateRoot.getStyleClass().add("tpl-1");
        }

        boolean minimalCapture = (templateId == 3);
        boolean minimalV2 = (templateId == 2);
        if ((minimalCapture || minimalV2) && !templateRoot.getStyleClass().contains("tpl-minimal")) {
            templateRoot.getStyleClass().add("tpl-minimal");
        }

        if (tplSidebar != null) {
            boolean showSidebar = (templateId != 2);
            tplSidebar.setVisible(showSidebar);
            tplSidebar.setManaged(showSidebar);
        }
        if (topContactRow != null) {
            boolean showTopContact = (templateId == 2);
            topContactRow.setVisible(showTopContact);
            topContactRow.setManaged(showTopContact);
        }
        if (tplHeader != null) {
            boolean showHeader = (templateId != 3);
            tplHeader.setVisible(showHeader);
            tplHeader.setManaged(showHeader);
        }
        if (sectionSkillsBody != null) {
            boolean showBodySkills = (templateId != 3);
            sectionSkillsBody.setVisible(showBodySkills);
            sectionSkillsBody.setManaged(showBodySkills);
        }

        if (lblPreviewNameSide != null) {
            lblPreviewNameSide.setVisible(templateId == 3);
            lblPreviewNameSide.setManaged(templateId == 3);
        }
        if (previewSkillsSidebarList != null) {
            previewSkillsSidebarList.setVisible(templateId == 3);
            previewSkillsSidebarList.setManaged(templateId == 3);
        }
        if (skillsSidebarBlock != null) {
            skillsSidebarBlock.setVisible(templateId == 3);
            skillsSidebarBlock.setManaged(templateId == 3);
        }

        boolean textOnlySkills = (templateId == 2 || templateId == 3);
        if (skillPctBox != null) {
            skillPctBox.setVisible(!textOnlySkills);
            skillPctBox.setManaged(!textOnlySkills);
        }
    }

    @FXML private void onBack(ActionEvent e) {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/cv.fxml");
            if (url == null) throw new RuntimeException("cv.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de revenir aux templates.").show();
        }
    }

    @FXML private void onSectionInfo(ActionEvent e) { setActive(btnSecInfo); showSection("info"); setFormTitle("Modifier Informations"); }
    @FXML private void onSectionExp(ActionEvent e) { setActive(btnSecExp); showSection("exp"); setFormTitle("Modifier Expérience"); }
    @FXML private void onSectionProjects(ActionEvent e) { setActive(btnSecProjects); showSection("projects"); setFormTitle("Modifier Projects"); }
    @FXML private void onSectionEdu(ActionEvent e) { setActive(btnSecEdu); showSection("edu"); setFormTitle("Modifier Formation"); }
    @FXML private void onSectionSkills(ActionEvent e) { setActive(btnSecSkills); showSection("skills"); setFormTitle("Modifier Compétences"); }
    @FXML private void onSectionHobbies(ActionEvent e) { setActive(btnSecHobbies); showSection("hobbies"); setFormTitle("Modifier Hobbies"); }
    @FXML private void onSectionLang(ActionEvent e) { setActive(btnSecLang); showSection("lang"); setFormTitle("Modifier Langues"); }

    private void setFormTitle(String t) {
        if (lblFormTitle != null) lblFormTitle.setText(t);
        if (lblFormTitleExp != null) lblFormTitleExp.setText(t);
    }

    private void setActive(Button active) {
        Button[] all = new Button[]{btnSecInfo, btnSecExp, btnSecProjects, btnSecEdu, btnSecSkills, btnSecHobbies, btnSecLang};
        for (Button b : all) {
            if (b == null) continue;
            if (!b.getStyleClass().contains("cb-sec")) {
                b.getStyleClass().add("cb-sec");
            }
            b.getStyleClass().remove("cb-sec-active");
        }
        if (active != null && !active.getStyleClass().contains("cb-sec-active")) {
            active.getStyleClass().add("cb-sec-active");
        }
    }

    private void showSection(String key) {
        boolean info = "info".equalsIgnoreCase(key);
        boolean exp = "exp".equalsIgnoreCase(key);
        boolean projects = "projects".equalsIgnoreCase(key);
        boolean edu = "edu".equalsIgnoreCase(key);
        boolean skills = "skills".equalsIgnoreCase(key);
        boolean hobbies = "hobbies".equalsIgnoreCase(key);
        boolean lang = "lang".equalsIgnoreCase(key);

        if (cardInfo != null) { cardInfo.setVisible(info); cardInfo.setManaged(info); }
        if (cardExp != null) { cardExp.setVisible(exp); cardExp.setManaged(exp); }
        if (cardProjects != null) { cardProjects.setVisible(projects); cardProjects.setManaged(projects); }
        if (cardEdu != null) { cardEdu.setVisible(edu); cardEdu.setManaged(edu); }
        if (cardSkills != null) { cardSkills.setVisible(skills); cardSkills.setManaged(skills); }
        if (cardHobbies != null) { cardHobbies.setVisible(hobbies); cardHobbies.setManaged(hobbies); }
        if (cardLang != null) { cardLang.setVisible(lang); cardLang.setManaged(lang); }
    }

    @FXML
    private void onAddProject(ActionEvent e) {
        if (previewProjectsList == null) return;

        String name = safe(tfProjectName);
        String desc = taProjectDesc == null ? "" : safe(taProjectDesc.getText());

        String title = name.isBlank() ? "Nom du projet" : name;
        String d = desc.isBlank() ? "Description du projet..." : desc;

        VBox item;
        Label lTitle;
        Label lDesc;

        if (editingProjectItem != null) {
            item = editingProjectItem;
            lTitle = (Label) item.getProperties().get("title");
            lDesc = (Label) item.getProperties().get("desc");
        } else {
            item = new VBox(4);
            item.getStyleClass().add("tpl-item");

            lTitle = new Label();
            lTitle.getStyleClass().add("tpl-item-title");

            lDesc = new Label();
            lDesc.setWrapText(true);
            lDesc.getStyleClass().add("tpl-item-desc");

            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("tpl-act-btn");
            Button btnDel = new Button("✕");
            btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

            HBox header = new HBox(6);
            header.setFillHeight(true);
            header.getChildren().addAll(lTitle, spacer(), btnEdit, btnDel);

            item.getChildren().addAll(header, lDesc);
            item.getProperties().put("title", lTitle);
            item.getProperties().put("desc", lDesc);

            btnEdit.setOnAction(ev -> {
                editingProjectItem = item;
                if (btnAddProject != null) btnAddProject.setText("Mettre à jour");
                if (tfProjectName != null) tfProjectName.setText(lTitle.getText());
                if (taProjectDesc != null) taProjectDesc.setText(lDesc.getText());
            });

            btnDel.setOnAction(ev -> {
                if (previewProjectsList != null) previewProjectsList.getChildren().remove(item);
                if (editingProjectItem == item) {
                    editingProjectItem = null;
                    if (btnAddProject != null) btnAddProject.setText("+ Ajouter un project");
                }
            });

            previewProjectsList.getChildren().add(item);
        }

        lTitle.setText(title);
        lDesc.setText(d);

        if (editingProjectItem != null) {
            editingProjectItem = null;
            if (btnAddProject != null) btnAddProject.setText("+ Ajouter un project");
        }

        if (tfProjectName != null) tfProjectName.clear();
        if (taProjectDesc != null) taProjectDesc.clear();

        refreshPreview();
    }

    private void setupPreviewBindings() {
        if (tfFirstName == null || tfLastName == null) return;

        tfFirstName.textProperty().addListener((obs, o, n) -> refreshPreview());
        tfLastName.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfTitle != null) tfTitle.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (taSummary != null) taSummary.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfPhone != null) tfPhone.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfEmail != null) tfEmail.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfAddress != null) tfAddress.textProperty().addListener((obs, o, n) -> refreshPreview());

        if (tfJobTitle != null) tfJobTitle.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfCompany != null) tfCompany.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfStart != null) tfStart.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfEnd != null) tfEnd.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (taExpDesc != null) taExpDesc.textProperty().addListener((obs, o, n) -> refreshPreview());

        if (tfDegree != null) tfDegree.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfSchool != null) tfSchool.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfEduStart != null) tfEduStart.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (tfEduEnd != null) tfEduEnd.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (taLanguages != null) taLanguages.textProperty().addListener((obs, o, n) -> refreshPreview());
        if (taHobbies != null) taHobbies.textProperty().addListener((obs, o, n) -> refreshPreview());

        refreshPreview();
    }

    @FXML
    private void onChoosePhoto(ActionEvent e) {
        if (imgPreviewPhoto == null) return;

        Window owner = null;
        if (imgPreviewPhoto.getScene() != null) owner = imgPreviewPhoto.getScene().getWindow();

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une photo");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );

        File f = fc.showOpenDialog(owner);
        if (f == null) return;

        try {
            Image img = new Image(f.toURI().toString(), false);
            imgPreviewPhoto.setImage(img);

            double w = imgPreviewPhoto.getFitWidth() > 0 ? imgPreviewPhoto.getFitWidth() : 96;
            double h = imgPreviewPhoto.getFitHeight() > 0 ? imgPreviewPhoto.getFitHeight() : 96;
            double r = Math.min(w, h) / 2.0;

            Circle clip = new Circle(r, r, r);
            imgPreviewPhoto.setClip(clip);

            imgPreviewPhoto.setVisible(true);
            imgPreviewPhoto.setManaged(true);

            if (lblPreviewInitials != null) {
                lblPreviewInitials.setVisible(false);
                lblPreviewInitials.setManaged(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de charger l'image.").show();
        }
    }

    @FXML
    private void onAddSkill(ActionEvent e) {
        VBox target = (templateId == 3) && previewSkillsSidebarList != null
                ? previewSkillsSidebarList
                : previewSkillsList;
        if (target == null) return;

        boolean textOnly = (templateId == 2 || templateId == 3);
        if (textOnly) {
            String name = safe(tfSkillName);
            String title = name.isBlank() ? "Compétence" : name;

            Label label;
            HBox row;

            if (editingSkillLabel != null) {
                label = editingSkillLabel;
                row = (HBox) label.getProperties().get("row");
            } else {
                label = new Label();
                label.getStyleClass().add("tpl-bullet");

                Button btnEdit = new Button("✎");
                btnEdit.getStyleClass().add("tpl-act-btn");
                Button btnDel = new Button("✕");
                btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

                row = new HBox(6);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getChildren().addAll(label, spacer(), btnEdit, btnDel);
                label.getProperties().put("row", row);

                btnEdit.setOnAction(ev -> {
                    editingSkillLabel = label;
                    if (btnAddSkill != null) btnAddSkill.setText("Mettre à jour");
                    if (tfSkillName != null) tfSkillName.setText(label.getText());
                });

                btnDel.setOnAction(ev -> {
                    if (target != null) target.getChildren().remove(row);
                    if (editingSkillLabel == label) {
                        editingSkillLabel = null;
                        if (btnAddSkill != null) btnAddSkill.setText("+ Ajouter une compétence");
                        if (tfSkillName != null) tfSkillName.clear();
                    }
                });

                target.getChildren().add(row);
            }

            label.setText(title);

            if (editingSkillLabel != null) {
                editingSkillLabel = null;
                if (btnAddSkill != null) btnAddSkill.setText("+ Ajouter une compétence");
            }
            if (tfSkillName != null) tfSkillName.clear();
            refreshPreview();
            return;
        }

        String name = safe(tfSkillName);
        String pctRaw = tfSkillPct == null ? "" : safe(tfSkillPct);
        String title = name.isBlank() ? "Project Management" : name;
        int pct = parsePercent(pctRaw, 80);

        VBox item;
        Label lName;
        Label lPct;
        Region fill;
        DoubleProperty pctProp;

        if (editingSkillItem != null) {
            item = editingSkillItem;
            lName = (Label) item.getProperties().get("name");
            lPct = (Label) item.getProperties().get("pct");
            fill = (Region) item.getProperties().get("fill");
            pctProp = (DoubleProperty) item.getProperties().get("pctProp");
        } else {
            item = new VBox(6);
            item.getStyleClass().add("tpl-skill");

            lName = new Label();
            lName.getStyleClass().add("tpl-skill-name");

            lPct = new Label();
            lPct.getStyleClass().add("tpl-skill-pct");

            HBox header = new HBox(8);
            header.setAlignment(Pos.CENTER_LEFT);
            header.getChildren().addAll(lName, spacer(), lPct);

            StackPane bar = new StackPane();
            bar.getStyleClass().add("tpl-skill-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            StackPane.setAlignment(bar, Pos.CENTER_LEFT);

            fill = new Region();
            fill.getStyleClass().add("tpl-skill-bar-fill");
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            fill.setMinWidth(0);
            fill.setMaxWidth(Region.USE_PREF_SIZE);
            bar.getChildren().add(fill);

            pctProp = new SimpleDoubleProperty(0);
            fill.prefWidthProperty().bind(Bindings.createDoubleBinding(
                    () -> (bar.getWidth() * (pctProp.get() / 100.0)),
                    bar.widthProperty(), pctProp
            ));

            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("tpl-act-btn");
            Button btnDel = new Button("✕");
            btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

            HBox actions = new HBox(6);
            actions.setAlignment(Pos.CENTER_LEFT);
            actions.getChildren().addAll(btnEdit, btnDel);

            item.getChildren().addAll(header, bar, actions);
            item.getProperties().put("name", lName);
            item.getProperties().put("pct", lPct);
            item.getProperties().put("fill", fill);
            item.getProperties().put("pctProp", pctProp);

            btnEdit.setOnAction(ev -> {
                editingSkillItem = item;
                if (btnAddSkill != null) btnAddSkill.setText("Mettre à jour");
                if (tfSkillName != null) tfSkillName.setText(lName.getText());
                if (tfSkillPct != null) tfSkillPct.setText(lPct.getText().replace("%", "").trim());
            });

            btnDel.setOnAction(ev -> {
                if (previewSkillsList != null) previewSkillsList.getChildren().remove(item);
                if (editingSkillItem == item) {
                    editingSkillItem = null;
                    if (btnAddSkill != null) btnAddSkill.setText("+ Ajouter une compétence");
                    if (tfSkillName != null) tfSkillName.clear();
                    if (tfSkillPct != null) tfSkillPct.clear();
                }
            });

            if (previewSkillsList != null) previewSkillsList.getChildren().add(item);
        }

        lName.setText(title);
        lPct.setText(pct + "%");
        pctProp.set(pct);

        if (editingSkillItem != null) {
            editingSkillItem = null;
            if (btnAddSkill != null) btnAddSkill.setText("+ Ajouter une compétence");
        }

        if (tfSkillName != null) tfSkillName.clear();
        if (tfSkillPct != null) tfSkillPct.clear();
        refreshPreview();
    }

    private int parsePercent(String raw, int fallback) {
        if (raw == null) return fallback;
        String s = raw.trim().replace("%", "");
        if (s.isBlank()) return fallback;
        try {
            int v = Integer.parseInt(s);
            if (v < 0) v = 0;
            if (v > 100) v = 100;
            return v;
        } catch (Exception ex) {
            return fallback;
        }
    }

    @FXML
    private void onAddEducation(ActionEvent e) {
        if (previewEduList == null) return;

        String degree = safe(tfDegree);
        String school = safe(tfSchool);
        String d1 = safe(tfEduStart);
        String d2 = safe(tfEduEnd);

        String title = degree.isBlank() ? "Nom du Diplôme" : degree;
        String sub = school.isBlank() ? "Nom de l’École" : school;
        String dates = (d1 + " - " + d2).trim().replaceAll("^\\s*-\\s*|\\s*-\\s*$", "");
        if (dates.isBlank()) dates = "2014 - 2023";

        VBox item;
        Label lTitle;
        Label lSub;
        Label lDates;

        if (editingEduItem != null) {
            item = editingEduItem;
            lTitle = (Label) item.getProperties().get("title");
            lSub = (Label) item.getProperties().get("sub");
            lDates = (Label) item.getProperties().get("dates");
        } else {
            item = new VBox(4);
            item.getStyleClass().add("tpl-item");

            lTitle = new Label();
            lTitle.getStyleClass().add("tpl-item-title");
            lSub = new Label();
            lSub.getStyleClass().add("tpl-item-sub");
            lDates = new Label();
            lDates.getStyleClass().add("tpl-item-date");

            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("tpl-act-btn");
            Button btnDel = new Button("✕");
            btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

            HBox header = new HBox(6);
            header.setFillHeight(true);
            header.getChildren().addAll(lTitle, spacer(), btnEdit, btnDel);

            item.getChildren().addAll(header, lSub, lDates);
            item.getProperties().put("title", lTitle);
            item.getProperties().put("sub", lSub);
            item.getProperties().put("dates", lDates);

            btnEdit.setOnAction(ev -> {
                editingEduItem = item;
                if (btnAddEducation != null) btnAddEducation.setText("Mettre à jour");
                if (tfDegree != null) tfDegree.setText(lTitle.getText());
                if (tfSchool != null) tfSchool.setText(lSub.getText());
                String[] parts = lDates.getText().split("-");
                if (tfEduStart != null) tfEduStart.setText(parts.length > 0 ? parts[0].trim() : "");
                if (tfEduEnd != null) tfEduEnd.setText(parts.length > 1 ? parts[1].trim() : "");
            });

            btnDel.setOnAction(ev -> {
                if (previewEduList != null) previewEduList.getChildren().remove(item);
                if (editingEduItem == item) {
                    editingEduItem = null;
                    if (btnAddEducation != null) btnAddEducation.setText("+ Ajouter une formation");
                }
            });

            previewEduList.getChildren().add(item);
        }

        lTitle.setText(title);
        lSub.setText(sub);
        lDates.setText(dates);

        if (editingEduItem != null) {
            editingEduItem = null;
            if (btnAddEducation != null) btnAddEducation.setText("+ Ajouter une formation");
        }

        if (tfDegree != null) tfDegree.clear();
        if (tfSchool != null) tfSchool.clear();
        if (tfEduStart != null) tfEduStart.clear();
        if (tfEduEnd != null) tfEduEnd.clear();

        refreshPreview();
    }

    @FXML
    private void onAddLanguages(ActionEvent e) {
        if (previewLangList == null || taLanguages == null) return;

        String raw = safe(taLanguages.getText());
        if (raw.isBlank()) return;

        if (editingLangLabel != null) {
            String v = raw.trim();
            if (!v.isBlank()) editingLangLabel.setText(v);
            editingLangLabel = null;
            if (btnAddLanguage != null) btnAddLanguage.setText("+ Ajouter des langues");
            taLanguages.clear();
            refreshPreview();
            return;
        }

        String[] langs = raw.split("\\r?\\n|,");
        for (String s : langs) {
            String v = s == null ? "" : s.trim();
            if (v.isBlank()) continue;

            Label l = new Label(v);
            l.getStyleClass().add("tpl-bullet");

            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("tpl-act-btn");
            Button btnDel = new Button("✕");
            btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

            HBox row = new HBox(6);
            row.getChildren().addAll(l, spacer(), btnEdit, btnDel);

            btnEdit.setOnAction(ev -> {
                editingLangLabel = l;
                if (btnAddLanguage != null) btnAddLanguage.setText("Mettre à jour");
                if (taLanguages != null) taLanguages.setText(l.getText());
            });

            btnDel.setOnAction(ev -> {
                if (previewLangList != null) previewLangList.getChildren().remove(row);
                if (editingLangLabel == l) {
                    editingLangLabel = null;
                    if (btnAddLanguage != null) btnAddLanguage.setText("+ Ajouter des langues");
                    if (taLanguages != null) taLanguages.clear();
                }
            });

            previewLangList.getChildren().add(row);
        }

        taLanguages.clear();
        refreshPreview();
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, javafx.scene.layout.Priority.ALWAYS);
        return r;
    }

    private void refreshPreview() {
        String first = safe(tfFirstName);
        String last = safe(tfLastName);
        String full = (first + " " + last).trim();

        if (lblPreviewName != null) {
            lblPreviewName.setText(full.isBlank() ? "VOTRE PRÉNOM VOTRE NOM" : (first + " " + last).trim().toUpperCase());
        }
        if (lblPreviewNameSide != null) {
            lblPreviewNameSide.setText(full.isBlank() ? "VOTRE PRÉNOM VOTRE NOM" : (first + " " + last).trim());
        }
        if (lblPreviewInitials != null) {
            lblPreviewInitials.setText(initials(first, last));
        }
        if (lblPreviewTitle != null) {
            String t = safe(tfTitle);
            lblPreviewTitle.setText(t.isBlank() ? "Votre Titre Professionnel" : t);
        }
        if (lblPreviewSummary != null) {
            String s = taSummary == null ? "" : safe(taSummary.getText());
            lblPreviewSummary.setText(s.isBlank() ? "Décrivez-vous en quelques lignes..." : s);
        }

        if (lblPreviewPhone != null) {
            String p = safe(tfPhone);
            lblPreviewPhone.setText(p.isBlank() ? "+33 6 12 34 56 78" : p);
        }
        if (lblPreviewEmail != null) {
            String em = safe(tfEmail);
            lblPreviewEmail.setText(em.isBlank() ? "email@example.com" : em);
        }
        if (lblPreviewAddress != null) {
            String a = safe(tfAddress);
            lblPreviewAddress.setText(a.isBlank() ? "Votre Adresse" : a);
        }

        if (lblPreviewJobTitle != null) {
            String jt = safe(tfJobTitle);
            lblPreviewJobTitle.setText(jt.isBlank() ? "Titre du Poste" : jt);
        }
        if (lblPreviewCompany != null) {
            String c = safe(tfCompany);
            lblPreviewCompany.setText(c.isBlank() ? "Nom de l’Entreprise - Ville, Pays" : c);
        }
        if (lblPreviewExpDates != null) {
            String d1 = safe(tfStart);
            String d2 = safe(tfEnd);
            String dates = (d1 + " - " + d2).trim();
            lblPreviewExpDates.setText((d1.isBlank() && d2.isBlank()) ? "2020 - 2022" : dates.replaceAll("^\\s*-\\s*|\\s*-\\s*$", ""));
        }
        if (lblPreviewExpDesc != null) {
            String desc = taExpDesc == null ? "" : safe(taExpDesc.getText());
            lblPreviewExpDesc.setText(desc.isBlank() ? "Description de vos responsabilités et réalisations..." : desc);
        }

        if (lblPreviewDegree != null) {
            String deg = safe(tfDegree);
            lblPreviewDegree.setText(deg.isBlank() ? "Nom du Diplôme" : deg);
        }
        if (lblPreviewSchool != null) {
            String sch = safe(tfSchool);
            lblPreviewSchool.setText(sch.isBlank() ? "Nom de l’École" : sch);
        }
        if (lblPreviewEduDates != null) {
            String d1 = safe(tfEduStart);
            String d2 = safe(tfEduEnd);
            String dates = (d1 + " - " + d2).trim();
            lblPreviewEduDates.setText((d1.isBlank() && d2.isBlank()) ? "2014 - 2023" : dates.replaceAll("^\\s*-\\s*|\\s*-\\s*$", ""));
        }

        String langText = taLanguages == null ? "" : safe(taLanguages.getText());
        String[] langs = langText.isBlank() ? new String[0] : langText.split("\\r?\\n|,");
        String l1 = (langs.length > 0) ? langs[0].trim() : "English";
        String l2 = (langs.length > 1) ? langs[1].trim() : "Germany (basic)";
        if (lblPreviewLang1 != null) lblPreviewLang1.setText(l1.isBlank() ? "English" : l1);
        if (lblPreviewLang2 != null) lblPreviewLang2.setText(l2.isBlank() ? "Germany (basic)" : l2);

        if (previewHobbiesList != null) {
            previewHobbiesList.getChildren().clear();
            String hText = taHobbies == null ? "" : safe(taHobbies.getText());
            String[] hobbies = hText.isBlank() ? new String[0] : hText.split("\\r?\\n|,");
            for (String raw : hobbies) {
                String v = raw == null ? "" : raw.trim();
                if (v.isBlank()) continue;
                Label l = new Label(v);
                l.setWrapText(true);
                l.getStyleClass().add("tpl-item-desc");
                previewHobbiesList.getChildren().add(l);
            }
        }
    }

    @FXML
    private void onAddHobbies(ActionEvent e) {
        refreshPreview();
    }

    @FXML
    private void onAddExperience(ActionEvent e) {
        if (previewExpList == null) return;

        String title = safe(tfJobTitle);
        String company = safe(tfCompany);
        String d1 = safe(tfStart);
        String d2 = safe(tfEnd);
        String desc = taExpDesc == null ? "" : safe(taExpDesc.getText());

        String headline = (title + (company.isBlank() ? "" : (" - " + company))).trim();
        if (headline.isBlank()) headline = "Titre du Poste";

        String dates = (d1 + " - " + d2).trim().replaceAll("^\\s*-\\s*|\\s*-\\s*$", "");
        if (dates.isBlank()) dates = "2020 - 2022";

        if (desc.isBlank()) desc = "Description de vos responsabilités et réalisations...";

        VBox item;
        Label lTitle;
        Label lDates;
        Label lDesc;

        if (editingExpItem != null) {
            item = editingExpItem;
            lTitle = (Label) item.getProperties().get("title");
            lDates = (Label) item.getProperties().get("dates");
            lDesc = (Label) item.getProperties().get("desc");
        } else {
            item = new VBox(4);
            item.getStyleClass().add("tpl-item");

            lTitle = new Label();
            lTitle.getStyleClass().add("tpl-item-title");

            lDates = new Label();
            lDates.getStyleClass().add("tpl-item-date");

            lDesc = new Label();
            lDesc.setWrapText(true);
            lDesc.getStyleClass().add("tpl-item-desc");

            Button btnEdit = new Button("✎");
            btnEdit.getStyleClass().add("tpl-act-btn");
            Button btnDel = new Button("✕");
            btnDel.getStyleClass().addAll("tpl-act-btn", "tpl-act-btn-danger");

            HBox header = new HBox(6);
            header.setFillHeight(true);
            header.getChildren().addAll(lTitle, spacer(), btnEdit, btnDel);

            item.getChildren().addAll(header, lDates, lDesc);
            item.getProperties().put("title", lTitle);
            item.getProperties().put("dates", lDates);
            item.getProperties().put("desc", lDesc);

            btnEdit.setOnAction(ev -> {
                editingExpItem = item;
                if (btnAddExperience != null) btnAddExperience.setText("Mettre à jour");

                if (tfJobTitle != null) tfJobTitle.setText(titlePart(lTitle.getText()));
                if (tfCompany != null) tfCompany.setText(companyPart(lTitle.getText()));

                String[] parts = lDates.getText().split("-");
                if (tfStart != null) tfStart.setText(parts.length > 0 ? parts[0].trim() : "");
                if (tfEnd != null) tfEnd.setText(parts.length > 1 ? parts[1].trim() : "");
                if (taExpDesc != null) taExpDesc.setText(lDesc.getText());
            });

            btnDel.setOnAction(ev -> {
                if (previewExpList != null) previewExpList.getChildren().remove(item);
                if (editingExpItem == item) {
                    editingExpItem = null;
                    if (btnAddExperience != null) btnAddExperience.setText("+ Ajouter une expérience");
                    if (tfJobTitle != null) tfJobTitle.clear();
                    if (tfCompany != null) tfCompany.clear();
                    if (tfStart != null) tfStart.clear();
                    if (tfEnd != null) tfEnd.clear();
                    if (taExpDesc != null) taExpDesc.clear();
                }
                refreshPreview();
            });

            previewExpList.getChildren().add(item);
        }

        lTitle.setText(headline);
        lDates.setText(dates);
        lDesc.setText(desc);

        if (editingExpItem != null) {
            editingExpItem = null;
            if (btnAddExperience != null) btnAddExperience.setText("+ Ajouter une expérience");
        }

        if (tfJobTitle != null) tfJobTitle.clear();
        if (tfCompany != null) tfCompany.clear();
        if (tfStart != null) tfStart.clear();
        if (tfEnd != null) tfEnd.clear();
        if (taExpDesc != null) taExpDesc.clear();

        refreshPreview();
    }

    private String titlePart(String headline) {
        if (headline == null) return "";
        int i = headline.indexOf(" - ");
        return i >= 0 ? headline.substring(0, i).trim() : headline.trim();
    }

    private String companyPart(String headline) {
        if (headline == null) return "";
        int i = headline.indexOf(" - ");
        return i >= 0 ? headline.substring(i + 3).trim() : "";
    }

    private String safe(TextField tf) {
        if (tf == null || tf.getText() == null) return "";
        return tf.getText().trim();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String initials(String first, String last) {
        String f = (first == null ? "" : first.trim());
        String l = (last == null ? "" : last.trim());
        String i1 = f.isEmpty() ? "" : ("" + Character.toUpperCase(f.charAt(0)));
        String i2 = l.isEmpty() ? "" : ("" + Character.toUpperCase(l.charAt(0)));
        String init = (i1 + i2).trim();
        return init.isEmpty() ? "VV" : init;
    }

    private Pane findCenterWrap() {
        var scene = javafx.stage.Window.getWindows().stream()
                .filter(w -> w.isShowing() && w.getScene() != null)
                .map(w -> w.getScene())
                .findFirst()
                .orElse(null);
        if (scene == null) return null;

        var n = scene.lookup("#pageContainer");
        if (n instanceof Pane p) return p;

        n = scene.lookup("#contentArea");
        if (n instanceof Pane p2) return p2;

        return null;
    }
}
