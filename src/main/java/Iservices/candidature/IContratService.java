package Iservices.candidature;

import Entities.candidature.Contrat;
import java.util.List;

public interface IContratService {
    void ajouterContrat(Contrat c);
    void modifierContrat(Contrat c);
    void supprimerContrat(int id);
    Contrat getContratParCandidature(int candidature_id);
    Contrat getContratById(int id);
    List<Contrat> afficherContrats();
    // void signerContrat(int id);
    void signerContrat(int id, String signatureBase64);

}