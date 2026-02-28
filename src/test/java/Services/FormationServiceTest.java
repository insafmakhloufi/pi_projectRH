package Services;

import Entities.Formation.Formation;
import Services.Formation.FormationServices;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FormationServiceTest {
    static FormationServices service;

    @BeforeAll
    static void setup() {
        service = new FormationServices();
    }

    static int idFormationTest;

    @Test
    @Order(1)
    void testAjouterFormation() throws SQLException {
        Formation f = new Formation("Tunis", "TestTitre", "TestDescription", "TestDomaine", Date.valueOf("2026-02-10"), 20.5f);
        service.ajouterFormation(f);
        List<Formation> formations = service.afficherFormations();
        assertFalse(formations.isEmpty());
        assertTrue(
                formations.stream().anyMatch(form -> form.getTitre().equals("TestTitre")
                )
        );
        idFormationTest = formations.get(formations.size()-1).getId();
        System.out.println(idFormationTest);
    }

    @Test
    @Order(2)
    void testModifierFormation() throws SQLException {
        Formation f = new Formation();
        f.setId(idFormationTest);
        f.setDescription("DescriptionModifie");
        service.modifierFormation(f);
        List<Formation> formations = service.afficherFormations();
        boolean trouve = formations.stream()
                .anyMatch(form -> "DescriptionModifie".equals(form.getDescription()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerFormation() throws SQLException {
        service.supprimerFormation(idFormationTest);
        List<Formation> formations = service.afficherFormations();
        boolean existe = formations.stream()
                .anyMatch(f -> f.getId() == idFormationTest);
        assertFalse(existe);
    }

    @AfterAll
    static void cleanUp() throws SQLException {
        if (service == null) {
            return;
        }
        if (idFormationTest <= 0) {
            return;
        }
        List<Formation> formations = service.afficherFormations();
        boolean existe = formations.stream().anyMatch(f -> f.getId() == idFormationTest);
        if (existe) {
            service.supprimerFormation(idFormationTest);
        }
    }
}
