package Iservices.evenement;

import Entities.evenement.Publication;
import java.util.List;

public interface IpublicationServices {
    void ajouterPublication(Publication p);
    void modifierPublication(Publication p);
    void supprimerPublication(int id);
    List<Publication> afficherPublications();
    List<Publication> afficherPublicationsParEvenement(int eventId);
}
