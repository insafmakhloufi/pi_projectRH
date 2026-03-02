package Iservices.testQuiz;

import Entities.testQuiz.Question;

import java.util.List;

public interface IQuestionService {

    boolean addQuestion(Question question);

    List<Question> getAllQuestions();

    List<Question> getByTestId(int testID);

    boolean updateQuestion(Question question);

    boolean deleteQuestion(int id);

    boolean testExists(int testID);

    boolean questionExists(int questionID);
}
