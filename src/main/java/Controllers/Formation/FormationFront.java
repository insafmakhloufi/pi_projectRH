package Controllers.Formation;

import Entities.Formation.Formation;
import Services.Formation.FavoritesService;
import Services.Formation.FormationServices;
import Services.Formation.FormationPurchaseService;
import Services.Formation.PredictiveFormationAnalyzer;
import Utils.UiState;
import Utils.Session;
import Utils.WindowUtil;
import Entities.User;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.Scene;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.*;
import java.util.Locale; // added import statement

public class FormationFront {
    @FXML
    private ScrollPane cardsScroll;

    @FXML
    private FlowPane cardsFlow;

    @FXML
    private FlowPane chipsFlow;

    @FXML
    private TextField tfSearchDomaine;

    @FXML
    private Button btnSortDate;

    @FXML
    private Button btnFavorites;

    @FXML
    private PieChart chartFormationsByDate;

    private boolean sortDateEnabled = false;

    private final ToggleGroup chipsGroup = new ToggleGroup();
    private boolean applyingChipSelection = false;

    private final FormationServices fs = new FormationServices();
    private final FormationPurchaseService purchaseService = new FormationPurchaseService();
    private final PredictiveFormationAnalyzer analyzer = new PredictiveFormationAnalyzer();
    private final FavoritesService favoritesService = new FavoritesService();

    private boolean favoritesOnlyEnabled = false;
    private Set<Integer> favoriteIdsCache = null;

    @FXML
    void initialize() {
        if (cardsScroll != null && cardsFlow != null) {
            cardsScroll.viewportBoundsProperty().addListener((obs, oldB, newB) -> {
                if (newB != null) {
                    cardsFlow.setPrefWrapLength(Math.max(0, newB.getWidth()));
                }
            });
            Platform.runLater(() -> {
                Bounds vb = cardsScroll.getViewportBounds();
                if (vb != null) {
                    cardsFlow.setPrefWrapLength(Math.max(0, vb.getWidth()));
                }
                cardsFlow.applyCss();
                cardsFlow.layout();
            });
        }

        refreshCards();

        if (chartFormationsByDate != null) {
            chartFormationsByDate.setVisible(false);
            chartFormationsByDate.setManaged(false);
        }

        if (tfSearchDomaine != null) {
            tfSearchDomaine.textProperty().addListener((obs, oldV, newV) -> refreshCards());
        }

        if (btnSortDate != null) {
            btnSortDate.setFocusTraversable(false);
            btnSortDate.setOnAction(e -> {
                sortDateEnabled = !sortDateEnabled;
                refreshCards();
            });
        }

        if (btnFavorites != null) {
            btnFavorites.setFocusTraversable(false);
            btnFavorites.setOnAction(e -> {
                favoritesOnlyEnabled = !favoritesOnlyEnabled;
                favoriteIdsCache = null;
                if (btnFavorites.getStyleClass().contains("fav-filter-active")) {
                    btnFavorites.getStyleClass().remove("fav-filter-active");
                }
                if (favoritesOnlyEnabled) {
                    btnFavorites.getStyleClass().add("fav-filter-active");
                }
                refreshCards();
            });
        }
    }

    private void buildDomainChips(List<Formation> allFormations) {
        if (chipsFlow == null) {
            return;
        }

        Set<String> domains = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (allFormations != null) {
            for (Formation f : allFormations) {
                if (f == null) {
                    continue;
                }
                String dom = f.getDomaine();
                if (dom != null && !dom.isBlank()) {
                    domains.add(dom.trim());
                }
            }
        }

        chipsFlow.getChildren().clear();

        ToggleButton allBtn = new ToggleButton("Tous");
        allBtn.getStyleClass().add("chip");
        allBtn.setToggleGroup(chipsGroup);
        allBtn.setSelected(true);
        allBtn.setOnAction(e -> applyChipToSearch(""));
        chipsFlow.getChildren().add(allBtn);

        for (String dom : domains) {
            ToggleButton b = new ToggleButton(dom);
            b.getStyleClass().add("chip");
            b.setToggleGroup(chipsGroup);
            b.setOnAction(e -> applyChipToSearch(dom));
            chipsFlow.getChildren().add(b);
        }
    }

    private void applyChipToSearch(String value) {
        if (tfSearchDomaine == null) {
            return;
        }
        applyingChipSelection = true;
        try {
            tfSearchDomaine.setText(value == null ? "" : value);
        } finally {
            applyingChipSelection = false;
        }
        refreshCards();
    }

    private void refreshCards() {
        List<Formation> all = fs.afficherFormations();
        if (all == null) {
            all = java.util.Collections.emptyList();
        }
        buildDomainChips(all);

        String q = tfSearchDomaine == null ? "" : tfSearchDomaine.getText();
        String query = (q == null) ? "" : q.trim().toLowerCase();

        ObservableList<Formation> observableList = FXCollections.observableList(all);
        if (!query.isEmpty()) {
            observableList = observableList.filtered(f -> {
                String dom = f.getDomaine();
                return dom != null && dom.toLowerCase().contains(query);
            });
        }

        if (favoritesOnlyEnabled) {
            User current = Session.getCurrentUser();
            if (current == null) {
                favoritesOnlyEnabled = false;
                if (btnFavorites != null) {
                    btnFavorites.getStyleClass().remove("fav-filter-active");
                }
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Favoris");
                a.setHeaderText(null);
                a.setContentText("Veuillez vous connecter pour accéder aux favoris.");
                a.showAndWait();
            } else {
                if (favoriteIdsCache == null) {
                    favoriteIdsCache = favoritesService.listFavoriteFormationIds(current.getId());
                }
                Set<Integer> ids = favoriteIdsCache;
                observableList = observableList.filtered(f -> f != null && ids.contains(f.getId()));
            }
        }

        if (sortDateEnabled) {
            observableList.sort((a, b) -> {
                java.sql.Date da = (a == null) ? null : a.getDateDebut();
                java.sql.Date db = (b == null) ? null : b.getDateDebut();
                if (da == null && db == null) {
                    return 0;
                }
                if (da == null) {
                    return 1;
                }
                if (db == null) {
                    return -1;
                }
                return da.compareTo(db);
            });
        }

        cardsFlow.getChildren().clear();
        for (Formation f : observableList) {
            cardsFlow.getChildren().add(createCard(f));
        }
    }

    private void updateStatsByDate(ObservableList<Formation> formations) {
        if (chartFormationsByDate == null) {
            return;
        }

        Map<LocalDate, Integer> counts = new TreeMap<>();
        if (formations != null) {
            for (Formation f : formations) {
                java.sql.Date d = (f == null) ? null : f.getDateDebut();
                if (d == null) {
                    continue;
                }
                LocalDate ld = d.toLocalDate();
                counts.put(ld, counts.getOrDefault(ld, 0) + 1);
            }
        }

        int total = 0;
        for (int v : counts.values()) {
            total += v;
        }

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (Map.Entry<LocalDate, Integer> e : counts.entrySet()) {
            String label = String.valueOf(e.getKey());
            data.add(new PieChart.Data(label, e.getValue()));
        }

        chartFormationsByDate.setData(data);

        final int totalFinal = total;
        for (PieChart.Data d : data) {
            d.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode == null) {
                    return;
                }
                double value = d.getPieValue();
                double pct = (totalFinal <= 0) ? 0.0 : (value * 100.0 / totalFinal);
                Tooltip tip = new Tooltip(String.format(Locale.US, "%.0f formations (%.1f%%)", value, pct));
                Tooltip.install(newNode, tip);
            });
        }
    }

    private VBox createCard(Formation f) {
        VBox card = new VBox(10);
        card.getStyleClass().add("formation-card");

        User current = Session.getCurrentUser();
        boolean isCandidate = current != null && current.getRole() != null && current.getRole().equalsIgnoreCase("CANDIDAT");
        boolean isLocked = f != null && f.isPayante() && (!isCandidate || !purchaseService.hasPurchased(current.getId(), f.getId()));
        boolean isPurchased = isCandidate && !isLocked && f != null && f.isPayante();

        if (isLocked) {
            card.getStyleClass().add("formation-locked");
        }

        Region cover = new Region();
        cover.getStyleClass().add("formation-cover");
        applyDomainCover(cover, f.getDomaine());

        Label code = new Label(f.getDomaine());
        code.getStyleClass().add("formation-code");

        Label title = new Label(f.getTitre());
        title.getStyleClass().add("formation-title");

        String statusText;
        if (f.isPayante()) {
            if (isLocked) {
                statusText = "Fermé • Payante • " + f.getPrix() + " DT";
            } else if (isPurchased) {
                statusText = "Ouvert • Payée";
            } else {
                statusText = "Ouvert • Payante • " + f.getPrix() + " DT";
            }
        } else {
            statusText = "Ouvert • FREE";
        }

        Label status = new Label(statusText);
        status.getStyleClass().add("formation-status");

        Label accessBadge = null;
        if (isLocked) {
            accessBadge = new Label("Fermé");
            accessBadge.getStyleClass().add("formation-badge-locked");
        } else if (isPurchased) {
            accessBadge = new Label("Payée");
            accessBadge.getStyleClass().add("formation-badge-paid");
        } else if (!f.isPayante()) {
            accessBadge = new Label("FREE");
            accessBadge.getStyleClass().add("formation-badge-paid");
        }

        Label prediction = new Label("Prédiction: " + analyzer.getSuccessPrediction(f));
        prediction.getStyleClass().add("formation-prediction");
        prediction.setWrapText(true);
        prediction.setMaxWidth(240);
        prediction.setVisible(true);
        prediction.setManaged(true);

        Tooltip tip = new Tooltip(analyzer.getSuggestions(f));
        Tooltip.install(prediction, tip);

        VBox body = new VBox(6);
        body.getStyleClass().add("formation-body");

        if (accessBadge != null) {
            HBox statusRow = new HBox(8);
            statusRow.getChildren().addAll(status, accessBadge);
            body.getChildren().addAll(code, title, statusRow, prediction);
        } else {
            body.getChildren().addAll(code, title, status, prediction);
        }

        card.setOnMouseClicked(e -> {
            ouvrirCours(f);
        });

        Region divider = new Region();
        divider.getStyleClass().add("formation-divider");

        String dateInfo = "";
        if (f.getDateDebut() != null) {
            dateInfo = String.valueOf(f.getDateDebut());
            if (f.getDateFin() != null) {
                dateInfo += " → " + f.getDateFin();
            }
        }
        Label meta = new Label(f.getLieu() + " • " + dateInfo);
        meta.getStyleClass().add("formation-meta");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button fav = new Button();
        fav.getStyleClass().add("formation-fav");
        fav.getStyleClass().add("fav-star");
        fav.setFocusTraversable(false);
        fav.setPickOnBounds(false);

        SVGPath star = new SVGPath();
        star.getStyleClass().add("fav-star-svg");
        star.setContent("M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z");
        fav.setGraphic(star);

        User currentForFav = Session.getCurrentUser();
        if (currentForFav != null && f != null) {
            boolean isFav = favoritesService.isFavorite(currentForFav.getId(), f.getId());
            if (isFav) {
                fav.getStyleClass().add("fav-star-active");
            }
        }

        fav.setOnMouseClicked(e -> e.consume());
        fav.setOnAction(e -> {
            e.consume();
            User cu = Session.getCurrentUser();
            if (cu == null || f == null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION);
                a.setTitle("Favoris");
                a.setHeaderText(null);
                a.setContentText("Veuillez vous connecter pour gérer vos favoris.");
                a.showAndWait();
                return;
            }
            boolean nowFav = favoritesService.toggleFavorite(cu.getId(), f.getId());
            favoriteIdsCache = null;
            if (nowFav) {
                if (!fav.getStyleClass().contains("fav-star-active")) {
                    fav.getStyleClass().add("fav-star-active");
                }
            } else {
                fav.getStyleClass().remove("fav-star-active");
                if (favoritesOnlyEnabled) {
                    refreshCards();
                }
            }
        });

        HBox footer = new HBox(10);
        footer.getStyleClass().add("formation-footer");
        footer.getChildren().addAll(meta, spacer, fav);

        card.getChildren().addAll(cover, body, divider, footer);
        return card;
    }

    private void applyDomainCover(Region cover, String domaine) {
        if (cover == null) {
            return;
        }

        String slug = slugify(domaine);
        if (slug.isEmpty()) {
            return;
        }

        java.net.URL url = getClass().getResource("/Images/domains/" + slug + ".png");
        if (url == null) {
            url = getClass().getResource("/Images/domains/" + slug + ".jpg");
        }
        if (url == null) {
            url = getClass().getResource("/Images/domains/" + slug + ".jpeg");
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
        return v;
    }

    private void ouvrirCours(Formation f) {
        if (isCandidatWithoutAccess(f)) {
            showPayDialogAndPurchase(f);
            return;
        }

        try {
            UiState.selectedFormationId = f.getId();
            UiState.selectedFormationTitre = f.getTitre();

            WindowUtil.navigate(cardsFlow, "/Formation/CourFront.fxml", "Cours de la formation");
        } catch (Exception ex) {
            throw new RuntimeException(ex);
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

        Dialog<ButtonType> pay = new Dialog<>();
        pay.setTitle("Paiement");
        pay.setHeaderText(null);

        ButtonType btnPayer = new ButtonType("Payer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        pay.getDialogPane().getButtonTypes().setAll(btnAnnuler, btnPayer);

        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 18; -fx-background-color: white;");

        Label title = new Label("Cette formation est payante");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #111827;");

        Label price = new Label("Prix: " + f.getPrix() + " DT");
        price.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #4f46e5;");

        Label hint = new Label("Voulez-vous payer maintenant ?");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");

        VBox card = new VBox(6, price, hint);
        card.setStyle("-fx-padding: 12; -fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-background-radius: 12; -fx-background-color: #f9fafb;");

        content.getChildren().addAll(title, card);
        pay.getDialogPane().setContent(content);
        pay.getDialogPane().setStyle("-fx-background-color: white; -fx-padding: 0; -fx-border-color: #e5e7eb; -fx-border-radius: 14; -fx-background-radius: 14;");

        Button payBtn = (Button) pay.getDialogPane().lookupButton(btnPayer);
        payBtn.setDefaultButton(true);
        payBtn.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 16;");

        Button cancelBtn = (Button) pay.getDialogPane().lookupButton(btnAnnuler);
        cancelBtn.setCancelButton(true);
        cancelBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #111827; -fx-font-weight: 700; -fx-background-radius: 10; -fx-padding: 8 16; -fx-border-color: #e5e7eb; -fx-border-radius: 10;");

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

            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Paiement Stripe");

            VBox root = new VBox(webView);
            Scene scene = new Scene(root, 980, 720);
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

                refreshCards();
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
}
