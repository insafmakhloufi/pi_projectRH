package Services.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Iservices.candidature.ICandidatureServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService implements ICandidatureServices {

    Connection con;

    public CandidatureService() {
        con = Mydatabase.getInstance().getConnection();
    }

    public List<Experience> getExperiencesByCandidatureId(int idCandidature) {
        String sql = "SELECT id_experience, id_candidature, poste, entreprise, date_debut, date_fin " +
                "FROM experience WHERE id_candidature = ? ORDER BY date_debut DESC";

        List<Experience> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Certification> getCertificationsByCandidatureId(int idCandidature) {
        String sql = "SELECT id_certification, id_candidature, organisme, nom_certification, numero, date_obtention, date_expiration " +
                "FROM certification WHERE id_candidature = ? ORDER BY date_obtention DESC";

        List<Certification> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    //jdidddddd
    public List<Candidature> afficherCandidaturesPourManagerRH(int managerRHId) {
        String sql = "SELECT c.*, " +
                "CASE WHEN EXISTS (SELECT 1 FROM entretien e WHERE e.id_candidature = c.IDCandidat) " +
                "THEN 'TRAITE' ELSE 'NON_TRAITE' END AS statut " +
                "FROM candidature c " +
                "JOIN offreemlpoi o ON c.offre_id = o.id " +
                "JOIN entreprise en ON o.id_entreprise = en.id_entreprise " +
                "WHERE en.id_user = ? " +
                "ORDER BY c.IDCandidat DESC";

        List<Candidature> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, managerRHId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidature c = new Candidature();
                    c.setIDCandidat(rs.getInt("IDCandidat"));
                    c.setPrenom(rs.getString("prenom"));
                    c.setNom(rs.getString("nom"));
                    c.setEmail(rs.getString("email"));
                    c.setIndicatif(rs.getString("indicatif"));
                    c.setTel(rs.getString("tel"));
                    c.setAdresse(rs.getString("adresse"));
                    c.setVille(rs.getString("ville"));
                    c.setDate_naissance(rs.getDate("date_naissance"));
                    c.setHighest_degree(rs.getString("highest_degree"));
                    c.setInstitution(rs.getString("institution"));
                    c.setLettre_motivation(rs.getString("lettre_motivation"));
                    c.setSkills_text(rs.getString("skills_text"));
                    c.setPieces_jointes(rs.getString("Pieces_jointes"));
                    c.setVideo_path(rs.getString("video_path"));
                    c.setConfirm_info(rs.getBoolean("confirm_info"));
                    c.setAccept_privacy(rs.getBoolean("accept_privacy"));
                    c.setOffreId((Integer) rs.getObject("offre_id"));
                    c.setStatut(rs.getString("statut"));
                    try { c.setDecisionRh(rs.getString("decision_rh")); }
                    catch (Exception ignored) {}
                    list.add(c);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }



    @Override
    public void ajouterCandidature(Candidature c) {

        String req = "INSERT INTO candidature " +
                "(user_id, prenom, nom, email, indicatif, tel, adresse, ville, date_naissance, highest_degree, institution, " +
                "lettre_motivation, skills_text, Pieces_jointes, video_path, confirm_info, accept_privacy, decision_rh, offre_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        try {
            PreparedStatement ste = con.prepareStatement(req);

            if (c.getUserId() != null) ste.setInt(1, c.getUserId());
            else ste.setNull(1, Types.INTEGER);

            ste.setString(2, c.getPrenom());
            ste.setString(3, c.getNom());
            ste.setString(4, c.getEmail());
            ste.setString(5, c.getIndicatif());
            ste.setString(6, c.getTel());
            ste.setString(7, c.getAdresse());
            ste.setString(8, c.getVille());
            ste.setDate(9, c.getDate_naissance()); // يمكن NULL
            ste.setString(10, c.getHighest_degree());
            ste.setString(11, c.getInstitution());
            ste.setString(12, c.getLettre_motivation());
            ste.setString(13, c.getSkills_text());
            ste.setString(14, c.getPieces_jointes() != null ? c.getPieces_jointes() : "");
            ste.setString(15, c.getVideo_path());
            ste.setBoolean(16, c.isConfirm_info());
            ste.setBoolean(17, c.isAccept_privacy());
            ste.setString(18, "EN_COURS");
            // APRÈS (correct)
            if (c.getOffreId() != null) ste.setInt(19, c.getOffreId());
            else ste.setNull(19, Types.INTEGER);

            ste.executeUpdate();
            System.out.println("Candidature ajoutée avec succès");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public void ajouterExperience(int idCandidature, String poste, String entreprise,
                                  java.sql.Date debut, java.sql.Date fin, boolean current) {

        String sql = "INSERT INTO experience (id_candidature, poste, entreprise, date_debut, date_fin) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);
            ps.setString(2, poste);
            ps.setString(3, entreprise);
            ps.setDate(4, debut);
            ps.setDate(5, fin); // null ok si "current"
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public void ajouterCertification(int idCandidature, String organisme, String certif,
                                     String numero, java.sql.Date obtention, java.sql.Date expiration) {

        String sql = "INSERT INTO certification (id_candidature, organisme, nom_certification, numero, date_obtention, date_expiration) " +
                "VALUES (?,?,?,?,?,?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidature);
            ps.setString(2, organisme);
            ps.setString(3, certif);
            ps.setString(4, numero);
            ps.setDate(5, obtention);
            ps.setDate(6, expiration);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }






    @Override
    public void supprimerCandidature(int IDCandidat) {
        String req = "DELETE FROM candidature WHERE IDCandidat = ?";

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, IDCandidat);
            ste.executeUpdate();
            System.out.println("Candidature supprimée avec succès");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifierCandidature(Candidature c) {

        String req = "UPDATE candidature SET " +
                "prenom=?, nom=?, email=?, indicatif=?, tel=?, adresse=?, ville=?, date_naissance=?, highest_degree=?, institution=?, " +
                "lettre_motivation=?, skills_text=?, Pieces_jointes=?, video_path=?, confirm_info=?, accept_privacy=? " +
                "WHERE IDCandidat=?";

        try {
            PreparedStatement ste = con.prepareStatement(req);

            ste.setString(1, c.getPrenom());
            ste.setString(2, c.getNom());
            ste.setString(3, c.getEmail());
            ste.setString(4, c.getIndicatif());
            
            // tel (int dans DB) => on convertit proprement
            if (c.getTel() != null && !c.getTel().isBlank()) {
                try {
                    ste.setLong(5, Long.parseLong(c.getTel().replaceAll("\\s+", "")));
                } catch (Exception ex) {
                    ste.setNull(5, java.sql.Types.BIGINT);
                }
            } else {
                ste.setNull(5, java.sql.Types.BIGINT);
            }
            
            ste.setString(6, c.getAdresse());
            ste.setString(7, c.getVille());
            ste.setDate(8, c.getDate_naissance());
            ste.setString(9, c.getHighest_degree());
            ste.setString(10, c.getInstitution());
            ste.setString(11, c.getLettre_motivation());
            ste.setString(12, c.getSkills_text());
            ste.setString(13, c.getPieces_jointes() != null ? c.getPieces_jointes() : "");
            ste.setString(14, c.getVideo_path());
            ste.setBoolean(15, c.isConfirm_info());
            ste.setBoolean(16, c.isAccept_privacy());
            ste.setInt(17, c.getIDCandidat());

            ste.executeUpdate();
            System.out.println("Candidature modifiée avec succès");

        } catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Candidature> afficherCandidature() {

        List<Candidature> candidatures = new ArrayList<>();
        String req = "SELECT * FROM candidature";

        try {
            Statement ste = con.createStatement();
            ResultSet rs = ste.executeQuery(req);

            while (rs.next()) {

                Candidature c = new Candidature(
                        rs.getInt("IDCandidat"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("indicatif"),
                        rs.getString("tel"),
                        rs.getString("adresse"),
                        rs.getString("ville"),
                        rs.getDate("date_naissance"),
                        rs.getString("highest_degree"),
                        rs.getString("institution"),
                        rs.getString("lettre_motivation"),
                        rs.getString("skills_text"),
                        rs.getString("Pieces_jointes"),
                        rs.getString("video_path"),
                        rs.getBoolean("confirm_info"),
                        rs.getBoolean("accept_privacy")
                );
                c.setOffreId((Integer) rs.getObject("offre_id"));

                candidatures.add(c);
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return candidatures;
    }


    public Candidature getCandidatureById(int idCandidat) {

        String sql = "SELECT c.*, " +
                "CASE WHEN EXISTS (SELECT 1 FROM entretien e WHERE e.id_candidature = c.IDCandidat) " +
                "THEN 'TRAITE' ELSE 'NON_TRAITE' END AS statut " +
                "FROM candidature c WHERE c.IDCandidat = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idCandidat);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Candidature c = new Candidature(
                        rs.getInt("IDCandidat"),
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email"),
                        rs.getString("indicatif"),
                        rs.getString("tel"),
                        rs.getString("adresse"),
                        rs.getString("ville"),
                        rs.getDate("date_naissance"),
                        rs.getString("highest_degree"),
                        rs.getString("institution"),
                        rs.getString("lettre_motivation"),
                        rs.getString("skills_text"),
                        rs.getString("Pieces_jointes"),
                        rs.getString("video_path"),
                        rs.getBoolean("confirm_info"),
                        rs.getBoolean("accept_privacy")
                );
                c.setOffreId((Integer) rs.getObject("offre_id"));

                c.setStatut(rs.getString("statut"));
                try {
                    c.setDecisionRh(rs.getString("decision_rh"));
                } catch (Exception ignored) {
                }
                return c;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public int ajouterCandidatureAndReturnId(Candidature c) {
        String sql = "INSERT INTO candidature " +
                "(user_id, prenom, nom, email, indicatif, tel, adresse, ville, date_naissance, highest_degree, institution, " +
                "lettre_motivation, skills_text, Pieces_jointes, video_path, confirm_info, accept_privacy, decision_rh, offre_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (c.getUserId() != null) ps.setInt(1, c.getUserId());
            else ps.setNull(1, Types.INTEGER);

            ps.setString(2, c.getPrenom());
            ps.setString(3, c.getNom());
            ps.setString(4, c.getEmail());
            ps.setString(5, c.getIndicatif());

            // tel (int dans DB) => on convertit proprement
            if (c.getTel() != null && !c.getTel().isBlank()) {
                try {
                    ps.setLong(6, Long.parseLong(c.getTel().replaceAll("\\s+", "")));
                } catch (Exception ex) {
                    ps.setNull(6, java.sql.Types.BIGINT);
                }
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }

            ps.setString(7, c.getAdresse());
            ps.setString(8, c.getVille());

            if (c.getDate_naissance() != null) ps.setDate(9, c.getDate_naissance());
            else ps.setNull(9, java.sql.Types.DATE);

            ps.setString(10, c.getHighest_degree());
            ps.setString(11, c.getInstitution());
            ps.setString(12, c.getLettre_motivation());
            ps.setString(13, c.getSkills_text());
            ps.setString(14, c.getPieces_jointes() != null ? c.getPieces_jointes() : "");
            ps.setString(15, c.getVideo_path());

            ps.setBoolean(16, c.isConfirm_info());
            ps.setBoolean(17, c.isAccept_privacy());
            ps.setString(18, "EN_COURS");

            if (c.getOffreId() != null) ps.setInt(19, c.getOffreId());
            else ps.setNull(19, Types.INTEGER);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("Candidature ajoutée avec succès (ID=" + id + ")");
                    return id;
                }
            }
            throw new RuntimeException("Impossible de récupérer l'ID de la candidature");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Candidature> afficherCandidatures() {
        String sql = "SELECT c.*, " +
                "CASE WHEN EXISTS (SELECT 1 FROM entretien e WHERE e.id_candidature = c.IDCandidat) " +
                "THEN 'TRAITE' ELSE 'NON_TRAITE' END AS statut " +
                "FROM candidature c ORDER BY c.IDCandidat DESC";
        List<Candidature> list = new java.util.ArrayList<>();

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Candidature c = new Candidature();

                c.setIDCandidat(rs.getInt("IDCandidat")); // ✅ مهمّة برشا

                c.setPrenom(rs.getString("prenom"));
                c.setNom(rs.getString("nom"));
                c.setEmail(rs.getString("email"));
                c.setIndicatif(rs.getString("indicatif"));
                c.setTel(rs.getString("tel"));
                c.setAdresse(rs.getString("adresse"));
                c.setVille(rs.getString("ville"));
                c.setDate_naissance(rs.getDate("date_naissance"));
                c.setHighest_degree(rs.getString("highest_degree"));
                c.setInstitution(rs.getString("institution"));
                c.setLettre_motivation(rs.getString("lettre_motivation"));
                c.setSkills_text(rs.getString("skills_text"));
                c.setPieces_jointes(rs.getString("Pieces_jointes"));
                c.setVideo_path(rs.getString("video_path"));
                c.setConfirm_info(rs.getBoolean("confirm_info"));
                c.setAccept_privacy(rs.getBoolean("accept_privacy"));
                c.setOffreId((Integer) rs.getObject("offre_id"));
                c.setStatut(rs.getString("statut"));

                try {
                    c.setDecisionRh(rs.getString("decision_rh"));
                } catch (Exception ignored) {
                }

                list.add(c);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public List<Candidature> afficherCandidaturesPourUserId(int userId) {
        String sql = "SELECT c.*, " +
                "CASE WHEN EXISTS (SELECT 1 FROM entretien e WHERE e.id_candidature = c.IDCandidat) " +
                "THEN 'TRAITE' ELSE 'NON_TRAITE' END AS statut " +
                "FROM candidature c WHERE c.user_id = ? ORDER BY c.IDCandidat DESC";

        List<Candidature> list = new java.util.ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidature c = new Candidature();

                    c.setIDCandidat(rs.getInt("IDCandidat"));

                    int uid = rs.getInt("user_id");
                    if (rs.wasNull()) c.setUserId(null);
                    else c.setUserId(uid);

                    c.setPrenom(rs.getString("prenom"));
                    c.setNom(rs.getString("nom"));
                    c.setEmail(rs.getString("email"));
                    c.setIndicatif(rs.getString("indicatif"));
                    c.setTel(rs.getString("tel"));
                    c.setAdresse(rs.getString("adresse"));
                    c.setVille(rs.getString("ville"));
                    c.setDate_naissance(rs.getDate("date_naissance"));
                    c.setHighest_degree(rs.getString("highest_degree"));
                    c.setInstitution(rs.getString("institution"));
                    c.setLettre_motivation(rs.getString("lettre_motivation"));
                    c.setSkills_text(rs.getString("skills_text"));
                    c.setPieces_jointes(rs.getString("Pieces_jointes"));
                    c.setVideo_path(rs.getString("video_path"));
                    c.setConfirm_info(rs.getBoolean("confirm_info"));
                    c.setAccept_privacy(rs.getBoolean("accept_privacy"));
                    c.setOffreId((Integer) rs.getObject("offre_id"));
                    c.setStatut(rs.getString("statut"));

                    try {
                        c.setDecisionRh(rs.getString("decision_rh"));
                    } catch (Exception ignored) {
                    }

                    list.add(c);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public List<Candidature> afficherCandidaturesPourEmail(String email) {
        String sql = "SELECT c.*, " +
                "CASE WHEN EXISTS (SELECT 1 FROM entretien e WHERE e.id_candidature = c.IDCandidat) " +
                "THEN 'TRAITE' ELSE 'NON_TRAITE' END AS statut " +
                "FROM candidature c WHERE c.email = ? ORDER BY c.IDCandidat DESC";

        List<Candidature> list = new java.util.ArrayList<>();

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Candidature c = new Candidature();

                    c.setIDCandidat(rs.getInt("IDCandidat"));
                    c.setPrenom(rs.getString("prenom"));
                    c.setNom(rs.getString("nom"));
                    c.setEmail(rs.getString("email"));
                    c.setIndicatif(rs.getString("indicatif"));
                    c.setTel(rs.getString("tel"));
                    c.setAdresse(rs.getString("adresse"));
                    c.setVille(rs.getString("ville"));
                    c.setDate_naissance(rs.getDate("date_naissance"));
                    c.setHighest_degree(rs.getString("highest_degree"));
                    c.setInstitution(rs.getString("institution"));
                    c.setLettre_motivation(rs.getString("lettre_motivation"));
                    c.setSkills_text(rs.getString("skills_text"));
                    c.setPieces_jointes(rs.getString("Pieces_jointes"));
                    c.setVideo_path(rs.getString("video_path"));
                    c.setConfirm_info(rs.getBoolean("confirm_info"));
                    c.setAccept_privacy(rs.getBoolean("accept_privacy"));
                    c.setOffreId((Integer) rs.getObject("offre_id"));
                    c.setStatut(rs.getString("statut"));

                    try {
                        c.setDecisionRh(rs.getString("decision_rh"));
                    } catch (Exception ignored) {
                    }

                    list.add(c);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public void setDecisionRh(int idCandidature, String decision) {
        String sql = "UPDATE candidature SET decision_rh = ? WHERE IDCandidat = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, decision);
            ps.setInt(2, idCandidature);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
