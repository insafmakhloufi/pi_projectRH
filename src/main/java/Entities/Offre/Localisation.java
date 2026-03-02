package Entities.Offre;

public class Localisation extends EntiteFaible {
    private String ville;
    private String pays;
    private String adresse;
    private Double latitude;
    private Double longitude;

    public Localisation() {
        super();
    }

    public Localisation(int id, String ville, String pays) {
        super(id, ville + ", " + pays);
        this.ville = ville;
        this.pays = pays;
    }

    public Localisation(int id, String ville, String pays, String adresse, Double latitude, Double longitude) {
        super(id, buildNom(ville, pays, adresse));
        this.ville = ville;
        this.pays = pays;
        this.adresse = adresse;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters/Setters spécifiques
    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
        this.nom = buildNom(this.ville, this.pays, this.adresse);
    }

    public String getPays() {
        return pays;
    }

    public void setPays(String pays) {
        this.pays = pays;
        this.nom = buildNom(this.ville, this.pays, this.adresse);
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
        this.nom = buildNom(this.ville, this.pays, this.adresse);
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    private static String buildNom(String ville, String pays, String adresse) {
        String a = adresse != null ? adresse.trim() : "";
        if (!a.isEmpty()) {
            return a;
        }
        String v = ville != null ? ville.trim() : "";
        String p = pays != null ? pays.trim() : "";
        if (v.isEmpty() && p.isEmpty()) {
            return "";
        }
        if (v.isEmpty()) {
            return p;
        }
        if (p.isEmpty()) {
            return v;
        }
        return v + ", " + p;
    }

    @Override
    public String getType() {
        return "Localisation";
    }
}