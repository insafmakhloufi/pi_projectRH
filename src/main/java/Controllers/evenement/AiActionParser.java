package Controllers.evenement;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AiActionParser {

    public enum ActionType {
        DELETE_EVENT,
        OPEN_EVENT,
        OPEN_ADD_FORM,
        MOST_PARTICIPANTS,
        COUNT_STATUS,
        ASK_CLARIFICATION,
        UNKNOWN
    }

    public static final class ParsedAction {
        public final ActionType type;
        public final Integer id;
        public final String rawActionLine;

        public ParsedAction(ActionType type, Integer id, String rawActionLine) {
            this.type = type;
            this.id = id;
            this.rawActionLine = rawActionLine;
        }
    }

    public ParsedAction parse(String assistantText) {
        if (assistantText == null || assistantText.isBlank()) {
            return new ParsedAction(ActionType.UNKNOWN, null, null);
        }

        String firstLine = assistantText.split("\\R", 2)[0].trim();
        if (!firstLine.toUpperCase().startsWith("ACTION:")) {
            return new ParsedAction(ActionType.UNKNOWN, null, firstLine);
        }

        String payload = firstLine.substring("ACTION:".length()).trim();
        String payloadLower = payload.toLowerCase();

        if (payloadLower.startsWith("delete_event")) {
            Integer id = extractId(payload);
            return new ParsedAction(ActionType.DELETE_EVENT, id, firstLine);
        }

        if (payloadLower.startsWith("open_event")) {
            Integer id = extractId(payload);
            return new ParsedAction(ActionType.OPEN_EVENT, id, firstLine);
        }

        if (payloadLower.startsWith("open_add_form")) {
            return new ParsedAction(ActionType.OPEN_ADD_FORM, null, firstLine);
        }

        if (payloadLower.startsWith("most_participants")) {
            return new ParsedAction(ActionType.MOST_PARTICIPANTS, null, firstLine);
        }

        if (payloadLower.startsWith("count_status")) {
            return new ParsedAction(ActionType.COUNT_STATUS, null, firstLine);
        }

        if (payloadLower.startsWith("ask_clarification")) {
            return new ParsedAction(ActionType.ASK_CLARIFICATION, null, firstLine);
        }

        return new ParsedAction(ActionType.UNKNOWN, null, firstLine);
    }

    private Integer extractId(String payload) {
        Pattern p = Pattern.compile("id\\s*=\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(payload);
        if (!m.find()) return null;
        try {
            return Integer.parseInt(m.group(1));
        } catch (Exception ignored) {
            return null;
        }
    }
}
