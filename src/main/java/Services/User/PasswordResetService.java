package Services.User;

import Utils.Mydatabase;
import Utils.PhoneUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PasswordResetService {

    public enum Channel {
        EMAIL,
        WHATSAPP
    }

    private static final int OTP_LENGTH = 6;
    private static final int OTP_TTL_MINUTES = 10;

    private final Connection conn;
    private final EmailSender emailSender;
    private final SmsSender smsSender;

    public PasswordResetService() {
        this(Mydatabase.getInstance().getConnection(), new GmailSmtpEmailSender(), createSmsSender());
    }

    private static SmsSender createSmsSender() {
        String greenUrl = System.getenv("GREEN_API_URL");
        String greenInstance = System.getenv("GREEN_API_INSTANCE_ID");
        String greenToken = System.getenv("GREEN_API_TOKEN");
        if (greenUrl != null && !greenUrl.isBlank() && greenInstance != null && !greenInstance.isBlank() && greenToken != null && !greenToken.isBlank()) {
            return new GreenApiWhatsAppSender();
        }
        return new WhatsAppCloudSender();
    }

    public PasswordResetService(Connection conn, EmailSender emailSender, SmsSender smsSender) {
        this.conn = conn;
        this.emailSender = emailSender;
        this.smsSender = smsSender;
    }

    public void requestOtp(String identifier, Channel channel) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier is required");
        }
        if (channel == null) {
            throw new IllegalArgumentException("Channel is required");
        }

        UserRecord user = findUserByIdentifier(identifier.trim());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        String otp = generateOtp();
        String otpHash = BCrypt.hashpw(otp, BCrypt.gensalt());
        Instant expiresAt = Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);

        long resetId = insertReset(user.id, channel, otpHash, Timestamp.from(expiresAt));

        if (channel == Channel.EMAIL) {
            String displayName = user.fullName == null || user.fullName.isBlank() ? "" : user.fullName.trim();
            String message = "Bonjour " + (displayName.isBlank() ? "" : (displayName + ",")) + "\n\n" +
                    "Votre code de v\u00e9rification CareerLink est " + otp + " (valide " + OTP_TTL_MINUTES + " minutes).";
            emailSender.send(user.email, "CareerLink password reset code", message);
        } else {
            String message = "CareerLink Team: Your verification code is " + otp + ". Expires in " + OTP_TTL_MINUTES + " min.";
            String phone = user.phone;
            if (phone == null || phone.isBlank()) {
                throw new IllegalArgumentException("No phone number for this user");
            }
            String normalizedPhone = PhoneUtil.normalizeIdentifierPhoneTN(phone);
            String templateName = System.getenv("WHATSAPP_TEMPLATE_NAME");
            String templateParamMode = System.getenv().getOrDefault("WHATSAPP_TEMPLATE_PARAM_MODE", "body_text");
            boolean useTemplateWithParam = templateName != null && !templateName.isBlank() && !"none".equalsIgnoreCase(templateParamMode);
            smsSender.send(normalizedPhone, useTemplateWithParam ? otp : message);
        }

        markSent(resetId);
    }

    public void verifyOtpAndResetPassword(String identifier, String otp, String newPassword) {
        if (identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier is required");
        }
        if (otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("OTP is required");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("New password is required");
        }

        UserRecord user = findUserByIdentifier(identifier.trim());
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (user.passwordHash != null) {
            try {
                if (BCrypt.checkpw(newPassword, user.passwordHash)) {
                    throw new IllegalArgumentException("New password must be different");
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        ResetRecord rr = findLatestValidReset(user.id);
        if (rr == null) {
            throw new IllegalArgumentException("No valid OTP request found");
        }

        boolean ok = false;
        try {
            ok = BCrypt.checkpw(otp.trim(), rr.otpHash);
        } catch (IllegalArgumentException ignored) {
            ok = false;
        }

        if (!ok) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        updatePassword(user.id, newHash);
        markUsed(rr.id);
    }

    private UserRecord findUserByIdentifier(String identifier) {
        String normalizedIdentifier = PhoneUtil.normalizeIdentifierPhoneTN(identifier);
        String digitsOnly = identifier == null ? null : identifier.replaceAll("\\D", "");
        String localTn = null;
        if (digitsOnly != null && digitsOnly.matches("\\d{8}")) {
            localTn = digitsOnly;
        }

        if (localTn == null && normalizedIdentifier != null) {
            String normDigits = normalizedIdentifier.replaceAll("\\D", "");
            if (normDigits.startsWith("216") && normDigits.length() == 11) {
                localTn = normDigits.substring(3);
            }
        }

        String sql = "SELECT id, email, phone, password, full_name FROM user WHERE email=? OR phone=? OR phone=? OR full_name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedIdentifier);
            ps.setString(2, normalizedIdentifier);
            ps.setString(3, localTn);
            ps.setString(4, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserRecord(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            rs.getString("password"),
                            rs.getString("full_name")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private long insertReset(int userId, Channel channel, String otpHash, Timestamp expiresAt) {
        String sql = "INSERT INTO password_reset_otp (user_id, channel, otp_hash, expires_at) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, channel.name());
            ps.setString(3, otpHash);
            ps.setTimestamp(4, expiresAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void markSent(long id) {
        if (id <= 0) return;
        String sql = "UPDATE password_reset_otp SET sent_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private ResetRecord findLatestValidReset(int userId) {
        String sql = "SELECT id, otp_hash, expires_at FROM password_reset_otp WHERE user_id=? AND used_at IS NULL AND expires_at > NOW() ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResetRecord(rs.getLong("id"), rs.getString("otp_hash"), rs.getTimestamp("expires_at"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void markUsed(long id) {
        String sql = "UPDATE password_reset_otp SET used_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private void updatePassword(int userId, String bcryptHash) {
        String sql = "UPDATE user SET password=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bcryptHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateOtp() {
        SecureRandom random = new SecureRandom();
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", value);
    }

    private static class UserRecord {
        final int id;
        final String email;
        final String phone;
        final String passwordHash;
        final String fullName;

        UserRecord(int id, String email, String phone, String passwordHash, String fullName) {
            this.id = id;
            this.email = email;
            this.phone = phone;
            this.passwordHash = passwordHash;
            this.fullName = fullName;
        }
    }

    private static class ResetRecord {
        final long id;
        final String otpHash;
        final Timestamp expiresAt;

        ResetRecord(long id, String otpHash, Timestamp expiresAt) {
            this.id = id;
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
        }
    }

    public interface EmailSender {
        void send(String toEmail, String subject, String textContent);
    }

    public interface SmsSender {
        void send(String toPhoneE164, String textContent);
    }
}
