package Iservices.Formation;
import Entities.Formation.Formation;

import java.util.List;

public interface IformationServices {
    void ajouterFormation(Formation f);
    void supprimerFormation(int id);
    void modifierFormation(Formation f);
    List<Formation> afficherFormations();
}
