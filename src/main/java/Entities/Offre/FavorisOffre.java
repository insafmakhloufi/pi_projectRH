package Entities.Offre;

import java.time.LocalDateTime;

/**
 * Entité représentant une offre d'emploi ajoutée aux favoris d'un utilisateur
 */
public class FavorisOffre {
    
    private Integer id;
    private Integer userId;
    private Integer offreId;
    private LocalDateTime dateAjout;
    
    // Relations
    private OffreEmploi offre;
    
    public FavorisOffre() {
        this.dateAjout = LocalDateTime.now();
    }
    
    public FavorisOffre(Integer userId, Integer offreId) {
        this();
        this.userId = userId;
        this.offreId = offreId;
    }
    
    // Getters et Setters
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }
    
    public Integer getUserId() {
        return userId;
    }
    
    public void setUserId(Integer userId) {
        this.userId = userId;
    }
    
    public Integer getOffreId() {
        return offreId;
    }
    
    public void setOffreId(Integer offreId) {
        this.offreId = offreId;
    }
    
    public LocalDateTime getDateAjout() {
        return dateAjout;
    }
    
    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }
    
    public OffreEmploi getOffre() {
        return offre;
    }
    
    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }
}
