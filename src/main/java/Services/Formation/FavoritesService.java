package Services.Formation;

import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

public class FavoritesService {

    private final Connection con;

    public FavoritesService() {
        con = Mydatabase.getInstance().getConnection();
    }

    public boolean isFavorite(int userId, int formationId) {
        String sql = "SELECT 1 FROM favorites WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addFavorite(int userId, int formationId) {
        String sql = "INSERT INTO favorites(user_id, formation_id, created_at) VALUES (?, ?, NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFavorite(int userId, int formationId) {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND formation_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean toggleFavorite(int userId, int formationId) {
        boolean current = isFavorite(userId, formationId);
        if (current) {
            removeFavorite(userId, formationId);
            return false;
        }
        addFavorite(userId, formationId);
        return true;
    }

    public Set<Integer> listFavoriteFormationIds(int userId) {
        String sql = "SELECT formation_id FROM favorites WHERE user_id = ?";
        Set<Integer> ids = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return ids;
    }
}
