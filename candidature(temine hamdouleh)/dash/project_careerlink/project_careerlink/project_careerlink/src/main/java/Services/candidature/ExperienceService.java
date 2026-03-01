package Services.candidature;

import Entities.candidature.Experience;
import Iservices.candidature.IExperienceServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.*;

import java.util.HashMap;

public class ExperienceService implements IExperienceServices {

    private final Connection con;

    public ExperienceService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterExperience(Experience e) {
        String sql = "INSERT INTO experience (id_candidature, poste, entreprise, date_debut, date_fin) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, e.getId_candidature());
            ps.setString(2, e.getPoste());
            ps.setString(3, e.getEntreprise());

            if (e.getDate_debut() != null) ps.setDate(4, e.getDate_debut());
            else ps.setNull(4, Types.DATE);

            if (e.getDate_fin() != null) ps.setDate(5, e.getDate_fin());
            else ps.setNull(5, Types.DATE);

            ps.executeUpdate();
            System.out.println("✅ Experience ajoutée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void supprimerExperience(int id_experience) {
        String sql = "DELETE FROM experience WHERE id_experience = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_experience);
            ps.executeUpdate();
            System.out.println("✅ Experience supprimée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void supprimerExperiencesParCandidature(int id_candidature) {
        String sql = "DELETE FROM experience WHERE id_candidature = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_candidature);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void modifierExperience(Experience e) {
        String sql = "UPDATE experience SET id_candidature=?, poste=?, entreprise=?, date_debut=?, date_fin=? " +
                "WHERE id_experience=?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, e.getId_candidature());
            ps.setString(2, e.getPoste());
            ps.setString(3, e.getEntreprise());

            if (e.getDate_debut() != null) ps.setDate(4, e.getDate_debut());
            else ps.setNull(4, Types.DATE);

            if (e.getDate_fin() != null) ps.setDate(5, e.getDate_fin());
            else ps.setNull(5, Types.DATE);

            ps.setInt(6, e.getId_experience());

            ps.executeUpdate();
            System.out.println("✅ Experience modifiée");
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public List<Experience> afficherExperiences() {
        String sql = "SELECT * FROM experience";
        List<Experience> list = new ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Experience e = new Experience(
                        rs.getInt("id_experience"),
                        rs.getInt("id_candidature"),
                        rs.getString("poste"),
                        rs.getString("entreprise"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin")
                );
                list.add(e);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    @Override
    public List<Experience> afficherExperiencesParCandidature(int id_candidature) {
        String sql = "SELECT * FROM experience WHERE id_candidature = ?";
        List<Experience> list = new ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id_candidature);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Experience e = new Experience(
                            rs.getInt("id_experience"),
                            rs.getInt("id_candidature"),
                            rs.getString("poste"),
                            rs.getString("entreprise"),
                            rs.getDate("date_debut"),
                            rs.getDate("date_fin")
                    );
                    list.add(e);
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    // =========================================================
    // ✅ NEW: Synchroniser expériences (UPDATE normal)
    // =========================================================
    public void syncExperiences(int idCand, List<Experience> uiList) {

        // 1) ce qui existe en DB
        List<Experience> dbList = afficherExperiencesParCandidature(idCand);

        Map<Integer, Experience> dbById = new HashMap<>();
        for (Experience e : dbList) dbById.put(e.getId_experience(), e);

        Set<Integer> seen = new HashSet<>();

        // 2) traiter UI list
        for (Experience ui : uiList) {
            ui.setId_candidature(idCand);

            int id = ui.getId_experience(); // 0 si nouveau
            if (id <= 0) {
                // nouveau bloc ajouté
                ajouterExperience(ui);
            } else {
                // existe -> update
                modifierExperience(ui);
                seen.add(id);
            }
        }

        // 3) supprimer ceux qui étaient en DB mais plus dans UI
        for (Experience db : dbList) {
            if (!seen.contains(db.getId_experience())) {
                supprimerExperience(db.getId_experience());
            }
        }
    }
}
