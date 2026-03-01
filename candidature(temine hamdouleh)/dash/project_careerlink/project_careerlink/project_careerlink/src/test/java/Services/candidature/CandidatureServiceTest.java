package Services.candidature;

import Entities.candidature.Candidature;
import Entities.candidature.Certification;
import Entities.candidature.Experience;
import Utils.Mydatabase;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CandidatureServiceTest {

    static CandidatureService service;
    static int idCandidatureTest;

    @BeforeAll
    static void setup() throws Exception {
        System.setProperty("db.url", "jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1");
        System.setProperty("db.user", "sa");
        System.setProperty("db.password", "");
        Mydatabase.reset();

        try (Connection con = Mydatabase.getInstance().getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("DROP TABLE IF EXISTS certification");
            st.executeUpdate("DROP TABLE IF EXISTS experience");
            st.executeUpdate("DROP TABLE IF EXISTS entretien");
            st.executeUpdate("DROP TABLE IF EXISTS candidature");

            st.executeUpdate(
                    "CREATE TABLE candidature (" +
                            "IDCandidat INT AUTO_INCREMENT PRIMARY KEY," +
                            "user_id INT," +                          // ✅ ajout
                            "id_offre INT," +
                            "prenom VARCHAR(255)," +
                            "nom VARCHAR(255)," +
                            "email VARCHAR(255)," +
                            "indicatif VARCHAR(50)," +
                            "tel BIGINT," +
                            "adresse VARCHAR(255)," +
                            "ville VARCHAR(255)," +
                            "date_naissance DATE," +
                            "highest_degree VARCHAR(255)," +
                            "institution VARCHAR(255)," +
                            "lettre_motivation TEXT," +
                            "skills_text TEXT," +
                            "Pieces_jointes VARCHAR(1024)," +
                            "video_path VARCHAR(1024)," +
                            "confirm_info BOOLEAN," +
                            "accept_privacy BOOLEAN," +
                            "decision_rh VARCHAR(50)" +               // ✅ ajout
                            ")"
            );


            st.executeUpdate(
                    "CREATE TABLE experience (" +
                            "id_experience INT AUTO_INCREMENT PRIMARY KEY," +
                            "id_candidature INT," +
                            "poste VARCHAR(255)," +
                            "entreprise VARCHAR(255)," +
                            "date_debut DATE," +
                            "date_fin DATE" +
                            ")"
            );

            st.executeUpdate(
                    "CREATE TABLE certification (" +
                            "id_certification INT AUTO_INCREMENT PRIMARY KEY," +
                            "id_candidature INT," +
                            "organisme VARCHAR(255)," +
                            "nom_certification VARCHAR(255)," +
                            "numero INT," +
                            "date_obtention DATE NOT NULL," +
                            "date_expiration DATE" +
                            ")"
            );

            st.executeUpdate(
                    "CREATE TABLE entretien (" +
                            "id_entretien INT AUTO_INCREMENT PRIMARY KEY," +
                            "id_candidature INT" +
                            ")"
            );
        }

        service = new CandidatureService();
    }

    @Test
    @Order(1)
    void testAjouterCandidature() {
        Candidature c = new Candidature();
        c.setPrenom("TestPrenom");
        c.setNom("TestNom");
        c.setEmail("test@mail.com");
        c.setIndicatif("+216");
        c.setTel("22123456");
        c.setAdresse("Adresse");
        c.setVille("Tunis");
        c.setHighest_degree("Bachelor");
        c.setInstitution("ESPRIT");
        c.setSkills_text("Java");
        c.setConfirm_info(true);
        c.setAccept_privacy(true);

        idCandidatureTest = service.ajouterCandidatureAndReturnId(c);
        assertTrue(idCandidatureTest > 0);

        List<Candidature> candidatures = service.afficherCandidatures();
        assertFalse(candidatures.isEmpty());
        assertTrue(candidatures.stream().anyMatch(x -> "TestNom".equals(x.getNom())));
    }

    @Test
    @Order(2)
    void testAfficherCandidatures() {
        // ajouter au moins 1 expérience + 1 certification pour pouvoir tester l'affichage complet
        ExperienceService expService = new ExperienceService();
        CertificationService certService = new CertificationService();

        Experience exp = new Experience();
        exp.setId_candidature(idCandidatureTest);
        exp.setPoste("Dev");
        exp.setEntreprise("EntrepriseTest");
        exp.setDate_debut(Date.valueOf("2022-01-01"));
        exp.setDate_fin(Date.valueOf("2023-01-01"));
        expService.ajouterExperience(exp);

        Certification cert = new Certification();
        cert.setId_candidature(idCandidatureTest);
        cert.setOrganisme("OrganismeTest");
        cert.setNom_certification("CertifTest");
        cert.setNumero(123);
        cert.setDate_obtention(Date.valueOf("2023-02-01"));
        cert.setDate_expiration(Date.valueOf("2024-02-01"));
        certService.ajouterCertification(cert);

        List<Candidature> candidatures = service.afficherCandidatures();
        assertNotNull(candidatures);

        Candidature foundCand = candidatures.stream().filter(x -> x.getIDCandidat() == idCandidatureTest).findFirst().orElse(null);
        assertNotNull(foundCand);

        List<Experience> expsTest = expService.afficherExperiencesParCandidature(idCandidatureTest);
        List<Certification> certsTest = certService.afficherCertificationsParCandidature(idCandidatureTest);
        assertFalse(expsTest.isEmpty());
        assertFalse(certsTest.isEmpty());

        System.out.println("=== AFFICHAGE COMPLET candidatures (avec expériences + certifications) ===");
        int limit = Math.min(10, candidatures.size());
        for (int idx = 0; idx < limit; idx++) {
            Candidature cand = candidatures.get(idx);
            int id = cand.getIDCandidat();

            System.out.println("\n--- CANDIDATURE ID=" + id + " ---");
            System.out.println("Prenom: " + cand.getPrenom());
            System.out.println("Nom: " + cand.getNom());
            System.out.println("Email: " + cand.getEmail());
            System.out.println("Indicatif: " + cand.getIndicatif());
            System.out.println("Tel: " + cand.getTel());
            System.out.println("Adresse: " + cand.getAdresse());
            System.out.println("Ville: " + cand.getVille());
            System.out.println("Date naissance: " + cand.getDate_naissance());
            System.out.println("Highest degree: " + cand.getHighest_degree());
            System.out.println("Institution: " + cand.getInstitution());
            System.out.println("Lettre motivation: " + cand.getLettre_motivation());
            System.out.println("Skills: " + cand.getSkills_text());
            System.out.println("Pieces_jointes: " + cand.getPieces_jointes());
            System.out.println("Video_path: " + cand.getVideo_path());
            System.out.println("Confirm_info: " + cand.isConfirm_info());
            System.out.println("Accept_privacy: " + cand.isAccept_privacy());

            List<Experience> exps = expService.afficherExperiencesParCandidature(id);
            List<Certification> certs = certService.afficherCertificationsParCandidature(id);

            System.out.println("EXPERIENCES (" + exps.size() + "):");
            for (Experience e : exps) {
                System.out.println("  " + e);
            }

            System.out.println("CERTIFICATIONS (" + certs.size() + "):");
            for (Certification c : certs) {
                System.out.println("  " + c);
            }
        }
    }

    @Test
    @Order(3)
    void testModifierCandidature() {
        Candidature c = new Candidature();
        c.setIDCandidat(idCandidatureTest);
        c.setPrenom("PrenomModifie");
        c.setNom("NomModifie");
        c.setEmail("mailmod@mail.com");
        c.setIndicatif("+216");
        c.setTel("55111222");
        c.setAdresse("Adresse2");
        c.setVille("Sfax");
        c.setHighest_degree("Masters");
        c.setInstitution("INSAT");
        c.setSkills_text("JavaFX");
        c.setConfirm_info(true);
        c.setAccept_privacy(true);

        service.modifierCandidature(c);

        List<Candidature> candidatures = service.afficherCandidatures();
        assertTrue(candidatures.stream().anyMatch(x -> x.getIDCandidat() == idCandidatureTest && "NomModifie".equals(x.getNom())));
    }

    @Test
    @Order(4)
    void testSupprimerCandidature() {
        service.supprimerCandidature(idCandidatureTest);
        List<Candidature> candidatures = service.afficherCandidatures();
        assertFalse(candidatures.stream().anyMatch(x -> x.getIDCandidat() == idCandidatureTest));
    }

    @AfterAll
    static void tearDown() {
        Mydatabase.reset();
        System.clearProperty("db.url");
        System.clearProperty("db.user");
        System.clearProperty("db.password");
    }
}
