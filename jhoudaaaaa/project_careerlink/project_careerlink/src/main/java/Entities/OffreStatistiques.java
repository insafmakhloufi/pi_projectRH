package Entities;

public class OffreStatistiques {
    private int offreId;
    private String titreOffre;
    private int nombreVues;
    private int nombrePostulations;
    private int nombreEntretiens;
    private double tauxConversion; // postulations / vues
    private String secteur;
    private String localisation;
    
    public OffreStatistiques() {}
    
    public OffreStatistiques(int offreId, String titreOffre, int nombreVues, int nombrePostulations, int nombreEntretiens, String secteur, String localisation) {
        this.offreId = offreId;
        this.titreOffre = titreOffre;
        this.nombreVues = nombreVues;
        this.nombrePostulations = nombrePostulations;
        this.nombreEntretiens = nombreEntretiens;
        this.secteur = secteur;
        this.localisation = localisation;
        calculateTauxConversion();
    }
    
    private void calculateTauxConversion() {
        if (nombreVues > 0) {
            this.tauxConversion = (nombrePostulations * 100.0) / nombreVues;
        } else {
            this.tauxConversion = 0.0;
        }
    }
    
    // Getters et Setters
    public int getOffreId() { return offreId; }
    public void setOffreId(int offreId) { this.offreId = offreId; }
    
    public String getTitreOffre() { return titreOffre; }
    public void setTitreOffre(String titreOffre) { this.titreOffre = titreOffre; }
    
    public int getNombreVues() { return nombreVues; }
    public void setNombreVues(int nombreVues) { 
        this.nombreVues = nombreVues;
        calculateTauxConversion();
    }
    
    public int getNombrePostulations() { return nombrePostulations; }
    public void setNombrePostulations(int nombrePostulations) { 
        this.nombrePostulations = nombrePostulations;
        calculateTauxConversion();
    }
    
    public int getNombreEntretiens() { return nombreEntretiens; }
    public void setNombreEntretiens(int nombreEntretiens) { this.nombreEntretiens = nombreEntretiens; }
    
    public double getTauxConversion() { return tauxConversion; }
    public void setTauxConversion(double tauxConversion) { this.tauxConversion = tauxConversion; }
    
    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }
    
    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }
    
    @Override
    public String toString() {
        return String.format("OffreStatistiques{id=%d, titre='%s', vues=%d, postulations=%d, taux=%.1f%%}",
                offreId, titreOffre, nombreVues, nombrePostulations, tauxConversion);
    }
}
