package Entities.Offre;

import java.time.LocalDate;

public class OffreEmploi {

    private int id;
    private String titre;
    private String description;
    private String status;

    private String niveauExperience;
    private String niveauEtudes;
    private String languesRequises;
    private Integer nombrePoste;
    private LocalDate dateLimiteCandidature;
    private Boolean teletravail;
    private LocalDate datePublication; // Date de publication de l'offre

    private Integer idEntreprise;

    private String nomEntreprise;
    private String logoEntreprise;

    // Attributs pour les entités faibles
    private TypeContrat typeContrat;
    private Localisation localisation;
    private SecteurActivite secteur;
    private TypeEmploi typeEmploi;

    // Constructeurs
    public OffreEmploi() {}

    public OffreEmploi(int id, String titre, String description, String status) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.status = status;
    }

    public OffreEmploi(String titre, String description, String status) {
        this.titre = titre;
        this.description = description;
        this.status = status;
    }

    // Constructeur complet avec entités faibles
    public OffreEmploi(String titre, String description, String status,
                       TypeContrat typeContrat, Localisation localisation,
                       SecteurActivite secteur, TypeEmploi typeEmploi) {
        this.titre = titre;
        this.description = description;
        this.status = status;
        this.typeContrat = typeContrat;
        this.localisation = localisation;
        this.secteur = secteur;
        this.typeEmploi = typeEmploi;
    }

    // Getters et Setters de base
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNiveauExperience() { return niveauExperience; }
    public void setNiveauExperience(String niveauExperience) { this.niveauExperience = niveauExperience; }

    public String getNiveauEtudes() { return niveauEtudes; }
    public void setNiveauEtudes(String niveauEtudes) { this.niveauEtudes = niveauEtudes; }

    public String getLanguesRequises() { return languesRequises; }
    public void setLanguesRequises(String languesRequises) { this.languesRequises = languesRequises; }

    public Integer getNombrePoste() { return nombrePoste; }
    public void setNombrePoste(Integer nombrePoste) { this.nombrePoste = nombrePoste; }

    public LocalDate getDateLimiteCandidature() { return dateLimiteCandidature; }
    public void setDateLimiteCandidature(LocalDate dateLimiteCandidature) { this.dateLimiteCandidature = dateLimiteCandidature; }

    public Boolean getTeletravail() { return teletravail; }
    public void setTeletravail(Boolean teletravail) { this.teletravail = teletravail; }

    public LocalDate getDatePublication() { return datePublication; }
    public void setDatePublication(LocalDate datePublication) { this.datePublication = datePublication; }

    /**
     * Vérifie si l'offre est nouvelle (publiée dans les 7 derniers jours)
     * @return true si l'offre a moins de 7 jours
     */
    public boolean isNouveau() {
        if (datePublication == null) return false;
        return datePublication.isAfter(LocalDate.now().minusDays(7));
    }

    public Integer getIdEntreprise() { return idEntreprise; }
    public void setIdEntreprise(Integer idEntreprise) { this.idEntreprise = idEntreprise; }

    public String getNomEntreprise() { return nomEntreprise; }
    public void setNomEntreprise(String nomEntreprise) { this.nomEntreprise = nomEntreprise; }

    public String getLogoEntreprise() { return logoEntreprise; }
    public void setLogoEntreprise(String logoEntreprise) { this.logoEntreprise = logoEntreprise; }

    // Getters et Setters pour les entités faibles
    public TypeContrat getTypeContrat() { return typeContrat; }
    public void setTypeContrat(TypeContrat typeContrat) { this.typeContrat = typeContrat; }

    public Localisation getLocalisation() { return localisation; }
    public void setLocalisation(Localisation localisation) { this.localisation = localisation; }

    public SecteurActivite getSecteur() { return secteur; }
    public void setSecteur(SecteurActivite secteur) { this.secteur = secteur; }

    public TypeEmploi getTypeEmploi() { return typeEmploi; }
    public void setTypeEmploi(TypeEmploi typeEmploi) { this.typeEmploi = typeEmploi; }

    @Override
    public String toString() {
        return "OffreEmploi{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", status='" + status + '\'' +
                ", niveauExperience='" + niveauExperience + '\'' +
                ", niveauEtudes='" + niveauEtudes + '\'' +
                ", languesRequises='" + languesRequises + '\'' +
                ", nombrePoste=" + nombrePoste +
                ", dateLimiteCandidature=" + dateLimiteCandidature +
                ", teletravail=" + teletravail +
                ", idEntreprise=" + idEntreprise +
                ", nomEntreprise='" + nomEntreprise + '\'' +
                ", logoEntreprise='" + logoEntreprise + '\'' +
                ", typeContrat=" + (typeContrat != null ? typeContrat.getNom() : "null") +
                ", localisation=" + (localisation != null ? localisation.getVille() : "null") +
                ", secteur=" + (secteur != null ? secteur.getNom() : "null") +
                ", typeEmploi=" + (typeEmploi != null ? typeEmploi.getNom() : "null") +
                '}';
    }
}