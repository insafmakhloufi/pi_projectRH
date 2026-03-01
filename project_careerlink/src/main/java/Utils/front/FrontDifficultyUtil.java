package Utils.front;

import Entities.testQuiz.Test;

public final class FrontDifficultyUtil {

    private FrontDifficultyUtil() {
    }

    public static String difficultyOf(Test test) {
        int points = Math.max(0, test.getTotalPoints());
        int durationMinutes = Math.max(0, test.getDurationSeconds()) / 60;
        int score = points + durationMinutes;
        if (score >= 22) return "Difficile";
        if (score >= 11) return "Moyen";
        return "Facile";
    }
}
