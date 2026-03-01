package Utils.front;

import Entities.testQuiz.Proposition;
import Entities.testQuiz.Question;
import Entities.testQuiz.Test;
import Services.testQuiz.PropositionService;
import Services.testQuiz.QuestionService;
import Services.testQuiz.TestService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FrontTestQuizAdapter {

    private final TestService testService = new TestService();
    private final QuestionService questionService = new QuestionService();
    private final PropositionService propositionService = new PropositionService();

    public List<Test> loadTests() {
        return testService.getAllTests();
    }

    public List<FrontQuizQuestion> loadQuizQuestions(Test test) {
        List<Question> questions = questionService.getByTestId(test.getId());
        questions.sort(Comparator.comparingInt(Question::getId));
        List<FrontQuizQuestion> out = new ArrayList<>();

        int fallbackPoints = computeFallbackPoints(test, questions.size());
        for (Question q : questions) {
            List<Proposition> propositions = propositionService.getByQuestionId(q.getId());
            if (propositions != null) {
                propositions.sort(Comparator.comparingInt(Proposition::getId));
            }
            out.add(toFrontQuestion(test, q, propositions, fallbackPoints));
        }
        return out;
    }

    public int inferDurationSeconds(Test test) {
        int d = test.getDurationSeconds();
        if (d > 0) return d;
        return 10 * 60;
    }

    private FrontQuizQuestion toFrontQuestion(Test test, Question q, List<Proposition> propositions, int fallbackPoints) {
        List<String> options = new ArrayList<>();
        int correctIndex = -1;
        String correctAnswer = null;
        boolean isCodingQuestion = "CODING".equalsIgnoreCase(safe(q.getType()));
        String codingLanguage = isCodingQuestion ? deduceCodingLanguage(test, q) : null;

        if (isCodingQuestion) {
            options = List.of();
            correctIndex = -1;
            correctAnswer = safe(q.getReponse());
        } else if (propositions != null && !propositions.isEmpty()) {
            for (Proposition proposition : propositions) {
                if (options.size() >= 4) break;
                options.add(safe(proposition.getContenu()));
                if (proposition.isEstCorrect()) {
                    correctIndex = options.size() - 1;
                }
            }
            while (options.size() < 4) {
                options.add("Option " + (options.size() + 1));
            }
            if (correctIndex < 0 || correctIndex >= 4) {
                correctIndex = 0;
            }
        } else {
            options = List.of();
            correctIndex = -1;
            String expected = safe(q.getReponse());
            correctAnswer = expected.isBlank() ? "" : expected;
        }

        int points = q.getPoints() > 0 ? q.getPoints() : fallbackPoints;
        return new FrontQuizQuestion(
                q.getId(),
                safe(q.getQuestion()),
                options,
                correctIndex,
                correctAnswer,
                isCodingQuestion,
                codingLanguage,
                points
        );
    }

    private int computeFallbackPoints(Test test, int count) {
        int n = Math.max(1, count);
        int total = test.getTotalPoints() > 0 ? test.getTotalPoints() : 10;
        return Math.max(1, (int) Math.ceil(total / (double) n));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String deduceCodingLanguage(Test test, Question q) {
        String text = (safe(test.getTitre()) + " " + safe(test.getType()) + " " + safe(q.getQuestion())).toLowerCase();
        if (text.contains("python")) return "python";
        if (text.contains("javascript") || text.contains("js")) return "javascript";
        if (text.contains("typescript") || text.contains("ts")) return "typescript";
        if (text.contains("c++") || text.contains("cpp")) return "cpp";
        if (text.contains("c#") || text.contains("csharp")) return "csharp";
        if (text.contains("php")) return "php";
        return "java";
    }
}
