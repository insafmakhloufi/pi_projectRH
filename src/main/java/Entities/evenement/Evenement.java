package Entities.evenement;

import java.time.LocalDateTime;

public class Evenement {

    private int idEvenement;
    private String titre;
    private String description;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String lieu;
    private String type;
    private int capaciteMax;
    private String statut;
    private String imageUrl;

    public Evenement() {}

    public Evenement(int idEvenement, String titre, String description,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String type, int capaciteMax,
                     String statut, String imageUrl) {
        this.idEvenement = idEvenement;
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.type = type;
        this.capaciteMax = capaciteMax;
        this.statut = statut;
        this.imageUrl = imageUrl;
    }

    public Evenement(String titre, String description,
                     LocalDateTime dateDebut, LocalDateTime dateFin,
                     String lieu, String type, int capaciteMax,
                     String statut, String imageUrl) {
        this.titre = titre;
        this.description = description;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.lieu = lieu;
        this.type = type;
        this.capaciteMax = capaciteMax;
        this.statut = statut;
        this.imageUrl = imageUrl;
    }

    public int getIdEvenement() {
        return idEvenement;
    }

    public void setIdEvenement(int idEvenement) {
        this.idEvenement = idEvenement;
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

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getCapaciteMax() {
        return capaciteMax;
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax = capaciteMax;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Evenement{" +
                "idEvenement=" + idEvenement +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", lieu='" + lieu + '\'' +
                ", type='" + type + '\'' +
                ", capaciteMax=" + capaciteMax +
                ", statut='" + statut + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
