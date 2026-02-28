package Services.evenement;

import Utils.Mydatabase;

import java.sql.*;

public class ParticipationService {

    private final Connection conn;

    public ParticipationService() {
        this.conn = Mydatabase.getInstance().getConnection();
    }

    public boolean isAlreadyParticipating(int idUser, int idEvenement) {
        String sql = "SELECT COUNT(*) FROM participer_event WHERE id_user = ? AND id_evenement = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idEvenement);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int countParticipants(int idEvenement) {
        String sql = "SELECT COUNT(*) FROM participer_event WHERE id_evenement = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getCapaciteMax(int idEvenement) {
        String sql = "SELECT capacite_max FROM evenement WHERE id_evenement = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEvenement);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void participer(int idUser, int idEvenement, String ticketId) throws SQLException {
        String sql = "INSERT INTO participer_event (id_user, id_evenement, date_participation, ticket_id) VALUES (?, ?, NOW(), ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idEvenement);
            ps.setString(3, ticketId);
            ps.executeUpdate();
        }
    }
}
