package Utils;

import Services.Formation.FormationPurchaseService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public final class StripeWebhookServer {

    private static volatile HttpServer server;

    private StripeWebhookServer() {
    }

    public static synchronized void startIfNeededAsync() {
        if (server != null) {
            return;
        }

        Thread t = new Thread(() -> {
            try {
                startInternal();
            } catch (Exception e) {
                System.out.println("[ERROR] Stripe webhook server failed to start: " + e.getMessage());
                e.printStackTrace();
            }
        }, "stripe-webhook-server");
        t.setDaemon(true);
        t.start();
    }

    private static void startInternal() throws IOException {
        String portRaw = getConfig("STRIPE_WEBHOOK_PORT");
        int port = parseIntOrDefault(portRaw, 4242);

        String stripeSecretKey = getConfig("STRIPE_SECRET_KEY");
        if (stripeSecretKey != null && !stripeSecretKey.isBlank()) {
            Stripe.apiKey = stripeSecretKey.trim();
        }

        String secret = getConfig("STRIPE_WEBHOOK_SECRET");
        String secretProp = System.getProperty("STRIPE_WEBHOOK_SECRET");
        String secretEnv = System.getenv("STRIPE_WEBHOOK_SECRET");
        String secretSource = (secretProp != null && !secretProp.isBlank()) ? "sysprop" : ((secretEnv != null && !secretEnv.isBlank()) ? "env" : "none");
        boolean secretHasWhitespace = secret != null && (secret.contains(" ") || secret.contains("\n") || secret.contains("\r") || secret.contains("\t"));
        int secretLen = secret == null ? 0 : secret.length();
        System.out.println(
                "[INFO] Stripe webhook config: secret_present=" + (secret != null && !secret.isBlank()) +
                        ", port_present=" + (portRaw != null && !portRaw.isBlank()) +
                        ", secret_source=" + secretSource +
                        ", secret_len=" + secretLen +
                        ", secret_has_whitespace=" + secretHasWhitespace +
                        ", stripe_api_key_present=" + (Stripe.apiKey != null && !Stripe.apiKey.isBlank())
        );

        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/stripe/webhook", new StripeWebhookHandler());
        server.createContext("/pay-redirect", new PayRedirectHandler());
        server.createContext("/pay-actual", new PayActualHandler());
        server.setExecutor(null);
        server.start();

        System.out.println("[OK] Stripe webhook server listening on http://localhost:" + port + "/stripe/webhook");
    }

    private static int parseIntOrDefault(String v, int def) {
        if (v == null || v.isBlank()) {
            return def;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String getConfig(String name) {
        String v = System.getProperty(name);
        if (v != null && !v.isBlank()) {
            return v;
        }
        v = System.getenv(name);
        if (v != null && !v.isBlank()) {
            return v;
        }
        return null;
    }

    private static final class PayActualHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                java.util.Map<String, String> params = parseQuery(query);
                String userIdStr = params.get("user_id");
                String formationIdStr = params.get("formation_id");
                if (userIdStr == null || formationIdStr == null) {
                    sendHtml(exchange, 400, "Paramètres manquants");
                    return;
                }
                int userId = Integer.parseInt(userIdStr);
                int formationId = Integer.parseInt(formationIdStr);

                FormationPurchaseService purchaseService = new FormationPurchaseService();
                if (purchaseService.hasPurchased(userId, formationId)) {
                    sendHtml(exchange, 200, "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;'><h1 style='color:#111827;'>Déjà payé</h1></body></html>");
                    return;
                }

                // Récupérer infos formation pour Stripe
                String titre = "Formation #" + formationId;
                double prix = 0;
                try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/careerlink", "root", "")) {
                    String sql = "SELECT titre, prix FROM formation WHERE id = ?";
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setInt(1, formationId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                titre = rs.getString("titre");
                                prix = rs.getDouble("prix");
                            }
                        }
                    }
                }

                String checkoutUrl = purchaseService.createCheckoutSessionUrl(titre, prix, formationId, userId);
                exchange.getResponseHeaders().set("Location", checkoutUrl);
                exchange.sendResponseHeaders(302, -1);
            } catch (Exception e) {
                sendHtml(exchange, 500, "Erreur redirection: " + e.getMessage());
            }
        }

        private java.util.Map<String, String> parseQuery(String query) {
            java.util.Map<String, String> result = new java.util.HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) { result.put(entry[0], entry[1]); }
            }
            return result;
        }

        private void sendHtml(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }

    private static final class PayRedirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                java.util.Map<String, String> params = parseQuery(query);
                
                String userIdStr = params.get("user_id");
                String formationIdStr = params.get("formation_id");
                
                if (userIdStr == null || formationIdStr == null) {
                    sendHtml(exchange, 400, "Paramètres manquants");
                    return;
                }
                
                int userId = Integer.parseInt(userIdStr);
                int formationId = Integer.parseInt(formationIdStr);
                
                FormationPurchaseService purchaseService = new FormationPurchaseService();
                if (purchaseService.hasPurchased(userId, formationId)) {
                    sendHtml(exchange, 200, "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;'>" +
                            "<h1 style='color:#111827;'>Formation déjà payée</h1>" +
                            "<p style='color:#374151;'>Vous avez déjà effectué le paiement de cette formation depuis l'application CareerLink.</p>" +
                            "<p style='color:#6b7280;font-size:14px;'>Vous pouvez y accéder directement dans votre espace candidat.</p>" +
                            "</body></html>");
                    return;
                }
                
                // Si pas encore payé, on crée une session Stripe et on redirige
                // Note: On récupère les infos de la formation depuis la DB idéalement, 
                // mais ici on simplifie en redirigeant vers une session Stripe.
                // Pour faire propre, il faudrait que createCheckoutSessionUrl soit appelé ici.
                // Mais l'email contient déjà l'URL Stripe originale.
                // Pour que ça marche, l'email doit maintenant pointer vers /pay-redirect?user_id=...&formation_id=...
                
                // On va chercher les détails minimalistes pour créer la session
                // Dans un vrai cas, on ferait un SELECT sur la table formation.
                // Ici on va rediriger vers l'app ou afficher un message d'erreur si on n'a pas tout.
                sendHtml(exchange, 200, "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;'>" +
                        "<h1 style='color:#111827;'>Redirection vers le paiement...</h1>" +
                        "<script>window.location.href='/pay-actual?user_id=" + userId + "&formation_id=" + formationId + "';</script>" +
                        "</body></html>");
                
            } catch (Exception e) {
                sendHtml(exchange, 500, "Erreur: " + e.getMessage());
            }
        }

        private java.util.Map<String, String> parseQuery(String query) {
            java.util.Map<String, String> result = new java.util.HashMap<>();
            if (query == null) return result;
            for (String param : query.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                }
            }
            return result;
        }
        
        private void sendHtml(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static final class StripeWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String userAgent = exchange.getRequestHeaders().getFirst("User-Agent");
                System.out.println(
                        "[INFO] Webhook request: method=" + exchange.getRequestMethod() +
                                ", path=" + exchange.getRequestURI() +
                                ", remote=" + exchange.getRemoteAddress() +
                                ", user_agent=" + (userAgent == null ? "" : userAgent)
                );
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    send(exchange, 405, "Method Not Allowed");
                    return;
                }

                String secret = getConfig("STRIPE_WEBHOOK_SECRET");
                if (secret == null || secret.isBlank()) {
                    System.out.println("[ERROR] STRIPE_WEBHOOK_SECRET is missing (check Run Configuration env vars)");
                    send(exchange, 500, "STRIPE_WEBHOOK_SECRET is missing");
                    return;
                }

                Headers headers = exchange.getRequestHeaders();
                String sigHeader = headers.getFirst("Stripe-Signature");
                if (sigHeader == null || sigHeader.isBlank()) {
                    System.out.println("[ERROR] Missing Stripe-Signature header");
                    send(exchange, 400, "Missing Stripe-Signature header");
                    return;
                }

                String payload = readAll(exchange.getRequestBody());

                Event event;
                try {
                    event = Webhook.constructEvent(payload, sigHeader, secret);
                } catch (SignatureVerificationException sve) {
                    System.out.println(
                            "[ERROR] Invalid Stripe signature: " + sve.getMessage() +
                                    " (sigHeader_present=" + (sigHeader != null && !sigHeader.isBlank()) +
                                    ", payload_len=" + (payload == null ? 0 : payload.length()) + ")"
                    );
                    send(exchange, 400, "Invalid signature");
                    return;
                } catch (Exception e) {
                    System.out.println("[ERROR] Webhook constructEvent failed: " + e.getClass().getName() + ": " + e.getMessage());
                    send(exchange, 400, "Invalid payload");
                    return;
                }

                if (event == null || event.getType() == null) {
                    send(exchange, 400, "Invalid event");
                    return;
                }

                System.out.println("[OK] Stripe webhook received event type=" + event.getType());

                if ("checkout.session.completed".equals(event.getType())) {
                    handleCheckoutSessionCompleted(event);
                }

                send(exchange, 200, "ok");
            } catch (Exception e) {
                System.out.println("[ERROR] Webhook handling failed: " + e.getMessage());
                e.printStackTrace();
                try {
                    String msg = e.getClass().getName() + ": " + (e.getMessage() == null ? "" : e.getMessage());
                    send(exchange, 500, msg);
                } catch (Exception ignored) {
                }
            }
        }

        private void handleCheckoutSessionCompleted(Event event) {
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            Object obj = deserializer == null ? null : deserializer.getObject().orElse(null);
            Session session = (obj instanceof Session) ? (Session) obj : null;
            String sessionId = session == null ? null : session.getId();

            if ((sessionId == null || sessionId.isBlank()) && deserializer != null) {
                try {
                    String rawJson = deserializer.getRawJson();
                    if (rawJson != null && !rawJson.isBlank()) {
                        JsonElement parsed = JsonParser.parseString(rawJson);
                        if (parsed != null && parsed.isJsonObject()) {
                            JsonObject raw = parsed.getAsJsonObject();
                            JsonElement idEl = raw.get("id");
                            if (idEl != null && !idEl.isJsonNull()) {
                                sessionId = idEl.getAsString();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (sessionId == null || sessionId.isBlank()) {
                return;
            }

            String userIdStr = session == null || session.getMetadata() == null ? null : session.getMetadata().get("user_id");
            String formationIdStr = session == null || session.getMetadata() == null ? null : session.getMetadata().get("formation_id");

            if ((userIdStr == null || formationIdStr == null)) {
                try {
                    Session full = Session.retrieve(sessionId);
                    if (full != null && full.getMetadata() != null) {
                        userIdStr = full.getMetadata().get("user_id");
                        formationIdStr = full.getMetadata().get("formation_id");
                    }
                    System.out.println(
                            "[INFO] checkout.session.completed: fetched full session for metadata (session_id=" + sessionId + ")"
                    );
                } catch (StripeException e) {
                    System.out.println(
                            "[ERROR] Failed to retrieve full Stripe session for webhook (session_id=" + sessionId + "): " + e.getMessage()
                    );
                } catch (Exception e) {
                    System.out.println(
                            "[ERROR] Unexpected error retrieving full Stripe session (session_id=" + sessionId + "): " + e.getMessage()
                    );
                }
            }

            System.out.println("[OK] checkout.session.completed metadata user_id=" + userIdStr + ", formation_id=" + formationIdStr);

            if (userIdStr == null || formationIdStr == null) {
                return;
            }

            try {
                int userId = Integer.parseInt(userIdStr);
                int formationId = Integer.parseInt(formationIdStr);

                FormationPurchaseService purchaseService = new FormationPurchaseService();
                boolean ok = purchaseService.verifyCheckoutSessionPaidAndRecordPurchase(sessionId, userId, formationId);
                if (ok) {
                    System.out.println("[OK] Purchase recorded from webhook (user_id=" + userId + ", formation_id=" + formationId + ")");
                } else {
                    System.out.println("[WARN] Webhook session not marked paid/verified (session_id=" + sessionId + ")");
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Failed to record purchase from webhook: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private String readAll(InputStream in) throws IOException {
            byte[] buf = in.readAllBytes();
            return new String(buf, StandardCharsets.UTF_8);
        }

        private void send(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
