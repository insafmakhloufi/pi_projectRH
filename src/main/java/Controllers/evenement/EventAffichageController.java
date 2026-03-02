package Controllers.evenement;

import Controllers.evenement.EventFormController;
import Controllers.evenement.EventPublicationsController;
import Controllers.evenement.Nav;
import Entities.evenement.Evenement;
import Entities.User.User;
import Services.evenement.EvenementService;
import Services.evenement.ParticipationService;
import Services.evenement.ticket.QrService;
import Services.evenement.ticket.TicketHtmlService;
import Utils.Session;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventAffichageController {

    @FXML protected TilePane flowEvents;
    @FXML protected TextField tfSearch;
    @FXML protected ComboBox<String> cbType;
    @FXML protected ComboBox<String> cbStatut;
    @FXML protected DatePicker dpFrom;
    @FXML protected DatePicker dpTo;
    @FXML protected Spinner<Integer> spCapacity;
    @FXML protected Button btnRefresh;
    @FXML protected Button btnNew;
    @FXML protected Button btnApplyFilter;
    @FXML protected Button btnResetFilter;
    @FXML protected Label resultCount;
    @FXML protected Label statusLabel;

    private EvenementService evenementService;
    private ObservableList<Evenement> allEvents;
    private ObservableList<Evenement> filteredEvents;

    private final Map<Integer, Integer> participantsByEventId = new HashMap<>();
    private final Set<Integer> participatingEventIds = new HashSet<>();
    private final Random random = new Random();
    private boolean isInitialized = false;

    private final ParticipationService participationService = new ParticipationService();
    private final QrService qrService = new QrService();
    private final TicketHtmlService ticketHtmlService = new TicketHtmlService();

    @FXML
    public void initialize() {
        evenementService = new EvenementService();
        allEvents = FXCollections.observableArrayList();
        filteredEvents = FXCollections.observableArrayList();

        setupFilters();
        setupSearchListener();
        setupButtonActions();
        loadEvents();

        if (!isInitialized) {
            animatePageEntrance();
            isInitialized = true;
        }
    }

    private void setupFilters() {
        ObservableList<String> types = FXCollections.observableArrayList(
                "Tous", "REUNION", "FORMATION", "TEAM_BUILDING", "WEBINAR", "AUTRE"
        );
        if (cbType != null) {
            cbType.setItems(types);
            cbType.setValue("Tous");
            cbType.setOnAction(e -> applyFilters());
        }

        ObservableList<String> statuses = FXCollections.observableArrayList(
                "Tous", "PUBLIE", "BROUILLON", "ANNULE", "TERMINE"
        );
        if (cbStatut != null) {
            cbStatut.setItems(statuses);
            cbStatut.setValue("Tous");
            cbStatut.setOnAction(e -> applyFilters());
        }

        if (btnApplyFilter != null) btnApplyFilter.setOnAction(e -> applyFilters());

        if (btnResetFilter != null) {
            btnResetFilter.setOnAction(e -> {
                if (cbType != null) cbType.setValue("Tous");
                if (cbStatut != null) cbStatut.setValue("Tous");
                if (dpFrom != null) dpFrom.setValue(null);
                if (dpTo != null) dpTo.setValue(null);
                if (spCapacity != null && spCapacity.getValueFactory() != null) spCapacity.getValueFactory().setValue(0);
                if (tfSearch != null) tfSearch.clear();
                applyFilters();
            });
        }

        if (spCapacity != null) {
            SpinnerValueFactory<Integer> vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 0, 5);
            spCapacity.setValueFactory(vf);
            spCapacity.setEditable(true);
            spCapacity.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    private void setupSearchListener() {
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
        }
    }

    protected void setupButtonActions() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> {
                if (statusLabel != null) statusLabel.setText("Actualisation...");
                loadEvents();
            });
        }

        if (btnNew != null) {
            if (Nav.getDefaultContentArea() != null) {
                btnNew.setVisible(true);
                btnNew.setManaged(true);
                btnNew.setOnAction(e -> openEventForm(null));
            } else {
                btnNew.setVisible(false);
                btnNew.setManaged(false);
            }
        }
    }

    protected void loadEvents() {
        try {
            List<Evenement> events = evenementService.afficherEvenements();
            allEvents.clear();
            allEvents.addAll(events);
            seedParticipationState(events);
            applyFilters();
            if (statusLabel != null) statusLabel.setText("Pret");
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de charger les evenements: " + e.getMessage());
            if (statusLabel != null) statusLabel.setText("Erreur de chargement");
        }
    }

    private void seedParticipationState(List<Evenement> events) {
        for (Evenement e : events) {
            int id = e.getIdEvenement();
            if (participantsByEventId.containsKey(id)) continue;
            int cap = Math.max(0, e.getCapaciteMax());
            int seeded = cap <= 0 ? 0 : Math.min(cap, random.nextInt(Math.min(cap, 12) + 1));
            participantsByEventId.put(id, seeded);
        }
    }

    protected void refreshEvents() {
        loadEvents();
    }

    private void applyFilters() {
        String searchText = tfSearch != null && tfSearch.getText() != null
                ? tfSearch.getText().toLowerCase() : "";
        String selectedType = cbType != null ? cbType.getValue() : "Tous";
        String selectedStatus = cbStatut != null ? cbStatut.getValue() : "Tous";
        java.time.LocalDate from = dpFrom != null ? dpFrom.getValue() : null;
        java.time.LocalDate to = dpTo != null ? dpTo.getValue() : null;
        int minCapacity = spCapacity != null && spCapacity.getValue() != null ? spCapacity.getValue() : 0;

        filteredEvents.clear();
        filteredEvents.addAll(allEvents.stream()
                .filter(event -> {
                    boolean matchesSearch = searchText.isEmpty()
                            || event.getTitre().toLowerCase().contains(searchText)
                            || event.getLieu().toLowerCase().contains(searchText)
                            || event.getType().toLowerCase().contains(searchText)
                            || event.getStatut().toLowerCase().contains(searchText);

                    boolean matchesType = "Tous".equals(selectedType)
                            || event.getType().equals(selectedType);

                    boolean matchesStatus = "Tous".equals(selectedStatus)
                            || event.getStatut().equals(selectedStatus);

                    boolean matchesDate = true;
                    if (from != null && event.getDateDebut() != null)
                        matchesDate = !event.getDateDebut().toLocalDate().isBefore(from);
                    if (matchesDate && to != null && event.getDateFin() != null)
                        matchesDate = !event.getDateFin().toLocalDate().isAfter(to);

                    boolean matchesCapacity = minCapacity <= 0
                            || (event.getCapaciteMax() >= minCapacity);

                    return matchesSearch && matchesType && matchesStatus && matchesDate && matchesCapacity;
                })
                .collect(Collectors.toList()));

        renderCards();
        updateResultCount();
    }

    protected void renderCards() {
        if (flowEvents == null) return;
        flowEvents.getChildren().clear();

        if (filteredEvents.isEmpty()) {
            showEmptyState();
            return;
        }

        for (int i = 0; i < filteredEvents.size(); i++) {
            VBox card = createEventCard(filteredEvents.get(i));
            flowEvents.getChildren().add(card);
        }

        playCardsEntranceAnimation();
    }

    protected VBox createEventCard(Evenement event) {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("event-card", "event-card-small", "event-card-compact");
        card.setPrefWidth(320);

        ImageView imageView = createEventImage(event.getImageUrl());
        imageView.getStyleClass().addAll("event-image", "event-image-small", "event-image-compact");
        imageView.setFitWidth(320);
        imageView.setFitHeight(140);
        imageView.setPreserveRatio(false);

        Rectangle clip = new Rectangle(320, 140);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        imageView.setClip(clip);

        StackPane imageContainer = new StackPane(imageView);
        imageContainer.getStyleClass().add("event-image-container");

        ScaleTransition imgIn = new ScaleTransition(Duration.millis(200), imageView);
        imgIn.setToX(1.035);
        imgIn.setToY(1.035);
        imgIn.setInterpolator(Interpolator.EASE_BOTH);
        ScaleTransition imgOut = new ScaleTransition(Duration.millis(200), imageView);
        imgOut.setToX(1.0);
        imgOut.setToY(1.0);
        imgOut.setInterpolator(Interpolator.EASE_BOTH);
        imageContainer.setOnMouseEntered(e -> imgIn.playFromStart());
        imageContainer.setOnMouseExited(e -> imgOut.playFromStart());

        VBox content = new VBox(8);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("event-content");

        Label titleLabel = new Label(event.getTitre());
        titleLabel.getStyleClass().add("event-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(260);

        HBox badgesContainer = new HBox(6);
        badgesContainer.setAlignment(Pos.CENTER_LEFT);
        badgesContainer.getStyleClass().add("chips-container");

        Label typeBadge = new Label(event.getType());
        typeBadge.getStyleClass().addAll("badge-pill", "badge-type", "chip", "chip-type");

        Label statusBadge = new Label(event.getStatut());
        statusBadge.getStyleClass().addAll("badge-pill", "badge-" + event.getStatut().toLowerCase(), "chip");
        badgesContainer.getChildren().addAll(typeBadge, statusBadge);

        Label dateLabel = new Label(formatDateRange(event.getDateDebut(), event.getDateFin()));
        dateLabel.getStyleClass().add("event-meta");

        HBox infoRow = new HBox(8);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        Label locationLabel = new Label("📍 " + event.getLieu());
        locationLabel.getStyleClass().add("event-meta");
        locationLabel.setWrapText(true);

        int currentParticipants = participantsByEventId.getOrDefault(event.getIdEvenement(), 0);
        int capacity = Math.max(0, event.getCapaciteMax());
        Label participantsChip = new Label("👥 " + currentParticipants + "/" + capacity);
        participantsChip.getStyleClass().addAll("chip");
        infoRow.getChildren().addAll(locationLabel, participantsChip);

        HBox actionsContainer = new HBox(8);
        actionsContainer.setAlignment(Pos.CENTER_LEFT);
        actionsContainer.getStyleClass().add("event-actions");

        Button btnParticiper = new Button();
        btnParticiper.getStyleClass().addAll("btn", "btn-primary", "btn-primary-small");
        updateParticipationUI(event, btnParticiper, participantsChip);
        btnParticiper.setOnAction(e -> handleParticiper(event, btnParticiper, participantsChip));

        Button btnShowMore = new Button("Show more");
        btnShowMore.getStyleClass().addAll("btn", "btn-ghost", "btn-ghost-small");
        btnShowMore.setOnAction(e -> openEventPublications(event));

        actionsContainer.getChildren().addAll(btnShowMore, btnParticiper);
        content.getChildren().addAll(titleLabel, badgesContainer, dateLabel, infoRow, actionsContainer);
        card.getChildren().addAll(imageContainer, content);

        setupCardHover(card);
        return card;
    }

    protected void playCardsEntranceAnimation() {
        if (flowEvents == null) return;

        SequentialTransition seq = new SequentialTransition();

        for (int i = 0; i < flowEvents.getChildren().size(); i++) {
            Node card = flowEvents.getChildren().get(i);

            card.setOpacity(0);
            card.setTranslateY(16);

            FadeTransition fade = new FadeTransition(Duration.millis(420), card);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition translate = new TranslateTransition(Duration.millis(520), card);
            translate.setFromY(16);
            translate.setToY(0);
            translate.setInterpolator(Interpolator.EASE_BOTH);

            ParallelTransition parallel = new ParallelTransition(fade, translate);
            parallel.setDelay(Duration.millis(i * 70));

            seq.getChildren().add(parallel);
        }

        seq.play();
    }

    private void handleParticiper(Evenement event, Button btnParticiper, Label participantsChip) {
        if (event == null) return;

        User current = Session.getCurrentUser();
        if (current == null) {
            showAlert("Connexion requise", "Veuillez vous connecter pour participer.");
            return;
        }

        int userId = current.getId();
        int eventId = event.getIdEvenement();

        btnParticiper.setDisable(true);
        btnParticiper.setText("...");

        javafx.concurrent.Task<TicketResult> task = new javafx.concurrent.Task<>() {
            @Override
            protected TicketResult call() throws Exception {
                if (participationService.isAlreadyParticipating(userId, eventId)) {
                    throw new IllegalStateException("Vous participez deja a cet evenement.");
                }

                int capaciteMax = participationService.getCapaciteMax(eventId);
                int count = participationService.countParticipants(eventId);
                if (capaciteMax > 0 && count >= capaciteMax) {
                    throw new IllegalStateException("Desole, cet evenement est complet.");
                }

                String ticketId = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                participationService.participer(userId, eventId, ticketId);

                Path ticketsDir = Path.of(System.getProperty("user.home"), "Tickets_Events");
                Files.createDirectories(ticketsDir);
                String safe = ticketId.replaceAll("[^a-zA-Z0-9._-]", "_");
                Path htmlPath = ticketsDir.resolve(safe + ".html");
                Path qrPath = ticketsDir.resolve(safe + "_qr.png");

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                String dateStr = "";
                if (event.getDateDebut() != null) dateStr = event.getDateDebut().format(fmt);
                if (event.getDateFin() != null) dateStr += " - " + event.getDateFin().format(fmt);

                ticketHtmlService.generateHtmlTicket(
                        ticketId,
                        current.getFullName(),
                        current.getEmail(),
                        event.getTitre(),
                        event.getType(),
                        dateStr,
                        event.getLieu(),
                        htmlPath
                );

                // Generate initial QR with file URI (will be replaced by server URL in popup)
                qrService.generateQrPng(htmlPath.toUri().toString(), qrPath);

                return new TicketResult(ticketId, qrPath, htmlPath);
            }
        };

        task.setOnSucceeded(evt -> {
            TicketResult result = task.getValue();

            participatingEventIds.add(eventId);
            participantsByEventId.put(eventId,
                    participantsByEventId.getOrDefault(eventId, 0) + 1);
            updateParticipationUI(event, btnParticiper, participantsChip);

            if (statusLabel != null) statusLabel.setText("Participation enregistree !");

            showQrPopup(result.qrPath, result.htmlPath, result.ticketId);
        });

        task.setOnFailed(evt -> {
            updateParticipationUI(event, btnParticiper, participantsChip);
            Throwable ex = task.getException();
            String msg = ex != null && ex.getMessage() != null ? ex.getMessage() : "Erreur inconnue";
            showAlert("Erreur", msg);
            if (ex != null) ex.printStackTrace();
        });

        Thread t = new Thread(task, "participation-ticket");
        t.setDaemon(true);
        t.start();
    }

    private void showQrPopup(Path qrPath, Path htmlPath, String ticketId) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views_event/qr_popup.fxml"));
            Parent root = loader.load();

            QrPopupController popup = loader.getController();

            Stage stage = new Stage();
            stage.setTitle("Votre Ticket");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();

            // Init AFTER show() so scene is attached
            popup.init(qrPath, htmlPath, ticketId);
            popup.initStage(stage);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ticket genere", "Ticket ID: " + ticketId
                    + "\nFichier: " + htmlPath.toAbsolutePath());
        }
    }

    private static class TicketResult {
        final String ticketId;
        final Path qrPath;
        final Path htmlPath;

        TicketResult(String ticketId, Path qrPath, Path htmlPath) {
            this.ticketId = ticketId;
            this.qrPath = qrPath;
            this.htmlPath = htmlPath;
        }
    }

    private String getBadgeStyleForStatus(String statut) {
        return statut;
    }

    protected ImageView createEventImage(String imageUrl) {
        ImageView imageView = new ImageView();
        imageView.setFitWidth(280);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        imageView.getStyleClass().add("event-thumb");

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                Image image = new Image(imageUrl, true);
                image.errorProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal) showPlaceholder(imageView);
                });
                imageView.setImage(image);
            } catch (Exception e) {
                showPlaceholder(imageView);
            }
        } else {
            showPlaceholder(imageView);
        }
        return imageView;
    }

    protected void showPlaceholder(ImageView imageView) {
        imageView.setImage(null);
    }

    private String formatDateRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        if (start == null && end == null) return "📅 -";
        if (start != null && end != null) {
            java.time.LocalDate s = start.toLocalDate();
            java.time.LocalDate e = end.toLocalDate();
            if (s.equals(e)) return "📅 " + s.format(formatter);
            return "📅 " + s.format(formatter) + " - " + e.format(formatter);
        }
        if (start != null) return "📅 " + start.toLocalDate().format(formatter);
        return "📅 " + end.toLocalDate().format(formatter);
    }

    protected void setupCardHover(VBox card) {
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), card);
        scaleIn.setToX(1.02); scaleIn.setToY(1.02);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(200), card);
        scaleOut.setToX(1.0); scaleOut.setToY(1.0);
        TranslateTransition liftIn = new TranslateTransition(Duration.millis(200), card);
        liftIn.setToY(-4);
        TranslateTransition liftOut = new TranslateTransition(Duration.millis(200), card);
        liftOut.setToY(0);

        card.setOnMouseEntered(e -> { scaleIn.playFromStart(); liftIn.playFromStart(); });
        card.setOnMouseExited(e -> { scaleOut.playFromStart(); liftOut.playFromStart(); });
    }

    private void updateParticipationUI(Evenement event, Button btnParticiper, Label participantsChip) {
        if (event == null || btnParticiper == null) return;

        int id = event.getIdEvenement();
        int capacity = Math.max(0, event.getCapaciteMax());
        int current = participantsByEventId.getOrDefault(id, 0);
        boolean isParticipating = participatingEventIds.contains(id);

        if (participantsChip != null)
            participantsChip.setText("👥 " + current + "/" + capacity);

        btnParticiper.setDisable(false);
        btnParticiper.getStyleClass().removeAll("btn-primary", "btn-danger", "btn-secondary", "btn-outline", "btn-disabled");

        if (capacity > 0 && current >= capacity && !isParticipating) {
            btnParticiper.setText("Complet");
            btnParticiper.getStyleClass().addAll("btn-disabled");
            btnParticiper.setDisable(true);
            return;
        }

        if (isParticipating) {
            btnParticiper.setText("Inscrit");
            btnParticiper.getStyleClass().addAll("btn-disabled");
            btnParticiper.setDisable(true);
            return;
        }

        btnParticiper.setText("Participer");
        btnParticiper.getStyleClass().addAll("btn-primary");
    }

    private void animateCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(30);
        FadeTransition fade = new FadeTransition(Duration.millis(600), card);
        fade.setToValue(1);
        TranslateTransition translate = new TranslateTransition(Duration.millis(600), card);
        translate.setToY(0);
        ParallelTransition parallel = new ParallelTransition(fade, translate);
        parallel.setDelay(Duration.millis(index * 100));
        parallel.play();
    }

    private void animatePageEntrance() {
        if (flowEvents == null) return;
        FadeTransition fade = new FadeTransition(Duration.millis(800), flowEvents);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showEmptyState() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(60));
        emptyBox.getStyleClass().add("empty-state");

        Label emptyIcon = new Label("📅");
        emptyIcon.setStyle("-fx-font-size: 64px;");
        Label emptyTitle = new Label("Aucun evenement trouve");
        emptyTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 600; -fx-text-fill: #374151;");
        Label emptyMessage = new Label("Essayez de modifier vos filtres");
        emptyMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #6B7280;");

        emptyBox.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        flowEvents.getChildren().add(emptyBox);
    }

    protected void openEventForm(Evenement event) {
        try {
            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_form.fxml", controller -> {
                    if (controller instanceof EventFormController && event != null)
                        ((EventFormController) controller).setEvent(event);
                });
                return;
            }
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_form.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventFormController && event != null)
                ((EventFormController) controller).setEvent(event);

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle(event == null ? "Nouvel Evenement" : "Modifier l'Evenement");
            modalStage.setScene(new Scene(root, 800, 600));
            modalStage.showAndWait();
            loadEvents();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    protected void openEventPublications(Evenement event) {
        try {
            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_publications.fxml", controller -> {
                    if (controller instanceof EventPublicationsController)
                        ((EventPublicationsController) controller).setEventId(event.getIdEvenement());
                });
                return;
            }

            if (Navigation.hasContentArea()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_publications.fxml"));
                Parent root = loader.load();
                Object controller = loader.getController();
                if (controller instanceof EventPublicationsController)
                    ((EventPublicationsController) controller).setEventId(event.getIdEvenement());
                Navigation.setContent(root);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_publications.fxml"));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof EventPublicationsController)
                ((EventPublicationsController) controller).setEvenementId(event.getIdEvenement());

            Stage stage = (Stage) btnRefresh.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) stage.setScene(new Scene(root, 1300, 780));
            else scene.setRoot(root);
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir les publications: " + e.getMessage());
        }
    }

    protected void deleteEvent(Evenement event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer l'evenement");
        confirm.setContentText("Etes-vous sur de vouloir supprimer \"" + event.getTitre() + "\" ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                evenementService.supprimerEvenement(event.getIdEvenement());
                if (statusLabel != null) statusLabel.setText("Evenement supprime");
                refreshEvents();
            } catch (Exception e) {
                showAlert("Erreur", "Impossible de supprimer l'evenement: " + e.getMessage());
            }
        }
    }

    private void updateResultCount() {
        if (resultCount == null) return;
        int count = filteredEvents.size();
        resultCount.setText(count + " evenement" + (count > 1 ? "s" : "") + " trouve" + (count > 1 ? "s" : ""));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
