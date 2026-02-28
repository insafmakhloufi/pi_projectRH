package Services.Formation;

import Entities.Formation.Formation;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PredictiveFormationAnalyzer {

    private final Connection con;
    private final FormationServices fs;

    public PredictiveFormationAnalyzer() {
        con = Mydatabase.getInstance().getConnection();
        fs = new FormationServices();
    }

    public double predictSuccessProbability(Formation formation) {
        if (formation == null) {
            return 0.0;
        }

        // Base score out of 100
        double score = 100.0;

        // Get number of courses
        int courseCount = getCourseCount(formation.getId());

        // Analyze complexity (based on course count)
        if (courseCount > 20) {
            score -= 25; // Too many chapters
        } else if (courseCount > 15) {
            score -= 15;
        } else if (courseCount < 3) {
            score -= 10; // Too few
        }

        // Additional factors could be added: domain complexity, etc.

        // Ensure score is between 0 and 100
        score = Math.max(0, Math.min(100, score));

        return score / 100.0; // Return as probability 0.0 to 1.0
    }

    public String getSuccessPrediction(Formation formation) {
        double prob = predictSuccessProbability(formation);
        if (prob >= 0.8) {
            return "Très probable de réussir";
        } else if (prob >= 0.6) {
            return "Probable de réussir";
        } else if (prob >= 0.4) {
            return "Risque modéré";
        } else if (prob >= 0.2) {
            return "Peu probable de réussir";
        } else {
            return "Très peu probable de réussir";
        }
    }

    public String getSuggestions(Formation formation) {
        double prob = predictSuccessProbability(formation);
        if (prob < 0.5) {
            StringBuilder suggestions = new StringBuilder("Suggestions pour améliorer: ");
            int courseCount = getCourseCount(formation.getId());

            if (courseCount > 15) {
                suggestions.append("Diminuer le nombre de chapitres. ");
            }
            if (courseCount < 5) {
                suggestions.append("Ajouter plus de chapitres. ");
            }
            return suggestions.toString().trim();
        }
        return "Aucune suggestion nécessaire.";
    }

    private int getCourseCount(int formationId) {
        String sql = "SELECT COUNT(*) FROM cour WHERE formation_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
