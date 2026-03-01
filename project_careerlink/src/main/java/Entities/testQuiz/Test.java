package Entities.testQuiz;

public class Test {

    private int id;
    private int candidatID;
    private String titre;
    private String type;
    private double scoreMax;
    private int durationSeconds;
    private int totalPoints;

    public Test() {
    }

    public Test(int candidatID, String titre, String type, double scoreMax, int durationSeconds, int totalPoints) {
        this.candidatID = candidatID;
        this.titre = titre;
        this.type = type;
        this.scoreMax = scoreMax;
        this.durationSeconds = durationSeconds;
        this.totalPoints = totalPoints;
    }

    public Test(int id, int candidatID, String titre, String type, double scoreMax, int durationSeconds, int totalPoints) {
        this.id = id;
        this.candidatID = candidatID;
        this.titre = titre;
        this.type = type;
        this.scoreMax = scoreMax;
        this.durationSeconds = durationSeconds;
        this.totalPoints = totalPoints;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCandidatID() {
        return candidatID;
    }

    public void setCandidatID(int candidatID) {
        this.candidatID = candidatID;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getScoreMax() {
        return scoreMax;
    }

    public void setScoreMax(double scoreMax) {
        this.scoreMax = scoreMax;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(int totalPoints) {
        this.totalPoints = totalPoints;
    }
}
