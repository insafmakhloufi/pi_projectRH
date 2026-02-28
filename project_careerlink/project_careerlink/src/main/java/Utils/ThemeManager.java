package Utils;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Window;

import java.util.prefs.Preferences;

public class ThemeManager {

    private static final String PREF_KEY_DARK = "theme.dark";
    private static final String BASE_STYLESHEET = "/User/css/style.css";
    private static final String DARK_STYLESHEET = "/User/css/dark.css";

    private static final Preferences PREFS = Preferences.userNodeForPackage(ThemeManager.class);

    private ThemeManager() {}

    public static boolean isDarkMode() {
        return PREFS.getBoolean(PREF_KEY_DARK, false);
    }

    public static void setDarkMode(boolean enabled) {
        PREFS.putBoolean(PREF_KEY_DARK, enabled);
    }

    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        scene.getStylesheets().clear();
        scene.getStylesheets().add(ThemeManager.class.getResource(BASE_STYLESHEET).toExternalForm());

        if (isDarkMode() && isDashboardScene(scene)) {
            scene.getStylesheets().add(ThemeManager.class.getResource(DARK_STYLESHEET).toExternalForm());
        }
    }

    public static void applyThemeToAllOpenWindows() {
        for (Window window : Window.getWindows()) {
            if (window == null) continue;
            Scene scene = window.getScene();
            if (scene == null) continue;
            applyTheme(scene);
        }
    }

    private static boolean isDashboardScene(Scene scene) {
        Parent root = scene.getRoot();
        return root != null && root.getStyleClass().contains("app-root");
    }
}
