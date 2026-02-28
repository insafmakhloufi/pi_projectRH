
package Services.Formation;

import Utils.EmailService;
import Utils.Mydatabase;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.security.SecureRandom;
import java.util.Locale;

public class FormationPurchaseService {

    private final Connection con;
    private final String successUrlBase;
    private final String cancelUrlBase;

    private static final int PROMO_DISCOUNT_PERCENT = 20;
    private static final int PROMO_EVERY_N_PURCHASES = 3;
    private static final SecureRandom RNG = new SecureRandom();
    private static volatile boolean promoTableEnsured = false;

    public FormationPurchaseService() {
        con = Mydatabase.getInstance().getConnection();
        // TODO: Move API key to server-side configuration
        Stripe.apiKey = "sk_test_51SffoJGlRq5Z61v4UbHZhBfvCoWmSXxalM7rSfXQFLleQ9f0ZwDNsXkYespALEPgVQYYKKv1RCbApxDlKA9Pk1f900WGymFYTR"; // Replace with your actual Stripe secret key

        successUrlBase = getenvOrDefault("STRIPE_SUCCESS_URL_BASE", "https://clever-cannoli-c18ecb.netlify.app/stripe-success.html");
        cancelUrlBase = getenvOrDefault("STRIPE_CANCEL_URL_BASE", "https://clever-cannoli-c18ecb.netlify.app/stripe-cancel.html");

        ensurePromoTableExists();
    }

    public String getSuccessUrlBase() {
        return successUrlBase;
    }

    public String getCancelUrlBase() {
        return cancelUrlBase;
    }

    public boolean hasPurchased(int userId, int formationId) {
        String sql = "SELECT 1 FROM formation_purchase WHERE user_id = ? AND formation_id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void purchase(int userId, int formationId) {
        String sql = "INSERT INTO formation_purchase(user_id, formation_id) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, formationId);
            ps.executeUpdate();

            maybeAwardPromoCode(userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String createCheckoutSessionUrl(String formationTitle, double price, int formationId, int userId) throws StripeException {
        return createCheckoutSessionUrl(formationTitle, price, formationId, userId, null);
    }

    public String createCheckoutSessionUrl(String formationTitle, double price, int formationId, int userId, String promoCode) throws StripeException {
        if (hasPurchased(userId, formationId)) {
            throw new IllegalStateException("Formation déjà payée");
        }
        Promo promo = validatePromoForUser(userId, promoCode);
        double finalPrice = applyDiscountPercent(price, promo == null ? null : promo.discountPercent);

        SessionCreateParams params = SessionCreateParams.builder()
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(successUrlBase + "?session_id={CHECKOUT_SESSION_ID}&formation_id=" + formationId + "&user_id=" + userId)
            .setCancelUrl(cancelUrlBase + "?formation_id=" + formationId + "&user_id=" + userId)
            .putMetadata("formation_id", String.valueOf(formationId))
            .putMetadata("user_id", String.valueOf(userId))
            .putMetadata("promo_code", promo == null ? "" : promo.code)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd") // Assuming USD, change if needed
                            .setUnitAmount((long)(finalPrice * 100)) // Convert to cents
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(formationTitle)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    public boolean verifyCheckoutSessionPaidAndRecordPurchase(String sessionId, int expectedUserId, int expectedFormationId) throws StripeException {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }

        Session session = Session.retrieve(sessionId.trim());
        if (session == null) {
            return false;
        }

        String paymentStatus = session.getPaymentStatus();
        if (paymentStatus == null || !paymentStatus.equalsIgnoreCase("paid")) {
            return false;
        }

        String mUserId = session.getMetadata() == null ? null : session.getMetadata().get("user_id");
        String mFormationId = session.getMetadata() == null ? null : session.getMetadata().get("formation_id");
        String mPromoCode = session.getMetadata() == null ? null : session.getMetadata().get("promo_code");

        if (mUserId == null || mFormationId == null) {
            return false;
        }

        try {
            int u = Integer.parseInt(mUserId);
            int f = Integer.parseInt(mFormationId);
            if (u != expectedUserId || f != expectedFormationId) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }

        if (!hasPurchased(expectedUserId, expectedFormationId)) {
            purchase(expectedUserId, expectedFormationId);
        }

        if (mPromoCode != null && !mPromoCode.isBlank()) {
            markPromoCodeUsed(expectedUserId, mPromoCode.trim());
        }
        return true;
    }

    public String getUnusedPromoCodeForUser(int userId) {
        ensurePromoTableExists();
        String sql = "SELECT code FROM formation_promo_code WHERE user_id = ? AND used = 0 ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public PromoCodeStatus getPromoCodeStatusForUser(int userId, String promoCode) {
        ensurePromoTableExists();
        if (promoCode == null) {
            return PromoCodeStatus.EMPTY;
        }
        String code = promoCode.trim();
        if (code.isEmpty()) {
            return PromoCodeStatus.EMPTY;
        }

        String sql = "SELECT used FROM formation_promo_code WHERE user_id = ? AND code = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return PromoCodeStatus.NOT_FOUND;
                }
                boolean used = rs.getInt(1) != 0;
                return used ? PromoCodeStatus.USED : PromoCodeStatus.VALID;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PromoEmailState getLatestUnusedPromoEmailState(int userId) {
        ensurePromoTableExists();
        String sql = "SELECT code, COALESCE(email_sent, 0) AS email_sent FROM formation_promo_code WHERE user_id = ? AND used = 0 ORDER BY id DESC LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String code = rs.getString("code");
                    boolean sent = rs.getInt("email_sent") != 0;
                    return new PromoEmailState(code, sent);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void ensurePromoTableExists() {
        if (promoTableEnsured) {
            return;
        }
        synchronized (FormationPurchaseService.class) {
            if (promoTableEnsured) {
                return;
            }
            String ddl = "CREATE TABLE IF NOT EXISTS formation_promo_code (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "code VARCHAR(64) NOT NULL UNIQUE," +
                "discount_percent INT NOT NULL," +
                "used TINYINT(1) NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "used_at TIMESTAMP NULL," +
                "email_sent TINYINT(1) NOT NULL DEFAULT 0," +
                "email_sent_at TIMESTAMP NULL" +
                ")";
            try (Statement st = con.createStatement()) {
                st.executeUpdate(ddl);

                // If the table already existed with an older schema, try to add the new columns.
                try {
                    st.executeUpdate("ALTER TABLE formation_promo_code ADD COLUMN IF NOT EXISTS email_sent TINYINT(1) NOT NULL DEFAULT 0");
                } catch (Exception ignored) {
                }
                try {
                    st.executeUpdate("ALTER TABLE formation_promo_code ADD COLUMN IF NOT EXISTS email_sent_at TIMESTAMP NULL");
                } catch (Exception ignored) {
                }

                promoTableEnsured = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int countPurchasesByUser(int userId) {
        String sql = "SELECT COUNT(*) FROM formation_purchase WHERE user_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private void maybeAwardPromoCode(int userId) {
        ensurePromoTableExists();

        int count = countPurchasesByUser(userId);
        System.out.println("[DEBUG] User " + userId + " has " + count + " purchases");
        if (count <= 0 || (count % PROMO_EVERY_N_PURCHASES) != 0) {
            return;
        }

        PromoEmailState existing = getLatestUnusedPromoEmailState(userId);
        if (existing != null && existing.code != null && !existing.code.isBlank()) {
            System.out.println("[DEBUG] User " + userId + " already has unused promo code: " + existing.code + " (email_sent=" + existing.emailSent + ")");
            if (!existing.emailSent) {
                new Thread(() -> {
                    try {
                        sendPromoCodeEmailAndMarkSent(userId, existing.code);
                    } catch (Exception e) {
                        System.out.println("[WARN] Promo email failed (user_id=" + userId + "): " + e.getMessage());
                    }
                }, "promo-email-sender").start();
            }
            return;
        }

        String code = generatePromoCode();
        System.out.println("[DEBUG] Generating new promo code for user " + userId + ": " + code);
        String sql = "INSERT INTO formation_promo_code(user_id, code, discount_percent, used, email_sent) VALUES (?, ?, ?, 0, 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setInt(3, PROMO_DISCOUNT_PERCENT);
            ps.executeUpdate();

            // Best-effort email: never block or fail the purchase if SMTP is not configured.
            new Thread(() -> {
                try {
                    sendPromoCodeEmailAndMarkSent(userId, code);
                } catch (Exception e) {
                    System.out.println("[WARN] Promo email failed (user_id=" + userId + "): " + e.getMessage());
                }
            }, "promo-email-sender").start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPromoCodeEmailAndMarkSent(int userId, String promoCode) throws java.sql.SQLException {
        if (promoCode == null || promoCode.isBlank()) {
            System.out.println("[DEBUG] Promo code is null or blank for user " + userId);
            return;
        }

        String email = getUserEmailById(userId);
        System.out.println("[DEBUG] Retrieved email for user " + userId + ": " + (email != null ? email : "null"));
        if (email == null || email.isBlank()) {
            System.out.println("[DEBUG] Email is null or blank, skipping email send for user " + userId);
            return;
        }

        String subject = "Votre code promo CareerLink (-" + PROMO_DISCOUNT_PERCENT + "%)";
        String html = "" +
                "<html><body style='font-family:Arial,sans-serif;background:#f6f7fb;padding:20px;'>" +
                "<div style='max-width:640px;margin:0 auto;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;'>" +
                "  <div style='background:#4f46e5;color:#ffffff;padding:18px 22px;font-weight:700;font-size:16px;'>CareerLink</div>" +
                "  <div style='padding:22px;color:#111827;'>" +
                "    <div style='font-size:16px;font-weight:700;margin-bottom:8px;'>Félicitations !</div>" +
                "    <div style='font-size:13px;line-height:1.5;color:#374151;'>" +
                "      Vous avez effectué " + PROMO_EVERY_N_PURCHASES + " achats de formations. Voici votre code promo de " + PROMO_DISCOUNT_PERCENT + "% sur votre prochain achat :" +
                "    </div>" +
                "    <div style='margin-top:14px;padding:12px 14px;border:1px dashed #c7d2fe;border-radius:12px;background:#eef2ff;display:inline-block;font-size:18px;font-weight:800;color:#3730a3;'>" +
                promoCode +
                "    </div>" +
                "    <div style='margin-top:14px;font-size:12px;color:#6b7280;'>" +
                "      Code à usage unique, valable sur la prochaine formation payante." +
                "    </div>" +
                "  </div>" +
                "</div>" +
                "</body></html>";

        System.out.println("[DEBUG] Sending promo email to " + email + " for user " + userId + " with code " + promoCode);
        EmailService.sendToHtml(email.trim(), subject, html);
        System.out.println("[DEBUG] Promo email sent successfully to " + email);

        String mark = "UPDATE formation_promo_code SET email_sent = 1, email_sent_at = CURRENT_TIMESTAMP WHERE user_id = ? AND code = ? AND email_sent = 0";
        try (PreparedStatement ps = con.prepareStatement(mark)) {
            ps.setInt(1, userId);
            ps.setString(2, promoCode);
            ps.executeUpdate();
            System.out.println("[DEBUG] Marked email_sent=1 for user " + userId + " code " + promoCode);
        }
    }

    private String getUserEmailById(int userId) throws java.sql.SQLException {
        String sql = "SELECT email FROM user WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private Promo validatePromoForUser(int userId, String promoCode) {
        ensurePromoTableExists();
        if (promoCode == null) {
            return null;
        }
        String code = promoCode.trim();
        if (code.isEmpty()) {
            return null;
        }

        String sql = "SELECT code, discount_percent FROM formation_promo_code WHERE user_id = ? AND code = ? AND used = 0 LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Promo(rs.getString("code"), rs.getInt("discount_percent"));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void markPromoCodeUsed(int userId, String promoCode) {
        ensurePromoTableExists();
        if (promoCode == null || promoCode.isBlank()) {
            return;
        }
        String sql = "UPDATE formation_promo_code SET used = 1, used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND code = ? AND used = 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, promoCode);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static double applyDiscountPercent(double price, Integer percent) {
        if (percent == null || percent <= 0) {
            return price;
        }
        double p = price * (100.0 - percent) / 100.0;
        return Math.max(0.0, p);
    }

    private static String generatePromoCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        sb.append("CL-");
        for (int i = 0; i < 10; i++) {
            int idx = RNG.nextInt(alphabet.length());
            sb.append(alphabet.charAt(idx));
        }
        return sb.toString().toUpperCase(Locale.ROOT);
    }

    private static final class Promo {
        private final String code;
        private final int discountPercent;

        private Promo(String code, int discountPercent) {
            this.code = code;
            this.discountPercent = discountPercent;
        }
    }

    private static final class PromoEmailState {
        private final String code;
        private final boolean emailSent;

        private PromoEmailState(String code, boolean emailSent) {
            this.code = code;
            this.emailSent = emailSent;
        }
    }

    public enum PromoCodeStatus {
        EMPTY,
        NOT_FOUND,
        USED,
        VALID
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
