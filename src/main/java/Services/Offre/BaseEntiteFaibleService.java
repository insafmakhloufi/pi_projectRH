package Services.Offre;

import Iservices.Offre.IEntiteFaible;
import Entities.Offre.EntiteFaible;
import Utils.Mydatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseEntiteFaibleService<T extends EntiteFaible> implements IEntiteFaible<T> {
    protected Connection connection;
    protected String tableName;

    public BaseEntiteFaibleService(String tableName) {
        this.connection = Mydatabase.getInstance().getConnection();
        this.tableName = tableName;
    }

    // Méthode abstraite pour créer l'objet à partir du ResultSet
    protected abstract T createEntityFromResultSet(ResultSet rs) throws SQLException;

    // Méthode pour obtenir le nom de la colonne ID
    protected String getIdColumnName() {
        // Convertit "TypeContrat" en "id_type"
        return "id_" + tableName.toLowerCase().replace("emploi", "_emploi")
                .replace("activite", "");
    }

    // Méthode pour obtenir le nom de la colonne NOM
    protected String getNomColumnName() {
        // Convertit "TypeContrat" en "nom_type"
        return "nom_" + tableName.toLowerCase().replace("contrat", "")
                .replace("emploi", "_emploi")
                .replace("activite", "");
    }

    // NOUVELLE MÉTHODE : Recherche par colonne
    protected List<T> getByColumn(String columnName, String value) {
        List<T> entities = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName + " WHERE " + columnName + " LIKE ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + value + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                entities.add(createEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entities;
    }

    @Override
    public List<T> getAll() {
        List<T> entities = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                entities.add(createEntityFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return entities;
    }

    @Override
    public T getById(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + getIdColumnName() + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return createEntityFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void ajouter(T entite) {
        String sql = "INSERT INTO " + tableName + " (" + getNomColumnName() + ") VALUES (?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, entite.getNom());
            pstmt.executeUpdate();
            System.out.println(entite.getType() + " ajouté avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void modifier(T entite) {
        String sql = "UPDATE " + tableName + " SET " + getNomColumnName() + " = ? WHERE " + getIdColumnName() + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, entite.getNom());
            pstmt.setInt(2, entite.getId());
            pstmt.executeUpdate();
            System.out.println(entite.getType() + " modifié avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void supprimer(int id) {
        String sql = "DELETE FROM " + tableName + " WHERE " + getIdColumnName() + " = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            System.out.println("Entité avec id=" + id + " supprimée avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}