package Controllers.evenement;

import Entities.evenement.Evenement;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import Services.evenement.AiAnalyticsService;
import Services.evenement.AiActionService;
import Controllers.evenement.OpenAIClient;
import Controllers.evenement.AiRequestParser;

public class EventAffichageBackendController extends EventAffichageController {

    // Statistics UI Components
    @FXML
    private Label kpiTotalEvents;
    @FXML private Label kpiTotalEventsTrend;
    @FXML private Label kpiPublishedEvents;
    @FXML private Label kpiPublishedEventsTrend;
    @FXML private Label kpiDraftEvents;
    @FXML private Label kpiDraftEventsTrend;
    @FXML private Label kpiCancelledEvents;
    @FXML private Label kpiCancelledEventsTrend;
    @FXML private Label kpiTotalCapacity;
    @FXML private Label kpiTotalCapacityTrend;
    @FXML private PieChart pieChartStatus;
    @FXML private BarChart<String, Number> barChartType;
    // Use parent's FlowPane field instead of TilePane
    // @FXML private TilePane tileEvents;

    // AI Command Bar
    @FXML private StackPane aiAvatar;
    @FXML private ImageView ivAiAvatar;
    @FXML private TextField tfAiCommand;
    @FXML private Button btnAiSend;
    @FXML private VBox aiResponseContainer;
    @FXML private Label lblAiResponse;

    private Timeline typingTimeline;

    private final OpenAIClient openAIClient = new OpenAIClient();

    private final AiRequestParser aiRequestParser = new AiRequestParser();
    private final AiAnalyticsService aiAnalyticsService = new AiAnalyticsService();
    private final AiActionService aiActionService = new AiActionService();

    @FXML
    public void initialize() {
        super.initialize();
        setupAiCommandBar();
    }

    @Override
    protected void setupButtonActions() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> {
                if (statusLabel != null) statusLabel.setText("Actualisation...");
                loadEvents();
            });
        }

        // Override the btnNew action to work with NavigationService or Nav
        if (btnNew != null) {
            // Always show the button in backend
            btnNew.setVisible(true);
            btnNew.setManaged(true);
            btnNew.setOnAction(e -> openEventForm(null));
        }

        // Setup search listener for backend filtering
        if (tfSearch != null) {
            tfSearch.textProperty().addListener((obs, oldVal, newVal) -> applyBackendFilters());
        }
    }

    // ===== AI command action wrappers (required signatures) =====

    private void deleteEvent(int id) {
        deleteEvent(id, "");
    }

    private void deleteEvent(int id, String assistantText) {
        Evenement event = findEventById(id);
        if (event == null) {
            showAiResponse(assistantText + "\n\nEvent id=" + id + " not found.");
            return;
        }
        if (!confirmDelete(event)) {
            showAiResponse(assistantText + "\n\nDelete cancelled.");
            return;
        }
        deleteEvent(event);
    }

    private void openEvent(int id) {
        openEvent(id, "");
    }

    private void openEvent(int id, String assistantText) {
        Evenement event = findEventById(id);
        if (event == null) {
            showAiResponse(assistantText + "\n\nEvent id=" + id + " not found.");
            return;
        }
        openEventPublications(event);
    }

    private void openAddEventForm() {
        openEventForm(null);
    }

    private void setupAiCommandBar() {
        if (btnAiSend != null) {
            btnAiSend.setOnAction(e -> handleAiSend());
        }

        if (tfAiCommand != null) {
            tfAiCommand.setOnAction(e -> handleAiSend());
            tfAiCommand.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    handleAiSend();
                    e.consume();
                }
            });
        }

        if (aiAvatar != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.millis(1200), aiAvatar);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.06);
            pulse.setToY(1.06);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(ScaleTransition.INDEFINITE);
            pulse.play();
        }
    }

    private void handleAiSend() {
        if (tfAiCommand == null) return;

        String question = tfAiCommand.getText() != null ? tfAiCommand.getText().trim() : "";
        if (question.isEmpty()) {
            showAiResponse("Type a command first.");
            return;
        }

        tfAiCommand.clear();
        showAiTyping();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String schemaContext = AiDbContextProvider.schemaContext();

                // STEP 1: plan
                String planText = openAIClient.planDbQuestion(question, schemaContext);

                AiRequestParser.AiRequest req = aiRequestParser.parse(planText);

                if (req.kind == AiRequestParser.Kind.CLARIFY) {
                    String clarify = AiRequestParser.extractClarifyQuestion(req.descriptionLine);
                    if (clarify == null || clarify.isBlank()) clarify = planText;
                    return "CLARIFY\n" + clarify;
                }

                String executionResults;
                if (req.kind == AiRequestParser.Kind.DATA_REQUEST) {
                    executionResults = aiAnalyticsService.executeRead(req);
                } else if (req.kind == AiRequestParser.Kind.ACTION_REQUEST) {
                    // Confirmation must happen on FX thread; return plan for UI to confirm.
                    return "ACTION_CONFIRM\n" + planText;
                } else {
                    executionResults = "Invalid plan format.\n\n" + planText;
                }

                // STEP 2: final answer
                String finalAnswer = openAIClient.finalAnswerDbQuestion(question, planText, executionResults);
                return finalAnswer;
            }
        };

        task.setOnSucceeded(evt -> {
            String result = task.getValue();
            if (result != null && result.startsWith("CLARIFY\n")) {
                showAiResponse(result.substring("CLARIFY\n".length()).trim());
                return;
            }

            if (result != null && result.startsWith("ACTION_CONFIRM\n")) {
                String planText = result.substring("ACTION_CONFIRM\n".length());
                handleActionRequestWithConfirmation(question, planText);
                return;
            }

            showAiResponse(result);
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            String msg = ex != null ? ex.getMessage() : "Unknown error";
            showAiResponse("OpenAI error: " + msg);
        });

        Thread t = new Thread(task, "openai-admin-command");
        t.setDaemon(true);
        t.start();
    }

    private void handleActionRequestWithConfirmation(String question, String planText) {
        AiRequestParser.AiRequest req = aiRequestParser.parse(planText);
        if (req.kind != AiRequestParser.Kind.ACTION_REQUEST) {
            showAiResponse("Invalid ACTION_REQUEST plan.\n\n" + planText);
            return;
        }

        String action = req.get("ACTION");
        String params = req.get("PARAMS");

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Execute this database action?");
        alert.setContentText((action != null ? action : "") + (params != null ? "\n" + params : ""));

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) {
            showAiResponse("Action cancelled.\n\n" + planText);
            return;
        }

        showAiTyping();

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                String executionResults = aiActionService.executeAction(req);
                return openAIClient.finalAnswerDbQuestion(question, planText, executionResults);
            }
        };

        task.setOnSucceeded(e -> showAiResponse(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showAiResponse("Action error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread t = new Thread(task, "openai-db-action");
        t.setDaemon(true);
        t.start();
    }

    private void showAiTyping() {
        if (aiResponseContainer == null || lblAiResponse == null) return;
        aiResponseContainer.setVisible(true);
        aiResponseContainer.setManaged(true);
        startTypingAnimation();
        fadeInNode(aiResponseContainer);
    }

    private void showAiResponse(String response) {
        if (aiResponseContainer == null || lblAiResponse == null) return;
        aiResponseContainer.setVisible(true);
        aiResponseContainer.setManaged(true);
        stopTypingAnimation();
        lblAiResponse.setText(response != null ? response : "");
        fadeInNode(aiResponseContainer);
    }

    private void startTypingAnimation() {
        stopTypingAnimation();
        if (lblAiResponse == null) return;

        lblAiResponse.setText("Typing");
        typingTimeline = new Timeline(
                new KeyFrame(Duration.millis(0), e -> lblAiResponse.setText("Typing")),
                new KeyFrame(Duration.millis(350), e -> lblAiResponse.setText("Typing.")),
                new KeyFrame(Duration.millis(700), e -> lblAiResponse.setText("Typing..")),
                new KeyFrame(Duration.millis(1050), e -> lblAiResponse.setText("Typing..."))
        );
        typingTimeline.setCycleCount(Timeline.INDEFINITE);
        typingTimeline.play();
    }

    private void stopTypingAnimation() {
        if (typingTimeline != null) {
            typingTimeline.stop();
            typingTimeline = null;
        }
    }

    // Previous ACTION-line command execution has been replaced by the 2-step DB analyst flow.

    private void fadeInNode(javafx.scene.Node node) {
        if (node == null) return;
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private Map<String, Long> countByStatus() {
        Map<String, Long> out = new HashMap<>();
        for (Evenement e : getAllEvents()) {
            String s = e != null && e.getStatut() != null ? e.getStatut().trim() : "";
            if (s.isEmpty()) s = "(vide)";
            out.put(s, out.getOrDefault(s, 0L) + 1L);
        }
        return out;
    }

    private Evenement findEventById(int id) {
        for (Evenement e : getAllEvents()) {
            if (e != null && e.getIdEvenement() == id) return e;
        }
        return null;
    }

    private boolean confirmDelete(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Delete event #" + event.getIdEvenement() + "?");
        alert.setContentText(safe(event.getTitre()));
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private Evenement getMostParticipatedEvent() {
        Map<Integer, Integer> map = getParticipantsMap();
        if (map.isEmpty()) return null;

        int bestId = -1;
        int bestCount = -1;
        for (Map.Entry<Integer, Integer> e : map.entrySet()) {
            int c = e.getValue() != null ? e.getValue() : 0;
            if (c > bestCount) {
                bestCount = c;
                bestId = e.getKey();
            }
        }
        return bestId > 0 ? findEventById(bestId) : null;
    }

    private int getParticipantCount(int eventId) {
        Map<Integer, Integer> map = getParticipantsMap();
        Integer v = map.get(eventId);
        return v != null ? v : 0;
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> getParticipantsMap() {
        try {
            java.lang.reflect.Field field = EventAffichageController.class.getDeclaredField("participantsByEventId");
            field.setAccessible(true);
            Object v = field.get(this);
            if (v instanceof Map) {
                return (Map<Integer, Integer>) v;
            }
        } catch (Exception ignored) {
        }
        return new HashMap<>();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void loadEvents() {
        super.loadEvents(); // Let parent handle the loading
        // Update statistics after loading
        updateStats(getFilteredEvents());
        // Render with our TilePane
        renderBackendCards();
    }

    /**
     * Update statistics based on the events list
     */
    private void updateStats(List<Evenement> events) {
        if (events == null) return;

        // Calculate KPIs
        long totalEvents = events.size();
        long publishedEvents = events.stream()
                .filter(e -> "PUBLIÉ".equalsIgnoreCase(e.getStatut()) || "PUBLIE".equalsIgnoreCase(e.getStatut()))
                .count();
        long draftEvents = events.stream()
                .filter(e -> "BROUILLON".equalsIgnoreCase(e.getStatut()) || "BROUILLON".equalsIgnoreCase(e.getStatut()))
                .count();
        long cancelledEvents = events.stream()
                .filter(e -> "ANNULÉ".equalsIgnoreCase(e.getStatut()) || "ANNULE".equalsIgnoreCase(e.getStatut()))
                .count();
        
        int totalCapacity = events.stream()
                .mapToInt(e -> {
                    Integer capacite = e.getCapaciteMax();
                    return capacite != null ? capacite : 0;
                })
                .sum();

        // Update KPI labels
        if (kpiTotalEvents != null) {
            kpiTotalEvents.setText(String.valueOf(totalEvents));
            kpiTotalEventsTrend.setText("+0 cette semaine"); // TODO: Calculate actual trend
        }
        
        if (kpiPublishedEvents != null) {
            kpiPublishedEvents.setText(String.valueOf(publishedEvents));
            kpiPublishedEventsTrend.setText("+0 cette semaine"); // TODO: Calculate actual trend
        }
        
        if (kpiDraftEvents != null) {
            kpiDraftEvents.setText(String.valueOf(draftEvents));
            kpiDraftEventsTrend.setText("+0 cette semaine"); // TODO: Calculate actual trend
        }
        
        if (kpiCancelledEvents != null) {
            kpiCancelledEvents.setText(String.valueOf(cancelledEvents));
            kpiCancelledEventsTrend.setText("+0 cette semaine"); // TODO: Calculate actual trend
        }
        
        if (kpiTotalCapacity != null) {
            kpiTotalCapacity.setText(String.valueOf(totalCapacity));
            kpiTotalCapacityTrend.setText("+0 cette semaine"); // TODO: Calculate actual trend
        }

        // Update Pie Chart - Status Distribution
        updateStatusPieChart(events);
        
        // Update Bar Chart - Type Distribution
        updateTypeBarChart(events);
    }

    private void updateStatusPieChart(List<Evenement> events) {
        if (pieChartStatus == null) return;

        Map<String, Long> statusCount = events.stream()
                .collect(Collectors.groupingBy(
                    e -> normalizeStatusForChart(e.getStatut()),
                    Collectors.counting()
                ));

        pieChartStatus.getData().clear();
        
        // Define colors for each status
        Map<String, String> statusColors = Map.of(
            "Publiés", "#10B981",
            "Brouillons", "#F59E0B", 
            "Annulés", "#EF4444"
        );

        for (Map.Entry<String, Long> entry : statusCount.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            pieChartStatus.getData().add(data);
            
            // Apply custom colors (this would need additional styling in CSS)
            // Note: JavaFX PieChart color customization is limited, would need custom approach
        }
    }

    private void updateTypeBarChart(List<Evenement> events) {
        if (barChartType == null) return;

        Map<String, Long> typeCount = events.stream()
                .collect(Collectors.groupingBy(
                    e -> e.getType() != null ? e.getType() : "Autre",
                    Collectors.counting()
                ));

        barChartType.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Nombre d'événements");

        for (Map.Entry<String, Long> entry : typeCount.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        barChartType.getData().add(series);
    }

    private String normalizeStatusForChart(String status) {
        if (status == null) return "Autre";
        String s = status.trim().toLowerCase();
        if (s.contains("publ") || s.contains("actif") || s.contains("active")) return "Publiés";
        if (s.contains("brou") || s.contains("draft")) return "Brouillons";
        if (s.contains("annul") || s.contains("cancel")) return "Annulés";
        return "Autre";
    }

    /**
     * Apply filters for backend - simplified search only
     */
    private void applyBackendFilters() {
        // For backend, we'll use a simplified filtering without date filters
        String searchText = tfSearch != null && tfSearch.getText() != null
                ? tfSearch.getText().toLowerCase() : "";

        // Create filtered list from all events
        List<Evenement> filtered = getAllEvents().stream()
                .filter(event -> {
                    boolean matchesSearch = searchText.isEmpty()
                            || (event.getTitre() != null && event.getTitre().toLowerCase().contains(searchText))
                            || (event.getLieu() != null && event.getLieu().toLowerCase().contains(searchText))
                            || (event.getType() != null && event.getType().toLowerCase().contains(searchText))
                            || (event.getStatut() != null && event.getStatut().toLowerCase().contains(searchText));

                    return matchesSearch;
                })
                .collect(Collectors.toList());

        // Update the filtered events list
        getFilteredEvents().clear();
        getFilteredEvents().addAll(FXCollections.observableArrayList(filtered));

        renderBackendCards();
        updateResultCount();
        
        // Update statistics with filtered events
        updateStats(getFilteredEvents());
    }

    /**
     * Get all events from parent controller
     */
    private ObservableList<Evenement> getAllEvents() {
        try {
            // Use reflection to access private field as last resort
            java.lang.reflect.Field field = EventAffichageController.class.getDeclaredField("allEvents");
            field.setAccessible(true);
            return (ObservableList<Evenement>) field.get(this);
        } catch (Exception e) {
            return FXCollections.observableArrayList();
        }
    }

    /**
     * Get filtered events from parent controller
     */
    private ObservableList<Evenement> getFilteredEvents() {
        try {
            // Use reflection to access private field as last resort
            java.lang.reflect.Field field = EventAffichageController.class.getDeclaredField("filteredEvents");
            field.setAccessible(true);
            return (ObservableList<Evenement>) field.get(this);
        } catch (Exception e) {
            return FXCollections.observableArrayList();
        }
    }

    /**
     * Render cards for backend using FlowPane
     */
    private void renderBackendCards() {
        if (flowEvents == null) return;
        flowEvents.getChildren().clear();

        ObservableList<Evenement> events = getFilteredEvents();
        if (events.isEmpty()) {
            showEmptyState();
            return;
        }

        for (int i = 0; i < events.size(); i++) {
            VBox card = createBackendEventCard(events.get(i));
            card.setPrefWidth(360);
            flowEvents.getChildren().add(card);
            animateCardEntrance(card, i);
        }
    }

    /**
     * Update result count display
     */
    private void updateResultCount() {
        if (resultCount != null) {
            int count = getFilteredEvents().size();
            resultCount.setText(count + " événement" + (count != 1 ? "s" : ""));
        }
    }

    private void showEmptyState() {
        if (flowEvents == null) return;
        
        VBox emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setStyle("-fx-padding: 60;");
        
        Label emptyIcon = new Label("📋");
        emptyIcon.setStyle("-fx-font-size: 48px;");
        
        Label emptyTitle = new Label("Aucun événement trouvé");
        emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: #64748B;");
        
        Label emptyMessage = new Label("Essayez de modifier votre recherche ou ajoutez un nouvel événement.");
        emptyMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: #94A3B8; -fx-wrap-text: true;");
        
        emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptyMessage);
        flowEvents.getChildren().add(emptyState);
    }

    @Override
    protected void openEventPublications(Evenement event) {
        try {
            if (event == null) return;

            int eventId = event.getIdEvenement();

            // Always try to open in the same window/content area first
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/event_publications_backend.fxml", controller -> {
                    if (controller instanceof EventPublicationsBackendController) {
                        ((EventPublicationsBackendController) controller).setEventId(eventId);
                    }
                });
                return;
            }

            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_publications_backend.fxml", controller -> {
                    if (controller instanceof EventPublicationsBackendController) {
                        ((EventPublicationsBackendController) controller).setEventId(eventId);
                    }
                });
                return;
            }

            if (Navigation.hasContentArea()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_publications_backend.fxml"));
                Parent root = loader.load();

                Object controller = loader.getController();
                if (controller instanceof EventPublicationsBackendController) {
                    ((EventPublicationsBackendController) controller).setEventId(eventId);
                }

                Navigation.setContent(root);
                return;
            }

            // Fallback: still open in the SAME window by swapping the current Scene root
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_publications_backend.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventPublicationsBackendController) {
                ((EventPublicationsBackendController) controller).setEventId(eventId);
            }

            Stage stage = null;
            if (btnNew != null && btnNew.getScene() != null && btnNew.getScene().getWindow() instanceof Stage) {
                stage = (Stage) btnNew.getScene().getWindow();
            } else if (flowEvents != null && flowEvents.getScene() != null && flowEvents.getScene().getWindow() instanceof Stage) {
                stage = (Stage) flowEvents.getScene().getWindow();
            }

            if (stage == null) {
                throw new IllegalStateException("Cannot resolve current Stage to open publications in-place");
            }

            Scene current = stage.getScene();
            if (current == null) {
                stage.setScene(new Scene(root, 1300, 780));
            } else {
                current.setRoot(root);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir les publications: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create premium event card for backend
     */
    private VBox createBackendEventCard(Evenement event) {
        VBox card = new VBox(16);
        card.getStyleClass().add("event-card");
        card.setPrefWidth(360);

        // Image container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("event-image-container");
        
        ImageView imageView = createEventImage(event.getImageUrl());
        imageView.getStyleClass().add("event-cover");
        imageView.setFitWidth(360);
        imageView.setFitHeight(160);
        
        Rectangle clip = new Rectangle(360, 160);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        imageView.setClip(clip);
        
        imageContainer.getChildren().add(imageView);

        // Status badge
        Label statusBadge = new Label(normalizeStatusLabel(event.getStatut()));
        statusBadge.getStyleClass().addAll("status-badge", mapStatusToBadgeClass(event.getStatut()));
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(12, 12, 0, 0));
        imageContainer.getChildren().add(statusBadge);

        // Content container
        VBox content = new VBox(12);
        content.getStyleClass().add("event-card-content");
        content.setAlignment(Pos.TOP_LEFT);
        content.setPadding(new Insets(20));

        // Title
        Label titleLabel = new Label(event.getTitre() != null ? event.getTitre() : "");
        titleLabel.getStyleClass().add("event-title");
        titleLabel.setWrapText(true);

        // Meta information
        Label metaLabel = new Label(buildMetaLine(event));
        metaLabel.getStyleClass().add("event-meta");

        // Date information
        Label dateLabel = new Label(formatDateRange(event.getDateDebut(), event.getDateFin()));
        dateLabel.getStyleClass().add("event-date");

        // Actions container
        HBox actionsContainer = new HBox(8);
        actionsContainer.setAlignment(Pos.CENTER_RIGHT);
        actionsContainer.getStyleClass().add("event-actions");

        Button btnVoir = new Button("Voir");
        btnVoir.getStyleClass().add("btn-view");
        btnVoir.setOnAction(e -> openEventPublications(event));
        btnVoir.setOnMouseClicked(e -> e.consume());

        Button btnModifier = new Button("Modifier");
        btnModifier.getStyleClass().add("btn-edit");
        btnModifier.setOnAction(e -> openEventForm(event));
        btnModifier.setOnMouseClicked(e -> e.consume());

        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().add("btn-delete");
        btnDelete.setOnAction(e -> deleteEvent(event));
        btnDelete.setOnMouseClicked(e -> e.consume());

        actionsContainer.getChildren().addAll(btnVoir, btnModifier, btnDelete);

        content.getChildren().addAll(titleLabel, metaLabel, dateLabel, actionsContainer);
        card.getChildren().addAll(imageContainer, content);

        // Click on card background opens publications
        card.setOnMouseClicked(e -> openEventPublications(event));

        setupCardHover(card);
        return card;
    }

    private String buildMetaLine(Evenement event) {
        String type = event.getType() != null ? event.getType() : "-";
        String dates = formatDateRange(event.getDateDebut(), event.getDateFin());
        return type + " • " + dates;
    }

    private String formatDateRange(LocalDateTime start, LocalDateTime end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        if (start == null && end == null) return "-";
        if (start != null && end != null) {
            if (start.toLocalDate().equals(end.toLocalDate())) {
                return start.toLocalDate().format(formatter);
            }
            return start.toLocalDate().format(formatter) + " → " + end.toLocalDate().format(formatter);
        }
        if (start != null) return start.toLocalDate().format(formatter);
        return end != null ? end.toLocalDate().format(formatter) : "-";
    }

    private String mapStatusToBadgeClass(String statut) {
        if (statut == null) return "status-draft";
        String s = statut.trim().toLowerCase();
        if (s.contains("publ") || s.contains("actif") || s.contains("active")) return "status-published";
        if (s.contains("term")) return "status-published";
        if (s.contains("annul") || s.contains("cancel")) return "status-cancelled";
        return "status-draft";
    }

    private String normalizeStatusLabel(String statut) {
        if (statut == null || statut.trim().isEmpty()) return "-";
        return statut.trim();
    }

    @Override
    protected void openEventForm(Evenement event) {
        try {
            // Always try to open in the same window/content area first
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/event_form.fxml", controller -> {
                    if (controller instanceof EventFormController && event != null) {
                        ((EventFormController) controller).setEvent(event);
                    }
                });
                return;
            }

            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_form.fxml", controller -> {
                    if (controller instanceof EventFormController && event != null) {
                        ((EventFormController) controller).setEvent(event);
                    }
                });
                return;
            }

            if (Navigation.hasContentArea()) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_form.fxml"));
                Parent root = loader.load();

                Object controller = loader.getController();
                if (controller instanceof EventFormController && event != null) {
                    ((EventFormController) controller).setEvent(event);
                }

                Navigation.setContent(root);
                return;
            }

            // Fallback: still open in the SAME window by swapping the current Scene root
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/event_form.fxml"));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof EventFormController && event != null) {
                ((EventFormController) controller).setEvent(event);
            }

            Stage stage = null;
            if (btnNew != null && btnNew.getScene() != null && btnNew.getScene().getWindow() instanceof Stage) {
                stage = (Stage) btnNew.getScene().getWindow();
            }

            if (stage == null) {
                throw new IllegalStateException("Cannot resolve current Stage to open event form in-place");
            }

            Scene current = stage.getScene();
            if (current == null) {
                stage.setScene(new Scene(root, 1300, 780));
            } else {
                current.setRoot(root);
            }
            stage.setTitle(event == null ? "Nouvel Événement" : "Modifier l'Événement");
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Animate card entrance with fade effect
     */
    private void animateCardEntrance(VBox card, int index) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), card);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setDelay(Duration.millis(index * 100));
        fade.play();
    }
}
