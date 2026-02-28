package Entities.Offre;

public class SecteurActivite extends EntiteFaible {

    public SecteurActivite() {
        super();
    }

    public SecteurActivite(int id, String nom) {
        super(id, nom);
    }

    @Override
    public String getType() {
        return "SecteurActivite";
    }
}