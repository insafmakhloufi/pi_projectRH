package Iservices.Offre;
import java.util.List;
import Entities.Offre.Localisation;

public interface ILocalisation extends IEntiteFaible<Localisation> {
    List<Localisation> getByVille(String ville);
    List<Localisation> getByPays(String pays);
}