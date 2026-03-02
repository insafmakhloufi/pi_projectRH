package Entities.candidature;

import java.sql.Date;

public class Candidature {

    private int IDCandidat;

    private Integer userId;

    private String prenom;
    private String nom;
    private String email;

    private String indicatif;
    private String tel;          // IMPORTANT: tel doit être String (pas int) car numéro + formats
    private String adresse;
    private String ville;

    private Date date_naissance;

    private String highest_degree;
    private String institution;
    private String lettre_motivation;
    private String skills_text;

    private String Pieces_jointes;
    private String video_path;

    private String statut;

    private String decisionRh;

    private boolean confirm_info;
    private boolean accept_privacy;
    private Integer offreId;



    public Candidature() {}

    // constructeur complet (si tu veux)
    public Candidature(int IDCandidat, String prenom, String nom, String email, String indicatif, String tel,
                       String adresse, String ville, Date date_naissance, String highest_degree, String institution,
                       String lettre_motivation, String skills_text, String pieces_jointes, String video_path,
                       boolean confirm_info, boolean accept_privacy) {
        this.IDCandidat = IDCandidat;
        this.prenom = prenom;
        this.nom = nom;
        this.email = email;
        this.indicatif = indicatif;
        this.tel = tel;
        this.adresse = adresse;
        this.ville = ville;
        this.date_naissance = date_naissance;
        this.highest_degree = highest_degree;
        this.institution = institution;
        this.lettre_motivation = lettre_motivation;
        this.skills_text = skills_text;
        this.Pieces_jointes = pieces_jointes;
        this.video_path = video_path;
        this.confirm_info = confirm_info;
        this.accept_privacy = accept_privacy;
    }

    // -------- getters/setters --------
    public int getIDCandidat() { return IDCandidat; }
    public void setIDCandidat(int IDCandidat) { this.IDCandidat = IDCandidat; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getIndicatif() { return indicatif; }
    public void setIndicatif(String indicatif) { this.indicatif = indicatif; }

    public String getTel() { return tel; }
    public void setTel(String tel) { this.tel = tel; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public Date getDate_naissance() { return date_naissance; }
    public void setDate_naissance(Date date_naissance) { this.date_naissance = date_naissance; }

    public String getHighest_degree() { return highest_degree; }
    public void setHighest_degree(String highest_degree) { this.highest_degree = highest_degree; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getLettre_motivation() { return lettre_motivation; }
    public void setLettre_motivation(String lettre_motivation) { this.lettre_motivation = lettre_motivation; }

    public String getSkills_text() { return skills_text; }
    public void setSkills_text(String skills_text) { this.skills_text = skills_text; }

    public String getPieces_jointes() { return Pieces_jointes; }
    public void setPieces_jointes(String pieces_jointes) { Pieces_jointes = pieces_jointes; }

    public String getVideo_path() { return video_path; }
    public void setVideo_path(String video_path) { this.video_path = video_path; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getDecisionRh() { return decisionRh; }
    public void setDecisionRh(String decisionRh) { this.decisionRh = decisionRh; }

    public boolean isConfirm_info() { return confirm_info; }
    public void setConfirm_info(boolean confirm_info) { this.confirm_info = confirm_info; }

    public boolean isAccept_privacy() { return accept_privacy; }
    public void setAccept_privacy(boolean accept_privacy) { this.accept_privacy = accept_privacy; }

    public Integer getOffreId() { return offreId; }
    public void setOffreId(Integer offreId) { this.offreId = offreId; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Candidature that = (Candidature) o;

        return IDCandidat == that.IDCandidat &&
                confirm_info == that.confirm_info &&
                accept_privacy == that.accept_privacy &&
                java.util.Objects.equals(prenom, that.prenom) &&
                java.util.Objects.equals(nom, that.nom) &&
                java.util.Objects.equals(email, that.email) &&
                java.util.Objects.equals(indicatif, that.indicatif) &&
                java.util.Objects.equals(tel, that.tel) &&
                java.util.Objects.equals(adresse, that.adresse) &&
                java.util.Objects.equals(ville, that.ville) &&
                java.util.Objects.equals(date_naissance, that.date_naissance) &&
                java.util.Objects.equals(highest_degree, that.highest_degree) &&
                java.util.Objects.equals(institution, that.institution) &&
                java.util.Objects.equals(lettre_motivation, that.lettre_motivation) &&
                java.util.Objects.equals(skills_text, that.skills_text) &&
                java.util.Objects.equals(Pieces_jointes, that.Pieces_jointes) &&
                java.util.Objects.equals(video_path, that.video_path);
    }

    @Override
    public String toString() {
        return "Candidature{" +
                "IDCandidat=" + IDCandidat +
                ", prenom='" + prenom + '\'' +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", indicatif='" + indicatif + '\'' +
                ", tel='" + tel + '\'' +
                ", adresse='" + adresse + '\'' +
                ", ville='" + ville + '\'' +
                ", date_naissance=" + date_naissance +
                ", highest_degree='" + highest_degree + '\'' +
                ", institution='" + institution + '\'' +
                ", lettre_motivation='" + lettre_motivation + '\'' +
                ", skills_text='" + skills_text + '\'' +
                ", Pieces_jointes='" + Pieces_jointes + '\'' +
                ", video_path='" + video_path + '\'' +
                ", confirm_info=" + confirm_info +
                ", accept_privacy=" + accept_privacy +
                '}';
    }

}
