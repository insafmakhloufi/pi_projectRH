package Entities.Offre;

public class TypeEmploi extends EntiteFaible {

    public TypeEmploi() {
        super();
    }

    public TypeEmploi(int id, String nom) {
        super(id, nom);
    }

    @Override
    public String getType() {
        return "TypeEmploi";
    }
}