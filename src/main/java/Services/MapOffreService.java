package Services;

import Entities.Offre.Localisation;
import Entities.Offre.OffreEmploi;
import Entities.Offre.SecteurActivite;
import Entities.Offre.TypeContrat;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour récupérer les offres avec données de localisation
 * pour affichage sur la carte interactive
 */
public class MapOffreService {
    
    private Connection con = Mydatabase.getInstance().getConnection();
    
    /**
     * Récupère toutes les offres actives avec leurs coordonnées GPS
     */
    public List<OffreMapData> getOffresAvecLocalisation() {
        List<OffreMapData> offres = new ArrayList<>();
        
        String req = "SELECT " +
                "o.id, o.titre, o.description, o.niveau_experience, o.status, " +
                "l.id_localisation, l.ville, l.pays, l.adresse, l.latitude, l.longitude, " +
                "s.nom_secteur, " +
                "tc.nom_type as contrat_nom, " +
                "e.nom_entreprise " +
                "FROM offreemlpoi o " +
                "LEFT JOIN localisation l ON o.id_localisation = l.id_localisation " +
                "LEFT JOIN secteuractivite s ON o.id_secteur = s.id_secteur " +
                "LEFT JOIN typecontrat tc ON o.id_type_contrat = tc.id_type " +
                "LEFT JOIN entreprise e ON o.id_entreprise = e.id_entreprise " +
                "WHERE LOWER(o.status) IN ('active', 'actif', 'publiée', 'publiee', 'ouverte', 'disponible') " +
                "AND (l.latitude IS NOT NULL AND l.longitude IS NOT NULL) " +
                "AND (l.latitude != 0 AND l.longitude != 0) " +
                "ORDER BY o.id DESC";
        
        try (PreparedStatement ps = con.prepareStatement(req)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                OffreMapData data = mapResultSet(rs);
                if (data != null && data.hasValidCoordinates()) {
                    offres.add(data);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur récupération offres pour carte: " + e.getMessage(), e);
        }
        
        return offres;
    }
    
    /**
     * Récupère les offres dans un rayon autour d'un point
     * @param lat Latitude du centre
     * @param lon Longitude du centre  
     * @param rayonKm Rayon en kilomètres
     */
    public List<OffreMapData> getOffresDansRayon(double lat, double lon, double rayonKm) {
        List<OffreMapData> offres = getOffresAvecLocalisation();
        List<OffreMapData> result = new ArrayList<>();
        
        for (OffreMapData offre : offres) {
            double distance = calculerDistance(lat, lon, offre.getLatitude(), offre.getLongitude());
            if (distance <= rayonKm) {
                offre.setDistance(distance);
                result.add(offre);
            }
        }
        
        // Trier par distance
        result.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));
        return result;
    }
    
    /**
     * Filtre par secteur d'activité
     */
    public List<OffreMapData> filtrerParSecteur(List<OffreMapData> offres, int secteurId) {
        List<OffreMapData> result = new ArrayList<>();
        for (OffreMapData offre : offres) {
            if (offre.getSecteurId() == secteurId) {
                result.add(offre);
            }
        }
        return result;
    }
    
    /**
     * Filtre par type de contrat
     */
    public List<OffreMapData> filtrerParTypeContrat(List<OffreMapData> offres, int typeContratId) {
        List<OffreMapData> result = new ArrayList<>();
        for (OffreMapData offre : offres) {
            if (offre.getTypeContratId() == typeContratId) {
                result.add(offre);
            }
        }
        return result;
    }
    
    /**
     * Calcule la distance entre deux points GPS (formule de Haversine)
     */
    private double calculerDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    private OffreMapData mapResultSet(ResultSet rs) throws SQLException {
        OffreMapData data = new OffreMapData();
        
        data.setId(rs.getInt("id"));
        data.setTitre(rs.getString("titre"));
        data.setDescription(rs.getString("description"));
        data.setNiveauExperience(rs.getString("niveau_experience"));
        data.setStatus(rs.getString("status"));
        
        // Localisation
        int locId = rs.getInt("id_localisation");
        if (!rs.wasNull()) {
            data.setLocalisationId(locId);
            data.setVille(rs.getString("ville"));
            data.setPays(rs.getString("pays"));
            data.setAdresse(rs.getString("adresse"));
            
            Double lat = rs.getObject("latitude") != null ? rs.getDouble("latitude") : null;
            Double lon = rs.getObject("longitude") != null ? rs.getDouble("longitude") : null;
            data.setLatitude(lat);
            data.setLongitude(lon);
        }
        
        data.setSecteurNom(rs.getString("nom_secteur"));
        data.setTypeContratNom(rs.getString("contrat_nom"));
        data.setEntrepriseNom(rs.getString("nom_entreprise"));
        
        return data;
    }
    
    /**
     * Classe interne représentant une offre avec données pour la carte
     */
    public static class OffreMapData {
        private int id;
        private String titre;
        private String description;
        private String niveauExperience;
        private String status;
        private int localisationId;
        private String ville;
        private String pays;
        private String adresse;
        private Double latitude;
        private Double longitude;
        private String secteurNom;
        private int secteurId;
        private String typeContratNom;
        private int typeContratId;
        private String entrepriseNom;
        private double distance; // Distance calculée du centre
        
        public boolean hasValidCoordinates() {
            return latitude != null && longitude != null 
                    && latitude != 0 && longitude != 0
                    && !Double.isNaN(latitude) && !Double.isNaN(longitude);
        }
        
        // Getters et Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getNiveauExperience() { return niveauExperience; }
        public void setNiveauExperience(String niveauExperience) { this.niveauExperience = niveauExperience; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getLocalisationId() { return localisationId; }
        public void setLocalisationId(int localisationId) { this.localisationId = localisationId; }
        
        public String getVille() { return ville; }
        public void setVille(String ville) { this.ville = ville; }
        
        public String getPays() { return pays; }
        public void setPays(String pays) { this.pays = pays; }
        
        public String getAdresse() { return adresse; }
        public void setAdresse(String adresse) { this.adresse = adresse; }
        
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        
        public String getSecteurNom() { return secteurNom; }
        public void setSecteurNom(String secteurNom) { this.secteurNom = secteurNom; }
        
        public int getSecteurId() { return secteurId; }
        public void setSecteurId(int secteurId) { this.secteurId = secteurId; }
        
        public String getTypeContratNom() { return typeContratNom; }
        public void setTypeContratNom(String typeContratNom) { this.typeContratNom = typeContratNom; }
        
        public int getTypeContratId() { return typeContratId; }
        public void setTypeContratId(int typeContratId) { this.typeContratId = typeContratId; }
        
        public String getEntrepriseNom() { return entrepriseNom; }
        public void setEntrepriseNom(String entrepriseNom) { this.entrepriseNom = entrepriseNom; }
        
        public double getDistance() { return distance; }
        public void setDistance(double distance) { this.distance = distance; }
        
        /**
         * Retourne un résumé amélioré pour le popup sur la carte avec bouton Postuler
         */
        public String getPopupContent() {
            StringBuilder sb = new StringBuilder();
            sb.append("<div style='min-width:200px;max-width:280px;'>");
            
            // Titre
            sb.append("<h3 style='margin:0 0 8px 0;font-size:14px;color:#001D39;border-bottom:2px solid #3B82F6;padding-bottom:6px;'>");
            sb.append(titre != null ? titre : "Poste");
            sb.append("</h3>");
            
            // Détails en grille
            sb.append("<div style='font-size:12px;line-height:1.6;color:#333;'>");
            
            if (entrepriseNom != null) {
                sb.append("<div style='margin-bottom:4px;'><b style='color:#2563EB;'>Entreprise:</b> ").append(entrepriseNom).append("</div>");
            }
            if (typeContratNom != null) {
                sb.append("<div style='margin-bottom:4px;'><b style='color:#2563EB;'>Contrat:</b> ").append(typeContratNom).append("</div>");
            }
            if (niveauExperience != null && !niveauExperience.isEmpty()) {
                sb.append("<div style='margin-bottom:4px;'><b style='color:#2563EB;'>Expérience:</b> ").append(niveauExperience).append("</div>");
            }
            if (secteurNom != null) {
                sb.append("<div style='margin-bottom:4px;'><b style='color:#2563EB;'>Secteur:</b> ").append(secteurNom).append("</div>");
            }
            if (ville != null) {
                sb.append("<div style='margin-bottom:4px;'><b style='color:#2563EB;'>Lieu:</b> ").append(ville);
                if (pays != null) sb.append(", ").append(pays);
                sb.append("</div>");
            }
            if (distance > 0) {
                sb.append("<div style='margin-bottom:4px;color:#059669;'><b>Distance:</b> ")
                  .append(String.format("%.1f km", distance)).append("</div>");
            }
            sb.append("</div>");
            
            // Bouton Postuler
            sb.append("<div style='margin-top:12px;text-align:center;'>");
            sb.append("<button onclick='postulerOffre(").append(id).append(")' ");
            sb.append("style='background:#2563EB;color:white;border:none;padding:8px 20px;");
            sb.append("border-radius:20px;cursor:pointer;font-size:12px;font-weight:bold;");
            sb.append("box-shadow:0 2px 4px rgba(37,99,235,0.3);transition:all 0.2s;'>");
            sb.append("📄 Postuler");
            sb.append("</button>");
            sb.append("</div>");
            
            sb.append("</div>");
            return sb.toString();
        }
        
        @Override
        public String toString() {
            return String.format("OffreMapData{id=%d, titre='%s', lat=%.5f, lon=%.5f, ville='%s'}",
                    id, titre, latitude != null ? latitude : 0, longitude != null ? longitude : 0, ville);
        }
    }
}
