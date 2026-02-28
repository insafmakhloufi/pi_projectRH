package Services.Offre;

import Entities.Offre.TypeEmploi;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TypeEmploiServiceTest {
    static TypeEmploiService service;
    static int idTypeEmploiTest;

    @BeforeAll
    static void setup() {
        service = new TypeEmploiService();
    }

    @Test
    @Order(1)
    void testAjouterTypeEmploi() {
        TypeEmploi te = new TypeEmploi();
        te.setNom("CategorieTest");

        service.ajouter(te);

        List<TypeEmploi> types = service.getAll();
        assertFalse(types.isEmpty());

        TypeEmploi inserted = types.stream()
                .filter(t -> "CategorieTest".equals(t.getNom()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        idTypeEmploiTest = inserted.getId();
        assertTrue(idTypeEmploiTest > 0);
    }

    @Test
    @Order(2)
    void testModifierTypeEmploi() {
        TypeEmploi te = new TypeEmploi();
        te.setId(idTypeEmploiTest);
        te.setNom("CategorieModifie");

        service.modifier(te);

        List<TypeEmploi> types = service.getAll();
        boolean trouve = types.stream()
                .anyMatch(t -> t.getId() == idTypeEmploiTest && "CategorieModifie".equals(t.getNom()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerTypeEmploi() {
        service.supprimer(idTypeEmploiTest);

        List<TypeEmploi> types = service.getAll();
        boolean existe = types.stream().anyMatch(t -> t.getId() == idTypeEmploiTest);
        assertFalse(existe);
        idTypeEmploiTest = 0;
    }

    @AfterAll
    static void cleanUp() {
        if (idTypeEmploiTest <= 0) {
            return;
        }

        try {
            List<TypeEmploi> types = service.getAll();
            boolean existe = types.stream().anyMatch(t -> t.getId() == idTypeEmploiTest);
            if (existe) {
                service.supprimer(idTypeEmploiTest);
            }
        } catch (Exception ignored) {
        }
    }
}
