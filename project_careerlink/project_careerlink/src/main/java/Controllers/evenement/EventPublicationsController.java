package Controllers.evenement;

import Entities.evenement.Evenement;
import Entities.evenement.Publication;
import Entities.User.User;
import Services.evenement.EvenementService;
import Services.evenement.PublicationService;
import Services.User.UserService;
import Controllers.evenement.NavigationService;
import Utils.Session;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.Desktop;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class EventPublicationsController {

    @FXML private ImageView imgCover;
    @FXML private Label lblEventTitle;
    @FXML private Label lblEventMeta;
    @FXML private Label lblEventDates;
    @FXML private Label lblEventLocation;
    @FXML private VBox feedBox;
    @FXML private Button btnAddPublication;
    @FXML private Button btnBack;
    @FXML private Button btnParticipate;
    @FXML private Button btnShare;
    @FXML private ScrollPane feedScrollPane;

    private final PublicationService publicationService = new PublicationService();
    private final EvenementService evenementService = new EvenementService();
    private final UserService userService = new UserService();

    private int evenementId;
    private Evenement evenement;

    /**
     * Get author information by ID
     */
    private User getAuthorById(int authorId) {
        try {
            return userService.getUserById(authorId);
        } catch (Exception e) {
            System.err.println("Error fetching author info: " + e.getMessage());
            return null;
        }
    }

    private void loadPublications() {
        refresh();
    }

    public void setEventId(int id) {
        setEvenementId(id);
        loadPublications();
    }

    public void setEvenementId(int id) {
        this.evenementId = id;
        loadHeader();
        refresh();
    }

    @FXML
    public void initialize() {
        if (feedBox != null) {
            feedBox.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(200), feedBox);
            ft.setToValue(1);
            ft.play();
        }

        if (btnAddPublication != null) {
            btnAddPublication.setOnMouseEntered(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), btnAddPublication);
                st.setToX(1.02);
                st.setToY(1.02);
                st.play();
            });
            btnAddPublication.setOnMouseExited(e -> {
                ScaleTransition st = new ScaleTransition(Duration.millis(120), btnAddPublication);
                st.setToX(1.0);
                st.setToY(1.0);
                st.play();
            });
        }
    }

    @FXML
    private void refresh() {
        try {
            // Load publications for selected event
            List<Publication> pubs = publicationService.afficherPublicationsParEvenement(evenementId);
            pubs.sort(Comparator.comparing(Publication::getDatePublication, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

            feedBox.getChildren().clear();

            if (pubs.isEmpty()) {
                Label empty = new Label("Aucune publication pour le moment");
                empty.getStyleClass().add("ep-empty");
                feedBox.getChildren().add(empty);
                return;
            }

            int i = 0;
            for (Publication p : pubs) {
                VBox card = buildPublicationCard(p);
                feedBox.getChildren().add(card);
                playCardEntrance(card, i++);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les publications: " + e.getMessage());
        }
    }

    @FXML
    private void openCreateForm() {
        openForm(null);
    }

    private void openForm(Publication toEdit) {
        try {
            // Prefer in-dashboard navigation (same window)
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/publication_form.fxml", c -> {
                    if (c instanceof PublicationFormController controller) {
                        controller.setEvenementId(evenementId);
                        controller.setReturnView("/views_event/event_publications.fxml");
                        if (toEdit != null) {
                            controller.setPublicationToEdit(toEdit);
                        } else {
                            controller.setModeAdd();
                        }
                    }
                });
                return;
            }

            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/publication_form.fxml", c -> {
                    if (c instanceof PublicationFormController controller) {
                        controller.setEvenementId(evenementId);
                        controller.setReturnView("/views_event/event_publications.fxml");
                        if (toEdit != null) {
                            controller.setPublicationToEdit(toEdit);
                        } else {
                            controller.setModeAdd();
                        }
                    }
                });
                return;
            }

            // Fallback: open as modal window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/publication_form.fxml"));
            Parent root = loader.load();
            PublicationFormController controller = loader.getController();
            controller.setEvenementId(evenementId);
            controller.setReturnView("/views_event/event_publications.fxml");
            if (toEdit != null) {
                controller.setPublicationToEdit(toEdit);
            } else {
                controller.setModeAdd();
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(toEdit == null ? "New Publication" : "Edit Publication");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            refresh();
        } catch (Exception e) {
            System.err.println("Detailed error loading publication form:");
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @FXML
    private void backToEvents() {
        try {
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/event_affichage.fxml");
                return;
            }

            if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/event_affichage.fxml");
                return;
            }

            if (Navigation.hasContentArea()) {
                Navigation.navigate("/views_event/event_affichage.fxml");
                return;
            }
        } catch (Exception e) {
            showAlert("Erreur", "Navigation impossible: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        backToEvents();
    }

    private void loadHeader() {
        try {
            if (evenementId <= 0) {
                lblEventTitle.setText("All Publications");
                lblEventMeta.setText("");
                lblEventDates.setText("");
                return;
            }

            evenement = evenementService.afficherEvenements().stream()
                    .filter(e -> e.getIdEvenement() == evenementId)
                    .findFirst()
                    .orElse(null);

            if (evenement == null) {
                lblEventTitle.setText("All Publications");
                lblEventMeta.setText("");
                lblEventDates.setText("");
                return;
            }

            lblEventTitle.setText(safe(evenement.getTitre()));
            String meta = safe(evenement.getType()) + " • " + safe(evenement.getStatut());
            lblEventMeta.setText(meta);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm");
            String d1 = evenement.getDateDebut() != null ? evenement.getDateDebut().format(fmt) : "?";
            String d2 = evenement.getDateFin() != null ? evenement.getDateFin().format(fmt) : "?";
            lblEventDates.setText(d1 + " → " + d2);
            
            lblEventLocation.setText("📍 " + safe(evenement.getLieu()));

            String imageUrl = evenement.getImageUrl();
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                try {
                    imgCover.setImage(new Image(imageUrl, true));
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox buildPublicationCard(Publication p) {
        VBox card = new VBox(12);
        card.getStyleClass().add("publication-card");

        // Header with avatar, author info, and menu
        HBox header = new HBox(12);
        header.getStyleClass().add("pub-header");

        // Author info
        VBox authorInfo = new VBox(2);
        authorInfo.getStyleClass().add("pub-author-info");
        
        // Get author information
        User author = getAuthorById(p.getAuteurId());
        String authorName = author != null ? author.getFullName() : "Utilisateur #" + p.getAuteurId();
        
        Label authorLabel = new Label(authorName);
        authorLabel.getStyleClass().add("pub-author-name");
        
        Label date = new Label(p.getDatePublication() != null
                ? p.getDatePublication().format(DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm"))
                : "");
        date.getStyleClass().add("pub-time");
        
        authorInfo.getChildren().addAll(authorLabel, date);

        // Avatar with profile photo
        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("pub-avatar");
        avatarContainer.setPrefSize(40, 40);
        
        // Create profile photo ImageView
        ImageView profilePhoto = new ImageView();
        profilePhoto.setFitWidth(40);
        profilePhoto.setFitHeight(40);
        profilePhoto.setPreserveRatio(true);
        profilePhoto.setSmooth(true);
        
        // Set profile photo or default avatar
        if (author != null && author.getProfilePhoto() != null && !author.getProfilePhoto().trim().isEmpty()) {
            try {
                profilePhoto.setImage(new Image(author.getProfilePhoto(), true));
            } catch (Exception e) {
                // Fallback to default avatar if image fails to load
                profilePhoto.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 20;");
            }
        } else {
            // Default avatar background
            profilePhoto.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 20;");
        }
        
        // Create circular clip for avatar
        Circle clip = new Circle(20);
        clip.setCenterX(20);
        clip.setCenterY(20);
        profilePhoto.setClip(clip);
        
        avatarContainer.getChildren().add(profilePhoto);

        // Menu button
        Button menuBtn = new Button("⋮");
        menuBtn.getStyleClass().add("pub-menu-btn");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(avatarContainer, authorInfo, spacer, menuBtn);

        // Content section
        VBox content = new VBox(12);
        content.getStyleClass().add("pub-content");

        // Title if exists
        if (p.getTitre() != null && !p.getTitre().trim().isEmpty()) {
            Label title = new Label(p.getTitre());
            title.getStyleClass().add("pub-text");
            title.setWrapText(true);
            content.getChildren().add(title);
        }

        // Main text content
        Label textContent = new Label(p.getContenu() != null ? p.getContenu() : "");
        textContent.getStyleClass().add("pub-text");
        textContent.setWrapText(true);
        content.getChildren().add(textContent);

        // Event reference if linked
        if (p.getEvenementId() != null && p.getEvenementId() > 0) {
            try {
                Evenement linkedEvent = evenementService.afficherEvenements().stream()
                        .filter(e -> e.getIdEvenement() == p.getEvenementId())
                        .findFirst()
                        .orElse(null);
                if (linkedEvent != null) {
                    Label eventLabel = new Label("📅 " + safe(linkedEvent.getTitre()));
                    eventLabel.getStyleClass().add("pub-text");
                    eventLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #3B82F6;");
                    content.getChildren().add(0, eventLabel);
                }
            } catch (Exception ignored) {}
        }

        // Media content
        if (p.getImageUrl() != null && !p.getImageUrl().trim().isEmpty()) {
            ImageView media = new ImageView();
            media.getStyleClass().add("pub-media");
            media.setPreserveRatio(true);
            media.setFitHeight(300);
            media.setSmooth(true);
            media.setCache(true);
            try {
                Image img = new Image(p.getImageUrl(), true);
                media.setImage(img);
                media.setOnMouseClicked(e -> openLink(p.getImageUrl()));
                media.setCursor(javafx.scene.Cursor.HAND);
                content.getChildren().add(media);
            } catch (Exception ignored) {}
        }

        // Video content
        if (p.getVideoUrl() != null && !p.getVideoUrl().trim().isEmpty()) {
            VBox videoContainer = new VBox(8);
            videoContainer.getStyleClass().add("pub-video-container");
            
            Label videoLabel = new Label("📹 Vidéo");
            videoLabel.getStyleClass().add("pub-text");
            videoLabel.setStyle("-fx-font-weight: 600;");
            
            StackPane videoArea = new StackPane();
            videoArea.setPrefHeight(240);
            videoArea.setMaxWidth(600);
            videoArea.getStyleClass().add("pub-media");
            
            ImageView videoThumb = new ImageView();
            videoThumb.setFitHeight(240);
            videoThumb.setFitWidth(600);
            videoThumb.setPreserveRatio(false);
            videoThumb.setOpacity(0.8);
            
            try {
                videoThumb.setImage(new Image("https://via.placeholder.com/600x240/1D4ED8/FFFFFF?text=Video+Click+to+Play", true));
            } catch (Exception ignored) {
                videoThumb.setStyle("-fx-background-color: #1D4ED8;");
            }
            
            Circle playCircle = new Circle(35, Color.WHITE);
            playCircle.setOpacity(0.9);
            
            Polygon playTriangle = new Polygon();
            playTriangle.getPoints().addAll(-12.0, -18.0, -12.0, 18.0, 18.0, 0.0);
            playTriangle.setFill(Color.web("#1D4ED8"));
            playTriangle.setTranslateX(4);
            
            StackPane playButton = new StackPane(playCircle, playTriangle);
            
            videoArea.getChildren().addAll(videoThumb, playButton);
            StackPane.setAlignment(playButton, javafx.geometry.Pos.CENTER);
            
            videoArea.setOnMouseClicked(evt -> {
                try {
                    String videoPath = p.getVideoUrl();
                    if (videoPath.startsWith("file:/")) {
                        videoPath = videoPath.replace("file:/", "").replace("/", "\\");
                        if (videoPath.startsWith("\\")) videoPath = videoPath.substring(1);
                    }
                    
                    java.io.File videoFile = new java.io.File(videoPath);
                    if (videoFile.exists()) {
                        Desktop.getDesktop().open(videoFile);
                    } else {
                        openLink(p.getVideoUrl());
                    }
                } catch (Exception ex) {
                    showAlert("Error", "Could not open video: " + ex.getMessage());
                }
            });
            
            videoArea.setCursor(Cursor.HAND);
            videoContainer.getChildren().addAll(videoLabel, videoArea);
            content.getChildren().add(videoContainer);
        }

        // Attachment if present
        if (p.getPieceJointeUrl() != null && !p.getPieceJointeUrl().trim().isEmpty()) {
            VBox attachmentContainer = new VBox(8);
            attachmentContainer.getStyleClass().add("pub-attachment-container");
            
            Label attachmentLabel = new Label("� Pièce jointe");
            attachmentLabel.getStyleClass().add("pub-text");
            attachmentLabel.setStyle("-fx-font-weight: 600;");
            
            Label fileName = new Label(p.getPieceJointeUrl().substring(p.getPieceJointeUrl().lastIndexOf('/') + 1));
            fileName.getStyleClass().add("pub-text");
            fileName.setStyle("-fx-text-fill: #64748B; -fx-font-size: 13px;");
            fileName.setWrapText(true);
            
            Button openAttachment = new Button("Ouvrir");
            openAttachment.getStyleClass().addAll("btn-outline", "btn-small");
            openAttachment.setOnAction(e -> openLink(p.getPieceJointeUrl()));
            
            attachmentContainer.getChildren().addAll(attachmentLabel, fileName, openAttachment);
            content.getChildren().add(attachmentContainer);
        }

        // Footer with actions
        VBox footer = new VBox(12);
        footer.getStyleClass().add("pub-footer");

        // Divider
        Separator divider = new Separator();
        divider.getStyleClass().add("pub-divider");

        // Action buttons
        HBox actions = new HBox(8);
        actions.getStyleClass().add("pub-actions");

        // Like button (toggleable)
        Button likeBtn = new Button("� J'aime");
        likeBtn.getStyleClass().add("reaction-btn");
        likeBtn.setOnAction(e -> toggleLike(likeBtn, p));

        // Comment button
        Button commentBtn = new Button("💬 Commenter");
        commentBtn.getStyleClass().add("reaction-btn");

        // Share button
        Button shareBtn = new Button("🔗 Partager");
        shareBtn.getStyleClass().add("reaction-btn");
        shareBtn.setOnAction(e -> sharePublication(p));

        actions.getChildren().addAll(likeBtn, commentBtn, shareBtn);

        // Admin actions (edit/delete)
        HBox adminActions = new HBox(8);
        adminActions.setAlignment(Pos.CENTER_RIGHT);
        
        Button btnEdit = new Button("Modifier");
        btnEdit.getStyleClass().addAll("btn-outline", "btn-small");
        btnEdit.setOnAction(e -> openForm(p));

        Button btnDelete = new Button("Supprimer");
        btnDelete.getStyleClass().addAll("btn-danger", "btn-small");
        btnDelete.setOnAction(e -> deletePublication(p));

        adminActions.getChildren().addAll(btnEdit, btnDelete);

        footer.getChildren().addAll(divider, actions, adminActions);

        card.getChildren().addAll(header, content, footer);

        applyHoverScale(card);
        return card;
    }

    private void toggleLike(Button likeBtn, Publication p) {
        if (likeBtn.getStyleClass().contains("liked")) {
            likeBtn.getStyleClass().remove("liked");
            likeBtn.setText("👍 J'aime");
        } else {
            likeBtn.getStyleClass().add("liked");
            likeBtn.setText("👍 Aimé");
        }
    }

    private void sharePublication(Publication p) {
        try {
            String shareText = "Découvrez cette publication: " + p.getTitre();
            java.awt.Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new java.awt.datatransfer.StringSelection(shareText), null);
            showAlert("Succès", "Lien de publication copié dans le presse-papiers!");
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de partager la publication: " + e.getMessage());
        }
    }

    private void deletePublication(Publication p) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la publication");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette publication ?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            publicationService.supprimerPublication(p.getIdPublication());
            refresh();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Suppression impossible: " + e.getMessage());
        }
    }

    private void openLink(String url) {
        try {
            if (url == null || url.trim().isEmpty()) return;
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignored) {
        }
    }

    private void playCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(18);

        FadeTransition ft = new FadeTransition(Duration.millis(220), card);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(Math.min(index * 40L, 240L)));

        TranslateTransition tt = new TranslateTransition(Duration.millis(220), card);
        tt.setToY(0);
        tt.setDelay(Duration.millis(Math.min(index * 40L, 240L)));

        new ParallelTransition(ft, tt).play();
    }

    private void applyHoverScale(VBox card) {
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), card);
            st.setToX(1.01);
            st.setToY(1.01);
            st.play();
        });

        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(110), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
