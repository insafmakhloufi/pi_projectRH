package Services.Export;

import Entities.Offre.OffreEmploi;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service d'export PDF stylisé des offres d'emploi
 * Utilise OpenPDF (fork libre d'iText)
 */
public class PdfExportService {

    private static final Color PRIMARY_COLOR = new Color(37, 99, 235); // #2563eb
    private static final Color SECONDARY_COLOR = new Color(100, 116, 139); // #64748b
    private static final Color LIGHT_BG = new Color(248, 250, 252); // #f8fafc
    private static final Color DARK_TEXT = new Color(15, 23, 42); // #0f172a

    /**
     * Exporte une offre d'emploi en PDF stylisé
     * @param offre L'offre à exporter
     * @param outputFile Fichier de sortie
     * @throws IOException Si erreur d'écriture
     */
    public void exportOffreToPdf(OffreEmploi offre, File outputFile) throws IOException {
        Document document = new Document(PageSize.A4, 50, 50, 60, 50);
        FileOutputStream fos = new FileOutputStream(outputFile);
        PdfWriter writer = PdfWriter.getInstance(document, fos);
        document.open();

        try {
            // En-tête avec logo CareerLink
            addHeader(document);

            // Titre de l'offre
            addTitle(document, offre);

            // Séparateur
            addSeparator(document);

            // Informations clés (grille)
            addKeyInfo(document, offre);

            // Description
            addDescription(document, offre);

            // Footer avec QR Code placeholder et date
            addFooter(document, offre);

        } finally {
            document.close();
            fos.close();
        }
    }

    private void addHeader(Document document) throws DocumentException {
        // Logo texte CareerLink
        Paragraph logo = new Paragraph();
        logo.add(new Chunk("CAREER", new Font(Font.HELVETICA, 24, Font.BOLD, PRIMARY_COLOR)));
        logo.add(new Chunk("LINK", new Font(Font.HELVETICA, 24, Font.BOLD, SECONDARY_COLOR)));
        logo.setAlignment(Element.ALIGN_CENTER);
        document.add(logo);

        // Slogan
        Paragraph slogan = new Paragraph("Votre carrière commence ici",
                new Font(Font.HELVETICA, 10, Font.ITALIC, SECONDARY_COLOR));
        slogan.setAlignment(Element.ALIGN_CENTER);
        slogan.setSpacingAfter(20);
        document.add(slogan);
    }

    private void addTitle(Document document, OffreEmploi offre) throws DocumentException {
        // Badge statut
        String status = offre.getStatus();
        Color statusColor = "active".equalsIgnoreCase(status) ?
                new Color(34, 197, 94) : new Color(239, 68, 68);

        Paragraph statusBadge = new Paragraph(status != null ? status.toUpperCase() : "NON ACTIVE",
                new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE));
        statusBadge.setAlignment(Element.ALIGN_CENTER);
        statusBadge.setLeading(0, 1.2f);

        // Titre du poste
        Paragraph title = new Paragraph(safe(offre.getTitre()),
                new Font(Font.HELVETICA, 22, Font.BOLD, DARK_TEXT));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(10);
        title.setSpacingAfter(5);
        document.add(title);

        // Nom entreprise
        String entreprise = offre.getNomEntreprise();
        if (entreprise != null && !entreprise.isBlank()) {
            Paragraph company = new Paragraph("chez " + entreprise,
                    new Font(Font.HELVETICA, 14, Font.NORMAL, SECONDARY_COLOR));
            company.setAlignment(Element.ALIGN_CENTER);
            company.setSpacingAfter(15);
            document.add(company);
        }
    }

    private void addSeparator(Document document) throws DocumentException {
        LineSeparator line = new LineSeparator();
        line.setLineColor(new Color(226, 232, 240));
        line.setLineWidth(1);
        document.add(new Chunk(line));
        document.add(Chunk.NEWLINE);
    }

    private void addKeyInfo(Document document, OffreEmploi offre) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingAfter(20);

        // Colonnes égales
        float[] columnWidths = {1f, 1f};
        table.setWidths(columnWidths);

        // Localisation
        addInfoCell(table, "📍 Localisation", getLocationText(offre));

        // Type contrat
        String contrat = offre.getTypeContrat() != null ? offre.getTypeContrat().getNom() : null;
        addInfoCell(table, "📋 Type de contrat", safe(contrat));

        // Secteur
        String secteur = offre.getSecteur() != null ? offre.getSecteur().getNom() : null;
        addInfoCell(table, "🏢 Secteur", safe(secteur));

        // Expérience
        addInfoCell(table, "⭐ Expérience", safe(offre.getNiveauExperience()));

        // Études
        addInfoCell(table, "🎓 Niveau d'études", safe(offre.getNiveauEtudes()));

        // Langues
        addInfoCell(table, "🌐 Langues", safe(offre.getLanguesRequises()));

        // Postes
        Integer nbPostes = offre.getNombrePoste();
        addInfoCell(table, "👥 Postes", nbPostes != null ? nbPostes + " poste(s)" : "Non spécifié");

        // Date limite
        LocalDate dateLimite = offre.getDateLimiteCandidature();
        addInfoCell(table, "📅 Date limite",
                dateLimite != null ? dateLimite.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Non spécifiée");

        document.add(table);
    }

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderWidth(0);
        cell.setPadding(8);
        cell.setBackgroundColor(LIGHT_BG);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n",
                new Font(Font.HELVETICA, 9, Font.NORMAL, SECONDARY_COLOR)));
        p.add(new Chunk(safe(value),
                new Font(Font.HELVETICA, 11, Font.BOLD, DARK_TEXT)));
        p.setLeading(0, 1.4f);

        cell.addElement(p);
        table.addCell(cell);
    }

    private void addDescription(Document document, OffreEmploi offre) throws DocumentException {
        // Titre section
        Paragraph sectionTitle = new Paragraph("Description du poste",
                new Font(Font.HELVETICA, 14, Font.BOLD, PRIMARY_COLOR));
        sectionTitle.setSpacingBefore(10);
        sectionTitle.setSpacingAfter(10);
        document.add(sectionTitle);

        // Contenu
        String description = offre.getDescription();
        if (description != null && !description.isBlank()) {
            Paragraph desc = new Paragraph(description,
                    new Font(Font.HELVETICA, 11, Font.NORMAL, DARK_TEXT));
            desc.setLeading(0, 1.6f);
            desc.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(desc);
        } else {
            Paragraph noDesc = new Paragraph("Aucune description disponible.",
                    new Font(Font.HELVETICA, 11, Font.ITALIC, SECONDARY_COLOR));
            document.add(noDesc);
        }

        // Télétravail
        Boolean teletravail = offre.getTeletravail();
        if (teletravail != null && teletravail) {
            Paragraph remote = new Paragraph("\n✅ Télétravail autorisé",
                    new Font(Font.HELVETICA, 11, Font.BOLD, new Color(34, 197, 94)));
            remote.setSpacingBefore(15);
            document.add(remote);
        }
    }

    private void addFooter(Document document, OffreEmploi offre) throws DocumentException {
        document.add(Chunk.NEWLINE);
        addSeparator(document);

        // Date génération
        Paragraph generated = new Paragraph(
                "Document généré le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                new Font(Font.HELVETICA, 8, Font.ITALIC, SECONDARY_COLOR));
        generated.setAlignment(Element.ALIGN_CENTER);
        document.add(generated);

        // Footer info
        Paragraph footer = new Paragraph(
                "© 2025 CareerLink - Offre d'emploi",
                new Font(Font.HELVETICA, 8, Font.NORMAL, SECONDARY_COLOR));
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(5);
        document.add(footer);

        // Note QR Code
        Paragraph qrNote = new Paragraph(
                "Scannez le QR Code avec votre smartphone pour postuler en ligne",
                new Font(Font.HELVETICA, 8, Font.ITALIC, PRIMARY_COLOR));
        qrNote.setAlignment(Element.ALIGN_CENTER);
        qrNote.setSpacingBefore(10);
        document.add(qrNote);
    }

    private String getLocationText(OffreEmploi o) {
        if (o == null || o.getLocalisation() == null) {
            return "Non spécifiée";
        }
        String v = o.getLocalisation().getVille();
        String p = o.getLocalisation().getPays();
        String a = o.getLocalisation().getAdresse();
        StringBuilder sb = new StringBuilder();
        if (a != null && !a.trim().isEmpty()) sb.append(a).append(", ");
        if (v != null && !v.trim().isEmpty()) sb.append(v).append(", ");
        if (p != null && !p.trim().isEmpty()) sb.append(p);
        String result = sb.toString().trim();
        return result.isEmpty() ? "Non spécifiée" : result;
    }

    private String safe(String s) {
        return s == null || s.isBlank() ? "Non spécifié" : s.trim();
    }
}
