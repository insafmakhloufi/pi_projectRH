package Services.User;

import Entites.User.User;
import Iservices.User.IUserService;
import Utils.Mydatabase;
import Utils.PhoneUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IUserService {

    private final Connection conn;

    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_MINUTES = 15;

    public UserService() {
        this.conn = Mydatabase.getInstance().getConnection();
    }

    public UserService(Connection conn) {
        this.conn = conn;
    }

    // =====================================================
    // CREATE USER
    // =====================================================
    public boolean addUser(User user) {

        if (!isValidUser(user)) {
            System.out.println("[ERROR] Invalid user data");
            return false;
        }

        if (emailExists(user.getEmail())) {
            System.out.println("[ERROR] Email already exists");
            return false;
        }

        if (phoneExists(user.getPhone())) {
            System.out.println("[ERROR] Phone already exists");
            return false;
        }

        if (fullNameExists(user.getFullName())) {
            System.out.println("[ERROR] Full name already exists");
            return false;
        }

        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());

        String sql = """
                INSERT INTO user (full_name, titre, email, phone, profile_photo, password, role)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getTitre());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getProfilePhoto());
            ps.setString(6, hashedPassword);
            ps.setString(7, user.getRole());

            ps.executeUpdate();
            System.out.println("[OK] User added successfully");
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Database error while adding user");
            e.printStackTrace();
        }

        return false;
    }

    // =====================================================
    // READ USERS
    // =====================================================
    public List<User> getAllUsers() {

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Error fetching users");
            e.printStackTrace();
        }

        return users;
    }

    // =====================================================
    // UPDATE USER (WITHOUT PASSWORD CHANGE)
    // =====================================================
    public boolean updateUser(User user) {

        if (emailExistsForOther(user.getEmail(), user.getId())) {
            System.out.println("[ERROR] Email already used by another user");
            return false;
        }

        if (phoneExistsForOther(user.getPhone(), user.getId())) {
            System.out.println("[ERROR] Phone already used by another user");
            return false;
        }

        if (fullNameExistsForOther(user.getFullName(), user.getId())) {
            System.out.println("[ERROR] Full name already used by another user");
            return false;
        }

        String sql = """
                UPDATE user
                SET full_name=?, titre=?, email=?, phone=?, profile_photo=?, role=?
                WHERE id=?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getFullName());
            ps.setString(2, user.getTitre());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getProfilePhoto());
            ps.setString(6, user.getRole());
            ps.setInt(7, user.getId());

            ps.executeUpdate();
            System.out.println("[OK] User updated successfully");
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Error updating user");
            e.printStackTrace();
        }

        return false;
    }

    // =====================================================
    // DELETE USER
    // =====================================================
    public boolean deleteUser(int id) {

        String sql = "DELETE FROM user WHERE id=?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("[OK] User deleted successfully");
            return true;

        } catch (SQLException e) {
            System.out.println("[ERROR] Error deleting user");
            e.printStackTrace();
        }

        return false;
    }

    // =====================================================
    // LOGIN
    // =====================================================
    public User login(String email, String password) {
        return loginWithIdentifier(email, password);
    }

    public User loginWithIdentifier(String identifier, String password) {

        String normalizedIdentifier = identifier;
        if (normalizedIdentifier != null) {
            String maybePhone = PhoneUtil.normalizeIdentifierPhoneTN(normalizedIdentifier);
            normalizedIdentifier = maybePhone;
        }

        String sql = "SELECT * FROM user WHERE email=? OR phone=? OR full_name=? LIMIT 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, normalizedIdentifier);
            ps.setString(2, normalizedIdentifier);
            ps.setString(3, identifier);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    String lockMsg = getLockMessageIfLocked(rs);
                    if (lockMsg != null) {
                        throw new RuntimeException(lockMsg);
                    }

                    int id = rs.getInt("id");
                    String storedPassword = rs.getString("password");

                    if (storedPassword != null) {
                        boolean matches = false;

                        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                            try {
                                matches = BCrypt.checkpw(password, storedPassword);
                            } catch (IllegalArgumentException ignored) {
                                matches = false;
                            }
                        } else {
                            matches = password.equals(storedPassword);
                            if (matches) {
                                String newHash = BCrypt.hashpw(password, BCrypt.gensalt());
                                updatePasswordHash(id, newHash);
                            }
                        }

                        if (matches) {
                            EmailVerificationService verificationService = new EmailVerificationService(conn);
                            if (!verificationService.isVerified(id)) {
                                throw new RuntimeException("Please verify your email before logging in");
                            }

                            TotpService totpService = new TotpService(conn);
                            if (totpService.isEnabled(id)) {
                                throw new RuntimeException("TOTP_REQUIRED|" + id);
                            }

                            resetFailedAttemptsIfSupported(id);
                            return extractUserFromResultSet(rs);
                        }

                        incrementFailedAttemptsIfSupported(id);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERROR] Login error");
            e.printStackTrace();
        }

        return null;
    }

    public User getUserById(int id) {
        String sql = "SELECT * FROM user WHERE id=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return null;

        String normalizedIdentifier = identifier.trim();
        String maybePhone = PhoneUtil.normalizeIdentifierPhoneTN(normalizedIdentifier);
        if (maybePhone != null && !maybePhone.isBlank()) {
            normalizedIdentifier = maybePhone;
        }

        String sql = "SELECT * FROM user WHERE email=? OR phone=? OR full_name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedIdentifier);
            ps.setString(2, normalizedIdentifier);
            ps.setString(3, identifier.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void updatePasswordHash(int id, String hashedPassword) {
        String sql = "UPDATE user SET password=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashedPassword);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getLockMessageIfLocked(ResultSet rs) {
        try {
            Timestamp lockUntil = rs.getTimestamp("lock_until");
            if (lockUntil == null) return null;

            java.time.Instant now = java.time.Instant.now();
            java.time.Instant until = lockUntil.toInstant();
            if (!until.isAfter(now)) return null;

            return "LOCKED|" + lockUntil.getTime();
        } catch (SQLException e) {
            return "Account locked. Try again later.";
        }
    }

    private void incrementFailedAttemptsIfSupported(int userId) {
        String sql = "UPDATE user SET failed_attempts = COALESCE(failed_attempts, 0) + 1, " +
                "lock_until = CASE WHEN COALESCE(failed_attempts, 0) + 1 >= ? THEN DATE_ADD(NOW(), INTERVAL ? MINUTE) ELSE lock_until END " +
                "WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, MAX_FAILED_ATTEMPTS);
            ps.setInt(2, LOCK_MINUTES);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    private void resetFailedAttemptsIfSupported(int userId) {
        String sql = "UPDATE user SET failed_attempts=0, lock_until=NULL WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

    // =====================================================
    // PUBLIC UNIQUE CHECK METHODS (FOR CONTROLLER)
    // =====================================================
    public boolean emailExists(String email) {
        return exists("SELECT id FROM user WHERE email=?", email);
    }

    public boolean phoneExists(String phone) {
        return exists("SELECT id FROM user WHERE phone=?", phone);
    }

    public boolean fullNameExists(String fullName) {
        return exists("SELECT id FROM user WHERE full_name=?", fullName);
    }

    public boolean emailExistsForOther(String email, int id) {
        return exists("SELECT id FROM user WHERE email=? AND id<>?", email, id);
    }

    public boolean phoneExistsForOther(String phone, int id) {
        return exists("SELECT id FROM user WHERE phone=? AND id<>?", phone, id);
    }

    public boolean fullNameExistsForOther(String fullName, int id) {
        return exists("SELECT id FROM user WHERE full_name=? AND id<>?", fullName, id);
    }

    // =====================================================
    // PRIVATE HELPERS
    // =====================================================

    private boolean exists(String sql, String value) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean exists(String sql, String value, int id) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            ps.setInt(2, id);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isValidUser(User user) {
        return user != null &&
                user.getFullName() != null && !user.getFullName().isEmpty() &&
                user.getEmail() != null && !user.getEmail().isEmpty() &&
                user.getPhone() != null && !user.getPhone().isEmpty() &&
                user.getPassword() != null && !user.getPassword().isEmpty() &&
                user.getRole() != null && !user.getRole().isEmpty();
    }

    private User createUserFromRole(int id,
                                   String fullName,
                                   String titre,
                                   String email,
                                   String phone,
                                   String profilePhoto,
                                   String password,
                                   String role) {
        return new User(id, fullName, titre, email, phone, profilePhoto, password, role);
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String fullName = rs.getString("full_name");
        String titre = rs.getString("titre");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String profilePhoto = rs.getString("profile_photo");
        String password = rs.getString("password");
        String role = rs.getString("role");

        return createUserFromRole(id, fullName, titre, email, phone, profilePhoto, password, role);
    }
}
