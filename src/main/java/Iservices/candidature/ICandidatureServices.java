package Iservices.candidature;
import Entities.candidature.Candidature;
import java.util.List;

public interface ICandidatureServices {
    void ajouterCandidature(Candidature c);
    void supprimerCandidature(int id);
    void modifierCandidature(Candidature c);
    List<Candidature> afficherCandidature();
}
