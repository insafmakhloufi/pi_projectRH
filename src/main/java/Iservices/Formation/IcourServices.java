package Iservices.Formation;

import Entites.Formation.Cour;

import java.util.List;

public interface IcourServices {
    void ajouterCour(Cour c);
    void supprimerCour(int id);
    void modifierCour(Cour c);
    List<Cour> afficherCours();
}
