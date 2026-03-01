package Services.testQuiz;

public class AiCodeResult {

    private final boolean correct;
    private final int score;
    private final String feedback;

    public AiCodeResult(boolean correct, int score, String feedback) {
        this.correct = correct;
        this.score = score;
        this.feedback = feedback == null ? "" : feedback;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getScore() {
        return score;
    }

    public String getFeedback() {
        return feedback;
    }
}
