package com.vedicmeet.appserver.admin;

import org.bson.Document;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Shared renderer for the legacy admin contract: logical failures still use HTTP 200. */
final class AdminResponses {

    private AdminResponses() {}

    static ResponseEntity<Map<String, Object>> execute(String successMessage, Supplier<?> action) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 200);
            body.put("success", true);
            body.put("message", successMessage);
            body.put("result", action.get());
            return ResponseEntity.ok(body);
        } catch (Exception error) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", 500);
            body.put("success", false);
            body.put("message", message(error));
            body.put("result", new Document());
            return ResponseEntity.ok(body);
        }
    }

    static ResponseEntity<Map<String, Object>> simple(Supplier<?> action) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("data", action.get());
            return ResponseEntity.ok(body);
        } catch (Exception error) {
            return ResponseEntity.ok(Map.of("success", false, "message", message(error)));
        }
    }

    static String text(Map<String, Object> input, String key) {
        Object value = input == null ? null : input.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            throw new IllegalArgumentException(key.toUpperCase() + "_REQUIRE");
        }
        return String.valueOf(value);
    }

    static String optional(Map<String, Object> input, String key) {
        Object value = input == null ? null : input.get(key);
        return value == null ? null : String.valueOf(value);
    }

    static boolean bool(Object value) {
        if (value instanceof Boolean bool) return bool;
        if (value != null && ("true".equalsIgnoreCase(String.valueOf(value))
                || "false".equalsIgnoreCase(String.valueOf(value)))) {
            return Boolean.parseBoolean(String.valueOf(value));
        }
        throw new IllegalArgumentException("INVALID_STATUS");
    }

    static int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return value == null ? fallback : Integer.parseInt(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String message(Exception error) {
        return error.getMessage() == null ? "Internal server error" : error.getMessage();
    }
}
