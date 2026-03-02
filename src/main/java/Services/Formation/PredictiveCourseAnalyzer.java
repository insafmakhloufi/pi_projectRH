package Services.Formation;

import Entites.Formation.Cour;
import Entites.Formation.Chapitre;

import java.util.List;

public class PredictiveCourseAnalyzer {

    private final ChapitreServices chapitreServices = new ChapitreServices();

    /**
     * Analyse un cours et ses chapitres pour prédire si c'est un cours facile ou complexe.
     * @param cour Le cours à analyser
     * @return Une chaîne indiquant la prédiction
     */
    public String getComplexityPrediction(Cour cour) {
        if (cour == null) return "Analyse impossible : Cours null";
        
        List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());
        int courComplexite = cour.getComplexite(); // La complexité définie sur le cours lui-même

        if (chapitres == null || chapitres.isEmpty()) {
            // Si pas de chapitres, on se base uniquement sur la complexité du cours
            if (courComplexite >= 4) return "Cours complexe (basé sur l'estimation globale)";
            if (courComplexite <= 2) return "Cours facile (basé sur l'estimation globale)";
            return "Cours de difficulté moyenne";
        }

        int nbChapitres = chapitres.size();
        double complexiteTotaleChapitres = 0.0;
        for (Chapitre ch : chapitres) {
            complexiteTotaleChapitres += ch.getComplexite();
        }
        double complexiteMoyenneChapitres = complexiteTotaleChapitres / nbChapitres;

        // Calcul d'un score combiné (50% complexité cours, 50% moyenne chapitres)
        double scoreFinal = (courComplexite + complexiteMoyenneChapitres) / 2.0;

        if (scoreFinal > 3.5 || (nbChapitres > 5 && scoreFinal > 3.0)) {
            return "Cours complexe";
        } else if (scoreFinal < 2.0 && nbChapitres <= 3) {
            return "Cours facile";
        } else {
            return "Cours de difficulté moyenne";
        }
    }

    /**
     * Fournit des suggestions basées sur l'analyse combinée.
     * @param cour Le cours à analyser
     * @return Une chaîne de suggestions
     */
    public String getSuggestions(Cour cour) {
        if (cour == null) return "";
        
        List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());
        int courComplexite = cour.getComplexite();

        StringBuilder suggestions = new StringBuilder();
        suggestions.append("Complexité déclarée du cours : ").append(courComplexite).append("/5\n");

        if (chapitres == null || chapitres.isEmpty()) {
            suggestions.append("Note : Aucun chapitre trouvé. Ajoutez des chapitres pour affiner l'analyse.\n");
            return suggestions.toString();
        }

        int nbChapitres = chapitres.size();
        double complexiteTotale = 0.0;
        for (Chapitre ch : chapitres) {
            complexiteTotale += ch.getComplexite();
        }
        double complexiteMoyenne = complexiteTotale / nbChapitres;

        suggestions.append("Nombre de chapitres : ").append(nbChapitres).append("\n");
        suggestions.append("Complexité moyenne des chapitres : ").append(String.format("%.1f", complexiteMoyenne)).append("/5\n\n");

        // Analyse de cohérence
        if (Math.abs(courComplexite - complexiteMoyenne) > 1.5) {
            suggestions.append("Attention : Il y a un écart important entre la complexité globale du cours et celle de ses chapitres.\n");
        }

        if (nbChapitres < 3 && courComplexite > 3) {
            suggestions.append("Suggestion : Pour un cours annoncé comme complexe, 3 chapitres ou moins peuvent sembler insuffisants.\n");
        }
        
        if (complexiteMoyenne > 4.0) {
            suggestions.append("Suggestion : Les chapitres sont très denses. Pensez à les diviser.\n");
        }

        return suggestions.toString();
    }
}
