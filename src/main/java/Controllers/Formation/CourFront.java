package Controllers.Formation;

import Entites.Formation.Cour;
import Entites.Formation.Formation;
import Services.Formation.CourServices;
import Services.Formation.FormationServices;
import Services.Formation.FormationPurchaseService;
import Services.Formation.PredictiveCourseAnalyzer;
import Utils.UiState;
import Utils.Session;
import Utils.WindowUtil;
import Entities.User.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class CourFront {

    @FXML
    private Button btnRetour;

    @FXML
    private Label lblTitre;

    @FXML
    private FlowPane cardsFlow;

    @FXML
    private FlowPane chipsFlow;

    @FXML
    private TextField tfSearchChap;

    @FXML
    private Button btnSortDuree;

    private boolean sortDureeEnabled = false;

    private final ToggleGroup chipsGroup = new ToggleGroup();
    private boolean applyingChipSelection = false;

    private final CourServices courServices = new CourServices();
    private final FormationServices formationServices = new FormationServices();
    private final FormationPurchaseService purchaseService = new FormationPurchaseService();
    private final PredictiveCourseAnalyzer courseAnalyzer = new PredictiveCourseAnalyzer(); // added instance

    private String coverSlug;

    @FXML
    void initialize() {
        if (btnRetour != null) {
            btnRetour.setOnAction(e -> retour());
        }

        if (tfSearchChap != null) {
            tfSearchChap.textProperty().addListener((obs, oldV, newV) -> refreshCards());
        }

        if (btnSortDuree != null) {
            btnSortDuree.setFocusTraversable(false);
            btnSortDuree.setOnAction(e -> {
                sortDureeEnabled = true;
                refreshCards();
            });
        }

        String titre = UiState.selectedFormationTitre;
        if (lblTitre != null) {
            if (titre != null && !titre.isBlank()) {
                lblTitre.setText("Cours - " + titre);
            } else {
                lblTitre.setText("Cours");
            }
        }

        refreshCards();
    }

    private void buildOrigineChips(List<Cour> allCours, String currentQuery) {
        if (chipsFlow == null) {
            return;
        }

        Set<String> origines = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (allCours != null) {
            for (Cour c : allCours) {
                if (c == null) {
                    continue;
                }
                String origine = c.getOrigine();
                if (origine != null && !origine.isBlank()) {
                    origines.add(origine.trim());
                }
            }
        }

        chipsFlow.getChildren().clear();

        ToggleButton allBtn = new ToggleButton("Tous");
        allBtn.getStyleClass().add("chip");
        allBtn.setToggleGroup(chipsGroup);
        allBtn.setOnAction(e -> applyChipToSearch(""));
        chipsFlow.getChildren().add(allBtn);

        ToggleButton toSelect = null;
        String q = currentQuery == null ? "" : currentQuery.trim().toLowerCase();

        for (String origine : origines) {
            ToggleButton b = new ToggleButton(origine);
            b.getStyleClass().add("chip");
            b.setToggleGroup(chipsGroup);
            b.setOnAction(e -> applyChipToSearch(origine));
            chipsFlow.getChildren().add(b);

            if (!q.isEmpty() && origine.toLowerCase().contains(q)) {
                toSelect = b;
            }
        }

        if (toSelect != null) {
            toSelect.setSelected(true);
        } else {
            allBtn.setSelected(true);
        }
    }

    private void applyChipToSearch(String value) {
        if (tfSearchChap == null) {
            return;
        }
        applyingChipSelection = true;
        try {
            tfSearchChap.setText(value == null ? "" : value);
        } finally {
            applyingChipSelection = false;
        }
        refreshCards();
    }

    private void refreshCards() {
        if (cardsFlow == null) {
            return;
        }
        if (UiState.selectedFormationId == null) {
            cardsFlow.getChildren().clear();
            return;
        }

        Formation selected = formationServices.getFormationById(UiState.selectedFormationId);
        if (selected != null && selected.isPayante() && isCandidatWithoutAccess(selected)) {
            if (!showPayDialogAndPurchase(selected)) {
                retour();
                return;
            }
        }

        if (coverSlug == null) {
            coverSlug = slugify(selected == null ? null : selected.getDomaine());
        }

        String q = tfSearchChap == null ? "" : tfSearchChap.getText();
        String query = (q == null) ? "" : q.trim().toLowerCase();

        List<Cour> all = courServices.afficherCoursParFormation(UiState.selectedFormationId);
        buildOrigineChips(all, q);

        ObservableList<Cour> cours = FXCollections.observableList(all);
        if (!query.isEmpty()) {
            cours = cours.filtered(c -> {
                String origine = c.getOrigine();
                return origine != null && origine.toLowerCase().contains(query);
            });
        }

        if (sortDureeEnabled) {
            cours.sort((a, b) -> {
                float da = (a == null) ? Float.NaN : a.getDure();
                float db = (b == null) ? Float.NaN : b.getDure();
                if (Float.isNaN(da) && Float.isNaN(db)) {
                    return 0;
                }
                if (Float.isNaN(da)) {
                    return 1;
                }
                if (Float.isNaN(db)) {
                    return -1;
                }
                return Float.compare(da, db);
            });
        }

        cardsFlow.getChildren().clear();
        for (Cour c : cours) {
            cardsFlow.getChildren().add(createCard(c));
        }
    }

    private boolean isCandidatWithoutAccess(Formation f) {
        User current = Session.getCurrentUser();
        if (current == null) {
            return true;
        }
        String role = current.getRole();
        if (role == null || !role.equalsIgnoreCase("CANDIDAT")) {
            return false;
        }
        if (f == null || !f.isPayante()) {
            return false;
        }
        return !purchaseService.hasPurchased(current.getId(), f.getId());
    }

    private boolean showPayDialogAndPurchase(Formation f) {
        User current = Session.getCurrentUser();
        if (current == null) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Accès restreint");
            a.setHeaderText("Formation payante");
            a.setContentText("Veuillez vous connecter pour accéder à cette formation.");
            a.showAndWait();
            return false;
        }

        Alert pay = new Alert(Alert.AlertType.CONFIRMATION);
        pay.setTitle("Paiement");
        pay.setHeaderText("Cette formation est payante");
        pay.setContentText("Prix: " + f.getPrix() + " DT\nVoulez-vous payer maintenant ?");

        ButtonType btnPayer = new ButtonType("Payer");
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pay.getButtonTypes().setAll(btnPayer, btnAnnuler);

        ButtonType res = pay.showAndWait().orElse(btnAnnuler);
        if (res != btnPayer) {
            return false;
        }

        String promoCode = askPromoCodeIfAny(current.getId());
        return openStripeCheckoutAndHandleResult(current.getId(), f, promoCode);
    }

    private boolean openStripeCheckoutAndHandleResult(int userId, Formation f, String promoCode) {
        try {
            String checkoutUrl = purchaseService.createCheckoutSessionUrl(
                f.getTitre(),
                f.getPrix(),
                f.getId(),
                userId,
                promoCode
            );

            javafx.scene.web.WebView webView = new javafx.scene.web.WebView();
            javafx.scene.web.WebEngine engine = webView.getEngine();

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Paiement Stripe");
            javafx.scene.Scene scene = new javafx.scene.Scene(new VBox(webView), 980, 720);
            stage.setScene(scene);

            final boolean[] success = {false};
            engine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                if (newLoc == null) {
                    return;
                }
                if (newLoc.startsWith(purchaseService.getSuccessUrlBase())) {
                    try {
                        String sessionId = getQueryParam(newLoc, "session_id");
                        if (sessionId != null && !sessionId.isBlank()) {
                            try {
                                success[0] = purchaseService.verifyCheckoutSessionPaidAndRecordPurchase(sessionId, userId, f.getId());
                            } catch (Exception e) {
                                success[0] = false;
                            }
                        } else {
                            purchaseService.purchase(userId, f.getId());
                            success[0] = true;
                        }
                    } finally {
                        stage.close();
                    }
                } else if (newLoc.startsWith(purchaseService.getCancelUrlBase())) {
                    stage.close();
                }
            });

            engine.load(checkoutUrl);
            stage.showAndWait();

            if (success[0]) {
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Paiement");
                ok.setHeaderText(null);
                ok.setContentText("Paiement effectué avec succès.");
                ok.showAndWait();
            }

            return success[0];
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Paiement");
            err.setHeaderText("Échec du paiement");
            err.setContentText(e.getMessage());
            err.showAndWait();
            return false;
        }
    }

    private String askPromoCodeIfAny(int userId) {
        while (true) {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Code promo");
            d.setHeaderText(null);

            ButtonType btnOk = new ButtonType("Appliquer", ButtonBar.ButtonData.OK_DONE);
            ButtonType btnSkip = new ButtonType("Continuer sans code", ButtonBar.ButtonData.CANCEL_CLOSE);
            d.getDialogPane().getButtonTypes().setAll(btnSkip, btnOk);

            VBox root = new VBox(10);
            root.setStyle("-fx-padding: 18; -fx-background-color: white;");

            Label title = new Label("Code promo");
            title.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #111827;");

            Label hint = new Label("Saisis ton code promo pour appliquer une réduction.");
            hint.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            TextField tf = new TextField();
            tf.setPromptText("Ex: CL-XXXXXXX");
            tf.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-color: #e5e7eb; -fx-padding: 10 12; -fx-font-size: 13px;");

            VBox card = new VBox(8, hint, tf);
            card.setStyle("-fx-padding: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #f9fafb;");

            root.getChildren().addAll(title, card);
            d.getDialogPane().setContent(root);
            d.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0; -fx-border-color: #e5e7eb; -fx-border-radius: 14; -fx-background-radius: 14;");

            Button okBtn = (Button) d.getDialogPane().lookupButton(btnOk);
            okBtn.setDefaultButton(true);
            okBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 8 16;");

            Button skipBtn = (Button) d.getDialogPane().lookupButton(btnSkip);
            skipBtn.setCancelButton(true);
            skipBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #111827; -fx-font-weight: 800; -fx-background-radius: 10; -fx-padding: 8 16; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

            ButtonType res = d.showAndWait().orElse(btnSkip);
            if (res != btnOk) {
                return "";
            }

            String code = tf.getText() == null ? "" : tf.getText().trim();
            if (code.isEmpty()) {
                return "";
            }

            FormationPurchaseService.PromoCodeStatus st = purchaseService.getPromoCodeStatusForUser(userId, code);
            if (st == FormationPurchaseService.PromoCodeStatus.VALID) {
                return code;
            }

            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Code promo");
            a.setHeaderText(null);
            if (st == FormationPurchaseService.PromoCodeStatus.USED) {
                a.setContentText("Ce code promo n'est plus valable car il a déjà été utilisé.");
            } else {
                a.setContentText("Code promo invalide.");
            }
            a.showAndWait();
        }
    }

    private static String extractSessionId(String input) {
        if (input == null) {
            return null;
        }
        String v = input.trim();
        if (v.isEmpty()) {
            return null;
        }
        if (v.startsWith("cs_")) {
            return v;
        }
        String fromUrl = getQueryParam(v, "session_id");
        if (fromUrl != null && !fromUrl.isBlank()) {
            return fromUrl;
        }
        return null;
    }

    private static String getQueryParam(String url, String key) {
        if (url == null || key == null || key.isBlank()) {
            return null;
        }
        int q = url.indexOf('?');
        if (q < 0 || q >= url.length() - 1) {
            return null;
        }
        String query = url.substring(q + 1);
        String[] pairs = query.split("&");
        for (String p : pairs) {
            int eq = p.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String k = p.substring(0, eq);
            if (!k.equals(key)) {
                continue;
            }
            String val = p.substring(eq + 1);
            return URLDecoder.decode(val, StandardCharsets.UTF_8);
        }
        return null;
    }

    private VBox createCard(Cour c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("formation-card");

        Region cover = new Region();
        cover.getStyleClass().add("formation-cover");
        applyDomainCover(cover);

        String origineText = (c.getOrigine() == null || c.getOrigine().isBlank()) ? "(Sans origine)" : c.getOrigine();
        Label code = new Label(origineText);
        code.getStyleClass().add("formation-code");

        String titleText = (c.getNomFormateur() == null || c.getNomFormateur().isBlank()) ? "Cours" : c.getNomFormateur();
        Label title = new Label(titleText);
        title.getStyleClass().add("formation-title");

        Label status = new Label("Complexité: " + c.getComplexite());
        status.getStyleClass().add("formation-status");

        VBox body = new VBox(6);
        body.getStyleClass().add("formation-body");
        body.getChildren().addAll(code, title, status);

        String description = (c.getDescription() == null) ? "" : c.getDescription();
        if (!description.isBlank()) {
            Label desc = new Label(description);
            desc.getStyleClass().add("formation-meta");
            desc.setWrapText(true);
            body.getChildren().add(desc);
        }

        String origine = (c.getOrigine() == null) ? "" : c.getOrigine();
        if (!origine.isBlank()) {
            Label origineLbl = new Label("Origine: " + origine);
            origineLbl.getStyleClass().add("formation-meta");
            origineLbl.setWrapText(true);
            body.getChildren().add(origineLbl);
        }

        Region divider = new Region();
        divider.getStyleClass().add("formation-divider");

        String nomFormateur = (c.getNomFormateur() == null) ? "" : c.getNomFormateur();
        String metaText = "";
        if (!nomFormateur.isBlank()) {
            metaText += nomFormateur;
        }
        if (!metaText.isBlank()) {
            metaText += " • ";
        }
        metaText += "Durée: " + c.getDure();
        Label meta = new Label(metaText);
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnChapitres = new Button("Chapitres");
        btnChapitres.getStyleClass().add("btn-ghost");
        btnChapitres.setFocusTraversable(false);
        btnChapitres.setOnAction(e -> {
            UiState.selectedCourId = (c == null) ? null : c.getId();
            UiState.selectedCourTitre = (c == null) ? null : c.getNomFormateur();
            WindowUtil.navigate(btnChapitres, "/Formation/ChapitreFront.fxml", "Chapitres");
        });

        Button btnAnalyser = new Button("Analyser");
        btnAnalyser.getStyleClass().add("btn-secondary");
        btnAnalyser.setFocusTraversable(false);
        btnAnalyser.setOnAction(e -> showPrediction(c));

        HBox footer = new HBox(10, meta, spacer, btnChapitres, btnAnalyser);
        footer.getStyleClass().add("formation-footer");

        card.getChildren().addAll(cover, body, divider, footer);
        return card;
    }

    private void showPrediction(Cour c) {
        String prediction = courseAnalyzer.getComplexityPrediction(c);
        String suggestions = courseAnalyzer.getSuggestions(c);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Analyse prédictive du cours");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.getStyleClass().add("modern-dialog");

        java.net.URL css = getClass().getResource("/Formation/app.css");
        if (css != null) {
            pane.getStylesheets().add(css.toExternalForm());
        }

        String headerText = (c == null || c.getNomFormateur() == null) ? "" : c.getNomFormateur();
        Label title = new Label("Prédiction de difficulté");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label(headerText);
        subtitle.getStyleClass().add("dialog-subtitle");
        VBox header = new VBox(2, title, subtitle);
        header.getStyleClass().add("dialog-header");
        pane.setHeader(header);

        Label predLabel = new Label("Résultat : " + prediction);
        predLabel.getStyleClass().add("dialog-textarea");
        Label suggLabel = new Label("Suggestions :\n" + suggestions);
        suggLabel.getStyleClass().add("dialog-textarea");
        suggLabel.setWrapText(true);

        VBox content = new VBox(10, predLabel, suggLabel);
        pane.setContent(content);
        pane.setPrefWidth(500);

        dialog.showAndWait();
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/FormationFront.fxml", "Formations");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String slugify(String s) {
        if (s == null) {
            return "";
        }
        String v = s.trim().toLowerCase();
        if (v.isEmpty()) {
            return "";
        }
        v = Normalizer.normalize(v, Normalizer.Form.NFD);
        v = v.replaceAll("\\p{M}", "");
        v = v.replaceAll("[^a-z0-9]+", "-");
        v = v.replaceAll("(^-+)|(-+$)", "");
        return v;
    }

    private void applyDomainCover(Region cover) {
        if (cover == null) {
            return;
        }
        if (coverSlug == null || coverSlug.isEmpty()) {
            return;
        }

        java.net.URL url = getClass().getResource("/images/domains/" + coverSlug + ".png");
        if (url == null) {
            url = getClass().getResource("/images/domains/" + coverSlug + ".jpg");
        }
        if (url == null) {
            url = getClass().getResource("/images/domains/" + coverSlug + ".jpeg");
        }
        if (url == null) {
            return;
        }

        String cssUrl = url.toExternalForm();
        cover.setStyle(
                "-fx-background-image: url('" + cssUrl + "');" +
                        "-fx-background-size: cover;" +
                        "-fx-background-position: center center;" +
                        "-fx-background-repeat: no-repeat;"
        );
    }
}
