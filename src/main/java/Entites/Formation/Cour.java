package Entites.Formation;

public class Cour {
    private int id;
    private Integer formationId;
    private String nomFormateur;
    private int complexite;
    private String description;
    private String origine;
    private float dure;
    private int nbChapitres;

    public Cour() {
    }

    public Cour(int id, String nomFormateur, int complexite, String description, String origine, float dure) {
        this.id = id;
        this.nomFormateur = nomFormateur;
        this.complexite = complexite;
        this.description = description;
        this.origine = origine;
        this.dure = dure;
        this.nbChapitres = 1;
    }

    public Cour(int id, Integer formationId, String nomFormateur, int complexite, String description, String origine, float dure) {
        this.id = id;
        this.formationId = formationId;
        this.nomFormateur = nomFormateur;
        this.complexite = complexite;
        this.description = description;
        this.origine = origine;
        this.dure = dure;
        this.nbChapitres = 1;
    }

    public Cour(String nomFormateur, int complexite, String description, String origine, float dure) {
        this.nomFormateur = nomFormateur;
        this.complexite = complexite;
        this.description = description;
        this.origine = origine;
        this.dure = dure;
        this.nbChapitres = 1;
    }

    public Cour(String nomFormateur, int complexite, String description, String origine, float dure, int nbChapitres) {
        this.nomFormateur = nomFormateur;
        this.complexite = complexite;
        this.description = description;
        this.origine = origine;
        this.dure = dure;
        this.nbChapitres = nbChapitres;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getFormationId() {
        return formationId;
    }

    public void setFormationId(Integer formationId) {
        this.formationId = formationId;
    }

    public String getNomFormateur() {
        return nomFormateur;
    }

    public void setNomFormateur(String nomFormateur) {
        this.nomFormateur = nomFormateur;
    }

    public int getComplexite() {
        return complexite;
    }

    public void setComplexite(int complexite) {
        this.complexite = complexite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOrigine() {
        return origine;
    }

    public void setOrigine(String origine) {
        this.origine = origine;
    }

    public float getDure() {
        return dure;
    }

    public void setDure(float dure) {
        this.dure = dure;
    }

    public int getNbChapitres() {
        return nbChapitres;
    }

    public void setNbChapitres(int nbChapitres) {
        this.nbChapitres = nbChapitres;
    }

    @Override
    public String toString() {
        return "Cour{" +
                "id=" + id +
                ", nomFormateur='" + nomFormateur + '\'' +
                ", complexite=" + complexite +
                ", description='" + description + '\'' +
                ", origine='" + origine + '\'' +
                ", dure=" + dure +
                ", nbChapitres=" + nbChapitres +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Cour cour = (Cour) o;
        return getId() == cour.getId();
    }
}
