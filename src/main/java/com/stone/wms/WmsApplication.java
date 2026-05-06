package com.stone.wms;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import com.stone.wms.dto.ReceiveRequest;
import com.stone.wms.dto.ShipRequest;
import com.stone.wms.exception.InsufficientInventoryException;
import com.stone.wms.repository.InMemoryInventoryRepository;
import com.stone.wms.repository.InMemoryStockMovementRepository;
import com.stone.wms.service.WarehouseService;

public class WmsApplication {
    private final WarehouseService warehouseService;

    public WmsApplication(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    public static void main(String[] args) throws IOException {
        WarehouseService warehouseService = new WarehouseService(
                new InMemoryInventoryRepository(),
                new InMemoryStockMovementRepository()
        );
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        WmsApplication application = new WmsApplication(warehouseService);
        server.createContext("/api/v1/warehouse/receive", application::receive);
        server.createContext("/api/v1/warehouse/ship", application::ship);
        server.createContext("/api/v1/inventory", application::inventory);
        server.start();
        System.out.println("Stone WMS started on http://localhost:8080");
    }

    private void receive(HttpExchange exchange) throws IOException {
        handle(exchange, "POST", () -> {
            Map<String, String> body = parseJson(exchange);
            return warehouseService.receive(new ReceiveRequest(
                    body.get("sku"),
                    body.get("location"),
                    Integer.parseInt(body.getOrDefault("quantity", "0")),
                    body.get("referenceNo")
            )).toJson();
        });
    }

    private void ship(HttpExchange exchange) throws IOException {
        handle(exchange, "POST", () -> {
            Map<String, String> body = parseJson(exchange);
            return warehouseService.ship(new ShipRequest(
                    body.get("sku"),
                    Integer.parseInt(body.getOrDefault("quantity", "0")),
                    body.get("referenceNo")
            )).stream().map(allocation -> allocation.toJson()).toList().toString();
        });
    }

    private void inventory(HttpExchange exchange) throws IOException {
        handle(exchange, "GET", () -> {
            String path = exchange.getRequestURI().getPath();
            String sku = path.substring(path.lastIndexOf('/') + 1);
            return warehouseService.getInventory(sku).toJson();
        });
    }

    private void handle(HttpExchange exchange, String expectedMethod, Handler handler) throws IOException {
        try {
            if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
                write(exchange, 405, "{\"message\":\"method not allowed\"}");
                return;
            }
            write(exchange, 200, handler.execute());
        } catch (InsufficientInventoryException ex) {
            write(exchange, 409, error(ex.getMessage()));
        } catch (RuntimeException ex) {
            write(exchange, 400, error(ex.getMessage()));
        }
    }

    private static Map<String, String> parseJson(HttpExchange exchange) throws IOException {
        String json = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim();
        Map<String, String> values = new LinkedHashMap<>();
        if (json.length() < 2) {
            return values;
        }
        String content = json.substring(1, json.length() - 1);
        for (String pair : content.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                values.put(clean(parts[0]), clean(parts[1]));
            }
        }
        return values;
    }

    private static String clean(String value) {
        return value.trim().replaceAll("^\\\"|\\\"$", "");
    }

    private static String error(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @FunctionalInterface
    private interface Handler {
        String execute() throws IOException;
    }
}
