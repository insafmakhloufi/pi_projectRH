package Utils;

import Entities.Entreprise.Entreprise;
import Entities.User.User;
import Services.Entreprise.EntrepriseService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Utilitaire pour vérifier que les MANAGER_RH ont une entreprise associée.
 * Redirige vers le formulaire entreprise si nécessaire.
 */
public class EntrepriseRequiredCheck {

    private static final EntrepriseService entrepriseService = new EntrepriseService();

    /**
     * Vérifie si l'utilisateur courant (si MANAGER_RH) a une entreprise.
     * Si pas d'entreprise → affiche alerte + redirige vers formulaire.
     * @param stage la fenêtre courante pour la redirection
     * @return true si l'utilisateur peut continuer (a une entreprise ou n'est pas MANAGER_RH)
     *         false si redirection effectuée (pas d'entreprise)
     */
    public static boolean checkAndRedirect(Stage stage) {
        User current = Session.getCurrentUser();
        if (current == null) {
            return false;
        }

        String role = current.getRole();
        if (!"MANAGER_RH".equalsIgnoreCase(role) && !"MANAGERRH".equalsIgnoreCase(role)) {
            // Pas un RH → pas besoin de vérifier
            return true;
        }

        Entreprise entreprise = entrepriseService.getByUserId(current.getId());
        if (entreprise == null) {
            // Pas d'entreprise → afficher alerte et rediriger
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Entreprise requise");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez créer votre entreprise avant d'accéder à cette fonctionnalité.");
            alert.showAndWait();

            redirectToEntrepriseForm(stage);
            return false;
        }

        return true;
    }

    /**
     * Vérifie silencieusement (sans alerte) si le MANAGER_RH a une entreprise.
     * @return true si entreprise existe ou utilisateur n'est pas MANAGER_RH
     */
    public static boolean hasEntreprise() {
        User current = Session.getCurrentUser();
        if (current == null) {
            return false;
        }

        String role = current.getRole();
        if (!"MANAGER_RH".equalsIgnoreCase(role) && !"MANAGERRH".equalsIgnoreCase(role)) {
            return true;
        }

        Entreprise entreprise = entrepriseService.getByUserId(current.getId());
        return entreprise != null;
    }

    private static void redirectToEntrepriseForm(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(EntrepriseRequiredCheck.class.getResource("/Entreprise/AjouterEntreprise.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            ThemeManager.applyTheme(scene);

            stage.setScene(scene);
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setContentText("Impossible d'ouvrir le formulaire entreprise.");
            error.show();
        }
    }
}
