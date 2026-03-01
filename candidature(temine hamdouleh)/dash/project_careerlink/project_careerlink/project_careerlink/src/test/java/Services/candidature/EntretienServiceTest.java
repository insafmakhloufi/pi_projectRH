package Services.candidature;

import Entities.candidature.Entretien;
import Utils.Mydatabase;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EntretienServiceTest {

    static EntretienService service;
    static int idCandidatureTest;
    static int idEntretienTest;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("db.url", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
        Mydatabase.reset();

        try (Connection con = Mydatabase.getInstance().getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate("DROP TABLE IF EXISTS entretien");
            st.executeUpdate("DROP TABLE IF EXISTS candidature");

            // Table candidature minimal (juste pour FK logique)
            st.executeUpdate(
                    "CREATE TABLE candidature (" +
                            "IDCandidat INT AUTO_INCREMENT PRIMARY KEY," +
                            "prenom VARCHAR(255)," +
                            "nom VARCHAR(255)," +
                            "email VARCHAR(255)" +
                            ")"
            );

            // Table entretien (doit matcher EntretienService: date + heure + platform)
            st.executeUpdate(
                    "CREATE TABLE entretien (" +
                            "id_entretien INT AUTO_INCREMENT PRIMARY KEY," +
                            "id_candidature INT NOT NULL," +
                            "date DATE," +
                            "heure INT," +
                            "mode VARCHAR(100)," +
                            "lieu VARCHAR(100)," +
                            "platform VARCHAR(100)," +
                            "statut VARCHAR(100)," +
                            "notes TEXT" +
                            ")"
            );

            // Insert candidature test
            st.executeUpdate(
                    "INSERT INTO candidature (prenom, nom, email) " +
                            "VALUES ('TestPrenom','TestNom','test@mail.com')"
            );

            // récupérer l'id inséré (H2 supporte IDENTITY()) :
            ResultSet rs = st.executeQuery("SELECT MAX(IDCandidat) AS id FROM candidature");
            if (rs.next()) idCandidatureTest = rs.getInt("id");
            rs.close();
        }

        service = new EntretienService();
    }

    @Test
    @Order(1)
    void testAjouterEntretien() {
        Entretien e = new Entretien();
        e.setId_candidature(idCandidatureTest);
        e.setDate_heure(LocalDateTime.of(2026, 2, 13, 14, 30));
        e.setMode("Présentiel");
        e.setLieu("Salle RH A");
        e.setStatut("PLANIFIE");
        e.setNotes("Premier entretien");

        idEntretienTest = service.ajouterEntretienAndReturnId(e);
        assertTrue(idEntretienTest > 0);

        List<Entretien> entretiens = service.afficherEntretiens();
        assertFalse(entretiens.isEmpty());
        assertTrue(entretiens.stream().anyMatch(x -> x.getId_entretien() == idEntretienTest));
    }

    @Test
    @Order(2)
    void testAfficherEntretiensEtParCandidature() {
        List<Entretien> all = service.afficherEntretiens();
        assertNotNull(all);

        List<Entretien> parCand = service.getEntretiensByCandidature(idCandidatureTest);
        assertNotNull(parCand);
        assertTrue(parCand.stream().anyMatch(x -> x.getId_entretien() == idEntretienTest));

        System.out.println("=== ENTRETIENS (candidature " + idCandidatureTest + ") ===");
        for (Entretien e : parCand) {
            System.out.println(e);
        }
    }

    @Test
    @Order(3)
    void testModifierEntretien() {
        Entretien e = new Entretien();
        e.setId_entretien(idEntretienTest);
        e.setId_candidature(idCandidatureTest);
        e.setDate_heure(LocalDateTime.of(2026, 2, 20, 9, 0));
        e.setMode("En ligne");
        e.setLieu("Google Meet");
        e.setStatut("CONFIRME");
        e.setNotes("Lien meet envoyé au candidat");

        service.modifierEntretien(e);

        Entretien updated = service.getEntretienById(idEntretienTest);
        assertNotNull(updated);
        assertEquals("CONFIRME", updated.getStatut());
        assertEquals("En ligne", updated.getMode());
        assertEquals(LocalDateTime.of(2026, 2, 20, 9, 0), updated.getDate_heure());
    }

    @Test
    @Order(4)
    void testSupprimerEntretien() {
        service.supprimerEntretien(idEntretienTest);

        List<Entretien> entretiens = service.afficherEntretiens();
        assertFalse(entretiens.stream().anyMatch(x -> x.getId_entretien() == idEntretienTest));

        Entretien e = service.getEntretienById(idEntretienTest);
        assertNull(e);
    }

    @AfterAll
    static void tearDown() {
        Mydatabase.reset();
        System.clearProperty("db.url");
        System.clearProperty("db.user");
        System.clearProperty("db.password");
    }
}
