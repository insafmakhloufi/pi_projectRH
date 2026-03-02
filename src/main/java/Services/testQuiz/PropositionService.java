package Services.testQuiz;

import Entities.testQuiz.Proposition;
import Iservices.testQuiz.IPropositionService;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PropositionService implements IPropositionService {

    private final Connection conn;

    public PropositionService() {
        this.conn = Mydatabase.getInstance().getConnection();
    }

    @Override
    public boolean addProposition(Proposition proposition) {
        if (!isValidProposition(proposition)) {
            System.out.println("[ERROR] Invalid proposition data");
            return false;
        }

        if (!questionExists(proposition.getQuestionID())) {
            System.out.println("[ERROR] questionID does not exist");
            return false;
        }

        String sql = """
                INSERT INTO proposition (questionID, contenu, estCorrect)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proposition.getQuestionID());
            ps.setString(2, proposition.getContenu());
            ps.setBoolean(3, proposition.isEstCorrect());

            ps.executeUpdate();
            System.out.println("[OK] Proposition added successfully");
            return true;
        } catch (SQLException e) {
            System.out.println("[ERROR] Error adding proposition");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<Proposition> getAllPropositions() {
        List<Proposition> propositions = new ArrayList<>();
        String sql = "SELECT * FROM proposition ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                propositions.add(extractProposition(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching propositions");
            e.printStackTrace();
        }

        return propositions;
    }

    @Override
    public List<Proposition> getByQuestionId(int questionID) {
        List<Proposition> propositions = new ArrayList<>();
        String sql = "SELECT * FROM proposition WHERE questionID = ? ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    propositions.add(extractProposition(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching propositions by questionID");
            e.printStackTrace();
        }

        return propositions;
    }

    @Override
    public boolean updateProposition(Proposition proposition) {
        if (proposition == null || proposition.getId() <= 0 || !isValidProposition(proposition)) {
            System.out.println("[ERROR] Invalid proposition data for update");
            return false;
        }

        if (!questionExists(proposition.getQuestionID())) {
            System.out.println("[ERROR] questionID does not exist");
            return false;
        }

        String sql = """
                UPDATE proposition
                SET questionID = ?, contenu = ?, estCorrect = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, proposition.getQuestionID());
            ps.setString(2, proposition.getContenu());
            ps.setBoolean(3, proposition.isEstCorrect());
            ps.setInt(4, proposition.getId());

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("[OK] Proposition updated successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error updating proposition");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deleteProposition(int id) {
        String sql = "DELETE FROM proposition WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[OK] Proposition deleted successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error deleting proposition");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean questionExists(int questionID) {
        String sql = "SELECT id FROM question WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking question existence");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean hasAtLeastOneCorrect(int questionID) {
        String sql = "SELECT id FROM proposition WHERE questionID = ? AND estCorrect = 1 LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking correct proposition existence");
            e.printStackTrace();
        }

        return false;
    }

    private boolean isValidProposition(Proposition proposition) {
        return proposition != null
                && proposition.getQuestionID() > 0
                && proposition.getContenu() != null
                && !proposition.getContenu().trim().isEmpty();
    }

    private Proposition extractProposition(ResultSet rs) throws SQLException {
        return new Proposition(
                rs.getInt("id"),
                rs.getInt("questionID"),
                rs.getString("contenu"),
                rs.getBoolean("estCorrect")
        );
    }
}
