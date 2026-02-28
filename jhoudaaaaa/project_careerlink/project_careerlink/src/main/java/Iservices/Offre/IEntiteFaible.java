package Iservices.Offre;

import java.util.List;
import Entities.Offre.EntiteFaible;

public interface IEntiteFaible<T extends EntiteFaible> {
    List<T> getAll();
    T getById(int id);
    void ajouter(T entite);
    void modifier(T entite);
    void supprimer(int id);
}