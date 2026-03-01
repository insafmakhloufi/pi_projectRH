package Services.messagerie;

import Entities.messagerie.Notification;
import Iservices.messagerie.INotificationService;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationService implements INotificationService {

    private Connection con;

    public NotificationService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterNotification(int userId, String message, String type) {
        String sql = "INSERT INTO notifications (user_id, message, type) VALUES (?, ?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setString(2, message);
            ps.setString(3, type);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Notification> getNotificationsNonLues(int userId) {
        List<Notification> liste = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = false ORDER BY created_at DESC";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Notification notif = new Notification();
                notif.setId(rs.getInt("id"));
                notif.setUserId(rs.getInt("user_id"));
                notif.setMessage(rs.getString("message"));
                notif.setRead(rs.getBoolean("is_read"));
                notif.setType(rs.getString("type"));
                notif.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                liste.add(notif);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }

    @Override
    public int compterNotificationsNonLues(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = false";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void marquerCommeLue(int notificationId) {
        String sql = "UPDATE notifications SET is_read = true WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void marquerToutesCommeLues(int userId) {
        String sql = "UPDATE notifications SET is_read = true WHERE user_id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
