package Services.Offre;

import Entities.Offre.SecteurActivite;
import Iservices.Offre.ISecteurActivite;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SecteurActiviteService extends BaseEntiteFaibleService<SecteurActivite> implements ISecteurActivite {

    public SecteurActiviteService() {
        super("SecteurActivite");
    }

    @Override
    protected SecteurActivite createEntityFromResultSet(ResultSet rs) throws SQLException {
        return new SecteurActivite(
                rs.getInt("id_secteur"),
                rs.getString("nom_secteur")
        );
    }

    @Override
    protected String getIdColumnName() {
        return "id_secteur";
    }

    @Override
    protected String getNomColumnName() {
        return "nom_secteur";
    }
}