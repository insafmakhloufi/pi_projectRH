package Services.Export;

import Entities.Offre.OffreEmploi;
import Services.Offre.OfrreEmploiServices;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Service de Newsletter hebdomadaire des offres d'emploi
 * Envoie un email récapitulatif des nouvelles offres
 */
public class NewsletterService {

    private final OfrreEmploiServices offreService;

    public NewsletterService() {
        this.offreService = new OfrreEmploiServices();
    }

    /**
     * Génère le contenu HTML de la newsletter
     * @param offres Liste des offres à inclure
     * @return Contenu HTML formaté
     */
    public String generateNewsletterHtml(List<OffreEmploi> offres) {
        StringBuilder html = new StringBuilder();

        // En-tête
        html.append("<!DOCTYPE html>");
        html.append("<html><head>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Arial, sans-serif; margin: 0; padding: 20px; background: #f8fafc; }");
        html.append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; color: white; }");
        html.append(".header h1 { margin: 0; font-size: 24px; }");
        html.append(".header p { margin: 10px 0 0; opacity: 0.9; }");
        html.append(".content { padding: 30px; }");
        html.append(".offer { border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin-bottom: 20px; }");
        html.append(".offer-title { color: #2563eb; font-size: 18px; font-weight: bold; margin-bottom: 8px; }");
        html.append(".offer-company { color: #64748b; font-size: 14px; margin-bottom: 12px; }");
        html.append(".offer-meta { display: flex; gap: 15px; flex-wrap: wrap; font-size: 13px; color: #475569; }");
        html.append(".offer-meta span { background: #f1f5f9; padding: 4px 10px; border-radius: 20px; }");
        html.append(".offer-desc { color: #334155; font-size: 14px; line-height: 1.6; margin-top: 12px; }");
        html.append(".cta-button { display: inline-block; background: #2563eb; color: white; text-decoration: none; padding: 12px 24px; border-radius: 8px; margin-top: 30px; font-weight: bold; }");
        html.append(".footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #64748b; }");
        html.append("</style>");
        html.append("</head><body>");

        // Conteneur principal
        html.append("<div class='container'>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>📧 Newsletter CareerLink</h1>");
        html.append("<p>Nouvelles offres d'emploi du ")
              .append(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
              .append("</p>");
        html.append("</div>");

        // Contenu
        html.append("<div class='content'>");
        html.append("<h2 style='color: #0f172a; margin-top: 0;'>")
              .append(offres.size())
              .append(" nouvelle(s) offre(s) cette semaine</h2>");

        if (offres.isEmpty()) {
            html.append("<p style='color: #64748b; text-align: center; padding: 40px 0;'>");
            html.append("Aucune nouvelle offre cette semaine. Revenez bientôt !");
            html.append("</p>");
        } else {
            for (OffreEmploi offre : offres) {
                html.append(generateOfferCard(offre));
            }
        }

        // CTA
        html.append("<div style='text-align: center;'>");
        html.append("<a href='https://careerlink.tn/offres' class='cta-button'>Voir toutes les offres</a>");
        html.append("</div>");

        html.append("</div>"); // /content

        // Footer
        html.append("<div class='footer'>");
        html.append("© 2025 CareerLink - Votre carrière commence ici<br>");
        html.append("<a href='#' style='color: #64748b;'>Se désabonner</a>");
        html.append("</div>");

        html.append("</div>"); // /container
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Génère la carte HTML d'une offre
     */
    private String generateOfferCard(OffreEmploi offre) {
        StringBuilder card = new StringBuilder();
        card.append("<div class='offer'>");

        // Titre
        card.append("<div class='offer-title'>").append(escapeHtml(offre.getTitre())).append("</div>");

        // Entreprise
        String company = offre.getNomEntreprise();
        if (company != null && !company.isBlank()) {
            card.append("<div class='offer-company'>chez ").append(escapeHtml(company)).append("</div>");
        }

        // Métadonnées
        card.append("<div class='offer-meta'>");

        String location = getLocationText(offre);
        if (!location.equals("Non spécifiée")) {
            card.append("<span>📍 ").append(escapeHtml(location)).append("</span>");
        }

        String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
        if (contrat != null) {
            card.append("<span>📋 ").append(escapeHtml(contrat)).append("</span>");
        }

        String exp = offre.getNiveauExperience();
        if (exp != null && !exp.isBlank()) {
            card.append("<span>⭐ ").append(escapeHtml(exp)).append("</span>");
        }

        card.append("</div>");

        // Description (tronquée)
        String desc = offre.getDescription();
        if (desc != null && !desc.isBlank()) {
            String truncated = desc.length() > 150 ? desc.substring(0, 150) + "..." : desc;
            card.append("<div class='offer-desc'>").append(escapeHtml(truncated)).append("</div>");
        }

        card.append("</div>");
        return card.toString();
    }

    /**
     * Récupère les offres de la semaine dernière
     */
    public List<OffreEmploi> getWeeklyOffers() {
        LocalDate oneWeekAgo = LocalDate.now().minusDays(7);

        List<OffreEmploi> allOffres = offreService.afficherOfrreEmploiLight();

        return allOffres.stream()
                .filter(o -> o.getDateLimiteCandidature() != null)
                .filter(o -> o.getDateLimiteCandidature().isAfter(oneWeekAgo))
                .collect(Collectors.toList());
    }

    /**
     * Configure les propriétés SMTP pour l'envoi d'emails
     * @return Propriétés mail configurées
     */
    public Properties getMailProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        return props;
    }

    private String getLocationText(OffreEmploi o) {
        if (o == null || o.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String v = o.getLocalisation().getVille();
        String p = o.getLocalisation().getPays();
        StringBuilder sb = new StringBuilder();
        if (v != null && !v.trim().isEmpty()) sb.append(v);
        if (p != null && !p.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(p);
        }
        return sb.length() == 0 ? "Non spécifiée" : sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
