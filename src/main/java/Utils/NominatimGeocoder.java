package Utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NominatimGeocoder {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static final Pattern DISPLAY_NAME_PATTERN = Pattern.compile("\\\"display_name\\\"\\s*:\\s*\\\"(.*?)\\\"", Pattern.DOTALL);

    private NominatimGeocoder() {
    }

    public static String reverse(double lat, double lng) {
        try {
            String url = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=" + lat + "&lon=" + lng;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "projet_formation/1.0 (JavaFX)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }

            String body = new String(response.body(), StandardCharsets.UTF_8);
            Matcher m = DISPLAY_NAME_PATTERN.matcher(body);
            if (!m.find()) {
                return null;
            }

            String displayName = unescapeJson(m.group(1));
            if (displayName.isBlank()) {
                return null;
            }

            return displayName;
        } catch (Exception e) {
            return null;
        }
    }

    private static String unescapeJson(String s) {
        return s
                .replace("\\\\\"", "\"")
                .replace("\\\\n", " ")
                .replace("\\\\r", " ")
                .replace("\\\\t", " ")
                .replace("\\\\/", "/")
                .replace("\\\\\\\\", "\\");
    }
}
