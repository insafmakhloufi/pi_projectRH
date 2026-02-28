package Services;

import Entities.Offre.Notification;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    
    private Connection con = Mydatabase.getInstance().getConnection();
    
    public void createNotification(Notification n) {
        String req = "INSERT INTO notification (user_id, type, titre, message, lu, date_creation, offre_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, n.getUserId());
            ps.setString(2, n.getType());
            ps.setString(3, n.getTitre());
            ps.setString(4, n.getMessage());
            ps.setBoolean(5, n.isLu());
            ps.setTimestamp(6, Timestamp.valueOf(n.getDateCreation()));
            if (n.getOffreId() != null) {
                ps.setInt(7, n.getOffreId());
            } else {
                ps.setNull(7, Types.INTEGER);
            }
            ps.executeUpdate();
            
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                n.setId(rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur création notification: " + e.getMessage(), e);
        }
    }
    
    public List<Notification> getNotificationsByUser(int userId) {
        List<Notification> list = new ArrayList<>();
        String req = "SELECT * FROM notification WHERE user_id = ? ORDER BY date_creation DESC";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération notifications: " + e.getMessage(), e);
        }
        return list;
    }
    
    public List<Notification> getUnreadNotifications(int userId) {
        List<Notification> list = new ArrayList<>();
        String req = "SELECT * FROM notification WHERE user_id = ? AND lu = false ORDER BY date_creation DESC";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération notifications non lues: " + e.getMessage(), e);
        }
        return list;
    }
    
    public int getUnreadCount(int userId) {
        String req = "SELECT COUNT(*) FROM notification WHERE user_id = ? AND lu = false";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur comptage notifications: " + e.getMessage(), e);
        }
        return 0;
    }
    
    public void markAsRead(int notificationId) {
        String req = "UPDATE notification SET lu = true WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur marquage notification lue: " + e.getMessage(), e);
        }
    }
    
    public void markAllAsRead(int userId) {
        String req = "UPDATE notification SET lu = true WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur marquage notifications lues: " + e.getMessage(), e);
        }
    }
    
    public void deleteNotification(int id) {
        String req = "DELETE FROM notification WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur suppression notification: " + e.getMessage(), e);
        }
    }
    
    public void notifyAllUsersForNewOffer(int offreId, String titreOffre) {
        String req = "SELECT id FROM user";
        try (PreparedStatement ps = con.prepareStatement(req);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int userId = rs.getInt("id");
                Notification n = new Notification();
                n.setUserId(userId);
                n.setType("NOUVELLE_OFFRE");
                n.setTitre("Nouvelle offre disponible !");
                n.setMessage("L'offre '" + titreOffre + "' vient d'être publiée. Postulez maintenant !");
                n.setOffreId(offreId);
                createNotification(n);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur notification nouvelle offre: " + e.getMessage(), e);
        }
    }
    
    private Notification mapResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setType(rs.getString("type"));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));
        n.setLu(rs.getBoolean("lu"));
        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            n.setDateCreation(ts.toLocalDateTime());
        }
        int offreId = rs.getInt("offre_id");
        if (!rs.wasNull()) {
            n.setOffreId(offreId);
        }
        return n;
    }
}
