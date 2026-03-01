package Entities.candidature;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

public class Entretien {

    private int id_entretien;
    private int id_candidature;

    private LocalDate date;
    private Integer heure;

    private String mode;
    private String lieu;
    private String platform;
    private String statut;
    private String notes;

    public Entretien() {}

    // constructeur complet
    public Entretien(int id_entretien, int id_candidature, LocalDate date, Integer heure,
                     String mode, String lieu, String platform, String statut, String notes) {
        this.id_entretien = id_entretien;
        this.id_candidature = id_candidature;
        this.date = date;
        this.heure = heure;
        this.mode = mode;
        this.lieu = lieu;
        this.platform = platform;
        this.statut = statut;
        this.notes = notes;
    }

    // constructeur sans id (pour INSERT)
    public Entretien(int id_candidature, LocalDate date, Integer heure,
                     String mode, String lieu, String platform, String statut, String notes) {
        this.id_candidature = id_candidature;
        this.date = date;
        this.heure = heure;
        this.mode = mode;
        this.lieu = lieu;
        this.platform = platform;
        this.statut = statut;
        this.notes = notes;
    }

    // -------- getters/setters --------

    public int getId_entretien() {
        return id_entretien;
    }

    public void setId_entretien(int id_entretien) {
        this.id_entretien = id_entretien;
    }

    public int getId_candidature() {
        return id_candidature;
    }

    public void setId_candidature(int id_candidature) {
        this.id_candidature = id_candidature;
    }

    public LocalDateTime getDate_heure() {
        if (date == null || heure == null) return null;
        int hhmm = heure;
        int h = Math.max(0, Math.min(23, hhmm / 100));
        int m = Math.max(0, Math.min(59, hhmm % 100));
        return date.atTime(LocalTime.of(h, m));
    }

    public void setDate_heure(LocalDateTime date_heure) {
        if (date_heure == null) {
            this.date = null;
            this.heure = null;
            return;
        }
        this.date = date_heure.toLocalDate();
        this.heure = (date_heure.getHour() * 100) + date_heure.getMinute();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getHeure() {
        return heure;
    }

    public void setHeure(Integer heure) {
        this.heure = heure;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getLieu() {
        return lieu;
    }

    public void setLieu(String lieu) {
        this.lieu = lieu;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // -------- equals --------

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Entretien entretien = (Entretien) o;
        return id_entretien == entretien.id_entretien &&
                id_candidature == entretien.id_candidature &&
                Objects.equals(date, entretien.date) &&
                Objects.equals(heure, entretien.heure) &&
                Objects.equals(mode, entretien.mode) &&
                Objects.equals(lieu, entretien.lieu) &&
                Objects.equals(platform, entretien.platform) &&
                Objects.equals(statut, entretien.statut) &&
                Objects.equals(notes, entretien.notes);
    }

    // -------- toString --------

    @Override
    public String toString() {
        return "Entretien{" +
                "id_entretien=" + id_entretien +
                ", id_candidature=" + id_candidature +
                ", date=" + date +
                ", heure=" + heure +
                ", mode='" + mode + '\'' +
                ", lieu='" + lieu + '\'' +
                ", platform='" + platform + '\'' +
                ", statut='" + statut + '\'' +
                ", notes='" + notes + '\'' +
                '}';
    }
}
