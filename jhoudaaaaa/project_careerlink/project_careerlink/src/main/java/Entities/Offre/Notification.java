package Entities.Offre;

import java.time.LocalDateTime;

public class Notification {
    private int id;
    private int userId;
    private String type; // "NOUVELLE_OFFRE", "CANDIDATURE", "ENTRETIEN", etc.
    private String titre;
    private String message;
    private boolean lu;
    private LocalDateTime dateCreation;
    private Integer offreId; // optionnel, lien vers offre concernée
    
    public Notification() {
        this.dateCreation = LocalDateTime.now();
        this.lu = false;
    }
    
    public Notification(int id, int userId, String type, String titre, String message, boolean lu, LocalDateTime dateCreation, Integer offreId) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.titre = titre;
        this.message = message;
        this.lu = lu;
        this.dateCreation = dateCreation;
        this.offreId = offreId;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public boolean isLu() { return lu; }
    public void setLu(boolean lu) { this.lu = lu; }
    
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    
    public Integer getOffreId() { return offreId; }
    public void setOffreId(Integer offreId) { this.offreId = offreId; }
    
    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", type='" + type + '\'' +
                ", titre='" + titre + '\'' +
                ", lu=" + lu +
                ", dateCreation=" + dateCreation +
                '}';
    }
}
