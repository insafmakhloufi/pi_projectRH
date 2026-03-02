package Entities.candidature;

import java.sql.Date;
import java.util.Objects;

public class Experience {
    private int id_experience;
    private int id_candidature;
    private String poste;
    private String entreprise;
    private Date date_debut;   // nullable
    private Date date_fin;     // nullable

    public Experience() {}

    public Experience(int id_experience, int id_candidature, String poste, String entreprise, Date date_debut, Date date_fin) {
        this.id_experience = id_experience;
        this.id_candidature = id_candidature;
        this.poste = poste;
        this.entreprise = entreprise;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
    }

    public Experience(int id_candidature, String poste, String entreprise, Date date_debut, Date date_fin) {
        this.id_candidature = id_candidature;
        this.poste = poste;
        this.entreprise = entreprise;
        this.date_debut = date_debut;
        this.date_fin = date_fin;
    }

    public int getId_experience() { return id_experience; }
    public void setId_experience(int id_experience) { this.id_experience = id_experience; }

    public int getId_candidature() { return id_candidature; }
    public void setId_candidature(int id_candidature) { this.id_candidature = id_candidature; }

    public String getPoste() { return poste; }
    public void setPoste(String poste) { this.poste = poste; }

    public String getEntreprise() { return entreprise; }
    public void setEntreprise(String entreprise) { this.entreprise = entreprise; }

    public Date getDate_debut() { return date_debut; }
    public void setDate_debut(Date date_debut) { this.date_debut = date_debut; }

    public Date getDate_fin() { return date_fin; }
    public void setDate_fin(Date date_fin) { this.date_fin = date_fin; }

    @Override
    public String toString() {
        return "Experience{" +
                "id_experience=" + id_experience +
                ", id_candidature=" + id_candidature +
                ", poste='" + poste + '\'' +
                ", entreprise='" + entreprise + '\'' +
                ", date_debut=" + date_debut +
                ", date_fin=" + date_fin +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Experience that = (Experience) o;
        return id_experience == that.id_experience &&
                id_candidature == that.id_candidature &&
                Objects.equals(poste, that.poste) &&
                Objects.equals(entreprise, that.entreprise) &&
                Objects.equals(date_debut, that.date_debut) &&
                Objects.equals(date_fin, that.date_fin);
    }


}
