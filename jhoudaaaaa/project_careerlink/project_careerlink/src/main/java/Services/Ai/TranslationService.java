package Services.Ai;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Service de traduction et internationalisation des offres d'emploi
 * Supporte FR ↔ EN ↔ AR avec adaptation culturelle
 */
public class TranslationService {
    
    private final GeminiAiService geminiService;
    private OpenAiService openAiService;
    private final HttpClient http = HttpClient.newHttpClient();
    
    public TranslationService() {
        this.geminiService = new GeminiAiService();
    }
    
    public TranslationService(GeminiAiService geminiService) {
        this.geminiService = geminiService;
    }

    private OpenAiService getOpenAiService() {
        if (openAiService == null) {
            openAiService = new OpenAiService();
        }
        return openAiService;
    }
    
    /**
     * Traduit une offre d'emploi complète
     * @param titre Titre de l'offre
     * @param description Description de l'offre
     * @param langueSource Langue source (auto-détectée si null)
     * @param langueCible Langue cible: "fr", "en", "ar"
     * @param paysCible Pays cible pour adaptation culturelle (ex: "tunisia", "france", "uae")
     * @return Offre traduite
     */
    public OffreTraduite traduireOffre(String titre, String description, String langueSource, 
                                       String langueCible, String paysCible) {
        try {
            // Détection auto de la langue source si non spécifiée
            if (langueSource == null || langueSource.isBlank()) {
                langueSource = detecterLangue(titre + " " + description);
            }
            
            // Éviter traduction inutile
            if (langueSource.equalsIgnoreCase(langueCible)) {
                return new OffreTraduite(titre, description, langueSource, langueCible, true);
            }
            
            String prompt = construirePromptTraduction(titre, description, langueSource, 
                                                        langueCible, paysCible);
            
            String reponse;
            // Stratégie: OpenAI en priorité (plus stable côté quota), sinon Gemini
            boolean hasOpenAi = hasOpenAiKey();
            
            if (hasOpenAi) {
                try {
                    reponse = getOpenAiService().generateText(prompt);
                    if (isUnavailableResponse(reponse)) {
                        throw new RuntimeException("OpenAI indisponible");
                    }
                } catch (Exception openAiEx) {
                    // fallback Gemini
                    try {
                        reponse = geminiService.generateText(prompt);
                        if (isUnavailableResponse(reponse)) {
                            OffreTraduite fallback = traduireOffreViaMyMemory(titre, description, langueSource, langueCible);
                            if (fallback != null && fallback.isSucces()) {
                                return fallback;
                            }
                            return new OffreTraduite(titre, description, langueSource, langueCible, false);
                        }
                    } catch (Exception geminiEx) {
                        OffreTraduite fallback = traduireOffreViaMyMemory(titre, description, langueSource, langueCible);
                        if (fallback != null && fallback.isSucces()) {
                            return fallback;
                        }
                        return new OffreTraduite(titre, description, langueSource, langueCible, false);
                    }
                }
            } else {
                reponse = geminiService.generateText(prompt);
                if (isUnavailableResponse(reponse)) {
                    OffreTraduite fallback = traduireOffreViaMyMemory(titre, description, langueSource, langueCible);
                    if (fallback != null && fallback.isSucces()) {
                        return fallback;
                    }
                    return new OffreTraduite(titre, description, langueSource, langueCible, false);
                }
            }

            OffreTraduite parsed = parserReponseTraduction(reponse, langueSource, langueCible);
            if (parsed == null || !parsed.isSucces()) {
                return new OffreTraduite(titre, description, langueSource, langueCible, false);
            }
            if (isUnavailableResponse(parsed.getTitre()) || isUnavailableResponse(parsed.getDescription())) {
                return new OffreTraduite(titre, description, langueSource, langueCible, false);
            }
            return parsed;
            
        } catch (Exception e) {
            // Fallback: retourner l'original avec erreur
            return new OffreTraduite(titre, description, langueSource, langueCible, false);
        }
    }

    private OffreTraduite traduireOffreViaMyMemory(String titre, String description, String langueSource, String langueCible) {
        try {
            String src = (langueSource == null || langueSource.isBlank()) ? "fr" : langueSource.trim().toLowerCase(Locale.ROOT);
            String tgt = (langueCible == null || langueCible.isBlank()) ? "fr" : langueCible.trim().toLowerCase(Locale.ROOT);
            if (src.equals(tgt)) {
                return new OffreTraduite(titre, description, src, tgt, true);
            }

            String textTitre = safe(titre);
            String textDesc = safe(description);
            if (textTitre.isBlank() && textDesc.isBlank()) {
                return new OffreTraduite(titre, description, src, tgt, false);
            }

            String t1 = textTitre.isBlank() ? "" : translateMyMemory(textTitre, src, tgt);
            String t2 = textDesc.isBlank() ? "" : translateMyMemory(textDesc, src, tgt);

            if ((textTitre.isBlank() || (t1 != null && !t1.isBlank())) && (textDesc.isBlank() || (t2 != null && !t2.isBlank()))) {
                String outTitre = textTitre.isBlank() ? "" : t1;
                String outDesc = textDesc.isBlank() ? "" : t2;
                if (outTitre == null) outTitre = "";
                if (outDesc == null) outDesc = "";
                if (!outTitre.isBlank() && !outDesc.isBlank()) {
                    return new OffreTraduite(outTitre.trim(), outDesc.trim(), src, tgt, true);
                }
            }
            return new OffreTraduite(titre, description, src, tgt, false);
        } catch (Exception e) {
            return new OffreTraduite(titre, description, langueSource, langueCible, false);
        }
    }

    private String translateMyMemory(String text, String src, String tgt) {
        try {
            if (text == null) {
                return null;
            }
            String normalized = text;
            // MyMemory free endpoint has a ~500 chars limit per query.
            // We chunk long texts to avoid: "QUERY LENGTH LIMIT EXCEEDED. MAX ALLOWED QUERY : 500 CHARS"
            if (normalized.length() > 450) {
                return translateMyMemoryChunked(normalized, src, tgt);
            }

            String q = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String langpair = URLEncoder.encode(src + "|" + tgt, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q=" + q + "&langpair=" + langpair;

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                return null;
            }
            String translated = extractMyMemoryTranslatedText(res.body());
            if (translated == null) {
                return null;
            }
            String upper = translated.toUpperCase(Locale.ROOT);
            if (upper.contains("QUERY LENGTH LIMIT EXCEEDED")) {
                // Should not happen if chunking works, but guard anyway.
                return null;
            }
            return translated;
        } catch (Exception e) {
            return null;
        }
    }

    private String translateMyMemoryChunked(String text, String src, String tgt) {
        if (text == null) {
            return null;
        }

        // Preserve paragraphs to keep readable output
        String[] paragraphs = text.split("\\n", -1);
        StringBuilder out = new StringBuilder();
        for (int p = 0; p < paragraphs.length; p++) {
            String para = paragraphs[p];
            String translatedPara = translateMyMemoryChunkedOneParagraph(para, src, tgt);
            if (translatedPara == null) {
                return null;
            }
            out.append(translatedPara);
            if (p < paragraphs.length - 1) {
                out.append("\n");
            }
        }
        String result = out.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String translateMyMemoryChunkedOneParagraph(String paragraph, String src, String tgt) {
        if (paragraph == null) {
            return "";
        }
        String para = paragraph.trim();
        if (para.isEmpty()) {
            return "";
        }

        // Split by sentences first (keep it simple)
        String[] sentences = para.split("(?<=[.!?])\\s+");
        StringBuilder buffer = new StringBuilder();
        StringBuilder translated = new StringBuilder();

        for (String s : sentences) {
            String part = s == null ? "" : s.trim();
            if (part.isEmpty()) {
                continue;
            }

            if (buffer.length() == 0) {
                buffer.append(part);
            } else if (buffer.length() + 1 + part.length() <= 450) {
                buffer.append(' ').append(part);
            } else {
                String t = translateMyMemory(buffer.toString(), src, tgt);
                if (t == null || t.isBlank()) {
                    return null;
                }
                if (translated.length() > 0) {
                    translated.append(' ');
                }
                translated.append(t.trim());
                buffer.setLength(0);
                buffer.append(part);
            }
        }

        if (buffer.length() > 0) {
            String t = translateMyMemory(buffer.toString(), src, tgt);
            if (t == null || t.isBlank()) {
                return null;
            }
            if (translated.length() > 0) {
                translated.append(' ');
            }
            translated.append(t.trim());
        }

        String result = translated.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String extractMyMemoryTranslatedText(String json) {
        if (json == null) {
            return null;
        }
        String marker = "\"translatedText\":";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        start++;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': break;
                    case '\\': sb.append('\\'); break;
                    case '"': sb.append('"'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                int code = Integer.parseInt(hex, 16);
                                sb.append((char) code);
                                i += 4;
                            } catch (Exception ex) {
                                sb.append('u');
                            }
                        } else {
                            sb.append('u');
                        }
                        break;
                    default: sb.append(c);
                }
                esc = false;
                continue;
            }
            if (c == '\\') {
                esc = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        String out = sb.toString().trim();
        return out.isEmpty() ? null : out;
    }
    
    /**
     * Traduit uniquement le titre
     */
    public String traduireTitre(String titre, String langueCible, String paysCible) {
        String prompt = "Traduis ce titre de poste en " + getNomLangue(langueCible) + 
                (paysCible != null ? " pour le marché " + paysCible : "") + ". " +
                "Réponds UNIQUEMENT avec le titre traduit, sans explication.\n\n" +
                "Titre: " + safe(titre);
        
        try {
            return geminiService.generateText(prompt).trim();
        } catch (Exception e) {
            return titre;
        }
    }
    
    /**
     * Traduit uniquement la description
     */
    public String traduireDescription(String description, String langueCible, String paysCible) {
        String prompt = "Traduis cette description d'offre d'emploi en " + getNomLangue(langueCible) + 
                (paysCible != null ? " pour le marché " + paysCible : "") + ". " +
                "Adapte le ton professionnel selon les normes culturelles locales. " +
                "Réponds UNIQUEMENT avec la description traduite, sans explication.\n\n" +
                "Description:\n" + safe(description);
        
        try {
            return geminiService.generateText(prompt).trim();
        } catch (Exception e) {
            return description;
        }
    }
    
    /**
     * Détecte la langue d'un texte
     */
    public String detecterLangue(String texte) {
        if (texte == null || texte.trim().isEmpty()) {
            return "fr";
        }
        
        // Détection simple par mots-clés
        String lower = texte.toLowerCase();
        
        // Arabe: présence de caractères arabes
        if (texte.matches(".*[\u0600-\u06FF].*")) {
            return "ar";
        }
        
        // Français: mots caractéristiques
        String[] motsFr = {"le", "la", "les", "un", "une", "des", "et", "pour", "dans", "sur", "avec", "nous", "vous", "poste", "emploi", "recherchons", "compétences", "profil"};
        int scoreFr = compterOccurrences(lower, motsFr);
        
        // Anglais: mots caractéristiques
        String[] motsEn = {"the", "a", "an", "and", "for", "in", "on", "with", "we", "you", "job", "position", "looking", "skills", "experience", "required"};
        int scoreEn = compterOccurrences(lower, motsEn);
        
        if (scoreFr > scoreEn) return "fr";
        if (scoreEn > scoreFr) return "en";
        return "fr"; // Défaut
    }
    
    /**
     * Obtient les instructions d'adaptation culturelle
     */
    private String getInstructionsCulturelles(String paysCible) {
        if (paysCible == null) return "";
        
        return switch (paysCible.toLowerCase()) {
            case "tunisia", "tunisie", "maroc", "morocco", "algeria", "algérie" ->
                "Utilise un ton professionnel mais chaleureux. Privilégie les formules de politesse arabes si traduction en arabe. " +
                "Mentionne la stabilité et la sécurité de l'emploi. ";
            case "france", "fr", "belgium", "belgique", "canada", "qc", "québec" ->
                "Utilise un ton professionnel formel. Respecte la structure classique des offres françaises. " +
                "Mentionne les avantages sociaux et la qualité de vie au travail. ";
            case "uae", "emirates", "saudi", "saudi arabia", "qatar", "kuwait", "bahrain" ->
                "Utilise un ton très respectueux et formel. Privilégie les titres honorifiques. " +
                "Mentionne les avantages attractifs (logement, transport). ";
            case "usa", "uk", "canada_en", "australia" ->
                "Utilise un ton direct et dynamique. Met en avant l'innovation et la croissance. " +
                "Mentionne les opportunités de développement de carrière. ";
            default -> "";
        };
    }
    
    private String construirePromptTraduction(String titre, String description, String langueSource,
                                              String langueCible, String paysCible) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Traduis cette offre d'emploi de ")
              .append(getNomLangue(langueSource))
              .append(" vers ")
              .append(getNomLangue(langueCible));
        
        if (paysCible != null) {
            prompt.append(" pour le marché ").append(paysCible);
        }
        
        prompt.append(".\n\n");
        
        // Instructions culturelles
        String instructionsCulturelles = getInstructionsCulturelles(paysCible);
        if (!instructionsCulturelles.isEmpty()) {
            prompt.append("Instructions culturelles: ").append(instructionsCulturelles).append("\n\n");
        }
        
        // Format de réponse attendu
        prompt.append("Format de réponse obligatoire:\n");
        prompt.append("TITRE: [titre traduit]\n");
        prompt.append("DESCRIPTION: [description traduite]\n\n");
        
        // Contenu à traduire
        prompt.append("Texte à traduire:\n");
        prompt.append("TITRE: ").append(safe(titre)).append("\n");
        prompt.append("DESCRIPTION: ").append(safe(description)).append("\n");
        
        return prompt.toString();
    }
    
    private OffreTraduite parserReponseTraduction(String reponse, String langueSource, String langueCible) {
        if (isUnavailableResponse(reponse)) {
            return new OffreTraduite("", "", langueSource, langueCible, false);
        }

        String titreTraduit = "";
        String descriptionTraduite = "";
        
        String[] lignes = reponse.split("\n");
        boolean dansDescription = false;
        StringBuilder descBuilder = new StringBuilder();
        
        for (String ligne : lignes) {
            ligne = ligne.trim();
            
            if (ligne.startsWith("TITRE:") || ligne.startsWith("Titre:")) {
                titreTraduit = ligne.substring(ligne.indexOf(":") + 1).trim();
                dansDescription = false;
            } else if (ligne.startsWith("DESCRIPTION:") || ligne.startsWith("Description:")) {
                descBuilder.append(ligne.substring(ligne.indexOf(":") + 1).trim());
                dansDescription = true;
            } else if (dansDescription && !ligne.isEmpty()) {
                descBuilder.append(" ").append(ligne);
            }
        }
        
        descriptionTraduite = descBuilder.toString().trim();
        
        // Échec si parsing échoue
        if (titreTraduit.isEmpty() || descriptionTraduite.isEmpty()) {
            return new OffreTraduite("", "", langueSource, langueCible, false);
        }

        if (isUnavailableResponse(titreTraduit) || isUnavailableResponse(descriptionTraduite)) {
            return new OffreTraduite("", "", langueSource, langueCible, false);
        }

        return new OffreTraduite(titreTraduit, descriptionTraduite, langueSource, langueCible, true);
    }

    private static boolean isUnavailableResponse(String text) {
        if (text == null) {
            return true;
        }
        String t = text.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) {
            return true;
        }
        return t.contains("analyse non disponible")
                || t.contains("quota")
                || t.contains("resource_exhausted")
                || t.contains("quota depasse")
                || t.contains("quota dépassé")
                || t.equals("n/a")
                || t.equals("na");
    }

    private static boolean hasOpenAiKey() {
        // Clé API hardcodée dans OpenAiService - toujours disponible
        return true;
    }
    
    private String getNomLangue(String code) {
        return switch (code.toLowerCase()) {
            case "fr" -> "français";
            case "en" -> "anglais";
            case "ar" -> "arabe";
            default -> code;
        };
    }
    
    private int compterOccurrences(String texte, String[] mots) {
        int count = 0;
        for (String mot : mots) {
            if (texte.contains(mot)) count++;
        }
        return count;
    }
    
    private String safe(String s) {
        return s == null ? "" : s;
    }
    
    /**
     * Classe interne représentant une offre traduite
     */
    public static class OffreTraduite {
        private final String titre;
        private final String description;
        private final String langueSource;
        private final String langueCible;
        private final boolean succes;
        
        public OffreTraduite(String titre, String description, String langueSource, 
                            String langueCible, boolean succes) {
            this.titre = titre;
            this.description = description;
            this.langueSource = langueSource;
            this.langueCible = langueCible;
            this.succes = succes;
        }
        
        public String getTitre() { return titre; }
        public String getDescription() { return description; }
        public String getLangueSource() { return langueSource; }
        public String getLangueCible() { return langueCible; }
        public boolean isSucces() { return succes; }
        
        @Override
        public String toString() {
            return "OffreTraduite{" + langueSource + "→" + langueCible + ", titre='" + titre + "'}";
        }
    }
}
