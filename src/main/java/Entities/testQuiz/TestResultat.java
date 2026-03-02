package Entities.testQuiz;

import java.time.LocalDateTime;

public class TestResultat {

    private int id;
    private int testId;
    private int candidatId;
    private int score;
    private int maxScore;
    private int correct;
    private int wrong;
    private int unanswered;
    private int timeSpentSec;
    private int percent;
    private String mention;
    private LocalDateTime datePassage;

    // transient display fields
    private transient String testTitre;
    private transient String candidatNom;

    public TestResultat() {
    }

    public TestResultat(int id, int testId, int candidatId, int score, int maxScore, int correct, int wrong,
                        int unanswered, int timeSpentSec, int percent, String mention, LocalDateTime datePassage) {
        this.id = id;
        this.testId = testId;
        this.candidatId = candidatId;
        this.score = score;
        this.maxScore = maxScore;
        this.correct = correct;
        this.wrong = wrong;
        this.unanswered = unanswered;
        this.timeSpentSec = timeSpentSec;
        this.percent = percent;
        this.mention = mention;
        this.datePassage = datePassage;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTestId() {
        return testId;
    }

    public void setTestId(int testId) {
        this.testId = testId;
    }

    public int getCandidatId() {
        return candidatId;
    }

    public void setCandidatId(int candidatId) {
        this.candidatId = candidatId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public int getCorrect() {
        return correct;
    }

    public void setCorrect(int correct) {
        this.correct = correct;
    }

    public int getWrong() {
        return wrong;
    }

    public void setWrong(int wrong) {
        this.wrong = wrong;
    }

    public int getUnanswered() {
        return unanswered;
    }

    public void setUnanswered(int unanswered) {
        this.unanswered = unanswered;
    }

    public int getTimeSpentSec() {
        return timeSpentSec;
    }

    public void setTimeSpentSec(int timeSpentSec) {
        this.timeSpentSec = timeSpentSec;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public String getMention() {
        return mention;
    }

    public void setMention(String mention) {
        this.mention = mention;
    }

    public LocalDateTime getDatePassage() {
        return datePassage;
    }

    public void setDatePassage(LocalDateTime datePassage) {
        this.datePassage = datePassage;
    }

    public String getTestTitre() {
        return testTitre;
    }

    public void setTestTitre(String testTitre) {
        this.testTitre = testTitre;
    }

    public String getCandidatNom() {
        return candidatNom;
    }

    public void setCandidatNom(String candidatNom) {
        this.candidatNom = candidatNom;
    }
}
