package Entities.testQuiz;

public class CandidatItem {

    private final int id;
    private final String fullName;

    public CandidatItem(int id, String fullName) {
        this.id = id;
        this.fullName = fullName;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
