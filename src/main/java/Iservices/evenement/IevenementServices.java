package Iservices.evenement;

import Entities.evenement.Evenement;
import java.util.List;

public interface IevenementServices {
    void ajouterEvenement(Evenement e);
    void modifierEvenement(Evenement e);
    void supprimerEvenement(int id);
    List<Evenement> afficherEvenements();
}
