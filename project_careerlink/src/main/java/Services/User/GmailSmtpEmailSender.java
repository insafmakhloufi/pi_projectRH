package Services.User;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GmailSmtpEmailSender implements PasswordResetService.EmailSender {

    private final String smtpHost;
    private final int smtpPort;
    private final String username;
    private final String appPassword;
    private final String fromEmail;
    private final String fromName;

    public GmailSmtpEmailSender() {
        this.smtpHost = System.getenv().getOrDefault("GMAIL_SMTP_HOST", "smtp.gmail.com");
        this.smtpPort = Integer.parseInt(System.getenv().getOrDefault("GMAIL_SMTP_PORT", "587"));
        this.username = getRequiredEnv("GMAIL_SMTP_USER");
        this.appPassword = getRequiredEnv("GMAIL_SMTP_APP_PASSWORD");
        this.fromEmail = System.getenv().getOrDefault("GMAIL_FROM_EMAIL", this.username);
        this.fromName = System.getenv().getOrDefault("GMAIL_FROM_NAME", "CareerLink Team");
    }

    @Override
    public void send(String toEmail, String subject, String textContent) {
        try {
            Session session = Session.getInstance(buildProps(), new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, appPassword);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, fromName, StandardCharsets.UTF_8.name()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail, false));
            message.setSubject(subject, StandardCharsets.UTF_8.name());

            String plain = textContent == null ? "" : textContent;

            MimeBodyPart plainPart = new MimeBodyPart();
            plainPart.setText(plain, StandardCharsets.UTF_8.name());

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(buildHtml(subject, plain), "text/html; charset=UTF-8");

            MimeMultipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(plainPart);
            multipart.addBodyPart(htmlPart);
            message.setContent(multipart);

            Transport.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Gmail SMTP error", e);
        } catch (Exception e) {
            throw new RuntimeException("Gmail SMTP error", e);
        }
    }

    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b");
    private static final Pattern GREETING_PATTERN = Pattern.compile("^\\s*Bonjour\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    private String buildHtml(String subject, String plainText) {
        String safeSubject = escapeHtml(subject == null ? "" : subject);
        String raw = plainText == null ? "" : plainText;

        String greeting = "Bonjour,";
        String bodyRaw = raw;

        String[] parts = raw.split("\\r?\\n", -1);
        if (parts.length > 0) {
            Matcher gm = GREETING_PATTERN.matcher(parts[0]);
            if (gm.matches()) {
                String rest = gm.group(1) == null ? "" : gm.group(1).trim();
                greeting = "Bonjour" + (rest.isBlank() ? "," : (" " + rest));
                int idx = raw.indexOf('\n');
                if (idx >= 0) {
                    bodyRaw = raw.substring(idx + 1);
                    while (bodyRaw.startsWith("\n") || bodyRaw.startsWith("\r")) {
                        bodyRaw = bodyRaw.substring(1);
                    }
                } else {
                    bodyRaw = "";
                }
            }
        }

        String safeGreeting = escapeHtml(greeting);
        String safeBody = escapeHtml(bodyRaw).replace("\n", "<br>");

        String otp = null;
        Matcher matcher = OTP_PATTERN.matcher(raw);
        if (matcher.find()) {
            otp = matcher.group(1);
        }

        String otpBlock = "";
        if (otp != null && !otp.isBlank()) {
            otpBlock = "" +
                    "<div style=\"margin:18px 0 6px 0; text-align:center;\">" +
                    "  <div style=\"display:inline-block; padding:12px 18px; border-radius:10px; background:#F3F4F6; border:1px solid #E5E7EB; font-size:22px; letter-spacing:4px; font-weight:700; color:#111827;\">" + escapeHtml(otp) + "</div>" +
                    "</div>";
        }

        return "" +
                "<!doctype html>" +
                "<html><head><meta charset=\"utf-8\"></head>" +
                "<body style=\"margin:0; padding:0; background:#F5F7FB; font-family:Arial, Helvetica, sans-serif;\">" +
                "  <div style=\"max-width:640px; margin:0 auto; padding:28px 16px;\">" +
                "    <div style=\"max-width:520px; margin:0 auto; background:#FFFFFF; border-radius:10px; overflow:hidden; border:1px solid #E8ECF3; box-shadow:0 10px 25px rgba(17,24,39,0.08);\">" +
                "      <div style=\"background:#635BFF; padding:22px 18px; text-align:center;\">" +
                "        <div style=\"color:#FFFFFF; font-size:18px; font-weight:700;\">R\u00e9initialisation de mot de passe</div>" +
                "      </div>" +
                "      <div style=\"padding:22px 22px 18px 22px;\">" +
                "        <div style=\"font-size:14px; font-weight:700; color:#111827;\">" + safeGreeting + "</div>" +
                "        <div style=\"margin-top:10px; color:#4B5563; font-size:13px; line-height:1.6;\">" + safeBody + "</div>" +
                "        <div style=\"margin-top:14px; color:#6B7280; font-size:12px;\">Ce code est valide pendant <b>10 minutes</b>.</div>" +
                "        " + otpBlock +
                "        <div style=\"margin:16px 0 10px 0; padding:14px 14px; background:#F8FAFF; border-radius:8px; border-left:4px solid #4F46E5;\">" +
                "          <div style=\"color:#4F46E5; font-weight:700; font-size:12px;\">Information importante</div>" +
                "          <div style=\"margin-top:6px; color:#4B5563; font-size:12px; line-height:1.5;\">Si vous n'avez pas demand\u00e9 cette r\u00e9initialisation, vous pouvez ignorer cet email. Votre compte reste s\u00e9curis\u00e9.</div>" +
                "        </div>" +
                "        <div style=\"margin-top:8px; color:#6B7280; font-size:12px;\">Cordialement,<br><b>L'\u00e9quipe CareerLink</b></div>" +
                "      </div>" +
                "      <div style=\"padding:16px 22px; border-top:1px solid #EEF2F7; text-align:center; color:#9CA3AF; font-size:11px;\">" +
                "        \u00a9 " + java.time.Year.now() + " CareerLink. Tous droits r\u00e9serv\u00e9s." +
                "      </div>" +
                "    </div>" +
                "  </div>" +
                "</body></html>";
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

    private Properties buildProps() {
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", String.valueOf(smtpPort));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        return props;
    }

    private static String getRequiredEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + key);
        }
        return v;
    }
}
