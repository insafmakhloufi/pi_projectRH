package Services.Offre;

import Entities.Offre.SecteurActivite;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecteurActiviteServiceTest {
    static SecteurActiviteService service;
    static int idSecteurTest;

    @BeforeAll
    static void setup() {
        service = new SecteurActiviteService();
    }

    @Test
    @Order(1)
    void testAjouterSecteur() {
        SecteurActivite s = new SecteurActivite();
        s.setNom("SecteurTest");

        service.ajouter(s);

        List<SecteurActivite> secteurs = service.getAll();
        assertFalse(secteurs.isEmpty());

        SecteurActivite inserted = secteurs.stream()
                .filter(sec -> "SecteurTest".equals(sec.getNom()))
                .findFirst()
                .orElse(null);

        assertNotNull(inserted);
        idSecteurTest = inserted.getId();
        assertTrue(idSecteurTest > 0);
    }

    @Test
    @Order(2)
    void testModifierSecteur() {
        SecteurActivite s = new SecteurActivite();
        s.setId(idSecteurTest);
        s.setNom("SecteurModifie");

        service.modifier(s);

        List<SecteurActivite> secteurs = service.getAll();
        boolean trouve = secteurs.stream()
                .anyMatch(sec -> sec.getId() == idSecteurTest && "SecteurModifie".equals(sec.getNom()));
        assertTrue(trouve);
    }

    @Test
    @Order(3)
    void testSupprimerSecteur() {
        service.supprimer(idSecteurTest);

        List<SecteurActivite> secteurs = service.getAll();
        boolean existe = secteurs.stream().anyMatch(sec -> sec.getId() == idSecteurTest);
        assertFalse(existe);
        idSecteurTest = 0;
    }

    @AfterAll
    static void cleanUp() {
        if (idSecteurTest <= 0) {
            return;
        }

        try {
            List<SecteurActivite> secteurs = service.getAll();
            boolean existe = secteurs.stream().anyMatch(sec -> sec.getId() == idSecteurTest);
            if (existe) {
                service.supprimer(idSecteurTest);
            }
        } catch (Exception ignored) {
        }
    }
}
