package Utils;

public class EmailVerificationContext {

    private static String email;

    private EmailVerificationContext() {
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        EmailVerificationContext.email = email;
    }

    public static void clear() {
        email = null;
    }
}
