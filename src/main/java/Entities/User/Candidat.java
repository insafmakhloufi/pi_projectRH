package Entities;

public class Candidat extends User {
    public Candidat(int id, String fullName, String email, String phone, String profilePhoto, String password) {
        super(id, fullName, email, phone, profilePhoto, password, "CANDIDAT");
    }
}
