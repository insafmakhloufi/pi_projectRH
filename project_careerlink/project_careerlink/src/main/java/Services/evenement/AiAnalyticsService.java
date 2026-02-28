package Services.evenement;

import Controllers.evenement.AiRequestParser;
import Utils.Mydatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AiAnalyticsService {

    private static final int MAX_LIMIT = 50;

    private final Connection con;

    public AiAnalyticsService() {
        this.con = Mydatabase.getInstance().getConnection();
    }

    /**
     * Executes a safe READ-ONLY analytics request.
     * This method never executes AI-provided SQL.
     */
    public String executeRead(AiRequestParser.AiRequest req) {
        if (req == null) {
            return "No request.";
        }
        if (req.kind != AiRequestParser.Kind.DATA_REQUEST) {
            return "Not a DATA_REQUEST.";
        }

        String tables = safeLower(req.get("TABLES"));
        String groupBy = safeLower(req.get("GROUP_BY"));
        String metrics = safeLower(req.get("METRICS"));
        int limit = parseLimit(req.get("LIMIT"));

        Map<String, String> filters = parseFilters(req.get("FILTERS"));

        // Basic routing
        boolean wantsEvenement = tables.contains("evenement");
        boolean wantsPublication = tables.contains("publication");

        try {
            if (wantsEvenement && wantsPublication) {
                return executeJoinRequest(groupBy, metrics, filters, limit);
            }
            if (wantsPublication) {
                return executePublicationRequest(groupBy, metrics, filters, limit);
            }
            // default evenement
            return executeEvenementRequest(groupBy, metrics, filters, limit);
        } catch (Exception e) {
            return "Analytics error: " + e.getMessage();
        }
    }

    private String executeEvenementRequest(String groupBy, String metrics, Map<String, String> filters, int limit) throws Exception {
        if (groupBy.contains("statut")) {
            return groupCount("evenement", "statut", filters);
        }
        if (groupBy.contains("type")) {
            return groupCount("evenement", "type", filters);
        }
        if (metrics.contains("count") || metrics.contains("total")) {
            long c = countRows("evenement", filters);
            return "evenement.count=" + c;
        }
        return listRowsEvenement(filters, limit);
    }

    private String executePublicationRequest(String groupBy, String metrics, Map<String, String> filters, int limit) throws Exception {
        if (groupBy.contains("statut")) {
            return groupCount("publication", "statut", filters);
        }
        if (groupBy.contains("type")) {
            return groupCount("publication", "type", filters);
        }
        if (groupBy.contains("evenement")) {
            return groupCount("publication", "evenement_id", filters);
        }
        if (metrics.contains("count") || metrics.contains("total")) {
            long c = countRows("publication", filters);
            return "publication.count=" + c;
        }
        return listRowsPublication(filters, limit);
    }

    private String executeJoinRequest(String groupBy, String metrics, Map<String, String> filters, int limit) throws Exception {
        // Most common: publications count by event
        if (metrics.contains("count_publications_by_event")
                || groupBy.contains("evenement")
                || groupBy.contains("event")) {

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT e.id_evenement, e.titre, COUNT(p.id_publication) AS pub_count ");
            sql.append("FROM evenement e LEFT JOIN publication p ON p.evenement_id = e.id_evenement ");

            List<Object> params = new ArrayList<>();
            appendWhereForJoin(sql, params, filters);

            sql.append(" GROUP BY e.id_evenement, e.titre ORDER BY pub_count DESC LIMIT ?");
            params.add(limit);

            try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
                bindParams(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    StringBuilder out = new StringBuilder();
                    out.append("top_events_by_publications\n");
                    int i = 0;
                    while (rs.next()) {
                        if (i++ >= limit) break;
                        out.append("- ")
                                .append(rs.getInt("id_evenement"))
                                .append(" | ")
                                .append(nullToEmpty(rs.getString("titre")))
                                .append(" | publications=")
                                .append(rs.getLong("pub_count"))
                                .append("\n");
                    }
                    return out.toString().trim();
                }
            }
        }

        // Fallback: counts only
        long eCount = countRows("evenement", filters);
        long pCount = countRows("publication", filters);
        return "evenement.count=" + eCount + "\npublication.count=" + pCount;
    }

    private long countRows(String table, Map<String, String> filters) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS c FROM ").append(table);
        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, table, filters);

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("c") : 0;
            }
        }
    }

    private String groupCount(String table, String column, Map<String, String> filters) throws Exception {
        if (!isAllowedGroupColumn(table, column)) {
            return "Unsupported group_by column: " + column;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ").append(column).append(" AS k, COUNT(*) AS c FROM ").append(table);

        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, table, filters);
        sql.append(" GROUP BY ").append(column).append(" ORDER BY c DESC LIMIT ?");
        params.add(MAX_LIMIT);

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder out = new StringBuilder();
                out.append(table).append(".group_count_by_").append(column).append("\n");
                while (rs.next()) {
                    out.append("- ").append(nullToEmpty(rs.getString("k")))
                            .append(": ").append(rs.getLong("c")).append("\n");
                }
                return out.toString().trim();
            }
        }
    }

    private String listRowsEvenement(Map<String, String> filters, int limit) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT id_evenement, titre, type, statut, date_debut, date_fin, lieu, capacite_max FROM evenement");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, "evenement", filters);
        sql.append(" ORDER BY id_evenement DESC LIMIT ?");
        params.add(limit);

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder out = new StringBuilder("evenement.rows\n");
                int i = 0;
                while (rs.next()) {
                    if (i++ >= limit) break;
                    out.append("- id=").append(rs.getInt("id_evenement"))
                            .append(" | titre=").append(nullToEmpty(rs.getString("titre")))
                            .append(" | type=").append(nullToEmpty(rs.getString("type")))
                            .append(" | statut=").append(nullToEmpty(rs.getString("statut")))
                            .append("\n");
                }
                return out.toString().trim();
            }
        }
    }

    private String listRowsPublication(Map<String, String> filters, int limit) throws Exception {
        StringBuilder sql = new StringBuilder("SELECT id_publication, titre, type, statut, visibilite, auteur_id, date_publication, evenement_id FROM publication");
        List<Object> params = new ArrayList<>();
        appendWhere(sql, params, "publication", filters);
        sql.append(" ORDER BY id_publication DESC LIMIT ?");
        params.add(limit);

        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder out = new StringBuilder("publication.rows\n");
                int i = 0;
                while (rs.next()) {
                    if (i++ >= limit) break;
                    out.append("- id=").append(rs.getInt("id_publication"))
                            .append(" | titre=").append(nullToEmpty(rs.getString("titre")))
                            .append(" | statut=").append(nullToEmpty(rs.getString("statut")))
                            .append(" | evenement_id=").append(rs.getObject("evenement_id"))
                            .append("\n");
                }
                return out.toString().trim();
            }
        }
    }

    private void appendWhere(StringBuilder sql, List<Object> params, String table, Map<String, String> filters) {
        List<String> clauses = new ArrayList<>();

        // Common filters
        addEqualsFilter(clauses, params, table, filters, "statut");
        addEqualsFilter(clauses, params, table, filters, "type");
        addEqualsFilter(clauses, params, table, filters, "lieu");

        // IDs
        if ("evenement".equals(table)) {
            addIntEqualsFilter(clauses, params, table, filters, "id_evenement");
        } else if ("publication".equals(table)) {
            addIntEqualsFilter(clauses, params, table, filters, "id_publication");
            addIntEqualsFilter(clauses, params, table, filters, "evenement_id");
            addIntEqualsFilter(clauses, params, table, filters, "auteur_id");
        }

        // Date ranges
        if ("evenement".equals(table)) {
            addDateRange(clauses, params, filters, "date_debut", "date_from", "date_to");
        } else if ("publication".equals(table)) {
            addDateRange(clauses, params, filters, "date_publication", "date_from", "date_to");
        }

        if (!clauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", clauses));
        }
    }

    private void appendWhereForJoin(StringBuilder sql, List<Object> params, Map<String, String> filters) {
        List<String> clauses = new ArrayList<>();

        // filters on evenement
        if (filters.containsKey("statut")) {
            clauses.add("e.statut = ?");
            params.add(filters.get("statut"));
        }
        if (filters.containsKey("type")) {
            clauses.add("e.type = ?");
            params.add(filters.get("type"));
        }
        if (filters.containsKey("lieu")) {
            clauses.add("e.lieu = ?");
            params.add(filters.get("lieu"));
        }

        // filter by event id
        if (filters.containsKey("id_evenement")) {
            Integer id = parseInt(filters.get("id_evenement"));
            if (id != null) {
                clauses.add("e.id_evenement = ?");
                params.add(id);
            }
        }

        if (!clauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", clauses));
        }
    }

    private void addEqualsFilter(List<String> clauses, List<Object> params, String table, Map<String, String> filters, String key) {
        if (!filters.containsKey(key)) return;
        if (!isAllowedFilterColumn(table, key)) return;
        clauses.add(key + " = ?");
        params.add(filters.get(key));
    }

    private void addIntEqualsFilter(List<String> clauses, List<Object> params, String table, Map<String, String> filters, String key) {
        if (!filters.containsKey(key)) return;
        if (!isAllowedFilterColumn(table, key)) return;
        Integer v = parseInt(filters.get(key));
        if (v == null) return;
        clauses.add(key + " = ?");
        params.add(v);
    }

    private void addDateRange(List<String> clauses, List<Object> params, Map<String, String> filters,
                              String column, String fromKey, String toKey) {
        LocalDateTime from = parseDate(filters.get(fromKey));
        LocalDateTime to = parseDate(filters.get(toKey));
        if (from != null) {
            clauses.add(column + " >= ?");
            params.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            clauses.add(column + " <= ?");
            params.add(Timestamp.valueOf(to));
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws Exception {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            int idx = i + 1;
            if (p instanceof Integer) {
                ps.setInt(idx, (Integer) p);
            } else if (p instanceof Long) {
                ps.setLong(idx, (Long) p);
            } else if (p instanceof Timestamp) {
                ps.setTimestamp(idx, (Timestamp) p);
            } else {
                ps.setString(idx, String.valueOf(p));
            }
        }
    }

    private static Map<String, String> parseFilters(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;

        // supports: key=value key=value OR key=value, key=value
        String norm = raw.replace(',', ' ');
        String[] parts = norm.trim().split("\\s+");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = p.substring(0, idx).trim().toLowerCase(Locale.ROOT);
            String v = p.substring(idx + 1).trim();
            if (!k.isEmpty() && !v.isEmpty()) {
                out.put(k, v);
            }
        }
        return out;
    }

    private static int parseLimit(String raw) {
        Integer v = parseInt(raw);
        if (v == null) return 20;
        return Math.max(1, Math.min(v, MAX_LIMIT));
    }

    private static Integer parseInt(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.isEmpty()) return null;
        try {
            return Integer.parseInt(t);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static LocalDateTime parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Expecting ISO-8601 or yyyy-MM-ddTHH:mm
        try {
            return LocalDateTime.parse(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isAllowedFilterColumn(String table, String col) {
        if (table == null || col == null) return false;
        switch (table) {
            case "evenement":
                return col.equals("statut") || col.equals("type") || col.equals("lieu") || col.equals("id_evenement");
            case "publication":
                return col.equals("statut") || col.equals("type") || col.equals("auteur_id")
                        || col.equals("evenement_id") || col.equals("id_publication");
            default:
                return false;
        }
    }

    private static boolean isAllowedGroupColumn(String table, String col) {
        if (table == null || col == null) return false;
        switch (table) {
            case "evenement":
                return col.equals("statut") || col.equals("type");
            case "publication":
                return col.equals("statut") || col.equals("type") || col.equals("evenement_id");
            default:
                return false;
        }
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
