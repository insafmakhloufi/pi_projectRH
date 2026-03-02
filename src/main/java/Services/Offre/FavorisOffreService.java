package Services.Offre;

import Entities.Offre.FavorisOffre;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour gérer les favoris des offres d'emploi
 */
public class FavorisOffreService {
    
    private Connection connection;
    
    public FavorisOffreService() {
        this.connection = Mydatabase.getInstance().getConnection();
    }
    
    /**
     * Ajouter une offre aux favoris d'un utilisateur
     */
    public boolean ajouterAuxFavoris(Integer userId, Integer offreId) {
        String sql = "INSERT INTO favoris_offre (user_id, offre_id, date_ajout) VALUES (?, ?, ?)";
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.setInt(2, offreId);
            pst.setTimestamp(3, Timestamp.valueOf(java.time.LocalDateTime.now()));
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            // Si doublon (déjà favoris), retourner false
            if (e.getMessage().contains("Duplicate entry")) {
                return false;
            }
            System.err.println("Erreur ajout favoris: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Retirer une offre des favoris
     */
    public boolean retirerDesFavoris(Integer userId, Integer offreId) {
        String sql = "DELETE FROM favoris_offre WHERE user_id = ? AND offre_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.setInt(2, offreId);
            
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur retrait favoris: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Vérifier si une offre est dans les favoris
     */
    public boolean estFavoris(Integer userId, Integer offreId) {
        String sql = "SELECT COUNT(*) FROM favoris_offre WHERE user_id = ? AND offre_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, userId);
            pst.setInt(2, offreId);
            
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification favoris: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Récupérer tous les favoris d'un utilisateur
     */
    public List<FavorisOffre> getFavorisByUser(Integer userId) {
        List<FavorisOffre> favoris = new ArrayList<>();
        String sql = "SELECT * FROM favoris_offre WHERE user_id = ? ORDER BY date_ajout DESC";
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            
            while (rs.next()) {
                FavorisOffre f = new FavorisOffre();
                f.setId(rs.getInt("id"));
                f.setUserId(rs.getInt("user_id"));
                f.setOffreId(rs.getInt("offre_id"));
                f.setDateAjout(rs.getTimestamp("date_ajout").toLocalDateTime());
                favoris.add(f);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération favoris: " + e.getMessage());
        }
        return favoris;
    }
    
    /**
     * Compter les favoris d'un utilisateur
     */
    public int compterFavoris(Integer userId) {
        String sql = "SELECT COUNT(*) FROM favoris_offre WHERE user_id = ?";
        
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur comptage favoris: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Basculer (toggle) favoris : ajoute si absent, retire si présent
     */
    public boolean toggleFavoris(Integer userId, Integer offreId) {
        if (estFavoris(userId, offreId)) {
            return retirerDesFavoris(userId, offreId);
        } else {
            return ajouterAuxFavoris(userId, offreId);
        }
    }
}
