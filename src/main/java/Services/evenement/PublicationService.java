package Services.evenement;

import Entities.evenement.Publication;
import Iservices.evenement.IpublicationServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PublicationService implements IpublicationServices {

    Connection con;

    public PublicationService() {
        con = Mydatabase.getInstance().getConnection();
        ;
    }

    @Override
    public void ajouterPublication(Publication p) {

        String req = "INSERT INTO publication (titre, contenu, date_publication, auteur_id, type, statut, visibilite, " +
                "image_url, video_url, piece_jointe_url, evenement_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, p.getTitre());
            ste.setString(2, p.getContenu());

            // LocalDateTime -> Timestamp
            if (p.getDatePublication() != null) ste.setTimestamp(3, Timestamp.valueOf(p.getDatePublication()));
            else ste.setNull(3, Types.TIMESTAMP);

            ste.setInt(4, p.getAuteurId());
            ste.setString(5, p.getType());
            ste.setString(6, p.getStatut());
            ste.setString(7, p.getVisibilite());
            ste.setString(8, p.getImageUrl());
            ste.setString(9, p.getVideoUrl());
            ste.setString(10, p.getPieceJointeUrl());

            // evenement_id nullable
            if (p.getEvenementId() != null) ste.setInt(11, p.getEvenementId());
            else ste.setNull(11, Types.INTEGER);

            ste.executeUpdate();
            System.out.println("✅ publication ajoutée");

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Erreur SQL ajouterPublication (SQLState=" + ex.getSQLState() + ", Code=" + ex.getErrorCode() + "): " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public void modifierPublication(Publication p) {

        String req = "UPDATE publication SET titre=?, contenu=?, date_publication=?, auteur_id=?, type=?, statut=?, visibilite=?, " +
                "image_url=?, video_url=?, piece_jointe_url=?, evenement_id=? " +
                "WHERE id_publication=?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, p.getTitre());
            ste.setString(2, p.getContenu());

            if (p.getDatePublication() != null) ste.setTimestamp(3, Timestamp.valueOf(p.getDatePublication()));
            else ste.setNull(3, Types.TIMESTAMP);

            ste.setInt(4, p.getAuteurId());
            ste.setString(5, p.getType());
            ste.setString(6, p.getStatut());
            ste.setString(7, p.getVisibilite());
            ste.setString(8, p.getImageUrl());
            ste.setString(9, p.getVideoUrl());
            ste.setString(10, p.getPieceJointeUrl());

            if (p.getEvenementId() != null) ste.setInt(11, p.getEvenementId());
            else ste.setNull(11, Types.INTEGER);

            ste.setInt(12, p.getIdPublication());

            ste.executeUpdate();
            System.out.println("✅ publication modifiée");

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Erreur SQL modifierPublication (SQLState=" + ex.getSQLState() + ", Code=" + ex.getErrorCode() + "): " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public void supprimerPublication(int id) {

        String req = "DELETE FROM publication WHERE id_publication=?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ste.executeUpdate();
            System.out.println("✅ publication supprimée (id=" + id + ")");

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Erreur SQL supprimerPublication (SQLState=" + ex.getSQLState() + ", Code=" + ex.getErrorCode() + "): " + ex.getMessage(),
                    ex
            );
        }
    }

    @Override
    public List<Publication> afficherPublications() {

        List<Publication> publications = new ArrayList<>();
        String req = "SELECT * FROM publication";

        try {
            Statement ste = con.createStatement();
            ResultSet rs = ste.executeQuery(req);

            while (rs.next()) {

                Timestamp ts = rs.getTimestamp("date_publication");

                // getObject pour garder NULL (sinon rs.getInt retourne 0 même si NULL)
                Integer evenementId = rs.getObject("evenement_id") != null
                        ? rs.getInt("evenement_id")
                        : null;

                Publication p = new Publication(
                        rs.getInt("id_publication"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getInt("auteur_id"),
                        rs.getString("type"),
                        rs.getString("statut"),
                        rs.getString("visibilite"),
                        rs.getString("image_url"),
                        rs.getString("video_url"),
                        rs.getString("piece_jointe_url"),
                        evenementId
                );

                publications.add(p);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return publications;
    }

    @Override
    public List<Publication> afficherPublicationsParEvenement(int eventId) {

        List<Publication> publications = new ArrayList<>();
        String req = "SELECT * FROM publication WHERE evenement_id = ?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, eventId);
            ResultSet rs = ste.executeQuery();

            while (rs.next()) {

                Timestamp ts = rs.getTimestamp("date_publication");

                Integer evenementId = rs.getObject("evenement_id") != null
                        ? rs.getInt("evenement_id")
                        : null;

                Publication p = new Publication(
                        rs.getInt("id_publication"),
                        rs.getString("titre"),
                        rs.getString("contenu"),
                        ts != null ? ts.toLocalDateTime() : null,
                        rs.getInt("auteur_id"),
                        rs.getString("type"),
                        rs.getString("statut"),
                        rs.getString("visibilite"),
                        rs.getString("image_url"),
                        rs.getString("video_url"),
                        rs.getString("piece_jointe_url"),
                        evenementId
                );

                publications.add(p);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return publications;
    }
}
