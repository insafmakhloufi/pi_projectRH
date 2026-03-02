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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class MapboxLocationPickerDialog {

    private MapboxLocationPickerDialog() {
    }

    public static LocationResult showAndWait(Stage owner) {
        String token = System.getenv("MAPBOX_TOKEN");
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("MAPBOX_TOKEN non défini. Ajoute la variable d'environnement MAPBOX_TOKEN.");
        }

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

        use.setOnAction(e -> {
            stage.close();
        });

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaBridge", new Bridge(holder, use));
        });

        String html = buildHtml(token);
        engine.loadContent(html);

        stage.setScene(new Scene(root, 950, 650));
        stage.showAndWait();

        return holder.result;
    }

    private static String buildHtml(String token) {
        String safeToken = escapeJs(token);
        String styleUrl = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.css";
        String scriptUrl = "https://api.mapbox.com/mapbox-gl-js/v2.15.0/mapbox-gl.js";

        return "<!doctype html>\n" +
                "<html>\n" +
                "<head>\n" +
                "  <meta charset='utf-8' />\n" +
                "  <meta name='viewport' content='initial-scale=1,maximum-scale=1,user-scalable=no' />\n" +
                "  <link href='" + styleUrl + "' rel='stylesheet' />\n" +
                "  <style>\n" +
                "    html, body { height: 100%; margin: 0; }\n" +
                "    #map { position: absolute; top: 0; bottom: 0; width: 100%; }\n" +
                "    .card { position: absolute; left: 16px; top: 16px; background: rgba(15, 23, 42, 0.85); color: #fff; padding: 10px 12px; border-radius: 12px; font-family: system-ui, -apple-system, Segoe UI, Roboto, Arial; max-width: 520px; }\n" +
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
                "    <div class='hint'>Tu peux zoomer/déplacer. La sélection sera enregistrée en BD.</div>\n" +
                "  </div>\n" +
                "  <script src='" + scriptUrl + "'></script>\n" +
                "  <script>\n" +
                "    mapboxgl.accessToken = '" + safeToken + "';\n" +
                "    const map = new mapboxgl.Map({\n" +
                "      container: 'map',\n" +
                "      style: 'mapbox://styles/mapbox/streets-v12',\n" +
                "      center: [10.1815, 36.8065],\n" +
                "      zoom: 10\n" +
                "    });\n" +
                "    map.addControl(new mapboxgl.NavigationControl());\n" +
                "    let marker = null;\n" +
                "\n" +
                "    function setAddrText(t) {\n" +
                "      const el = document.getElementById('addr');\n" +
                "      el.textContent = t || 'Aucune sélection';\n" +
                "    }\n" +
                "\n" +
                "    async function reverseGeocode(lng, lat) {\n" +
                "      const url = 'https://api.mapbox.com/geocoding/v5/mapbox.places/' + encodeURIComponent(lng + ',' + lat) + '.json?types=address,place,locality,region,country&language=fr&limit=1&access_token=' + mapboxgl.accessToken;\n" +
                "      const res = await fetch(url);\n" +
                "      const data = await res.json();\n" +
                "      let adresse = '';\n" +
                "      let ville = '';\n" +
                "      let pays = '';\n" +
                "      if (data && data.features && data.features.length > 0) {\n" +
                "        const f = data.features[0];\n" +
                "        adresse = f.place_name || '';\n" +
                "        if (f.context && Array.isArray(f.context)) {\n" +
                "          for (const c of f.context) {\n" +
                "            if (c && typeof c.id === 'string') {\n" +
                "              if (c.id.startsWith('place')) ville = c.text || ville;\n" +
                "              if (c.id.startsWith('country')) pays = c.text || pays;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "      return { adresse, ville, pays };\n" +
                "    }\n" +
                "\n" +
                "    map.on('click', async (e) => {\n" +
                "      const lng = e.lngLat.lng;\n" +
                "      const lat = e.lngLat.lat;\n" +
                "      if (!marker) {\n" +
                "        marker = new mapboxgl.Marker({ color: '#2563eb' }).setLngLat([lng, lat]).addTo(map);\n" +
                "      } else {\n" +
                "        marker.setLngLat([lng, lat]);\n" +
                "      }\n" +
                "      setAddrText('Chargement...');\n" +
                "      try {\n" +
                "        const geo = await reverseGeocode(lng, lat);\n" +
                "        const display = geo.adresse || (geo.ville && geo.pays ? (geo.ville + ', ' + geo.pays) : 'Point sélectionné');\n" +
                "        setAddrText(display);\n" +
                "        if (window.javaBridge && window.javaBridge.onPicked) {\n" +
                "          window.javaBridge.onPicked(lat, lng, geo.adresse || '', geo.ville || '', geo.pays || '');\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        setAddrText('Point sélectionné');\n" +
                "        if (window.javaBridge && window.javaBridge.onPicked) {\n" +
                "          window.javaBridge.onPicked(lat, lng, '', '', '');\n" +
                "        }\n" +
                "      }\n" +
                "    });\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>\n";
    }

    private static String escapeJs(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "");
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

        public void onPicked(double lat, double lng, String adresse, String ville, String pays) {
            holder.result = new LocationResult(lat, lng, adresse, ville, pays);
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
