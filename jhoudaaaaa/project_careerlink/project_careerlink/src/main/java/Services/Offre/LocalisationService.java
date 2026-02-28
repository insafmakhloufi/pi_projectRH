package Services.Offre;

import Entities.Offre.Localisation;
import Iservices.Offre.ILocalisation;
import Utils.Mydatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LocalisationService implements ILocalisation {
    private Connection connection;

    public LocalisationService() {
        this.connection = Mydatabase.getInstance().getConnection();
    }

    @Override
    public List<Localisation> getAll() {
        List<Localisation> localisations = new ArrayList<>();
        String sql = "SELECT * FROM Localisation";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                localisations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return localisations;
    }

    @Override
    public Localisation getById(int id) {
        String sql = "SELECT * FROM Localisation WHERE id_localisation = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void ajouter(Localisation localisation) {
        String sql = "INSERT INTO Localisation (ville, pays, adresse, latitude, longitude) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, localisation.getVille());
            pstmt.setString(2, localisation.getPays());
            pstmt.setString(3, localisation.getAdresse());
            if (localisation.getLatitude() != null) {
                pstmt.setDouble(4, localisation.getLatitude());
            } else {
                pstmt.setNull(4, Types.DOUBLE);
            }
            if (localisation.getLongitude() != null) {
                pstmt.setDouble(5, localisation.getLongitude());
            } else {
                pstmt.setNull(5, Types.DOUBLE);
            }
            pstmt.executeUpdate();
            System.out.println("Localisation ajoutée avec succès");
        } catch (SQLException e) {
            String fallbackSql = "INSERT INTO Localisation (ville, pays) VALUES (?, ?)";
            try (PreparedStatement ps2 = connection.prepareStatement(fallbackSql)) {
                ps2.setString(1, localisation.getVille());
                ps2.setString(2, localisation.getPays());
                ps2.executeUpdate();
                System.out.println("Localisation ajoutée avec succès");
            } catch (SQLException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    @Override
    public void modifier(Localisation localisation) {
        String sql = "UPDATE Localisation SET ville = ?, pays = ?, adresse = ?, latitude = ?, longitude = ? WHERE id_localisation = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, localisation.getVille());
            pstmt.setString(2, localisation.getPays());
            pstmt.setString(3, localisation.getAdresse());
            if (localisation.getLatitude() != null) {
                pstmt.setDouble(4, localisation.getLatitude());
            } else {
                pstmt.setNull(4, Types.DOUBLE);
            }
            if (localisation.getLongitude() != null) {
                pstmt.setDouble(5, localisation.getLongitude());
            } else {
                pstmt.setNull(5, Types.DOUBLE);
            }
            pstmt.setInt(6, localisation.getId());
            pstmt.executeUpdate();
            System.out.println("Localisation modifiée avec succès");
        } catch (SQLException e) {
            String fallbackSql = "UPDATE Localisation SET ville = ?, pays = ? WHERE id_localisation = ?";
            try (PreparedStatement ps2 = connection.prepareStatement(fallbackSql)) {
                ps2.setString(1, localisation.getVille());
                ps2.setString(2, localisation.getPays());
                ps2.setInt(3, localisation.getId());
                ps2.executeUpdate();
                System.out.println("Localisation modifiée avec succès");
            } catch (SQLException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM Localisation WHERE id_localisation = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Localisation supprimée avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Localisation> getByVille(String ville) {
        List<Localisation> localisations = new ArrayList<>();
        String sql = "SELECT * FROM Localisation WHERE ville LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + ville + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                localisations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return localisations;
    }

    @Override
    public List<Localisation> getByPays(String pays) {
        List<Localisation> localisations = new ArrayList<>();
        String sql = "SELECT * FROM Localisation WHERE pays LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + pays + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                localisations.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return localisations;
    }

    public Localisation findOrCreateByCoordinates(String adresse, String ville, String pays, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        Localisation existing = getByCoordinates(latitude, longitude);
        if (existing != null) {
            boolean changed = false;
            if (adresse != null && !adresse.isBlank() && (existing.getAdresse() == null || existing.getAdresse().isBlank())) {
                existing.setAdresse(adresse);
                changed = true;
            }
            if (ville != null && !ville.isBlank() && (existing.getVille() == null || existing.getVille().isBlank())) {
                existing.setVille(ville);
                changed = true;
            }
            if (pays != null && !pays.isBlank() && (existing.getPays() == null || existing.getPays().isBlank())) {
                existing.setPays(pays);
                changed = true;
            }
            if (changed) {
                try {
                    modifier(existing);
                } catch (Exception ignored) {
                }
            }
            return existing;
        }

        Localisation l = new Localisation();
        l.setAdresse(adresse);
        l.setVille(ville);
        l.setPays(pays);
        l.setLatitude(latitude);
        l.setLongitude(longitude);
        ajouter(l);

        Localisation inserted = getByCoordinates(latitude, longitude);
        return inserted != null ? inserted : l;
    }

    private Localisation getByCoordinates(double latitude, double longitude) {
        String sql = "SELECT * FROM Localisation WHERE latitude IS NOT NULL AND longitude IS NOT NULL AND ABS(latitude - ?) < 0.000001 AND ABS(longitude - ?) < 0.000001 LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setDouble(1, latitude);
            ps.setDouble(2, longitude);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            return null;
        }
        return null;
    }

    private Localisation mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id_localisation");
        String ville = safeGetString(rs, "ville");
        String pays = safeGetString(rs, "pays");
        String adresse = safeGetString(rs, "adresse");
        Double lat = safeGetDouble(rs, "latitude");
        Double lng = safeGetDouble(rs, "longitude");
        return new Localisation(id, ville, pays, adresse, lat, lng);
    }

    private String safeGetString(ResultSet rs, String col) {
        try {
            return rs.getString(col);
        } catch (SQLException e) {
            return null;
        }
    }

    private Double safeGetDouble(ResultSet rs, String col) {
        try {
            Object o = rs.getObject(col);
            return o == null ? null : rs.getDouble(col);
        } catch (SQLException e) {
            return null;
        }
    }
}