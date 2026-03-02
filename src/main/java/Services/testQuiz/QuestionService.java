package Services.testQuiz;

import Entities.testQuiz.Question;
import Iservices.testQuiz.IQuestionService;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IQuestionService {

    private final Connection conn;

    public QuestionService() {
        this.conn = Mydatabase.getInstance().getConnection();
        ensureTypeColumnExists();
    }

    @Override
    public boolean addQuestion(Question question) {
        if (!isValidQuestion(question)) {
            System.out.println("[ERROR] Invalid question data");
            return false;
        }

        if (!testExists(question.getTestID())) {
            System.out.println("[ERROR] testID does not exist");
            return false;
        }

        String sql = """
                INSERT INTO question (testID, question, reponse, points, type)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, question.getTestID());
            ps.setString(2, question.getQuestion());
            ps.setString(3, question.getReponse());
            ps.setInt(4, question.getPoints());
            ps.setString(5, normalizeType(question.getType()));

            ps.executeUpdate();
            System.out.println("[OK] Question added successfully");
            return true;
        } catch (SQLException e) {
            System.out.println("[ERROR] Error adding question");
            e.printStackTrace();
        }

        return false;
    }

    public int addQuestionAndReturnId(Question question) {
        if (!isValidQuestion(question)) {
            System.out.println("[ERROR] Invalid question data");
            return -1;
        }

        if (!testExists(question.getTestID())) {
            System.out.println("[ERROR] testID does not exist");
            return -1;
        }

        String sql = """
                INSERT INTO question (testID, question, reponse, points, type)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, question.getTestID());
            ps.setString(2, question.getQuestion());
            ps.setString(3, question.getReponse());
            ps.setInt(4, question.getPoints());
            ps.setString(5, normalizeType(question.getType()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error adding question");
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public List<Question> getAllQuestions() {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM question ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                questions.add(extractQuestion(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching questions");
            e.printStackTrace();
        }

        return questions;
    }

    @Override
    public List<Question> getByTestId(int testID) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE testID = ? ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testID);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(extractQuestion(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching questions by testID");
            e.printStackTrace();
        }

        return questions;
    }

    @Override
    public boolean updateQuestion(Question question) {
        if (question == null || question.getId() <= 0 || !isValidQuestion(question)) {
            System.out.println("[ERROR] Invalid question data for update");
            return false;
        }

        if (!testExists(question.getTestID())) {
            System.out.println("[ERROR] testID does not exist");
            return false;
        }

        String sql = """
                UPDATE question
                SET testID = ?, question = ?, reponse = ?, points = ?, type = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, question.getTestID());
            ps.setString(2, question.getQuestion());
            ps.setString(3, question.getReponse());
            ps.setInt(4, question.getPoints());
            ps.setString(5, normalizeType(question.getType()));
            ps.setInt(6, question.getId());

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("[OK] Question updated successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error updating question");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deleteQuestion(int id) {
        String sql = "DELETE FROM question WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[OK] Question deleted successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error deleting question");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean testExists(int testID) {
        String sql = "SELECT id FROM test WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking test existence");
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

    private boolean isValidQuestion(Question question) {
        return question != null
                && question.getTestID() > 0
                && question.getQuestion() != null && !question.getQuestion().trim().isEmpty()
                && question.getPoints() >= 1;
    }

    private Question extractQuestion(ResultSet rs) throws SQLException {
        return new Question(
                rs.getInt("id"),
                rs.getInt("testID"),
                rs.getString("question"),
                rs.getString("reponse"),
                rs.getInt("points"),
                rs.getString("type")
        );
    }

    private String normalizeType(String type) {
        String t = type == null ? "" : type.trim().toUpperCase();
        if (t.equals("OPEN") || t.equals("CODING") || t.equals("QCM")) {
            return t;
        }
        return "QCM";
    }

    private void ensureTypeColumnExists() {
        String sql = "ALTER TABLE question ADD COLUMN type VARCHAR(20) DEFAULT 'QCM'";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Column may already exist.
        }
    }
}
