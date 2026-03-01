package Services.candidature;

import java.util.Properties;
import java.util.Random;
import javax.mail.*;
import javax.mail.internet.*;

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

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("CareerLink - Code de signature de contrat");
        message.setText(
                "Bonjour " + nomCandidat + ",\n\n" +
                        "Votre code de signature pour votre contrat de travail est :\n\n" +
                        "    " + code + "\n\n" +
                        "Ce code est valable pendant 15 minutes.\n\n" +
                        "Si vous n'avez pas demande cette action, ignorez cet email.\n\n" +
                        "CareerLink"
        );

        Transport.send(message);
        System.out.println("Email envoye a : " + toEmail);
    }

    public static String genererCode() {
        Random r = new Random();
        int code = 100000 + r.nextInt(900000);
        return String.valueOf(code);
    }
}