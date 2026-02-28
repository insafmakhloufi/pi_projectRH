package Services.Offre;

import Entities.Offre.TypeContrat;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TypeContratServiceTest {
    static TypeContratService service;
    static int idTypeContratTest;

    @BeforeAll
    static void setup() {
        service = new TypeContratService();
    }

    @Test
    @Order(1)
    void testAjouterTypeContrat() {
        TypeContrat tc = new TypeContrat();
        tc.setNom("NomTypeTest");
        tc.setDescription("DescTypeTest");

        service.ajouter(tc);

        List<TypeContrat> types = service.getAll();
        assertFalse(types.isEmpty());

        TypeContrat inserted = types.stream()
                .filter(t -> "NomTypeTest".equals(t.getNom()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        idTypeContratTest = inserted.getId();
        assertTrue(idTypeContratTest > 0);
    }

    @Test
    @Order(2)
    void testModifierTypeContrat() {
        TypeContrat tc = new TypeContrat();
        tc.setId(idTypeContratTest);
        tc.setNom("NomTypeModifie");
        tc.setDescription("DescTypeModifie");

        service.modifier(tc);

        List<TypeContrat> types = service.getAll();
        boolean trouve = types.stream()
                .anyMatch(t -> t.getId() == idTypeContratTest && "NomTypeModifie".equals(t.getNom()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerTypeContrat() {
        service.supprimer(idTypeContratTest);

        List<TypeContrat> types = service.getAll();
        boolean existe = types.stream().anyMatch(t -> t.getId() == idTypeContratTest);
        assertFalse(existe);
        idTypeContratTest = 0;
    }

    @AfterAll
    static void cleanUp() {
        if (idTypeContratTest <= 0) {
            return;
        }

        try {
            List<TypeContrat> types = service.getAll();
            boolean existe = types.stream().anyMatch(t -> t.getId() == idTypeContratTest);
            if (existe) {
                service.supprimer(idTypeContratTest);
            }
        } catch (Exception ignored) {
        }
    }
}
