package Iservices.Offre;

import java.util.List;
import Entities.Offre.OffreEmploi;

public interface Ioffreemploi {
    void ajouterOffreEmploi(OffreEmploi o);
    void supprimerOffreEmploi(int id);
    void modifierOffreEmploi(OffreEmploi o);
    List<OffreEmploi> afficherOfrreEmploi();
}