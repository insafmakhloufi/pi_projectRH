package Services;

import Services.Ai.GeminiAiService;
import Utils.Mydatabase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class TendancesCompetencesService {
    
    private Connection con = Mydatabase.getInstance().getConnection();
    private GeminiAiService geminiService;
    
    public TendancesCompetencesService() {
        this.geminiService = new GeminiAiService();
    }
    
    /**
     * Analyse les compétences les plus demandées par secteur
     * Utilise SQL pour l'extraction et Gemini pour l'analyse intelligente
     */
    public Map<String, List<CompetenceTrend>> getTendancesParSecteur() {
        Map<String, List<CompetenceTrend>> tendances = new HashMap<>();
        
        // Récupérer les descriptions d'offres par secteur
        String req = "SELECT s.nom_secteur, o.description, o.titre, o.niveau_experience " +
                "FROM offreemlpoi o " +
                "LEFT JOIN secteuractivite s ON o.id_secteur = s.id_secteur " +
                "WHERE o.status = 'active' " +
                "ORDER BY s.nom_secteur";
        
        Map<String, List<String>> descriptionsParSecteur = new HashMap<>();
        
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String secteur = rs.getString("nom_secteur");
                if (secteur == null) secteur = "Non classé";
                
                String description = rs.getString("description");
                String titre = rs.getString("titre");
                
                if (!descriptionsParSecteur.containsKey(secteur)) {
                    descriptionsParSecteur.put(secteur, new ArrayList<>());
                }
                descriptionsParSecteur.get(secteur).add(titre + " : " + description);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur extraction données: " + e.getMessage(), e);
        }
        
        // Pour chaque secteur, utiliser Gemini pour extraire les compétences
        for (Map.Entry<String, List<String>> entry : descriptionsParSecteur.entrySet()) {
            String secteur = entry.getKey();
            List<String> descriptions = entry.getValue();
            
            try {
                List<CompetenceTrend> competences = analyserCompetencesAvecGemini(secteur, descriptions);
                tendances.put(secteur, competences);
            } catch (Exception e) {
                // Fallback: extraction simple par mot-clé
                List<CompetenceTrend> competencesSimples = extraireCompetencesSimples(descriptions);
                tendances.put(secteur, competencesSimples);
            }
        }
        
        return tendances;
    }
    
    /**
     * Utilise Gemini pour analyser et extraire les compétences pertinentes
     */
    private List<CompetenceTrend> analyserCompetencesAvecGemini(String secteur, List<String> descriptions) {
        // Limiter à 10 descriptions pour ne pas surcharger l'API
        String texteEchantillon = descriptions.stream()
                .limit(10)
                .collect(Collectors.joining("\n---\n"));
        
        String prompt = "Analyse ces offres d'emploi du secteur '" + secteur + "' et identifie les 5 compétences techniques les plus demandées. " +
                "Réponds UNIQUEMENT au format: COMPETENCE|FREQUENCE (ex: Java|8, Python|5, Spring|4, Angular|3, SQL|6). " +
                "N'invente pas de compétences, base-toi uniquement sur le texte. " +
                "Texte des offres:\n" + texteEchantillon.substring(0, Math.min(2000, texteEchantillon.length()));
        
        try {
            String reponse = geminiService.generateText(prompt);
            return parserReponseGemini(reponse);
        } catch (Exception e) {
            throw new RuntimeException("Erreur analyse Gemini: " + e.getMessage());
        }
    }
    
    /**
     * Fallback: extraction simple par mots-clés courants
     */
    private List<CompetenceTrend> extraireCompetencesSimples(List<String> descriptions) {
        // Liste de compétences techniques courantes à rechercher
        String[] competencesTechniques = {
            "Java", "Python", "JavaScript", "C++", "C#", "PHP", "Ruby", "Go", "Rust",
            "Spring", "Hibernate", "Angular", "React", "Vue", "Node.js", "Express",
            "SQL", "MySQL", "PostgreSQL", "MongoDB", "Oracle", "Redis",
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Linux",
            "Git", "Jenkins", "GitLab", "CI/CD", "DevOps",
            "Machine Learning", "Data Science", "TensorFlow", "PyTorch",
            "HTML", "CSS", "Bootstrap", "Tailwind", "SASS",
            "REST", "GraphQL", "SOAP", "Microservices", "API"
        };
        
        Map<String, Integer> compteur = new HashMap<>();
        String texteGlobal = String.join(" ", descriptions).toLowerCase();
        
        for (String competence : competencesTechniques) {
            int count = compterOccurrences(texteGlobal, competence.toLowerCase());
            if (count > 0) {
                compteur.put(competence, count);
            }
        }
        
        return compteur.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> new CompetenceTrend(e.getKey(), e.getValue(), "Frequent"))
                .collect(Collectors.toList());
    }
    
    private int compterOccurrences(String texte, String mot) {
        int count = 0;
        int index = 0;
        while ((index = texte.indexOf(mot, index)) != -1) {
            count++;
            index += mot.length();
        }
        return count;
    }
    
    private List<CompetenceTrend> parserReponseGemini(String reponse) {
        List<CompetenceTrend> result = new ArrayList<>();
        
        // Format attendu: "Java|8, Python|5, Spring|4"
        String[] parties = reponse.split("[,;]");
        for (String partie : parties) {
            partie = partie.trim();
            if (partie.contains("|")) {
                String[] elements = partie.split("\\|");
                if (elements.length >= 2) {
                    String competence = elements[0].trim();
                    try {
                        int frequence = Integer.parseInt(elements[1].trim().replaceAll("[^0-9]", ""));
                        result.add(new CompetenceTrend(competence, frequence, "Gemini"));
                    } catch (NumberFormatException ignored) {
                        // Ignorer les entrées mal formatées
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Récupère les compétences les plus demandées globalement
     */
    public List<CompetenceTrend> getTopCompetencesGlobales(int limit) {
        String req = "SELECT description, titre FROM offreemlpoi WHERE status = 'active' LIMIT 100";
        List<String> descriptions = new ArrayList<>();
        
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                descriptions.add(rs.getString("titre") + " " + rs.getString("description"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur extraction: " + e.getMessage(), e);
        }
        
        List<CompetenceTrend> toutes = extraireCompetencesSimples(descriptions);
        return toutes.stream().limit(limit).collect(Collectors.toList());
    }
    
    /**
     * Classe représentant une compétence tendance
     */
    public static class CompetenceTrend {
        private String competence;
        private int frequence;
        private String source; // "Gemini" ou "Keyword"
        
        public CompetenceTrend(String competence, int frequence, String source) {
            this.competence = competence;
            this.frequence = frequence;
            this.source = source;
        }
        
        public String getCompetence() { return competence; }
        public int getFrequence() { return frequence; }
        public String getSource() { return source; }
        
        @Override
        public String toString() {
            return competence + " (" + frequence + ")";
        }
    }
}
