package Controllers.Formation;

import Entities.Formation.Chapitre;
import Services.Formation.ChapitreServices;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.collections.FXCollections;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.rendering.ImageType;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetailChapitre {

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    @FXML private Button btnRetour;
    @FXML private Button btnOuvrirPdf;
    @FXML private Button btnResume;
    @FXML private Label lblTitre;
    @FXML private Label lblMeta;
    @FXML private TextArea taContenu;
    @FXML private ScrollPane spText;
    @FXML private VBox pdfPane;
    @FXML private ComboBox<String> cbPdfs;
    @FXML private Button btnPrevPage;
    @FXML private Button btnNextPage;
    @FXML private Label lblPage;
    @FXML private ScrollPane spPdf;
    @FXML private ImageView ivPdf;
    @FXML private Label lblMessage;

    private final ChapitreServices chapitreServices = new ChapitreServices();

    private final List<String> pdfPaths = new ArrayList<>();
    private PDDocument currentDoc;
    private PDFRenderer currentRenderer;
    private int currentPageIndex = 0;
    private String currentPdfPath;
    private Chapitre currentChapitre;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private static final Pattern OPENAI_CONTENT_PATTERN = Pattern.compile("\\\"content\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);

    @FXML
    void initialize() {
        if (btnRetour != null) {
            btnRetour.setOnAction(e -> retour());
        }
        if (btnOuvrirPdf != null) {
            btnOuvrirPdf.setOnAction(e -> ouvrirPdf());
        }
        if (btnResume != null) {
            btnResume.setOnAction(e -> resumeChapitre());
        }

        if (btnPrevPage != null) {
            btnPrevPage.setOnAction(e -> prevPage());
        }
        if (btnNextPage != null) {
            btnNextPage.setOnAction(e -> nextPage());
        }
        if (cbPdfs != null) {
            cbPdfs.getSelectionModel().selectedIndexProperty().addListener((obs, o, n) -> {
                if (n == null) {
                    return;
                }
                int idx = n.intValue();
                if (idx < 0 || idx >= pdfPaths.size()) {
                    return;
                }
                openPdfInViewer(pdfPaths.get(idx));
            });
        }

        loadChapitre();
    }

    private void loadChapitre() {
        Integer chapitreId = UiState.selectedChapitreId;
        if (chapitreId == null) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Aucun chapitre sélectionné");
            }
            if (btnOuvrirPdf != null) {
                btnOuvrirPdf.setDisable(true);
                btnOuvrirPdf.setVisible(false);
                btnOuvrirPdf.setManaged(false);
            }
            return;
        }

        Chapitre ch = chapitreServices.getChapitreById(chapitreId);
        if (ch == null) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Chapitre introuvable");
            }
            if (btnOuvrirPdf != null) {
                btnOuvrirPdf.setDisable(true);
                btnOuvrirPdf.setVisible(false);
                btnOuvrirPdf.setManaged(false);
            }
            return;
        }

        currentChapitre = ch;

        if (lblTitre != null) {
            String titre = (ch.getTitre() == null) ? "Chapitre" : ch.getTitre();
            lblTitre.setText(titre);
        }
        if (lblMeta != null) {
            lblMeta.setText("Ordre: " + ch.getOrdre());
        }

        String contenu = ch.getContenu() == null ? "" : ch.getContenu();
        boolean isPdf = contenu.startsWith(PDF_PREFIX) || contenu.startsWith(PDFS_PREFIX);

        if (!isPdf) {
            showTextPane();
            if (taContenu != null) {
                taContenu.setText(contenu);
            }
            if (btnOuvrirPdf != null) {
                btnOuvrirPdf.setDisable(true);
                btnOuvrirPdf.setVisible(false);
                btnOuvrirPdf.setManaged(false);
            }
            return;
        }

        // PDF case
        parsePdfPaths(contenu);
        showPdfPane();
        setupPdfCombo();
        if (!pdfPaths.isEmpty()) {
            openPdfInViewer(pdfPaths.get(0));
        }

        if (btnOuvrirPdf != null) {
            btnOuvrirPdf.setDisable(pdfPaths.isEmpty());
            btnOuvrirPdf.setVisible(true);
            btnOuvrirPdf.setManaged(true);
        }
    }

    private void resumeChapitre() {
        Chapitre ch = currentChapitre;
        if (ch == null) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Aucun chapitre sélectionné");
            }
            return;
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Clé OPENAI_API_KEY manquante");
            }
            return;
        }

        if (btnResume != null) {
            btnResume.setDisable(true);
        }
        if (lblMessage != null) {
            lblMessage.setStyle("-fx-text-fill: #6b7280;");
            lblMessage.setText("Génération du résumé...");
        }

        new Thread(() -> {
            try {
                String sourceText = extractSourceText(ch);
                if (sourceText == null || sourceText.isBlank()) {
                    Platform.runLater(() -> {
                        if (lblMessage != null) {
                            lblMessage.setStyle("-fx-text-fill: #dc2626;");
                            lblMessage.setText("Aucun texte à résumer");
                        }
                    });
                    return;
                }

                String result = callOpenAiForSummary(apiKey, sourceText);
                String clean = unescapeJson(result == null ? "" : result).trim();
                Platform.runLater(() -> showSummaryDialog(clean));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    if (lblMessage != null) {
                        lblMessage.setStyle("-fx-text-fill: #dc2626;");
                        lblMessage.setText("Erreur: " + ex.getMessage());
                    }
                });
            } finally {
                Platform.runLater(() -> {
                    if (btnResume != null) {
                        btnResume.setDisable(false);
                    }
                });
            }
        }).start();
    }

    private String extractSourceText(Chapitre ch) {
        String contenu = (ch == null || ch.getContenu() == null) ? "" : ch.getContenu().trim();
        if (contenu.isBlank()) {
            return "";
        }
        if (!contenu.startsWith(PDF_PREFIX) && !contenu.startsWith(PDFS_PREFIX)) {
            return contenu;
        }

        String path = currentPdfPath;
        if ((path == null || path.isBlank()) && !pdfPaths.isEmpty()) {
            path = pdfPaths.get(0);
        }
        if (path == null || path.isBlank()) {
            return "";
        }
        File f = new File(path);
        if (!f.exists()) {
            return "";
        }

        try (PDDocument doc = PDDocument.load(f)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return text == null ? "" : text;
        } catch (Exception e) {
            return "";
        }
    }

    private String callOpenAiForSummary(String apiKey, String sourceText) throws Exception {
        String model = System.getenv("OPENAI_MODEL");
        if (model == null || model.isBlank()) {
            model = "openai/gpt-4o-mini";
        } else {
            String trimmed = model.trim();
            if (!trimmed.contains("/")) {
                model = "openai/" + trimmed;
            } else {
                model = trimmed;
            }
        }

        String chapterTitle = (currentChapitre == null || currentChapitre.getTitre() == null) ? "Chapitre" : currentChapitre.getTitre();
        String prompt = "Tu es un assistant pédagogique. Donne un résumé clair et court (5-8 lignes) et ensuite une liste de points clés (5-10 bullets) pour le contenu suivant. Réponds en français. Format: \nRésumé: ...\n\nPoints clés:\n- ...\n- ...\n\nTitre: " + chapterTitle + "\n\nContenu:\n";

        String clipped = clipText(sourceText, 6000);
        String json = "{" +
                "\"model\":\"" + escapeJson(model) + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"Tu génères un résumé et des points clés.\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt + clipped) + "\"}" +
                "]," +
                "\"temperature\":0.3," +
                "\"max_tokens\":450" +
                "}";

        URI uri = URI.create("https://openrouter.ai/api/v1/chat/completions");
        String body;
        HttpResponse<byte[]> response;

        int attempts = 0;
        while (true) {
            attempts++;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(45))
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("HTTP-Referer", "http://localhost")
                    .header("X-Title", "CareerLink")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
            body = new String(response.body(), StandardCharsets.UTF_8);

            if (response.statusCode() == 429 && attempts < 3) {
                try {
                    Thread.sleep(1200L * attempts);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            break;
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            int code = response.statusCode();
            if (code == 401) {
                throw new RuntimeException("Clé OpenRouter invalide (401)");
            }
            if (code == 429) {
                throw new RuntimeException("Trop de requêtes ou quota dépassé (429). Vérifie ton abonnement/crédit, ou réessaie dans 1-2 minutes.");
            }
            throw new RuntimeException("Erreur API (" + code + ")");
        }

        Matcher m = OPENAI_CONTENT_PATTERN.matcher(body);
        if (!m.find()) {
            return null;
        }
        return m.group(1);
    }

    private void showSummaryDialog(String text) {
        if (lblMessage != null) {
            lblMessage.setText("");
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Résumé du chapitre");
        dialog.setResizable(true);

        DialogPane pane = dialog.getDialogPane();
        pane.setStyle(
                "-fx-background-color: white;" +
                "-fx-font-family: 'Segoe UI';" +
                "-fx-font-size: 13px;"
        );

        ButtonType copyType = new ButtonType("Copier", ButtonBar.ButtonData.OTHER);
        pane.getButtonTypes().addAll(copyType, ButtonType.CLOSE);

        Label header = new Label("Résumé & points clés");
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #111827;");

        Label hint = new Label("Tu peux copier le contenu pour le partager.");
        hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

        TextArea ta = new TextArea(text == null ? "" : text);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setStyle(
                "-fx-control-inner-background: #f9fafb;" +
                "-fx-text-fill: #111827;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-highlight-fill: #bfdbfe;" +
                "-fx-highlight-text-fill: #111827;"
        );
        ta.setPrefHeight(520);

        VBox content = new VBox(10);
        content.setPadding(new Insets(14, 14, 14, 14));
        content.getChildren().addAll(header, hint, ta);
        VBox.setVgrow(ta, Priority.ALWAYS);

        pane.setContent(content);
        pane.setPrefWidth(820);
        pane.setPrefHeight(640);

        final Button copyBtn = (Button) pane.lookupButton(copyType);
        if (copyBtn != null) {
            copyBtn.setDefaultButton(false);
            copyBtn.addEventFilter(ActionEvent.ACTION, e -> {
                ClipboardContent cc = new ClipboardContent();
                cc.putString(ta.getText());
                Clipboard.getSystemClipboard().setContent(cc);
                if (lblMessage != null) {
                    lblMessage.setStyle("-fx-text-fill: #16a34a;");
                    lblMessage.setText("Résumé copié dans le presse-papier");
                }
                e.consume();
            });
            copyBtn.setStyle(
                    "-fx-background-color: #111827;" +
                    "-fx-text-fill: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 6 14;"
            );
        }

        final Button closeBtn = (Button) pane.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setStyle(
                    "-fx-background-color: #e5e7eb;" +
                    "-fx-text-fill: #111827;" +
                    "-fx-background-radius: 10;" +
                    "-fx-padding: 6 14;"
            );
        }
        dialog.showAndWait();
    }

    private static String clipText(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        String v = s.trim();
        if (v.length() <= maxChars) {
            return v;
        }
        return v.substring(0, maxChars);
    }

    private static String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) {
            return "";
        }
        return s
                .replace("\\\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\/", "/")
                .replace("\\\\", "\\");
    }

    private void parsePdfPaths(String contenu) {
        pdfPaths.clear();
        if (contenu == null) {
            return;
        }
        if (contenu.startsWith(PDF_PREFIX)) {
            String raw = contenu.substring(PDF_PREFIX.length()).trim();
            if (!raw.isEmpty()) {
                pdfPaths.add(raw);
            }
            return;
        }
        if (contenu.startsWith(PDFS_PREFIX)) {
            String raw = contenu.substring(PDFS_PREFIX.length()).trim();
            if (raw.isEmpty()) {
                return;
            }
            String[] parts = raw.split(";");
            for (String p : parts) {
                if (p == null) {
                    continue;
                }
                String v = p.trim();
                if (v.isEmpty()) {
                    continue;
                }
                pdfPaths.add(v);
            }
        }
    }

    private void setupPdfCombo() {
        if (cbPdfs == null) {
            return;
        }
        List<String> items = new ArrayList<>();
        for (String p : pdfPaths) {
            if (p == null) {
                continue;
            }
            File f = new File(p);
            items.add(f.getName());
        }
        cbPdfs.setItems(FXCollections.observableArrayList(items));
        if (!items.isEmpty()) {
            cbPdfs.getSelectionModel().select(0);
        }
    }

    private void showTextPane() {
        if (spText != null) {
            spText.setVisible(true);
            spText.setManaged(true);
        }
        if (pdfPane != null) {
            pdfPane.setVisible(false);
            pdfPane.setManaged(false);
        }
        closeCurrentDoc();
    }

    private void showPdfPane() {
        if (spText != null) {
            spText.setVisible(false);
            spText.setManaged(false);
        }
        if (pdfPane != null) {
            pdfPane.setVisible(true);
            pdfPane.setManaged(true);
        }
    }

    private void openPdfInViewer(String rawPath) {
        try {
            closeCurrentDoc();

            if (rawPath == null || rawPath.isBlank()) {
                return;
            }
            currentPdfPath = rawPath;
            File f = new File(rawPath);
            if (!f.exists()) {
                if (lblMessage != null) {
                    lblMessage.setStyle("-fx-text-fill: #dc2626;");
                    lblMessage.setText("Fichier PDF introuvable");
                }
                return;
            }

            currentDoc = PDDocument.load(f);
            currentRenderer = new PDFRenderer(currentDoc);
            currentPageIndex = 0;
            renderCurrentPage();
        } catch (Exception e) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Erreur: " + e.getMessage());
            }
        }
    }

    private void renderCurrentPage() {
        if (currentDoc == null || currentRenderer == null) {
            return;
        }
        try {
            int pageCount = currentDoc.getNumberOfPages();
            if (pageCount <= 0) {
                return;
            }
            if (currentPageIndex < 0) {
                currentPageIndex = 0;
            }
            if (currentPageIndex >= pageCount) {
                currentPageIndex = pageCount - 1;
            }

            BufferedImage img = currentRenderer.renderImageWithDPI(currentPageIndex, 150, ImageType.RGB);
            Image fxImg = SwingFXUtils.toFXImage(img, null);
            if (ivPdf != null) {
                ivPdf.setImage(fxImg);
                // Fit to available width
                if (spPdf != null) {
                    ivPdf.setFitWidth(Math.max(400, spPdf.getWidth() - 30));
                }
            }

            if (lblPage != null) {
                lblPage.setText((currentPageIndex + 1) + " / " + pageCount);
            }
            if (btnPrevPage != null) {
                btnPrevPage.setDisable(currentPageIndex <= 0);
            }
            if (btnNextPage != null) {
                btnNextPage.setDisable(currentPageIndex >= pageCount - 1);
            }
        } catch (Exception e) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Erreur: " + e.getMessage());
            }
        }
    }

    private void prevPage() {
        if (currentDoc == null) {
            return;
        }
        currentPageIndex--;
        renderCurrentPage();
    }

    private void nextPage() {
        if (currentDoc == null) {
            return;
        }
        currentPageIndex++;
        renderCurrentPage();
    }

    private void closeCurrentDoc() {
        try {
            if (currentDoc != null) {
                currentDoc.close();
            }
        } catch (Exception ignored) {
        } finally {
            currentDoc = null;
            currentRenderer = null;
        }
    }

    private void ouvrirPdf() {
        try {
            // External open fallback
            for (String p : pdfPaths) {
                openPdfPath(p);
            }
        } catch (Exception e) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Erreur: " + e.getMessage());
            }
        }
    }

    private void openPdfPath(String rawPath) throws Exception {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }
        File f = new File(rawPath);
        if (!f.exists()) {
            if (lblMessage != null) {
                lblMessage.setStyle("-fx-text-fill: #dc2626;");
                lblMessage.setText("Fichier PDF introuvable");
            }
            return;
        }
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(f);
        }
    }

    private void retour() {
        try {
            String target = UiState.selectedChapitreReturnFxml;
            if (target == null || target.isBlank()) {
                target = "/Formation/ChapitreFront.fxml";
            }
            WindowUtil.navigate(btnRetour, target, "Chapitres");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
