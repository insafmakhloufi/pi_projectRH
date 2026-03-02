package Utils;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class OsmLocationPickerDialog {

    private OsmLocationPickerDialog() {
    }

    public static LocationResult showAndWait(Stage owner) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.setTitle("Choisir une localisation");

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        LocationResultHolder holder = new LocationResultHolder();

        BorderPane root = new BorderPane();
        root.setCenter(webView);

        Button cancel = new Button("Annuler");
        Button use = new Button("Utiliser");
        use.setDisable(true);

        HBox actions = new HBox(10);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        actions.getChildren().addAll(spacer, cancel, use);
        root.setBottom(actions);

        cancel.setOnAction(e -> {
            holder.result = null;
            stage.close();
        });

        use.setOnAction(e -> stage.close());

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", new Bridge(holder, use));
        });

        engine.titleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                return;
            }
            if (!newVal.startsWith("PICK:")) {
                return;
            }
            LocationResult parsed = parsePickTitle(newVal);
            if (parsed == null) {
                return;
            }
            holder.result = parsed;
            Platform.runLater(() -> use.setDisable(false));
        });

        engine.loadContent(buildHtml());

        stage.setScene(new Scene(root, 950, 650));
        stage.showAndWait();
        return holder.result;
    }

    private static String buildHtml() {
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
                "    .card { position: absolute; left: 16px; top: 16px; background: rgba(15, 23, 42, 0.86); color: #fff; padding: 10px 12px; border-radius: 12px; font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial; max-width: 560px; z-index: 999; }\n" +
                "    .title { font-size: 13px; opacity: 0.9; }\n" +
                "    .value { margin-top: 6px; font-size: 14px; }\n" +
                "    .hint { margin-top: 6px; font-size: 12px; opacity: 0.75; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div id='map'></div>\n" +
                "  <div class='card'>\n" +
                "    <div class='title'>Clique sur la carte pour choisir une localisation</div>\n" +
                "    <div id='addr' class='value'>Aucune sélection</div>\n" +
                "    <div class='hint'>La sélection sera enregistrée en BD.</div>\n" +
                "  </div>\n" +
                "  <script src='" + leafletJs + "'></script>\n" +
                "  <script>\n" +
                "    const map = L.map('map', { zoomControl: true }).setView([36.8065, 10.1815], 10);\n" +
                "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {\n" +
                "      maxZoom: 19,\n" +
                "      attribution: '&copy; OpenStreetMap contributors'\n" +
                "    }).addTo(map);\n" +
                "\n" +
                "    function invalidateLater() {\n" +
                "      setTimeout(() => { try { map.invalidateSize(true); } catch(e) {} }, 250);\n" +
                "    }\n" +
                "    invalidateLater();\n" +
                "    window.addEventListener('resize', invalidateLater);\n" +
                "\n" +
                "    let marker = null;\n" +
                "\n" +
                "    function setAddrText(t) {\n" +
                "      document.getElementById('addr').textContent = t || 'Aucune sélection';\n" +
                "    }\n" +
                "\n" +
                "    function publishPick(lat, lon, adresse, ville, pays) {\n" +
                "      try {\n" +
                "        const payload = 'PICK:' + lat + ',' + lon + '|' + encodeURIComponent(adresse||'') + '|' + encodeURIComponent(ville||'') + '|' + encodeURIComponent(pays||'');\n" +
                "        document.title = payload;\n" +
                "      } catch(e) {}\n" +
                "      try {\n" +
                "        if (window.javaBridge && window.javaBridge.onPicked) {\n" +
                "          window.javaBridge.onPicked(lat, lon, adresse||'', ville||'', pays||'');\n" +
                "        }\n" +
                "      } catch(e) {}\n" +
                "    }\n" +
                "\n" +
                "    async function reverseGeocode(lat, lon) {\n" +
                "      const url = 'https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=' + encodeURIComponent(lat) + '&lon=' + encodeURIComponent(lon) + '&addressdetails=1';\n" +
                "      const res = await fetch(url);\n" +
                "      const data = await res.json();\n" +
                "      const adresse = (data && data.display_name) ? data.display_name : '';\n" +
                "      const addr = (data && data.address) ? data.address : {};\n" +
                "      const ville = addr.city || addr.town || addr.village || addr.county || '';\n" +
                "      const pays = addr.country || '';\n" +
                "      return { adresse, ville, pays };\n" +
                "    }\n" +
                "\n" +
                "    map.on('click', async (e) => {\n" +
                "      const lat = e.latlng.lat;\n" +
                "      const lon = e.latlng.lng;\n" +
                "      if (!marker) marker = L.marker([lat, lon]).addTo(map);\n" +
                "      else marker.setLatLng([lat, lon]);\n" +
                "\n" +
                "      setAddrText('Chargement...');\n" +
                "      try {\n" +
                "        const geo = await reverseGeocode(lat, lon);\n" +
                "        const display = geo.adresse || ((geo.ville && geo.pays) ? (geo.ville + ', ' + geo.pays) : 'Point sélectionné');\n" +
                "        setAddrText(display);\n" +
                "        publishPick(lat, lon, geo.adresse || '', geo.ville || '', geo.pays || '');\n" +
                "      } catch (err) {\n" +
                "        setAddrText('Point sélectionné');\n" +
                "        publishPick(lat, lon, '', '', '');\n" +
                "      }\n" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>\n";
    }

    private static LocationResult parsePickTitle(String title) {
        try {
            String s = title.substring("PICK:".length());
            String[] parts = s.split("\\|", -1);
            if (parts.length < 1) {
                return null;
            }
            String[] coords = parts[0].split(",", -1);
            if (coords.length != 2) {
                return null;
            }
            double lat = Double.parseDouble(coords[0]);
            double lon = Double.parseDouble(coords[1]);
            String adresse = parts.length > 1 ? urlDecode(parts[1]) : "";
            String ville = parts.length > 2 ? urlDecode(parts[2]) : "";
            String pays = parts.length > 3 ? urlDecode(parts[3]) : "";
            return new LocationResult(lat, lon, adresse, ville, pays);
        } catch (Exception e) {
            return null;
        }
    }

    private static String urlDecode(String s) {
        if (s == null) {
            return "";
        }
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }

    private static final class LocationResultHolder {
        private LocationResult result;
    }

    private static final class Bridge {
        private final LocationResultHolder holder;
        private final Button useBtn;

        private Bridge(LocationResultHolder holder, Button useBtn) {
            this.holder = holder;
            this.useBtn = useBtn;
        }

        public void onPicked(double lat, double lon, String adresse, String ville, String pays) {
            holder.result = new LocationResult(lat, lon, adresse, ville, pays);
            Platform.runLater(() -> useBtn.setDisable(false));
        }
    }

    public static final class LocationResult {
        private final double latitude;
        private final double longitude;
        private final String adresse;
        private final String ville;
        private final String pays;

        public LocationResult(double latitude, double longitude, String adresse, String ville, String pays) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.adresse = adresse;
            this.ville = ville;
            this.pays = pays;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public String getAdresse() {
            return adresse;
        }

        public String getVille() {
            return ville;
        }

        public String getPays() {
            return pays;
        }

        public String getDisplayName() {
            if (adresse != null && !adresse.isBlank()) {
                return adresse.trim();
            }
            String v = ville != null ? ville.trim() : "";
            String p = pays != null ? pays.trim() : "";
            if (!v.isEmpty() && !p.isEmpty()) {
                return v + ", " + p;
            }
            if (!v.isEmpty()) {
                return v;
            }
            if (!p.isEmpty()) {
                return p;
            }
            return latitude + ", " + longitude;
        }
    }
}
