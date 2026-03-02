package Utils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class EmailService {

    private static volatile boolean configLogged = false;

    private EmailService() {
    }

    public static void sendBcc(List<String> recipients, String subject, String body) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        String from = getenvOrDefault("SMTP_FROM", getenvOrDefault("SMTP_USER", ""));
        if (from.isBlank()) {
            throw new IllegalStateException("SMTP_FROM (ou SMTP_USER) est requis pour envoyer des emails");
        }

        MimeMessage message = new MimeMessage(buildSession());
        try {
            message.setFrom(new InternetAddress(from));

            String bcc = recipients.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(EmailService::isValidEmail)
                    .distinct()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            if (bcc.isEmpty()) {
                return;
            }

            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
            message.setSubject(subject == null ? "" : subject);
            message.setText(body == null ? "" : body);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendToHtml(String recipient, String subject, String htmlBody) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }

        recipient = recipient.trim();
        if (!isValidEmail(recipient)) {
            return;
        }

        String from = getenvOrDefault("SMTP_FROM", getenvOrDefault("SMTP_USER", ""));
        if (from.isBlank()) {
            throw new IllegalStateException("SMTP_FROM (ou SMTP_USER) est requis pour envoyer des emails");
        }

        MimeMessage message = new MimeMessage(buildSession());
        try {
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject == null ? "" : subject, "UTF-8");
            message.setContent(htmlBody == null ? "" : htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendBccHtml(List<String> recipients, String subject, String htmlBody) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }

        String from = getenvOrDefault("SMTP_FROM", getenvOrDefault("SMTP_USER", ""));
        if (from.isBlank()) {
            throw new IllegalStateException("SMTP_FROM (ou SMTP_USER) est requis pour envoyer des emails");
        }

        MimeMessage message = new MimeMessage(buildSession());
        try {
            message.setFrom(new InternetAddress(from));

            String bcc = recipients.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(EmailService::isValidEmail)
                    .distinct()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");

            if (bcc.isEmpty()) {
                return;
            }

            message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
            message.setSubject(subject == null ? "" : subject, "UTF-8");
            message.setContent(htmlBody == null ? "" : htmlBody, "text/html; charset=UTF-8");

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Session buildSession() {
        String host = getenvOrDefault("SMTP_HOST", "smtp.gmail.com");
        String port = getenvOrDefault("SMTP_PORT", "587");
        String user = getenvOrDefault("SMTP_USER", "");
        String pass = getenvOrDefault("SMTP_PASS", "");

        boolean auth = Boolean.parseBoolean(getenvOrDefault("SMTP_AUTH", "true"));
        boolean startTls = Boolean.parseBoolean(getenvOrDefault("SMTP_TLS", "true"));
        boolean ssl = Boolean.parseBoolean(getenvOrDefault("SMTP_SSL", "false"));

        if (!configLogged) {
            configLogged = true;
            System.out.println(
                    "[DEBUG] SMTP config: host=" + host +
                            ", port=" + port +
                            ", user_present=" + (!user.isBlank()) +
                            ", pass_present=" + (!pass.isBlank()) +
                            ", pass_len=" + (pass == null ? 0 : pass.length()) +
                            ", auth=" + auth +
                            ", tls=" + startTls +
                            ", ssl=" + ssl
            );
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(startTls));

        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
        }

        if (!auth) {
            return Session.getInstance(props);
        }

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null) ? def : v;
    }

    private static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String v = email.trim();
        if (v.isEmpty()) {
            return false;
        }
        try {
            InternetAddress addr = new InternetAddress(v);
            addr.validate();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
