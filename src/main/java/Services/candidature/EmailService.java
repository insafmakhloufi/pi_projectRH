package Services.candidature;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Random;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class EmailService {

    // APRES
    private static final String FROM_EMAIL = "jihed.khlifi@esprit.tn"; // ← ton vrai Gmail
    private static final String FROM_PASSWORD = "ppmg onao exng rlrr";      // ← mot de passe app Gmail

    public void envoyerCodeSignature(String toEmail, String code, String nomCandidat) throws Exception {

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL, "CareerLink", StandardCharsets.UTF_8.name()));
        message.setReplyTo(new Address[]{new InternetAddress(FROM_EMAIL)});
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
        message.setSubject("CareerLink - Code de signature de contrat", StandardCharsets.UTF_8.name());

        String safeNom = nomCandidat == null ? "" : nomCandidat.trim();
        String safeCode = code == null ? "" : code.trim();

        String plainText =
                "Bonjour " + safeNom + ",\n\n" +
                        "Votre code de signature pour votre contrat de travail est :\n\n" +
                        "    " + safeCode + "\n\n" +
                        "Ce code est valable pendant 15 minutes.\n\n" +
                        "Si vous n'avez pas demandé cette action, ignorez cet email.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe CareerLink";

        String html = "" +
                "<!doctype html>" +
                "<html><head><meta charset=\"utf-8\"></head>" +
                "<body style=\"margin:0;padding:0;background:#F5F7FB;font-family:Arial,Helvetica,sans-serif;\">" +
                "<div style=\"max-width:640px;margin:0 auto;padding:28px 16px;\">" +
                "  <div style=\"max-width:520px;margin:0 auto;background:#FFFFFF;border-radius:12px;overflow:hidden;border:1px solid #E8ECF3;box-shadow:0 10px 25px rgba(17,24,39,0.08);\">" +
                "    <div style=\"background:#635BFF;padding:22px 18px;text-align:center;\">" +
                "      <div style=\"color:#FFFFFF;font-size:18px;font-weight:700;\">CareerLink</div>" +
                "      <div style=\"color:#E9E7FF;font-size:12px;margin-top:6px;\">Signature de contrat</div>" +
                "    </div>" +
                "    <div style=\"padding:22px 22px 18px 22px;\">" +
                "      <div style=\"font-size:14px;font-weight:700;color:#111827;\">Bonjour " + escapeHtml(safeNom) + ",</div>" +
                "      <div style=\"margin-top:10px;color:#4B5563;font-size:13px;line-height:1.6;\">" +
                "        Voici votre code de signature pour confirmer votre contrat de travail." +
                "      </div>" +
                "      <div style=\"margin:18px 0 8px 0;text-align:center;\">" +
                "        <div style=\"display:inline-block;padding:12px 18px;border-radius:10px;background:#F3F4F6;border:1px solid #E5E7EB;font-size:22px;letter-spacing:4px;font-weight:700;color:#111827;\">" + escapeHtml(safeCode) + "</div>" +
                "      </div>" +
                "      <div style=\"margin-top:12px;color:#6B7280;font-size:12px;\">Ce code est valable pendant <b>15 minutes</b>.</div>" +
                "      <div style=\"margin:16px 0 10px 0;padding:14px 14px;background:#F8FAFF;border-radius:8px;border-left:4px solid #4F46E5;\">" +
                "        <div style=\"color:#4F46E5;font-weight:700;font-size:12px;\">Information importante</div>" +
                "        <div style=\"margin-top:6px;color:#4B5563;font-size:12px;line-height:1.5;\">Si vous n'avez pas demandé cette action, vous pouvez ignorer cet email.</div>" +
                "      </div>" +
                "      <div style=\"margin-top:8px;color:#6B7280;font-size:12px;\">Cordialement,<br><b>L'équipe CareerLink</b></div>" +
                "    </div>" +
                "    <div style=\"padding:16px 22px;border-top:1px solid #EEF2F7;text-align:center;color:#9CA3AF;font-size:11px;\">" +
                "      &copy; " + java.time.Year.now() + " CareerLink. Tous droits réservés." +
                "    </div>" +
                "  </div>" +
                "</div>" +
                "</body></html>";

        MimeBodyPart plainPart = new MimeBodyPart();
        plainPart.setText(plainText, StandardCharsets.UTF_8.name());

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");

        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(plainPart);
        multipart.addBodyPart(htmlPart);
        message.setContent(multipart);

        message.setHeader("X-Mailer", "CareerLink");
        message.saveChanges();

        Transport.send(message);
        System.out.println("Email envoye a : " + toEmail);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String genererCode() {
        Random r = new Random();
        int code = 100000 + r.nextInt(900000);
        return String.valueOf(code);
    }
}