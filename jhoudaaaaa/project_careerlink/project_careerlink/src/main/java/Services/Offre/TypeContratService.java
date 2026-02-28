package Services.Offre;

import Entities.Offre.TypeContrat;
import Iservices.Offre.ITypeContrat;

import java.sql.*;
import java.util.List;

public class TypeContratService extends BaseEntiteFaibleService<TypeContrat> implements ITypeContrat {

    public TypeContratService() {
        super("TypeContrat");
    }

    @Override
    protected TypeContrat createEntityFromResultSet(ResultSet rs) throws SQLException {
        return new TypeContrat(
                rs.getInt("id_type"),
                rs.getString("nom_type"),
                rs.getString("description")
        );
    }

    @Override
    protected String getIdColumnName() {
        return "id_type";
    }

    @Override
    protected String getNomColumnName() {
        return "nom_type";
    }

    // Surcharger la méthode ajouter
    @Override
    public void ajouter(TypeContrat typeContrat) {
        String sql = "INSERT INTO TypeContrat (nom_type, description) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, typeContrat.getNom());
            pstmt.setString(2, typeContrat.getDescription());
            pstmt.executeUpdate();
            System.out.println("TypeContrat ajouté avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Surcharger la méthode modifier
    @Override
    public void modifier(TypeContrat typeContrat) {
        String sql = "UPDATE TypeContrat SET nom_type = ?, description = ? WHERE id_type = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, typeContrat.getNom());
            pstmt.setString(2, typeContrat.getDescription());
            pstmt.setInt(3, typeContrat.getId());
            pstmt.executeUpdate();
            System.out.println("TypeContrat modifié avec succès");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TypeContrat> getByNom(String nom) {
        return getByColumn("nom_type", nom);
    }
}