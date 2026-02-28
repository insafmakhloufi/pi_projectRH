package Iservices.Offre;
import java.util.List;
import Entities.Offre.TypeContrat;

public interface ITypeContrat extends IEntiteFaible<TypeContrat> {
    // Méthodes spécifiques à TypeContrat si nécessaire
    List<TypeContrat> getByNom(String nom);
}