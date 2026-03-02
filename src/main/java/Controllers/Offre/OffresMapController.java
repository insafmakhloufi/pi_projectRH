package Controllers.Offre;

import Services.MapOffreService;
import Services.MapOffreService.OffreMapData;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la carte interactive des offres d'emploi
 * Affiche les offres sur une carte Leaflet avec filtres par rayon
 */
public class OffresMapController implements Initializable {

    @FXML
    private BorderPane rootPane;

    @FXML
    private WebView mapWebView;

    @FXML
    private ComboBox<Integer> rayonCombo;

    @FXML
    private Label statusLabel;

    @FXML
    private Label countLabel;

    private WebEngine engine;
    private MapOffreService mapService;
    private List<OffreMapData> allOffres;
    private boolean mapReady = false;
    private double userLat = 36.8065;  // Position par défaut (Tunis)
    private double userLon = 10.1815;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mapService = new MapOffreService();
        engine = mapWebView.getEngine();

        // Initialiser les filtres
        setupFilters();

        // Charger la carte
        loadMap();

        // Charger les offres
        loadOffres();
    }

    private void setupFilters() {
        // Options de rayon en km
        rayonCombo.getItems().addAll(10, 25, 50, 100, 200);
        rayonCombo.setValue(50); // Par défaut 50km

        rayonCombo.setOnAction(e -> applyFilter());
    }

    private void loadMap() {
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                System.out.println("[DEBUG] Carte chargée avec succès");
                // La carte est chargée, injecter les marqueurs
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaController", new JavaBridge());
                mapReady = true;
                
                // Si les offres sont déjà chargées, mettre à jour les marqueurs
                if (allOffres != null && !allOffres.isEmpty()) {
                    Platform.runLater(() -> updateMarkers());
                }
            }
        });

        engine.loadContent(buildMapHtml());
    }

    private void loadOffres() {
        try {
            allOffres = mapService.getOffresAvecLocalisation();
            System.out.println("[DEBUG] Total offres récupérées: " + allOffres.size());
            
            // Compter combien ont des coordonnées valides
            long validCount = allOffres.stream().filter(OffreMapData::hasValidCoordinates).count();
            System.out.println("[DEBUG] Offres avec coordonnées valides: " + validCount);
            
            if (allOffres.size() > 0) {
                OffreMapData first = allOffres.get(0);
                System.out.println("[DEBUG] Première offre: " + first);
                System.out.println("[DEBUG] Lat: " + first.getLatitude() + ", Lon: " + first.getLongitude());
            }
            
            updateStatus(allOffres.size() + " offres trouvées (" + validCount + " avec coordonnées)");
            
            if (allOffres.isEmpty()) {
                System.out.println("[DEBUG] Aucune offre avec coordonnées GPS trouvée!");
                // Afficher quand même la carte, mais sans marqueurs
                if (mapReady) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Aucune offre géolocalisée");
                        alert.setHeaderText(null);
                        alert.setContentText("Aucune offre active avec coordonnées GPS trouvée.\n\n" +
                                "Vérifiez que vos offres ont des localisations avec latitude/longitude.\n" +
                                "Utilisez le bouton '🗺 Choisir sur la carte' lors de la création d'offres.");
                        alert.showAndWait();
                    });
                }
            } else if (mapReady) {
                // Si la carte est déjà chargée, mettre à jour les marqueurs maintenant
                Platform.runLater(() -> updateMarkers());
            }
            // Sinon les marqueurs seront mis à jour quand la carte sera prête (dans loadMap)
        } catch (Exception e) {
            System.err.println("[DEBUG] Erreur loadOffres: " + e.getMessage());
            e.printStackTrace();
            updateStatus("Erreur: " + e.getMessage());
        }
    }

    private void applyFilter() {
        if (allOffres == null) return;

        int rayon = rayonCombo.getValue();
        List<OffreMapData> filtered = mapService.getOffresDansRayon(userLat, userLon, rayon);

        updateStatus(filtered.size() + " offres dans un rayon de " + rayon + " km");
        updateMarkers(filtered);
    }

    private void updateMarkers() {
        updateMarkers(allOffres);
    }

    private void updateMarkers(List<OffreMapData> offres) {
        if (offres == null || engine == null) {
            System.out.println("[DEBUG] updateMarkers: offres=null ou engine=null");
            return;
        }
        
        if (!mapReady) {
            System.out.println("[DEBUG] updateMarkers: carte pas encore prête");
            return;
        }

        // Convertir les offres en JSON pour JavaScript
        String markersJson = offres.stream()
                .filter(OffreMapData::hasValidCoordinates)
                .map(this::toMarkerJson)
                .collect(Collectors.joining(", ", "[", "]"));
        
        System.out.println("[DEBUG] JSON marqueurs: " + markersJson);

        // Utiliser une approche en deux étapes: d'abord définir la variable, puis appeler addMarkers
        String scriptSetData = "window.markerData = " + markersJson + ";";
        String scriptCallFunction = "if (typeof addMarkers === 'function') { console.log('addMarkers trouvé, appel...'); addMarkers(window.markerData); } else { console.log('ERREUR: addMarkers non défini'); }";

        Platform.runLater(() -> {
            try {
                // Étape 1: Définir les données
                engine.executeScript(scriptSetData);
                System.out.println("[DEBUG] Données définies dans window.markerData");
                
                // Étape 2: Appeler la fonction après un court délai
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                pause.setOnFinished(e -> {
                    try {
                        engine.executeScript(scriptCallFunction);
                        System.out.println("[DEBUG] Fonction addMarkers appelée");
                    } catch (Exception ex) {
                        System.err.println("[DEBUG] Erreur appel addMarkers: " + ex.getMessage());
                    }
                });
                pause.play();
            } catch (Exception e) {
                System.err.println("[DEBUG] Erreur mise à jour marqueurs: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String toMarkerJson(OffreMapData offre) {
        String title = escapeJson(offre.getTitre());
        String popup = escapeJson(offre.getPopupContent());
        // Forcer le format anglais avec point décimal pour JSON valide
        return String.format(java.util.Locale.US,
                "{\"id\": %d, \"lat\": %.6f, \"lon\": %.6f, \"title\": \"%s\", \"popup\": \"%s\"}",
                offre.getId(),
                offre.getLatitude(),
                offre.getLongitude(),
                title,
                popup
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("'", "\\'")
                .replace("\n", " ")
                .replace("\r", "")
                .replace("\t", " ")
                .replace("<b>", "")
                .replace("</b>", "")
                .replace("🏢", "Ent: ")
                .replace("📍", "Lieu: ");
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            }
        });
    }

    /**
     * Bridge JavaScript -> Java pour les interactions sur la carte
     */
    public class JavaBridge {
        public void onMarkerClick(int offreId) {
            Platform.runLater(() -> {
                System.out.println("Offre cliquée: " + offreId);
                // Ouvrir le détail de l'offre
                ouvrirDetailOffre(offreId);
            });
        }

        public void onPostulerClick(int offreId) {
            Platform.runLater(() -> {
                System.out.println("Postuler cliqué pour offre: " + offreId);
                // Fermer la carte et ouvrir le détail de l'offre pour postuler
                ouvrirDetailOffrePourPostuler(offreId);
            });
        }

        public void onMapClick(double lat, double lon) {
            userLat = lat;
            userLon = lon;
            Platform.runLater(() -> applyFilter());
        }
    }

    private void ouvrirDetailOffre(int offreId) {
        // TODO: Implémenter l'ouverture du détail de l'offre
        System.out.println("Ouverture détail offre: " + offreId);
    }

    private void ouvrirDetailOffrePourPostuler(int offreId) {
        // Fermer la fenêtre de carte
        handleClose();
        // Ouvrir le détail de l'offre avec focus sur la section postuler
        System.out.println("Ouverture détail offre pour postuler: " + offreId);
        // TODO: Naviguer vers la page de détail de l'offre
    }

    private String buildMapHtml() {
        String leafletCss = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
        String leafletJs = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";

        return "<!doctype html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset='utf-8' />\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0' />\n" +
                "  <link rel='stylesheet' href='" + leafletCss + "'/>\n" +
                "  <style>\n" +
                "    html, body { height: 100%; margin: 0; }\n" +
                "    #map { position: absolute; inset: 0; }\n" +
                "    .leaflet-popup-content { font-family: system-ui, sans-serif; font-size: 13px; margin: 12px; }\n" +
                "    .leaflet-popup-content button:hover { background: #1D4ED8 !important; transform: translateY(-1px); box-shadow: 0 4px 8px rgba(37,99,235,0.4) !important; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id='map'></div>\n" +
                "  <script src='" + leafletJs + "'></script>\n" +
                "  <script>\n" +
                "    const map = L.map('map').setView([36.8065, 10.1815], 8);\n" +
                "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "      maxZoom: 19,\n" +
                "      attribution: '&copy; OpenStreetMap contributors'\n" +
                "    }).addTo(map);\n" +
                "\n" +
                "    let markers = [];\n" +
                "\n" +
                "    // Fonction appelée quand on clique sur Postuler dans le popup\n" +
                "    function postulerOffre(offreId) {\n" +
                "      console.log('Postuler clicked for offre:', offreId);\n" +
                "      if (window.javaController) {\n" +
                "        window.javaController.onPostulerClick(offreId);\n" +
                "      } else {\n" +
                "        console.log('ERREUR: javaController non défini');\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function addMarkers(data) {\n" +
                "      console.log('addMarkers appelé avec:', data);\n" +
                "      console.log('Nombre d\\'offres:', data ? data.length : 0);\n" +
                "      \n" +
                "      // Supprimer anciens marqueurs\n" +
                "      markers.forEach(m => map.removeLayer(m));\n" +
                "      markers = [];\n" +
                "\n" +
                "      if (!data || data.length === 0) {\n" +
                "        console.log('Aucune donnée à afficher');\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      // Créer groupe pour centrer la carte\n" +
                "      const group = [];\n" +
                "\n" +
                "      data.forEach(o => {\n" +
                "        console.log('Création marqueur pour:', o.title, 'lat:', o.lat, 'lon:', o.lon);\n" +
                "        const marker = L.marker([o.lat, o.lon]).addTo(map);\n" +
                "        marker.bindPopup(o.popup);\n" +
                "        marker.on('click', () => {\n" +
                "          if (window.javaController) window.javaController.onMarkerClick(o.id);\n" +
                "        });\n" +
                "        markers.push(marker);\n" +
                "        group.push([o.lat, o.lon]);\n" +
                "      });\n" +
                "\n" +
                "      // Centrer sur les marqueurs\n" +
                "      if (group.length > 0) {\n" +
                "        console.log('Centrage sur', group.length, 'marqueurs');\n" +
                "        map.fitBounds(group, { padding: [50, 50], maxZoom: 13 });\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    map.on('click', e => {\n" +
                "      const lat = e.latlng.lat;\n" +
                "      const lon = e.latlng.lng;\n" +
                "      if (window.javaController) window.javaController.onMapClick(lat, lon);\n" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>\n";
    }

    @FXML
    private void handleClose() {
        // Fermer la fenêtre de la carte
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            rootPane.getScene().getWindow().hide();
        }
    }

    @FXML
    void forceRefreshMarkers(ActionEvent event) {
        System.out.println("[DEBUG] Force refresh markers");
        if (allOffres == null) {
            loadOffres();
        }
        updateMarkers();
        updateStatus("Marqueurs rafraîchis: " + allOffres.size() + " offres");
    }
}
