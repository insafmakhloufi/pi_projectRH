package Entities.candidature;

import java.sql.Date;
import java.sql.Timestamp;

public class Contrat {

    private int id;
    private int candidature_id;
    private Integer candidat_id;

    // Infos offre (en dur pour l'instant)
    private String type_contrat;
    private String type_emploi;
    private String secteur;
    private String localisation;
    private String nom_entreprise;

    // Infos saisies par RH
    private Date date_debut;
    private Date date_fin;
    private Double salaire;
    private String horaire_travail;
    private String periode_essai;
    private String avantages;
    private String mode_travail;

    // Statut et signature
    private String statut; // GENERATED, SIGNED, REFUSED
    private Timestamp signed_at;
    private String qr_hash;

    private Timestamp created_at;

    public Contrat() {}

    // -------- Getters / Setters --------

    private String signature_image;

    public String getSignature_image() { return signature_image; }
    public void setSignature_image(String signature_image) { this.signature_image = signature_image; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCandidature_id() { return candidature_id; }
    public void setCandidature_id(int candidature_id) { this.candidature_id = candidature_id; }

    public Integer getCandidat_id() { return candidat_id; }
    public void setCandidat_id(Integer candidat_id) { this.candidat_id = candidat_id; }

    public String getType_contrat() { return type_contrat; }
    public void setType_contrat(String type_contrat) { this.type_contrat = type_contrat; }

    public String getType_emploi() { return type_emploi; }
    public void setType_emploi(String type_emploi) { this.type_emploi = type_emploi; }

    public String getSecteur() { return secteur; }
    public void setSecteur(String secteur) { this.secteur = secteur; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getNom_entreprise() { return nom_entreprise; }
    public void setNom_entreprise(String nom_entreprise) { this.nom_entreprise = nom_entreprise; }

    public Date getDate_debut() { return date_debut; }
    public void setDate_debut(Date date_debut) { this.date_debut = date_debut; }

    public Date getDate_fin() { return date_fin; }
    public void setDate_fin(Date date_fin) { this.date_fin = date_fin; }

    public Double getSalaire() { return salaire; }
    public void setSalaire(Double salaire) { this.salaire = salaire; }

    public String getHoraire_travail() { return horaire_travail; }
    public void setHoraire_travail(String horaire_travail) { this.horaire_travail = horaire_travail; }

    public String getPeriode_essai() { return periode_essai; }
    public void setPeriode_essai(String periode_essai) { this.periode_essai = periode_essai; }

    public String getAvantages() { return avantages; }
    public void setAvantages(String avantages) { this.avantages = avantages; }

    public String getMode_travail() { return mode_travail; }
    public void setMode_travail(String mode_travail) { this.mode_travail = mode_travail; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public Timestamp getSigned_at() { return signed_at; }
    public void setSigned_at(Timestamp signed_at) { this.signed_at = signed_at; }

    public String getQr_hash() { return qr_hash; }
    public void setQr_hash(String qr_hash) { this.qr_hash = qr_hash; }

    public Timestamp getCreated_at() { return created_at; }
    public void setCreated_at(Timestamp created_at) { this.created_at = created_at; }

    @Override
    public String toString() {
        return "Contrat{" +
                "id=" + id +
                ", candidature_id=" + candidature_id +
                ", type_contrat='" + type_contrat + '\'' +
                ", statut='" + statut + '\'' +
                ", date_debut=" + date_debut +
                '}';
    }
}