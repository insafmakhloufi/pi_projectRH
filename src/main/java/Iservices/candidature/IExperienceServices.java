package Iservices.candidature;

import Entities.candidature.Experience;
import java.util.List;

public interface IExperienceServices {
    void ajouterExperience(Experience e);
    void supprimerExperience(int id_experience);
    void modifierExperience(Experience e);
    List<Experience> afficherExperiences();
    List<Experience> afficherExperiencesParCandidature(int id_candidature);
}
