package Entities.testQuiz;

public class Proposition {

    private int id;
    private int questionID;
    private String contenu;
    private boolean estCorrect;

    public Proposition() {
    }

    public Proposition(int questionID, String contenu, boolean estCorrect) {
        this.questionID = questionID;
        this.contenu = contenu;
        this.estCorrect = estCorrect;
    }

    public Proposition(int id, int questionID, String contenu, boolean estCorrect) {
        this.id = id;
        this.questionID = questionID;
        this.contenu = contenu;
        this.estCorrect = estCorrect;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getQuestionID() {
        return questionID;
    }

    public void setQuestionID(int questionID) {
        this.questionID = questionID;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public boolean isEstCorrect() {
        return estCorrect;
    }

    public void setEstCorrect(boolean estCorrect) {
        this.estCorrect = estCorrect;
    }
}
