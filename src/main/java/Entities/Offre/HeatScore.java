package Entities.Offre;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Représente le score de "chaleur" d'une offre d'emploi
 * Calculé à partir des champs existants : dateLimite, datePublication, nombrePoste, status
 * Sans modification de la base de données
 */
public class HeatScore {
    
    private final int scoreGlobal;           // 0-100
    private final int scoreUrgence;          // 0-100 (basé sur dateLimite)
    private final int scoreRareté;           // 0-100 (basé sur nombrePoste)
    private final int scoreFraicheur;        // 0-100 (basé sur datePublication)
    private final String label;              // 🔥 URGENT, ⚡ RAPIDE, ✨ NEW, 📌 CLASSIC
    private final String emoji;              // 🔥, ⚡, ✨, 📌
    private final long joursRestants;        // Jours avant deadline
    private final long joursEnLigne;         // Jours depuis publication
    
    public HeatScore(int scoreUrgence, int scoreRareté, int scoreFraicheur, 
                     long joursRestants, long joursEnLigne) {
        this.scoreUrgence = scoreUrgence;
        this.scoreRareté = scoreRareté;
        this.scoreFraicheur = scoreFraicheur;
        this.joursRestants = joursRestants;
        this.joursEnLigne = joursEnLigne;
        
        // Score global = moyenne pondérée
        this.scoreGlobal = (scoreUrgence * 4 + scoreRareté * 3 + scoreFraicheur * 2) / 9;
        
        // Déterminer le label et emoji
        if (scoreGlobal >= 85) {
            this.label = "🔥 URGENT";
            this.emoji = "🔥";
        } else if (scoreGlobal >= 70) {
            this.label = "⚡ RAPIDE";
            this.emoji = "⚡";
        } else if (scoreFraicheur >= 80) {
            this.label = "✨ NEW";
            this.emoji = "✨";
        } else {
            this.label = "📌 CLASSIC";
            this.emoji = "📌";
        }
    }
    
    // Getters
    public int getScoreGlobal() { return scoreGlobal; }
    public int getScoreUrgence() { return scoreUrgence; }
    public int getScoreRareté() { return scoreRareté; }
    public int getScoreFraicheur() { return scoreFraicheur; }
    public String getLabel() { return label; }
    public String getEmoji() { return emoji; }
    public long getJoursRestants() { return joursRestants; }
    public long getJoursEnLigne() { return joursEnLigne; }
    
    /**
     * Retourne les barres de chaleur visuelles (🔥🔥🔥⚪⚪)
     */
    public String getHeatBars() {
        int filledBars = scoreGlobal / 20; // 5 niveaux
        StringBuilder bars = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            bars.append(i < filledBars ? "🔥" : "⚪");
        }
        return bars.toString();
    }
    
    /**
     * Message de recommandation personnalisé
     */
    public String getRecommendationMessage() {
        if (scoreGlobal >= 85) {
            return "Cette offre ferme bientôt ! Postulez immédiatement.";
        } else if (scoreGlobal >= 70) {
            return "Offre populaire avec peu de postes disponibles.";
        } else if (scoreFraicheur >= 80) {
            return "Nouvelle offre - soyez parmi les premiers !";
        } else {
            return "Offre classique toujours active.";
        }
    }
    
    @Override
    public String toString() {
        return String.format("HeatScore[%s %d%% | Urgence:%d%% Rareté:%d%% Fraîcheur:%d%%]",
            emoji, scoreGlobal, scoreUrgence, scoreRareté, scoreFraicheur);
    }
}
