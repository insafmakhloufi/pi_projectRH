package Services.Offre;

import Entities.Offre.HeatScore;
import Entities.Offre.OffreEmploi;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de Heat-Map pour calculer la "chaleur" des offres d'emploi
 * Utilise UNIQUEMENT les champs existants : dateLimite, datePublication, nombrePoste, status
 * 
 * Innovation : Score temps réel basé sur l'urgence, la rareté et la fraîcheur
 * Sans modification de la base de données
 */
public class JobHeatMapService {
    
    /**
     * Calcule le score de chaleur pour une offre
     * 
     * @param offre L'offre d'emploi à analyser
     * @return HeatScore avec tous les scores calculés
     */
    public HeatScore calculerHeatScore(OffreEmploi offre) {
        if (offre == null || !"active".equalsIgnoreCase(offre.getStatus())) {
            return null;
        }
        
        // Calcul des métriques temporelles
        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateLimite = offre.getDateLimiteCandidature();
        LocalDate datePublication = offre.getDatePublication();
        Integer nombrePoste = offre.getNombrePoste();
        
        long joursRestants = 30; // Valeur par défaut
        long joursEnLigne = 7;   // Valeur par défaut
        
        if (dateLimite != null) {
            joursRestants = ChronoUnit.DAYS.between(aujourdHui, dateLimite);
        }
        
        if (datePublication != null) {
            joursEnLigne = ChronoUnit.DAYS.between(datePublication, aujourdHui);
        }
        
        // Score d'URGENCE (basé sur dateLimite)
        int scoreUrgence = calculerScoreUrgence(joursRestants);
        
        // Score de RARETÉ (basé sur nombrePoste)
        int scoreRareté = calculerScoreRareté(nombrePoste);
        
        // Score de FRAÎCHEUR (basé sur datePublication)
        int scoreFraicheur = calculerScoreFraicheur(joursEnLigne);
        
        return new HeatScore(scoreUrgence, scoreRareté, scoreFraicheur, joursRestants, joursEnLigne);
    }
    
    /**
     * Calcule le score d'urgence basé sur les jours restants avant deadline
     * < 3 jours = 100 points (urgence maximale)
     * < 7 jours = 80 points (rapide)
     * < 14 jours = 60 points (modéré)
     * < 30 jours = 40 points (standard)
     * > 30 jours = 20 points (lointain)
     */
    private int calculerScoreUrgence(long joursRestants) {
        if (joursRestants <= 2) return 100;      // 🔥 Ultra urgent
        if (joursRestants <= 5) return 90;       // 🔥 Très urgent
        if (joursRestants <= 7) return 80;       // ⚡ Urgent
        if (joursRestants <= 14) return 70;      // ⚡ Pressé
        if (joursRestants <= 21) return 50;      // 📅 Standard
        if (joursRestants <= 30) return 30;      // 📅 Lointain
        if (joursRestants > 30) return 20;       // 📅 Très lointain
        return 0;                                // Expiré
    }
    
    /**
     * Calcule le score de rareté basé sur le nombre de postes disponibles
     * 1-2 postes = 90-100 points (très exclusif)
     * 3-5 postes = 70-80 points (sélectif)
     * 6-10 postes = 50-60 points (standard)
     * > 10 postes = 30-40 points (massif)
     */
    private int calculerScoreRareté(Integer nombrePoste) {
        if (nombrePoste == null) return 50;
        
        int nb = nombrePoste;
        if (nb == 1) return 100;      // 💎 Ultra exclusif
        if (nb == 2) return 90;       // 💎 Très exclusif
        if (nb <= 3) return 85;       // 💎 Exclusif
        if (nb <= 5) return 75;       // ⭐ Sélectif
        if (nb <= 7) return 65;       // ⭐ Modérément sélectif
        if (nb <= 10) return 50;      // 📊 Standard
        if (nb <= 20) return 40;      // 📊 Recrutement massif
        if (nb <= 50) return 30;      // 📊 Campagne large
        return 20;                    // 📊 Très massif
    }
    
    /**
     * Calcule le score de fraîcheur basé sur les jours depuis la publication
     * < 2 jours = 100 points (tout chaud)
     * < 7 jours = 80-90 points (récent)
     * < 14 jours = 60-70 points (frais)
     * < 30 jours = 40-50 points (standard)
     * > 30 jours = 20-30 points (vieillissant)
     */
    private int calculerScoreFraicheur(long joursEnLigne) {
        if (joursEnLigne <= 1) return 100;       // ✨ Publié aujourd'hui
        if (joursEnLigne <= 2) return 95;         // ✨ Hier
        if (joursEnLigne <= 3) return 90;        // ✨ Très récent
        if (joursEnLigne <= 5) return 85;        // 🆕 Récent
        if (joursEnLigne <= 7) return 80;        // 🆕 Cette semaine
        if (joursEnLigne <= 10) return 70;       // 📅 Semaine dernière
        if (joursEnLigne <= 14) return 60;       // 📅 2 semaines
        if (joursEnLigne <= 21) return 50;       // 📅 3 semaines
        if (joursEnLigne <= 30) return 40;       // 📅 1 mois
        if (joursEnLigne <= 45) return 30;       // 📅 1.5 mois
        if (joursEnLigne <= 60) return 25;       // 📅 2 mois
        return 20;                               // 📅 Vieux
    }
    
    /**
     * Trie une liste d'offres par chaleur décroissante
     * Les offres les plus "chaudes" apparaissent en premier
     */
    public List<OffreEmploi> trierParChaleur(List<OffreEmploi> offres) {
        return offres.stream()
            .sorted((o1, o2) -> {
                HeatScore h1 = calculerHeatScore(o1);
                HeatScore h2 = calculerHeatScore(o2);
                if (h1 == null && h2 == null) return 0;
                if (h1 == null) return 1;
                if (h2 == null) return -1;
                return Integer.compare(h2.getScoreGlobal(), h1.getScoreGlobal());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Filtre les offres par niveau de chaleur minimum
     */
    public List<OffreEmploi> filtrerParChaleurMin(List<OffreEmploi> offres, int scoreMin) {
        return offres.stream()
            .filter(o -> {
                HeatScore score = calculerHeatScore(o);
                return score != null && score.getScoreGlobal() >= scoreMin;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère uniquement les offres "chaudes" (score >= 70)
     * Parfait pour "Les opportunités du moment"
     */
    public List<OffreEmploi> getOffresChaudes(List<OffreEmploi> offres) {
        return filtrerParChaleurMin(offres, 70);
    }
    
    /**
     * Récupère les offres urgentes (ferment dans moins de 3 jours)
     */
    public List<OffreEmploi> getOffresUrgentes(List<OffreEmploi> offres) {
        return offres.stream()
            .filter(o -> {
                HeatScore score = calculerHeatScore(o);
                return score != null && score.getJoursRestants() <= 3;
            })
            .sorted(Comparator.comparingInt(o -> (int) calculerHeatScore(o).getJoursRestants()))
            .collect(Collectors.toList());
    }
    
    /**
     * Récupère les nouvelles offres (publiées dans les 3 derniers jours)
     */
    public List<OffreEmploi> getNouvellesOffres(List<OffreEmploi> offres) {
        return offres.stream()
            .filter(o -> {
                HeatScore score = calculerHeatScore(o);
                return score != null && score.getJoursEnLigne() <= 3;
            })
            .sorted((o1, o2) -> {
                // Plus récent en premier
                HeatScore h1 = calculerHeatScore(o1);
                HeatScore h2 = calculerHeatScore(o2);
                return Long.compare(h1.getJoursEnLigne(), h2.getJoursEnLigne());
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Génère un résumé des statistiques de chaleur
     * Utile pour le dashboard RH
     */
    public HeatStats genererStatistiques(List<OffreEmploi> offres) {
        List<HeatScore> scores = offres.stream()
            .map(this::calculerHeatScore)
            .filter(s -> s != null)
            .collect(Collectors.toList());
        
        if (scores.isEmpty()) {
            return new HeatStats(0, 0, 0, 0, 0);
        }
        
        long ultraChaud = scores.stream().filter(s -> s.getScoreGlobal() >= 85).count();
        long chaud = scores.stream().filter(s -> s.getScoreGlobal() >= 70 && s.getScoreGlobal() < 85).count();
        long nouveau = scores.stream().filter(s -> s.getScoreFraicheur() >= 80).count();
        long urgent = scores.stream().filter(s -> s.getJoursRestants() <= 3).count();
        
        double moyenne = scores.stream().mapToInt(HeatScore::getScoreGlobal).average().orElse(0);
        
        return new HeatStats(ultraChaud, chaud, nouveau, urgent, (int) moyenne);
    }
    
    /**
     * Classe interne pour les statistiques de chaleur
     */
    public static class HeatStats {
        public final long ultraChaud;    // Score >= 85
        public final long chaud;         // Score >= 70
        public final long nouveau;       // Fraîcheur >= 80
        public final long urgent;        // Jours restants <= 3
        public final int moyenne;        // Score moyen
        
        public HeatStats(long ultraChaud, long chaud, long nouveau, long urgent, int moyenne) {
            this.ultraChaud = ultraChaud;
            this.chaud = chaud;
            this.nouveau = nouveau;
            this.urgent = urgent;
            this.moyenne = moyenne;
        }
        
        @Override
        public String toString() {
            return String.format("HeatStats[🔥%d ⚡%d ✨%d ⏰%d | Moy:%d%%]",
                ultraChaud, chaud, nouveau, urgent, moyenne);
        }
    }
}
