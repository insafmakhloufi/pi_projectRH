package Entities.testQuiz;

public class Question {

    private int id;
    private int testID;
    private String question;
    private String reponse;
    private int points;
    private String type = "QCM";

    public Question() {
    }

    public Question(int testID, String question, String reponse, int points) {
        this.testID = testID;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.type = "QCM";
    }

    public Question(int testID, String question, String reponse, int points, String type) {
        this.testID = testID;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.type = type;
    }

    public Question(int id, int testID, String question, String reponse, int points) {
        this.id = id;
        this.testID = testID;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.type = "QCM";
    }

    public Question(int id, int testID, String question, String reponse, int points, String type) {
        this.id = id;
        this.testID = testID;
        this.question = question;
        this.reponse = reponse;
        this.points = points;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTestID() {
        return testID;
    }

    public void setTestID(int testID) {
        this.testID = testID;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getType() {
        return type == null || type.isBlank() ? "QCM" : type;
    }

    public void setType(String type) {
        this.type = (type == null || type.isBlank()) ? "QCM" : type;
    }
}
