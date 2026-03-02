package Services.Formation;

import Entites.Formation.Chapitre;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChapitreServices {

    private final Connection con;
    private final CourServices courServices;
    private final String chapitreComplexiteCol;
    private final boolean chapitreHasComplexite;

    public ChapitreServices() {
        con = Mydatabase.getInstance().getConnection();
        courServices = new CourServices();
        String detected = resolveColumnName("Chapitre", "complexite", "complexit\u00E9");
        chapitreHasComplexite = detected != null;
        chapitreComplexiteCol = chapitreHasComplexite ? detected : "complexite";
    }

    public int countByCour(int courId) {
        String sql = "SELECT COUNT(*) FROM Chapitre WHERE cour_id = ?";
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

    public List<Chapitre> afficherChapitresParCour(int courId) {
        List<Chapitre> out = new ArrayList<>();
        String sql = chapitreHasComplexite
                ? "SELECT id, cour_id, titre, ordre, contenu, " + q(chapitreComplexiteCol) + " AS complexite FROM Chapitre WHERE cour_id = ? ORDER BY ordre"
                : "SELECT id, cour_id, titre, ordre, contenu, 1 AS complexite FROM Chapitre WHERE cour_id = ? ORDER BY ordre";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Chapitre(
                            rs.getInt("id"),
                            rs.getInt("cour_id"),
                            rs.getString("titre"),
                            rs.getInt("ordre"),
                            rs.getString("contenu"),
                            rs.getInt("complexite")
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    public Chapitre getChapitreById(int id) {
        String sql = chapitreHasComplexite
                ? "SELECT id, cour_id, titre, ordre, contenu, " + q(chapitreComplexiteCol) + " AS complexite FROM Chapitre WHERE id = ?"
                : "SELECT id, cour_id, titre, ordre, contenu, 1 AS complexite FROM Chapitre WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Chapitre(
                            rs.getInt("id"),
                            rs.getInt("cour_id"),
                            rs.getString("titre"),
                            rs.getInt("ordre"),
                            rs.getString("contenu"),
                            rs.getInt("complexite")
                    );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void ajouterChapitre(Chapitre chapitre) {
        if (chapitre == null) {
            throw new IllegalArgumentException("Chapitre ne doit pas etre null");
        }
        if (chapitre.getCourId() <= 0) {
            throw new IllegalArgumentException("cour_id invalide");
        }
        if (chapitre.getTitre() == null || chapitre.getTitre().isBlank()) {
            throw new IllegalArgumentException("Titre requis");
        }
        if (chapitre.getContenu() == null || chapitre.getContenu().isBlank()) {
            throw new IllegalArgumentException("Contenu requis");
        }

        int max = courServices.getNbChapitresMax(chapitre.getCourId());
        int current = countByCour(chapitre.getCourId());
        if (current >= max) {
            throw new IllegalStateException("Limite atteinte: " + current + "/" + max);
        }

        int ordre = nextOrdre(chapitre.getCourId());
        String sql = chapitreHasComplexite
                ? "INSERT INTO Chapitre (cour_id, titre, ordre, contenu, " + q(chapitreComplexiteCol) + ") VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO Chapitre (cour_id, titre, ordre, contenu) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, chapitre.getCourId());
            ps.setString(2, chapitre.getTitre().trim());
            ps.setInt(3, ordre);
            ps.setString(4, chapitre.getContenu());
            if (chapitreHasComplexite) {
                ps.setInt(5, chapitre.getComplexite());
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    chapitre.setId(rs.getInt(1));
                }
            }
            chapitre.setOrdre(ordre);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void modifierChapitre(Chapitre chapitre) {
        if (chapitre == null) {
            throw new IllegalArgumentException("Chapitre ne doit pas etre null");
        }
        if (chapitre.getId() <= 0) {
            throw new IllegalArgumentException("id invalide");
        }
        if (chapitre.getTitre() == null || chapitre.getTitre().isBlank()) {
            throw new IllegalArgumentException("Titre requis");
        }
        if (chapitre.getContenu() == null || chapitre.getContenu().isBlank()) {
            throw new IllegalArgumentException("Contenu requis");
        }

        String sql = chapitreHasComplexite
                ? "UPDATE Chapitre SET titre = ?, contenu = ?, " + q(chapitreComplexiteCol) + " = ? WHERE id = ?"
                : "UPDATE Chapitre SET titre = ?, contenu = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, chapitre.getTitre().trim());
            ps.setString(2, chapitre.getContenu());
            if (chapitreHasComplexite) {
                ps.setInt(3, chapitre.getComplexite());
                ps.setInt(4, chapitre.getId());
            } else {
                ps.setInt(3, chapitre.getId());
            }
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void supprimerChapitre(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id invalide");
        }
        String sql = "DELETE FROM Chapitre WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int nextOrdre(int courId) {
        String sql = "SELECT COALESCE(MAX(ordre), 0) + 1 FROM Chapitre WHERE cour_id = ?";
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
        return 1;
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
        return null;
    }

    private String normalize(String s) {
        if (s == null) {
            return "";
        }
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }
}
