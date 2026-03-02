package Services.candidature;

import Entities.candidature.Contrat;
import Iservices.candidature.IContratService;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContratService implements IContratService {

    private final Connection con;

    public ContratService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterContrat(Contrat c) {
        String sql = """
            INSERT INTO contrat (candidature_id, candidat_id, type_contrat, type_emploi,
            secteur, localisation, nom_entreprise, date_debut, date_fin, salaire,
            horaire_travail, periode_essai, avantages, mode_travail, statut, qr_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getCandidature_id());

            if (c.getCandidat_id() != null) ps.setInt(2, c.getCandidat_id());
            else ps.setNull(2, Types.INTEGER);

            ps.setString(3, c.getType_contrat());
            ps.setString(4, c.getType_emploi());
            ps.setString(5, c.getSecteur());
            ps.setString(6, c.getLocalisation());
            ps.setString(7, c.getNom_entreprise());

            ps.setDate(8, c.getDate_debut());

            if (c.getDate_fin() != null) ps.setDate(9, c.getDate_fin());
            else ps.setNull(9, Types.DATE);

            if (c.getSalaire() != null) ps.setDouble(10, c.getSalaire());
            else ps.setNull(10, Types.DECIMAL);

            ps.setString(11, c.getHoraire_travail());
            ps.setString(12, c.getPeriode_essai());
            ps.setString(13, c.getAvantages());
            ps.setString(14, c.getMode_travail());
            ps.setString(15, c.getStatut() != null ? c.getStatut() : "GENERATED");
            ps.setString(16, c.getQr_hash());

            ps.executeUpdate();

            // Récupérer l'id généré
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                c.setId(rs.getInt(1));
            }

            System.out.println("Contrat ajouté avec id=" + c.getId());

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void modifierContrat(Contrat c) {
        String sql = """
            UPDATE contrat SET type_contrat=?, type_emploi=?, secteur=?, localisation=?,
            nom_entreprise=?, date_debut=?, date_fin=?, salaire=?, horaire_travail=?,
            periode_essai=?, avantages=?, mode_travail=?, statut=?, qr_hash=?
            WHERE id=?
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getType_contrat());
            ps.setString(2, c.getType_emploi());
            ps.setString(3, c.getSecteur());
            ps.setString(4, c.getLocalisation());
            ps.setString(5, c.getNom_entreprise());
            ps.setDate(6, c.getDate_debut());

            if (c.getDate_fin() != null) ps.setDate(7, c.getDate_fin());
            else ps.setNull(7, Types.DATE);

            if (c.getSalaire() != null) ps.setDouble(8, c.getSalaire());
            else ps.setNull(8, Types.DECIMAL);

            ps.setString(9, c.getHoraire_travail());
            ps.setString(10, c.getPeriode_essai());
            ps.setString(11, c.getAvantages());
            ps.setString(12, c.getMode_travail());
            ps.setString(13, c.getStatut());
            ps.setString(14, c.getQr_hash());
            ps.setInt(15, c.getId());

            ps.executeUpdate();
            System.out.println("Contrat modifié id=" + c.getId());

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void supprimerContrat(int id) {
        String sql = "DELETE FROM contrat WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Contrat getContratParCandidature(int candidature_id) {
        String sql = "SELECT * FROM contrat WHERE candidature_id=? ORDER BY created_at DESC LIMIT 1";
        try {
            Connection freshCon = Mydatabase.getInstance().getConnection();
            PreparedStatement ps = freshCon.prepareStatement(sql);
            ps.setInt(1, candidature_id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("DEBUG contrat trouvé ! id=" + rs.getInt("id"));
                return mapRow(rs); // ✅ rs est déjà positionné sur la ligne
            } else {
                System.out.println("DEBUG aucun contrat pour candidature_id=" + candidature_id);
            }

        } catch (SQLException ex) {
            System.out.println("ERREUR: " + ex.getMessage());
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public Contrat getContratById(int id) {
        String sql = "SELECT * FROM contrat WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return null;
    }

    @Override
    public List<Contrat> afficherContrats() {
        String sql = "SELECT * FROM contrat ORDER BY created_at DESC";
        List<Contrat> list = new ArrayList<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return list;
    }

    /*
    @Override
    public void signerContrat(int id) {
        String sql = "UPDATE contrat SET statut='SIGNED', signed_at=NOW() WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Contrat signé id=" + id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }
    */
   //contractttt jdidd

    @Override
    public void signerContrat(int id, String signatureBase64) {
        String sql = "UPDATE contrat SET statut='SIGNED', signed_at=NOW(), signature_image=? WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, signatureBase64);
            ps.setInt(2, id);
            ps.executeUpdate();
            System.out.println("Contrat signé avec signature id=" + id);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }





    // ---- Méthodes pour les codes de vérification ----

    public void sauvegarderCode(int contractId, String code, Timestamp expiresAt) {
        // Supprimer les anciens codes non utilisés
        String del = "DELETE FROM contrat_verification_code WHERE contract_id=? AND used=0";
        try (PreparedStatement ps = con.prepareStatement(del)) {
            ps.setInt(1, contractId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }

        String sql = "INSERT INTO contrat_verification_code (contract_id, code, expires_at, used) VALUES (?, ?, ?, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            ps.setString(2, code);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean validerCode(int contractId, String code) {
        String sql = """
            SELECT id FROM contrat_verification_code
            WHERE contract_id=? AND code=? AND used=0 AND expires_at > NOW()
            """;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, contractId);
            ps.setString(2, code);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Marquer comme utilisé
                int codeId = rs.getInt("id");
                String upd = "UPDATE contrat_verification_code SET used=1 WHERE id=?";
                try (PreparedStatement ps2 = con.prepareStatement(upd)) {
                    ps2.setInt(1, codeId);
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        return false;
    }

    // ---- Helper ----

    private Contrat mapRow(ResultSet rs) throws SQLException {
        Contrat c = new Contrat();
        c.setId(rs.getInt("id"));
        c.setCandidature_id(rs.getInt("candidature_id"));
        Object candidatIdObj = rs.getObject("candidat_id");
        if (candidatIdObj != null) {
            c.setCandidat_id(((Number) candidatIdObj).intValue());
        }        c.setType_contrat(rs.getString("type_contrat"));
        c.setType_emploi(rs.getString("type_emploi"));
        c.setSecteur(rs.getString("secteur"));
        c.setLocalisation(rs.getString("localisation"));
        c.setNom_entreprise(rs.getString("nom_entreprise"));
        c.setDate_debut(rs.getDate("date_debut"));
        c.setDate_fin(rs.getDate("date_fin"));
        Object salaireObj = rs.getObject("salaire");
        if (salaireObj != null) {
            if (salaireObj instanceof java.math.BigDecimal) {
                c.setSalaire(((java.math.BigDecimal) salaireObj).doubleValue());
            } else {
                c.setSalaire((Double) salaireObj);
            }
        }        c.setHoraire_travail(rs.getString("horaire_travail"));
        c.setPeriode_essai(rs.getString("periode_essai"));
        c.setAvantages(rs.getString("avantages"));
        c.setMode_travail(rs.getString("mode_travail"));
        c.setStatut(rs.getString("statut"));
        c.setSigned_at(rs.getTimestamp("signed_at"));
        c.setQr_hash(rs.getString("qr_hash"));
        c.setCreated_at(rs.getTimestamp("created_at"));
        return c;
    }
}