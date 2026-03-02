package Services.candidature;

import Entities.Offre.OffreEmploi;
import Entities.candidature.AnalyseResult;
import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Services.Offre.OfrreEmploiServices;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalyseIAService {

    private static final Gson GSON = new Gson();

    private static final String SYSTEM_PROMPT = (
            "Tu es un expert RH et recruteur technique senior.\n" +
            "Analyse la candidature par rapport à l'offre d'emploi fournie.\n" +
            "IMPORTANT: tu dois EXTRAIRE les compétences du candidat depuis le texte (skills + lettre de motivation).\n" +
            "Ensuite, tu dois évaluer les COMPÉTENCES DE L'OFFRE (requises + souhaitées) une par une.\n" +
            "Donc competences_techniques.items doit contenir EXACTEMENT les compétences listées dans l'offre (requises + souhaitées),\n" +
            "dans le même ordre.\n" +
            "Retourne UNIQUEMENT un objet JSON valide, sans texte avant ni après,\n" +
            "sans balises markdown, sans commentaires.\n\n" +
            "Règles de sortie:\n" +
            "- Le JSON doit être parseable strictement.\n" +
            "- competences_techniques.items: liste ordonnée, 1 compétence par ligne, nom OBLIGATOIRE non vide.\n" +
            "  * nom = une compétence de l'offre.\n" +
            "  * present=true  => compétence trouvée/confirmée dans la lettre/skills (✓).\n" +
            "  * present=false => compétence non trouvée ou non confirmée (✗).\n" +
            "  * niveau doit être l'une de: Confirmé, Intermédiaire, Débutant, Absent\n" +
            "- experience.commentaire / diplome_certifications.commentaire / soft_skills.commentaire: 1 phrase claire qui dit si c'est suffisant vs requis.\n" +
            "- points_ameliorer: inclure des formations/certifications concrètes à suivre (si manque), avec icone=warning ou error.\n\n" +
            "Structure JSON OBLIGATOIRE :\n" +
            "{\n" +
            "  \"score_global\": <int 0-100>,\n" +
            "  \"compatibilite\": <\"Élevée\" | \"Moyenne\" | \"Faible\">,\n" +
            "  \"recommandation\": <\"Entretien fortement conseillé\" | \"Entretien possible\" | \"Non recommandé\">,\n" +
            "  \"classement\": <\"Top 3\" | \"Top 10\" | \"Standard\">,\n" +
            "  \"classement_detail\": <string>,\n\n" +
            "  \"competences_techniques\": {\n" +
            "    \"score\": <int 0-100>,\n" +
            "    \"taux_couverture\": <int 0-100>,\n" +
            "    \"commentaire\": <string>,\n" +
            "    \"items\": [\n" +
            "      { \"nom\": <string non vide>, \"present\": <bool>, \"niveau\": <\"Confirmé\"|\"Intermédiaire\"|\"Débutant\"|\"Absent\"> }\n" +
            "    ]\n" +
            "  },\n\n" +
            "  \"experience\": {\n" +
            "    \"score\": <int 0-100>,\n" +
            "    \"annees_candidat\": <int>,\n" +
            "    \"annees_requis\": <int>,\n" +
            "    \"type_experience\": <string>,\n" +
            "    \"commentaire\": <string>\n" +
            "  },\n\n" +
            "  \"diplome_certifications\": {\n" +
            "    \"score\": <int 0-100>,\n" +
            "    \"diplome\": <string>,\n" +
            "    \"niveau_requis\": <string>,\n" +
            "    \"diplome_valide\": <bool>,\n" +
            "    \"commentaire\": <string>,\n" +
            "    \"certifications\": [\n" +
            "      { \"nom\": <string>, \"presente\": <bool> }\n" +
            "    ]\n" +
            "  },\n\n" +
            "  \"soft_skills\": {\n" +
            "    \"score\": <int 0-100>,\n" +
            "    \"items\": [<string>],\n" +
            "    \"commentaire\": <string>\n" +
            "  },\n\n" +
            "  \"points_forts\": [\n" +
            "    { \"icone\": <\"technique\"|\"experience\"|\"soft\"|\"diplome\">, \"mot_cle\": <string>, \"texte\": <string> }\n" +
            "  ],\n\n" +
            "  \"points_ameliorer\": [\n" +
            "    { \"icone\": <\"warning\"|\"error\">, \"texte\": <string> }\n" +
            "  ],\n\n" +
            "  \"analyse_globale\": <string>\n" +
            "}"
    );

    private final OpenAIService openAIService;
    private final OfrreEmploiServices offreService;

    public AnalyseIAService() {
        this.openAIService = new OpenAIService();
        this.offreService = new OfrreEmploiServices();
    }

    public AnalyseResult analyser(Candidature candidat) throws Exception {
        return analyser(candidat, null, null);
    }

    public AnalyseResult analyser(Candidature candidat, List<Experience> experiences, List<Certification> certifications) throws Exception {
        if (candidat == null) throw new IllegalArgumentException("candidat null");

        Offre offre = buildOffreFromDb(candidat);
        if (offre == null) {
            throw new IllegalArgumentException("Offre introuvable pour cette candidature (offreId=" + candidat.getOffreId() + ")");
        }

        String content = openAIService.chatCompletion(SYSTEM_PROMPT, buildUserPrompt(candidat, offre, experiences, certifications));
        String cleaned = stripMarkdownCodeFence(content);

        AnalyseResult result;
        try {
            result = GSON.fromJson(cleaned, AnalyseResult.class);
        } catch (Exception e) {
            throw new RuntimeException("JSON invalide renvoyé par l'IA. Contenu brut:\n" + content, e);
        }

        if (result == null) {
            throw new RuntimeException("Désérialisation AnalyseResult échouée. Contenu brut:\n" + content);
        }

        result._nom = candidat.getNom();
        result._prenom = candidat.getPrenom();
        result._offre_titre = offre.titre;
        result._raw_json = cleaned;

        return result;
    }

    private Offre buildOffreFromDb(Candidature candidat) {
        if (candidat == null) return null;
        Integer offreId = candidat.getOffreId();
        if (offreId == null) return null;

        OffreEmploi o = offreService.getOffreById(offreId);
        if (o == null) return null;

        String titre = safe(o.getTitre());
        String entreprise = safe(o.getNomEntreprise());
        if (entreprise.isBlank() && o.getIdEntreprise() != null) {
            entreprise = "Entreprise #" + o.getIdEntreprise();
        }

        String localisation = "";
        if (o.getLocalisation() != null) {
            String v = o.getLocalisation().getVille();
            String p = o.getLocalisation().getPays();
            String vv = v != null ? v.trim() : "";
            String pp = p != null ? p.trim() : "";
            localisation = (!vv.isEmpty() && !pp.isEmpty()) ? (vv + ", " + pp) : (!vv.isEmpty() ? vv : pp);
        }

        String typeContrat = o.getTypeContrat() != null ? safe(o.getTypeContrat().getNom()) : "";
        int anneesExperience = parseFirstInt(safe(o.getNiveauExperience()));
        String niveauEtudes = safe(o.getNiveauEtudes());
        String description = safe(o.getDescription());

        String[] competences = extractCompetences(titre + "\n" + description + "\n" + safe(o.getLanguesRequises()));

        return new Offre(
                o.getId(),
                titre,
                entreprise,
                localisation,
                typeContrat,
                anneesExperience,
                niveauEtudes,
                competences,
                new String[0],
                new String[0],
                new String[0],
                description
        );
    }

    private int parseFirstInt(String s) {
        if (s == null) return 0;
        Matcher m = Pattern.compile("(\\d+)").matcher(s);
        if (!m.find()) return 0;
        try {
            return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String[] extractCompetences(String text) {
        if (text == null) return new String[0];
        String t = text.toLowerCase(Locale.ROOT);

        String[] dict = new String[]{
                "java", "spring", "spring boot", "sql", "mysql", "postgres", "oracle",
                "docker", "kubernetes", "git", "rest", "api", "microservices", "jenkins",
                "react", "angular", "vue", "node", "javascript", "typescript", "aws", "azure", "gcp",
                "maven", "gradle", "hibernate", "jpa"
        };

        Set<String> out = new LinkedHashSet<>();
        for (String k : dict) {
            if (k == null || k.isBlank()) continue;
            String kk = k.toLowerCase(Locale.ROOT);
            if (t.contains(kk)) {
                out.add(normalizeCompetenceLabel(k));
            }
        }

        return out.toArray(new String[0]);
    }

    private String normalizeCompetenceLabel(String k) {
        if (k == null) return "";
        String s = k.trim();
        if (s.equalsIgnoreCase("spring")) return "Spring";
        if (s.equalsIgnoreCase("spring boot")) return "Spring Boot";
        if (s.equalsIgnoreCase("sql")) return "SQL";
        if (s.equalsIgnoreCase("mysql")) return "MySQL";
        if (s.equalsIgnoreCase("rest")) return "REST";
        if (s.equalsIgnoreCase("api")) return "API";
        if (s.equalsIgnoreCase("aws")) return "AWS";
        if (s.equalsIgnoreCase("gcp")) return "GCP";
        if (s.equalsIgnoreCase("git")) return "Git";
        if (s.equalsIgnoreCase("jpa")) return "JPA";
        if (s.equalsIgnoreCase("hibernate")) return "Hibernate";
        if (s.equalsIgnoreCase("javascript")) return "JavaScript";
        if (s.equalsIgnoreCase("typescript")) return "TypeScript";
        if (s.equalsIgnoreCase("microservices")) return "Microservices";
        return s;
    }

    private String buildUserPrompt(Candidature candidat, Offre offre, List<Experience> experiences, List<Certification> certifications) {
        String competences = candidat.getSkills_text() != null ? candidat.getSkills_text() : "";
        String lettre = candidat.getLettre_motivation() != null ? candidat.getLettre_motivation() : "";

        String expTxt = experiences == null || experiences.isEmpty()
                ? "(Aucune expérience enregistrée en base)"
                : experiences.stream().map(e -> {
                    String deb = e.getDate_debut() != null ? e.getDate_debut().toString() : "?";
                    String fin = e.getDate_fin() != null ? e.getDate_fin().toString() : "Aujourd'hui";
                    return "- " + safe(e.getPoste()) + " | " + safe(e.getEntreprise()) + " | " + deb + " -> " + fin;
                }).reduce((a, b) -> a + "\n" + b).orElse("");

        String certTxt = certifications == null || certifications.isEmpty()
                ? "(Aucune certification enregistrée en base)"
                : certifications.stream().map(c -> {
                    String obt = c.getDate_obtention() != null ? c.getDate_obtention().toString() : "?";
                    return "- " + safe(c.getNom_certification()) + " | " + safe(c.getOrganisme()) + " | obtenu: " + obt;
                }).reduce((a, b) -> a + "\n" + b).orElse("");

        return (
                "=== CANDIDATURE ===\n" +
                "Nom complet   : " + safe(candidat.getPrenom()) + " " + safe(candidat.getNom()) + "\n" +
                "Email         : " + safe(candidat.getEmail()) + "\n" +
                "Ville         : " + safe(candidat.getVille()) + "\n" +
                "Diplôme       : " + safe(candidat.getHighest_degree()) + "\n" +
                "Institution   : " + safe(candidat.getInstitution()) + "\n" +
                "Expériences DB :\n" + expTxt + "\n" +
                "Certifications DB :\n" + certTxt + "\n" +
                "Compétences   : " + competences + "\n" +
                "Lettre motiv. : " + lettre + "\n\n" +
                "=== OFFRE D'EMPLOI ===\n" +
                "Titre         : " + offre.titre + "\n" +
                "Entreprise    : " + offre.entreprise + "\n" +
                "Contrat       : " + offre.typeContrat + "\n" +
                "Expérience    : " + offre.anneesExperienceRequis + " ans minimum\n" +
                "Études requis : " + offre.niveauEtudesRequis + "\n" +
                "Compétences   : " + String.join(", ", offre.competencesRequises) + "\n" +
                "Optionnelles  : " + String.join(", ", offre.competencesSouhaitees) + "\n" +
                "Certifications: " + String.join(", ", offre.certificationsSouhaitees) + "\n" +
                "Soft skills   : " + String.join(", ", offre.softSkillsRequis) + "\n" +
                "Description   : " + offre.description + "\n\n" +
                "Analyse et retourne le JSON."
        );
    }

    private String stripMarkdownCodeFence(String content) {
        if (content == null) return "";
        String c = content.trim();
        if (!c.startsWith("```")) return c;

        String[] lines = c.split("\\r?\\n");
        if (lines.length <= 1) return c;

        int start = 1;
        int end = lines.length;
        if (lines[lines.length - 1].trim().equals("```")) {
            end = lines.length - 1;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            sb.append(lines[i]);
            if (i < end - 1) sb.append("\n");
        }
        return sb.toString().trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static class Offre {
        final int idOffre;
        final String titre;
        final String entreprise;
        final String localisation;
        final String typeContrat;
        final int anneesExperienceRequis;
        final String niveauEtudesRequis;
        final String[] competencesRequises;
        final String[] competencesSouhaitees;
        final String[] certificationsSouhaitees;
        final String[] softSkillsRequis;
        final String description;

        Offre(int idOffre,
              String titre,
              String entreprise,
              String localisation,
              String typeContrat,
              int anneesExperienceRequis,
              String niveauEtudesRequis,
              String[] competencesRequises,
              String[] competencesSouhaitees,
              String[] certificationsSouhaitees,
              String[] softSkillsRequis,
              String description) {
            this.idOffre = idOffre;
            this.titre = titre;
            this.entreprise = entreprise;
            this.localisation = localisation;
            this.typeContrat = typeContrat;
            this.anneesExperienceRequis = anneesExperienceRequis;
            this.niveauEtudesRequis = niveauEtudesRequis;
            this.competencesRequises = competencesRequises;
            this.competencesSouhaitees = competencesSouhaitees;
            this.certificationsSouhaitees = certificationsSouhaitees;
            this.softSkillsRequis = softSkillsRequis;
            this.description = description;
        }
    }
}
