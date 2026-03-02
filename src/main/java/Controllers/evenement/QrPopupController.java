package Controllers.evenement;
import Services.evenement.ticket.QrService;
import Services.evenement.ticket.TicketServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.awt.Desktop;
import java.nio.file.Path;
public class QrPopupController {
    @FXML private ImageView qrImageView;
    @FXML private Label ticketIdLabel;
    @FXML private Label urlLabel;

    private Path htmlPath;
    private Path qrPath;
    private final TicketServer ticketServer = new TicketServer();
    private final QrService qrService = new QrService();

    public void init(Path qrPath, Path htmlPath, String ticketId) {
        this.qrPath = qrPath;
        this.htmlPath = htmlPath;

        ticketIdLabel.setText(ticketId);

        // Start local HTTP server in background thread
        new Thread(() -> {
            try {
                String ticketUrl = ticketServer.start(htmlPath);

                // Regenerate QR with HTTP URL
                qrService.generateQrPng(ticketUrl, qrPath);

                // Small delay to ensure file is written
                Thread.sleep(300);

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    try {
                        // Force fresh image load
                        qrImageView.setImage(null);
                        Image freshImage = new Image(qrPath.toUri().toString(), false);
                        qrImageView.setImage(freshImage);

                        if (urlLabel != null) {
                            urlLabel.setText(ticketUrl);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    if (urlLabel != null) urlLabel.setText("Erreur serveur");
                });
            }
        }, "ticket-server").start();
    }

    public void initStage(Stage stage) {
        stage.setOnCloseRequest(e -> ticketServer.stop());
    }

    @FXML
    private void openInBrowser() {
        try {
            if (htmlPath != null && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(htmlPath.toUri());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void close() {
        ticketServer.stop();
        ((Stage) qrImageView.getScene().getWindow()).close();
    }
}