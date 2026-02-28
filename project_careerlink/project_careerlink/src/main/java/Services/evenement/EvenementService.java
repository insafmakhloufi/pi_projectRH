package Services.evenement;

import Entities.evenement.Evenement;
import Iservices.evenement.IevenementServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IevenementServices {

    Connection con;

    public EvenementService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterEvenement(Evenement e) {

        String req = "INSERT INTO evenement (titre, description, date_debut, date_fin, lieu, type, capacite_max, statut, image_url) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, e.getTitre());
            ste.setString(2, e.getDescription());

            // LocalDateTime -> Timestamp
            if (e.getDateDebut() != null) ste.setTimestamp(3, Timestamp.valueOf(e.getDateDebut()));
            else ste.setNull(3, Types.TIMESTAMP);

            if (e.getDateFin() != null) ste.setTimestamp(4, Timestamp.valueOf(e.getDateFin()));
            else ste.setNull(4, Types.TIMESTAMP);

            ste.setString(5, e.getLieu());
            ste.setString(6, e.getType());
            ste.setInt(7, e.getCapaciteMax());
            ste.setString(8, e.getStatut());
            ste.setString(9, e.getImageUrl());

            ste.executeUpdate();
            System.out.println("✅ evenement ajouté");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void modifierEvenement(Evenement e) {

        String req = "UPDATE evenement SET titre=?, description=?, date_debut=?, date_fin=?, lieu=?, type=?, capacite_max=?, statut=?, image_url=? " +
                "WHERE id_evenement=?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, e.getTitre());
            ste.setString(2, e.getDescription());

            if (e.getDateDebut() != null) ste.setTimestamp(3, Timestamp.valueOf(e.getDateDebut()));
            else ste.setNull(3, Types.TIMESTAMP);

            if (e.getDateFin() != null) ste.setTimestamp(4, Timestamp.valueOf(e.getDateFin()));
            else ste.setNull(4, Types.TIMESTAMP);

            ste.setString(5, e.getLieu());
            ste.setString(6, e.getType());
            ste.setInt(7, e.getCapaciteMax());
            ste.setString(8, e.getStatut());
            ste.setString(9, e.getImageUrl());

            ste.setInt(10, e.getIdEvenement());

            ste.executeUpdate();
            System.out.println("✅ evenement modifié");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void supprimerEvenement(int id) {

        String req = "DELETE FROM evenement WHERE id_evenement=?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ste.executeUpdate();
            System.out.println("✅ evenement supprimé (id=" + id + ")");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Evenement> afficherEvenements() {

        List<Evenement> evenements = new ArrayList<>();
        String req = "SELECT * FROM evenement";

        try {
            Statement ste = con.createStatement();
            ResultSet rs = ste.executeQuery(req);

            while (rs.next()) {

                Timestamp tsDebut = rs.getTimestamp("date_debut");
                Timestamp tsFin   = rs.getTimestamp("date_fin");

                Evenement e = new Evenement(
                        rs.getInt("id_evenement"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        tsDebut != null ? tsDebut.toLocalDateTime() : null,
                        tsFin != null ? tsFin.toLocalDateTime() : null,
                        rs.getString("lieu"),
                        rs.getString("type"),
                        rs.getInt("capacite_max"),
                        rs.getString("statut"),
                        rs.getString("image_url")
                );

                evenements.add(e);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return evenements;
    }
}
