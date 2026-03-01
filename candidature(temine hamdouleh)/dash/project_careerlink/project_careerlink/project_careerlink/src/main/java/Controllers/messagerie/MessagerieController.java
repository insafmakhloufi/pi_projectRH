package Controllers.messagerie;

import Entities.messagerie.Conversation;
import Entities.messagerie.Message;
import Iservices.messagerie.IConversationService;
import Iservices.messagerie.IMessageService;
import Iservices.messagerie.INotificationService;
import Services.messagerie.*;
import Utils.Session;
import Entities.User.User;

import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.scene.layout.FlowPane;

public class MessagerieController {

    // ── Services ────────────────────────────────────────
    private IConversationService conversationService;
    private IMessageService messageService;
    private INotificationService notificationService;
    private OpenAIService openAIService;

    // ── État ────────────────────────────────────────────
    private User currentUser;
    private Timeline badgeRefresh;
    private Stage chatStage;
    private boolean popupOuvert = false;

    // ── UI Composants ───────────────────────────────────
    private VBox msgContent;
    private Label msgBadge;
    private Button btnToggleMsg;
    private ListView<Conversation> listeConversations;
    private StackPane parentContainer;

    private VocalService vocalService = new VocalService();

    public MessagerieController() {
        conversationService = new ConversationService();
        messageService      = new MessageService();
        notificationService = new NotificationService();
        openAIService       = new OpenAIService();
        currentUser         = Session.getCurrentUser();
    }

    public void initPopup(StackPane container) {
        if (currentUser == null) {
            System.out.println("MessagerieController: aucun utilisateur connecté, popup ignoré.");
            return;
        }

        this.parentContainer = container;

        final boolean isManager = currentUser.getRole().equals("MANAGER_RH");
        final String couleur    = isManager ? "#5b5fc7" : "#2d6a9f";

        // ── Barre messagerie (toujours visible) ─────────
        HBox msgBar = new HBox(10);
        msgBar.setAlignment(Pos.CENTER_LEFT);
        msgBar.setPadding(new Insets(10, 15, 10, 15));
        msgBar.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-background-radius: 10 10 0 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, -2);"
        );
        msgBar.setMinWidth(280);
        msgBar.setPrefWidth(280);

        Circle avatar = new Circle(14);
        avatar.setStyle("-fx-fill: rgba(255,255,255,0.3);");

        Label titre = new Label(isManager ? "💬 Messagerie RH" : "💬 Messagerie");
        titre.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        HBox.setHgrow(titre, Priority.ALWAYS);

        msgBadge = new Label("0");
        msgBadge.setStyle(
                "-fx-background-color: #ff4444;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 10;" +
                        "-fx-padding: 1 6;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;"
        );
        msgBadge.setVisible(false);

        btnToggleMsg = new Button("∧");
        btnToggleMsg.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 14px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 4;"
        );

        msgBar.getChildren().addAll(avatar, titre, msgBadge, btnToggleMsg);
        msgBar.setOnMouseClicked(e -> togglePopup());

        // ── Liste conversations ──────────────────────────
        listeConversations = new ListView<>();
        listeConversations.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-width: 0;" +
                        "-fx-background-insets: 0;"
        );
        VBox.setVgrow(listeConversations, Priority.ALWAYS);
        configurerListeConversations(couleur);

        // ── Résultats de recherche ───────────────────────
        VBox searchResults = new VBox(0);
        searchResults.setStyle("-fx-background-color: white;");
        searchResults.setVisible(false);
        searchResults.setManaged(false);

        // ── Barre de recherche ───────────────────────────
        TextField searchField = new TextField();
        searchField.setPromptText("Rechercher un utilisateur...");
        searchField.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 7 12;" +
                        "-fx-font-size: 12px;"
        );

        // ── Logique recherche ────────────────────────────
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.trim().isEmpty()) {
                // Vider résultats → afficher liste conversations
                searchResults.getChildren().clear();
                searchResults.setVisible(false);
                searchResults.setManaged(false);
                listeConversations.setVisible(true);
                listeConversations.setManaged(true);
                return;
            }

            // Chercher users par nom
            List<Entities.User.User> users = conversationService
                    .rechercherUsers(val.trim(), currentUser.getId());

            searchResults.getChildren().clear();

            if (users.isEmpty()) {
                Label aucun = new Label("Aucun résultat pour \"" + val + "\"");
                aucun.setStyle(
                        "-fx-padding: 12 10;" +
                                "-fx-text-fill: #888;" +
                                "-fx-font-size: 12px;"
                );
                searchResults.getChildren().add(aucun);
            } else {
                for (Entities.User.User u : users) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(8, 12, 8, 12));
                    row.setStyle("-fx-cursor: hand; -fx-background-color: white;");

                    // Avatar avec initiale
                    Circle ava = new Circle(16);
                    ava.setStyle("-fx-fill: " + couleur + "33;");
                    Label initial = new Label(
                            u.getFullName().substring(0, 1).toUpperCase()
                    );
                    initial.setStyle(
                            "-fx-text-fill: " + couleur + ";" +
                                    "-fx-font-weight: bold;" +
                                    "-fx-font-size: 12px;"
                    );
                    StackPane avatarPane = new StackPane(ava, initial);

                    // Nom + rôle
                    VBox info = new VBox(2);
                    Label nomLabel = new Label(u.getFullName());
                    nomLabel.setStyle(
                            "-fx-font-weight: bold;" +
                                    "-fx-font-size: 12px;"
                    );
                    Label roleLabel = new Label(u.getRole());
                    roleLabel.setStyle(
                            "-fx-font-size: 10px;" +
                                    "-fx-text-fill: #888;"
                    );
                    info.getChildren().addAll(nomLabel, roleLabel);
                    HBox.setHgrow(info, Priority.ALWAYS);

                    // Bouton message
                    Label msgIco = new Label("💬");
                    msgIco.setStyle("-fx-font-size: 14px;");

                    row.getChildren().addAll(avatarPane, info, msgIco);

                    // Hover
                    row.setOnMouseEntered(e ->
                            row.setStyle("-fx-cursor: hand; -fx-background-color: #f0f2f5;")
                    );
                    row.setOnMouseExited(e ->
                            row.setStyle("-fx-cursor: hand; -fx-background-color: white;")
                    );

                    // ── Clic → créer conversation + ouvrir chat ──
                    row.setOnMouseClicked(e -> {
                        int candidatId, managerId;

                        // Déterminer les rôles correctement
                        if (currentUser.getRole().equals("CANDIDAT")) {
                            candidatId = currentUser.getId();
                            managerId  = u.getId();
                        } else {
                            candidatId = u.getId();
                            managerId  = currentUser.getId();
                        }

                        // Créer ou récupérer la conversation existante
                        Conversation conv = conversationService
                                .creerConversation(candidatId, managerId);

                        if (conv == null) {
                            System.out.println("Erreur: impossible de créer la conversation");
                            return;
                        }

                        // Reset recherche
                        searchField.clear();
                        searchResults.getChildren().clear();
                        searchResults.setVisible(false);
                        searchResults.setManaged(false);
                        listeConversations.setVisible(true);
                        listeConversations.setManaged(true);

                        // Recharger liste + ouvrir chat
                        chargerConversations();
                        ouvrirFenetreChat(conv);
                    });

                    searchResults.getChildren().add(row);
                }
            }

            // Afficher résultats, cacher liste
            searchResults.setVisible(true);
            searchResults.setManaged(true);
            listeConversations.setVisible(false);
            listeConversations.setManaged(false);
        });

        HBox searchZone = new HBox(searchField);
        searchZone.setPadding(new Insets(8, 10, 8, 10));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchZone.setStyle("-fx-background-color: white;");

        // ── Assemblage contenu popup ─────────────────────
        msgContent = new VBox(0, searchZone, searchResults, listeConversations);
        msgContent.setPrefWidth(280);
        msgContent.setPrefHeight(380);
        msgContent.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #ddd;" +
                        "-fx-border-width: 1 1 0 1;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, -3);"
        );
        msgContent.setVisible(false);
        msgContent.setManaged(false);

        // ── Assemblage popup final ───────────────────────
        VBox popup = new VBox(0, msgContent, msgBar);
        popup.setMaxWidth(280);
        popup.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane.setAlignment(popup, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(popup, new Insets(0, 20, 0, 0));

        container.getChildren().add(popup);

        // ── Charger conversations + démarrer badge ───────
        chargerConversations();
        demarrerBadgeRefresh();
    }

    // ── Toggle popup ─────────────────────────────────────
    private void togglePopup() {
        popupOuvert = !popupOuvert;
        msgContent.setVisible(popupOuvert);
        msgContent.setManaged(popupOuvert);
        btnToggleMsg.setText(popupOuvert ? "∨" : "∧");
        if (popupOuvert) chargerConversations();
    }

    // ── Charger conversations ────────────────────────────
    private void chargerConversations() {
        List<Conversation> convs = conversationService
                .getConversationsParUser(currentUser.getId());
        listeConversations.getItems().clear();
        listeConversations.getItems().addAll(convs);
    }

    private void filtrerConversations(String query) {
        List<Conversation> convs = conversationService
                .getConversationsParUser(currentUser.getId());
        if (query == null || query.isEmpty()) {
            listeConversations.getItems().setAll(convs);
        } else {
            listeConversations.getItems().setAll(
                    convs.stream()
                            .filter(c -> String.valueOf(c.getId()).contains(query))
                            .toList()
            );
        }
    }

    // ── Configurer la ListView ───────────────────────────
    private void configurerListeConversations(String couleur) {
        listeConversations.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conv, boolean empty) {
                super.updateItem(conv, empty);
                if (empty || conv == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                // ✅ Récupérer le vrai nom de l'interlocuteur
                int interlocuteurId = currentUser.getId() == conv.getCandidatId()
                        ? conv.getManagerId()
                        : conv.getCandidatId();

                Entities.User.User interlocuteur = conversationService.getUserById(interlocuteurId);
                String nom = interlocuteur != null ? interlocuteur.getFullName() : "Utilisateur #" + interlocuteurId;
                String role = interlocuteur != null ? interlocuteur.getRole() : "";

                Circle ava = new Circle(18);
                ava.setStyle("-fx-fill: " + couleur + "33;");

                Label initial = new Label(nom.substring(0, 1).toUpperCase());
                initial.setStyle("-fx-text-fill: " + couleur + "; -fx-font-weight: bold;");
                StackPane avatarPane = new StackPane(ava, initial);

                Label nomLabel = new Label(nom);
                nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                /*Label sousTitre = new Label(role);
                sousTitre.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");*/

                // APRÈS jdiddd
                List<Message> msgs = messageService.getMessagesParConversation(conv.getId());
                String apercu;
                if (msgs.isEmpty()) {
                    apercu = "Aucun message";
                } else {
                    Message dernierMsg = msgs.get(msgs.size() - 1);
                    if (dernierMsg.isVocal()) {
                        apercu = "🎤 Message vocal";
                    } else if (dernierMsg.getFilePath() != null && !dernierMsg.getFilePath().isEmpty()) {
                        apercu = "📎 " + (dernierMsg.getFileName() != null
                                ? dernierMsg.getFileName() : "Fichier");
                    } else {
                        String content = dernierMsg.getContent();
                        apercu = content != null && content.length() > 30
                                ? content.substring(0, 30) + "..."
                                : content;
                    }
                }
                Label sousTitre = new Label(apercu);
                sousTitre.setStyle("-fx-font-size: 10px; -fx-text-fill: #888;");

                VBox info = new VBox(2, nomLabel, sousTitre);
                HBox cell = new HBox(10, avatarPane, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(8, 10, 8, 10));

                setGraphic(cell);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
            }
        });

        listeConversations.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        ouvrirFenetreChat(newVal);
                        Platform.runLater(() ->
                                listeConversations.getSelectionModel().clearSelection()
                        );
                    }
                });
    }

    // ── Fenêtre chat style LinkedIn ──────────────────────
    private void ouvrirFenetreChat(Conversation conv) {
        if (chatStage != null && chatStage.isShowing()) {
            chatStage.close();
        }

        boolean isManager = currentUser.getRole().equals("MANAGER_RH");
        String couleur = isManager ? "#5b5fc7" : "#2d6a9f";

        // ✅ Récupérer le vrai nom de l'interlocuteur
        int interlocuteurId = isManager ? conv.getCandidatId() : conv.getManagerId();
        Entities.User.User interlocuteur = conversationService.getUserById(interlocuteurId);
        String nomInterlocuteur = interlocuteur != null
                ? interlocuteur.getFullName()
                : (isManager ? "Candidat #" + interlocuteurId : "Manager RH #" + interlocuteurId);
        String roleInterlocuteur = interlocuteur != null
                ? interlocuteur.getRole()
                : (isManager ? "Candidat" : "Responsable RH");

        // ── Header ──────────────────────────────────────
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 15, 12, 15));
        header.setStyle("-fx-background-color: " + couleur + ";");

        Circle ava = new Circle(16);
        ava.setStyle("-fx-fill: rgba(255,255,255,0.3);");

        VBox headerInfo = new VBox(1);
        Label nomLabel = new Label(nomInterlocuteur);
        nomLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label roleLabel = new Label(roleInterlocuteur);
        roleLabel.setStyle("-fx-text-fill: rgba(255,255,255,0.75); -fx-font-size: 10px;");
        headerInfo.getChildren().addAll(nomLabel, roleLabel);
        HBox.setHgrow(headerInfo, Priority.ALWAYS);
        // ... reste inchangé

        Button btnFermer = new Button("✕");
        btnFermer.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: white;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 13px;"
        );

        header.getChildren().addAll(ava, headerInfo, btnFermer);

        // ── Zone messages ────────────────────────────────
        VBox messagesContainer = new VBox(8);
        messagesContainer.setPadding(new Insets(12));

        ScrollPane scrollMessages = new ScrollPane(messagesContainer);
        scrollMessages.setFitToWidth(true);
        scrollMessages.setStyle(
                "-fx-background-color: #f5f5f5;" +
                        "-fx-background: #f5f5f5;" +
                        "-fx-border-width: 0;"
        );
        VBox.setVgrow(scrollMessages, Priority.ALWAYS);

        chargerMessagesDansContainer(conv.getId(), messagesContainer,
                scrollMessages, couleur);

        // ── Zone saisie ──────────────────────────────────

        /*TextArea inputMsg = new TextArea();
        inputMsg.setPromptText("Écrire un message...");
        inputMsg.setPrefRowCount(2);
        inputMsg.setWrapText(true);
        inputMsg.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-width: 0;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8 12;"
        );
        HBox.setHgrow(inputMsg, Priority.ALWAYS);

        Button btnEnvoyer = new Button("➤");
        btnEnvoyer.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 50;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-min-width: 38;" +
                        "-fx-min-height: 38;" +
                        "-fx-max-width: 38;" +
                        "-fx-max-height: 38;"
        );

        HBox inputZone = new HBox(8, inputMsg, btnEnvoyer);
        inputZone.setPadding(new Insets(10));
        inputZone.setAlignment(Pos.CENTER);
        inputZone.setStyle("-fx-background-color: white;");


         */

        //jdiddddddd

        // ── Zone saisie ──────────────────────────────────

        TextArea inputMsg = new TextArea();
        inputMsg.setPromptText("Écrire un message...");
        inputMsg.setPrefRowCount(2);
        inputMsg.setWrapText(true);
        inputMsg.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-width: 0;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 8 12;"
        );
        HBox.setHgrow(inputMsg, Priority.ALWAYS);

        Button btnEnvoyer = new Button("➤");
        btnEnvoyer.setStyle(
                "-fx-background-color: " + couleur + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 50;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-min-width: 38;" +
                        "-fx-min-height: 38;" +
                        "-fx-max-width: 38;" +
                        "-fx-max-height: 38;"
        );

// ── Bouton fichier 📎 ────────────────────────────
// APRÈS
        Button btnFichier = new Button();
        Label icoFichier = new Label("⊕");
        icoFichier.setStyle("-fx-text-fill: #888; -fx-font-size: 16px;");
        btnFichier.setGraphic(icoFichier);        btnFichier.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;"
        );

        btnFichier.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Choisir un fichier");
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );

            java.io.File selectedFile = fileChooser.showOpenDialog(chatStage);
            if (selectedFile != null) {
                try {
                    // Copier dans uploads/
                    java.nio.file.Path uploadsDir = java.nio.file.Paths.get("uploads");
                    if (!java.nio.file.Files.exists(uploadsDir)) {
                        java.nio.file.Files.createDirectories(uploadsDir);
                    }

                    String uniqueFileName = System.currentTimeMillis()
                            + "_" + selectedFile.getName();
                    java.nio.file.Path dest = uploadsDir.resolve(uniqueFileName);
                    java.nio.file.Files.copy(
                            selectedFile.toPath(), dest,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );

                    int receiverId = isManager
                            ? conv.getCandidatId()
                            : conv.getManagerId();

                    // Sauvegarder en BD
                    messageService.envoyerFichier(
                            conv.getId(),
                            currentUser.getId(),
                            dest.toString(),
                            selectedFile.getName()
                    );

                    // Notification
                    notificationService.ajouterNotification(
                            receiverId,
                            currentUser.getFullName() + " vous a envoyé un fichier : "
                                    + selectedFile.getName(),
                            "fichier"
                    );

                    // Rafraîchir les messages
                    chargerMessagesDansContainer(
                            conv.getId(), messagesContainer, scrollMessages, couleur
                    );

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // ── Bouton vocal 🎤 ──────────────────────────────
        Button btnVocal = new Button();
        Label icoVocal = new Label("🎤");        icoVocal.setStyle("-fx-text-fill: #888; -fx-font-size: 16px;");
        btnVocal.setGraphic(icoVocal);

        btnVocal.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-font-size: 18px;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;"
        );



        // ── Bouton emoji 😊 ──────────────────────────────
        Button btnEmoji = new Button();
        Label icoEmoji = new Label("☺");
        icoEmoji.setStyle("-fx-text-fill: #888; -fx-font-size: 16px;");
        btnEmoji.setGraphic(icoEmoji);
        btnEmoji.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;"
        );


        // ── Popup emoji ──────────────────────────────────
        javafx.stage.Popup emojiPopup = new javafx.stage.Popup();
        emojiPopup.setAutoHide(true);

// Catégories d'emojis
        String[][] categories = {
                // Visages
                {"😀","😁","😂","🤣","😃","😄","😅","😆","😇","😈",
                        "😉","😊","😋","😌","😍","😎","😏","😐","😑","😒",
                        "😓","😔","😕","😖","😗","😘","😙","😚","😛","😜",
                        "😝","😞","😟","😠","😡","😢","😣","😤","😥","😦"},
                // Gestes
                {"👍","👎","👏","🙌","👐","🤲","🤝","🙏","✌️","🤞",
                        "🤟","🤘","🤙","👈","👉","👆","👇","☝️","👋","🤚",
                        "🖐️","✋","🖖","💪","🦾","🖕","✍️","🤳","💅","🦵"},
                // Coeurs
                {"❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔",
                        "❣️","💕","💞","💓","💗","💖","💘","💝","💟","☮️"},
                // Objets
                {"💼","📁","📂","🗂️","📋","📊","📈","📉","🗒️","🗓️",
                        "📆","📅","🗑️","📌","📍","✂️","🖇️","📎","🖊️","📝"}
        };

        String[] categoryNames = {"😀 Visages", "👍 Gestes", "❤️ Coeurs", "💼 Objets"};

// Container principal du popup
        VBox popupContainer = new VBox(0);
        popupContainer.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #ddd;" +
                        "-fx-border-width: 1;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 3);"
        );
        popupContainer.setPrefWidth(280);
        popupContainer.setMaxHeight(300);

// Onglets catégories
        HBox tabs = new HBox(0);
        tabs.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 12 12 0 0;");

// Zone emojis scrollable
        ScrollPane emojiScroll = new ScrollPane();
        emojiScroll.setFitToWidth(true);
        emojiScroll.setPrefHeight(220);
        emojiScroll.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background: white;" +
                        "-fx-border-width: 0;"
        );

        FlowPane emojiGrid = new FlowPane();
        emojiGrid.setHgap(4);
        emojiGrid.setVgap(4);
        emojiGrid.setPadding(new Insets(8));
        emojiGrid.setPrefWrapLength(260);
        emojiScroll.setContent(emojiGrid);

// Fonction pour charger une catégorie
        java.util.function.Consumer<Integer> loadCategory = (index) -> {
            emojiGrid.getChildren().clear();
            for (String emoji : categories[index]) {
                Button btnE = new Button(emoji);
                btnE.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 18px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 4;" +
                                "-fx-background-radius: 6;"
                );
                btnE.setOnMouseEntered(e ->
                        btnE.setStyle(
                                "-fx-background-color: #f0f0f0;" +
                                        "-fx-font-size: 18px;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-padding: 4;" +
                                        "-fx-background-radius: 6;"
                        )
                );
                btnE.setOnMouseExited(e ->
                        btnE.setStyle(
                                "-fx-background-color: transparent;" +
                                        "-fx-font-size: 18px;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-padding: 4;" +
                                        "-fx-background-radius: 6;"
                        )
                );
                // Clic → insérer dans inputMsg
                btnE.setOnAction(e -> {
                    int pos = inputMsg.getCaretPosition();
                    inputMsg.insertText(pos, emoji);
                    emojiPopup.hide();
                });
                emojiGrid.getChildren().add(btnE);
            }
        };

// Créer les onglets
        for (int i = 0; i < categoryNames.length; i++) {
            final int idx = i;
            Button tab = new Button(categoryNames[i]);
            tab.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-font-size: 11px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 6 10;" +
                            "-fx-border-width: 0 0 2 0;" +
                            "-fx-border-color: transparent;"
            );
            tab.setOnAction(e -> {
                // Reset tous les onglets
                for (var node : tabs.getChildren()) {
                    if (node instanceof Button t) {
                        t.setStyle(
                                "-fx-background-color: transparent;" +
                                        "-fx-font-size: 11px;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-padding: 6 10;" +
                                        "-fx-border-width: 0 0 2 0;" +
                                        "-fx-border-color: transparent;"
                        );
                    }
                }
                // Activer l'onglet sélectionné
                tab.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 11px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 6 10;" +
                                "-fx-border-width: 0 0 2 0;" +
                                "-fx-border-color: " + couleur + ";"
                );
                loadCategory.accept(idx);
            });
            tabs.getChildren().add(tab);
        }

// Charger la première catégorie par défaut
        loadCategory.accept(0);
// Activer le premier onglet
        if (!tabs.getChildren().isEmpty()) {
            Button firstTab = (Button) tabs.getChildren().get(0);
            firstTab.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-font-size: 11px;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 6 10;" +
                            "-fx-border-width: 0 0 2 0;" +
                            "-fx-border-color: " + couleur + ";"
            );
        }

// Barre de recherche emoji
        HBox searchEmoji = new HBox(8);
        searchEmoji.setPadding(new Insets(6, 8, 6, 8));
        searchEmoji.setStyle("-fx-background-color: white;");
        TextField searchEmojiField = new TextField();
        searchEmojiField.setPromptText("Rechercher un emoji...");
        searchEmojiField.setStyle(
                "-fx-background-color: #f0f2f5;" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-width: 0;" +
                        "-fx-padding: 5 10;" +
                        "-fx-font-size: 11px;"
        );
        HBox.setHgrow(searchEmojiField, Priority.ALWAYS);

// Recherche dans tous les emojis
        searchEmojiField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) {
                loadCategory.accept(0);
                return;
            }
            // Chercher dans toutes les catégories (simplement afficher tous)
            emojiGrid.getChildren().clear();
            for (String[] cat : categories) {
                for (String emoji : cat) {
                    Button btnE = new Button(emoji);
                    btnE.setStyle(
                            "-fx-background-color: transparent;" +
                                    "-fx-font-size: 18px;" +
                                    "-fx-cursor: hand;" +
                                    "-fx-padding: 4;" +
                                    "-fx-background-radius: 6;"
                    );
                    btnE.setOnAction(e -> {
                        int pos = inputMsg.getCaretPosition();
                        inputMsg.insertText(pos, emoji);
                        emojiPopup.hide();
                    });
                    emojiGrid.getChildren().add(btnE);
                }
            }
        });

        searchEmoji.getChildren().add(searchEmojiField);

        popupContainer.getChildren().addAll(tabs, searchEmoji, emojiScroll);
        emojiPopup.getContent().add(popupContainer);

        btnEmoji.setOnAction(e -> {
            if (emojiPopup.isShowing()) {
                emojiPopup.hide();
            } else {
                // Positionner le popup au-dessus du bouton
                javafx.geometry.Bounds bounds = btnEmoji.localToScreen(
                        btnEmoji.getBoundsInLocal()
                );
                emojiPopup.show(
                        btnEmoji,
                        bounds.getMinX() - 100,
                        bounds.getMinY() - 310
                );
            }
        });


        //emojiiiiiiiiiiiiiiiiiiiiiii



// État enregistrement
        final boolean[] enCours = {false};

        btnVocal.setOnAction(e -> {
            if (!enCours[0]) {
                // ── Démarrer enregistrement ──────────────
                boolean ok = vocalService.demarrerEnregistrement();
                if (ok) {
                    enCours[0] = true;
                    btnVocal.setText("⏹");
                    btnVocal.setStyle(
                            "-fx-background-color: #ff4444;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 50;" +
                                    "-fx-font-size: 16px;" +
                                    "-fx-cursor: hand;" +
                                    "-fx-padding: 4 8;"
                    );
                }
            } else {
                // ── Arrêter et envoyer ───────────────────
                String filePath = vocalService.arreterEnregistrement();
                enCours[0] = false;
                btnVocal.setText("🎤");
                btnVocal.setStyle(
                        "-fx-background-color: transparent;" +
                                "-fx-font-size: 18px;" +
                                "-fx-cursor: hand;" +
                                "-fx-padding: 4 6;"
                );

                if (filePath != null) {
                    int receiverId = isManager
                            ? conv.getCandidatId()
                            : conv.getManagerId();

                    // Sauvegarder en BD
                    Message msgVocal = new Message();
                    msgVocal.setConversationId(conv.getId());
                    msgVocal.setSenderId(currentUser.getId());
                    msgVocal.setContent("🎤 Message vocal");
                    msgVocal.setFilePath(filePath);
                    msgVocal.setFileName("vocal.wav");
                    msgVocal.setVocal(true);
                    msgVocal.setAi(false);
                    messageService.envoyerMessage(msgVocal);

                    // Notification
                    notificationService.ajouterNotification(
                            receiverId,
                            currentUser.getFullName() + " vous a envoyé un message vocal",
                            "vocal"
                    );

                    // Rafraîchir
                    chargerMessagesDansContainer(
                            conv.getId(), messagesContainer, scrollMessages, couleur
                    );
                }
            }
        });

// ── Assemblage zone saisie ───────────────────────
        /*
        HBox inputZone = new HBox(8, btnFichier, btnVocal, inputMsg, btnEnvoyer);
        inputZone.setPadding(new Insets(10));
        inputZone.setAlignment(Pos.CENTER);
        inputZone.setStyle("-fx-background-color: white;");
        */

        HBox inputZone = new HBox(8, btnEmoji, btnFichier, btnVocal, inputMsg, btnEnvoyer);
        inputZone.setPadding(new Insets(10));
        inputZone.setAlignment(Pos.CENTER);
        inputZone.setStyle("-fx-background-color: white;");




        //youfa houni jdidd



        // ── Badge IA ─────────────────────────────────────
        HBox iaBadge = new HBox();
        iaBadge.setPadding(new Insets(4, 12, 4, 12));
        iaBadge.setStyle("-fx-background-color: " + couleur + "15;");
        Label iaLabel = new Label("🤖 L'IA répond automatiquement aux questions basiques");
        iaLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + couleur + ";");
        iaBadge.getChildren().add(iaLabel);

        // ── Action envoyer ───────────────────────────────
        Runnable envoyerAction = () -> {
            String texte = inputMsg.getText().trim();
            if (texte.isEmpty()) return;

            int receiverId = isManager
                    ? conv.getCandidatId()
                    : conv.getManagerId();

            Message msg = new Message(conv.getId(),
                    currentUser.getId(), texte, false);
            messageService.envoyerMessage(msg);

            notificationService.ajouterNotification(
                    receiverId,
                    "Nouveau message de " + currentUser.getFullName(),
                    "message"
            );

            if (estQuestionBasique(texte)) {
                String txtFinal = texte;
                new Thread(() -> {
                    String reponseIA = openAIService.obtenirReponse(txtFinal);
                    Message msgIA = new Message(conv.getId(), 0, reponseIA, true);
                    messageService.envoyerMessage(msgIA);
                    Platform.runLater(() ->
                            chargerMessagesDansContainer(conv.getId(),
                                    messagesContainer, scrollMessages, couleur)
                    );
                }).start();
            }

            inputMsg.clear();
            chargerMessagesDansContainer(conv.getId(),
                    messagesContainer, scrollMessages, couleur);
        };

        btnEnvoyer.setOnAction(e -> envoyerAction.run());
        inputMsg.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("ENTER") && !e.isShiftDown()) {
                e.consume();
                envoyerAction.run();
            }
        });

        // ── Assemblage ───────────────────────────────────
        VBox chatBox = new VBox(0, header, scrollMessages, iaBadge, inputZone);
        chatBox.setStyle("-fx-background-color: #f5f5f5;");

        // ── Stage ────────────────────────────────────────
        chatStage = new Stage(StageStyle.DECORATED);
        chatStage.setTitle(nomInterlocuteur);
        chatStage.setScene(new Scene(chatBox, 340, 500));
        chatStage.setResizable(false);
        chatStage.setAlwaysOnTop(true);

        // Position bas droite
        double maxX = Screen.getPrimary().getVisualBounds().getMaxX();
        double maxY = Screen.getPrimary().getVisualBounds().getMaxY();
        chatStage.setX(maxX - 700);
        chatStage.setY(maxY - 560);

        btnFermer.setOnAction(e -> chatStage.close());
        chatStage.show();
    }

    // ── Afficher messages ────────────────────────────────
    /*
    private void chargerMessagesDansContainer(int convId, VBox container,
                                              ScrollPane scroll, String couleur) {
        List<Message> messages = messageService.getMessagesParConversation(convId);
        messageService.marquerCommeLu(convId, currentUser.getId());
        container.getChildren().clear();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Message msg : messages) {
            boolean estMoi = msg.getSenderId() == currentUser.getId();
            boolean estIA  = msg.isAi();

            Label bulle = new Label(estIA ? "🤖  " + msg.getContent() : msg.getContent());
            bulle.setWrapText(true);
            bulle.setMaxWidth(220);
            bulle.setPadding(new Insets(8, 12, 8, 12));

            if (estIA) {
                bulle.setStyle(
                        "-fx-background-color: " + couleur + "15;" +
                                "-fx-background-radius: 15;" +
                                "-fx-border-color: " + couleur + ";" +
                                "-fx-border-radius: 15;" +
                                "-fx-border-width: 1;" +
                                "-fx-font-size: 12px;"
                );
            } else if (estMoi) {
                bulle.setStyle(
                        "-fx-background-color: " + couleur + ";" +
                                "-fx-background-radius: 15 15 4 15;" +
                                "-fx-text-fill: white;" +
                                "-fx-font-size: 12px;"
                );
            } else {
                bulle.setStyle(
                        "-fx-background-color: white;" +
                                "-fx-background-radius: 15 15 15 4;" +
                                "-fx-font-size: 12px;" +
                                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
                );
            }

            Label heure = new Label(
                    msg.getSentAt() != null ? msg.getSentAt().format(fmt) : "");
            heure.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");

            VBox bulleBox = new VBox(2, bulle, heure);
            HBox ligne = new HBox(bulleBox);
            ligne.setPadding(new Insets(2, 0, 2, 0));

            if (estMoi) {
                ligne.setAlignment(Pos.CENTER_RIGHT);
                heure.setStyle("-fx-font-size:10px; -fx-text-fill:#aaa; -fx-alignment:CENTER_RIGHT;");
            } else {
                ligne.setAlignment(Pos.CENTER_LEFT);
            }

            container.getChildren().add(ligne);
        }

        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

     */



    //jdiddddddd
    private void chargerMessagesDansContainer(int convId, VBox container,
                                              ScrollPane scroll, String couleur) {
        List<Message> messages = messageService.getMessagesParConversation(convId);
        messageService.marquerCommeLu(convId, currentUser.getId());
        container.getChildren().clear();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");

        for (Message msg : messages) {
            // boolean estMoi = msg.getSenderId() == currentUser.getId();
            boolean estMoi = msg.getSenderId() != 0
                    && msg.getSenderId() == currentUser.getId();
            boolean estIA  = msg.isAi();

            Label heure = new Label(
                    msg.getSentAt() != null ? msg.getSentAt().format(fmt) : "");
            heure.setStyle("-fx-font-size: 10px; -fx-text-fill: #aaa;");

            if (estMoi) {
                heure.setStyle("-fx-font-size:10px; -fx-text-fill:#aaa; -fx-alignment:CENTER_RIGHT;");
            }

            // ── CAS 1 : Message vocal ────────────────────
            if (msg.isVocal() && msg.getFilePath() != null) {

                HBox vocalBox = new HBox(8);
                vocalBox.setAlignment(Pos.CENTER_LEFT);
                vocalBox.setPadding(new Insets(8, 12, 8, 12));
                vocalBox.setStyle(
                        "-fx-background-color: " + (estMoi ? couleur : "white") + ";" +
                                "-fx-background-radius: 20;" +
                                "-fx-cursor: hand;" +
                                (estMoi ? "" : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);")
                );

                Label icoVocal = new Label("🎤");
                icoVocal.setStyle("-fx-font-size: 16px;");

                Label txtVocal = new Label("Message vocal");
                txtVocal.setStyle(
                        "-fx-text-fill: " + (estMoi ? "white" : "#333") + ";" +
                                "-fx-font-size: 12px;"
                );

                Button btnPlay = new Button("▶");
                btnPlay.setStyle(
                        "-fx-background-color: " + (estMoi ? "rgba(255,255,255,0.3)" : couleur) + ";" +
                                "-fx-text-fill: white;" +
                                "-fx-background-radius: 50;" +
                                "-fx-min-width: 28;" +
                                "-fx-min-height: 28;" +
                                "-fx-cursor: hand;" +
                                "-fx-font-size: 11px;"
                );

                String vocalPath = msg.getFilePath();
                btnPlay.setOnAction(ev -> {
                    vocalService.lireVocal(vocalPath);
                    btnPlay.setText("⏸");
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            javafx.application.Platform.runLater(() -> btnPlay.setText("▶"));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }).start();
                });

                vocalBox.getChildren().addAll(icoVocal, txtVocal, btnPlay);

                VBox bulleBox = new VBox(2, vocalBox, heure);
                HBox ligne = new HBox(bulleBox);
                ligne.setPadding(new Insets(2, 0, 2, 0));
                ligne.setAlignment(estMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                container.getChildren().add(ligne);

                // ── CAS 2 : Fichier ──────────────────────────
            } else if (msg.getFilePath() != null && !msg.getFilePath().isEmpty()) {

                HBox fichierBox = new HBox(8);
                fichierBox.setAlignment(Pos.CENTER_LEFT);
                fichierBox.setPadding(new Insets(8, 12, 8, 12));
                fichierBox.setStyle(
                        "-fx-background-color: " + (estMoi ? couleur : "white") + ";" +
                                "-fx-background-radius: 15;" +
                                "-fx-cursor: hand;" +
                                (estMoi ? "" : "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);")
                );

                Label icoFichier = new Label("📎");
                icoFichier.setStyle("-fx-font-size: 16px;");

                Label nomFichier = new Label(
                        msg.getFileName() != null ? msg.getFileName() : "Fichier"
                );
                nomFichier.setStyle(
                        "-fx-text-fill: " + (estMoi ? "white" : couleur) + ";" +
                                "-fx-font-size: 12px;" +
                                "-fx-underline: true;"
                );

                fichierBox.getChildren().addAll(icoFichier, nomFichier);

                fichierBox.setOnMouseClicked(ev -> {
                    try {
                        java.awt.Desktop.getDesktop()
                                .open(new java.io.File(msg.getFilePath()));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                VBox bulleBox = new VBox(2, fichierBox, heure);
                HBox ligne = new HBox(bulleBox);
                ligne.setPadding(new Insets(2, 0, 2, 0));
                ligne.setAlignment(estMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                container.getChildren().add(ligne);

                // ── CAS 3 : Texte normal ─────────────────────
            } else {

                Label bulle = new Label(estIA ? "🤖  " + msg.getContent() : msg.getContent());
                bulle.setWrapText(true);
                bulle.setMaxWidth(220);
                bulle.setPadding(new Insets(8, 12, 8, 12));

                if (estIA) {
                    bulle.setStyle(
                            "-fx-background-color: " + couleur + "15;" +
                                    "-fx-background-radius: 15;" +
                                    "-fx-border-color: " + couleur + ";" +
                                    "-fx-border-radius: 15;" +
                                    "-fx-border-width: 1;" +
                                    "-fx-font-size: 12px;"
                    );
                } else if (estMoi) {
                    bulle.setStyle(
                            "-fx-background-color: " + couleur + ";" +
                                    "-fx-background-radius: 15 15 4 15;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-font-size: 12px;"
                    );
                } else {
                    bulle.setStyle(
                            "-fx-background-color: white;" +
                                    "-fx-background-radius: 15 15 15 4;" +
                                    "-fx-font-size: 12px;" +
                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);"
                    );
                }

                VBox bulleBox = new VBox(2, bulle, heure);
                HBox ligne = new HBox(bulleBox);
                ligne.setPadding(new Insets(2, 0, 2, 0));
                ligne.setAlignment(estMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                container.getChildren().add(ligne);
            }
        }

        Platform.runLater(() -> scroll.setVvalue(1.0));
    }

    // ── Badge refresh toutes les 15s ─────────────────────
    private void demarrerBadgeRefresh() {
        badgeRefresh = new Timeline(
                new KeyFrame(Duration.seconds(15), e -> {
                    int count = notificationService
                            .compterNotificationsNonLues(currentUser.getId());
                    Platform.runLater(() -> {
                        msgBadge.setText(String.valueOf(count));
                        msgBadge.setVisible(count > 0);
                    });
                })
        );
        badgeRefresh.setCycleCount(Timeline.INDEFINITE);
        badgeRefresh.play();
    }

    // ── Détection question basique ───────────────────────
    private boolean estQuestionBasique(String contenu) {
        String lower = contenu.toLowerCase();
        return lower.contains("salaire")     || lower.contains("poste")       ||
                lower.contains("contrat")     || lower.contains("télétravail") ||
                lower.contains("horaire")     || lower.contains("avantage")    ||
                lower.contains("recrutement") || lower.contains("candidature") ||
                lower.contains("entretien")   || lower.contains("cv")          ||
                lower.contains("bonjour")     || lower.contains("comment")     ||
                lower.contains("délai")       || lower.contains("documents");
    }

    public void stopRefresh() {
        if (badgeRefresh != null) badgeRefresh.stop();
    }
}