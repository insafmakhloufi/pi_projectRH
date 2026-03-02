package Entites.Formation;

public class Chapitre {
    private int id;
    private int courId;
    private String titre;
    private int ordre;
    private String contenu;
    private int complexite;

    public Chapitre() {
    }

    public Chapitre(int id, int courId, String titre, int ordre, String contenu, int complexite) {
        this.id = id;
        this.courId = courId;
        this.titre = titre;
        this.ordre = ordre;
        this.contenu = contenu;
        this.complexite = complexite;
    }

    public Chapitre(int courId, String titre, int ordre, String contenu, int complexite) {
        this.courId = courId;
        this.titre = titre;
        this.ordre = ordre;
        this.contenu = contenu;
        this.complexite = complexite;
    }

    public Chapitre(int id, int courId, String titre, int ordre, String contenu) {
        this.id = id;
        this.courId = courId;
        this.titre = titre;
        this.ordre = ordre;
        this.contenu = contenu;
        this.complexite = 1;
    }

    public Chapitre(int courId, String titre, int ordre, String contenu) {
        this.courId = courId;
        this.titre = titre;
        this.ordre = ordre;
        this.contenu = contenu;
        this.complexite = 1;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCourId() {
        return courId;
    }

    public void setCourId(int courId) {
        this.courId = courId;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public int getOrdre() {
        return ordre;
    }

    public void setOrdre(int ordre) {
        this.ordre = ordre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public int getComplexite() {
        return complexite;
    }

    public void setComplexite(int complexite) {
        this.complexite = complexite;
    }

    @Override
    public String toString() {
        return titre;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Chapitre chapitre = (Chapitre) o;
        return getId() == chapitre.getId();
    }
}
