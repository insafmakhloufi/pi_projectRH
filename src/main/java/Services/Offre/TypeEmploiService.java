package Services.Offre;

import Entities.Offre.TypeEmploi;
import Iservices.Offre.ITypeEmploi;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TypeEmploiService extends BaseEntiteFaibleService<TypeEmploi> implements ITypeEmploi {

    public TypeEmploiService() {
        super("TypeEmploi");
    }

    @Override
    protected TypeEmploi createEntityFromResultSet(ResultSet rs) throws SQLException {
        return new TypeEmploi(
                rs.getInt("id_type_emploi"),
                rs.getString("categorie")
        );
    }

    @Override
    protected String getIdColumnName() {
        return "id_type_emploi";
    }

    @Override
    protected String getNomColumnName() {
        return "categorie";
    }
}