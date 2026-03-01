package Services.messagerie;

import Entities.messagerie.Conversation;
import Iservices.messagerie.IConversationService;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationService implements IConversationService {

    private Connection con;

    public ConversationService() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public Conversation creerConversation(int candidatId, int managerId) {
        // Vérifier si une conversation existe déjà
        Conversation existing = getConversationEntreUsers(candidatId, managerId);
        if (existing != null) return existing;

        String sql = "INSERT INTO conversations (candidat_id, manager_id) VALUES (?, ?)";
        try {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, candidatId);
            ps.setInt(2, managerId);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                Conversation conv = new Conversation(candidatId, managerId);
                conv.setId(rs.getInt(1));
                return conv;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Conversation getConversationEntreUsers(int candidatId, int managerId) {
        String sql = "SELECT * FROM conversations WHERE candidat_id = ? AND manager_id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, candidatId);
            ps.setInt(2, managerId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Conversation conv = new Conversation();
                conv.setId(rs.getInt("id"));
                conv.setCandidatId(rs.getInt("candidat_id"));
                conv.setManagerId(rs.getInt("manager_id"));
                return conv;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Conversation> getConversationsParUser(int userId) {
        List<Conversation> liste = new ArrayList<>();
        String sql = "SELECT * FROM conversations WHERE candidat_id = ? OR manager_id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Conversation conv = new Conversation();
                conv.setId(rs.getInt("id"));
                conv.setCandidatId(rs.getInt("candidat_id"));
                conv.setManagerId(rs.getInt("manager_id"));
                liste.add(conv);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }
    @Override
    public List<Entities.User.User> rechercherUsers(String nom, int currentUserId) {
        List<Entities.User.User> liste = new ArrayList<>();
        String sql = "SELECT * FROM user WHERE full_name LIKE ? AND id != ? LIMIT 10";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%" + nom + "%");
            ps.setInt(2, currentUserId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Entities.User.User u = new Entities.User.User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("profile_photo"),
                        rs.getString("password"),
                        rs.getString("role")
                );
                liste.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return liste;
    }
    @Override
    public Entities.User.User getUserById(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Entities.User.User(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getString("profile_photo"),
                        rs.getString("password"),
                        rs.getString("role")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
