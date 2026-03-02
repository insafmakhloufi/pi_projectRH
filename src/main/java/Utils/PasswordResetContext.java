package Utils;

import Services.User.PasswordResetService;

public class PasswordResetContext {

    private static String identifier;
    private static PasswordResetService.Channel channel;

    private PasswordResetContext() {
    }

    public static String getIdentifier() {
        return identifier;
    }

    public static void setIdentifier(String identifier) {
        PasswordResetContext.identifier = identifier;
    }

    public static PasswordResetService.Channel getChannel() {
        return channel;
    }

    public static void setChannel(PasswordResetService.Channel channel) {
        PasswordResetContext.channel = channel;
    }

    public static void clear() {
        identifier = null;
        channel = null;
    }
}
