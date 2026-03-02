package Controllers.evenement;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AiRequestParser {

    public enum Kind {
        DATA_REQUEST,
        ACTION_REQUEST,
        CLARIFY,
        UNKNOWN
    }

    public static final class AiRequest {
        public final Kind kind;
        public final String descriptionLine;
        public final Map<String, String> fields;

        public AiRequest(Kind kind, String descriptionLine, Map<String, String> fields) {
            this.kind = kind;
            this.descriptionLine = descriptionLine;
            this.fields = fields;
        }

        public String get(String key) {
            if (key == null) return null;
            return fields.get(key.toUpperCase());
        }
    }

    public AiRequest parse(String planText) {
        if (planText == null || planText.isBlank()) {
            return new AiRequest(Kind.UNKNOWN, null, new LinkedHashMap<>());
        }

        String[] lines = planText.split("\\R");
        if (lines.length == 0) {
            return new AiRequest(Kind.UNKNOWN, null, new LinkedHashMap<>());
        }

        String first = lines[0].trim();
        Kind kind;
        if (first.toUpperCase().startsWith("DATA_REQUEST:")) {
            kind = Kind.DATA_REQUEST;
        } else if (first.toUpperCase().startsWith("ACTION_REQUEST:")) {
            kind = Kind.ACTION_REQUEST;
        } else if (first.toUpperCase().startsWith("CLARIFY:")) {
            kind = Kind.CLARIFY;
        } else {
            kind = Kind.UNKNOWN;
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            int idx = line.indexOf(':');
            if (idx <= 0) continue;

            String key = line.substring(0, idx).trim().toUpperCase();
            String value = line.substring(idx + 1).trim();
            if (!key.isEmpty() && !value.isEmpty()) {
                fields.put(key, value);
            }
        }

        return new AiRequest(kind, first, fields);
    }

    public static Map<String, String> parseKeyValueParams(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return out;

        // naive split: key=value pairs separated by spaces
        String[] parts = raw.trim().split("\\s+");
        for (String p : parts) {
            int idx = p.indexOf('=');
            if (idx <= 0) continue;
            String k = p.substring(0, idx).trim();
            String v = p.substring(idx + 1).trim();
            if (!k.isEmpty() && !v.isEmpty()) {
                out.put(k, v);
            }
        }
        return out;
    }

    public static String extractClarifyQuestion(String line) {
        if (line == null) return null;
        int idx = line.indexOf("question=");
        if (idx < 0) return null;
        String q = line.substring(idx + "question=".length()).trim();
        if (q.startsWith("\"") && q.endsWith("\"") && q.length() >= 2) {
            q = q.substring(1, q.length() - 1);
        }
        return q;
    }
}
