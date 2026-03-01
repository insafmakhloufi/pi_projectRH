package Entities.candidature;

/**
 * InterviewResultat
 * Stocke le résultat d'une question : transcription + analyse IA
 */
public class InterviewResultat {

    private int    numeroQuestion;
    private String question;
    private String reponseTranscrite;
    private int    score;           // 0-100
    private String feedback;        // analyse courte
    private String pointsForts;     // ce qui était bien
    private String pointsFaibles;   // ce qui doit être amélioré
    private String conseils;        // conseils personnalisés

    // ── Constructeur ──────────────────────────────────────────────
    public InterviewResultat(int numeroQuestion, String question) {
        this.numeroQuestion   = numeroQuestion;
        this.question         = question;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int    getNumeroQuestion()    { return numeroQuestion; }
    public String getQuestion()          { return question; }

    public String getReponseTranscrite() { return reponseTranscrite; }
    public void   setReponseTranscrite(String r) { this.reponseTranscrite = r; }

    public int    getScore()             { return score; }
    public void   setScore(int s)        { this.score = s; }

    public String getFeedback()          { return feedback; }
    public void   setFeedback(String f)  { this.feedback = f; }

    public String getPointsForts()       { return pointsForts; }
    public void   setPointsForts(String p) { this.pointsForts = p; }

    public String getPointsFaibles()     { return pointsFaibles; }
    public void   setPointsFaibles(String p) { this.pointsFaibles = p; }

    public String getConseils()          { return conseils; }
    public void   setConseils(String c)  { this.conseils = c; }

    // Score en couleur (vert/orange/rouge)
    public String getCouleurScore() {
        if (score >= 70) return "#52b788";      // vert
        if (score >= 40) return "#ffd166";      // orange
        return "#ff4d6d";                        // rouge
    }
}
