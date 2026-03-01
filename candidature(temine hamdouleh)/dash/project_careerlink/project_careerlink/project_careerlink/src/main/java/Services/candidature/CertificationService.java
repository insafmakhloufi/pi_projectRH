package Services.candidature;

import Entities.candidature.Certification;
import Iservices.candidature.ICertificationServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.*;

import java.util.HashSet;

public class CertificationService implements ICertificationServices {

    private final Connection con;

    public CertificationService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterCertification(Certification c) {
        String sql = "INSERT INTO certification (id_candidature, organisme, nom_certification, numero, date_obtention, date_expiration) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getId_candidature());
            ps.setString(2, c.getOrganisme());
            ps.setString(3, c.getNom_certification());

            if (c.getNumero() != null) ps.setInt(4, c.getNumero());
            else ps.setNull(4, Types.INTEGER);

            if (c.getDate_obtention() != null) ps.setDate(5, c.getDate_obtention());
            else throw new RuntimeException("date_obtention est obligatoire (NOT NULL)");

            if (c.getDate_expiration() != null) ps.setDate(6, c.getDate_expiration());
            else ps.setNull(6, Types.DATE);

            ps.executeUpdate();
            System.out.println("✅ Certification ajoutée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void supprimerCertification(int id_certification) {
        String sql = "DELETE FROM certification WHERE id_certification = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_certification);
            ps.executeUpdate();
            System.out.println("✅ Certification supprimée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void supprimerCertificationsParCandidature(int id_candidature) {
        String sql = "DELETE FROM certification WHERE id_candidature = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_candidature);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void modifierCertification(Certification c) {
        String sql = "UPDATE certification SET id_candidature=?, organisme=?, nom_certification=?, numero=?, date_obtention=?, date_expiration=? " +
                "WHERE id_certification=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, c.getId_candidature());
            ps.setString(2, c.getOrganisme());
            ps.setString(3, c.getNom_certification());

            if (c.getNumero() != null) ps.setInt(4, c.getNumero());
            else ps.setNull(4, Types.INTEGER);

            if (c.getDate_obtention() != null) ps.setDate(5, c.getDate_obtention());
            else throw new RuntimeException("date_obtention est obligatoire (NOT NULL)");

            if (c.getDate_expiration() != null) ps.setDate(6, c.getDate_expiration());
            else ps.setNull(6, Types.DATE);

            ps.setInt(7, c.getId_certification());

            ps.executeUpdate();
            System.out.println("✅ Certification modifiée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Certification> afficherCertifications() {
        String sql = "SELECT * FROM certification";
        List<Certification> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Certification c = new Certification(
                        rs.getInt("id_certification"),
                        rs.getInt("id_candidature"),
                        rs.getString("organisme"),
                        rs.getString("nom_certification"),
                        (Integer) rs.getObject("numero"),
                        rs.getDate("date_obtention"),
                        rs.getDate("date_expiration")
                );
                list.add(c);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    @Override
    public List<Certification> afficherCertificationsParCandidature(int id_candidature) {
        String sql = "SELECT * FROM certification WHERE id_candidature = ?";
        List<Certification> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_candidature);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Certification c = new Certification(
                            rs.getInt("id_certification"),
                            rs.getInt("id_candidature"),
                            rs.getString("organisme"),
                            rs.getString("nom_certification"),
                            (Integer) rs.getObject("numero"),
                            rs.getDate("date_obtention"),
                            rs.getDate("date_expiration")
                    );
                    list.add(c);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    // =========================================================
    // ✅ NEW: Synchroniser certifications (UPDATE normal)
    // =========================================================
    public void syncCertifications(int idCand, List<Certification> uiList) {

        List<Certification> dbList = afficherCertificationsParCandidature(idCand);

        Set<Integer> seen = new HashSet<>();

        for (Certification ui : uiList) {
            ui.setId_candidature(idCand);

            int id = ui.getId_certification(); // 0 si nouveau
            if (id <= 0) {
                ajouterCertification(ui);
            } else {
                modifierCertification(ui);
                seen.add(id);
            }
        }

        // supprimer ce qui n'existe plus dans UI
        for (Certification db : dbList) {
            if (!seen.contains(db.getId_certification())) {
                supprimerCertification(db.getId_certification());
            }
        }
    }
}
