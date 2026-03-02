package Iservices.candidature;

import Entities.candidature.Certification;
import java.util.List;

public interface ICertificationServices {
    void ajouterCertification(Certification c);
    void supprimerCertification(int id_certification);
    void modifierCertification(Certification c);
    List<Certification> afficherCertifications();
    List<Certification> afficherCertificationsParCandidature(int id_candidature);
}
