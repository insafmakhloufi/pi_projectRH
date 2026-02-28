package Test;

import Utils.ThemeManager;
import Utils.StripeWebhookServer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;


public class MainFX extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        StripeWebhookServer.startIfNeededAsync();

        Parent root = FXMLLoader.load(
                Objects.requireNonNull(getClass().getResource("/User/fxml/login.fxml"))
        );

        Scene scene = new Scene(root);
        ThemeManager.applyTheme(scene);
        stage.setTitle("CareerLink");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}