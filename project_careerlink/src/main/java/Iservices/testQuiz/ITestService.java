package Iservices.testQuiz;

import Entities.testQuiz.CandidatItem;
import Entities.testQuiz.Test;

import java.util.List;

public interface ITestService {

    boolean addTest(Test test);

    List<Test> getAllTests();

    List<Test> searchTests(String titre, String type, Integer candidatID);

    boolean updateTest(Test test);

    boolean deleteTest(int id);

    boolean candidatExists(int candidatID);

    boolean testExists(int testID);

    List<CandidatItem> getAllCandidats();

    boolean existsTitreForCandidat(int candidatID, String titre, int excludeId);
}
