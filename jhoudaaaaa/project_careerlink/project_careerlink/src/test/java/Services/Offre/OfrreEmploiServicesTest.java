package Services.Offre;

import Entities.Offre.OffreEmploi;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OfrreEmploiServicesTest {
    static OfrreEmploiServices service;

    static int idOffreTest;

    @BeforeAll
    static void setup() {
        service = new OfrreEmploiServices();
    }

    @Test
    @Order(1)
    void testAjouterOffreEmploi() {
        OffreEmploi o = new OffreEmploi("TitreTest", "DescriptionTest", "ACTIVE");
        service.ajouterOffreEmploi(o);

        List<OffreEmploi> offres = service.afficherOfrreEmploi();
        assertFalse(offres.isEmpty());

        OffreEmploi inserted = offres.stream()
                .filter(off -> "TitreTest".equals(off.getTitre()) && "DescriptionTest".equals(off.getDescription()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        idOffreTest = inserted.getId();
        assertTrue(idOffreTest > 0);
    }

    @Test
    @Order(2)
    void testModifierOffreEmploi() {
        OffreEmploi o = new OffreEmploi();
        o.setId(idOffreTest);
        o.setTitre("TitreModifie");
        o.setDescription("DescriptionModifie");
        o.setStatus("INACTIVE");

        service.modifierOffreEmploi(o);

        List<OffreEmploi> offres = service.afficherOfrreEmploi();
        boolean trouve = offres.stream().anyMatch(off -> off.getId() == idOffreTest && "TitreModifie".equals(off.getTitre()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerOffreEmploi() {
        service.supprimerOffreEmploi(idOffreTest);
        List<OffreEmploi> offres = service.afficherOfrreEmploi();
        boolean existe = offres.stream().anyMatch(off -> off.getId() == idOffreTest);
        assertFalse(existe);
        idOffreTest = 0;
    }

    @AfterAll
    static void cleanUp() {
        if (idOffreTest <= 0) {
            return;
        }

        try {
            List<OffreEmploi> offres = service.afficherOfrreEmploi();
            boolean existe = offres.stream().anyMatch(off -> off.getId() == idOffreTest);
            if (existe) {
                service.supprimerOffreEmploi(idOffreTest);
            }
        } catch (Exception ignored) {
        }
    }
}