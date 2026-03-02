package Services.Ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class GeminiAiService {

    private final HttpClient http = HttpClient.newHttpClient();

    private String apiKey() {
        // Clé API hardcodée (déconseillé pour production)
        return "AIzaSyBCpx9CN5ReBa8R2MEOaXiJVJgM-GimKQU";
    }

    public String generateOfferText(String poste, String niveau, String ville) {
        String prompt = "Rédige une description d'offre d'emploi professionnelle en français. " +
                "Texte continu sans numérotation ni titres de section. " +
                "Commence directement par 'Nous recherchons un(e) [poste]'. " +
                "Inclus naturellement dans le texte: les missions, les compétences requises, et le profil recherché. " +
                "Style: professionnel, attractif, concis, sans emojis. " +
                "Longueur: environ 300-500 caractères.\n\n" +
                "Poste: " + safe(poste) + "\n" +
                "Niveau: " + safe(niveau) + "\n" +
                "Ville: " + safe(ville);

        try {
            return callGemini(prompt);
        } catch (RuntimeException ex) {
            if (isQuotaException(ex)) {
                return localGenerateOfferText(poste, niveau, ville);
            }
            throw ex;
        }
    }

    public String improveText(String rawText) {
        String prompt = "Corrige et améliore ce texte d'offre d'emploi en français: " +
                "corrige les fautes, reformule de manière professionnelle, rend plus attractif. " +
                "Ne change pas le sens. Retourne uniquement le texte amélioré, sans commentaire.\n\n" +
                safe(rawText);

        try {
            return callGemini(prompt);
        } catch (RuntimeException ex) {
            if (isQuotaException(ex)) {
                return localImproveText(rawText);
            }
            throw ex;
        }
    }

    /**
     * Génère du texte à partir d'un prompt personnalisé
     * Utilisé pour les analyses de tendances et autres fonctionnalités avancées
     */
    public String generateText(String prompt) {
        try {
            return callGemini(prompt);
        } catch (RuntimeException ex) {
            if (isQuotaException(ex)) {
                return "Analyse non disponible (quota dépassé).";
            }
            throw ex;
        }
    }

    /**
     * Estime la fourchette salariale pour un poste basé sur la description, le secteur et le niveau d'expérience
     * @param titre Titre du poste
     * @param description Description de l'offre
     * @param secteur Secteur d'activité
     * @param niveauExperience Niveau d'expérience requis
     * @param localisation Ville/Pays du poste
     * @return Fourchette salariale estimée (ex: "2500-3500 TND")
     */
    public String estimateSalary(String titre, String description, String secteur, String niveauExperience, String localisation) {
        String prompt = "En tant qu'expert RH, estime la fourchette salariale mensuelle en TND (dinars tunisiens) pour ce poste. " +
                "Base-toi sur le titre, la description, le secteur et le niveau d'expérience. " +
                "Réponds UNIQUEMENT avec le format: 'XXXX-YYYY TND' (ex: '2500-3500 TND'). " +
                "Si tu ne peux pas estimer, réponds 'N/A'.\n\n" +
                "Titre: " + safe(titre) + "\n" +
                "Secteur: " + safe(secteur) + "\n" +
                "Niveau d'expérience: " + safe(niveauExperience) + "\n" +
                "Localisation: " + safe(localisation) + "\n" +
                "Description: " + safe(description).substring(0, Math.min(500, safe(description).length()));

        try {
            String response = callGemini(prompt);
            // Nettoyer la réponse
            response = response.replaceAll("(?i)fourchette\s*:\s*", "")
                    .replaceAll("(?i)salaire\s*:\s*", "")
                    .replaceAll("(?i)estimation\s*:\s*", "")
                    .replaceAll("\\*", "")
                    .trim();
            
            // Si la réponse ne contient pas TND, l'ajouter si c'est un nombre
            if (!response.toLowerCase().contains("tnd") && !response.equals("N/A") && !response.equals("n/a")) {
                // Essayer d'extraire les nombres
                response = response + " TND";
            }
            
            return response;
        } catch (RuntimeException ex) {
            if (isQuotaException(ex)) {
                return "Estimation indisponible (quota)";
            }
            return "N/A";
        }
    }

    private static boolean isQuotaException(RuntimeException ex) {
        if (ex == null || ex.getMessage() == null) {
            return false;
        }
        String m = ex.getMessage().toLowerCase();
        return m.contains("quota gemini") || m.contains("resource_exhausted") || m.contains("quota exceeded");
    }

    private static String localGenerateOfferText(String poste, String niveau, String ville) {
        String p = safe(poste);
        String n = safe(niveau);
        String v = safe(ville);
        if (p.isEmpty()) {
            p = "Poste";
        }
        String header = "Nous recherchons un(e) " + p + (n.isEmpty() ? "" : " " + n) +
                (v.isEmpty() ? "." : " à " + v + ".");

        return header + " Vous rejoindrez une équipe dynamique et participerez au développement et à l'amélioration de solutions fiables et maintenables. " +
                "Missions: Développer des fonctionnalités en respectant les bonnes pratiques, " +
                "corriger les anomalies et améliorer la performance, " +
                "participer aux revues de code et à la conception technique. " +
                "Compétences requises: Java, POO, principes SOLID, bases de données SQL, Git et travail collaboratif. " +
                "Profil recherché: Autonomie, rigueur, esprit d'équipe, bonne capacité d'analyse.";
    }

    private static String localImproveText(String rawText) {
        String s = rawText == null ? "" : rawText;
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        s = s.replaceAll("[ \t]+", " ").trim();
        if (s.isEmpty()) {
            return s;
        }
        s = s.replaceAll("\n{3,}", "\n\n");
        s = s.replaceAll("\\s+([,;:.!?])", "$1");
        s = s.replaceAll("([,;:.!?])([^\\s\\n])", "$1 $2");
        s = s.substring(0, 1).toUpperCase() + s.substring(1);
        return s;
    }

    private String callGemini(String prompt) {
        String[] apiVersions = new String[]{"v1beta", "v1"};

        String envModel = System.getenv("GEMINI_MODEL");
        String[] models = (envModel != null && !envModel.isBlank())
                ? new String[]{envModel.trim()}
                : new String[]{
                "gemini-1.5-flash",
                "gemini-1.5-pro",
                "gemini-2.0-flash",
                "gemini-pro"
        };

        String body = "{\"contents\":[{\"parts\":[{\"text\":" + jsonString(prompt) + "}]}]," +
                "\"generationConfig\":{\"temperature\":0.6,\"maxOutputTokens\":600}}";

        RuntimeException lastNotFound = null;
        for (String version : apiVersions) {
            for (String model : models) {
                try {
                    String endpoint = "https://generativelanguage.googleapis.com/" + version + "/models/" + model + ":generateContent?key=" + apiKey();

                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(endpoint))
                            .header("Content-Type", "application/json; charset=UTF-8")
                            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                            .build();

                    HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (res.statusCode() >= 200 && res.statusCode() < 300) {
                        return extractText(res.body());
                    }

                    if (isQuotaError(res.statusCode(), res.body())) {
                        String retry = extractRetrySeconds(res.body());
                        String msg = "Quota Gemini dépassé (free tier).";
                        if (retry != null) {
                            msg += " Réessaie dans " + retry + " secondes.";
                        } else {
                            msg += " Réessaie plus tard.";
                        }
                        msg += "\n\nSolutions: utiliser une autre clé/projet, activer la facturation, ou réduire la fréquence d'appels.";
                        throw new RuntimeException(msg);
                    }

                    if (res.statusCode() == 404) {
                        lastNotFound = new RuntimeException("Erreur Gemini HTTP 404 (model not found): " + model + " @" + version + ": " + res.body());
                        continue;
                    }

                    throw new RuntimeException("Erreur Gemini HTTP " + res.statusCode() + ": " + res.body());
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Erreur appel Gemini: " + e.getMessage(), e);
                }
            }
        }

        if (lastNotFound != null) {
            throw new RuntimeException(
                    (lastNotFound.getMessage() != null ? lastNotFound.getMessage() : "Model Gemini introuvable") +
                            "\n\nAstuce: définis la variable d'environnement GEMINI_MODEL (ex: gemini-pro) selon les modèles disponibles sur ton projet.");
        }

        throw new RuntimeException("Erreur Gemini: impossible d'appeler generateContent");
    }

    private static boolean isQuotaError(int statusCode, String body) {
        if (statusCode == 429) {
            return true;
        }
        if (body == null) {
            return false;
        }
        String b = body.toLowerCase();
        return b.contains("resource_exhausted") || b.contains("quota exceeded") || b.contains("quotaexceeded");
    }

    private static String extractRetrySeconds(String body) {
        if (body == null) {
            return null;
        }
        String marker = "Please retry in ";
        int idx = body.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        int end = start;
        while (end < body.length() && (Character.isDigit(body.charAt(end)) || body.charAt(end) == '.' )) {
            end++;
        }
        if (end <= start) {
            return null;
        }
        String num = body.substring(start, end);
        int dot = num.indexOf('.');
        return dot >= 0 ? num.substring(0, dot) : num;
    }

    // Minimal JSON escaping for string literal
    private static String jsonString(String s) {
        if (s == null) {
            return "\"\"";
        }
        String out = s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
        return "\"" + out + "\"";
    }

    private static String safe(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }

    // Very small JSON extractor to avoid new dependencies.
    // Extracts first candidate text: candidates[0].content.parts[0].text
    private static String extractText(String json) {
        if (json == null) {
            return "";
        }
        String marker = "\"text\":";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return json;
        }
        int start = idx + marker.length();
        // skip whitespace
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            return json;
        }
        start++;
        StringBuilder sb = new StringBuilder();
        boolean esc = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (esc) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> {
                    }
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    default -> sb.append(c);
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
        return sb.toString().trim();
    }
}
