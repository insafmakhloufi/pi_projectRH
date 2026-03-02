package Utils.front;

import java.util.List;

public class FrontQuizQuestion {

    private final int id;
    private final String text;
    private final List<String> options;
    private final int correctIndex;
    private final String correctAnswer;
    private final boolean codingQuestion;
    private final String codingLanguage;
    private final int points;

    public FrontQuizQuestion(
            int id,
            String text,
            List<String> options,
            int correctIndex,
            String correctAnswer,
            boolean codingQuestion,
            String codingLanguage,
            int points
    ) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.correctIndex = correctIndex;
        this.correctAnswer = correctAnswer;
        this.codingQuestion = codingQuestion;
        this.codingLanguage = codingLanguage;
        this.points = points;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public boolean isOpenQuestion() {
        return !codingQuestion && correctAnswer != null;
    }

    public boolean isCodingQuestion() {
        return codingQuestion;
    }

    public String getCodingLanguage() {
        return codingLanguage;
    }

    public int getPoints() {
        return points;
    }
}
