package Controllers.Entreprise;

import Entities.Entreprise.Entreprise;
import Services.Entreprise.EntrepriseService;
import Utils.Session;
import Utils.ThemeManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AjouterEntrepriseController {

    @FXML private TextField tfNom;
    @FXML private TextField tfAdresse;
    @FXML private TextField tfVille;
    @FXML private TextField tfTelephone;
    @FXML private TextField tfEmail;
    @FXML private TextArea taDescription;

    @FXML private ImageView ivLogo;
    @FXML private Label lblLogoPlaceholder;

    @FXML private Label errLogo;
    @FXML private Label errNom;
    @FXML private Label errAdresse;
    @FXML private Label errVille;
    @FXML private Label errTelephone;
    @FXML private Label errEmail;
    @FXML private Label errDescription;

    @FXML private Button btnSave;

    private final EntrepriseService entrepriseService = new EntrepriseService();

    private String logoValue;

    private final Random random = new Random();

    @FXML
    private void initialize() {
        if (Session.getCurrentUser() != null) {
            Entreprise e = entrepriseService.getByUserId(Session.getCurrentUser().getId());
            if (e != null) {
                if (tfNom != null) tfNom.setText(e.getNomEntreprise());
                if (tfAdresse != null) tfAdresse.setText(e.getAdresse());
                if (tfVille != null) tfVille.setText(e.getVille());
                if (tfTelephone != null) tfTelephone.setText(e.getTelephone());
                if (tfEmail != null) tfEmail.setText(e.getEmailContact());
                if (taDescription != null) taDescription.setText(e.getDescription());
                logoValue = safe(e.getLogo());
                refreshLogoPreview();
            }
        }

        if (tfNom != null) tfNom.textProperty().addListener((obs, o, n) -> clearInlineError(tfNom, errNom));
        if (tfAdresse != null) tfAdresse.textProperty().addListener((obs, o, n) -> clearInlineError(tfAdresse, errAdresse));
        if (tfVille != null) tfVille.textProperty().addListener((obs, o, n) -> clearInlineError(tfVille, errVille));
        if (tfTelephone != null) tfTelephone.textProperty().addListener((obs, o, n) -> clearInlineError(tfTelephone, errTelephone));
        if (tfEmail != null) tfEmail.textProperty().addListener((obs, o, n) -> clearInlineError(tfEmail, errEmail));
        if (taDescription != null) taDescription.textProperty().addListener((obs, o, n) -> clearInlineError(taDescription, errDescription));
    }

    private boolean validateEntrepriseFormAndShowErrors(String nom, String adresse, String ville, String telephone, String email, String description) {
        boolean ok = true;

        clearInlineError(tfNom, errNom);
        clearInlineError(tfAdresse, errAdresse);
        clearInlineError(tfVille, errVille);
        clearInlineError(tfTelephone, errTelephone);
        clearInlineError(tfEmail, errEmail);
        clearInlineError(taDescription, errDescription);

        if (nom == null || nom.isBlank()) {
            showInlineError(tfNom, errNom, "Nom obligatoire");
            ok = false;
        } else {
            if (nom.length() < 2) {
                showInlineError(tfNom, errNom, "Minimum 2 caractères");
                ok = false;
            } else if (nom.length() > 80) {
                showInlineError(tfNom, errNom, "Maximum 80 caractères");
                ok = false;
            }
        }

        if (adresse == null || adresse.isBlank()) {
            showInlineError(tfAdresse, errAdresse, "Adresse obligatoire");
            ok = false;
        }

        if (ville == null || ville.isBlank()) {
            showInlineError(tfVille, errVille, "Ville obligatoire");
            ok = false;
        } else if (!ville.matches("[A-Za-zÀ-ÿ' -]+")) {
            showInlineError(tfVille, errVille, "Caractères invalides");
            ok = false;
        }

        if (telephone == null || telephone.isBlank()) {
            showInlineError(tfTelephone, errTelephone, "Téléphone obligatoire");
            ok = false;
        } else {
            String normalized = telephone.replaceAll("\\s+", "");
            if (!normalized.matches("[0-9+]+")) {
                showInlineError(tfTelephone, errTelephone, "Chiffres seulement (+ autorisé)");
                ok = false;
            } else if (normalized.length() < 8 || normalized.length() > 15) {
                showInlineError(tfTelephone, errTelephone, "Longueur invalide");
                ok = false;
            }
        }

        if (email == null || email.isBlank()) {
            showInlineError(tfEmail, errEmail, "Email obligatoire");
            ok = false;
        } else if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showInlineError(tfEmail, errEmail, "Email invalide");
            ok = false;
        }

        if (description == null || description.isBlank()) {
            showInlineError(taDescription, errDescription, "Description obligatoire");
            ok = false;
        } else if (description.length() < 10) {
            showInlineError(taDescription, errDescription, "Minimum 10 caractères");
            ok = false;
        } else if (description.length() > 500) {
            showInlineError(taDescription, errDescription, "Maximum 500 caractères");
            ok = false;
        }

        return ok;
    }

    private void showInlineError(javafx.scene.control.Control control, Label label, String message) {
        if (control != null && !control.getStyleClass().contains("error")) {
            control.getStyleClass().add("error");
        }
        if (label != null) {
            label.setText(message == null ? "" : message);
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void clearInlineError(javafx.scene.control.Control control, Label label) {
        if (control != null) {
            control.getStyleClass().remove("error");
        }
        if (label != null) {
            label.setText("");
            label.setVisible(false);
            label.setManaged(false);
        }
    }

    @FXML
    private void chooseLogo() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choisir un logo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            Stage stage = (Stage) btnSave.getScene().getWindow();
            File file = chooser.showOpenDialog(stage);
            if (file == null) return;

            logoValue = file.getAbsolutePath();
            refreshLogoPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible de choisir le logo.").show();
        }
    }

    @FXML
    private void generateLogo() {
        String name = tfNom == null ? "" : safe(tfNom.getText());
        if (name.isBlank()) {
            new Alert(Alert.AlertType.INFORMATION, "Saisissez d'abord le nom de l'entreprise.").show();
            return;
        }

        String ideogramKey = System.getenv("IDEOGRAM_API_KEY");
        if (ideogramKey != null && !ideogramKey.isBlank()) {
            try {
                String maybePath = generateProfessionalLogoWithIdeogram(ideogramKey, name);
                if (maybePath != null && !maybePath.isBlank()) {
                    logoValue = maybePath;
                    refreshLogoPreview();
                    clearInlineError(null, errLogo);
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String maybePath = generateProfessionalLogoWithOpenAI(apiKey, name);
                if (maybePath != null && !maybePath.isBlank()) {
                    logoValue = maybePath;
                    refreshLogoPreview();
                    clearInlineError(null, errLogo);
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String[] styles = {"shapes", "lorelei", "icons", "bottts-neutral"};
        String style = styles[random.nextInt(styles.length)];
        String seed = URLEncoder.encode(name + "-" + (1000 + random.nextInt(9000)), StandardCharsets.UTF_8);

        String[] gradients = {"b6e3f4", "c0aede", "d1d4f9", "ffd5dc", "f1f4dc"};
        String bg = gradients[random.nextInt(gradients.length)];

        logoValue = "https://api.dicebear.com/7.x/" + style + "/png"
                + "?seed=" + seed
                + "&backgroundType=gradientLinear"
                + "&backgroundColor=" + bg
                + "&radius=16";
        refreshLogoPreview();
    }

    private String generateProfessionalLogoWithIdeogram(String apiKey, String entrepriseName) throws IOException, InterruptedException {
        String prompt = "Professional corporate company logo for '" + entrepriseName + "'. "
                + "Make a clean brandmark + wordmark with the exact company name readable. "
                + "Premium minimal vector style, high contrast, modern typography, centered. "
                + "No cartoon, no mascot, no 3D, no mockup, no extra words.";

        String boundary = "----IdeogramBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary,
                new String[][]{
                        {"prompt", prompt},
                        {"rendering_speed", "TURBO"}
                }
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ideogram.ai/v1/ideogram-v3/generate"))
                .header("Api-Key", apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return null;
        }

        String url = extractFirstUrl(resp.body());
        if (url == null || url.isBlank()) {
            return null;
        }

        HttpRequest dl = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<byte[]> imgResp = client.send(dl, HttpResponse.BodyHandlers.ofByteArray());
        if (imgResp.statusCode() < 200 || imgResp.statusCode() >= 300) {
            return null;
        }

        Path out = ensureLogoOutputPath(entrepriseName);
        Files.write(out, imgResp.body());
        return out.toAbsolutePath().toString();
    }

    private byte[] buildMultipartBody(String boundary, String[][] fields) {
        StringBuilder sb = new StringBuilder();
        for (String[] f : fields) {
            String name = f[0];
            String value = f[1];
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
            sb.append(value == null ? "" : value).append("\r\n");
        }
        sb.append("--").append(boundary).append("--\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String extractFirstUrl(String json) {
        if (json == null) {
            return null;
        }
        Pattern p = Pattern.compile("\\\"url\\\"\\s*:\\s*\\\"(https?:\\\\/\\\\/.*?)(?=\\\")\\\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("\\/", "/");
    }

    private String generateProfessionalLogoWithOpenAI(String apiKey, String entrepriseName) throws IOException, InterruptedException {
        String prompt = "Create a PROFESSIONAL COMPANY LOGO with the exact company name '" + entrepriseName + "' written in the logo. "
                + "Style: premium corporate branding, clean modern vector/flat design. "
                + "Composition: icon/brandmark on the left or above + WORDMARK text (company name) aligned and clearly readable. "
                + "Typography: elegant sans-serif, high legibility, balanced spacing. "
                + "Colors: 2 to 3 colors max, corporate palette (navy/blue/teal/white). "
                + "Hard rules: NO cartoons, NO mascots, NO faces/characters, NO emojis, NO clipart, NO 3D, NO busy details, NO extra words/slogans. "
                + "Output: centered logo, high contrast, crisp edges, suitable for a real business website and mobile app.";

        String payload = "{"
                + "\"model\":\"gpt-image-1\","
                + "\"prompt\":" + jsonString(prompt) + ","
                + "\"size\":\"1024x1024\","
                + "\"background\":\"transparent\","
                + "\"response_format\":\"b64_json\""
                + "}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/images/generations"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            return null;
        }

        String b64 = extractFirstB64Json(resp.body());
        if (b64 == null || b64.isBlank()) {
            return null;
        }

        byte[] bytes = java.util.Base64.getDecoder().decode(b64);
        Path out = ensureLogoOutputPath(entrepriseName);
        Files.write(out, bytes);
        return out.toAbsolutePath().toString();
    }

    private Path ensureLogoOutputPath(String entrepriseName) throws IOException {
        Path dir = Paths.get(System.getProperty("user.home"), "app-logos");
        Files.createDirectories(dir);
        String safeName = entrepriseName == null ? "entreprise" : entrepriseName.replaceAll("[^A-Za-z0-9_-]+", "_");
        String filename = safeName + "_" + System.currentTimeMillis() + ".png";
        return dir.resolve(filename);
    }

    private String extractFirstB64Json(String json) {
        if (json == null) {
            return null;
        }
        Pattern p = Pattern.compile("\\\"b64_json\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("\\n", "");
    }

    private String jsonString(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private void refreshLogoPreview() {
        if (ivLogo == null) return;
        String v = safe(logoValue);
        if (v.isBlank()) {
            ivLogo.setImage(null);
            return;
        }
        try {
            Image img;
            if (v.startsWith("http://") || v.startsWith("https://")) {
                img = new Image(v, true);
            } else {
                img = new Image(new File(v).toURI().toString(), true);
            }
            ivLogo.setImage(img);
        } catch (Exception ignored) {
            ivLogo.setImage(null);
        }
    }

    @FXML
    private void save() {
        if (Session.getCurrentUser() == null) {
            new Alert(Alert.AlertType.ERROR, "Utilisateur non connecté.").show();
            return;
        }

        String nom = tfNom == null ? "" : safe(tfNom.getText());

        String adresse = tfAdresse == null ? "" : safe(tfAdresse.getText());
        String ville = tfVille == null ? "" : safe(tfVille.getText());
        String telephone = tfTelephone == null ? "" : safe(tfTelephone.getText());
        String email = tfEmail == null ? "" : safe(tfEmail.getText());
        String description = taDescription == null ? "" : safe(taDescription.getText());

        if (!validateEntrepriseFormAndShowErrors(nom, adresse, ville, telephone, email, description)) {
            return;
        }

        Entreprise e = new Entreprise();
        e.setIdUser(Session.getCurrentUser().getId());
        e.setNomEntreprise(nom);
        e.setLogo(safe(logoValue));
        e.setAdresse(adresse);
        e.setVille(ville);
        e.setTelephone(telephone);
        e.setEmailContact(email);
        e.setDescription(description);

        try {
            Entreprise existingForUser = entrepriseService.getByUserId(e.getIdUser());
            Integer excludeId = existingForUser != null ? existingForUser.getIdEntreprise() : null;
            if (entrepriseService.existsDuplicate(e, excludeId)) {
                showInlineError(tfNom, errNom, "Entreprise identique existe déjà");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la vérification de l'entreprise.").show();
            return;
        }

        try {
            entrepriseService.saveForUser(e);
            openDashboardAdmin();
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement de l'entreprise.").show();
        }
    }

    @FXML
    private void cancel() {
        // Retourner au dashboard
        openDashboardAdmin();
    }

    private void openDashboardAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboardfxml/Dashboard_Admin.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
