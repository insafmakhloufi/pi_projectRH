package Controllers.User;

import Entites.User.User;
import Services.User.FaceAuthService;
import Services.User.TotpService;
import Services.User.UserService;
import Utils.Session;
import Utils.ThemeManager;
import javafx.embed.swing.SwingFXUtils;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.css.PseudoClass;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

public class AdminDashboardController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{6,}$");
    private static final PseudoClass INVALID_PSEUDO = PseudoClass.getPseudoClass("invalid");

    @FXML private Label pageTitle;
    @FXML private Label todayDateLabel;
    @FXML private TextField searchField;
    @FXML private TextField usersSearchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private MenuButton userMenu;
    @FXML private Circle topbarAvatarCircle;
    @FXML private ImageView topbarAvatarImage;

    @FXML private Button dashboardNav;
    @FXML private Button usersNav;
    @FXML private Button settingsNav;

    @FXML private VBox dashboardPage;
    @FXML private VBox usersPage;
    @FXML private VBox settingsPage;
    @FXML private VBox profilePage;
    @FXML private ScrollPane profilePageScroll;

    @FXML private Label totalUsersLabel;
    @FXML private Label adminsLabel;
    @FXML private Label candidatesLabel;

    @FXML private BarChart<String, Number> usersByRoleChart;
    @FXML private PieChart usersSharePie;

    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colTitre;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colPhone;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TableColumn<User, Void> colActions;

    @FXML private ToggleButton themeToggleTop;
    @FXML private ToggleButton themeToggleSettings;

    @FXML private Label profileNamePreview;
    @FXML private Label profileRolePreview;
    @FXML private Label profileStatusLabel;
    @FXML private Circle profileAvatarCircle;
    @FXML private ImageView profileAvatarImage;
    @FXML private TextField profileFullNameField;
    @FXML private TextField profileEmailField;
    @FXML private TextField profilePhoneField;
    @FXML private TextField profilePhotoField;
    @FXML private javafx.scene.layout.StackPane profilePhotoDropZone;
    @FXML private ImageView profilePhotoPreview;
    @FXML private Label profilePhotoHint;
    @FXML private Button choosePhotoButton;

    @FXML private Label totpStatusLabel;
    @FXML private ImageView totpQrImage;
    @FXML private TextField totpCodeField;
    @FXML private Label totpErrorLabel;
    @FXML private Label totpActionStatusLabel;
    @FXML private Button totpEnableButton;
    @FXML private Button totpDisableButton;
    @FXML private Button totpRefreshQrButton;

    @FXML private Label faceStatusLabel;
    @FXML private Button faceRegisterButton;
    @FXML private Button faceDisableButton;

    private final UserService userService = new UserService();
    private final TotpService totpService = new TotpService();
    private final FaceAuthService faceAuthService = new FaceAuthService();
    private final ObservableList<User> users = FXCollections.observableArrayList();

    @FXML
    public void logout() {
        Session.clear();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);

            Stage stage;
            if (userMenu != null && userMenu.getScene() != null) {
                stage = (Stage) userMenu.getScene().getWindow();
            } else if (usersTable != null && usersTable.getScene() != null) {
                stage = (Stage) usersTable.getScene().getWindow();
            } else {
                return;
            }

            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        User current = Session.getCurrentUser();
        if (current == null) {
            Stage stage = null;
            if (userMenu != null && userMenu.getScene() != null) {
                stage = (Stage) userMenu.getScene().getWindow();
            } else if (usersTable != null && usersTable.getScene() != null) {
                stage = (Stage) usersTable.getScene().getWindow();
            }
            if (stage != null) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/login.fxml"));
                    Scene scene = new Scene(loader.load());
                    ThemeManager.applyTheme(scene);
                    stage.setScene(scene);
                    stage.setMaximized(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        if (todayDateLabel != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            todayDateLabel.setText(LocalDate.now().format(fmt));
        }

        if (colName != null) colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getFullName()));
        if (colTitre != null) colTitre.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getTitre()));
        if (colEmail != null) colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getEmail()));
        if (colPhone != null) colPhone.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getPhone()));
        if (colRole != null) colRole.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().getRole()));

        usersTable.setItems(users);

        userMenu.setText(current.getFullName());
        setupAvatarImageClips();
        updateAvatarImages(current.getProfilePhoto());

        loadProfileFromSession();
        setupProfilePhotoDnD();

        boolean dark = ThemeManager.isDarkMode();
        if (themeToggleTop != null) themeToggleTop.setSelected(dark);
        if (themeToggleSettings != null) themeToggleSettings.setSelected(dark);
        updateThemeToggleGraphics();

        if (roleFilterCombo != null) {
            roleFilterCombo.getItems().setAll("All Roles", "ADMIN", "MANAGER_RH", "CANDIDAT");
            roleFilterCombo.setValue("All Roles");
        }

        installUsersTableRendering();

        refreshUsers();
        showDashboard();

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
        }
        if (usersSearchField != null) {
            usersSearchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter(newVal));
        }
        if (roleFilterCombo != null) {
            roleFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilter(getUsersSearchQuery()));
        }
    }

    private String getUsersSearchQuery() {
        if (usersSearchField == null) return "";
        String v = usersSearchField.getText();
        return v == null ? "" : v;
    }

    private void installUsersTableRendering() {
        if (colName != null) {
            colName.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || getIndex() < 0 || getTableView() == null || getTableView().getItems() == null || getIndex() >= getTableView().getItems().size()) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    User u = getTableView().getItems().get(getIndex());
                    String fullName = u != null ? nvl(u.getFullName()) : "";
                    String initials = initialsOf(fullName);

                    Circle circle = new Circle(14);
                    circle.getStyleClass().add("user-avatar-circle");
                    circle.setFill(Color.web(colorFromString(fullName)));

                    Label init = new Label(initials);
                    init.getStyleClass().add("user-avatar-text");

                    StackPane avatar = new StackPane(circle, init);
                    avatar.getStyleClass().add("user-avatar");

                    Label name = new Label(fullName);
                    name.getStyleClass().add("user-name");

                    HBox box = new HBox(10, avatar, name);
                    box.getStyleClass().add("user-cell");
                    box.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(box);
                    setText(null);
                }
            });
        }

        if (colRole != null) {
            colRole.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Label pill = new Label(item);
                    pill.getStyleClass().addAll("pill", "role-pill");
                    if ("ADMIN".equals(item)) pill.getStyleClass().add("role-admin");
                    else if ("MANAGER_RH".equals(item)) pill.getStyleClass().add("role-rh");
                    else pill.getStyleClass().add("role-cand");
                    setGraphic(pill);
                    setText(null);
                }
            });
        }

        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<>() {
                private final Button viewBtn = createActionIconButton(
                        "M12 5c-7 0-11 7-11 7s4 7 11 7s11-7 11-7s-4-7-11-7Zm0 12a5 5 0 1 1 0-10a5 5 0 0 1 0 10Zm0-8a3 3 0 1 0 0 6a3 3 0 0 0 0-6Z",
                        "action-view");
                private final Button editBtn = createActionIconButton(
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25Zm18.71-11.04a1 1 0 0 0 0-1.41l-2.5-2.5a1 1 0 0 0-1.41 0l-1.83 1.83l3.75 3.75l1.99-1.67Z",
                        "action-edit");
                private final Button delBtn = createActionIconButton(
                        "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12Zm3.46-7.12L11 13.41V17h2v-3.59l1.54-1.53L13.12 10.5L12 11.62L10.88 10.5L9.46 11.88ZM15.5 4l-1-1h-5l-1 1H5v2h14V4h-3.5Z",
                        "action-delete");
                private final HBox box = new HBox(6, viewBtn, editBtn, delBtn);

                {
                    box.getStyleClass().add("actions-cell");
                    viewBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        getTableView().getSelectionModel().select(u);
                    });
                    editBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        getTableView().getSelectionModel().select(u);
                        editSelected();
                    });
                    delBtn.setOnAction(e -> {
                        User u = getTableView().getItems().get(getIndex());
                        getTableView().getSelectionModel().select(u);
                        deleteSelected();
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    setGraphic(box);
                }
            });
        }
    }

    private Button createActionIconButton(String svg, String styleClass) {
        Button btn = new Button("");
        btn.getStyleClass().addAll("icon-button", "action-icon", styleClass);
        SVGPath icon = new SVGPath();
        icon.getStyleClass().add("svg-path");
        icon.setContent(svg);
        btn.setGraphic(icon);
        btn.setFocusTraversable(false);
        return btn;
    }

    private static String initialsOf(String fullName) {
        String v = fullName == null ? "" : fullName.trim();
        if (v.isBlank()) return "?";
        String[] parts = v.split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        String a = parts[0].isEmpty() ? "" : parts[0].substring(0, 1);
        String b = parts[1].isEmpty() ? "" : parts[1].substring(0, 1);
        return (a + b).toUpperCase();
    }

    private static String colorFromString(String input) {
        String s = input == null ? "" : input;
        int h = Math.abs(s.hashCode());
        String[] palette = new String[] { "#7c3aed", "#2563eb", "#0ea5e9", "#14b8a6", "#f59e0b", "#ef4444" };
        return palette[h % palette.length];
    }

    private void setupProfilePhotoDnD() {
        if (profilePhotoDropZone == null) return;

        profilePhotoDropZone.setOnDragOver(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        profilePhotoDropZone.setOnDragEntered(e -> {
            profilePhotoDropZone.getStyleClass().add("drop-zone-active");
            e.consume();
        });

        profilePhotoDropZone.setOnDragExited(e -> {
            profilePhotoDropZone.getStyleClass().remove("drop-zone-active");
            e.consume();
        });

        profilePhotoDropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;
            if (db.hasFiles() && !db.getFiles().isEmpty()) {
                File file = db.getFiles().get(0);
                setProfilePhotoFile(file);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });
    }

    @FXML
    public void chooseProfilePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose profile photo");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
        );

        Stage stage = (Stage) pageTitle.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            setProfilePhotoFile(file);
        }
    }

    private void setProfilePhotoFile(File file) {
        if (file == null) return;

        if (profilePhotoField != null) profilePhotoField.setText(file.getAbsolutePath());
        updateProfilePhotoPreviewFromPath(file.getAbsolutePath());
        updateAvatarImages(file.getAbsolutePath());
    }

    private void loadProfileFromSession() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        if (profileFullNameField != null) profileFullNameField.setText(nvl(current.getFullName()));
        if (profileEmailField != null) profileEmailField.setText(nvl(current.getEmail()));
        if (profilePhoneField != null) profilePhoneField.setText(nvl(current.getPhone()));
        if (profilePhotoField != null) profilePhotoField.setText(nvl(current.getProfilePhoto()));

        updateProfilePhotoPreviewFromPath(nvl(current.getProfilePhoto()));
        setupAvatarImageClips();
        updateAvatarImages(nvl(current.getProfilePhoto()));

        if (profileNamePreview != null) profileNamePreview.setText(nvl(current.getFullName()));
        if (profileRolePreview != null) profileRolePreview.setText(nvl(current.getRole()));
        if (profileStatusLabel != null) profileStatusLabel.setText("");
    }

    private void setupAvatarImageClips() {
        setupAvatarClip(topbarAvatarImage, 14);
        setupAvatarClip(profileAvatarImage, 28);
    }

    private void setupAvatarClip(ImageView imageView, double radius) {
        if (imageView == null) return;
        Circle clip = new Circle(radius);
        clip.setCenterX(radius);
        clip.setCenterY(radius);
        imageView.setClip(clip);
    }

    private void updateAvatarImages(String path) {
        Image img = loadImageOrNull(path);

        if (topbarAvatarImage != null) {
            topbarAvatarImage.setImage(img);
        }
        if (profileAvatarImage != null) {
            profileAvatarImage.setImage(img);
        }

        applyAvatarFallback(topbarAvatarCircle, img);
        applyAvatarFallback(profileAvatarCircle, img);
    }

    private void applyAvatarFallback(Circle circle, Image img) {
        if (circle == null) return;
        if (img == null) {
            circle.setFill(Color.web("#cbd5e1"));
        }
    }

    private Image loadImageOrNull(String path) {
        if (path == null || path.isBlank()) return null;
        String uri = resolveToUri(path);
        if (uri == null || uri.isBlank()) return null;
        Image img = new Image(uri, false);
        return img.isError() ? null : img;
    }

    private String resolveToUri(String path) {
        if (path == null) return "";
        String p = path.trim();
        if (p.isBlank()) return "";

        try {
            if (p.startsWith("file:") || p.startsWith("http://") || p.startsWith("https://")) {
                return p;
            }

            File f = new File(p);
            if (!f.isAbsolute()) {
                f = new File(System.getProperty("user.dir"), p);
            }

            if (f.exists()) {
                return f.toURI().toString();
            }

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private void updateProfilePhotoPreviewFromPath(String path) {
        if (profilePhotoPreview == null) return;

        if (path == null || path.isBlank()) {
            profilePhotoPreview.setImage(null);
            profilePhotoPreview.setVisible(false);
            profilePhotoPreview.setManaged(false);
            if (profilePhotoHint != null) profilePhotoHint.setText("Drag & drop a photo here");
            return;
        }

        try {
            String uri = resolveToUri(path);
            if (uri == null || uri.isBlank()) {
                profilePhotoPreview.setImage(null);
                profilePhotoPreview.setVisible(false);
                profilePhotoPreview.setManaged(false);
                if (profilePhotoHint != null) profilePhotoHint.setText("Drag & drop a photo here");
                return;
            }
            Image img = new Image(uri, false);
            profilePhotoPreview.setImage(img);
            profilePhotoPreview.setVisible(true);
            profilePhotoPreview.setManaged(true);
            if (profilePhotoHint != null) {
                File f = new File(path);
                profilePhotoHint.setText(f.exists() ? f.getName() : path);
            }
        } catch (Exception e) {
            profilePhotoPreview.setImage(null);
            profilePhotoPreview.setVisible(false);
            profilePhotoPreview.setManaged(false);
            if (profilePhotoHint != null) profilePhotoHint.setText("Drag & drop a photo here");
        }
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private void updateThemeToggleGraphics() {
        updateThemeToggleGraphic(themeToggleTop);
        updateThemeToggleGraphic(themeToggleSettings);
    }

    private void updateThemeToggleGraphic(ToggleButton toggle) {
        if (toggle == null) return;

        boolean dark = toggle.isSelected();
        SVGPath icon = new SVGPath();

        icon.setContent(dark
                ? "M21 12.8A8.5 8.5 0 0 1 11.2 3a7 7 0 1 0 9.8 9.8Z"  // moon-ish
                : "M12 18a6 6 0 1 0 0-12a6 6 0 1 0 0 12Z M12 1v3 M12 20v3 M1 12h3 M20 12h3 M4.2 4.2l2.1 2.1 M17.7 17.7l2.1 2.1 M19.8 4.2l-2.1 2.1 M6.3 17.7l-2.1 2.1");

        icon.setFill(dark ? Color.web("#f9fafb") : Color.TRANSPARENT);
        icon.setStroke(dark ? Color.TRANSPARENT : Color.web("#374151"));
        icon.setStrokeWidth(2);

        toggle.setText("");
        toggle.setGraphic(icon);
    }

    private void applyThemeFromToggles(boolean dark) {
        ThemeManager.setDarkMode(dark);
        ThemeManager.applyThemeToAllOpenWindows();

        if (themeToggleTop != null) themeToggleTop.setSelected(dark);
        if (themeToggleSettings != null) themeToggleSettings.setSelected(dark);
        updateThemeToggleGraphics();
    }

    @FXML
    public void toggleThemeFromTop() {
        boolean dark = themeToggleTop != null && themeToggleTop.isSelected();
        applyThemeFromToggles(dark);
    }

    @FXML
    public void toggleThemeFromSettings() {
        boolean dark = themeToggleSettings != null && themeToggleSettings.isSelected();
        applyThemeFromToggles(dark);
    }

    private void applyFilter(String query) {
        if (usersTable == null) return;

        String q = query == null ? "" : query.trim().toLowerCase();
        String roleFilter = roleFilterCombo != null ? roleFilterCombo.getValue() : null;
        boolean allRoles = roleFilter == null || roleFilter.isBlank() || "All Roles".equals(roleFilter);

        ObservableList<User> filtered = FXCollections.observableArrayList();
        for (User u : users) {
            boolean matchQuery = q.isBlank()
                    || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    || (u.getPhone() != null && u.getPhone().toLowerCase().contains(q))
                    || (u.getRole() != null && u.getRole().toLowerCase().contains(q));

            boolean matchRole = allRoles || (u.getRole() != null && u.getRole().equals(roleFilter));

            if (matchQuery && matchRole) {
                filtered.add(u);
            }
        }

        usersTable.setItems(filtered);
    }

    @FXML
    public void showDashboard() {
        setActivePage("Dashboard", dashboardPage);
        setActiveNav(dashboardNav);
    }

    @FXML
    public void showUsers() {
        setActivePage("Users & Authentication", usersPage);
        setActiveNav(usersNav);
    }

    @FXML
    public void showSettings() {
        setActivePage("Settings", settingsPage);
        setActiveNav(settingsNav);
    }

    @FXML
    public void showProfile() {
        setActivePage("Profile", null, profilePageScroll);
        loadProfileFromSession();
        refreshTotpUi();
        refreshFaceUi();
    }

    @FXML
    public void openFaceEnroll() {
        System.out.println("[AdminDashboardController] openFaceEnroll() called");
        User current = Session.getCurrentUser();
        if (current == null) {
            System.out.println("[AdminDashboardController] No current user, returning");
            return;
        }

        try {
            System.out.println("[AdminDashboardController] Loading face_enroll.fxml...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/User/fxml/face_enroll.fxml"));
            Scene scene = new Scene(loader.load());
            ThemeManager.applyTheme(scene);

            System.out.println("[AdminDashboardController] Creating dialog stage...");
            Stage dialog = new Stage(StageStyle.DECORATED);
            dialog.initModality(Modality.WINDOW_MODAL);
            
            Window ownerWindow = null;
            if (profilePage != null && profilePage.getScene() != null && profilePage.getScene().getWindow() != null) {
                ownerWindow = profilePage.getScene().getWindow();
                System.out.println("[AdminDashboardController] Using profilePage window as owner: " + ownerWindow);
            } else if (pageTitle != null && pageTitle.getScene() != null && pageTitle.getScene().getWindow() != null) {
                ownerWindow = pageTitle.getScene().getWindow();
                System.out.println("[AdminDashboardController] Using pageTitle window as owner: " + ownerWindow);
            } else {
                System.out.println("[AdminDashboardController] No owner window found!");
            }
            
            if (ownerWindow != null) {
                dialog.initOwner(ownerWindow);
            }
            
            dialog.setTitle("Register Face");
            dialog.setScene(scene);
            dialog.setResizable(false);
            
            System.out.println("[AdminDashboardController] Showing dialog...");
            dialog.showAndWait();
            System.out.println("[AdminDashboardController] Dialog closed");

            refreshFaceUi();
        } catch (Exception e) {
            System.err.println("[AdminDashboardController] Exception in openFaceEnroll:");
            e.printStackTrace();
        }
    }

    @FXML
    public void disableFaceLogin() {
        User current = Session.getCurrentUser();
        if (current == null) return;
        faceAuthService.disable(current.getId());
        refreshFaceUi();
    }

    private void refreshFaceUi() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        boolean enabled = faceAuthService.isEnabled(current.getId());
        boolean hasModel = faceAuthService.hasModel(current.getId());

        if (faceStatusLabel != null) {
            faceStatusLabel.setText(enabled && hasModel ? "Enabled" : (hasModel ? "Registered" : "Not enabled"));
        }
        if (faceDisableButton != null) {
            faceDisableButton.setDisable(!enabled);
        }
    }

    @FXML
    public void refreshTotpQr() {
        refreshTotpUi();
    }

    @FXML
    public void enableTotp() {
        clearTotpMessages();
        User current = Session.getCurrentUser();
        if (current == null) return;

        String code = totpCodeField == null ? "" : totpCodeField.getText();
        if (code == null || !code.trim().matches("\\d{6}")) {
            if (totpErrorLabel != null) totpErrorLabel.setText("Code must be 6 digits");
            return;
        }

        boolean ok = totpService.verifyCode(current.getId(), code);
        if (!ok) {
            if (totpErrorLabel != null) totpErrorLabel.setText("Invalid code");
            return;
        }

        totpService.enable(current.getId());
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("2FA enabled");
        refreshTotpUi();
    }

    @FXML
    public void disableTotp() {
        clearTotpMessages();
        User current = Session.getCurrentUser();
        if (current == null) return;
        totpService.disable(current.getId());
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("2FA disabled");
        refreshTotpUi();
    }

    private void refreshTotpUi() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        boolean enabled = totpService.isEnabled(current.getId());
        if (totpStatusLabel != null) {
            totpStatusLabel.setText(enabled ? "Enabled" : "Not enabled");
        }

        if (totpEnableButton != null) {
            totpEnableButton.setDisable(enabled);
        }
        if (totpDisableButton != null) {
            totpDisableButton.setDisable(!enabled);
        }

        String secret = totpService.getOrCreateSecretBase32(current.getId());
        String uri = totpService.buildOtpAuthUri("CareerLink", current.getEmail(), secret);

        if (totpQrImage != null) {
            totpQrImage.setImage(buildQrImage(uri, 220, 220));
        }

        if (totpCodeField != null) {
            totpCodeField.setDisable(enabled);
        }
    }

    private Image buildQrImage(String text, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(matrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (WriterException e) {
            return null;
        }
    }

    private void clearTotpMessages() {
        if (totpErrorLabel != null) totpErrorLabel.setText("");
        if (totpActionStatusLabel != null) totpActionStatusLabel.setText("");
    }

    @FXML
    public void saveProfile() {
        User current = Session.getCurrentUser();
        if (current == null) return;

        String fullName = profileFullNameField != null ? profileFullNameField.getText() : current.getFullName();
        String email = profileEmailField != null ? profileEmailField.getText() : current.getEmail();
        String phone = profilePhoneField != null ? profilePhoneField.getText() : current.getPhone();
        String photo = profilePhotoField != null ? profilePhotoField.getText() : current.getProfilePhoto();

        User updated = new User(
                current.getId(),
                fullName,
                email,
                phone,
                photo,
                current.getPassword(),
                current.getRole()
        );

        boolean ok = userService.updateUser(updated);
        if (ok) {
            current.setFullName(fullName);
            current.setEmail(email);
            current.setPhone(phone);
            current.setProfilePhoto(photo);

            userMenu.setText(current.getFullName());
            setupAvatarImageClips();
            updateAvatarImages(current.getProfilePhoto());
            if (profileNamePreview != null) profileNamePreview.setText(nvl(current.getFullName()));
            if (profileRolePreview != null) profileRolePreview.setText(nvl(current.getRole()));
            if (profileStatusLabel != null) profileStatusLabel.setText("Saved successfully");

            refreshUsers();
        } else {
            if (profileStatusLabel != null) profileStatusLabel.setText("Unable to save changes");
        }
    }

    private void setActivePage(String title, VBox active) {
        setActivePage(title, active, null);
    }

    private void setActivePage(String title, VBox active, ScrollPane activeScroll) {
        pageTitle.setText(title);

        dashboardPage.setVisible(active == dashboardPage);
        dashboardPage.setManaged(active == dashboardPage);

        usersPage.setVisible(active == usersPage);
        usersPage.setManaged(active == usersPage);

        settingsPage.setVisible(active == settingsPage);
        settingsPage.setManaged(active == settingsPage);

        if (profilePage != null) {
            profilePage.setVisible(true);
            profilePage.setManaged(true);
        }
        if (profilePageScroll != null) {
            profilePageScroll.setVisible(activeScroll == profilePageScroll);
            profilePageScroll.setManaged(activeScroll == profilePageScroll);
        }
    }

    private void setActiveNav(Button active) {
        dashboardNav.getStyleClass().remove("active");
        usersNav.getStyleClass().remove("active");
        settingsNav.getStyleClass().remove("active");
        active.getStyleClass().add("active");
    }

    @FXML
    public void refreshUsers() {
        List<User> all = userService.getAllUsers();
        users.setAll(all);
        applyFilter(searchField.getText());

        long admins = all.stream().filter(u -> "ADMIN".equals(u.getRole())).count();
        long candidates = all.stream().filter(u -> "CANDIDAT".equals(u.getRole())).count();
        long managers = all.stream().filter(u -> "MANAGER_RH".equals(u.getRole())).count();

        if (totalUsersLabel != null) totalUsersLabel.setText(String.valueOf(all.size()));
        if (adminsLabel != null) adminsLabel.setText(String.valueOf(admins));
        if (candidatesLabel != null) candidatesLabel.setText(String.valueOf(candidates));

        if (usersByRoleChart != null) {
            usersByRoleChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Admins", admins));
            series.getData().add(new XYChart.Data<>("Managers", managers));
            series.getData().add(new XYChart.Data<>("Candidates", candidates));
            usersByRoleChart.getData().add(series);
        }

        if (usersSharePie != null) {
            ObservableList<PieChart.Data> pie = FXCollections.observableArrayList(
                    new PieChart.Data("Admins", admins),
                    new PieChart.Data("Managers", managers),
                    new PieChart.Data("Candidates", candidates)
            );
            usersSharePie.setData(pie);
        }
    }

    @FXML
    public void createUser() {
        Stage ownerStage;
        try {
            ownerStage = (Stage) pageTitle.getScene().getWindow();
        } catch (Exception e) {
            return;
        }

        Stage modal = new Stage();
        modal.initOwner(ownerStage);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("modal-backdrop");
        backdrop.setOnMouseClicked(e -> modal.close());

        TextField name = new TextField();
        TextField titre = new TextField();
        TextField email = new TextField();
        TextField phone = new TextField();
        PasswordField password = new PasswordField();
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("ADMIN", "MANAGER_RH", "CANDIDAT");

        name.setPromptText("Enter full name");
        titre.setPromptText("Enter title (optional)");
        email.setPromptText("email@example.com");
        phone.setPromptText("8-15 digits");
        password.setPromptText("Enter password");
        role.setPromptText("Select a role");

        Label nameError = new Label("");
        Label titreError = new Label("");
        Label emailError = new Label("");
        Label phoneError = new Label("");
        Label passwordError = new Label("");
        Label roleError = new Label("");
        nameError.getStyleClass().add("field-error");
        titreError.getStyleClass().add("field-error");
        emailError.getStyleClass().add("field-error");
        phoneError.getStyleClass().add("field-error");
        passwordError.getStyleClass().add("field-error");
        roleError.getStyleClass().add("field-error");

        VBox nameBox = labeledWithError("Full Name", name, nameError);
        VBox titreBox = labeledWithError("Titre", titre, titreError);
        VBox emailBox = labeledWithError("Email", email, emailError);
        VBox phoneBox = labeledWithError("Phone", phone, phoneError);
        VBox passBox = labeledWithError("Password", password, passwordError);
        VBox roleBox = labeledWithError("Role", role, roleError);

        Label title = new Label("Create New User");
        title.getStyleClass().add("modal-title");
        Label subtitle = new Label("Add a new user to CareerLink");
        subtitle.getStyleClass().add("modal-subtitle");
        VBox titleBox = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("");
        closeBtn.getStyleClass().addAll("icon-button", "modal-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.getStyleClass().add("svg-path");
        closeIcon.setContent("M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.4 4.29 19.71 2.88 18.29 9.17 12 2.88 5.71 4.29 4.29 10.59 10.6 16.89 4.29z");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> modal.close());

        HBox header = new HBox(10, titleBox, spacer, closeBtn);
        header.getStyleClass().add("modal-header");

        Circle avatarCircle = new Circle(26);
        avatarCircle.getStyleClass().add("modal-avatar-circle");
        Label avatarLetter = new Label("?");
        avatarLetter.getStyleClass().add("modal-avatar-letter");
        StackPane avatar = new StackPane(avatarCircle, avatarLetter);
        avatar.getStyleClass().add("modal-avatar");

        ImageView avatarImage = new ImageView();
        avatarImage.setFitWidth(52);
        avatarImage.setFitHeight(52);
        avatarImage.setPreserveRatio(true);
        Circle clip = new Circle(26, 26, 26);
        avatarImage.setClip(clip);
        avatarImage.setVisible(false);
        avatar.getChildren().add(avatarImage);

        final String[] photoPath = new String[] { null };
        Button upload = new Button("Upload Photo");
        upload.getStyleClass().add("btn-secondary");
        SVGPath upIcon = new SVGPath();
        upIcon.getStyleClass().add("svg-path");
        upIcon.setContent("M5 20h14v-2H5v2Zm7-18l-5.5 5.5 1.42 1.42L11 6.84V16h2V6.84l3.08 3.08 1.42-1.42L12 2Z");
        upload.setGraphic(upIcon);
        upload.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose profile photo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
            );
            File file = chooser.showOpenDialog(modal);
            if (file != null) {
                photoPath[0] = file.getAbsolutePath();
                Image img = loadImageOrNull(photoPath[0]);
                if (img != null) {
                    avatarImage.setImage(img);
                    avatarImage.setVisible(true);
                    avatarLetter.setVisible(false);
                }
            }
        });

        Label photoHint = new Label("JPG or PNG, max 2MB");
        photoHint.getStyleClass().add("modal-hint");

        VBox avatarBlock = new VBox(8, avatar, upload, photoHint);
        avatarBlock.setAlignment(Pos.CENTER);
        avatarBlock.getStyleClass().add("modal-avatar-block");

        GridPane form = new GridPane();
        form.getStyleClass().add("modal-form-grid");
        form.setHgap(12);
        form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        form.getColumnConstraints().addAll(c1, c2);

        form.add(nameBox, 0, 0, 2, 1);
        form.add(titreBox, 0, 1, 2, 1);
        form.add(emailBox, 0, 2);
        form.add(phoneBox, 1, 2);
        form.add(passBox, 0, 3, 2, 1);
        form.add(roleBox, 0, 4, 2, 1);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> modal.close());

        Button createBtn = new Button("Create");
        createBtn.getStyleClass().add("btn-primary");

        HBox footer = new HBox(10, cancelBtn, createBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("modal-footer");

        VBox card = new VBox(14, header, avatarBlock, form, footer);
        card.getStyleClass().addAll("modal-dialog", "modal-card");

        StackPane root = new StackPane(backdrop, card);
        root.getStyleClass().addAll("modal-stage-root", "app-root");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);
        modal.setScene(scene);

        root.widthProperty().addListener((obs, o, n) -> backdrop.setPrefWidth(n.doubleValue()));
        root.heightProperty().addListener((obs, o, n) -> backdrop.setPrefHeight(n.doubleValue()));

        Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        modal.setX(bounds.getMinX());
        modal.setY(bounds.getMinY());
        modal.setWidth(bounds.getWidth());
        modal.setHeight(bounds.getHeight());

        PauseTransition nameDebounce = new PauseTransition(Duration.millis(350));
        PauseTransition emailDebounce = new PauseTransition(Duration.millis(350));
        PauseTransition phoneDebounce = new PauseTransition(Duration.millis(350));

        Runnable validateAll = () -> {
            boolean ok = validateNameBasic(name.getText(), nameError, name)
                    & validateEmailBasic(email.getText(), emailError, email)
                    & validatePhoneBasic(phone.getText(), phoneError, phone)
                    & validatePassword(password.getText(), passwordError, password)
                    & validateRole(role.getValue(), roleError, role);
            createBtn.setDisable(!ok);
        };

        name.textProperty().addListener((obs, oldVal, newVal) -> {
            validateNameBasic(newVal, nameError, name);
            nameDebounce.stop();
            if (validateNameBasic(newVal, nameError, name)) {
                String value = newVal == null ? "" : newVal.trim();
                nameDebounce.setOnFinished(e -> {
                    if (userService.fullNameExists(value)) {
                        nameError.setText("Full name already exists");
                        name.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Full name already exists".equals(nameError.getText())) {
                        nameError.setText("");
                        name.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                nameDebounce.playFromStart();
            }
            validateAll.run();
        });

        email.textProperty().addListener((obs, oldVal, newVal) -> {
            validateEmailBasic(newVal, emailError, email);
            emailDebounce.stop();
            if (validateEmailBasic(newVal, emailError, email)) {
                String value = newVal == null ? "" : newVal.trim();
                emailDebounce.setOnFinished(e -> {
                    if (userService.emailExists(value)) {
                        emailError.setText("Email already exists");
                        email.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Email already exists".equals(emailError.getText())) {
                        emailError.setText("");
                        email.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                emailDebounce.playFromStart();
            }
            validateAll.run();
        });

        phone.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePhoneBasic(newVal, phoneError, phone);
            phoneDebounce.stop();
            if (validatePhoneBasic(newVal, phoneError, phone)) {
                String value = newVal == null ? "" : newVal.trim();
                phoneDebounce.setOnFinished(e -> {
                    if (userService.phoneExists(value)) {
                        phoneError.setText("Phone already exists");
                        phone.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Phone already exists".equals(phoneError.getText())) {
                        phoneError.setText("");
                        phone.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                phoneDebounce.playFromStart();
            }
            validateAll.run();
        });

        password.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePassword(newVal, passwordError, password);
            validateAll.run();
        });

        role.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateRole(newVal, roleError, role);
            validateAll.run();
        });

        validateAll.run();

        createBtn.setOnAction(e -> {
            validateAll.run();
            if (createBtn.isDisabled()) return;

            User u = new User(
                    name.getText(),
                    titre.getText(),
                    email.getText(),
                    phone.getText(),
                    photoPath[0],
                    password.getText(),
                    role.getValue()
            );
            if (userService.addUser(u)) {
                refreshUsers();
                showUsers();
                modal.close();
            }
        });

        modal.showAndWait();
    }

    private VBox labeledWithError(String text, Control control, Label error) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, control, error);
        box.getStyleClass().add("dialog-field");
        return box;
    }

    private boolean validateNameBasic(String name, Label error, Control control) {
        String value = name == null ? "" : name.trim();
        if (value.isEmpty()) {
            error.setText("Full name is required");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (value.length() < 3) {
            error.setText("Minimum 3 characters");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!"Full name already exists".equals(error.getText())) {
            error.setText("");
            control.pseudoClassStateChanged(INVALID_PSEUDO, false);
        }
        return error.getText().isEmpty();
    }

    private boolean validateEmailBasic(String email, Label error, Control control) {
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            error.setText("Email is required");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            error.setText("Invalid email format");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!"Email already exists".equals(error.getText())) {
            error.setText("");
            control.pseudoClassStateChanged(INVALID_PSEUDO, false);
        }
        return error.getText().isEmpty();
    }

    private boolean validatePhoneBasic(String phone, Label error, Control control) {
        String value = phone == null ? "" : phone.trim();
        if (value.isEmpty()) {
            error.setText("Phone is required");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!value.matches("\\d{8,15}")) {
            error.setText("Phone must be 8-15 digits");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!"Phone already exists".equals(error.getText())) {
            error.setText("");
            control.pseudoClassStateChanged(INVALID_PSEUDO, false);
        }
        return error.getText().isEmpty();
    }

    private boolean validatePassword(String password, Label error, Control control) {
        String value = password == null ? "" : password.trim();
        if (value.isEmpty()) {
            error.setText("Password is required");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        if (!PASSWORD_PATTERN.matcher(value).matches()) {
            error.setText("Password must have 1 uppercase, 1 lowercase, 1 digit, 1 special char");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        error.setText("");
        control.pseudoClassStateChanged(INVALID_PSEUDO, false);
        return true;
    }

    private boolean validateRole(String role, Label error, Control control) {
        if (role == null) {
            error.setText("Role is required");
            control.pseudoClassStateChanged(INVALID_PSEUDO, true);
            return false;
        }
        error.setText("");
        control.pseudoClassStateChanged(INVALID_PSEUDO, false);
        return true;
    }

    @FXML
    public void editSelected() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Stage ownerStage;
        try {
            ownerStage = (Stage) pageTitle.getScene().getWindow();
        } catch (Exception e) {
            return;
        }

        Stage modal = new Stage();
        modal.initOwner(ownerStage);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("modal-backdrop");
        backdrop.setOnMouseClicked(e -> modal.close());

        TextField name = new TextField(nvl(selected.getFullName()));
        TextField titre = new TextField(nvl(selected.getTitre()));
        TextField email = new TextField(nvl(selected.getEmail()));
        TextField phone = new TextField(nvl(selected.getPhone()));
        ComboBox<String> role = new ComboBox<>();
        role.getItems().addAll("ADMIN", "MANAGER_RH", "CANDIDAT");
        role.setValue(selected.getRole());

        name.setPromptText("Enter full name");
        titre.setPromptText("Enter title (optional)");
        email.setPromptText("email@example.com");
        phone.setPromptText("8-15 digits");
        role.setPromptText("Select a role");

        Label nameError = new Label("");
        Label titreError = new Label("");
        Label emailError = new Label("");
        Label phoneError = new Label("");
        Label roleError = new Label("");
        nameError.getStyleClass().add("field-error");
        titreError.getStyleClass().add("field-error");
        emailError.getStyleClass().add("field-error");
        phoneError.getStyleClass().add("field-error");
        roleError.getStyleClass().add("field-error");

        VBox nameBox = labeledWithError("Full Name", name, nameError);
        VBox titreBox = labeledWithError("Titre", titre, titreError);
        VBox emailBox = labeledWithError("Email", email, emailError);
        VBox phoneBox = labeledWithError("Phone", phone, phoneError);
        VBox roleBox = labeledWithError("Role", role, roleError);

        Label title = new Label("Edit User");
        title.getStyleClass().add("modal-title");
        Label subtitle = new Label("Update user information");
        subtitle.getStyleClass().add("modal-subtitle");
        VBox titleBox = new VBox(2, title, subtitle);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("");
        closeBtn.getStyleClass().addAll("icon-button", "modal-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.getStyleClass().add("svg-path");
        closeIcon.setContent("M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.4 4.29 19.71 2.88 18.29 9.17 12 2.88 5.71 4.29 4.29 10.59 10.6 16.89 4.29z");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> modal.close());

        HBox header = new HBox(10, titleBox, spacer, closeBtn);
        header.getStyleClass().add("modal-header");

        Circle avatarCircle = new Circle(26);
        avatarCircle.getStyleClass().add("modal-avatar-circle");
        Label avatarLetter = new Label("?");
        avatarLetter.getStyleClass().add("modal-avatar-letter");
        StackPane avatar = new StackPane(avatarCircle, avatarLetter);
        avatar.getStyleClass().add("modal-avatar");

        ImageView avatarImage = new ImageView();
        avatarImage.setFitWidth(52);
        avatarImage.setFitHeight(52);
        avatarImage.setPreserveRatio(true);
        Circle clip = new Circle(26, 26, 26);
        avatarImage.setClip(clip);
        avatarImage.setVisible(false);
        avatar.getChildren().add(avatarImage);

        final String[] photoPath = new String[] { selected.getProfilePhoto() };
        Image initialImg = loadImageOrNull(photoPath[0]);
        if (initialImg != null) {
            avatarImage.setImage(initialImg);
            avatarImage.setVisible(true);
            avatarLetter.setVisible(false);
        } else {
            avatarLetter.setText(initialsOf(nvl(selected.getFullName())));
        }

        Button upload = new Button("Upload Photo");
        upload.getStyleClass().add("btn-secondary");
        SVGPath upIcon = new SVGPath();
        upIcon.getStyleClass().add("svg-path");
        upIcon.setContent("M5 20h14v-2H5v2Zm7-18l-5.5 5.5 1.42 1.42L11 6.84V16h2V6.84l3.08 3.08 1.42-1.42L12 2Z");
        upload.setGraphic(upIcon);
        upload.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose profile photo");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp")
            );
            File file = chooser.showOpenDialog(modal);
            if (file != null) {
                photoPath[0] = file.getAbsolutePath();
                Image img = loadImageOrNull(photoPath[0]);
                if (img != null) {
                    avatarImage.setImage(img);
                    avatarImage.setVisible(true);
                    avatarLetter.setVisible(false);
                }
            }
        });

        Label photoHint = new Label("JPG or PNG, max 2MB");
        photoHint.getStyleClass().add("modal-hint");

        VBox avatarBlock = new VBox(8, avatar, upload, photoHint);
        avatarBlock.setAlignment(Pos.CENTER);
        avatarBlock.getStyleClass().add("modal-avatar-block");

        GridPane form = new GridPane();
        form.getStyleClass().add("modal-form-grid");
        form.setHgap(12);
        form.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(50);
        form.getColumnConstraints().addAll(c1, c2);

        form.add(nameBox, 0, 0, 2, 1);
        form.add(titreBox, 0, 1, 2, 1);
        form.add(emailBox, 0, 2);
        form.add(phoneBox, 1, 2);
        form.add(roleBox, 0, 3, 2, 1);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> modal.close());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");

        HBox footer = new HBox(10, cancelBtn, saveBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("modal-footer");

        VBox card = new VBox(14, header, avatarBlock, form, footer);
        card.getStyleClass().addAll("modal-dialog", "modal-card");

        StackPane root = new StackPane(backdrop, card);
        root.getStyleClass().addAll("modal-stage-root", "app-root");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);
        modal.setScene(scene);

        root.widthProperty().addListener((obs, o, n) -> backdrop.setPrefWidth(n.doubleValue()));
        root.heightProperty().addListener((obs, o, n) -> backdrop.setPrefHeight(n.doubleValue()));

        Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        modal.setX(bounds.getMinX());
        modal.setY(bounds.getMinY());
        modal.setWidth(bounds.getWidth());
        modal.setHeight(bounds.getHeight());

        PauseTransition nameDebounce = new PauseTransition(Duration.millis(350));
        PauseTransition emailDebounce = new PauseTransition(Duration.millis(350));
        PauseTransition phoneDebounce = new PauseTransition(Duration.millis(350));

        Runnable validateAll = () -> {
            boolean ok = validateNameBasic(name.getText(), nameError, name)
                    & validateEmailBasic(email.getText(), emailError, email)
                    & validatePhoneBasic(phone.getText(), phoneError, phone)
                    & validateRole(role.getValue(), roleError, role);
            saveBtn.setDisable(!ok);
        };

        name.textProperty().addListener((obs, oldVal, newVal) -> {
            validateNameBasic(newVal, nameError, name);
            nameDebounce.stop();
            if (validateNameBasic(newVal, nameError, name)) {
                String value = newVal == null ? "" : newVal.trim();
                nameDebounce.setOnFinished(e -> {
                    if (userService.fullNameExistsForOther(value, selected.getId())) {
                        nameError.setText("Full name already exists");
                        name.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Full name already exists".equals(nameError.getText())) {
                        nameError.setText("");
                        name.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                nameDebounce.playFromStart();
            }
            if (!avatarImage.isVisible()) {
                avatarLetter.setText(initialsOf(newVal));
            }
            validateAll.run();
        });

        email.textProperty().addListener((obs, oldVal, newVal) -> {
            validateEmailBasic(newVal, emailError, email);
            emailDebounce.stop();
            if (validateEmailBasic(newVal, emailError, email)) {
                String value = newVal == null ? "" : newVal.trim();
                emailDebounce.setOnFinished(e -> {
                    if (userService.emailExistsForOther(value, selected.getId())) {
                        emailError.setText("Email already exists");
                        email.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Email already exists".equals(emailError.getText())) {
                        emailError.setText("");
                        email.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                emailDebounce.playFromStart();
            }
            validateAll.run();
        });

        phone.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePhoneBasic(newVal, phoneError, phone);
            phoneDebounce.stop();
            if (validatePhoneBasic(newVal, phoneError, phone)) {
                String value = newVal == null ? "" : newVal.trim();
                phoneDebounce.setOnFinished(e -> {
                    if (userService.phoneExistsForOther(value, selected.getId())) {
                        phoneError.setText("Phone already exists");
                        phone.pseudoClassStateChanged(INVALID_PSEUDO, true);
                    } else if ("Phone already exists".equals(phoneError.getText())) {
                        phoneError.setText("");
                        phone.pseudoClassStateChanged(INVALID_PSEUDO, false);
                    }
                    validateAll.run();
                });
                phoneDebounce.playFromStart();
            }
            validateAll.run();
        });

        role.valueProperty().addListener((obs, oldVal, newVal) -> {
            validateRole(newVal, roleError, role);
            validateAll.run();
        });

        validateAll.run();

        saveBtn.setOnAction(e -> {
            validateAll.run();
            if (saveBtn.isDisabled()) return;

            User u = new User(
                    selected.getId(),
                    name.getText(),
                    titre.getText(),
                    email.getText(),
                    phone.getText(),
                    photoPath[0],
                    selected.getPassword(),
                    role.getValue()
            );
            if (userService.updateUser(u)) {
                refreshUsers();
                showUsers();
                modal.close();
            }
        });

        modal.showAndWait();
    }

    @FXML
    public void deleteSelected() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Stage ownerStage;
        try {
            ownerStage = (Stage) pageTitle.getScene().getWindow();
        } catch (Exception e) {
            return;
        }

        Stage modal = new Stage();
        modal.initOwner(ownerStage);
        modal.initModality(Modality.WINDOW_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);

        Region backdrop = new Region();
        backdrop.getStyleClass().add("modal-backdrop");
        backdrop.setOnMouseClicked(e -> modal.close());

        Label title = new Label("Delete User");
        title.getStyleClass().add("modal-title");
        Label subtitle = new Label("This action cannot be undone");
        subtitle.getStyleClass().add("modal-subtitle");
        VBox titleBox = new VBox(2, title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("");
        closeBtn.getStyleClass().addAll("icon-button", "modal-close");
        SVGPath closeIcon = new SVGPath();
        closeIcon.getStyleClass().add("svg-path");
        closeIcon.setContent("M18.3 5.71 12 12l6.3 6.29-1.41 1.42L10.59 13.4 4.29 19.71 2.88 18.29 9.17 12 2.88 5.71 4.29 4.29 10.59 10.6 16.89 4.29z");
        closeBtn.setGraphic(closeIcon);
        closeBtn.setOnAction(e -> modal.close());

        HBox header = new HBox(10, titleBox, spacer, closeBtn);
        header.getStyleClass().add("modal-header");

        Label question = new Label("Delete selected user?");
        question.getStyleClass().add("headline");

        Label userLine = new Label(nvl(selected.getFullName()) + " (" + nvl(selected.getEmail()) + ")");
        userLine.getStyleClass().add("subhead");

        VBox body = new VBox(8, question, userLine);
        VBox.setVgrow(body, Priority.NEVER);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-secondary");
        cancelBtn.setOnAction(e -> modal.close());

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("btn-primary", "btn-danger");

        HBox footer = new HBox(10, cancelBtn, deleteBtn);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("modal-footer");

        VBox card = new VBox(14, header, body, footer);
        card.getStyleClass().addAll("modal-dialog", "modal-card");
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setPrefHeight(Region.USE_COMPUTED_SIZE);

        StackPane root = new StackPane(backdrop, card);
        root.getStyleClass().addAll("modal-stage-root", "app-root");
        StackPane.setAlignment(card, Pos.CENTER);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        ThemeManager.applyTheme(scene);
        modal.setScene(scene);

        root.widthProperty().addListener((obs, o, n) -> backdrop.setPrefWidth(n.doubleValue()));
        root.heightProperty().addListener((obs, o, n) -> backdrop.setPrefHeight(n.doubleValue()));

        Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        modal.setX(bounds.getMinX());
        modal.setY(bounds.getMinY());
        modal.setWidth(bounds.getWidth());
        modal.setHeight(bounds.getHeight());

        deleteBtn.setOnAction(e -> {
            if (userService.deleteUser(selected.getId())) {
                refreshUsers();
            }
            modal.close();
        });

        modal.showAndWait();
    }
    private VBox labeled(String text, Control control) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, control);
        return box;
    }
}
