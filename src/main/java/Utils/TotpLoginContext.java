package Utils;

public class TotpLoginContext {

    private static int userId;

    private TotpLoginContext() {
    }

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int userId) {
        TotpLoginContext.userId = userId;
    }

    public static void clear() {
        userId = 0;
    }
}
