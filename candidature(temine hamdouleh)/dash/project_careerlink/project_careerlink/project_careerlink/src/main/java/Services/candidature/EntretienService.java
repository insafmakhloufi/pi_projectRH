package Services.candidature;

import Entities.candidature.Entretien;
import Iservices.candidature.IEntretienServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntretienService implements IEntretienServices {

    Connection con;

    public EntretienService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterEntretien(Entretien e) {

        String req = "INSERT INTO entretien (id_candidature, date, heure, mode, lieu, platform, statut, notes) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try {
            PreparedStatement ps = con.prepareStatement(req);

            ps.setInt(1, e.getId_candidature());

            if (e.getDate() != null) ps.setDate(2, java.sql.Date.valueOf(e.getDate()));
            else ps.setNull(2, Types.DATE);

            if (e.getHeure() != null) ps.setInt(3, e.getHeure());
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, e.getMode());
            if (e.getLieu() != null && !e.getLieu().trim().isEmpty()) ps.setString(5, e.getLieu());
            else ps.setNull(5, Types.VARCHAR);
            if (e.getPlatform() != null && !e.getPlatform().trim().isEmpty()) ps.setString(6, e.getPlatform());
            else ps.setNull(6, Types.VARCHAR);
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getNotes());

            ps.executeUpdate();
            System.out.println("Entretien ajouté avec succès");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void supprimerEntretien(int id) {

        String req = "DELETE FROM entretien WHERE id_entretien = ?";

        try {
            PreparedStatement ps = con.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Entretien supprimé avec succès");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void modifierEntretien(Entretien e) {

        String req = "UPDATE entretien SET id_candidature=?, date=?, heure=?, mode=?, lieu=?, platform=?, statut=?, notes=? " +
                "WHERE id_entretien=?";

        try {
            PreparedStatement ps = con.prepareStatement(req);

            ps.setInt(1, e.getId_candidature());

            if (e.getDate() != null) ps.setDate(2, java.sql.Date.valueOf(e.getDate()));
            else ps.setNull(2, Types.DATE);

            if (e.getHeure() != null) ps.setInt(3, e.getHeure());
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, e.getMode());
            if (e.getLieu() != null && !e.getLieu().trim().isEmpty()) ps.setString(5, e.getLieu());
            else ps.setNull(5, Types.VARCHAR);
            if (e.getPlatform() != null && !e.getPlatform().trim().isEmpty()) ps.setString(6, e.getPlatform());
            else ps.setNull(6, Types.VARCHAR);
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getNotes());

            ps.setInt(9, e.getId_entretien());

            ps.executeUpdate();
            System.out.println("Entretien modifié avec succès");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Entretien> afficherEntretiens() {

        List<Entretien> list = new ArrayList<>();
        String req = "SELECT * FROM entretien ORDER BY date DESC, heure DESC, id_entretien DESC";

        try {
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery(req);

            while (rs.next()) {
                Entretien e = new Entretien(
                        rs.getInt("id_entretien"),
                        rs.getInt("id_candidature"),
                        rs.getDate("date") == null ? null : rs.getDate("date").toLocalDate(),
                        readHeure(rs),
                        rs.getString("mode"),
                        rs.getString("lieu"),
                        rs.getString("platform"),
                        rs.getString("statut"),
                        rs.getString("notes")
                );
                list.add(e);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return list;
    }

    public Entretien getLatestEntretienByCandidature(int idCandidature) {
        String sql = "SELECT * FROM entretien WHERE id_candidature = ? ORDER BY date DESC, heure DESC, id_entretien DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Entretien(
                        rs.getInt("id_entretien"),
                        rs.getInt("id_candidature"),
                        rs.getDate("date") == null ? null : rs.getDate("date").toLocalDate(),
                        readHeure(rs),
                        rs.getString("mode"),
                        rs.getString("lieu"),
                        rs.getString("platform"),
                        rs.getString("statut"),
                        rs.getString("notes")
                );
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    // ------------------ Helpers utiles ------------------

    public Entretien getEntretienById(int idEntretien) {

        String sql = "SELECT * FROM entretien WHERE id_entretien = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEntretien);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                return new Entretien(
                        rs.getInt("id_entretien"),
                        rs.getInt("id_candidature"),
                        rs.getDate("date") == null ? null : rs.getDate("date").toLocalDate(),
                        readHeure(rs),
                        rs.getString("mode"),
                        rs.getString("lieu"),
                        rs.getString("platform"),
                        rs.getString("statut"),
                        rs.getString("notes")
                );
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Entretien> getEntretiensByCandidature(int idCandidature) {

        String sql = "SELECT * FROM entretien WHERE id_candidature = ? ORDER BY date DESC, heure DESC, id_entretien DESC";
        List<Entretien> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Entretien e = new Entretien(
                            rs.getInt("id_entretien"),
                            rs.getInt("id_candidature"),
                            rs.getDate("date") == null ? null : rs.getDate("date").toLocalDate(),
                            readHeure(rs),
                            rs.getString("mode"),
                            rs.getString("lieu"),
                            rs.getString("platform"),
                            rs.getString("statut"),
                            rs.getString("notes")
                    );
                    list.add(e);
                }
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        return list;
    }

    public int ajouterEntretienAndReturnId(Entretien e) {

        String sql = "INSERT INTO entretien (id_candidature, date, heure, mode, lieu, platform, statut, notes) " +
                "VALUES (?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, e.getId_candidature());

            if (e.getDate() != null) ps.setDate(2, java.sql.Date.valueOf(e.getDate()));
            else ps.setNull(2, Types.DATE);

            if (e.getHeure() != null) ps.setInt(3, e.getHeure());
            else ps.setNull(3, Types.INTEGER);

            ps.setString(4, e.getMode());
            if (e.getLieu() != null && !e.getLieu().trim().isEmpty()) ps.setString(5, e.getLieu());
            else ps.setNull(5, Types.VARCHAR);
            if (e.getPlatform() != null && !e.getPlatform().trim().isEmpty()) ps.setString(6, e.getPlatform());
            else ps.setNull(6, Types.VARCHAR);
            ps.setString(7, e.getStatut());
            ps.setString(8, e.getNotes());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("Entretien ajouté (ID=" + id + ")");
                    return id;
                }
            }

            throw new RuntimeException("Impossible de récupérer l'ID de l'entretien");

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Integer readHeure(ResultSet rs) throws SQLException {
        int v = rs.getInt("heure");
        if (rs.wasNull()) return null;
        return v;
    }
}
