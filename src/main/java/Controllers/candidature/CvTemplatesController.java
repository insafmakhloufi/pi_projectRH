package Controllers.candidature;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CvTemplatesController {

    @FXML private VBox card1;
    @FXML private VBox card2;
    @FXML private VBox card3;

    @FXML private Label badge1;
    @FXML private Label badge2;
    @FXML private Label badge3;

    @FXML private Button btnSelect1;
    @FXML private Button btnSelect2;
    @FXML private Button btnSelect3;

    @FXML private Button btnContinue;

    private Integer selectedTemplateId;

    @FXML
    private void initialize() {
        setSelected(null);
    }

    @FXML
    private void onPreview1() { openPdfResource("/Images/template-1.pdf", "template-1"); }

    @FXML
    private void onPreview2() { openPdfResource("/Images/template-2.pdf", "template-2"); }

    @FXML
    private void onPreview3() { openPdfResource("/Images/template-3.pdf", "template-3"); }

    @FXML
    private void onSelect1() { setSelected(1); }

    @FXML
    private void onSelect2() { setSelected(2); }

    @FXML
    private void onSelect3() { setSelected(3); }

    @FXML
    private void onContinue() {
        if (selectedTemplateId == null) return;
        openBuilder(selectedTemplateId);
    }

    @FXML
    private void onBack(ActionEvent event) {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/MesCandidatures.fxml");
            if (url == null) throw new RuntimeException("MesCandidatures.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de revenir à Mes candidatures.").show();
        }
    }

    private void openBuilder(int templateId) {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/cv_builder.fxml");
            if (url == null) throw new RuntimeException("cv_builder.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            CvBuilderController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.initTemplate(templateId);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir l'éditeur CV.").show();
        }
    }

    private void setSelected(Integer templateId) {
        this.selectedTemplateId = templateId;

        boolean s1 = templateId != null && templateId == 1;
        boolean s2 = templateId != null && templateId == 2;
        boolean s3 = templateId != null && templateId == 3;

        setCardSelected(card1, badge1, btnSelect1, s1);
        setCardSelected(card2, badge2, btnSelect2, s2);
        setCardSelected(card3, badge3, btnSelect3, s3);

        if (btnContinue != null) {
            btnContinue.setDisable(templateId == null);
        }
    }

    private void setCardSelected(VBox card, Label badge, Button btn, boolean selected) {
        if (card != null) {
            if (selected) {
                if (!card.getStyleClass().contains("cv-card-selected")) card.getStyleClass().add("cv-card-selected");
            } else {
                card.getStyleClass().remove("cv-card-selected");
            }
        }
        if (badge != null) {
            badge.setVisible(selected);
            badge.setManaged(selected);
        }
        if (btn != null) {
            btn.setText(selected ? "Sélectionné" : "Sélectionner");
            btn.setDisable(selected);
        }
    }

    private void openPdfResource(String resourcePath, String tempPrefix) {
        try {
            if (!Desktop.isDesktopSupported()) {
                new Alert(Alert.AlertType.ERROR, "Ouverture de fichier non supportée sur cette machine.").show();
                return;
            }

            URL url = getClass().getResource(resourcePath);
            if (url == null) {
                new Alert(Alert.AlertType.ERROR, "Fichier introuvable: " + resourcePath).show();
                return;
            }

            File file;
            try {
                URI uri = url.toURI();
                if ("file".equalsIgnoreCase(uri.getScheme())) {
                    file = new File(uri);
                } else {
                    file = copyToTemp(url, tempPrefix);
                }
            } catch (Exception e) {
                file = copyToTemp(url, tempPrefix);
            }

            Desktop.getDesktop().open(file);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le template.").show();
        }
    }

    private File copyToTemp(URL url, String prefix) throws Exception {
        Path tmp = Files.createTempFile(prefix + "-", ".pdf");
        try (InputStream in = url.openStream()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        tmp.toFile().deleteOnExit();
        return tmp.toFile();
    }

    private Pane findCenterWrap() {
        return lookupCenterWrap();
    }

    private Pane lookupCenterWrap() {
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
