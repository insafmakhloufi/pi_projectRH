package Services.Offre;

import Entities.Offre.*;
import Entities.OffreStatistiques;
import Iservices.Offre.Ioffreemploi;
import Utils.Mydatabase;
import java.sql.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class OfrreEmploiServices implements Ioffreemploi {
    Connection con;

    // Services pour les entités faibles
    private TypeContratService typeContratService;
    private LocalisationService localisationService;
    private SecteurActiviteService secteurService;
    private TypeEmploiService typeEmploiService;

    public OfrreEmploiServices() {
        con = Mydatabase.getInstance().getConnection();
        // Initialiser les services
        typeContratService = new TypeContratService();
        localisationService = new LocalisationService();
        secteurService = new SecteurActiviteService();
        typeEmploiService = new TypeEmploiService();
    }

    public Connection getConnection() {
        return con;
    }

    public OffreEmploi getOffreById(int id) {
        String sql = "SELECT id, titre, description, niveau_experience, niveau_etudes, langues_requises, nombre_poste, date_limite_candidature, date_publication, teletravail, status, id_entreprise FROM offreemlpoi WHERE id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                OffreEmploi o = new OffreEmploi(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("status")
                );
                o.setNiveauExperience(rs.getString("niveau_experience"));
                o.setNiveauEtudes(rs.getString("niveau_etudes"));
                o.setLanguesRequises(rs.getString("langues_requises"));
                if (rs.getObject("nombre_poste") != null) {
                    o.setNombrePoste(rs.getInt("nombre_poste"));
                }
                if (rs.getDate("date_limite_candidature") != null) {
                    o.setDateLimiteCandidature(rs.getDate("date_limite_candidature").toLocalDate());
                }
                if (rs.getDate("date_publication") != null) {
                    o.setDatePublication(rs.getDate("date_publication").toLocalDate());
                }
                if (rs.getObject("teletravail") != null) {
                    o.setTeletravail(rs.getBoolean("teletravail"));
                }
                if (rs.getObject("id_entreprise") != null) {
                    o.setIdEntreprise(rs.getInt("id_entreprise"));
                }
                return o;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur getOffreById: " + e.getMessage(), e);
        }
    }

    @Override
    public void ajouterOffreEmploi(OffreEmploi o) {
        String req = "INSERT INTO offreemlpoi (titre, description, niveau_experience, niveau_etudes, langues_requises, nombre_poste, date_limite_candidature, date_publication, teletravail, status, id_entreprise, " +
                "id_type_contrat, id_localisation, id_secteur, id_type_emploi) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, o.getTitre());
            ste.setString(2, o.getDescription());
            ste.setString(3, o.getNiveauExperience());
            ste.setString(4, o.getNiveauEtudes());
            ste.setString(5, o.getLanguesRequises());

            if (o.getNombrePoste() != null) {
                ste.setInt(6, o.getNombrePoste());
            } else {
                ste.setNull(6, Types.INTEGER);
            }

            if (o.getDateLimiteCandidature() != null) {
                ste.setDate(7, Date.valueOf(o.getDateLimiteCandidature()));
            } else {
                ste.setNull(7, Types.DATE);
            }
            
            // Date de publication : utiliser la date fournie ou la date du jour
            if (o.getDatePublication() != null) {
                ste.setDate(8, Date.valueOf(o.getDatePublication()));
            } else {
                ste.setDate(8, Date.valueOf(java.time.LocalDate.now()));
            }

            if (o.getTeletravail() != null) {
                ste.setBoolean(9, o.getTeletravail());
            } else {
                ste.setNull(9, Types.BOOLEAN);
            }

            ste.setString(10, o.getStatus());

            if (o.getIdEntreprise() != null) {
                ste.setInt(11, o.getIdEntreprise());
            } else {
                ste.setNull(11, Types.INTEGER);
            }

            // Gérer les entités faibles (peuvent être null)
            if (o.getTypeContrat() != null) {
                ste.setInt(12, o.getTypeContrat().getId());
            } else {
                ste.setNull(12, Types.INTEGER);
            }

            if (o.getLocalisation() != null) {
                ste.setInt(13, o.getLocalisation().getId());
            } else {
                ste.setNull(13, Types.INTEGER);
            }

            if (o.getSecteur() != null) {
                ste.setInt(14, o.getSecteur().getId());
            } else {
                ste.setNull(14, Types.INTEGER);
            }

            if (o.getTypeEmploi() != null) {
                ste.setInt(15, o.getTypeEmploi().getId());
            } else {
                ste.setNull(15, Types.INTEGER);
            }

            ste.executeUpdate();
            System.out.println("✅ Offre ajoutée avec entités faibles");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur ajout: " + e.getMessage());
        }
    }

    @Override
    public void supprimerOffreEmploi(int id) {
        String req = "DELETE FROM offreemlpoi WHERE id=?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ste.executeUpdate();
            System.out.println("✅ Offre " + id + " supprimée");
        } catch (Exception e) {
            throw new RuntimeException("Erreur suppression: " + e.getMessage());
        }
    }

    @Override
    public void modifierOffreEmploi(OffreEmploi o) {
        String req = "UPDATE offreemlpoi SET titre=?, description=?, niveau_experience=?, niveau_etudes=?, langues_requises=?, nombre_poste=?, date_limite_candidature=?, date_publication=?, teletravail=?, status=?, id_entreprise=?, " +
                "id_type_contrat=?, id_localisation=?, id_secteur=?, id_type_emploi=? " +
                "WHERE id=?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, o.getTitre());
            ste.setString(2, o.getDescription());
            ste.setString(3, o.getNiveauExperience());
            ste.setString(4, o.getNiveauEtudes());
            ste.setString(5, o.getLanguesRequises());

            if (o.getNombrePoste() != null) {
                ste.setInt(6, o.getNombrePoste());
            } else {
                ste.setNull(6, Types.INTEGER);
            }

            if (o.getDateLimiteCandidature() != null) {
                ste.setDate(7, Date.valueOf(o.getDateLimiteCandidature()));
            } else {
                ste.setNull(7, Types.DATE);
            }
            
            if (o.getDatePublication() != null) {
                ste.setDate(8, Date.valueOf(o.getDatePublication()));
            } else {
                ste.setNull(8, Types.DATE);
            }

            if (o.getTeletravail() != null) {
                ste.setBoolean(9, o.getTeletravail());
            } else {
                ste.setNull(9, Types.BOOLEAN);
            }

            ste.setString(10, o.getStatus());

            if (o.getIdEntreprise() != null) {
                ste.setInt(11, o.getIdEntreprise());
            } else {
                ste.setNull(11, Types.INTEGER);
            }

            // Gérer les entités faibles
            if (o.getTypeContrat() != null) {
                ste.setInt(12, o.getTypeContrat().getId());
            } else {
                ste.setNull(12, Types.INTEGER);
            }

            if (o.getLocalisation() != null) {
                ste.setInt(13, o.getLocalisation().getId());
            } else {
                ste.setNull(13, Types.INTEGER);
            }

            if (o.getSecteur() != null) {
                ste.setInt(14, o.getSecteur().getId());
            } else {
                ste.setNull(14, Types.INTEGER);
            }

            if (o.getTypeEmploi() != null) {
                ste.setInt(15, o.getTypeEmploi().getId());
            } else {
                ste.setNull(15, Types.INTEGER);
            }

            ste.setInt(16, o.getId());
            ste.executeUpdate();
            System.out.println("✅ Offre " + o.getId() + " modifiée");
        } catch (SQLException e) {
            throw new RuntimeException("Erreur modification: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OffreEmploi> afficherOfrreEmploi() {
        List<OffreEmploi> offres = new ArrayList<>();
        String req = "SELECT " +
                "o.id, o.titre, o.description, o.niveau_experience, o.niveau_etudes, o.langues_requises, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "ORDER BY o.id DESC";

        try (PreparedStatement ps = con.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                offres.add(buildOffreFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur affichage: " + e.getMessage(), e);
        }
        return offres;
    }

    public List<OffreEmploi> afficherOfrreEmploiLight() {
        List<OffreEmploi> offres = new ArrayList<>();
        String req = "SELECT o.id, o.titre, o.description, o.niveau_experience, o.niveau_etudes, o.langues_requises, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "ORDER BY o.id DESC LIMIT 50";

        try (PreparedStatement ps = con.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                OffreEmploi o = buildOffreFromResultSet(rs);
                offres.add(o);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur afficherOfrreEmploiLight: " + e.getMessage(), e);
        }
        return offres;
    }

    public List<OffreCompatibilite> getOffresCompatibles(int userId) {
        List<OffreCompatibilite> resultats = new ArrayList<>();

        String req = "SELECT " +
                "o.id, o.titre, o.description, o.niveau_experience, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, o.niveau_etudes, o.langues_requises, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie, " +
                "c.ville as cand_ville, c.highest_degree as cand_degree, c.skills_text as cand_skills, " +
                "( " +
                "    CASE " +
                "        WHEN c.skills_text IS NULL OR c.skills_text = '' THEN 0 " +
                "        ELSE ( " +
                "            CASE WHEN LOWER(o.titre) LIKE CONCAT('%', LOWER(c.skills_text), '%') THEN 20 ELSE 0 END + " +
                "            CASE WHEN LOWER(o.description) LIKE CONCAT('%', LOWER(c.skills_text), '%') THEN 20 ELSE 0 END + " +
                "            CASE WHEN o.langues_requises IS NOT NULL AND LOWER(o.langues_requises) LIKE CONCAT('%', LOWER(c.skills_text), '%') THEN 10 ELSE 0 END " +
                "        ) " +
                "    END + " +
                "    CASE " +
                "        WHEN c.highest_degree IS NULL OR o.niveau_etudes IS NULL THEN 0 " +
                "        WHEN LOWER(o.niveau_etudes) LIKE CONCAT('%', LOWER(c.highest_degree), '%') THEN 30 " +
                "        WHEN LOWER(c.highest_degree) LIKE CONCAT('%', LOWER(o.niveau_etudes), '%') THEN 25 " +
                "        ELSE 0 " +
                "    END + " +
                "    CASE " +
                "        WHEN c.ville IS NULL OR l.ville IS NULL THEN 0 " +
                "        WHEN LOWER(TRIM(c.ville)) = LOWER(TRIM(l.ville)) THEN 10 " +
                "        ELSE 0 " +
                "    END " +
                ") AS score_compatibilite " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "JOIN ( " +
                "    SELECT ville, highest_degree, skills_text " +
                "    FROM candidature " +
                "    WHERE id_user = ? " +
                "    ORDER BY IDCandidat DESC " +
                "    LIMIT 1 " +
                ") c ON 1=1 " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "WHERE o.id NOT IN ( " +
                "    SELECT id_offre FROM candidature WHERE id_user = ? " +
                ") " +
                "ORDER BY score_compatibilite DESC, o.id DESC " +
                "LIMIT 20";

        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OffreEmploi o = buildOffreFromResultSet(rs);

                    OffreCompatibilite oc = new OffreCompatibilite();
                    oc.setOffre(o);
                    oc.setScore(rs.getInt("score_compatibilite"));
                    oc.setCandidatSkills(rs.getString("cand_skills"));
                    oc.setCandidatDiplome(rs.getString("cand_degree"));
                    oc.setCandidatVille(rs.getString("cand_ville"));
                    oc.setOffreVille(rs.getString("l_ville"));

                    // Ne garder que les offres avec score > 0
                    if (oc.getScore() > 0) {
                        resultats.add(oc);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur compatibilité offres: " + e.getMessage(), e);
        }

        return resultats;
    }

    /**
     * Recommande des offres en fonction du titre du candidat (ex: "Ingénieur Informatique").
     */
    public List<OffreCompatibilite> getOffresRecommandeesParTitre(String userTitre) {
        List<OffreCompatibilite> resultats = new ArrayList<>();
        List<String> keywords = tokenizeTitre(userTitre);
        if (keywords.isEmpty()) {
            return resultats;
        }

        StringBuilder where = new StringBuilder();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) {
                where.append(" OR ");
            }
            where.append("(LOWER(o.titre) LIKE ? OR LOWER(o.description) LIKE ? OR LOWER(s.nom_secteur) LIKE ? OR LOWER(te.categorie) LIKE ?)");
        }

        String req = "SELECT " +
                "o.id, o.titre, o.description, o.niveau_experience, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, o.niveau_etudes, o.langues_requises, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "WHERE (" + where + ") " +
                "ORDER BY o.id DESC LIMIT 20";

        try (PreparedStatement ps = con.prepareStatement(req)) {
            int param = 1;
            for (String k : keywords) {
                String like = "%" + k.toLowerCase(Locale.ROOT) + "%";
                ps.setString(param++, like);
                ps.setString(param++, like);
                ps.setString(param++, like);
                ps.setString(param++, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OffreEmploi o = buildOffreFromResultSet(rs);

                    OffreCompatibilite oc = new OffreCompatibilite();
                    oc.setOffre(o);
                    oc.setScore(computeTitreScore(o, keywords));
                    oc.setCandidatSkills(userTitre);
                    oc.setCandidatDiplome(null);
                    oc.setCandidatVille(null);
                    oc.setOffreVille(rs.getString("l_ville"));

                    // Ne garder que les offres avec score > 0
                    if (oc.getScore() > 0) {
                        resultats.add(oc);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur recommandation par titre: " + e.getMessage(), e);
        }

        return resultats;
    }

    private OffreEmploi buildOffreFromResultSet(ResultSet rs) throws SQLException {
        OffreEmploi o = new OffreEmploi(
                rs.getInt("id"),
                rs.getString("titre"),
                rs.getString("description"),
                rs.getString("status")
        );
        if (rs.getObject("id_entreprise") != null) {
            o.setIdEntreprise(rs.getInt("id_entreprise"));
        }
        try {
            o.setNomEntreprise(rs.getString("ent_nom"));
            o.setLogoEntreprise(rs.getString("ent_logo"));
        } catch (SQLException ignored) {
        }
        o.setNiveauExperience(rs.getString("niveau_experience"));
        o.setNiveauEtudes(rs.getString("niveau_etudes"));
        o.setLanguesRequises(rs.getString("langues_requises"));
        
        if (rs.getObject("nombre_poste") != null) {
            o.setNombrePoste(rs.getInt("nombre_poste"));
        }
        if (rs.getDate("date_limite_candidature") != null) {
            o.setDateLimiteCandidature(rs.getDate("date_limite_candidature").toLocalDate());
        }
        if (rs.getDate("date_publication") != null) {
            o.setDatePublication(rs.getDate("date_publication").toLocalDate());
        }
        if (rs.getObject("teletravail") != null) {
            o.setTeletravail(rs.getBoolean("teletravail"));
        }

        int idTypeContrat = rs.getObject("id_type_contrat") != null ? rs.getInt("id_type_contrat") : 0;
        if (idTypeContrat > 0) {
            String nom = rs.getString("tc_nom");
            TypeContrat tc = new TypeContrat(idTypeContrat, nom != null ? nom : "", "");
            o.setTypeContrat(tc);
        }

        int idLoc = rs.getObject("id_localisation") != null ? rs.getInt("id_localisation") : 0;
        if (idLoc > 0) {
            String ville = rs.getString("l_ville");
            String pays = rs.getString("l_pays");
            String adresse = rs.getString("l_adresse");
            Double lat = rs.getObject("l_latitude") != null ? rs.getDouble("l_latitude") : null;
            Double lng = rs.getObject("l_longitude") != null ? rs.getDouble("l_longitude") : null;
            Localisation loc = new Localisation(idLoc, ville, pays, adresse, lat, lng);
            o.setLocalisation(loc);
        }

        int idSecteur = rs.getObject("id_secteur") != null ? rs.getInt("id_secteur") : 0;
        if (idSecteur > 0) {
            String nom = rs.getString("s_nom");
            SecteurActivite sec = new SecteurActivite(idSecteur, nom != null ? nom : "");
            o.setSecteur(sec);
        }

        int idTypeEmploi = rs.getObject("id_type_emploi") != null ? rs.getInt("id_type_emploi") : 0;
        if (idTypeEmploi > 0) {
            String nom = rs.getString("te_categorie");
            TypeEmploi te = new TypeEmploi(idTypeEmploi, nom != null ? nom : "");
            o.setTypeEmploi(te);
        }

        return o;
    }

    private List<String> tokenizeTitre(String titre) {
        List<String> keywords = new ArrayList<>();
        if (titre == null || titre.trim().isEmpty()) {
            return keywords;
        }
        String normalized = Normalizer.normalize(titre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);

        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            token = token.replaceAll("[^a-z0-9]", "");
            if (token.length() > 2) {
                keywords.add(token);
                keywords.addAll(getSynonyms(token));
            }
        }
        return new ArrayList<>(new java.util.LinkedHashSet<>(keywords));
    }

    private List<String> getSynonyms(String token) {
        List<String> synonyms = new ArrayList<>();
        switch (token) {
            case "commercial":
                synonyms.add("commerce");
                synonyms.add("vente");
                break;
            case "commerce":
                synonyms.add("commercial");
                synonyms.add("vente");
                break;
            case "medecin":
            case "medecine":
                synonyms.add("sante");
                synonyms.add("santé");
                synonyms.add("hopital");
                synonyms.add("clinique");
                break;
            case "sante":
            case "santé":
                synonyms.add("medecin");
                synonyms.add("medecine");
                synonyms.add("hopital");
                synonyms.add("clinique");
                break;
            case "ingenieur":
                synonyms.add("informatique");
                synonyms.add("tech");
                synonyms.add("technique");
                break;
            case "informatique":
                synonyms.add("ingenieur");
                synonyms.add("tech");
                synonyms.add("developpeur");
                synonyms.add("dev");
                break;
            case "developpeur":
            case "developer":
                synonyms.add("informatique");
                synonyms.add("ingenieur");
                synonyms.add("dev");
                synonyms.add("programmation");
                break;
            case "marketing":
                synonyms.add("communication");
                synonyms.add("digital");
                break;
            case "comptable":
            case "comptabilite":
                synonyms.add("finance");
                synonyms.add("financier");
                synonyms.add("audit");
                break;
            case "finance":
            case "financier":
                synonyms.add("comptable");
                synonyms.add("comptabilite");
                synonyms.add("audit");
                break;
            case "rh":
            case "ressourceshumaines":
                synonyms.add("hr");
                synonyms.add("humaines");
                synonyms.add("personnel");
                break;
            case "juriste":
            case "avocat":
                synonyms.add("droit");
                synonyms.add("legal");
                synonyms.add("juridique");
                break;
            case "enseignant":
            case "professeur":
                synonyms.add("education");
                synonyms.add("ecole");
                synonyms.add("formation");
                break;
        }
        return synonyms;
    }

    private int computeTitreScore(OffreEmploi o, List<String> keywords) {
        int score = 0;
        String titre = o.getTitre() != null ? o.getTitre().toLowerCase(Locale.ROOT) : "";
        String desc = o.getDescription() != null ? o.getDescription().toLowerCase(Locale.ROOT) : "";
        String secteur = o.getSecteur() != null && o.getSecteur().getNom() != null ? o.getSecteur().getNom().toLowerCase(Locale.ROOT) : "";
        String categorie = o.getTypeEmploi() != null && o.getTypeEmploi().getNom() != null ? o.getTypeEmploi().getNom().toLowerCase(Locale.ROOT) : "";
        
        for (String kw : keywords) {
            if (titre.contains(kw)) {
                score += 30; // Titre = plus fort poids
            } else if (desc.contains(kw)) {
                score += 15;
            } else if (secteur.contains(kw)) {
                score += 25; // Secteur = poids important
            } else if (categorie.contains(kw)) {
                score += 20; // Catégorie = bon poids
            }
        }
        return Math.min(score, 100);
    }

    public void incrementerVues(int offreId) {
        String req = "INSERT INTO offre_vue (id_offre, date_vue) VALUES (?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, offreId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("doesn't exist") || e.getMessage().contains("n'existe pas")) {
                creerTableOffreVue();
                try (PreparedStatement ps2 = con.prepareStatement(req)) {
                    ps2.setInt(1, offreId);
                    ps2.executeUpdate();
                } catch (SQLException e2) {
                    System.err.println("[ERREUR] Échec incrémentation après création table: " + e2.getMessage());
                }
            } else {
                System.err.println("[ERREUR] Incrémentation vues: " + e.getMessage());
            }
        }
    }

    private void creerTableOffreVue() {
        String createTable = "CREATE TABLE IF NOT EXISTS offre_vue (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "id_offre INT NOT NULL," +
                "date_vue TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (id_offre) REFERENCES offreemlpoi(id) ON DELETE CASCADE" +
                ")";
        try (PreparedStatement ps = con.prepareStatement(createTable)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[ERREUR] Impossible de créer la table offre_vue: " + e.getMessage());
        }
    }

    public List<OffreEmploi> afficherOfrreEmploiLightByEntrepriseId(int idEntreprise) {
        List<OffreEmploi> offres = new ArrayList<>();
        String req = "SELECT o.id, o.titre, o.description, o.niveau_experience, o.niveau_etudes, o.langues_requises, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "WHERE o.id_entreprise = ? " +
                "ORDER BY o.id DESC LIMIT 20";

        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, idEntreprise);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OffreEmploi o = buildOffreFromResultSet(rs);
                    offres.add(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur afficherOfrreEmploiLightByEntrepriseId: " + e.getMessage(), e);
        }
        return offres;
    }

    public Map<String, Integer> getOffresParSecteur(int idEntreprise) {
        Map<String, Integer> stats = new HashMap<>();
        String req = "SELECT s.nom_secteur, COUNT(*) as count " +
                "FROM offreemlpoi o " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "WHERE o.id_entreprise = ? " +
                "GROUP BY s.nom_secteur " +
                "ORDER BY count DESC";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, idEntreprise);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String nom = rs.getString("nom_secteur");
                int count = rs.getInt("count");
                if (nom != null && !nom.isEmpty()) {
                    stats.put(nom, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération stats secteur: " + e.getMessage());
        }
        return stats;
    }

    public Map<String, Integer> getOffresParLocalisation(int idEntreprise) {
        Map<String, Integer> stats = new HashMap<>();
        String req = "SELECT l.ville, COUNT(*) as count " +
                "FROM offreemlpoi o " +
                "JOIN localisation l ON o.id_localisation = l.id_localisation " +
                "WHERE o.id_entreprise = ? " +
                "GROUP BY l.ville " +
                "ORDER BY count DESC";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, idEntreprise);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                stats.put(rs.getString("ville"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération stats localisation: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Récupère les statistiques globales pour une entreprise
     */
    public StatistiquesGlobales getStatistiquesGlobales(int idEntreprise) {
        StatistiquesGlobales stats = new StatistiquesGlobales();
        try {
            // Count offres
            String reqOffres = "SELECT COUNT(*) as total FROM offreemlpoi WHERE id_entreprise = ?";
            try (PreparedStatement ps = con.prepareStatement(reqOffres)) {
                ps.setInt(1, idEntreprise);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.setTotalOffres(rs.getInt("total"));
                }
            }
            // Count candidatures and vues
            String req = "SELECT COUNT(DISTINCT c.id_user) as total_candidatures, " +
                    "COUNT(DISTINCT v.id) as total_vues, " +
                    "AVG(CASE WHEN c.id_offre IS NOT NULL THEN 1 ELSE 0 END) * 100 as taux_conversion " +
                    "FROM offreemlpoi o " +
                    "LEFT JOIN candidature c ON o.id = c.id_offre " +
                    "LEFT JOIN offre_vue v ON o.id = v.id_offre " +
                    "WHERE o.id_entreprise = ?";
            try (PreparedStatement ps = con.prepareStatement(req)) {
                ps.setInt(1, idEntreprise);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    stats.setTotalCandidatures(rs.getInt("total_candidatures"));
                    stats.setTotalVues(rs.getInt("total_vues"));
                    stats.setTauxConversionGlobal(rs.getDouble("taux_conversion"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération stats globales: " + e.getMessage());
        }
        return stats;
    }

    public static class StatistiquesGlobales {
        private int totalOffres;
        private int totalCandidatures;
        private int totalVues;
        private double tauxConversionGlobal;

        public int getTotalOffres() { return totalOffres; }
        public void setTotalOffres(int totalOffres) { this.totalOffres = totalOffres; }

        public int getTotalCandidatures() { return totalCandidatures; }
        public void setTotalCandidatures(int totalCandidatures) { this.totalCandidatures = totalCandidatures; }

        public int getTotalVues() { return totalVues; }
        public void setTotalVues(int totalVues) { this.totalVues = totalVues; }

        public double getTauxConversionGlobal() { return tauxConversionGlobal; }
        public void setTauxConversionGlobal(double tauxConversionGlobal) { this.tauxConversionGlobal = tauxConversionGlobal; }
    }

    /**
     * Détails des vues par offre
     */
    public static class OffreVueDetails {
        private String offreTitre;
        private String userNom;
        private String dateVue;

        public OffreVueDetails(String offreTitre, String userNom, String dateVue) {
            this.offreTitre = offreTitre;
            this.userNom = userNom;
            this.dateVue = dateVue;
        }

        public String getOffreTitre() { return offreTitre; }
        public void setOffreTitre(String offreTitre) { this.offreTitre = offreTitre; }

        public String getUserNom() { return userNom; }
        public void setUserNom(String userNom) { this.userNom = userNom; }

        public String getDateVue() { return dateVue; }
        public void setDateVue(String dateVue) { this.dateVue = dateVue; }
    }

    /**
     * Récupère les détails des vues pour les offres d'une entreprise
     */
    public List<OffreVueDetails> getDetailsVuesParOffre(int idEntreprise) {
        List<OffreVueDetails> details = new ArrayList<>();
        String req = "SELECT o.titre as offre_titre, " +
                     "'Anonyme' as user_nom, " +
                     "v.date_vue as date_vue " +
                     "FROM offreemlpoi o " +
                     "JOIN offre_vue v ON o.id = v.id_offre " +
                     "WHERE o.id_entreprise = ? " +
                     "ORDER BY o.titre, v.date_vue DESC";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, idEntreprise);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                details.add(new OffreVueDetails(
                    rs.getString("offre_titre"),
                    rs.getString("user_nom"),
                    rs.getTimestamp("date_vue").toString()
                ));
            }
        } catch (SQLException e) {
            System.err.println("[DEBUG] Erreur récupération détails vues: " + e.getMessage());
            e.printStackTrace();
        }
        return details;
    }

    /**
     * Compte le nombre de filiales uniques (localisations distinctes) pour une entreprise
     */
    public int countFilialesUniques(int idEntreprise) {
        String req = "SELECT COUNT(DISTINCT o.id_localisation) as total " +
                "FROM offreemlpoi o " +
                "WHERE o.id_entreprise = ? AND o.id_localisation IS NOT NULL";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, idEntreprise);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("Erreur count filiales: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Calcule le taux de part du marché de l'entreprise
     * (offres de l'entreprise / total des offres dans la base)
     */
    public double getTauxMarcheEntreprise(int idEntreprise) {
        try {
            // Compter les offres de l'entreprise
            String reqEntreprise = "SELECT COUNT(*) as total FROM offreemlpoi WHERE id_entreprise = ?";
            int offresEntreprise = 0;
            try (PreparedStatement ps = con.prepareStatement(reqEntreprise)) {
                ps.setInt(1, idEntreprise);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    offresEntreprise = rs.getInt("total");
                }
            }

            // Compter toutes les offres actives dans la base
            String reqTotal = "SELECT COUNT(*) as total FROM offreemlpoi WHERE status = 'active' OR status IS NULL";
            int totalOffres = 0;
            try (PreparedStatement ps = con.prepareStatement(reqTotal)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalOffres = rs.getInt("total");
                }
            }

            if (totalOffres == 0) {
                return 0.0;
            }

            return (offresEntreprise * 100.0) / totalOffres;
        } catch (SQLException e) {
            System.err.println("Erreur calcul taux marché: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Filtre les offres par plage de dates de publication
     * @param dateDebut Date de début (incluse), null = pas de limite
     * @param dateFin Date de fin (incluse), null = pas de limite
     * @return Liste des offres dans la plage de dates
     */
    public List<OffreEmploi> filtrerParDatePublication(java.time.LocalDate dateDebut, java.time.LocalDate dateFin) {
        List<OffreEmploi> offres = new ArrayList<>();
        StringBuilder req = new StringBuilder("SELECT " +
                "o.id, o.titre, o.description, o.niveau_experience, o.niveau_etudes, o.langues_requises, o.status, " +
                "o.id_type_contrat, o.id_localisation, o.id_secteur, o.id_type_emploi, " +
                "o.nombre_poste, o.date_limite_candidature, o.date_publication, o.teletravail, " +
                "o.id_entreprise, e.nom_entreprise as ent_nom, e.logo as ent_logo, " +
                "l.ville as l_ville, l.pays as l_pays, l.adresse as l_adresse, l.latitude as l_latitude, l.longitude as l_longitude, " +
                "s.nom_secteur as s_nom, " +
                "tc.nom_type as tc_nom, " +
                "te.categorie as te_categorie " +
                "FROM offreemlpoi o " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "LEFT JOIN Localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN SecteurActivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN TypeContrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN TypeEmploi te ON o.id_type_emploi = te.id_type_emploi " +
                "WHERE 1=1 ");
        
        if (dateDebut != null) {
            req.append("AND o.date_publication >= ? ");
        }
        if (dateFin != null) {
            req.append("AND o.date_publication <= ? ");
        }
        req.append("ORDER BY o.date_publication DESC");
        
        try (PreparedStatement ps = con.prepareStatement(req.toString())) {
            int paramIndex = 1;
            if (dateDebut != null) {
                ps.setDate(paramIndex++, Date.valueOf(dateDebut));
            }
            if (dateFin != null) {
                ps.setDate(paramIndex++, Date.valueOf(dateFin));
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    offres.add(buildOffreFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur filtre par date: " + e.getMessage(), e);
        }
        return offres;
    }

    /**
     * Récupère les offres publiées récemment (moins de 7 jours)
     * @return Liste des offres "Nouveau"
     */
    public List<OffreEmploi> getOffresRecentes() {
        return filtrerParDatePublication(
            java.time.LocalDate.now().minusDays(7),
            java.time.LocalDate.now()
        );
    }
}