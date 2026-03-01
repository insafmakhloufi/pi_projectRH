package Services.User;

import Utils.Mydatabase;
import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;

import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;

public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final int QR_SIZE = 220;
    private static final Duration PERIOD = Duration.ofSeconds(30);

    private final Connection conn;
    private final TimeBasedOneTimePasswordGenerator totp;
    private final Base32 base32;

    public TotpService() {
        this(Mydatabase.getInstance().getConnection());
    }

    public TotpService(Connection conn) {
        this.conn = conn;
        try {
            this.totp = new TimeBasedOneTimePasswordGenerator(PERIOD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.base32 = new Base32();
        ensureTable();
    }

    public boolean isEnabled(int userId) {
        String sql = "SELECT enabled FROM user_totp WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("enabled") == 1;
                }
            }
        } catch (SQLException ignored) {
        }
        return false;
    }

    public String getOrCreateSecretBase32(int userId) {
        String existing = getSecretBase32(userId);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }

        byte[] secret = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(secret);
        String secretB32 = base32.encodeToString(secret).replace("=", "");
        upsertSecret(userId, secretB32);
        return secretB32;
    }

    public void enable(int userId) {
        String sql = "UPDATE user_totp SET enabled=1, confirmed_at=NOW() WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void disable(int userId) {
        String sql = "UPDATE user_totp SET enabled=0 WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyCode(int userId, String code) {
        if (code == null || !code.trim().matches("\\d{6}")) return false;
        String secretB32 = getSecretBase32(userId);
        if (secretB32 == null || secretB32.isBlank()) return false;

        byte[] secretBytes = base32.decode(secretB32);
        SecretKeySpec key = new SecretKeySpec(secretBytes, totp.getAlgorithm());

        int otp;
        try {
            otp = totp.generateOneTimePassword(key, Instant.now());
        } catch (Exception e) {
            return false;
        }

        String expected = String.format("%06d", otp);
        if (expected.equals(code.trim())) return true;

        // small drift tolerance: previous and next step
        try {
            String prev = String.format("%06d", totp.generateOneTimePassword(key, Instant.now().minus(PERIOD)));
            if (prev.equals(code.trim())) return true;
            String next = String.format("%06d", totp.generateOneTimePassword(key, Instant.now().plus(PERIOD)));
            return next.equals(code.trim());
        } catch (Exception e) {
            return false;
        }
    }

    public String buildOtpAuthUri(String issuer, String accountName, String secretB32) {
        String safeIssuer = urlEncode(issuer);
        String label = urlEncode(issuer + ":" + accountName);
        String secret = urlEncode(secretB32);
        return "otpauth://totp/" + label + "?secret=" + secret + "&issuer=" + safeIssuer + "&digits=6&period=" + PERIOD.toSeconds();
    }

    private static String urlEncode(String v) {
        return URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8);
    }

    private void ensureTable() {
        String sql = "CREATE TABLE IF NOT EXISTS user_totp (" +
                "user_id INT PRIMARY KEY," +
                "secret_base32 VARCHAR(128) NOT NULL," +
                "enabled TINYINT NOT NULL DEFAULT 0," +
                "confirmed_at DATETIME NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_user_totp_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ignored) {
        }
    }

    private String getSecretBase32(int userId) {
        String sql = "SELECT secret_base32 FROM user_totp WHERE user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("secret_base32");
            }
        } catch (SQLException ignored) {
        }
        return null;
    }

    private void upsertSecret(int userId, String secretBase32) {
        String sql = "INSERT INTO user_totp (user_id, secret_base32, enabled, confirmed_at) VALUES (?,?,0,NULL) " +
                "ON DUPLICATE KEY UPDATE secret_base32=VALUES(secret_base32), enabled=0, confirmed_at=NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, secretBase32);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
