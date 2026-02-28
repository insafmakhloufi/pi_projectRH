package Entities.evenement;

import java.time.LocalDateTime;

public class Publication {

    private int idPublication;
    private String titre;
    private String contenu;
    private LocalDateTime datePublication;
    private int auteurId;
    private String type;
    private String statut;
    private String visibilite;
    private String imageUrl;
    private String videoUrl;
    private String pieceJointeUrl;
    private Integer evenementId; // nullable

    public Publication() {}

    public Publication(int idPublication, String titre, String contenu,
                       LocalDateTime datePublication, int auteurId,
                       String type, String statut, String visibilite,
                       String imageUrl, String videoUrl,
                       String pieceJointeUrl, Integer evenementId) {
        this.idPublication = idPublication;
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.auteurId = auteurId;
        this.type = type;
        this.statut = statut;
        this.visibilite = visibilite;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.pieceJointeUrl = pieceJointeUrl;
        this.evenementId = evenementId;
    }

    public Publication(String titre, String contenu,
                       LocalDateTime datePublication, int auteurId,
                       String type, String statut, String visibilite,
                       String imageUrl, String videoUrl,
                       String pieceJointeUrl, Integer evenementId) {
        this.titre = titre;
        this.contenu = contenu;
        this.datePublication = datePublication;
        this.auteurId = auteurId;
        this.type = type;
        this.statut = statut;
        this.visibilite = visibilite;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.pieceJointeUrl = pieceJointeUrl;
        this.evenementId = evenementId;
    }

    public int getIdPublication() {
        return idPublication;
    }

    public void setIdPublication(int idPublication) {
        this.idPublication = idPublication;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(LocalDateTime datePublication) {
        this.datePublication = datePublication;
    }

    public int getAuteurId() {
        return auteurId;
    }

    public void setAuteurId(int auteurId) {
        this.auteurId = auteurId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getVisibilite() {
        return visibilite;
    }

    public void setVisibilite(String visibilite) {
        this.visibilite = visibilite;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getPieceJointeUrl() {
        return pieceJointeUrl;
    }

    public void setPieceJointeUrl(String pieceJointeUrl) {
        this.pieceJointeUrl = pieceJointeUrl;
    }

    public Integer getEvenementId() {
        return evenementId;
    }

    public void setEvenementId(Integer evenementId) {
        this.evenementId = evenementId;
    }

    @Override
    public String toString() {
        return "Publication{" +
                "idPublication=" + idPublication +
                ", titre='" + titre + '\'' +
                ", contenu='" + contenu + '\'' +
                ", datePublication=" + datePublication +
                ", auteurId=" + auteurId +
                ", type='" + type + '\'' +
                ", statut='" + statut + '\'' +
                ", visibilite='" + visibilite + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", pieceJointeUrl='" + pieceJointeUrl + '\'' +
                ", evenementId=" + evenementId +
                '}';
    }
}
