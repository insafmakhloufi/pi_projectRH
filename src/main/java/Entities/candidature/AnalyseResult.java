package Entities.candidature;

import java.util.List;

public class AnalyseResult {
    public int score_global;
    public String compatibilite;
    public String recommandation;
    public String classement;
    public String classement_detail;

    public String _raw_json;

    public String _nom;
    public String _prenom;
    public String _offre_titre;

    public CompetencesTechniques competences_techniques;
    public Experience experience;
    public DiplomeCertifications diplome_certifications;
    public SoftSkills soft_skills;

    public List<PointFort> points_forts;
    public List<PointAmeliorer> points_ameliorer;
    public String analyse_globale;

    public static class CompetencesTechniques {
        public int score;
        public int taux_couverture;
        public String commentaire;
        public List<Item> items;

        public static class Item {
            public String nom;
            public boolean present;
            public String niveau;
        }
    }

    public static class Experience {
        public int score;
        public int annees_candidat;
        public int annees_requis;
        public String type_experience;
        public String commentaire;
    }

    public static class DiplomeCertifications {
        public int score;
        public String diplome;
        public String niveau_requis;
        public boolean diplome_valide;
        public String commentaire;
        public List<Certification> certifications;

        public static class Certification {
            public String nom;
            public boolean presente;
        }
    }

    public static class SoftSkills {
        public int score;
        public List<String> items;
        public String commentaire;
    }

    public static class PointFort {
        public String icone;
        public String mot_cle;
        public String texte;
    }

    public static class PointAmeliorer {
        public String icone;
        public String texte;
    }
}
