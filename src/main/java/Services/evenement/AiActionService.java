package Services.evenement;

import Controllers.evenement.AiRequestParser;
import Entities.evenement.Evenement;
import Entities.evenement.Publication;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

public final class AiActionService {

    private final EvenementService evenementService;
    private final PublicationService publicationService;

    public AiActionService() {
        this.evenementService = new EvenementService();
        this.publicationService = new PublicationService();
    }

    public String executeAction(AiRequestParser.AiRequest req) {
        if (req == null) return "No request.";
        if (req.kind != AiRequestParser.Kind.ACTION_REQUEST) return "Not an ACTION_REQUEST.";

        String action = safeLower(req.get("ACTION"));
        String paramsRaw = req.get("PARAMS");
        Map<String, String> params = AiRequestParser.parseKeyValueParams(paramsRaw);

        switch (action) {
            case "delete_event":
                return deleteEvent(params);
            case "delete_publication":
                return deletePublication(params);
            case "create_event":
                return createEvent(params);
            case "update_event":
                return updateEvent(params);
            case "create_publication":
                return createPublication(params);
            case "update_publication":
                return updatePublication(params);
            case "bulk_delete_by_event":
                return bulkDeletePublicationsByEvent(params);
            case "bulk_publish":
                return bulkPublishPublicationsByEvent(params);
            default:
                return "Unsupported action: " + action;
        }
    }

    private String deleteEvent(Map<String, String> params) {
        Integer id = parseInt(params.get("id"));
        if (id == null) return "Missing param: id";
        evenementService.supprimerEvenement(id);
        return "deleted evenement id=" + id;
    }

    private String deletePublication(Map<String, String> params) {
        Integer id = parseInt(params.get("id"));
        if (id == null) return "Missing param: id";
        publicationService.supprimerPublication(id);
        return "deleted publication id=" + id;
    }

    private String createEvent(Map<String, String> params) {
        String titre = params.getOrDefault("titre", "");
        if (titre.isBlank()) return "Missing param: titre";

        String description = params.getOrDefault("description", "");
        String lieu = params.getOrDefault("lieu", "");
        String type = params.getOrDefault("type", "");
        Integer capacite = parseInt(params.get("capacite_max"));
        if (capacite == null) capacite = 0;
        String statut = params.getOrDefault("statut", "BROUILLON");
        String imageUrl = params.getOrDefault("image_url", null);

        LocalDateTime dateDebut = parseDate(params.get("date_debut"));
        LocalDateTime dateFin = parseDate(params.get("date_fin"));

        Evenement e = new Evenement(titre, description, dateDebut, dateFin, lieu, type, capacite, statut, imageUrl);
        evenementService.ajouterEvenement(e);
        return "created evenement titre=" + titre;
    }

    private String updateEvent(Map<String, String> params) {
        Integer id = parseInt(params.get("id"));
        if (id == null) return "Missing param: id";

        // Minimal update strategy: requires the current record to be re-provided by UI or AI.
        // For safety, only allow updating statut/type/lieu/titre/capacite_max/date_debut/date_fin/description/image_url.
        // We'll build a full Evenement object using required fields from params.

        String titre = params.getOrDefault("titre", "");
        String description = params.getOrDefault("description", "");
        String lieu = params.getOrDefault("lieu", "");
        String type = params.getOrDefault("type", "");
        Integer capacite = parseInt(params.get("capacite_max"));
        if (capacite == null) capacite = 0;
        String statut = params.getOrDefault("statut", "");
        String imageUrl = params.getOrDefault("image_url", null);

        LocalDateTime dateDebut = parseDate(params.get("date_debut"));
        LocalDateTime dateFin = parseDate(params.get("date_fin"));

        Evenement e = new Evenement(id, titre, description, dateDebut, dateFin, lieu, type, capacite, statut, imageUrl);
        evenementService.modifierEvenement(e);
        return "updated evenement id=" + id;
    }

    private String createPublication(Map<String, String> params) {
        String titre = params.getOrDefault("titre", "");
        String contenu = params.getOrDefault("contenu", "");
        Integer auteurId = parseInt(params.get("auteur_id"));
        if (titre.isBlank()) return "Missing param: titre";
        if (contenu.isBlank()) return "Missing param: contenu";
        if (auteurId == null) return "Missing param: auteur_id";

        String type = params.getOrDefault("type", "");
        String statut = params.getOrDefault("statut", "BROUILLON");
        String visibilite = params.getOrDefault("visibilite", "PUBLIC");
        String imageUrl = params.getOrDefault("image_url", null);
        String videoUrl = params.getOrDefault("video_url", null);
        String pieceJointeUrl = params.getOrDefault("piece_jointe_url", null);
        Integer evenementId = parseInt(params.get("evenement_id"));

        LocalDateTime date = parseDate(params.get("date_publication"));
        Publication p = new Publication(titre, contenu, date, auteurId, type, statut, visibilite, imageUrl, videoUrl, pieceJointeUrl, evenementId);
        publicationService.ajouterPublication(p);
        return "created publication titre=" + titre;
    }

    private String updatePublication(Map<String, String> params) {
        Integer id = parseInt(params.get("id"));
        if (id == null) return "Missing param: id";

        String titre = params.getOrDefault("titre", "");
        String contenu = params.getOrDefault("contenu", "");
        Integer auteurId = parseInt(params.get("auteur_id"));
        if (auteurId == null) auteurId = 0;

        String type = params.getOrDefault("type", "");
        String statut = params.getOrDefault("statut", "");
        String visibilite = params.getOrDefault("visibilite", "");
        String imageUrl = params.getOrDefault("image_url", null);
        String videoUrl = params.getOrDefault("video_url", null);
        String pieceJointeUrl = params.getOrDefault("piece_jointe_url", null);
        Integer evenementId = parseInt(params.get("evenement_id"));

        LocalDateTime date = parseDate(params.get("date_publication"));

        Publication p = new Publication(id, titre, contenu, date, auteurId, type, statut, visibilite, imageUrl, videoUrl, pieceJointeUrl, evenementId);
        publicationService.modifierPublication(p);
        return "updated publication id=" + id;
    }

    private String bulkDeletePublicationsByEvent(Map<String, String> params) {
        Integer eventId = parseInt(params.get("evenement_id"));
        if (eventId == null) return "Missing param: evenement_id";

        int deleted = 0;
        for (Publication p : publicationService.afficherPublicationsParEvenement(eventId)) {
            publicationService.supprimerPublication(p.getIdPublication());
            deleted++;
        }
        return "bulk deleted publications for evenement_id=" + eventId + " count=" + deleted;
    }

    private String bulkPublishPublicationsByEvent(Map<String, String> params) {
        Integer eventId = parseInt(params.get("evenement_id"));
        if (eventId == null) return "Missing param: evenement_id";

        int updated = 0;
        for (Publication p : publicationService.afficherPublicationsParEvenement(eventId)) {
            p.setStatut("PUBLIÉ");
            publicationService.modifierPublication(p);
            updated++;
        }
        return "bulk published publications for evenement_id=" + eventId + " count=" + updated;
    }

    private static Integer parseInt(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
