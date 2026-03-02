package Services.Formation;

import Entites.Formation.Formation;
import Iservices.Formation.IformationServices;
import Utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationServices implements IformationServices{
    Connection con;

    public FormationServices() {
        con = Mydatabase.getInstance().getConnection();
    }

    @Override
    public void ajouterFormation(Formation f) {
        String req = "INSERT INTO  Formation (lieu, titre, description, domaine, date_debut, date_fin, is_payante, prix) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ste=con.prepareStatement(req);
            ste.setString(1, f.getLieu());
            ste.setString(2,f.getTitre());
            ste.setString(3,f.getDescription());
            ste.setString(4,f.getDomaine());
            ste.setDate(5, f.getDateDebut());
            ste.setDate(6, f.getDateFin());
            ste.setBoolean(7, f.isPayante());
            ste.setFloat(8, f.getPrix());
            ste.executeUpdate();
            System.out.println("Ajouter avec succes");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int ajouterFormationAndReturnId(Formation f) {
        String req = "INSERT INTO  Formation (lieu, titre, description, domaine, date_debut, date_fin, is_payante, prix) VALUES (?,?,?,?,?,?,?,?)";
        try {
            PreparedStatement ste = con.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
            ste.setString(1, f.getLieu());
            ste.setString(2, f.getTitre());
            ste.setString(3, f.getDescription());
            ste.setString(4, f.getDomaine());
            ste.setDate(5, f.getDateDebut());
            ste.setDate(6, f.getDateFin());
            ste.setBoolean(7, f.isPayante());
            ste.setFloat(8, f.getPrix());
            ste.executeUpdate();

            try (ResultSet rs = ste.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    f.setId(id);
                    return id;
                }
            }

            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void supprimerFormation(int id) {
        Connection connection = Mydatabase.getInstance().getConnection();
        String reqDeleteCours = "DELETE FROM Cour WHERE formation_id = ?";
        String reqDeleteFormation = "DELETE FROM Formation WHERE id = ?";

        try {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (PreparedStatement steCours = connection.prepareStatement(reqDeleteCours);
                 PreparedStatement steFormation = connection.prepareStatement(reqDeleteFormation)) {

                steCours.setInt(1, id);
                steCours.executeUpdate();

                steFormation.setInt(1, id);
                steFormation.executeUpdate();

                connection.commit();
                System.out.println("Supprimer avec succes");
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void modifierFormation(Formation f) {
        String req = "UPDATE Formation SET lieu = ?, titre = ?, description = ?, domaine = ?, date_debut = ?, date_fin = ?, is_payante = ?, prix = ? WHERE id = ?";

        if (f == null) {
            throw new IllegalArgumentException("Formation ne doit pas être null");
        }
        if (f.getId() <= 0) {
            throw new IllegalArgumentException("Id formation invalide");
        }

        Formation existing = getFormationById(f.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Formation introuvable (id=" + f.getId() + ")");
        }

        String lieu = (f.getLieu() == null || f.getLieu().isBlank()) ? existing.getLieu() : f.getLieu();
        String titre = (f.getTitre() == null || f.getTitre().isBlank()) ? existing.getTitre() : f.getTitre();
        String description = (f.getDescription() == null || f.getDescription().isBlank()) ? existing.getDescription() : f.getDescription();
        String domaine = (f.getDomaine() == null || f.getDomaine().isBlank()) ? existing.getDomaine() : f.getDomaine();
        Date dateDebut = (f.getDateDebut() == null) ? existing.getDateDebut() : f.getDateDebut();
        Date dateFin = (f.getDateFin() == null) ? existing.getDateFin() : f.getDateFin();
        boolean payante = f.isPayante();
        float prix = f.getPrix();

        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setString(1, lieu);
            ste.setString(2, titre);
            ste.setString(3, description);
            ste.setString(4, domaine);
            ste.setDate(5, dateDebut);
            ste.setDate(6, dateFin);
            ste.setBoolean(7, payante);
            ste.setFloat(8, prix);
            ste.setInt(9, f.getId());
            ste.executeUpdate();
            System.out.println("Modifier avec succes");
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Formation> afficherFormations() {
        List<Formation> formations = new ArrayList<>();
        String  req = "SELECT * FROM Formation";
        try{
            Statement ste = con.createStatement();
            ResultSet rs = ste.executeQuery(req);
            while(rs.next()){
                Formation f = new Formation(
                        rs.getInt("id"),
                        rs.getString("lieu"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("domaine"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getBoolean("is_payante"),
                        rs.getFloat("prix")
                );
                formations.add(f);
            }
        }catch(SQLException e){
            throw new RuntimeException(e);
        }
        return formations;
    }

    public Formation getFormationById(int id) {
        String req = "SELECT * FROM Formation WHERE id = ?";
        try {
            PreparedStatement ste = con.prepareStatement(req);
            ste.setInt(1, id);
            ResultSet rs = ste.executeQuery();
            if (rs.next()) {
                return new Formation(
                        rs.getInt("id"),
                        rs.getString("lieu"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("domaine"),
                        rs.getDate("date_debut"),
                        rs.getDate("date_fin"),
                        rs.getBoolean("is_payante"),
                        rs.getFloat("prix")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
