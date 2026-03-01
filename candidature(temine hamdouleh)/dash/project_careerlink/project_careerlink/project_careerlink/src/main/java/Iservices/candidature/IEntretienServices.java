package Iservices.candidature;

import Entities.candidature.Entretien;
import java.util.List;

public interface IEntretienServices {

    void ajouterEntretien(Entretien e);

    void supprimerEntretien(int id);

    void modifierEntretien(Entretien e);

    List<Entretien> afficherEntretiens();
}
