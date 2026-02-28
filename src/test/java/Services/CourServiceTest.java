package Services;

import Entities.Formation.Cour;
import Services.Formation.CourServices;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CourServiceTest {

    static CourServices service;

    @BeforeAll
    static void setup() {
        service = new CourServices();
    }

    static int idCourTest;

    @Test
    @Order(1)
    void testAjouterCour() throws SQLException {
        Cour c = new Cour(
                "Formateur Test",
                3,
                "Description test",
                "Chapitre 1",
                "Origine test",
                2.5f,
                "Contenu test"
        );
        service.ajouterCour(c);

        List<Cour> cours = service.afficherCours();
        assertFalse(cours.isEmpty());
        assertTrue(
                cours.stream().anyMatch(co -> "Formateur Test".equals(co.getNomFormateur()))
        );

        idCourTest = cours.get(cours.size() - 1).getId();
        System.out.println(idCourTest);
    }

    @Test
    @Order(2)
    void testModifierCour() throws SQLException {
        Cour c = new Cour();
        c.setId(idCourTest);
        c.setDescription("DescriptionModifie");
        service.modifierCour(c);

        List<Cour> cours = service.afficherCours();
        boolean trouve = cours.stream()
                .anyMatch(co -> "DescriptionModifie".equals(co.getDescription()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerCour() throws SQLException {
        service.supprimerCour(idCourTest);
        List<Cour> cours = service.afficherCours();
        boolean existe = cours.stream()
                .anyMatch(c -> c.getId() == idCourTest);
        assertFalse(existe);
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        if (service == null) {
            return;
        }
        if (idCourTest <= 0) {
            return;
        }
        List<Cour> cours = service.afficherCours();
        boolean existe = cours.stream().anyMatch(c -> c.getId() == idCourTest);
        if (existe) {
            service.supprimerCour(idCourTest);
        }
    }
}
