package Utils;

import Entities.User.User;

public class Session {

    private static User currentUser;

    private Session() {}

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
