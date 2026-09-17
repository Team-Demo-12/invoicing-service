package com.beaconstone.invoicing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Invoicing is downstream and asynchronous: when Payments stops publishing, this service
 * stays healthy and simply issues nothing, which is why a Payments incident reaches the
 * business workflow without this service itself degrading.
 */
public final class Application {
    private static final String SERVICE = JsonHttp.serviceName("invoicing-service");
    private static final String RELEASE = JsonHttp.release("invoicing-1.12.0");
    private static final Map<String, Map<String, Object>> STORE = new ConcurrentHashMap<>();
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        HttpServer server = JsonHttp.server(JsonHttp.port());
        server.createContext("/health", ex -> JsonHttp.json(ex, 200,
            Map.of("status", "ok", "service", SERVICE, "release", RELEASE)));
        server.createContext("/ready", ex -> JsonHttp.json(ex, 200, Map.of("status", "ready")));
        server.createContext("/metrics", ex -> JsonHttp.json(ex, 200,
            Map.of("service", SERVICE, "release", RELEASE, "documents", STORE.size())));
        server.createContext("/events/payment-succeeded", Application::ingest);
        server.createContext("/documents", Application::documents);
        server.start();
        System.out.println("{\"level\":\"info\",\"message\":\"invoicing-service started\",\"service\":\""
            + SERVICE + "\",\"release\":\"" + RELEASE + "\",\"port\":" + JsonHttp.port() + "}");
    }

    private static void ingest(HttpExchange exchange) throws java.io.IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            JsonHttp.json(exchange, 405, Map.of("error", "MethodNotAllowed"));
            return;
        }
        Map<String, Object> event = JsonHttp.readJson(exchange);
        String invoiceId = String.valueOf(event.get("invoiceId"));
        for (Map<String, Object> existing : STORE.values()) {
            if (invoiceId.equals(String.valueOf(existing.get("invoiceId")))) {
                JsonHttp.json(exchange, 201, existing);
                return;
            }
        }
        int amountMinor = event.get("amountMinor") instanceof Number n ? n.intValue() : 0;
        int taxMinor = event.get("taxMinor") instanceof Number n ? n.intValue() : 0;
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("documentId", UUID.randomUUID().toString());
        document.put("issuedAt", Instant.now().toString());
        document.put("documentNumber", nextDocumentNumber());
        document.put("invoiceId", invoiceId);
        document.put("authorizationId", event.get("authorizationId"));
        document.put("network", event.get("network"));
        document.put("amountMinor", amountMinor);
        document.put("currency", event.getOrDefault("currency", "USD"));
        document.put("taxMinor", taxMinor);
        document.put("netMinor", amountMinor - taxMinor);
        document.put("profileId", event.get("profileId"));
        document.put("captureMode", event.get("captureMode"));
        document.put("status", "issued");
        STORE.put((String) document.get("documentId"), document);
        JsonHttp.json(exchange, 201, Map.of(
            "documentId", document.get("documentId"),
            "documentNumber", document.get("documentNumber"),
            "invoiceId", invoiceId,
            "status", "issued",
            "requestId", JsonHttp.requestId(exchange)
        ));
    }

    private static void documents(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/documents".equals(path)) {
            String invoiceId = query(exchange, "invoiceId");
            List<Map<String, Object>> found = new ArrayList<>();
            for (Map<String, Object> document : STORE.values()) {
                if (invoiceId == null || invoiceId.equals(String.valueOf(document.get("invoiceId")))) {
                    found.add(document);
                }
            }
            JsonHttp.json(exchange, 200, Map.of("documents", found));
            return;
        }
        if (path.startsWith("/documents/") && path.endsWith("/void") && "POST".equals(exchange.getRequestMethod())) {
            String id = path.substring("/documents/".length(), path.length() - "/void".length());
            Map<String, Object> document = STORE.get(id);
            if (document == null) {
                JsonHttp.json(exchange, 404, Map.of("error", "DocumentNotFound"));
                return;
            }
            Map<String, Object> body = JsonHttp.readJson(exchange);
            document.put("status", "void");
            document.put("voidedAt", Instant.now().toString());
            document.put("voidReason", body.getOrDefault("reason", "payment_reversed"));
            JsonHttp.json(exchange, 200, document);
            return;
        }
        if (path.startsWith("/documents/")) {
            Map<String, Object> document = STORE.get(path.substring("/documents/".length()));
            if (document == null) {
                JsonHttp.json(exchange, 404, Map.of("error", "DocumentNotFound"));
                return;
            }
            JsonHttp.json(exchange, 200, document);
            return;
        }
        JsonHttp.json(exchange, 404, Map.of("error", "NotFound"));
    }

    private static String nextDocumentNumber() {
        return "INV-" + Year.now() + "-" + String.format("%06d", SEQUENCE.incrementAndGet());
    }

    private static String query(HttpExchange exchange, String key) {
        String raw = exchange.getRequestURI().getQuery();
        if (raw == null) return null;
        for (String part : raw.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) return pair[1];
        }
        return null;
    }

    private Application() {}
}
