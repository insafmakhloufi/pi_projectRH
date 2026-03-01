package Entities.candidature;

import java.sql.Date;
import java.util.Objects;

public class Certification {
    private int id_certification;
    private int id_candidature;
    private String organisme;
    private String nom_certification;
    private Integer numero;         // nullable (car int(11))
    private Date date_obtention;    // NOT NULL dans ta table
    private Date date_expiration;   // nullable

    public Certification() {}

    public Certification(int id_certification, int id_candidature, String organisme, String nom_certification,
                         Integer numero, Date date_obtention, Date date_expiration) {
        this.id_certification = id_certification;
        this.id_candidature = id_candidature;
        this.organisme = organisme;
        this.nom_certification = nom_certification;
        this.numero = numero;
        this.date_obtention = date_obtention;
        this.date_expiration = date_expiration;
    }

    public Certification(int id_candidature, String organisme, String nom_certification,
                         Integer numero, Date date_obtention, Date date_expiration) {
        this.id_candidature = id_candidature;
        this.organisme = organisme;
        this.nom_certification = nom_certification;
        this.numero = numero;
        this.date_obtention = date_obtention;
        this.date_expiration = date_expiration;
    }

    public int getId_certification() { return id_certification; }
    public void setId_certification(int id_certification) { this.id_certification = id_certification; }

    public int getId_candidature() { return id_candidature; }
    public void setId_candidature(int id_candidature) { this.id_candidature = id_candidature; }

    public String getOrganisme() { return organisme; }
    public void setOrganisme(String organisme) { this.organisme = organisme; }

    public String getNom_certification() { return nom_certification; }
    public void setNom_certification(String nom_certification) { this.nom_certification = nom_certification; }

    public Integer getNumero() { return numero; }
    public void setNumero(Integer numero) { this.numero = numero; }

    public Date getDate_obtention() { return date_obtention; }
    public void setDate_obtention(Date date_obtention) { this.date_obtention = date_obtention; }

    public Date getDate_expiration() { return date_expiration; }
    public void setDate_expiration(Date date_expiration) { this.date_expiration = date_expiration; }

    @Override
    public String toString() {
        return "Certification{" +
                "id_certification=" + id_certification +
                ", id_candidature=" + id_candidature +
                ", organisme='" + organisme + '\'' +
                ", nom_certification='" + nom_certification + '\'' +
                ", numero=" + numero +
                ", date_obtention=" + date_obtention +
                ", date_expiration=" + date_expiration +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Certification that = (Certification) o;
        return id_certification == that.id_certification &&
                id_candidature == that.id_candidature &&
                Objects.equals(organisme, that.organisme) &&
                Objects.equals(nom_certification, that.nom_certification) &&
                Objects.equals(numero, that.numero) &&
                Objects.equals(date_obtention, that.date_obtention) &&
                Objects.equals(date_expiration, that.date_expiration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_certification, id_candidature, organisme, nom_certification, numero, date_obtention, date_expiration);
    }
}
