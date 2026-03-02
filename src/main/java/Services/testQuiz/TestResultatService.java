package Services.testQuiz;

import Entities.testQuiz.TestResultat;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestResultatService {

    private final Connection conn;

    public TestResultatService() {
        this.conn = Mydatabase.getInstance().getConnection();
        ensureTableExists();
    }

    public boolean saveResultat(TestResultat r) {
        String sql = """
                INSERT INTO test_resultat (test_id, candidat_id, score, max_score,
                  correct, wrong, unanswered, time_spent_sec, percent, mention, date_passage)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, r.getTestId());
            ps.setInt(2, r.getCandidatId());
            ps.setInt(3, r.getScore());
            ps.setInt(4, r.getMaxScore());
            ps.setInt(5, r.getCorrect());
            ps.setInt(6, r.getWrong());
            ps.setInt(7, r.getUnanswered());
            ps.setInt(8, r.getTimeSpentSec());
            ps.setInt(9, r.getPercent());
            ps.setString(10, r.getMention());
            LocalDateTime date = r.getDatePassage() == null ? LocalDateTime.now() : r.getDatePassage();
            ps.setTimestamp(11, Timestamp.valueOf(date));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("[ERROR] Error saving test_resultat");
            e.printStackTrace();
            return false;
        }
    }

    public List<TestResultat> getAllResultats() {
        List<TestResultat> list = new ArrayList<>();
        String sql = """
                SELECT tr.*,
                       t.titre as test_titre,
                       COALESCE(
                           NULLIF(TRIM(CONCAT(
                               COALESCE(u.full_name, ''),
                               ' '
                           )), ''),
                           CONCAT('Candidat #', tr.candidat_id)
                       ) as candidat_nom
                FROM test_resultat tr
                LEFT JOIN test t ON tr.test_id = t.id
                LEFT JOIN user u ON tr.candidat_id = u.id
                ORDER BY tr.date_passage DESC
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TestResultat r = new TestResultat();
                r.setId(rs.getInt("id"));
                r.setTestId(rs.getInt("test_id"));
                r.setCandidatId(rs.getInt("candidat_id"));
                r.setScore(rs.getInt("score"));
                r.setMaxScore(rs.getInt("max_score"));
                r.setCorrect(rs.getInt("correct"));
                r.setWrong(rs.getInt("wrong"));
                r.setUnanswered(rs.getInt("unanswered"));
                r.setTimeSpentSec(rs.getInt("time_spent_sec"));
                r.setPercent(rs.getInt("percent"));
                r.setMention(rs.getString("mention"));
                Timestamp ts = rs.getTimestamp("date_passage");
                r.setDatePassage(ts == null ? null : ts.toLocalDateTime());
                r.setTestTitre(rs.getString("test_titre"));
                String candidatNom = rs.getString("candidat_nom");
                r.setCandidatNom(candidatNom == null ? "" : candidatNom.trim());
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching test_resultat");
            e.printStackTrace();
        }
        return list;
    }

    private void ensureTableExists() {
        String sql = """
                CREATE TABLE IF NOT EXISTS test_resultat (
                  id int(11) NOT NULL AUTO_INCREMENT,
                  test_id int(11) NOT NULL,
                  candidat_id int(11) NOT NULL,
                  score int(11) NOT NULL DEFAULT 0,
                  max_score int(11) NOT NULL DEFAULT 0,
                  correct int(11) NOT NULL DEFAULT 0,
                  wrong int(11) NOT NULL DEFAULT 0,
                  unanswered int(11) NOT NULL DEFAULT 0,
                  time_spent_sec int(11) NOT NULL DEFAULT 0,
                  percent int(11) NOT NULL DEFAULT 0,
                  mention varchar(50) DEFAULT NULL,
                  date_passage datetime NOT NULL DEFAULT current_timestamp(),
                  PRIMARY KEY (id),
                  KEY fk_resultat_test (test_id),
                  KEY fk_resultat_candidat (candidat_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """;
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            System.out.println("[WARN] Unable to ensure test_resultat table");
            e.printStackTrace();
        }
    }

    private String resolveCandidatDisplayName(ResultSet rs) throws SQLException {
        String nom = rs.getString("nom");
        String prenom = rs.getString("prenom");
        String full = ((nom == null ? "" : nom.trim()) + " " + (prenom == null ? "" : prenom.trim())).trim();
        if (!full.isEmpty()) return full;
        String fullName = rs.getString("full_name");
        if (fullName != null && !fullName.isBlank()) return fullName.trim();
        return "Candidat #" + rs.getInt("candidat_id");
    }
}
