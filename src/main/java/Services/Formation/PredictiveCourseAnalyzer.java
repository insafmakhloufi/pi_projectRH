package Services.Formation;

import Entities.Formation.Cour;
import Entities.Formation.Chapitre;

import java.util.List;

public class PredictiveCourseAnalyzer {

    private final ChapitreServices chapitreServices = new ChapitreServices();
    private final MlComplexityPredictor mlPredictor = new MlComplexityPredictor();

    private static final String PDF_PREFIX = "pdf:";
    private static final String PDFS_PREFIX = "pdfs:";

    private static int countPdfFilesFromContenu(String contenu) {
        if (contenu == null) {
            return 0;
        }
        String v = contenu.trim();
        if (v.isEmpty()) {
            return 0;
        }
        String lower = v.toLowerCase();
        if (lower.startsWith(PDF_PREFIX)) {
            String raw = v.substring(PDF_PREFIX.length()).trim();
            return raw.isEmpty() ? 0 : 1;
        }
        if (lower.startsWith(PDFS_PREFIX)) {
            String raw = v.substring(PDFS_PREFIX.length()).trim();
            if (raw.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (String p : raw.split(";")) {
                if (p == null) continue;
                if (!p.trim().isEmpty()) {
                    count++;
                }
            }
            return count;
        }
        return 0;
    }

    private static boolean isPdfContent(String contenu) {
        if (contenu == null) {
            return false;
        }
        String v = contenu.trim().toLowerCase();
        return v.startsWith(PDF_PREFIX) || v.startsWith(PDFS_PREFIX);
    }

    private static long textLen(String contenu) {
        if (contenu == null) {
            return 0L;
        }
        return contenu.trim().length();
    }

    private static int estimateReadingMinutesFromChars(long chars) {
        if (chars <= 0) {
            return 0;
        }
        double minutes = chars / 900.0;
        return (int) Math.max(1, Math.round(minutes));
    }

    /**
     * Analyse un cours et ses chapitres pour prédire si c'est un cours facile ou complexe.
     * @param cour Le cours à analyser
     * @return Une chaîne indiquant la prédiction
     */
    public String getComplexityPrediction(Cour cour) {
        if (cour == null) return "Analyse impossible : Cours null";

        // Try ML prediction first
        try {
            if (mlPredictor.isLoaded()) {
                List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());
                String label = mlPredictor.predictLabel(cour, chapitres);
                if (label != null) {
                    String v = label.trim().toLowerCase();
                    if (v.contains("facile")) return "Cours facile";
                    if (v.contains("moyen")) return "Cours de difficulté moyenne";
                    if (v.contains("complex")) return "Cours complexe";
                    return label;
                }
            }
        } catch (Exception ignored) {
            // Fall through to heuristic
        }

        // Fallback to heuristic based on chapter content
        double scoreFinal = getComplexityScore(cour);
        if (scoreFinal > 2.5) return "Cours complexe";
        if (scoreFinal < 2.5) return "Cours facile";
        return "Cours de difficulté moyenne";
    }

    /**
     * Calcule un score de complexité basé UNIQUEMENT sur le contenu des chapitres.
     * Score de 1.0 (très facile) à 5.0 (très complexe)
     * Ne dépend PAS de la complexité déclarée par l'utilisateur.
     */
    public double getComplexityScore(Cour cour) {
        if (cour == null) return 2.5; // neutral

        List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());

        // Sans chapitres, on retourne une valeur neutre
        if (chapitres == null || chapitres.isEmpty()) {
            return 2.5;
        }

        int nbChapitres = chapitres.size();
        int pdfCount = 0;
        long totalTextLen = 0;
        int textCount = 0;

        for (Chapitre ch : chapitres) {
            if (ch == null) continue;
            String contenu = ch.getContenu();
            if (contenu == null) continue;
            int pdfFiles = countPdfFilesFromContenu(contenu);
            if (pdfFiles > 0) {
                pdfCount += pdfFiles;
            } else {
                totalTextLen += textLen(contenu);
                textCount++;
            }
        }

        // Score de base neutre (2.5 = moyen)
        double score = 2.5;

        // Facteur: Nombre de chapitres (plus de chapitres = plus complexe)
        if (nbChapitres >= 10) {
            score += 1.5;
        } else if (nbChapitres >= 7) {
            score += 1.0;
        } else if (nbChapitres >= 4) {
            score += 0.5;
        } else if (nbChapitres <= 2) {
            score -= 0.5;
        }

        // Facteur: Longueur moyenne du texte (plus de texte = plus complexe)
        double avgTextLen = (textCount == 0) ? 0.0 : ((double) totalTextLen / textCount);
        if (avgTextLen >= 1500) {
            score += 1.0;
        } else if (avgTextLen >= 800) {
            score += 0.5;
        } else if (avgTextLen > 0 && avgTextLen < 300) {
            score -= 0.5;
        }

        // Facteur: Nombre de PDF (plus de PDF = plus complexe)
        if (pdfCount >= 4) {
            score += 1.0;
        } else if (pdfCount >= 2) {
            score += 0.5;
        }

        // Facteur: Temps total estimé (combinaison texte + PDF)
        double estimatedTextMinutes = totalTextLen / 900.0; // 900 chars/min
        double estimatedPdfMinutes = pdfCount * 20.0;       // 20 min/PDF
        double totalMinutes = estimatedTextMinutes + estimatedPdfMinutes;

        if (totalMinutes >= 120) {
            score += 0.5;
        } else if (totalMinutes < 15) {
            score -= 0.5;
        }

        return Math.max(1.0, Math.min(5.0, score));
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
        int pdfCount = 0;
        long totalTextLen = 0;
        int textCount = 0;

        for (Chapitre ch : chapitres) {
            if (ch == null) {
                continue;
            }
            String contenu = ch.getContenu();
            if (contenu == null) {
                continue;
            }
            int pdfFiles = countPdfFilesFromContenu(contenu);
            if (pdfFiles > 0) {
                pdfCount += pdfFiles;
            } else {
                totalTextLen += textLen(contenu);
                textCount++;
            }
        }

        double avgTextLen = (textCount == 0) ? 0.0 : ((double) totalTextLen / textCount);

        suggestions.append("Nombre de chapitres : ").append(nbChapitres).append("\n");
        suggestions.append("Chapitres PDF : ").append(pdfCount).append("\n");
        if (textCount > 0) {
            suggestions.append("Taille moyenne du contenu texte : ").append(String.format("%.0f", avgTextLen)).append(" caractères\n");
        }
        suggestions.append("\n");

        if (courComplexite >= 4 && nbChapitres < 3) {
            suggestions.append("Suggestion : Pour un cours annoncé comme complexe, augmentez le nombre de chapitres pour structurer davantage.\n");
        }
        if (avgTextLen >= 1200) {
            suggestions.append("Suggestion : Les chapitres contiennent beaucoup de texte. Pensez à les découper ou ajouter des exemples.\n");
        }
        if (pdfCount > 0 && textCount == 0) {
            suggestions.append("Suggestion : Vous utilisez uniquement des PDFs. Ajoutez un résumé texte par chapitre pour améliorer la compréhension.\n");
        }
        if (courComplexite <= 2 && nbChapitres >= 8) {
            suggestions.append("Note : Beaucoup de chapitres pour un cours annoncé facile. Vérifiez si la complexité du cours est bien estimée.\n");
        }

        return suggestions.toString();
    }

    public String getChaptersAnalysis(Cour cour) {
        if (cour == null) {
            return "";
        }

        List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());
        if (chapitres == null || chapitres.isEmpty()) {
            return "Aucun chapitre trouvé pour ce cours.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Nombre de chapitres : ").append(chapitres.size()).append("\n\n");

        int pdfCount = 0;
        long totalChars = 0;
        int textCount = 0;

        for (Chapitre ch : chapitres) {
            if (ch == null) {
                continue;
            }
            String titre = ch.getTitre() == null ? "(Sans titre)" : ch.getTitre().trim();
            if (titre.isBlank()) {
                titre = "(Sans titre)";
            }

            String contenu = ch.getContenu();
            boolean pdf = isPdfContent(contenu);
            long chars = pdf ? 0 : textLen(contenu);
            int minutes = estimateReadingMinutesFromChars(chars);

            sb.append("- ").append(ch.getOrdre()).append(". ").append(titre).append("\n");
            sb.append("  Type : ").append(pdf ? "PDF" : "Texte").append("\n");
            if (!pdf) {
                sb.append("  Taille : ").append(chars).append(" caractères").append("\n");
                sb.append("  Temps de lecture estimé : ").append(minutes).append(" min\n");
            } else {
                sb.append("  Conseil : note les points clés pendant la lecture du PDF\n");
            }
            sb.append("\n");

            if (pdf) {
                pdfCount++;
            } else {
                totalChars += chars;
                textCount++;
            }
        }

        if (textCount > 0) {
            double avg = (double) totalChars / textCount;
            sb.append("Résumé :\n");
            sb.append("- Chapitres texte : ").append(textCount).append("\n");
            sb.append("- Chapitres PDF : ").append(pdfCount).append("\n");
            sb.append("- Taille moyenne (texte) : ").append(String.format("%.0f", avg)).append(" caractères\n");
        } else {
            sb.append("Résumé :\n");
            sb.append("- Chapitres texte : 0\n");
            sb.append("- Chapitres PDF : ").append(pdfCount).append("\n");
        }

        return sb.toString();
    }

    public String getLearningTips(Cour cour) {
        if (cour == null) {
            return "";
        }

        List<Chapitre> chapitres = chapitreServices.afficherChapitresParCour(cour.getId());
        int nb = (chapitres == null) ? 0 : chapitres.size();
        int cpx = cour.getComplexite();

        int pdfCount = 0;
        int textCount = 0;
        long totalText = 0;
        if (chapitres != null) {
            for (Chapitre ch : chapitres) {
                if (ch == null) {
                    continue;
                }
                String contenu = ch.getContenu();
                if (isPdfContent(contenu)) {
                    pdfCount++;
                } else if (contenu != null && !contenu.isBlank()) {
                    textCount++;
                    totalText += textLen(contenu);
                }
            }
        }

        double avgTextLen = (textCount == 0) ? 0.0 : ((double) totalText / textCount);

        StringBuilder tips = new StringBuilder();

        tips.append("Plan d'étude conseillé :\n");
        tips.append("- Lis d'abord les titres des chapitres pour comprendre la structure\n");
        tips.append("- Après chaque chapitre, écris un résumé de 3-5 lignes\n");
        tips.append("- Fais une liste des mots clés et définitions importantes\n\n");

        if (cpx >= 4) {
            tips.append("Conseils pour un cours complexe :\n");
            tips.append("- Avance chapitre par chapitre (pas tout d'un coup)\n");
            tips.append("- Refais des exercices/exemples après chaque chapitre\n");
            tips.append("- Revois le cours le lendemain (répétition espacée)\n\n");
        } else if (cpx <= 2) {
            tips.append("Conseils pour un cours facile :\n");
            tips.append("- Va rapidement mais assure-toi de pratiquer avec un mini-projet\n\n");
        }

        if (nb <= 1) {
            tips.append("Structure :\n");
            tips.append("- Tu as très peu de chapitres. Pour mieux comprendre, découpe le cours en plusieurs petites sections.\n\n");
        } else if (nb >= 8) {
            tips.append("Structure :\n");
            tips.append("- Il y a beaucoup de chapitres. Fais un planning (ex: 2 chapitres/jour) et révise à la fin.\n\n");
        }

        if (pdfCount > 0 && textCount == 0) {
            tips.append("PDF uniquement :\n");
            tips.append("- Prends des notes en lisant (titre -> idées -> exemple)\n");
            tips.append("- Transforme chaque page en questions/réponses (flashcards)\n\n");
        } else if (avgTextLen >= 1200) {
            tips.append("Chapitres très longs :\n");
            tips.append("- Lis par blocs (10-15 min), puis fais une pause et résume\n");
            tips.append("- Cherche 1 exemple concret pour chaque idée importante\n\n");
        }

        tips.append("Astuce : Si tu bloques, relis la description du cours et identifie l'objectif principal du cours.");
        return tips.toString();
    }
}
