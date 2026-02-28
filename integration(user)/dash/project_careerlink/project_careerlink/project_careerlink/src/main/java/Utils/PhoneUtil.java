package Utils;

public class PhoneUtil {

    private PhoneUtil() {
    }

    public static String normalizeToE164(String countryDialCode, String nationalNumber) {
        if (countryDialCode == null) countryDialCode = "";
        if (nationalNumber == null) nationalNumber = "";

        String dial = countryDialCode.trim();
        String number = nationalNumber.trim();

        if (dial.isEmpty()) {
            throw new IllegalArgumentException("Country dial code is required");
        }

        if (!dial.startsWith("+")) {
            dial = "+" + dial;
        }

        dial = dial.replaceAll("[^+0-9]", "");
        if (!dial.startsWith("+")) {
            throw new IllegalArgumentException("Invalid country dial code");
        }

        if (number.startsWith("+")) {
            String full = number.replaceAll("[^+0-9]", "");
            if (!full.matches("\\+\\d{8,15}")) {
                throw new IllegalArgumentException("Invalid phone number");
            }
            return full;
        }

        number = number.replaceAll("\\D", "");
        while (number.startsWith("0") && number.length() > 1) {
            number = number.substring(1);
        }

        String digits = (dial.substring(1) + number);
        if (!digits.matches("\\d{8,15}")) {
            throw new IllegalArgumentException("Invalid phone number");
        }

        return "+" + digits;
    }

    public static String normalizeIdentifierPhoneTN(String identifier) {
        if (identifier == null) return null;
        String v = identifier.trim();
        if (v.isEmpty()) return v;

        if (v.startsWith("+")) {
            String full = v.replaceAll("[^+0-9]", "");
            return full;
        }

        String digits = v.replaceAll("\\D", "");
        if (!digits.matches("\\d{8,15}")) {
            return v;
        }

        try {
            return normalizeToE164("+216", digits);
        } catch (IllegalArgumentException e) {
            return v;
        }
    }
}
