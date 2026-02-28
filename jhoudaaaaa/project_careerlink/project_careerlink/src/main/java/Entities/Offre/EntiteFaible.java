package Entities.Offre;

public abstract class EntiteFaible {
    protected int id;
    protected String nom;

    // Constructeurs
    public EntiteFaible() {}

    public EntiteFaible(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    // Méthode abstraite
    public abstract String getType();

    @Override
    public String toString() {
        return nom;
    }
}