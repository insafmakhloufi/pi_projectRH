package Controllers.Offre;

import Entities.Offre.OffreEmploi;
import Services.Offre.OfrreEmploiServices;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;

import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * Contrôleur pour le Virtual Tour 3D des entreprises et offres d'emploi
 * Mode AR-lite : exploration 3D immersive des locaux et offres
 */
public class VirtualTour3DController implements Initializable {

    @FXML private StackPane rootContainer;
    @FXML private VBox welcomeScreen;
    @FXML private VBox tourUI;
    @FXML private VBox controlsPanel;
    @FXML private VBox closePanel;
    @FXML private VBox legendPanel;
    @FXML private Button btnStartTour;
    @FXML private Button btnClose;
    @FXML private Button btnResetView;
    @FXML private Button btnToggleLabels;
    @FXML private Button btnHelp;
    
    // Scène 3D
    private SubScene subScene3D;
    private Group worldGroup;
    private PerspectiveCamera camera;
    private PointLight cameraLight;
    
    // Navigation
    private double mousePosX, mousePosY;
    private double mouseOldX, mouseOldY;
    private double cameraX = 0, cameraY = -500, cameraZ = -1500;
    private double cameraYaw = 0, cameraPitch = 20;
    private final double moveSpeed = 50;
    private final double rotateSpeed = 0.5;

    // Cadrage
    private double worldRadius = 800;
    
    // État
    private boolean showLabels = true;
    private AnimationTimer gameLoop;
    private Set<KeyCode> pressedKeys = new HashSet<>();
    
    // Données
    private List<OffreEmploi> offres;
    private Map<String, Building3D> buildings = new HashMap<>();
    private final OfrreEmploiServices offreService = new OfrreEmploiServices();
    
    // Couleurs modernes - AMÉLIORÉES pour meilleure visibilité
    private static final Color COLOR_GROUND = Color.web("#f1f5f9"); // Gris clair
    private static final Color COLOR_BUILDING = Color.web("#f97316"); // Orange vif
    private static final Color COLOR_BUILDING_HIGHLIGHT = Color.web("#fb923c"); // Orange clair
    private static final Color COLOR_OFFER_MARKER = Color.web("#22c55e"); // Vert vif
    private static final Color COLOR_OFFER_GLOW = Color.web("#4ade80"); // Vert brillant
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        init3DScene();
        initControls();
        loadOffersAndBuildWorld();
        startGameLoop();
    }
    
    private void init3DScene() {
        // Créer le monde 3D
        worldGroup = new Group();
        
        // Configurer la caméra
        camera = new PerspectiveCamera(true);
        camera.setNearClip(10);
        camera.setFarClip(5000);
        camera.setFieldOfView(60);
        updateCameraPosition();

        // Lumière attachée à la caméra (améliore fortement la visibilité)
        cameraLight = new PointLight(Color.web("#ffffff"));
        cameraLight.setTranslateX(cameraX);
        cameraLight.setTranslateY(cameraY);
        cameraLight.setTranslateZ(cameraZ);
        worldGroup.getChildren().add(cameraLight);
        
        // Créer la SubScene 3D directement avec le worldGroup
        subScene3D = new SubScene(worldGroup, 1200, 800, true, SceneAntialiasing.BALANCED);
        subScene3D.setFill(Color.web("#1e293b")); // Fond gris-bleu plus clair
        subScene3D.setCamera(camera);

        // Responsive: la SubScene doit remplir la fenêtre
        subScene3D.widthProperty().bind(rootContainer.widthProperty());
        subScene3D.heightProperty().bind(rootContainer.heightProperty());
        
        // Ajouter à la racine
        rootContainer.getChildren().add(0, subScene3D);
        
        // Configurer les événements souris
        setupMouseControls();
        setupKeyboardControls();
    }
    
    private void updateCameraPosition() {
        camera.getTransforms().clear();
        
        // Rotation (pitch puis yaw)
        Rotate rotateX = new Rotate(cameraPitch, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(cameraYaw, Rotate.Y_AXIS);
        
        // Translation
        camera.setTranslateX(cameraX);
        camera.setTranslateY(cameraY);
        camera.setTranslateZ(cameraZ);

        if (cameraLight != null) {
            cameraLight.setTranslateX(cameraX);
            cameraLight.setTranslateY(cameraY);
            cameraLight.setTranslateZ(cameraZ);
        }
        
        camera.getTransforms().addAll(rotateX, rotateY);
    }
    
    private void setupMouseControls() {
        subScene3D.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
            subScene3D.requestFocus();
        });
        
        subScene3D.setOnMouseDragged((MouseEvent me) -> {
            double dx = me.getSceneX() - mouseOldX;
            double dy = me.getSceneY() - mouseOldY;
            
            if (me.isPrimaryButtonDown()) {
                // Rotation caméra
                cameraYaw += dx * rotateSpeed;
                cameraPitch -= dy * rotateSpeed;
                cameraPitch = Math.max(-80, Math.min(80, cameraPitch));
                updateCameraPosition();
            }
            
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        
        // Scroll pour zoom
        subScene3D.setOnScroll((event) -> {
            double zoom = event.getDeltaY() > 0 ? -100 : 100;
            cameraZ += zoom;
            cameraZ = Math.max(-3000, Math.min(-500, cameraZ));
            updateCameraPosition();
        });
    }
    
    private void setupKeyboardControls() {
        subScene3D.setFocusTraversable(true);
        subScene3D.requestFocus();
        
        subScene3D.setOnKeyPressed((event) -> {
            pressedKeys.add(event.getCode());
        });
        
        subScene3D.setOnKeyReleased((event) -> {
            pressedKeys.remove(event.getCode());
        });
    }
    
    private void initControls() {
        btnStartTour.setOnAction(e -> startTour());
        btnClose.setOnAction(this::closeTour);
        btnResetView.setOnAction(e -> {
            resetCamera();
            subScene3D.requestFocus();
        });
        btnToggleLabels.setOnAction(e -> {
            toggleLabels();
            subScene3D.requestFocus();
        });
        btnHelp.setOnAction(e -> {
            showHelpDialog();
            Platform.runLater(() -> subScene3D.requestFocus());
        });
        
        // Cacher la scène 3D au début
        subScene3D.setVisible(false);
    }
    
    private void startTour() {
        // Cacher l'écran d'accueil
        welcomeScreen.setVisible(false);
        welcomeScreen.setManaged(false);
        
        // Montrer la scène 3D
        subScene3D.setVisible(true);

        // Remettre le texte du bouton (si on a ouvert l'aide auparavant)
        btnStartTour.setText("🚀 Commencer le Virtual Tour");
        
        // Montrer l'UI du tour
        tourUI.setVisible(true);
        tourUI.setManaged(true);
        controlsPanel.setVisible(true);
        controlsPanel.setManaged(true);
        closePanel.setVisible(true);
        closePanel.setManaged(true);
        legendPanel.setVisible(true);
        legendPanel.setManaged(true);
        
        // Focus sur la scène 3D
        subScene3D.requestFocus();
    }
    
    private void showHelpDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aide - Virtual Tour 3D");
        alert.setHeaderText("Comment utiliser le Virtual Tour 3D");
        alert.setContentText(
            "🏢 Bâtiments ORANGE : Entreprises (plus c'est haut = plus d'offres)\n" +
            "🟢 Sphères VERTES : Offres d'emploi\n\n" +
            "🎮 Contrôles :\n" +
            "- WASD : se déplacer\n" +
            "- Souris (clic gauche + glisser) : tourner la caméra\n" +
            "- Scroll : zoom\n" +
            "- Espace / Shift : monter / descendre\n\n" +
            "Astuce : cliquez sur 'Vue initiale' si vous êtes perdu."
        );
        if (rootContainer != null && rootContainer.getScene() != null) {
            alert.initOwner(rootContainer.getScene().getWindow());
        }
        alert.showAndWait();
    }
    
    private void loadOffersAndBuildWorld() {
        // Charger les offres
        offres = offreService.afficherOfrreEmploiLight();

        if (offres == null) {
            offres = new ArrayList<>();
        }
        
        // Créer le sol
        createGround();
        
        // Créer les bâtiments par entreprise
        Map<String, List<OffreEmploi>> offresParEntreprise = new HashMap<>();
        for (OffreEmploi offre : offres) {
            String company = offre.getNomEntreprise();
            if (company == null || company.isBlank()) company = "Autres";
            offresParEntreprise.computeIfAbsent(company, k -> new ArrayList<>()).add(offre);
        }
        
        // Positionner les bâtiments en cercle
        int index = 0;
        int total = offresParEntreprise.size();
        // Rayon dynamique: assure que les sociétés restent "dans le cadre" au centre
        // (valeurs calibrées pour une bonne visibilité)
        double radius = Math.max(450, Math.min(900, 180 + (total * 90)));
        worldRadius = radius;

        if (total == 0) {
            // Même si aucune offre, on garde un éclairage et la scène stable
            createLighting();
            return;
        }
        
        for (Map.Entry<String, List<OffreEmploi>> entry : offresParEntreprise.entrySet()) {
            double angle = (2 * Math.PI * index) / total;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            
            Building3D building = createBuilding(entry.getKey(), entry.getValue(), x, z);
            buildings.put(entry.getKey(), building);
            worldGroup.getChildren().add(building.getNode());
            
            index++;
        }
        
        // Créer éclairage
        createLighting();
        
        // Animation d'intro
        frameWorldInView();
        animateIntro();
    }

    private void frameWorldInView() {
        // Place la caméra pour que les sociétés soient visibles au milieu
        cameraX = 0;
        cameraYaw = 0;
        cameraPitch = 28;

        // On recule la caméra en fonction du rayon
        cameraZ = -(worldRadius + 1200);
        // Légèrement au-dessus du sol
        cameraY = -250;
        updateCameraPosition();
    }
    
    private void createGround() {
        // Sol principal
        Box ground = new Box(3000, 10, 3000);
        PhongMaterial groundMat = new PhongMaterial(COLOR_GROUND);
        groundMat.setSpecularColor(Color.WHITE);
        groundMat.setSpecularPower(10);
        ground.setMaterial(groundMat);
        ground.setTranslateY(100);
        worldGroup.getChildren().add(ground);
        
        // Grille décorative (plus visible)
        for (int i = -1500; i <= 1500; i += 200) {
            Box lineX = new Box(3000, 2, 2);
            lineX.setMaterial(new PhongMaterial(Color.web("#94a3b8")));
            lineX.setTranslateY(95);
            lineX.setTranslateZ(i);
            worldGroup.getChildren().add(lineX);
            
            Box lineZ = new Box(2, 2, 3000);
            lineZ.setMaterial(new PhongMaterial(Color.web("#94a3b8")));
            lineZ.setTranslateY(95);
            lineZ.setTranslateX(i);
            worldGroup.getChildren().add(lineZ);
        }
    }
    
    private Building3D createBuilding(String companyName, List<OffreEmploi> offres, double x, double z) {
        Group buildingGroup = new Group();
        
        // Hauteur basée sur le nombre d'offres
        double baseHeight = 200;
        double heightPerOffer = 80;
        double totalHeight = baseHeight + (offres.size() * heightPerOffer);
        
        // Base du bâtiment (plus grande pour meilleure visibilité)
        Box base = new Box(220, totalHeight, 220);
        PhongMaterial baseMat = new PhongMaterial(COLOR_BUILDING);
        baseMat.setSpecularColor(Color.WHITE);
        baseMat.setSpecularPower(30);
        base.setMaterial(baseMat);
        base.setTranslateY(-totalHeight / 2 + 100);
        
        // Effet de brillance (glow)
        Glow glow = new Glow(0.3);
        base.setEffect(glow);
        
        buildingGroup.getChildren().add(base);
        
        // Label avec le nom de l'entreprise au sommet
        VBox companyLabel = createCompanyLabel(companyName, offres.size());
        companyLabel.setTranslateY(-totalHeight - 60);
        buildingGroup.getChildren().add(companyLabel);
        
        // Marqueurs d'offres flottants
        for (int i = 0; i < offres.size(); i++) {
            OffreEmploi offre = offres.get(i);
            
            // Sphère lumineuse
            Sphere marker = new Sphere(20);
            PhongMaterial markerMat = new PhongMaterial(COLOR_OFFER_MARKER);
            markerMat.setSpecularColor(COLOR_OFFER_GLOW);
            markerMat.setSpecularPower(50);
            marker.setMaterial(markerMat);
            
            double angle = (2 * Math.PI * i) / offres.size();
            double mx = Math.cos(angle) * 100;
            double mz = Math.sin(angle) * 100;
            double my = -150 - (i * 50);
            
            marker.setTranslateX(mx);
            marker.setTranslateY(my);
            marker.setTranslateZ(mz);
            
            // Animation flottante
            animateFloating(marker, my, i);
            
            // Label 3D avec le titre de l'offre
            if (showLabels) {
                VBox label3D = createFloatingLabel(offre);
                label3D.setTranslateX(mx + 30);
                label3D.setTranslateY(my);
                label3D.setTranslateZ(mz);
                label3D.setRotationAxis(Rotate.Y_AXIS);
                label3D.setRotate(-30);
                buildingGroup.getChildren().add(label3D);
            }
            
            buildingGroup.getChildren().add(marker);
        }
        
        // Positionnement global
        buildingGroup.setTranslateX(x);
        buildingGroup.setTranslateZ(z);
        
        return new Building3D(companyName, offres, buildingGroup);
    }
    
    private VBox createCompanyLabel(String companyName, int offerCount) {
        VBox labelBox = new VBox(5);
        labelBox.setAlignment(javafx.geometry.Pos.CENTER);
        labelBox.setStyle(
            "-fx-background-color: rgba(249, 115, 22, 0.98);" + // Orange vif opaque
            "-fx-padding: 15 25;" + // Plus de padding
            "-fx-background-radius: 25;" +
            "-fx-border-color: #fff7ed;" + // Bordure blanche
            "-fx-border-width: 3;" + // Bordure plus épaisse
            "-fx-border-radius: 25;"
        );
        
        // Nom de l'entreprise - POLICE PLUS GROSSE
        String displayName = companyName.length() > 18 ? companyName.substring(0, 18) + "..." : companyName;
        Text nameText = new Text("🏢 " + displayName);
        nameText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18)); // 18px au lieu de 14
        nameText.setFill(Color.WHITE);
        nameText.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 0.5, 0, 0);"); // Ombre pour lisibilité
        
        // Nombre d'offres
        String offerText = offerCount == 1 ? "1 offre" : offerCount + " offres";
        Text countText = new Text(offerText);
        countText.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); // 14px au lieu de 11
        countText.setFill(Color.web("#ffedd5"));
        countText.setStyle("-fx-effect: dropshadow(gaussian, black, 1, 0.5, 0, 0);");
        
        labelBox.getChildren().addAll(nameText, countText);
        
        // Toujours face à la caméra
        labelBox.setRotationAxis(Rotate.Y_AXIS);
        labelBox.setRotate(0);
        
        return labelBox;
    }

    private VBox createFloatingLabel(OffreEmploi offre) {
        VBox labelBox = new VBox(5);
        labelBox.setStyle(
            "-fx-background-color: rgba(34, 197, 94, 0.98);" + // Vert vif opaque
            "-fx-padding: 15 20;" + // Plus de padding
            "-fx-background-radius: 15;" +
            "-fx-border-color: #f0fdf4;" + // Bordure blanche
            "-fx-border-width: 2;" +
            "-fx-border-radius: 15;"
        );
        
        // Titre du poste - POLICE PLUS GROSSE
        Text title = new Text(offre.getTitre() != null ? offre.getTitre() : "Poste");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14)); // 14px au lieu de 12
        title.setFill(Color.WHITE);
        title.setStyle("-fx-effect: dropshadow(gaussian, black, 2, 0.5, 0, 0);");
        
        // Nom entreprise
        Text company = new Text(offre.getNomEntreprise() != null ? offre.getNomEntreprise() : "Entreprise");
        company.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12)); // 12px au lieu de 10
        company.setFill(Color.web("#dcfce7"));
        company.setStyle("-fx-effect: dropshadow(gaussian, black, 1, 0.5, 0, 0);");
        
        labelBox.getChildren().addAll(title, company);
        
        return labelBox;
    }
    
    private void animateFloating(Sphere marker, double baseY, int index) {
        final double[] time = {0};
        AnimationTimer floatAnim = new AnimationTimer() {
            @Override
            public void handle(long now) {
                time[0] += 0.02;
                double offset = Math.sin(time[0] + index) * 10;
                marker.setTranslateY(baseY + offset);
            }
        };
        floatAnim.start();
    }
    
    private void createLighting() {
        // Lumière ambiante (plus forte)
        AmbientLight ambient = new AmbientLight(Color.web("#ffffff"));
        worldGroup.getChildren().add(ambient);
        
        // Lumière directionnelle (plus proche)
        PointLight sun = new PointLight(Color.web("#ffffff"));
        sun.setTranslateX(-1000);
        sun.setTranslateY(-1200);
        sun.setTranslateZ(-1000);
        worldGroup.getChildren().add(sun);
        
        // Lumière ponctuelle au centre pour le contraste
        PointLight center = new PointLight(Color.web("#f97316"));
        center.setTranslateY(-300);
        worldGroup.getChildren().add(center);
    }

    private void animateIntro() {
        // Animation de caméra d'introduction
        final double[] t = {0};

        double startZ = -(worldRadius + 2200);
        double endZ = -(worldRadius + 1200);
        double startY = -650;
        double endY = -250;

        cameraZ = startZ;

        AnimationTimer intro = new AnimationTimer() {
            @Override
            public void handle(long now) {
                t[0] += 0.01;
                if (t[0] <= 1) {
                    double ease = 1 - Math.pow(1 - t[0], 3);
                    cameraZ = startZ + ((endZ - startZ) * ease);
                    cameraY = startY + ((endY - startY) * ease);
                    cameraPitch = 40 - (12 * ease);
                    updateCameraPosition();
                } else {
                    this.stop();
                }
            }
        };
        intro.start();
    }
    
    private void startGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                processInput();
            }
        };
        gameLoop.start();
    }
    
    private void processInput() {
        double moveX = 0, moveZ = 0;
        
        // Convertir les touches en mouvement
        double yawRad = Math.toRadians(cameraYaw);
        
        if (pressedKeys.contains(KeyCode.W)) {
            moveX += Math.sin(yawRad) * moveSpeed;
            moveZ += Math.cos(yawRad) * moveSpeed;
        }
        if (pressedKeys.contains(KeyCode.S)) {
            moveX -= Math.sin(yawRad) * moveSpeed;
            moveZ -= Math.cos(yawRad) * moveSpeed;
        }
        if (pressedKeys.contains(KeyCode.A)) {
            moveX += Math.sin(yawRad - Math.PI/2) * moveSpeed;
            moveZ += Math.cos(yawRad - Math.PI/2) * moveSpeed;
        }
        if (pressedKeys.contains(KeyCode.D)) {
            moveX += Math.sin(yawRad + Math.PI/2) * moveSpeed;
            moveZ += Math.cos(yawRad + Math.PI/2) * moveSpeed;
        }
        if (pressedKeys.contains(KeyCode.SPACE)) {
            cameraY += moveSpeed;
        }
        if (pressedKeys.contains(KeyCode.SHIFT)) {
            cameraY -= moveSpeed;
        }
        
        // Appliquer mouvement
        cameraX += moveX;
        cameraZ += moveZ;
        
        // Limites du monde
        cameraX = Math.max(-1400, Math.min(1400, cameraX));
        cameraZ = Math.max(-1400, Math.min(1400, cameraZ));
        cameraY = Math.max(-1500, Math.min(0, cameraY));
        
        if (moveX != 0 || moveZ != 0 || pressedKeys.contains(KeyCode.SPACE) || pressedKeys.contains(KeyCode.SHIFT)) {
            updateCameraPosition();
        }
    }
    
    private void resetCamera() {
        frameWorldInView();
        animateIntro();
        Platform.runLater(() -> subScene3D.requestFocus());
    }
    
    private void toggleLabels() {
        showLabels = !showLabels;
        // Recharger la scène avec/sans labels
        // IMPORTANT : conserver la lumière caméra (sinon la scène devient sombre/invisible)
        worldGroup.getChildren().clear();
        if (cameraLight != null) {
            worldGroup.getChildren().add(cameraLight);
        }
        loadOffersAndBuildWorld();
    }
    
    @FXML
    private void closeTour(ActionEvent event) {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        // Fermer la fenêtre
        Stage stage = (Stage) rootContainer.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Classe interne représentant un bâtiment 3D
     */
    private static class Building3D {
        private final String companyName;
        private final List<OffreEmploi> offres;
        private final Group node;
        
        public Building3D(String companyName, List<OffreEmploi> offres, Group node) {
            this.companyName = companyName;
            this.offres = offres;
            this.node = node;
        }
        
        public Group getNode() { return node; }
        public String getCompanyName() { return companyName; }
        public List<OffreEmploi> getOffres() { return offres; }
    }
}
