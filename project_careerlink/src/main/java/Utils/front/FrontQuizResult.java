package Utils.front;

import Entities.testQuiz.Test;

public class FrontQuizResult {

    private final Test test;
    private final int score;
    private final int maxScore;
    private final int correct;
    private final int wrong;
    private final int unanswered;
    private final int timeSpentSeconds;

    public FrontQuizResult(
            Test test,
            int score,
            int maxScore,
            int correct,
            int wrong,
            int unanswered,
            int timeSpentSeconds
    ) {
        this.test = test;
        this.score = score;
        this.maxScore = maxScore;
        this.correct = correct;
        this.wrong = wrong;
        this.unanswered = unanswered;
        this.timeSpentSeconds = timeSpentSeconds;
    }

    public Test getTest() {
        return test;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public int getCorrect() {
        return correct;
    }

    public int getWrong() {
        return wrong;
    }

    public int getUnanswered() {
        return unanswered;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    public int percent() {
        if (maxScore <= 0) return 0;
        return (int) Math.round((score * 100.0) / maxScore);
    }

    public String gradeMessage() {
        int p = percent();
        if (p >= 80) return "Excellent";
        if (p >= 60) return "Good";
        return "Try again";
    }
}
