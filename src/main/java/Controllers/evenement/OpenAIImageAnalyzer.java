package Controllers.evenement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

public class OpenAIImageAnalyzer {

    // ========================================================================
    // PUT YOUR REAL OPENAI API KEY HERE (or set env variable OPENAI_API_KEY)
    // ========================================================================
    private static final String OPENAI_API_KEY = "";

    private static final URI API_ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAIImageAnalyzer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Sends the image to OpenAI for full content moderation.
     * Detects: violence, blood, gore, injuries, weapons, drugs, nudity,
     * sexual content, harassment, hate speech, self-harm, terrorism,
     * animal abuse, child exploitation, and other harmful content.
     */
    public AnalysisResult analyzeImageForHate(File imageFile) throws IOException, InterruptedException {
        Objects.requireNonNull(imageFile, "imageFile");

        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()
                || apiKey.contains("PUT_YOUR_KEY_HERE")
                || apiKey.contains("XXXXXXXX")) {
            throw new IllegalStateException(
                    "OpenAI API key is missing. "
                            + "Set it in OpenAIImageAnalyzer.java or as environment variable OPENAI_API_KEY.");
        }

        long fileSize = imageFile.length();
        if (fileSize > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image too large (max 5 MB).");
        }

        byte[] bytes = Files.readAllBytes(imageFile.toPath());
        String mime = detectMimeFromLastExtension(imageFile.getName());
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:" + mime + ";base64," + base64;

        System.out.println("[OpenAIImageAnalyzer] Detected MIME: " + mime);
        System.out.println("[OpenAIImageAnalyzer] File size: " + bytes.length + " bytes");

        String requestBody = buildRequestBody(dataUrl);
        System.out.println("[OpenAIImageAnalyzer] Request size: "
                + requestBody.getBytes(StandardCharsets.UTF_8).length + " bytes");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(API_ENDPOINT)
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        System.out.println("[OpenAIImageAnalyzer] HTTP status: " + response.statusCode());

        if (response.statusCode() != 200) {
            String body = safeTrim(response.body());
            System.out.println("[OpenAIImageAnalyzer] Error body: " + body);
            throw new IllegalArgumentException(
                    "OpenAI API error: HTTP " + response.statusCode() + " â€” " + body);
        }

        AnalysisResult result = parseChatCompletionResult(response.body());
        if (result == null) {
            throw new IOException("Could not parse OpenAI response into the expected JSON result.");
        }
        return result;
    }

    // ========================================================================
    // Validation helper
    // ========================================================================

    public static MimeValidation validateImageFile(File imageFile) {
        if (imageFile == null) {
            return new MimeValidation(false, null, "Invalid image type");
        }

        String ext = getLastExtensionLower(imageFile.getName());
        if (ext == null || ext.isBlank()) {
            return new MimeValidation(false, null, "Invalid image type");
        }

        switch (ext) {
            case "jpg":
            case "jpeg":
            case "png":
            case "webp":
                return new MimeValidation(true,
                        detectMimeFromLastExtension(imageFile.getName()), null);
            default:
                return new MimeValidation(false, null, "Unsupported image format");
        }
    }

    // ========================================================================
    // Request body â€” comprehensive content moderation prompt
    // ========================================================================

    private String buildRequestBody(String imageDataUrl) throws JsonProcessingException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", "gpt-4o-mini");
        root.put("max_tokens", 300);

        ArrayNode messages = root.putArray("messages");

        // ---- System message with detailed moderation instructions ----
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content",
                "You are a strict content moderation assistant. "
                        + "Your job is to analyze images and detect ANY inappropriate or harmful content. "
                        + "You must flag the image as BANNED (isHate = true) if it contains ANY of the following:\n\n"
                        + "1. VIOLENCE: blood, gore, wounds, injuries, fights, beatings, torture, dead bodies, war scenes\n"
                        + "2. WEAPONS: guns, knives, bombs, explosives, any weapon used threateningly\n"
                        + "3. HATE SPEECH: racist symbols, Nazi symbols, swastikas, KKK imagery, slurs in text\n"
                        + "4. HARASSMENT / BULLYING: mocking, humiliation, cyberbullying content, threatening messages\n"
                        + "5. SEXUAL CONTENT: nudity, pornography, sexual acts, suggestive poses, explicit content\n"
                        + "6. DRUGS / ALCOHOL: illegal drug use, drug paraphernalia, promoting substance abuse\n"
                        + "7. SELF-HARM: cutting, suicide imagery, pro-anorexia content, self-injury\n"
                        + "8. TERRORISM: terrorist propaganda, extremist symbols, recruitment material, ISIS/Al-Qaeda flags\n"
                        + "9. ANIMAL ABUSE: cruelty to animals, animal fighting, injured animals from abuse\n"
                        + "10. CHILD EXPLOITATION: any inappropriate content involving minors\n"
                        + "11. GRAPHIC / DISTURBING: accident scenes, medical gore, mutilation, corpses\n"
                        + "12. PROFANITY / OFFENSIVE TEXT: slurs, extreme insults, threats visible in the image\n"
                        + "13. DANGEROUS ACTIVITIES: encouraging dangerous stunts, reckless behavior\n\n"
                        + "Be VERY STRICT. When in doubt, flag it as banned to keep the platform safe.\n"
                        + "If the image is clearly safe (normal photo, art, nature, food, people smiling, landscapes, "
                        + "animals, cartoons, memes without hate, etc.), mark it as safe.\n\n"
                        + "Respond ONLY with valid JSON, nothing else:\n"
                        + "{\"isHate\":true/false,\"category\":\"category name or safe\",\"reason\":\"short explanation\"}");

        // ---- User message with image ----
        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");

        ArrayNode content = userMsg.putArray("content");

        content.addObject()
                .put("type", "text")
                .put("text", "Analyze this image strictly. Check for: violence, blood, injuries, weapons, "
                        + "harassment, bullying, hate speech, nudity, drugs, self-harm, terrorism, "
                        + "animal abuse, offensive text, or any harmful content. "
                        + "Return ONLY JSON: "
                        + "{\"isHate\":true/false,\"category\":\"category\",\"reason\":\"short reason\"}");

        ObjectNode imagePart = content.addObject();
        imagePart.put("type", "image_url");
        ObjectNode imageUrlObj = imagePart.putObject("image_url");
        imageUrlObj.put("url", imageDataUrl);
        imageUrlObj.put("detail", "low");

        ObjectNode responseFormat = root.putObject("response_format");
        responseFormat.put("type", "json_object");

        return objectMapper.writeValueAsString(root);
    }

    // ========================================================================
    // Response parser
    // ========================================================================

    private AnalysisResult parseChatCompletionResult(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return null;
        }

        JsonNode root = objectMapper.readTree(json);

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            System.out.println("[OpenAIImageAnalyzer] No choices in response");
            return null;
        }

        String text = choices.get(0)
                .path("message")
                .path("content")
                .asText(null);

        if (text == null || text.isBlank()) {
            System.out.println("[OpenAIImageAnalyzer] Empty content in response");
            return null;
        }

        System.out.println("[OpenAIImageAnalyzer] Model output: " + text);

        JsonNode modelJson = objectMapper.readTree(text);
        JsonNode isHateNode = modelJson.get("isHate");
        JsonNode reasonNode = modelJson.get("reason");
        JsonNode categoryNode = modelJson.get("category");

        if (isHateNode == null || !isHateNode.isBoolean()) {
            throw new IOException("Invalid model JSON: missing/invalid isHate: " + text);
        }

        String reason = "";
        if (reasonNode != null && reasonNode.isTextual()) {
            reason = reasonNode.asText("");
        }

        String category = "";
        if (categoryNode != null && categoryNode.isTextual()) {
            category = categoryNode.asText("");
        }

        // Combine category + reason for a clear ban message
        String fullReason;
        if (!category.isBlank() && !category.equalsIgnoreCase("safe")) {
            fullReason = "[" + category.toUpperCase() + "] " + reason;
        } else {
            fullReason = reason;
        }

        return new AnalysisResult(isHateNode.asBoolean(), fullReason);
    }

    // ========================================================================
    // Utility methods
    // ========================================================================

    private static String detectMimeFromLastExtension(String filename) {
        String ext = getLastExtensionLower(filename);
        if (ext == null) {
            return "image/jpeg";
        }
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    private static String getLastExtensionLower(String filename) {
        if (filename == null) return null;
        String name = filename.trim();
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0 || lastDot == name.length() - 1) {
            return null;
        }
        return name.substring(lastDot + 1).toLowerCase();
    }

    private static String resolveApiKey() {
        String env = System.getenv("OPENAI_API_KEY");
        if (env != null && !env.isBlank()) return env.trim();
        return OPENAI_API_KEY;
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }

    // ========================================================================
    // Inner classes
    // ========================================================================

    public static final class MimeValidation {
        private final boolean valid;
        private final String mime;
        private final String message;

        public MimeValidation(boolean valid, String mime, String message) {
            this.valid = valid;
            this.mime = mime;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String mime() {
            return mime == null || mime.isBlank() ? "image/jpeg" : mime;
        }

        public String message() {
            return message == null ? "" : message;
        }
    }

    public static final class AnalysisResult {
        private final boolean isHate;
        private final String reason;

        public AnalysisResult(boolean isHate, String reason) {
            this.isHate = isHate;
            this.reason = reason == null ? "" : reason;
        }

        public boolean isHate() {
            return isHate;
        }

        public String reason() {
            return reason;
        }
    }
}

