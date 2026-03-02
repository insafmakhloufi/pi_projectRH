package Controllers.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Entretien;
import Services.candidature.CandidatureService;
import Services.candidature.EntretienService;
import Utils.Session;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import Services.candidature.ContratService;
import Entities.candidature.Contrat;

public class Mescandidatures {

    @FXML private TextField tfSearch;
    @FXML private Button btnAdd;
    @FXML private Button btnCv;

    @FXML private Button btnSearch;

    @FXML private ScrollPane scroll;
    @FXML private GridPane cardsGrid;
    @FXML private Label lblNom;
    @FXML private Label lblEmail;
    @FXML private Label lblStatut;
    @FXML private Label lblPhone;
    @FXML private Label lblNotes;
    @FXML private Label lblEmpty;

    @FXML
    private VBox detailsPlaceholder;

    @FXML
    private VBox detailsContent;

    @FXML private Button btnDetails;
    @FXML private Button btnInterview;
    @FXML private Button btnInterviewIA;

    private final CandidatureService candidatureService = new CandidatureService();
    private final EntretienService entretienService = new EntretienService();
    private final List<Candidature> master = new ArrayList<>();
    private List<Candidature> filtered = new ArrayList<>();
    private final Map<Integer, Boolean> entretienCache = new HashMap<>();
    private Candidature selected;
    private VBox selectedCard;

    private static final PseudoClass PC_SELECTED = PseudoClass.getPseudoClass("selected");

    @FXML
    private void initialize() {
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, o, n) -> applyFilter());
        }

        if (btnSearch != null) {
            btnSearch.setOnAction(e -> applyFilter());
        }

        if (scroll != null && cardsGrid != null) {
            scroll.viewportBoundsProperty().addListener((obs, o, n) -> {
                cardsGrid.setPrefWidth(Math.max(0, n.getWidth()));
                cardsGrid.requestLayout();
            });
            Platform.runLater(() -> {
                if (scroll.getViewportBounds() != null) {
                    cardsGrid.setPrefWidth(Math.max(0, scroll.getViewportBounds().getWidth()));
                }
                cardsGrid.requestLayout();
            });

            // Global catcher: ensures selection works even when clicking on inner nodes/overlays
            cardsGrid.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                Node n = (e.getTarget() instanceof Node) ? (Node) e.getTarget() : null;
                while (n != null) {
                    if (n instanceof VBox v && v.getProperties().get("candidature") instanceof Candidature c) {
                        setSelected(c, v);
                        return;
                    }
                    n = n.getParent();
                }
            });
        }
        loadData();
        // Force re-render après chargement
        Platform.runLater(() -> {
            applyFilter();
        });
    }

    private void onInterviewIAFor() {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/interview.fxml");
            if (url == null) throw new RuntimeException("interview.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            // Optionnel: si tu ajoutes plus tard setCandidature(...) dans le controller
            Object ctrl = loader.getController();
            if (ctrl instanceof Interviewcontroller ic) {
                ic.setNavigationContainer(centerWrap);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir l'interview IA.").show();
        }
    }

    private void loadData() {
        master.clear();
        try {
            var user = Session.getCurrentUser();
            int userId = (user == null) ? 0 : user.getId();
            if (userId <= 0) {
                if (lblEmpty != null) {
                    lblEmpty.setText("Veuillez vous connecter pour voir vos candidatures.");
                }
            } else {
                master.addAll(candidatureService.afficherCandidaturesPourUserId(userId));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors du chargement des candidatures.").show();
        }
        refreshEntretienCache();
        setSelected(null, null);
        applyFilter();
    }

    private void refreshEntretienCache() {
        entretienCache.clear();
        for (Candidature c : master) {
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
    private void onCv() {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/cv.fxml");
            if (url == null) throw new RuntimeException("cv.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            centerWrap.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir la page CV.").show();
        }
    }

    private void applyFilter() {
        String q = (tfSearch == null || tfSearch.getText() == null) ? "" : tfSearch.getText().trim().toLowerCase();
        if (q.isBlank()) {
            filtered = new ArrayList<>(master);
        } else {
            filtered = master.stream().filter(c -> matches(c, q)).collect(Collectors.toList());
        }
        render();
    }

    private boolean matches(Candidature c, String q) {
        if (c == null) return false;
        return safe(c.getPrenom()).toLowerCase().contains(q)
                || safe(c.getNom()).toLowerCase().contains(q)
                || safe(c.getEmail()).toLowerCase().contains(q)
                || safe(c.getStatut()).toLowerCase().contains(q);
    }

    private void render() {
        if (cardsGrid == null) return;
        cardsGrid.getChildren().clear();

        if (filtered.isEmpty()) {
            if (lblEmpty != null) lblEmpty.setText("Aucune candidature trouvée.");
            setSelected(null, null);
            return;
        }
        if (lblEmpty != null) lblEmpty.setText("");

        int row = 0;
        int col = 0;
        for (Candidature c : filtered) {
            VBox card = buildCard(c);
            card.setMinWidth(0);
            card.setMaxWidth(Double.MAX_VALUE);

            // Wrapper fills the whole grid cell so the entire area is clickable (even empty space to the right)
            StackPane cell = new StackPane(card);
            cell.setPickOnBounds(true);
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMinWidth(0);
            cell.setOnMousePressed(e -> setSelected(c, card));
            StackPane.setAlignment(card, Pos.TOP_LEFT);

            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setFillWidth(cell, true);
            cardsGrid.add(cell, col, row);
            col++;
            if (col == 2) {
                col = 0;
                row++;
            }
        }

        VBox match = null;
        if (selected != null) {
            for (var n : cardsGrid.getChildren()) {
                VBox v = null;
                if (n instanceof VBox vb) {
                    v = vb;
                } else if (n instanceof StackPane sp) {
                    for (Node ch : sp.getChildren()) {
                        if (ch instanceof VBox vb) {
                            v = vb;
                            break;
                        }
                    }
                }

                if (v != null && selected.equals(v.getProperties().get("candidature"))) {
                    match = v;
                    break;
                }
            }
        }

        if (selected != null) {
            boolean stillThere = filtered.stream().anyMatch(x -> x.getIDCandidat() == selected.getIDCandidat());
            if (!stillThere) {
                setSelected(null, null);
            } else if (match != null) {
                // Re-apply selected state after re-render (filter/resize)
                setSelected(selected, match);
            }
        }
    }

    private VBox buildCard(Candidature c) {
        VBox card = new VBox(8);
        card.getStyleClass().add("mes-cand-card");
        card.setPadding(new Insets(12));
        card.setMinHeight(122);
        card.setPickOnBounds(true);
        card.getProperties().put("candidature", c);

        // Capture selection as early as possible (works even when clicking on children)
        card.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> setSelected(c, card));

        boolean hasEntretien = entretienCache.getOrDefault(c.getIDCandidat(), false);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Button btnPin = new Button("📌");
        btnPin.getStyleClass().add("mes-cand-card-ico");
        if (hasEntretien) btnPin.getStyleClass().add("mes-cand-card-ico-entretien");
        btnPin.setMouseTransparent(true);
        btnPin.setFocusTraversable(false);

        Button btnHat = new Button("🎓");
        btnHat.getStyleClass().add("mes-cand-card-ico");
        btnHat.setMouseTransparent(true);
        btnHat.setFocusTraversable(false);

        String dec = safe(c.getDecisionRh()).trim().toUpperCase();
        String decIcon = "⏳";
        String decClass = "mes-cand-card-decision-pending";
        if (dec.contains("ACCEP")) {
            decIcon = "✅";
            decClass = "mes-cand-card-decision-accept";
        } else if (dec.contains("REFUS")) {
            decIcon = "❌";
            decClass = "mes-cand-card-decision-refuse";
        }

        Button btnDecision = new Button(decIcon);
        btnDecision.getStyleClass().addAll("mes-cand-card-ico", "mes-cand-card-decision", decClass);
        btnDecision.setMouseTransparent(true);
        btnDecision.setFocusTraversable(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnEdit = new Button("✎");
        btnEdit.getStyleClass().add("mes-cand-card-ico");
        btnEdit.setOnAction(e -> {
            setSelected(c, card);
            onEditFor(c);
        });

        Button btnDelete = new Button("🗑");
        btnDelete.getStyleClass().addAll("mes-cand-card-ico", "mes-cand-card-del");
        btnDelete.setOnAction(e -> {
            setSelected(c, card);
            onDeleteFor(c);
        });

        top.getChildren().addAll(btnPin, btnHat, btnDecision, spacer, btnEdit, btnDelete);

        Label title = new Label(cardTitle(c));
        title.getStyleClass().add("mes-cand-card-title");

        Label sub = new Label(subtitle(c));
        sub.getStyleClass().add("mes-cand-card-sub");

        Label note = new Label(notesText(c));
        note.getStyleClass().add("mes-cand-card-note");

        HBox emailPill = new HBox(8);
        emailPill.getStyleClass().add("mes-cand-pill");
        Label mailIcon = new Label("✉");
        mailIcon.getStyleClass().add("mes-cand-pill-ico");
        Label mail = new Label(safe(c.getEmail()));
        mail.getStyleClass().add("mes-cand-pill-text");
        emailPill.getChildren().addAll(mailIcon, mail);

        Button btnCardDetails = new Button("Voir les détails");
        btnCardDetails.getStyleClass().add("mes-cand-card-wide");
        btnCardDetails.setMaxWidth(Double.MAX_VALUE);
        btnCardDetails.setOnAction(e -> {
            setSelected(c, card);
            onDetailsFor(c);
        });

        //zerferferferferferferthr
        // Test temporaire - à supprimer après vérification
        System.out.println("DEBUG - DecisionRH: " + c.getDecisionRh() + " | IDCandidat: " + c.getIDCandidat());

        Button btnCardEntretien = new Button("Mon entretien");
        btnCardEntretien.getStyleClass().add("mes-cand-card-wide");
        btnCardEntretien.setMaxWidth(Double.MAX_VALUE);
        btnCardEntretien.setOnAction(e -> {
            setSelected(c, card);
            onInterviewFor(c);
        });
        btnCardEntretien.setVisible(hasEntretien);
        btnCardEntretien.setManaged(hasEntretien);

        //boutton contract
        ContratService contratServiceLocal = new ContratService();
        Contrat contrat = null;
        try {
            String sqlTest = "SELECT * FROM contrat WHERE candidature_id=? ORDER BY created_at DESC LIMIT 1";
            java.sql.Connection connTest = Utils.Mydatabase.getInstance().getConnection();
            java.sql.PreparedStatement psTest = connTest.prepareStatement(sqlTest);
            psTest.setInt(1, c.getIDCandidat());
            java.sql.ResultSet rsTest = psTest.executeQuery();
            if (rsTest.next()) {
                contrat = contratServiceLocal.getContratById(rsTest.getInt("id"));
                System.out.println("DEBUG DIRECT SQL ✅ contrat id=" + rsTest.getInt("id") + " pour candidature=" + c.getIDCandidat());
            }
        } catch (Exception ex) {
            System.out.println("ERREUR DIRECT SQL: " + ex.getMessage());
        }

        boolean hasContrat = contrat != null;
        boolean isAcceptee = safe(c.getDecisionRh()).toUpperCase().contains("ACCEP");

// DEBUG temporaire - à supprimer après
        System.out.println("DEBUG CONTRAT - ID: " + c.getIDCandidat()
                + " | decisionRh: '" + c.getDecisionRh() + "'"
                + " | hasContrat: " + hasContrat
                + " | isAcceptee: " + isAcceptee
                + " | contrat: " + (contrat != null ? contrat.getId() : "null"));
        Button btnVoirContrat = new Button("📄 Voir Contrat");
        btnVoirContrat.getStyleClass().add("mes-cand-card-wide");
        btnVoirContrat.setMaxWidth(Double.MAX_VALUE);
        btnVoirContrat.setStyle("-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold;");

        final Contrat contratFinal = contrat;
        btnVoirContrat.setOnAction(e -> {
            setSelected(c, card);
            ouvrirVueContrat(c, contratFinal);
        });

        // APRES
        if (isAcceptee && hasContrat) {
            btnVoirContrat.setVisible(true);
            btnVoirContrat.setManaged(true);
            System.out.println("DEBUG ✅ Bouton contrat VISIBLE pour ID=" + c.getIDCandidat());
        } else {
            btnVoirContrat.setVisible(false);
            btnVoirContrat.setManaged(false);
            System.out.println("DEBUG ❌ Bouton contrat CACHE pour ID=" + c.getIDCandidat()
                    + " isAcceptee=" + isAcceptee + " hasContrat=" + hasContrat);
        }

        // ✅ buttons déclaré ICI avant d'être utilisé
        VBox buttons = new VBox(10);
        buttons.getChildren().add(btnCardDetails);
        if (hasEntretien) buttons.getChildren().add(btnCardEntretien);
        buttons.getChildren().add(btnVoirContrat);

        card.getChildren().addAll(top, title, sub, note, emailPill, buttons);

        card.setOnMouseClicked(e -> setSelected(c, card));
        return card;
    }

    private void setSelected(Candidature c, VBox cardNode) {
        selected = c;

        if (selectedCard != null) {
            selectedCard.pseudoClassStateChanged(PC_SELECTED, false);
        }
        selectedCard = cardNode;
        if (selectedCard != null) {
            selectedCard.pseudoClassStateChanged(PC_SELECTED, true);
        }

        boolean has = (c != null);

        if (detailsPlaceholder != null) {
            detailsPlaceholder.setVisible(!has);
            detailsPlaceholder.setManaged(!has);
        }
        if (detailsContent != null) {
            detailsContent.setVisible(has);
            detailsContent.setManaged(has);
        }

        if (lblNom != null) lblNom.setText(!has ? "" : (safe(c.getPrenom()) + " " + safe(c.getNom())).trim());
        if (lblEmail != null) lblEmail.setText(!has ? "" : safe(c.getEmail()));
        if (lblStatut != null) lblStatut.setText(!has ? "" : statusText(c));
        if (lblPhone != null) lblPhone.setText(!has ? "" : phoneText(c));
        if (lblNotes != null) lblNotes.setText(!has ? "" : notesText(c));

        if (btnDetails != null) btnDetails.setDisable(!has);
        if (btnInterview != null) {
            boolean hasEntretien = has && entretienCache.getOrDefault(c.getIDCandidat(), false);
            btnInterview.setDisable(!has);
            btnInterview.setVisible(hasEntretien);
            btnInterview.setManaged(hasEntretien);
        }
        if (btnInterviewIA != null) {
            btnInterviewIA.setDisable(false);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String cardTitle(Candidature c) {
        String name = (safe(c.getPrenom()) + " " + safe(c.getNom())).trim();
        return name.isBlank() ? ("Candidature #" + c.getIDCandidat()) : name;
    }

    private String badgeText(Candidature c) {
        String st = safe(c.getStatut()).trim();
        return st.isBlank() ? "EN COURS" : st;
    }

    private String subtitle(Candidature c) {
        String deg = safe(c.getHighest_degree()).trim();
        String inst = safe(c.getInstitution()).trim();
        if (!deg.isBlank() && !inst.isBlank()) return deg + " • " + inst;
        if (!deg.isBlank()) return deg;
        if (!inst.isBlank()) return inst;
        return "";
    }

    private String statusText(Candidature c) {
        // In DB, getCandidatureById sets statut to TRAITE/NON_TRAITE, but list might not.
        String st = safe(c.getStatut()).trim();
        if (st.isBlank()) {
            boolean hasEntretien = entretienCache.getOrDefault(c.getIDCandidat(), false);
            st = hasEntretien ? "TRAITE" : "NON_TRAITE";
        }

        String u = st.toUpperCase();
        if (u.contains("TRAITE") && !u.contains("NON")) return "✓ TRAITE";
        if (u.contains("NON")) return "⏳ NON_TRAITE";
        return st;
    }

    private String phoneText(Candidature c) {
        String ind = safe(c.getIndicatif()).trim();
        String tel = safe(c.getTel()).trim();
        String p = (ind + " " + tel).trim();
        return p.isBlank() ? "-" : p;
    }

    private String notesText(Candidature c) {
        // Placeholder: you can later replace by real notes from DB
        String ville = safe(c.getVille()).trim();
        String addr = safe(c.getAdresse()).trim();
        if (!ville.isBlank() && !addr.isBlank()) return addr + ", " + ville;
        if (!addr.isBlank()) return addr;
        if (!ville.isBlank()) return ville;
        return "";
    }

    private String previewNote(Candidature c) {
        String s = notesText(c);
        if (s.isBlank()) return "";
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    // ---------------- actions ----------------

    @FXML
    private void onAdd() {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/Ajouter.fxml");
            if (url == null) throw new RuntimeException("Ajouter.fxml introuvable");

            Parent view = new FXMLLoader(url).load();
            centerWrap.getChildren().setAll(view);
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire d'ajout.").show();
        }
    }

    @FXML
    private void onDetails() {
        if (selected == null) return;
        onDetailsFor(selected);
    }

    @FXML
    private void onInterview() {
        if (selected == null) return;
        onInterviewFor(selected);
    }

    @FXML
    private void onInterviewIA() {
        System.out.println("[MesCandidatures] Click Interview IA");
        onInterviewIAFor();
    }

    @FXML
    private void onEditFor(Candidature c) {
        if (c == null) return;
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/Ajouter.fxml");
            if (url == null) throw new RuntimeException("Ajouter.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            AjouterCandidature ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setEditCandidature(c);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le formulaire de modification.").show();
        }
    }

    @FXML
    private void onDetailsFor(Candidature c) {
        if (c == null) return;
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/CandidatureDetails.fxml");
            if (url == null) throw new RuntimeException("CandidatureDetails.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            if (view instanceof Region r) {
                r.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                StackPane.setAlignment(r, Pos.TOP_LEFT);
            }

            CandidatureDetailsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setCandidature(c);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les détails de la candidature.").show();
        }
    }

    private Pane findCenterWrap() {
        if (cardsGrid == null || cardsGrid.getScene() == null) return null;
        Parent root = cardsGrid.getScene().getRoot();

        var page = root.lookup("#pageContainer");
        if (page instanceof Pane p) return p;

        var content = root.lookup("#contentArea");
        if (content instanceof Pane p) return p;

        return null;
    }

    @FXML
    private void onInterviewFor(Candidature c) {
        if (c == null) return;
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/CandidateEntretienDetails.fxml");
            if (url == null) throw new RuntimeException("CandidateEntretienDetails.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            CandidateEntretienDetailsController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setCandidature(c);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir les détails de l'entretien.").show();
        }
    }

    @FXML
    private void onDeleteFor(Candidature c) {
        if (c == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer la candidature ID=" + c.getIDCandidat() + " ?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Suppression");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                int id = c.getIDCandidat();
                candidatureService.supprimerCandidature(id);
                master.removeIf(x -> x.getIDCandidat() == id);
                entretienCache.remove(id);
                applyFilter();
            } catch (Exception ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la suppression.").show();
            }
        });
    }

    private void ouvrirVueContrat(Candidature c, Contrat contrat) {
        try {
            Pane centerWrap = findCenterWrap();
            if (centerWrap == null) throw new RuntimeException("pageContainer/contentArea not found");

            var url = getClass().getResource("/candidaturefxml/Candidat_ContratView.fxml");
            if (url == null) throw new RuntimeException("Candidat_ContratView.fxml introuvable");

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();

            Candidat_ContratViewController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.initContrat(c, contrat);
            }

            centerWrap.getChildren().setAll(view);

        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Impossible d'ouvrir le contrat.\n" + ex.getMessage()).show();
        }
    }
}
