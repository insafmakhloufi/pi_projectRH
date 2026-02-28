package Entities.Offre;

/**
 * DTO représentant une offre avec son score de compatibilité pour un candidat.
 * Utilisé pour l'affichage des offres recommandées dans le front.
 */
public class OffreCompatibilite {
    private OffreEmploi offre;
    private int score;  // 0-100
    private String candidatVille;
    private String candidatSkills;
    private String candidatDiplome;
    private String offreVille;
    
    public OffreCompatibilite() {
    }
    
    public OffreCompatibilite(OffreEmploi offre, int score, String candidatVille, 
                              String candidatSkills, String candidatDiplome, String offreVille) {
        this.offre = offre;
        this.score = score;
        this.candidatVille = candidatVille;
        this.candidatSkills = candidatSkills;
        this.candidatDiplome = candidatDiplome;
        this.offreVille = offreVille;
    }
    
    public OffreEmploi getOffre() {
        return offre;
    }
    
    public void setOffre(OffreEmploi offre) {
        this.offre = offre;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public String getCandidatVille() {
        return candidatVille;
    }
    
    public void setCandidatVille(String candidatVille) {
        this.candidatVille = candidatVille;
    }
    
    public String getCandidatSkills() {
        return candidatSkills;
    }
    
    public void setCandidatSkills(String candidatSkills) {
        this.candidatSkills = candidatSkills;
    }
    
    public String getCandidatDiplome() {
        return candidatDiplome;
    }
    
    public void setCandidatDiplome(String candidatDiplome) {
        this.candidatDiplome = candidatDiplome;
    }
    
    public String getOffreVille() {
        return offreVille;
    }
    
    public void setOffreVille(String offreVille) {
        this.offreVille = offreVille;
    }
    
    /**
     * Retourne le label qualitatif du score
     */
    public String getScoreLabel() {
        if (score >= 80) return "Excellent";
        if (score >= 60) return "Très bon";
        if (score >= 40) return "Bon";
        if (score >= 20) return "Moyen";
        return "Faible";
    }
    
    /**
     * Retourne la couleur CSS associée au score
     */
    public String getScoreColor() {
        if (score >= 80) return "#22c55e";  // vert
        if (score >= 60) return "#3b82f6";  // bleu
        if (score >= 40) return "#f59e0b";  // orange
        return "#ef4444";  // rouge
    }
    
    @Override
    public String toString() {
        return "OffreCompatibilite{" +
                "offre=" + (offre != null ? offre.getTitre() : "null") +
                ", score=" + score +
                ", candidatVille='" + candidatVille + '\'' +
                ", candidatSkills='" + candidatSkills + '\'' +
                ", candidatDiplome='" + candidatDiplome + '\'' +
                ", offreVille='" + offreVille + '\'' +
                '}';
    }
}
