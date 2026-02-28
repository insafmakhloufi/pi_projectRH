package Controllers.evenement;

public final class AiDbContextProvider {

    private AiDbContextProvider() {
    }

    public static String schemaContext() {
        return "DATABASE SCHEMA (MySQL)\n"
                + "Table: evenement\n"
                + "- id_evenement (INT, PK)\n"
                + "- titre (VARCHAR)\n"
                + "- description (TEXT)\n"
                + "- date_debut (TIMESTAMP, nullable)\n"
                + "- date_fin (TIMESTAMP, nullable)\n"
                + "- lieu (VARCHAR)\n"
                + "- type (VARCHAR)\n"
                + "- capacite_max (INT)\n"
                + "- statut (VARCHAR)\n"
                + "- image_url (VARCHAR, nullable)\n\n"
                + "Table: publication\n"
                + "- id_publication (INT, PK)\n"
                + "- titre (VARCHAR)\n"
                + "- contenu (TEXT)\n"
                + "- date_publication (TIMESTAMP, nullable)\n"
                + "- auteur_id (INT)\n"
                + "- type (VARCHAR)\n"
                + "- statut (VARCHAR)\n"
                + "- visibilite (VARCHAR)\n"
                + "- image_url (VARCHAR, nullable)\n"
                + "- video_url (VARCHAR, nullable)\n"
                + "- piece_jointe_url (VARCHAR, nullable)\n"
                + "- evenement_id (INT, nullable, FK -> evenement.id_evenement)\n\n"
                + "RELATION\n"
                + "- publication.evenement_id references evenement.id_evenement\n";
    }
}
