package Services.Entreprise;

import Entities.Entreprise.Entreprise;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EntrepriseService {

    private final Connection con;

    public EntrepriseService() {
        this.con = Mydatabase.getInstance().getConnection();
    }

    public Entreprise getByUserId(int idUser) {
        String sql = "SELECT * FROM entreprise WHERE id_user = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Entreprise e = new Entreprise();
                e.setIdEntreprise(rs.getInt("id_entreprise"));
                e.setNomEntreprise(rs.getString("nom_entreprise"));
                e.setLogo(rs.getString("logo"));
                e.setAdresse(rs.getString("adresse"));
                e.setVille(rs.getString("ville"));
                e.setTelephone(rs.getString("telephone"));
                e.setEmailContact(rs.getString("email_contact"));
                e.setDescription(rs.getString("description"));
                e.setIdUser(rs.getInt("id_user"));
                return e;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Entreprise getById(int idEntreprise) {
        String sql = "SELECT * FROM entreprise WHERE id_entreprise = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idEntreprise);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Entreprise e = new Entreprise();
                e.setIdEntreprise(rs.getInt("id_entreprise"));
                e.setNomEntreprise(rs.getString("nom_entreprise"));
                e.setLogo(rs.getString("logo"));
                e.setAdresse(rs.getString("adresse"));
                e.setVille(rs.getString("ville"));
                e.setTelephone(rs.getString("telephone"));
                e.setEmailContact(rs.getString("email_contact"));
                e.setDescription(rs.getString("description"));
                e.setIdUser(rs.getInt("id_user"));
                return e;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Entreprise> getAll() {
        String sql = "SELECT * FROM entreprise ORDER BY id_entreprise DESC";
        List<Entreprise> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Entreprise e = new Entreprise();
                e.setIdEntreprise(rs.getInt("id_entreprise"));
                e.setNomEntreprise(rs.getString("nom_entreprise"));
                e.setLogo(rs.getString("logo"));
                e.setAdresse(rs.getString("adresse"));
                e.setVille(rs.getString("ville"));
                e.setTelephone(rs.getString("telephone"));
                e.setEmailContact(rs.getString("email_contact"));
                e.setDescription(rs.getString("description"));
                e.setIdUser(rs.getInt("id_user"));
                list.add(e);
            }
            return list;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public int ajouterEntreprise(Entreprise e) {
        String sql = "INSERT INTO entreprise (nom_entreprise, logo, adresse, ville, telephone, email_contact, description, id_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, e.getNomEntreprise());
            ps.setString(2, e.getLogo());
            ps.setString(3, e.getAdresse());
            ps.setString(4, e.getVille());
            ps.setString(5, e.getTelephone());
            ps.setString(6, e.getEmailContact());
            ps.setString(7, e.getDescription());
            ps.setInt(8, e.getIdUser());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return 0;
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void modifierEntreprise(Entreprise e) {
        String sql = "UPDATE entreprise SET nom_entreprise=?, logo=?, adresse=?, ville=?, telephone=?, email_contact=?, description=? WHERE id_entreprise=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNomEntreprise());
            ps.setString(2, e.getLogo());
            ps.setString(3, e.getAdresse());
            ps.setString(4, e.getVille());
            ps.setString(5, e.getTelephone());
            ps.setString(6, e.getEmailContact());
            ps.setString(7, e.getDescription());
            ps.setInt(8, e.getIdEntreprise());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void saveForUser(Entreprise e) {
        Entreprise existing = getByUserId(e.getIdUser());
        if (existing == null) {
            ajouterEntreprise(e);
        } else {
            e.setIdEntreprise(existing.getIdEntreprise());
            modifierEntreprise(e);
        }
    }

    public boolean existsDuplicate(Entreprise e, Integer excludeIdEntreprise) {
        if (e == null) {
            return false;
        }
        String sql = "SELECT 1 FROM entreprise WHERE LOWER(nom_entreprise)=LOWER(?) "
                + "AND COALESCE(adresse,'')=COALESCE(?, '') "
                + "AND COALESCE(ville,'')=COALESCE(?, '') "
                + "AND COALESCE(telephone,'')=COALESCE(?, '') "
                + "AND COALESCE(email_contact,'')=COALESCE(?, '') "
                + "AND COALESCE(description,'')=COALESCE(?, '') "
                + (excludeIdEntreprise != null ? "AND id_entreprise<>? " : "")
                + "LIMIT 1";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNomEntreprise());
            ps.setString(2, e.getAdresse());
            ps.setString(3, e.getVille());
            ps.setString(4, e.getTelephone());
            ps.setString(5, e.getEmailContact());
            ps.setString(6, e.getDescription());
            if (excludeIdEntreprise != null) {
                ps.setInt(7, excludeIdEntreprise);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteCascade(int idEntreprise) {
        String delOffres = "DELETE FROM offreemlpoi WHERE id_entreprise = ?";
        String delEntreprise = "DELETE FROM entreprise WHERE id_entreprise = ?";
        try (PreparedStatement psOffres = con.prepareStatement(delOffres);
             PreparedStatement psEnt = con.prepareStatement(delEntreprise)) {
            psOffres.setInt(1, idEntreprise);
            psOffres.executeUpdate();

            psEnt.setInt(1, idEntreprise);
            psEnt.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void deleteByUserIdCascade(int idUser) {
        Entreprise e = getByUserId(idUser);
        if (e == null) {
            return;
        }
        deleteCascade(e.getIdEntreprise());
    }
}
