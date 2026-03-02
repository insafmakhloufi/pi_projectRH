package Entities.Entreprise;

public class Entreprise {

    private int idEntreprise;
    private String nomEntreprise;
    private String logo;
    private String adresse;
    private String ville;
    private String telephone;
    private String emailContact;
    private String description;
    private int idUser;

    public Entreprise() {
    }

    public Entreprise(int idEntreprise, String nomEntreprise, String adresse, String ville,
                      String telephone, String emailContact, String description, int idUser) {
        this.idEntreprise = idEntreprise;
        this.nomEntreprise = nomEntreprise;
        this.adresse = adresse;
        this.ville = ville;
        this.telephone = telephone;
        this.emailContact = emailContact;
        this.description = description;
        this.idUser = idUser;
    }

    public int getIdEntreprise() {
        return idEntreprise;
    }

    public void setIdEntreprise(int idEntreprise) {
        this.idEntreprise = idEntreprise;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmailContact() {
        return emailContact;
    }

    public void setEmailContact(String emailContact) {
        this.emailContact = emailContact;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }
}
