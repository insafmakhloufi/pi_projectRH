package Services.User;

import Utils.Mydatabase;
import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_MINUTES = 30;

    private final Connection conn;
    private final PasswordResetService.EmailSender emailSender;

    public EmailVerificationService() {
        this(Mydatabase.getInstance().getConnection(), new GmailSmtpEmailSender());
    }

    public EmailVerificationService(Connection conn) {
        this(conn, null);
    }

    public EmailVerificationService(Connection conn, PasswordResetService.EmailSender emailSender) {
        this.conn = conn;
        this.emailSender = emailSender;
        ensureTable();
    }

    public void sendVerificationCode(String email, String fullName) {
        if (emailSender == null) {
            throw new IllegalStateException("Email sender is not configured");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.trim();

        Integer userId = findUserIdByEmail(normalizedEmail);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        if (isVerified(userId)) {
            return;
        }

        String code = generateCode();
        String hash = BCrypt.hashpw(code, BCrypt.gensalt());
        Instant expiresAt = Instant.now().plus(TTL_MINUTES, ChronoUnit.MINUTES);

        upsertCode(userId, hash, Timestamp.from(expiresAt));

        String name = fullName;
        if (name == null || name.isBlank()) {
            name = findUserFullNameByEmail(normalizedEmail);
        }

        String displayName = name == null || name.isBlank() ? "" : name.trim();
        String msg = "Bonjour " + (displayName.isBlank() ? "" : (displayName + ",")) + "\n\n" +
                "Votre code de v\u00e9rification CareerLink est " + code + " (valide " + TTL_MINUTES + " minutes).";

        emailSender.send(normalizedEmail, "V\u00e9rifiez votre email CareerLink", msg);
    }

    public void verifyCode(String email, String code) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (code == null || !code.trim().matches("\\d{6}")) {
            throw new IllegalArgumentException("Code must be 6 digits");
        }

        Integer userId = findUserIdByEmail(email.trim());
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        VerificationRecord vr = findLatestCode(userId);
        if (vr == null) {
            throw new IllegalArgumentException("No verification code found");
        }
        if (vr.verifiedAt != null) {
            return;
        }
        if (vr.expiresAt == null || vr.expiresAt.toInstant().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Code expired");
        }

        boolean ok;
        try {
            ok = BCrypt.checkpw(code.trim(), vr.codeHash);
        } catch (IllegalArgumentException e) {
            ok = false;
        }
        if (!ok) {
            throw new IllegalArgumentException("Invalid code");
        }

        markVerified(userId);
    }

    public boolean isVerified(int userId) {
        String sql = "SELECT verified_at FROM email_verification_code WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("verified_at") != null;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public boolean isEmailVerifiedByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) return false;
        Integer id = findUserIdByIdentifier(identifier.trim());
        if (id == null) return false;
        return isVerified(id);
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS email_verification_code (" +
                "user_id INT PRIMARY KEY," +
                "code_hash VARCHAR(255) NOT NULL," +
                "expires_at DATETIME NOT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "verified_at DATETIME NULL," +
                "CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    private Integer findUserIdByEmail(String email) {
        String sql = "SELECT id FROM user WHERE email=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private String findUserFullNameByEmail(String email) {
        String sql = "SELECT full_name FROM user WHERE email=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("full_name");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private Integer findUserIdByIdentifier(String identifier) {
        String sql = "SELECT id FROM user WHERE email=? OR phone=? OR full_name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);
            ps.setString(3, identifier);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private void upsertCode(int userId, String hash, Timestamp expiresAt) {
        String sql = "INSERT INTO email_verification_code (user_id, code_hash, expires_at, verified_at) VALUES (?,?,?,NULL) " +
                "ON DUPLICATE KEY UPDATE code_hash=VALUES(code_hash), expires_at=VALUES(expires_at), verified_at=NULL, created_at=CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, hash);
            ps.setTimestamp(3, expiresAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private VerificationRecord findLatestCode(int userId) {
        String sql = "SELECT code_hash, expires_at, verified_at FROM email_verification_code WHERE user_id=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new VerificationRecord(rs.getString("code_hash"), rs.getTimestamp("expires_at"), rs.getTimestamp("verified_at"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void markVerified(int userId) {
        String sql = "UPDATE email_verification_code SET verified_at=NOW() WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateCode() {
        SecureRandom random = new SecureRandom();
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", value);
    }

    private static class VerificationRecord {
        final String codeHash;
        final Timestamp expiresAt;
        final Timestamp verifiedAt;

        VerificationRecord(String codeHash, Timestamp expiresAt, Timestamp verifiedAt) {
            this.codeHash = codeHash;
            this.expiresAt = expiresAt;
            this.verifiedAt = verifiedAt;
        }
    }
}
