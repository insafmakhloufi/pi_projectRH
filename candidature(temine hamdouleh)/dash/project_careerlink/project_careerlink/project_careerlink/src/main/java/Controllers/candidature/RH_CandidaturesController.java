package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Entretien;
import Services.candidature.CandidatureService;
import Services.candidature.EntretienService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import Services.candidature.ContratService;
import Entities.candidature.Contrat;
import javafx.scene.control.ButtonBar;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RH_CandidaturesController {

    @FXML private TextField tfSearch;
    @FXML private GridPane cardsGrid;
    @FXML private ScrollPane scroll;

    @FXML private VBox candDetailBox;
    @FXML private VBox candDetailPlaceholder;
    @FXML private Label lblDetInitials;
    @FXML private Label lblDetName;
    @FXML private Label lblDetRole;
    @FXML private Label lblDetEmail;
    @FXML private Label lblDetPhone;
    @FXML private Label lblDetDesc;
    @FXML private Button btnDetDetails;
    @FXML private Button btnDetEntretien;

    @FXML private Label lblStatTotal;
    @FXML private Label lblStatShown;
    @FXML private Label lblStatConfirmed;

    private final CandidatureService candidatureService = new CandidatureService();
    private final EntretienService entretienService = new EntretienService();
    private final ObservableList<Candidature> masterList = FXCollections.observableArrayList();
    private final Map<Integer, Boolean> entretienCache = new HashMap<>();

    private static final PseudoClass PC_SELECTED = PseudoClass.getPseudoClass("selected");
    private VBox selectedCard;
    private Candidature selectedCandidature;

    @FXML
    private void initialize() {
        List<Candidature> data = candidatureService.afficherCandidatures();
        masterList.setAll(data);

        refreshEntretienCache();

        renderCards(masterList);

        updateStats(masterList);

        setSelected(null, null);

        tfSearch.textProperty().addListener((obs, old, q) -> {
            String query = (q == null) ? "" : q.toLowerCase().trim();
            if (query.isEmpty()) {
                renderCards(masterList);
                return;
            }

            ObservableList<Candidature> filtered = FXCollections.observableArrayList();
            for (Candidature c : masterList) {
                String s = (safe(c.getPrenom()) + " " + safe(c.getNom()) + " " + safe(c.getEmail()) + " " + safe(c.getVille()))
                        .toLowerCase();
                if (s.contains(query)) filtered.add(c);
            }
            renderCards(filtered);
            updateStats(filtered);
        });

        if (scroll != null) {
            scroll.viewportBoundsProperty().addListener((obs, o, n) -> cardsGrid.requestLayout());
        }
        Platform.runLater(() -> cardsGrid.requestLayout());
    }

    private void refreshEntretienCache() {
        entretienCache.clear();
        for (Candidature c : masterList) {
            if (c == null) continue;
            int id = c.getIDCandidat();
            if (id <= 0) continue;
            try {
                Entretien e = entretienService.getLatestEntretienByCandidature(id);
                entretienCache.put(id, e != null);
            } catch (Exception ex) {
                entretienCache.put(id, false);
            }
        }
    }

    @FXML
    private void openSelectedDetails() {
        if (selectedCandidature == null) return;
        openDetailsView(selectedCandidature);
    }

    @FXML
    private void openSelectedEntretien() {
        if (selectedCandidature == null) return;
        int id = selectedCandidature.getIDCandidat();
        boolean hasEntretien = entretienCache.getOrDefault(id, false);
        if (hasEntretien) {
            openEntretienDetailsFor(id);
        } else {
            openEntretienFormFor(id, null);
        }
    }

    private void openEntretienDetailsFor(int idCandidature) {
        try {
            Parent root = cardsGrid.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof javafx.scene.layout.Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            Entretien e = entretienService.getLatestEntretienByCandidature(idCandidature);
            if (e == null) {
                new Alert(Alert.AlertType.INFORMATION, "Aucun entretien trouvé pour cette candidature.").show();
                entretienCache.put(idCandidature, false);
                if (selectedCandidature != null && selectedCandidature.getIDCandidat() == idCandidature) {
                    updateDetails(selectedCandidature);
                }
                return;
            }

            var url = getClass().getResource("/candidaturefxml/RH_EntretienDetails.fxml");
            if (url == null) throw new RuntimeException("RH_EntretienDetails.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienDetailsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setEntretien(e);
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les détails de l'entretien.").show();
        }
    }

    private void renderCards(ObservableList<Candidature> list) {
        cardsGrid.getChildren().clear();

        int row = 0, col = 0;
        for (Candidature c : list) {
            VBox card = buildCard(c);
            card.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(card, Priority.ALWAYS);
            card.setMinWidth(300);

            cardsGrid.add(card, col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }

        if (selectedCandidature != null) {
            VBox match = null;
            for (var n : cardsGrid.getChildren()) {
                if (n instanceof VBox v && selectedCandidature.equals(v.getProperties().get("candidature"))) {
                    match = v;
                    break;
                }
            }
            setSelected(selectedCandidature, match);
        }
    }

    private void updateStats(List<Candidature> shownList) {
        int total = masterList.size();
        int shown = (shownList == null) ? 0 : shownList.size();
        int confirmed = 0;
        for (Candidature c : masterList) {
            if (c != null && c.isConfirm_info()) confirmed++;
        }

        if (lblStatTotal != null) lblStatTotal.setText(String.valueOf(total));
        if (lblStatShown != null) lblStatShown.setText(String.valueOf(shown));
        if (lblStatConfirmed != null) lblStatConfirmed.setText(String.valueOf(confirmed));
    }

    private VBox buildCard(Candidature c) {
        VBox card = new VBox(10);
        card.getStyleClass().add("rh-cand-card");
        card.setFillWidth(true);
        card.setMaxWidth(Double.MAX_VALUE);
        card.getProperties().put("candidature", c);

        boolean hasEntretien = c != null && entretienCache.getOrDefault(c.getIDCandidat(), false);

        Label ico1 = new Label("📌");
        ico1.getStyleClass().addAll("rh-cand-tile-ico", "rh-cand-tile-ico-a");
        if (hasEntretien) ico1.getStyleClass().add("rh-cand-tile-ico-entretien");
        Label ico2 = new Label("🎓");
        ico2.getStyleClass().addAll("rh-cand-tile-ico", "rh-cand-tile-ico-b");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAccept = new Button("✔");
        btnAccept.getStyleClass().addAll("rh-icon-btn", "rh-edit", "rh-cand-tile-accept");
        btnAccept.setOnAction(e -> placeholderAccept(c));

        Button btnRefuse = new Button("✖");
        btnRefuse.getStyleClass().addAll("rh-icon-btn", "rh-danger", "rh-cand-tile-refuse");
        btnRefuse.setOnAction(e -> placeholderRefuse(c));

        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().addAll("rh-icon-btn", "rh-danger", "rh-cand-tile-delete");
        btnDelete.setOnAction(e -> confirmDelete(c));

        HBox top = new HBox(10);
        top.getStyleClass().add("rh-cand-tile-top");
        top.getChildren().addAll(ico1, ico2, spacer, btnAccept, btnRefuse, btnDelete);

        String titleText = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
        if (titleText.isBlank()) titleText = "Candidature";
        Label title = new Label(titleText);
        title.getStyleClass().add("rh-cand-tile-title");

        String role = safe(c.getHighest_degree());
        if (role.isBlank()) role = "Candidat";
        String city = safe(c.getVille());

        String subtitleText = role + (city.isBlank() ? "" : " • " + city);
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("rh-cand-tile-sub");

        String desc = safe(c.getLettre_motivation());
        if (desc.isBlank()) desc = safe(c.getSkills_text());
        if (desc.isBlank()) desc = "Profil candidat : informations non renseignées.";
        desc = desc.replaceAll("\\s+", " ").trim();
        if (desc.length() > 120) desc = desc.substring(0, 120).trim() + "...";
        Label description = new Label(desc);
        description.getStyleClass().add("rh-cand-tile-desc");
        description.setWrapText(true);

        String badgeText = safe(c.getEmail());
        if (badgeText.isBlank()) badgeText = "Email non renseigné";
        Label badge = new Label("✉  " + badgeText);
        badge.getStyleClass().add("rh-cand-tile-badge");
        badge.setWrapText(false);

        Button btnAnalyseIA = new Button("Analyse IA");
        btnAnalyseIA.getStyleClass().addAll("rh-btn-secondary", "rh-cand-tile-ai");
        btnAnalyseIA.setOnAction(e -> placeholderAnalyseIA(c));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottom = new HBox(10);
        bottom.getStyleClass().add("rh-cand-tile-bottom");
        bottom.getChildren().addAll(bottomSpacer, btnAnalyseIA, badge);

        card.getChildren().addAll(top, title, subtitle, description, bottom);

        card.setOnMouseClicked(e -> setSelected(c, card));
        return card;
    }

    private void placeholderAnalyseIA(Candidature c) {
        if (c == null) return;

        try {
            Parent root = cardsGrid.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof javafx.scene.layout.Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/AnalyseIA_RH.fxml");
            if (url == null) throw new RuntimeException("AnalyseIA_RH.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            AnalyseIAController_RH ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setCandidature(c);
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            String msg = "Impossible d'ouvrir la page Analyse IA.";
            String detail = ex.toString();
            if (ex.getCause() != null) detail += "\nCause: " + ex.getCause();
            new Alert(Alert.AlertType.ERROR, msg + "\n\n" + detail).show();
        }
    }

    private void setSelected(Candidature c, VBox cardNode) {
        selectedCandidature = c;

        if (selectedCard != null) {
            selectedCard.pseudoClassStateChanged(PC_SELECTED, false);
        }
        selectedCard = cardNode;
        if (selectedCard != null) {
            selectedCard.pseudoClassStateChanged(PC_SELECTED, true);
        }

        updateDetails(c);
    }

    private void updateDetails(Candidature c) {
        boolean has = (c != null);

        if (candDetailBox != null) {
            candDetailBox.setVisible(has);
            candDetailBox.setManaged(has);
        }
        if (candDetailPlaceholder != null) {
            candDetailPlaceholder.setVisible(!has);
            candDetailPlaceholder.setManaged(!has);
        }

        if (btnDetDetails != null) btnDetDetails.setDisable(!has);
        if (btnDetEntretien != null) {
            btnDetEntretien.setDisable(!has);
            if (has) {
                boolean hasEntretien = entretienCache.getOrDefault(c.getIDCandidat(), false);
                btnDetEntretien.setText(hasEntretien ? "Voir entretien" : "Entretien");
            }
        }

        if (!has) {
            if (lblDetInitials != null) lblDetInitials.setText("");
            if (lblDetName != null) lblDetName.setText("");
            if (lblDetRole != null) lblDetRole.setText("");
            if (lblDetEmail != null) lblDetEmail.setText("");
            if (lblDetPhone != null) lblDetPhone.setText("");
            if (lblDetDesc != null) lblDetDesc.setText("");
            return;
        }

        String initials = initialsOf(safe(c.getPrenom()), safe(c.getNom()));
        if (lblDetInitials != null) lblDetInitials.setText(initials);
        if (lblDetName != null) lblDetName.setText((safe(c.getPrenom()) + " " + safe(c.getNom())).trim());
        if (lblDetRole != null) lblDetRole.setText(safe(c.getHighest_degree()));
        if (lblDetEmail != null) lblDetEmail.setText("✉  " + safe(c.getEmail()));
        String phone = (safe(c.getIndicatif()) + " " + safe(c.getTel())).trim();
        if (lblDetPhone != null) lblDetPhone.setText("☎  " + (phone.isBlank() ? "-" : phone));

        if (lblDetDesc != null) {
            String desc = safe(c.getLettre_motivation());
            if (desc.isBlank()) desc = safe(c.getSkills_text());
            if (desc.isBlank()) {
                desc = "Profil candidat : informations non renseignées.";
            }
            if (desc.length() > 220) desc = desc.substring(0, 220).trim() + "...";
            lblDetDesc.setText(desc);
        }
    }

    private String initialsOf(String prenom, String nom) {
        String p = (prenom == null) ? "" : prenom.trim();
        String n = (nom == null) ? "" : nom.trim();
        String a = p.isEmpty() ? "" : p.substring(0, 1).toUpperCase();
        String b = n.isEmpty() ? "" : n.substring(0, 1).toUpperCase();
        String r = (a + b).trim();
        return r.isEmpty() ? "?" : r;
    }

    private void openDetailsView(Candidature c) {
        try {
            Parent root = cardsGrid.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof javafx.scene.layout.Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/RH_CandidatureDetails.fxml");
            if (url == null) throw new RuntimeException("RH_CandidatureDetails.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            if (view instanceof Region r) {
                r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                StackPane.setAlignment(r, Pos.TOP_LEFT);
            }

            RH_CandidatureDetailsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setCandidature(c);
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les détails de la candidature.").show();
        }
    }

    private void confirmDelete(Candidature c) {
        if (c == null) return;

        int id = c.getIDCandidat();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer cette candidature ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    candidatureService.supprimerCandidature(id);
                    masterList.removeIf(x -> x.getIDCandidat() == id);
                    renderCards(masterList);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
                }
            }
        });
    }

    private void placeholderRefuse(Candidature c) {
        if (c == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Refuser");
        confirm.setHeaderText("Refuser cette candidature ?");
        confirm.setContentText("Le candidat verra la décision sur la page Mes Candidatures.");

        confirm.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            try {
                int id = c.getIDCandidat();
                candidatureService.setDecisionRh(id, "REFUSEE");
                c.setDecisionRh("REFUSEE");
                if (selectedCandidature != null && selectedCandidature.getIDCandidat() == id) {
                    selectedCandidature.setDecisionRh("REFUSEE");
                }
                renderCards(masterList);
                new Alert(Alert.AlertType.INFORMATION, "Candidature refusée.").show();
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour de la décision.").show();
            }
        });
    }

    //contracttttt
    /*
    private void placeholderAccept(Candidature c) {
        if (c == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter");
        confirm.setHeaderText("Accepter cette candidature ?");
        confirm.setContentText("Le candidat verra la décision sur la page Mes Candidatures.");

        confirm.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            try {
                int id = c.getIDCandidat();
                candidatureService.setDecisionRh(id, "ACCEPTEE");
                c.setDecisionRh("ACCEPTEE");
                if (selectedCandidature != null && selectedCandidature.getIDCandidat() == id) {
                    selectedCandidature.setDecisionRh("ACCEPTEE");
                }
                renderCards(masterList);
                new Alert(Alert.AlertType.INFORMATION, "Candidature acceptée.").show();
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour de la décision.").show();
            }
        });
    }  */

    private void placeholderAccept(Candidature c) {
        if (c == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter");
        confirm.setHeaderText("Accepter cette candidature ?");
        confirm.setContentText("Le candidat verra la décision sur la page Mes Candidatures.");

        confirm.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;
            try {
                int id = c.getIDCandidat();
                candidatureService.setDecisionRh(id, "ACCEPTEE");
                c.setDecisionRh("ACCEPTEE");
                if (selectedCandidature != null && selectedCandidature.getIDCandidat() == id) {
                    selectedCandidature.setDecisionRh("ACCEPTEE");
                }
                renderCards(masterList);

                // ✅ Popup 3 choix après acceptation
                afficherPopupContrat(c);

            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise à jour de la décision.").show();
            }
        });
    }

    private void afficherPopupContrat(Candidature c) {
        // Boutons personnalisés
        ButtonType btnGenererMaintenant = new ButtonType("📄 Générer contrat maintenant");
        ButtonType btnSansContrat       = new ButtonType("✅ Accepter sans contrat");
        ButtonType btnAnnuler           = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert popup = new Alert(Alert.AlertType.CONFIRMATION);
        popup.setTitle("Contrat de travail");
        popup.setHeaderText("Candidature acceptée !");
        popup.setContentText("Que souhaitez-vous faire pour "
                + c.getPrenom() + " " + c.getNom() + " ?");
        popup.getButtonTypes().setAll(btnGenererMaintenant, btnSansContrat, btnAnnuler);

        popup.showAndWait().ifPresent(choix -> {
            if (choix == btnGenererMaintenant) {
                ouvrirFormulaireContrat(c);
            } else if (choix == btnSansContrat) {
                new Alert(Alert.AlertType.INFORMATION,
                        "Candidature acceptée sans contrat.").show();
            }
            // Annuler → rien
        });
    }

    private void ouvrirFormulaireContrat(Candidature c) {
        try {
            Parent root = cardsGrid.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof javafx.scene.layout.Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/RH_ContratForm.fxml");
            if (url == null) throw new RuntimeException("RH_ContratForm.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_ContratFormController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.initContrat(c);
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire contrat.\n" + ex.getMessage()).show();
        }
    }


    private void openEntretienFormFor(int idCandidature, Integer idEntretien) {
        try {
            Parent root = cardsGrid.getScene().getRoot();
            var centerWrap = root.lookup("#adminContentArea");
            if (!(centerWrap instanceof javafx.scene.layout.Pane pane)) {
                throw new RuntimeException("adminContentArea not found");
            }

            var url = getClass().getResource("/candidaturefxml/RH_EntretienForm.fxml");
            if (url == null) throw new RuntimeException("RH_EntretienForm.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            RH_EntretienFormController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.openForCreate(idCandidature);
            }

            pane.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire d'entretien.").show();
        }
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
