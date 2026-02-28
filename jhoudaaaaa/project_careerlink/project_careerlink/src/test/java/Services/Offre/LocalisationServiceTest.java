package Services.Offre;

import Entities.Offre.Localisation;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LocalisationServiceTest {
    static LocalisationService service;
    static int idLocalisationTest;

    @BeforeAll
    static void setup() {
        service = new LocalisationService();
    }

    @Test
    @Order(1)
    void testAjouterLocalisation() {
        Localisation l = new Localisation();
        l.setVille("VilleTest");
        l.setPays("PaysTest");

        service.ajouter(l);

        List<Localisation> localisations = service.getAll();
        assertFalse(localisations.isEmpty());

        Localisation inserted = localisations.stream()
                .filter(loc -> "VilleTest".equals(loc.getVille()) && "PaysTest".equals(loc.getPays()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        idLocalisationTest = inserted.getId();
        assertTrue(idLocalisationTest > 0);
    }

    @Test
    @Order(2)
    void testModifierLocalisation() {
        Localisation l = new Localisation();
        l.setId(idLocalisationTest);
        l.setVille("VilleModifie");
        l.setPays("PaysModifie");

        service.modifier(l);

        List<Localisation> localisations = service.getAll();
        boolean trouve = localisations.stream()
                .anyMatch(loc -> loc.getId() == idLocalisationTest && "VilleModifie".equals(loc.getVille()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerLocalisation() {
        service.supprimer(idLocalisationTest);

        List<Localisation> localisations = service.getAll();
        boolean existe = localisations.stream().anyMatch(loc -> loc.getId() == idLocalisationTest);
        assertFalse(existe);
        idLocalisationTest = 0;
    }

    @AfterAll
    static void cleanUp() {
        if (idLocalisationTest <= 0) {
            return;
        }

        try {
            List<Localisation> localisations = service.getAll();
            boolean existe = localisations.stream().anyMatch(loc -> loc.getId() == idLocalisationTest);
            if (existe) {
                service.supprimer(idLocalisationTest);
            }
        } catch (Exception ignored) {
        }
    }
}
