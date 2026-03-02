package Controllers.evenement;

import Entities.evenement.Publication;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public class PublicationDetailsController {

    @FXML private Label lblTitle;
    @FXML private Label lblMeta;
    @FXML private Label lblSubMeta;
    @FXML private Label lblContent;
    @FXML private ImageView ivImage;
    @FXML private Hyperlink lnkVideo;
    @FXML private Hyperlink lnkAttachment;

    private int eventId = -1;
    private Publication publication;

    @FXML
    public void initialize() {
        if (lnkVideo != null) {
            lnkVideo.setOnAction(e -> openUrlSafe(publication != null ? publication.getVideoUrl() : null));
        }
        if (lnkAttachment != null) {
            lnkAttachment.setOnAction(e -> openFileSafe(publication != null ? publication.getPieceJointeUrl() : null));
        }

        applyPublication();
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
        applyPublication();
    }

    @FXML
    private void back() {
        NavigationService.go("/views_event/event_publications_backend.fxml", ctrl -> {
            if (ctrl instanceof EventPublicationsBackendController) {
                ((EventPublicationsBackendController) ctrl).setEventId(eventId);
            }
        });
    }

    private void applyPublication() {
        if (publication == null) return;

        if (lblTitle != null) {
            lblTitle.setText(nvl(publication.getTitre()));
        }

        if (lblMeta != null) {
            String meta = nvl(publication.getType());
            if (!nvl(publication.getStatut()).isEmpty()) meta += " • " + nvl(publication.getStatut());
            if (!nvl(publication.getVisibilite()).isEmpty()) meta += " • " + nvl(publication.getVisibilite());
            lblMeta.setText(meta);
        }

        if (lblSubMeta != null) {
            String sub = (publication.getDatePublication() != null ? publication.getDatePublication().toString() : "");
            String auth = "Auteur #" + publication.getAuteurId();
            if (!sub.isEmpty()) sub = sub + " • " + auth;
            else sub = auth;
            lblSubMeta.setText(sub);
        }

        if (lblContent != null) {
            lblContent.setText(nvl(publication.getContenu()));
        }

        if (ivImage != null) {
            String raw = publication.getImageUrl();
            if (raw == null || raw.trim().isEmpty()) {
                ivImage.setImage(null);
                ivImage.setManaged(false);
                ivImage.setVisible(false);
            } else {
                try {
                    ivImage.setImage(new Image(raw, true));
                    ivImage.setManaged(true);
                    ivImage.setVisible(true);
                } catch (Exception ignored) {
                    ivImage.setImage(null);
                    ivImage.setManaged(false);
                    ivImage.setVisible(false);
                }
            }
        }

        if (lnkVideo != null) {
            boolean show = publication.getVideoUrl() != null && !publication.getVideoUrl().trim().isEmpty();
            lnkVideo.setVisible(show);
            lnkVideo.setManaged(show);
        }

        if (lnkAttachment != null) {
            boolean show = publication.getPieceJointeUrl() != null && !publication.getPieceJointeUrl().trim().isEmpty();
            lnkAttachment.setVisible(show);
            lnkAttachment.setManaged(show);
        }
    }

    private void openUrlSafe(String url) {
        try {
            if (url == null || url.trim().isEmpty()) return;
            if (!Desktop.isDesktopSupported()) return;
            Desktop.getDesktop().browse(URI.create(url.trim()));
        } catch (Exception ignored) {
        }
    }

    private void openFileSafe(String path) {
        try {
            if (path == null || path.trim().isEmpty()) return;
            if (!Desktop.isDesktopSupported()) return;

            String p = path.trim();
            if (p.startsWith("file:")) {
                Desktop.getDesktop().browse(URI.create(p));
                return;
            }

            File f = new File(p);
            if (f.exists()) {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception ignored) {
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
