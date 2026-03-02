package Controllers.evenement;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import Services.evenement.PublicationService;
import Utils.Mydatabase;
import Entities.evenement.Publication;
import Entities.evenement.Evenement;
import javafx.scene.paint.Color;
import javafx.scene.effect.DropShadow;
import javafx.util.Duration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.List;

public class EventPublicationsBackendController {

    @FXML private ImageView ivCover;
    @FXML private StackPane coverPlaceholder;
    @FXML private ImageView ivAvatar;
    @FXML private HBox heroCard;
    @FXML private Label lblTitle;
    @FXML private Label lblMeta;
    @FXML private Label lblHeroType;
    @FXML private Label lblHeroStatus;
    @FXML private Label lblDesc;
    @FXML private VBox infoBox;
    @FXML private VBox feedContainer;
    @FXML private Label lblPostCount;
    @FXML private Button btnAddPost;
    @FXML private Button btnBack;
    
    // New buttons for event actions
    @FXML private Button btnParticipate;
    @FXML private Button btnShare;

    private PublicationService publicationService = new PublicationService();
    private int currentEventId = -1;
    private List<Publication> allPublications;
    private final Connection con = Mydatabase.getInstance().getConnection();

    @FXML
    public void initialize() {
        if (btnBack != null) {
            btnBack.setOnAction(e -> handleBack());
        }

        if (btnAddPost != null) {
            btnAddPost.setOnAction(e -> handleAddPost());
        }
        
        // New event action buttons
        if (btnParticipate != null) {
            btnParticipate.setOnAction(e -> handleParticipate());
        }
        
        if (btnShare != null) {
            btnShare.setOnAction(e -> handleShare());
        }

        // Premium hover for hero images (safe: if null, ignored)
        if (ivCover != null) {
            setupHoverScale(ivCover, 1.01);
        }
        if (ivAvatar != null) {
            setupHoverScale(ivAvatar, 1.03);
        }

        if (heroCard != null) {
            setupCardHover(heroCard);
        }
    }

    public void setEventId(int eventId) {
        this.currentEventId = eventId;
        loadEventHeader();
        loadPublications();
    }

    public void setEvent(Evenement ev) {
        if (ev == null) return;
        this.currentEventId = ev.getIdEvenement();
        applyPageFromEvent(ev);
        loadPublications();
    }

    @FXML
    private void handleBack() {
        if (NavigationService.hasContentArea()) {
            NavigationService.go("/views_event/event_affichage_backend.fxml");
        } else if (Nav.getDefaultContentArea() != null) {
            Nav.go("/views_event/event_affichage_backend.fxml");
        } else {
            showErrorAlert("Navigation", "Aucune zone de navigation n'est configuree.");
        }
    }

    private void handleAddPost() {
        if (NavigationService.hasContentArea()) {
            NavigationService.go("/views_event/publication_form.fxml", controller -> {
                if (controller instanceof PublicationFormController) {
                    PublicationFormController formController = (PublicationFormController) controller;
                    formController.setEventId(currentEventId);
                    formController.setModeAdd();
                }
            });
        } else if (Nav.getDefaultContentArea() != null) {
            Nav.go("/views_event/publication_form.fxml", controller -> {
                if (controller instanceof PublicationFormController) {
                    PublicationFormController formController = (PublicationFormController) controller;
                    formController.setEventId(currentEventId);
                    formController.setModeAdd();
                }
            });
        } else {
            openPublicationFormModal(null);
        }
    }
    
    private void handleParticipate() {
        showSuccessAlert("Participation", "Votre participation à l'événement a été enregistrée!");
    }
    
    private void handleShare() {
        showSuccessAlert("Partage", "L'événement a été partagé avec succès!");
    }

    private void loadEventHeader() {
        if (currentEventId <= 0) return;

        try {
            String req = "SELECT * FROM evenement WHERE id_evenement = ?";
            PreparedStatement st = con.prepareStatement(req);
            st.setInt(1, currentEventId);
            ResultSet rs = st.executeQuery();

            if (rs.next()) {
                Timestamp tsDebut = rs.getTimestamp("date_debut");
                Timestamp tsFin = rs.getTimestamp("date_fin");

                Evenement ev = new Evenement(
                        rs.getInt("id_evenement"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        tsDebut != null ? tsDebut.toLocalDateTime() : null,
                        tsFin != null ? tsFin.toLocalDateTime() : null,
                        rs.getString("lieu"),
                        rs.getString("type"),
                        rs.getInt("capacite_max"),
                        rs.getString("statut"),
                        rs.getString("image_url")
                );

                applyPageFromEvent(ev);
            } else {
                applyHeaderFallback();
            }

        } catch (Exception e) {
            applyHeaderFallback();
        }
    }

    private void applyHeaderFallback() {
        if (lblTitle != null) lblTitle.setText("Événement ID: " + currentEventId);
        if (lblMeta != null) lblMeta.setText("");
        if (lblDesc != null) lblDesc.setText("");

        if (lblHeroType != null) lblHeroType.setText("");
        if (lblHeroStatus != null) lblHeroStatus.setText("");

        if (ivCover != null) ivCover.setImage(null);
        if (coverPlaceholder != null) {
            coverPlaceholder.setVisible(true);
            coverPlaceholder.setManaged(true);
        }

        if (ivAvatar != null) ivAvatar.setImage(null);
        buildInfoCard(null);
    }

    private void applyPageFromEvent(Evenement ev) {
        if (ev == null) return;

        if (lblTitle != null) lblTitle.setText(nvl(ev.getTitre()));

        if (lblMeta != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String d1 = ev.getDateDebut() != null ? ev.getDateDebut().toLocalDate().format(fmt) : "?";
            String d2 = ev.getDateFin() != null ? ev.getDateFin().toLocalDate().format(fmt) : "?";
            String lieu = nvl(ev.getLieu());
            String meta = d1 + " → " + d2;
            if (!lieu.isEmpty()) meta = meta + " • " + lieu;
            lblMeta.setText(meta);
        }

        if (lblDesc != null) lblDesc.setText(nvl(ev.getDescription()));

        if (lblHeroType != null) {
            lblHeroType.setText(nvl(ev.getType()));
        }
        if (lblHeroStatus != null) {
            lblHeroStatus.setText(nvl(ev.getStatut()));
        }

        String image = nvl(ev.getImageUrl());
        Image cover = loadImageSafe(image);
        if (ivCover != null) ivCover.setImage(cover);
        if (coverPlaceholder != null) {
            boolean showPlaceholder = (cover == null);
            coverPlaceholder.setVisible(showPlaceholder);
            coverPlaceholder.setManaged(showPlaceholder);
        }

        // Avatar: reuse same image for now (fallback). If later you add a dedicated avatar field, plug it here.
        if (ivAvatar != null) {
            ivAvatar.setImage(cover);
        }

        buildInfoCard(ev);
    }

    private void buildInfoCard(Evenement ev) {
        if (infoBox == null) return;
        infoBox.getChildren().clear();

        Label infoTitle = new Label("Infos");
        infoTitle.getStyleClass().add("info-title");
        infoBox.getChildren().add(infoTitle);

        if (ev == null) return;

        HBox chips = new HBox(8);
        Label chipType = new Label(nvl(ev.getType()));
        chipType.getStyleClass().addAll("chip", "chip-type");
        Label chipStatut = new Label(nvl(ev.getStatut()));
        chipStatut.getStyleClass().addAll("chip", "chip-status");
        chips.getChildren().addAll(chipType, chipStatut);
        infoBox.getChildren().add(chips);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        String d1 = ev.getDateDebut() != null ? ev.getDateDebut().toLocalDate().format(fmt) : "?";
        String d2 = ev.getDateFin() != null ? ev.getDateFin().toLocalDate().format(fmt) : "?";

        infoBox.getChildren().add(infoLine("Dates", d1 + " → " + d2));
        infoBox.getChildren().add(infoLine("Lieu", nvl(ev.getLieu())));
        infoBox.getChildren().add(infoLine("Capacité", String.valueOf(ev.getCapaciteMax())));
    }

    private HBox infoLine(String k, String v) {
        HBox row = new HBox(10);
        row.getStyleClass().add("info-row");
        Label key = new Label(k);
        key.getStyleClass().add("info-key");
        Label val = new Label(v);
        val.getStyleClass().add("info-val");
        row.getChildren().addAll(key, val);
        return row;
    }

    private Image loadImageSafe(String raw) {
        try {
            if (raw == null || raw.trim().isEmpty()) return null;
            String s = raw.trim();

            // If already a URL (http/https/file/jar), load directly
            if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:") || s.startsWith("jar:")) {
                return new Image(s, true);
            }

            // Try local filesystem path
            File f = new File(s);
            if (f.exists()) {
                return new Image(f.toURI().toString(), true);
            }

            // Try as classpath resource
            if (!s.startsWith("/")) s = "/" + s;
            if (getClass().getResource(s) != null) {
                return new Image(Objects.requireNonNull(getClass().getResource(s)).toExternalForm(), true);
            }

            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    public void loadPublications() {
        if (currentEventId <= 0) return;
        try {
            allPublications = publicationService.afficherPublicationsParEvenement(currentEventId);
            displayPublications(allPublications);
        } catch (Exception e) {
            showErrorAlert("Erreur", "Impossible de charger les publications: " + e.getMessage());
        }
    }

    // Backward-compatible alias (existing code may call refresh())
    public void refresh() {
        loadPublications();
    }

    private void displayPublications(List<Publication> publications) {
        if (feedContainer == null) return;
        feedContainer.getChildren().clear();

        if (lblPostCount != null) {
            int count = (publications == null) ? 0 : publications.size();
            lblPostCount.setText("(" + count + ")");
        }

        if (publications == null || publications.isEmpty()) {
            Label noPosts = new Label("Aucune publication trouvée");
            noPosts.getStyleClass().add("feed-empty");
            feedContainer.getChildren().add(noPosts);
            return;
        }

        int i = 0;
        for (Publication publication : publications) {
            VBox card = createPublicationCard(publication);
            feedContainer.getChildren().add(card);
            animateCardEntrance(card, i++);
        }
    }

    private VBox createPublicationCard(Publication pub) {
        VBox card = new VBox(0);
        card.getStyleClass().add("feed-card");
        setupCardHover(card);

        // Header with avatar, author, and menu
        HBox header = new HBox(12);
        header.getStyleClass().add("feed-card-header");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 12, 16));

        // Avatar
        StackPane avatarContainer = new StackPane();
        avatarContainer.getStyleClass().add("avatar-container");
        
        ImageView miniAvatar = new ImageView();
        miniAvatar.setFitWidth(40);
        miniAvatar.setFitHeight(40);
        miniAvatar.setPreserveRatio(false);
        miniAvatar.getStyleClass().add("author-avatar");
        if (ivAvatar != null && ivAvatar.getImage() != null) {
            miniAvatar.setImage(ivAvatar.getImage());
        }
        avatarContainer.getChildren().add(miniAvatar);

        // Author info
        VBox authorInfo = new VBox(2);
        authorInfo.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Label author = new Label("Auteur #" + pub.getAuteurId());
        author.getStyleClass().add("author-name");
        
        Label date = new Label(pub.getDatePublication() != null ?
            pub.getDatePublication().format(DateTimeFormatter.ofPattern("d MMMM 'à' HH:mm")) : "");
        date.getStyleClass().add("post-time");

        authorInfo.getChildren().addAll(author, date);

        // Three dots menu
        Button menuBtn = new Button("⋮");
        menuBtn.getStyleClass().add("menu-button");
        menuBtn.setOnAction(e -> showPublicationMenu(pub));

        HBox.setHgrow(authorInfo, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(avatarContainer, authorInfo, menuBtn);

        // Content
        Label content = new Label(nvl(pub.getContenu()));
        content.getStyleClass().add("feed-content");
        content.setWrapText(true);
        content.setPadding(new Insets(0, 16, 12, 16));

        // Media section
        VBox mediaSection = new VBox(0);
        mediaSection.getStyleClass().add("media-section");

        if (pub.getImageUrl() != null && !pub.getImageUrl().trim().isEmpty()) {
            try {
                Image img = loadImageSafe(pub.getImageUrl());
                if (img != null) {
                    StackPane imageContainer = new StackPane();
                    imageContainer.getStyleClass().add("media-image-container");
                    
                    ImageView imageView = new ImageView(img);
                    imageView.setFitWidth(600);
                    imageView.setPreserveRatio(true);
                    imageView.getStyleClass().add("feed-media-img");
                    
                    imageContainer.getChildren().add(imageView);
                    mediaSection.getChildren().add(imageContainer);
                }
            } catch (Exception ignored) {
            }
        }

        // Footer with action buttons
        HBox footer = new HBox(0);
        footer.getStyleClass().add("feed-card-footer");
        footer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 16, 16, 16));

        // Action buttons (Like, Comment, Share)
        Button likeBtn = new Button("👍 J'aime");
        likeBtn.getStyleClass().add("action-pill");
        likeBtn.setOnAction(e -> handleLike(pub));

        Button commentBtn = new Button("💬 Commenter");
        commentBtn.getStyleClass().add("action-pill");
        commentBtn.setOnAction(e -> handleComment(pub));

        Button shareBtn = new Button("🔄 Partager");
        shareBtn.getStyleClass().add("action-pill");
        shareBtn.setOnAction(e -> handleSharePublication(pub));

        footer.getChildren().addAll(likeBtn, commentBtn, shareBtn);

        // Assemble card
        card.getChildren().addAll(header, content);
        if (!mediaSection.getChildren().isEmpty()) {
            card.getChildren().add(mediaSection);
        }
        card.getChildren().add(footer);

        return card;
    }

    private void setupCardHover(Node card) {
        DropShadow base = new DropShadow(24, Color.rgb(17, 24, 39, 0.10));
        base.setOffsetY(10);
        DropShadow hover = new DropShadow(34, Color.rgb(17, 24, 39, 0.18));
        hover.setOffsetY(14);
        card.setEffect(base);

        ScaleTransition in = new ScaleTransition(Duration.millis(160), card);
        in.setToX(1.015);
        in.setToY(1.015);

        ScaleTransition out = new ScaleTransition(Duration.millis(160), card);
        out.setToX(1.0);
        out.setToY(1.0);

        card.setOnMouseEntered(e -> {
            card.setEffect(hover);
            in.playFromStart();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(base);
            out.playFromStart();
        });
    }

    private void setupHoverScale(Node node, double scale) {
        ScaleTransition in = new ScaleTransition(Duration.millis(160), node);
        in.setToX(scale);
        in.setToY(scale);

        ScaleTransition out = new ScaleTransition(Duration.millis(160), node);
        out.setToX(1.0);
        out.setToY(1.0);

        node.setOnMouseEntered(e -> in.playFromStart());
        node.setOnMouseExited(e -> out.playFromStart());
    }

    private void animateCardEntrance(VBox card, int index) {
        card.setOpacity(0);
        card.setTranslateY(14);

        FadeTransition fade = new FadeTransition(Duration.millis(320), card);
        fade.setToValue(1);

        javafx.animation.TranslateTransition translate = new javafx.animation.TranslateTransition(Duration.millis(320), card);
        translate.setToY(0);

        ParallelTransition p = new ParallelTransition(fade, translate);
        p.setDelay(Duration.millis(Math.min(12, index) * 55L));
        p.play();
    }

    private void deletePublication(Publication publication) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation de Suppression");
        confirm.setHeaderText("Supprimer la publication");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer la publication \"" + publication.getTitre() + "\" ?");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                publicationService.supprimerPublication(publication.getIdPublication());
                refresh();
                showSuccessAlert("Succès", "Publication supprimée avec succès.");
            } catch (Exception e) {
                showErrorAlert("Erreur", "Impossible de supprimer la publication: " + e.getMessage());
            }
        }
    }

    private void showSuccessAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showPublicationMenu(Publication pub) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem viewDetails = new MenuItem("Voir détails");
        viewDetails.setOnAction(e -> {
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/publication_details.fxml", ctrl -> {
                    if (ctrl instanceof PublicationDetailsController) {
                        ((PublicationDetailsController) ctrl).setEventId(currentEventId);
                        ((PublicationDetailsController) ctrl).setPublication(pub);
                    }
                });
            } else if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/publication_details.fxml", ctrl -> {
                    if (ctrl instanceof PublicationDetailsController) {
                        ((PublicationDetailsController) ctrl).setEventId(currentEventId);
                        ((PublicationDetailsController) ctrl).setPublication(pub);
                    }
                });
            } else {
                openPublicationDetailsModal(pub);
            }
        });
        
        MenuItem editItem = new MenuItem("Modifier");
        editItem.setOnAction(e -> {
            if (NavigationService.hasContentArea()) {
                NavigationService.go("/views_event/publication_form.fxml", controller -> {
                    if (controller instanceof PublicationFormController) {
                        PublicationFormController formController = (PublicationFormController) controller;
                        formController.setEventId(currentEventId);
                        formController.setModeEdit(pub);
                    }
                });
            } else if (Nav.getDefaultContentArea() != null) {
                Nav.go("/views_event/publication_form.fxml", controller -> {
                    if (controller instanceof PublicationFormController) {
                        PublicationFormController formController = (PublicationFormController) controller;
                        formController.setEventId(currentEventId);
                        formController.setModeEdit(pub);
                    }
                });
            } else {
                openPublicationFormModal(pub);
            }
        });
        
        MenuItem deleteItem = new MenuItem("Supprimer");
        deleteItem.setOnAction(e -> deletePublication(pub));
        
        contextMenu.getItems().addAll(viewDetails, new SeparatorMenuItem(), editItem, deleteItem);
        if (feedContainer != null && feedContainer.getScene() != null) {
            contextMenu.show(feedContainer, Screen.getPrimary().getVisualBounds().getMaxX() - 200, 100);
        }
    }
    
    private void handleLike(Publication pub) {
        showSuccessAlert("J'aime", "Vous avez aimé cette publication!");
    }
    
    private void handleComment(Publication pub) {
        showSuccessAlert("Commenter", "Fonctionnalité de commentaire bientôt disponible!");
    }
    
    private void handleSharePublication(Publication pub) {
        showSuccessAlert("Partager", "Publication partagée avec succès!");
    }
    private void openPublicationFormModal(Publication toEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/publication_form.fxml"));
            Parent root = loader.load();
            PublicationFormController controller = loader.getController();
            controller.setEventId(currentEventId);
            if (toEdit == null) {
                controller.setModeAdd();
            } else {
                controller.setModeEdit(toEdit);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(toEdit == null ? "Ajouter publication" : "Modifier publication");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            refresh();
        } catch (Exception e) {
            showErrorAlert("Navigation", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void openPublicationDetailsModal(Publication pub) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views_event/publication_details.fxml"));
            Parent root = loader.load();
            PublicationDetailsController controller = loader.getController();
            controller.setEventId(currentEventId);
            controller.setPublication(pub);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Details publication");
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            showErrorAlert("Navigation", "Impossible d'ouvrir les details: " + e.getMessage());
        }
    }
}

