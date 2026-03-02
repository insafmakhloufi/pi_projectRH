package Services.Formation;

import Entites.Formation.Cour;
import Iservices.Formation.IcourServices;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CourServices implements IcourServices {
    private final Connection con;
    private final String courComplexiteCol;
    private final String courDureCol;

    public CourServices() {
        con = Mydatabase.getInstance().getConnection();
        courComplexiteCol = resolveColumnName("Cour", "complexite", "complexit\u00E9");
        courDureCol = resolveColumnName("Cour", "dure", "dur\u00E9");
    }

    @Override
    public void ajouterCour(Cour c) {
        String req = "INSERT INTO Cour (formation_id, nom_formateur, " + q(courComplexiteCol) + ", description, origine, " + q(courDureCol) + ", nb_chapitres) VALUES (?,?,?,?,?,?,?)";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            if (c.getFormationId() == null) {
                ste.setNull(1, Types.INTEGER);
            } else {
                ste.setInt(1, c.getFormationId());
            }
            ste.setString(2, c.getNomFormateur());
            ste.setInt(3, c.getComplexite());
            ste.setString(4, c.getDescription());
            ste.setString(5, c.getOrigine());
            ste.setFloat(6, c.getDure());
            ste.setInt(7, c.getNbChapitres());
            ste.executeUpdate();
            System.out.println("Ajouter avec succes");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void supprimerCour(int id) {
        String req = "DELETE FROM Cour WHERE id = ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ste.executeUpdate();
            System.out.println("Supprimer avec succes");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void modifierCour(Cour c) {
        String req = "UPDATE Cour SET nom_formateur = ?, " + q(courComplexiteCol) + " = ?, description = ?, origine = ?, " + q(courDureCol) + " = ?, nb_chapitres = ? WHERE id = ?";

        if (c == null) {
            throw new IllegalArgumentException("Cour ne doit pas etre null");
        }
        if (c.getId() <= 0) {
            throw new IllegalArgumentException("Id cour invalide");
        }

        Cour existing = getCourById(c.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Cour introuvable (id=" + c.getId() + ")");
        }

        String nomFormateur = (c.getNomFormateur() == null || c.getNomFormateur().isBlank()) ? existing.getNomFormateur() : c.getNomFormateur();
        int complexite = (c.getComplexite() <= 0) ? existing.getComplexite() : c.getComplexite();
        String description = (c.getDescription() == null || c.getDescription().isBlank()) ? existing.getDescription() : c.getDescription();
        String origine = (c.getOrigine() == null || c.getOrigine().isBlank()) ? existing.getOrigine() : c.getOrigine();
        float dure = (c.getDure() <= 0f) ? existing.getDure() : c.getDure();
        int nbChapitres = (c.getNbChapitres() <= 0) ? existing.getNbChapitres() : c.getNbChapitres();

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, nomFormateur);
            ste.setInt(2, complexite);
            ste.setString(3, description);
            ste.setString(4, origine);
            ste.setFloat(5, dure);
            ste.setInt(6, nbChapitres);
            ste.setInt(7, c.getId());
            ste.executeUpdate();
            System.out.println("Modifier avec succes");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Cour getCourById(int id) {
        String req = "SELECT id, formation_id, nom_formateur, " + q(courComplexiteCol) + " AS complexite, description, origine, " + q(courDureCol) + " AS dure, nb_chapitres FROM Cour WHERE id = ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ResultSet rs = ste.executeQuery();
            if (rs.next()) {
                Integer formationId = (Integer) rs.getObject("formation_id");
                Cour c = new Cour(
                        rs.getInt("id"),
                        formationId,
                        rs.getString("nom_formateur"),
                        rs.getInt("complexite"),
                        rs.getString("description"),
                        rs.getString("origine"),
                        rs.getFloat("dure")
                );
                c.setNbChapitres(rs.getInt("nb_chapitres"));
                return c;
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getNbChapitresMax(int courId) {
        String sql = "SELECT nb_chapitres FROM Cour WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public List<Cour> afficherCours() {
        List<Cour> cours = new ArrayList<>();
        String req = "SELECT id, formation_id, nom_formateur, " + q(courComplexiteCol) + " AS complexite, description, origine, " + q(courDureCol) + " AS dure, nb_chapitres FROM Cour";
        try {
            Statement ste = con.createStatement();
            ResultSet rs = ste.executeQuery(req);
            while (rs.next()) {
                Integer formationId = (Integer) rs.getObject("formation_id");
                Cour c = new Cour(
                        rs.getInt("id"),
                        formationId,
                        rs.getString("nom_formateur"),
                        rs.getInt("complexite"),
                        rs.getString("description"),
                        rs.getString("origine"),
                        rs.getFloat("dure")
                );
                c.setNbChapitres(rs.getInt("nb_chapitres"));
                cours.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return cours;
    }

    public List<Cour> afficherCoursParFormation(int formationId) {
        List<Cour> cours = new ArrayList<>();
        String req = "SELECT id, formation_id, nom_formateur, " + q(courComplexiteCol) + " AS complexite, description, origine, " + q(courDureCol) + " AS dure, nb_chapitres FROM Cour WHERE formation_id = ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, formationId);
            ResultSet rs = ste.executeQuery();
            while (rs.next()) {
                Integer fid = (Integer) rs.getObject("formation_id");
                Cour c = new Cour(
                        rs.getInt("id"),
                        fid,
                        rs.getString("nom_formateur"),
                        rs.getInt("complexite"),
                        rs.getString("description"),
                        rs.getString("origine"),
                        rs.getFloat("dure")
                );
                c.setNbChapitres(rs.getInt("nb_chapitres"));
                cours.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return cours;
    }

    public float sommeDureeCoursParFormation(int formationId) {
        String req = "SELECT COALESCE(SUM(" + q(courDureCol) + "), 0) AS total FROM Cour WHERE formation_id = ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, formationId);
            ResultSet rs = ste.executeQuery();
            if (rs.next()) {
                return rs.getFloat("total");
            }
            return 0f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public float sommeDureeCoursParFormationSansCour(int formationId, int courId) {
        String req = "SELECT COALESCE(SUM(" + q(courDureCol) + "), 0) AS total FROM Cour WHERE formation_id = ? AND id <> ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, formationId);
            ste.setInt(2, courId);
            ResultSet rs = ste.executeQuery();
            if (rs.next()) {
                return rs.getFloat("total");
            }
            return 0f;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String q(String identifier) {
        return "`" + identifier.replace("`", "") + "`";
    }

    private String resolveColumnName(String table, String... aliases) {
        try {
            DatabaseMetaData md = con.getMetaData();
            try (ResultSet rs = md.getColumns(con.getCatalog(), null, table, null)) {
                List<String> cols = new ArrayList<>();
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME"));
                }
                for (String alias : aliases) {
                    String target = normalize(alias);
                    for (String col : cols) {
                        if (normalize(col).equals(target)) {
                            return col;
                        }
                    }
                }
            }
        } catch (SQLException ignored) {
        }
        return aliases[0];
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }
}
