package Iservices.testQuiz;

import Entities.testQuiz.Proposition;

import java.util.List;

public interface IPropositionService {

    boolean addProposition(Proposition proposition);

    List<Proposition> getAllPropositions();

    List<Proposition> getByQuestionId(int questionID);

    boolean updateProposition(Proposition proposition);

    boolean deleteProposition(int id);

    boolean questionExists(int questionID);

    boolean hasAtLeastOneCorrect(int questionID);
}
