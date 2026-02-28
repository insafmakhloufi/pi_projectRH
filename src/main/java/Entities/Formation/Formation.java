package Entities.Formation;

import java.sql.Date;

public class Formation {
    private int id;
    private String lieu;
    private String titre;
    private String description;
    private String domaine;
    private Date dateDebut;
    private Date dateFin;

    private boolean payante;
    private float prix;
    public Formation() {
    }

    public Formation(int id, String lieu, String titre, String description, String domaine, Date dateDebut, Date dateFin) {
        this.id = id;
        this.lieu = lieu;
        this.titre = titre;
        this.description = description;
        this.domaine = domaine;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.payante = false;
        this.prix = 0f;
    }

    public Formation(int id, String lieu, String titre, String description, String domaine, Date dateDebut, Date dateFin, boolean payante, float prix) {
        this.id = id;
        this.lieu = lieu;
        this.titre = titre;
        this.description = description;
        this.domaine = domaine;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.payante = payante;
        this.prix = prix;
    }

    public Formation(String lieu, String titre, String description, String domaine, Date dateDebut, Date dateFin) {
        this.lieu = lieu;
        this.titre = titre;
        this.description = description;
        this.domaine = domaine;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.payante = false;
        this.prix = 0f;
    }

    public Formation(String lieu, String titre, String description, String domaine, Date dateDebut, Date dateFin, boolean payante, float prix) {
        this.lieu = lieu;
        this.titre = titre;
        this.description = description;
        this.domaine = domaine;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.payante = payante;
        this.prix = prix;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDomaine() {
        return domaine;
    }

    public void setDomaine(String domaine) {
        this.domaine = domaine;
    }

    public Date getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(Date dateDebut) {
        this.dateDebut = dateDebut;
    }

    public Date getDateFin() {
        return dateFin;
    }

    public void setDateFin(Date dateFin) {
        this.dateFin = dateFin;
    }

    public boolean isPayante() {
        return payante;
    }

    public void setPayante(boolean payante) {
        this.payante = payante;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }

    @Override
    public String toString() {
        return titre;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Formation formation = (Formation) o;
        return getId() == formation.getId();
    }
}

