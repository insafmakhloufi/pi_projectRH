package Services.testQuiz;

import Entities.testQuiz.CandidatItem;
import Entities.testQuiz.Proposition;
import Entities.testQuiz.Question;
import Entities.testQuiz.Test;
import Iservices.testQuiz.ITestService;
import Services.testQuiz.AiTestGeneratorService.GeneratedProposition;
import Services.testQuiz.AiTestGeneratorService.GeneratedQuestion;
import Services.testQuiz.AiTestGeneratorService.GeneratedTestPayload;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TestService implements ITestService {

    private final Connection conn;

    public TestService() {
        this.conn = Mydatabase.getInstance().getConnection();
        ensureQuestionTypeColumnExists(this.conn);
        ensureTestVisibleColumnExists(this.conn);
    }

    @Override
    public boolean addTest(Test test) {
        if (!isValidTest(test)) {
            System.out.println("[ERROR] Invalid test data");
            return false;
        }

        if (!candidatExists(test.getCandidatID())) {
            System.out.println("[ERROR] candidatID does not exist in user table");
            return false;
        }
        if (existsTitreForCandidat(test.getCandidatID(), test.getTitre(), 0)) {
            System.out.println("[ERROR] Duplicate titre for same candidat");
            return false;
        }

        String sql = """
                INSERT INTO test (candidatID, titre, type, scoreMax, durationSeconds, totalPoints, visible)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, test.getCandidatID());
            ps.setString(2, test.getTitre());
            ps.setString(3, test.getType());
            ps.setDouble(4, test.getScoreMax());
            ps.setInt(5, test.getDurationSeconds());
            ps.setInt(6, test.getTotalPoints());
            ps.setBoolean(7, test.isVisible());

            ps.executeUpdate();
            System.out.println("[OK] Test added successfully");
            return true;
        } catch (SQLException e) {
            System.out.println("[ERROR] Error adding test");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public List<Test> getAllTests() {
        List<Test> tests = new ArrayList<>();
        String sql = "SELECT * FROM test ORDER BY id DESC";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tests.add(extractTest(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching tests");
            e.printStackTrace();
        }

        return tests;
    }

    @Override
    public List<Test> searchTests(String titre, String type, Integer candidatID) {
        List<Test> tests = new ArrayList<>();

        String sql = """
                SELECT * FROM test
                WHERE (? IS NULL OR LOWER(titre) LIKE LOWER(?))
                  AND (? IS NULL OR LOWER(type) LIKE LOWER(?))
                  AND (? IS NULL OR candidatID = ?)
                ORDER BY id DESC
                """;

        String titreLike = (titre == null || titre.isBlank()) ? null : "%" + titre.trim() + "%";
        String typeLike = (type == null || type.isBlank()) ? null : "%" + type.trim() + "%";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, titreLike);
            ps.setString(2, titreLike);
            ps.setString(3, typeLike);
            ps.setString(4, typeLike);

            if (candidatID == null) {
                ps.setObject(5, null);
                ps.setObject(6, null);
            } else {
                ps.setInt(5, candidatID);
                ps.setInt(6, candidatID);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tests.add(extractTest(rs));
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error searching tests");
            e.printStackTrace();
        }

        return tests;
    }

    @Override
    public boolean updateTest(Test test) {
        if (test == null || test.getId() <= 0 || !isValidTest(test)) {
            System.out.println("[ERROR] Invalid test data for update");
            return false;
        }

        if (!candidatExists(test.getCandidatID())) {
            System.out.println("[ERROR] candidatID does not exist in user table");
            return false;
        }
        if (existsTitreForCandidat(test.getCandidatID(), test.getTitre(), test.getId())) {
            System.out.println("[ERROR] Duplicate titre for same candidat");
            return false;
        }

        String sql = """
                UPDATE test
                SET candidatID = ?, titre = ?, type = ?, scoreMax = ?, durationSeconds = ?, totalPoints = ?, visible = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, test.getCandidatID());
            ps.setString(2, test.getTitre());
            ps.setString(3, test.getType());
            ps.setDouble(4, test.getScoreMax());
            ps.setInt(5, test.getDurationSeconds());
            ps.setInt(6, test.getTotalPoints());
            ps.setBoolean(7, test.isVisible());
            ps.setInt(8, test.getId());

            int updated = ps.executeUpdate();
            if (updated > 0) {
                System.out.println("[OK] Test updated successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error updating test");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean deleteTest(int id) {
        String sql = "DELETE FROM test WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                System.out.println("[OK] Test deleted successfully");
                return true;
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error deleting test");
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean candidatExists(int candidatID) {
        String sql = "SELECT id FROM user WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidatID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking candidatID");
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
    public List<CandidatItem> getAllCandidats() {
        List<CandidatItem> list = new ArrayList<>();

        String sqlNomPrenom = "SELECT id, nom, prenom FROM user WHERE role = 'CANDIDAT' ORDER BY nom, prenom";
        String sqlFullName = "SELECT id, full_name FROM user WHERE role = 'CANDIDAT' ORDER BY full_name";

        try (PreparedStatement ps = conn.prepareStatement(sqlNomPrenom);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                String full = ((nom == null ? "" : nom.trim()) + " " + (prenom == null ? "" : prenom.trim())).trim();
                if (full.isEmpty()) full = "Candidat #" + rs.getInt("id");
                list.add(new CandidatItem(rs.getInt("id"), full));
            }
            return list;
        } catch (SQLException ignored) {
            // fallback full_name for existing user module schema
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlFullName);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String full = rs.getString("full_name");
                if (full == null || full.isBlank()) full = "Candidat #" + rs.getInt("id");
                list.add(new CandidatItem(rs.getInt("id"), full.trim()));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching candidats");
            e.printStackTrace();
        }

        return list;
    }

    public boolean toggleVisible(int testId) {
        String sql = "UPDATE test SET visible = NOT visible WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, testId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] Error toggling visible");
            e.printStackTrace();
            return false;
        }
    }

    public boolean setVisible(int testId, boolean visible) {
        String sql = "UPDATE test SET visible = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, visible);
            ps.setInt(2, testId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Test> getTestsForCandidat(int candidatId) {
        List<Test> tests = new ArrayList<>();
        String sql = """
                SELECT * FROM test
                WHERE candidatID = ?
                  AND visible = 1
                ORDER BY id DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tests.add(extractTest(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching tests for candidat");
            e.printStackTrace();
        }
        return tests;
    }

    @Override
    public boolean existsTitreForCandidat(int candidatID, String titre, int excludeId) {
        String sql = """
                SELECT COUNT(*) FROM test
                WHERE candidatID = ?
                  AND LOWER(titre) = LOWER(?)
                  AND id <> ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, candidatID);
            ps.setString(2, titre == null ? "" : titre.trim());
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error checking unique titre+candidat");
            e.printStackTrace();
        }
        return false;
    }

    public boolean insertGeneratedTest(int candidatId, GeneratedTestPayload payload) {
        if (payload == null || payload.test() == null || payload.questions() == null || payload.questions().isEmpty()) {
            return false;
        }

        Connection tx = null;
        try {
            tx = Mydatabase.getInstance().getConnection();
            tx.setAutoCommit(false);
            ensureQuestionTypeColumnExists(tx);

            if (!candidatExistsInConnection(tx, candidatId)) {
                tx.rollback();
                return false;
            }

            String uniqueTitle = resolveUniqueTitle(tx, candidatId, payload.test().titre());

            int testId = insertTestTx(tx, candidatId, uniqueTitle, payload);
            if (testId <= 0) {
                tx.rollback();
                return false;
            }

            boolean isQcm = payload.test().type() != null && payload.test().type().trim().equalsIgnoreCase("QCM");
            for (GeneratedQuestion generatedQuestion : payload.questions()) {
                Question question = new Question(
                        testId,
                        generatedQuestion.question(),
                        isQcm ? null : generatedQuestion.reponse(),
                        generatedQuestion.points(),
                        payload.test().type()
                );
                int questionId = insertQuestionTx(tx, question);
                if (questionId <= 0) {
                    tx.rollback();
                    return false;
                }

                if (isQcm) {
                    List<GeneratedProposition> propositions = generatedQuestion.propositions();
                    if (propositions == null || propositions.isEmpty()) {
                        tx.rollback();
                        return false;
                    }
                    for (GeneratedProposition generatedProposition : propositions) {
                        Proposition proposition = new Proposition(
                                questionId,
                                generatedProposition.contenu(),
                                generatedProposition.estCorrect()
                        );
                        if (!insertPropositionTx(tx, proposition)) {
                            tx.rollback();
                            return false;
                        }
                    }
                }
            }

            tx.commit();
            return true;
        } catch (SQLException e) {
            if (tx != null) {
                try {
                    tx.rollback();
                } catch (SQLException ignored) {
                }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (tx != null) {
                try {
                    tx.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private boolean candidatExistsInConnection(Connection connection, int candidatID) throws SQLException {
        String sql = "SELECT id FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private int insertTestTx(Connection connection, int candidatId, String title, GeneratedTestPayload payload) throws SQLException {
        String sql = """
                INSERT INTO test (candidatID, titre, type, scoreMax, durationSeconds, totalPoints)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, candidatId);
            ps.setString(2, title);
            ps.setString(3, payload.test().type());
            ps.setDouble(4, payload.test().scoreMax());
            ps.setInt(5, payload.test().durationSeconds());
            ps.setInt(6, payload.test().totalPoints());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private int insertQuestionTx(Connection connection, Question question) throws SQLException {
        String sql = """
                INSERT INTO question (testID, question, reponse, points, type)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, question.getTestID());
            ps.setString(2, question.getQuestion());
            ps.setString(3, question.getReponse());
            ps.setInt(4, question.getPoints());
            ps.setString(5, question.getType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    private boolean insertPropositionTx(Connection connection, Proposition proposition) throws SQLException {
        String sql = """
                INSERT INTO proposition (questionID, contenu, estCorrect)
                VALUES (?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, proposition.getQuestionID());
            ps.setString(2, proposition.getContenu());
            ps.setBoolean(3, proposition.isEstCorrect());
            return ps.executeUpdate() > 0;
        }
    }

    private String resolveUniqueTitle(Connection connection, int candidatId, String rawTitle) throws SQLException {
        String base = rawTitle == null ? "Test IA" : rawTitle.trim();
        if (base.isBlank()) base = "Test IA";

        String candidate = base;
        int suffix = 1;
        while (existsTitreForCandidatTx(connection, candidatId, candidate)) {
            suffix++;
            candidate = base + " (" + suffix + ")";
        }
        return candidate;
    }

    private boolean existsTitreForCandidatTx(Connection connection, int candidatId, String titre) throws SQLException {
        String sql = """
                SELECT COUNT(*) FROM test
                WHERE candidatID = ?
                  AND LOWER(titre) = LOWER(?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidatId);
            ps.setString(2, titre == null ? "" : titre.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean isValidTest(Test test) {
        return test != null
                && test.getCandidatID() > 0
                && test.getTitre() != null && !test.getTitre().trim().isEmpty()
                && test.getType() != null && !test.getType().trim().isEmpty()
                && test.getScoreMax() >= 0
                && test.getDurationSeconds() >= 0
                && test.getTotalPoints() >= 0;
    }

    private Test extractTest(ResultSet rs) throws SQLException {
        Test test = new Test(
                rs.getInt("id"),
                rs.getInt("candidatID"),
                rs.getString("titre"),
                rs.getString("type"),
                rs.getDouble("scoreMax"),
                rs.getInt("durationSeconds"),
                rs.getInt("totalPoints")
        );
        try {
            test.setVisible(rs.getBoolean("visible"));
        } catch (SQLException ignored) {
            test.setVisible(false);
        }
        return test;
    }

    private void ensureQuestionTypeColumnExists(Connection connection) {
        String sql = "ALTER TABLE question ADD COLUMN type VARCHAR(20) DEFAULT 'QCM'";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Column may already exist.
        }
    }

    private void ensureTestVisibleColumnExists(Connection connection) {
        String sql = "ALTER TABLE test ADD COLUMN IF NOT EXISTS visible tinyint(1) NOT NULL DEFAULT 0";
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
            // Column may already exist or be unsupported depending on DB engine version.
        }
    }
}
