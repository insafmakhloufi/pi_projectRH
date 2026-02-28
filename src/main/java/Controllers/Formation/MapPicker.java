package Controllers.Formation;

import Utils.NominatimGeocoder;
import Utils.UiState;
import Utils.WindowUtil;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

import java.net.URL;

public class MapPicker {

    @FXML
    private WebView webView;

    @FXML
    private Label lblSelected;

    @FXML
    private Button btnValider;

    @FXML
    private Button btnRetour;

    private double selectedLat;
    private double selectedLng;
    private boolean hasSelection;

    @FXML
    void initialize() {
        WebEngine engine = webView.getEngine();
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");
        URL url = getClass().getResource("/Formation/map.html");
        if (url != null) {
            engine.load(url.toExternalForm());
        }

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) engine.executeScript("window");
                window.setMember("javaConnector", new JavaConnector());

                Platform.runLater(() -> {
                    runFixTiles(engine, 150);
                    runFixTiles(engine, 650);
                    runFixTiles(engine, 1500);
                });
            }
        });

        btnValider.setOnAction(e -> valider());
        btnRetour.setOnAction(e -> retour());

        if (UiState.selectedLieu != null && !UiState.selectedLieu.isBlank()) {
            lblSelected.setText(UiState.selectedLieu);
        }
    }

    private void runFixTiles(WebEngine engine, int delayMs) {
        PauseTransition pt = new PauseTransition(javafx.util.Duration.millis(delayMs));
        pt.setOnFinished(e -> {
            try {
                engine.executeScript("window.fixTiles && window.fixTiles()");
            } catch (Exception ignored) {
            }
        });
        pt.play();
    }

    public class JavaConnector {
        public void setLocation(double lat, double lng) {
            selectedLat = lat;
            selectedLng = lng;
            hasSelection = true;
            lblSelected.setText(String.format("%.6f, %.6f", lat, lng));
        }

        public void log(String msg) {
            System.out.println("[Map] " + msg);
        }
    }

    private void valider() {
        if (!hasSelection) {
            return;
        }

        String address = NominatimGeocoder.reverse(selectedLat, selectedLng);
        if (address != null && !address.isBlank()) {
            UiState.selectedLieu = address;
        } else {
            UiState.selectedLieu = String.format("%.6f, %.6f", selectedLat, selectedLng);
        }
        retour();
    }

    private void retour() {
        try {
            WindowUtil.navigate(btnRetour, "/Formation/AjouterFormation.fxml", "Ajouter une formation");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
