package Entities.Offre;

public class TypeContrat extends EntiteFaible {
    private String description;

    public TypeContrat() {
        super();
    }

    public TypeContrat(int id, String nom, String description) {
        super(id, nom);
        this.description = description;
    }

    // Getter/Setter spécifique
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return "TypeContrat";
    }
}