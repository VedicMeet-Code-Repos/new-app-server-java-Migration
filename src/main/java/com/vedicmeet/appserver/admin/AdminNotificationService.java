package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminNotificationService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminNotificationService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, Object> body) {
        return todo("GET /");
    }

    public Map<String, Object> send(Map<String, Object> body) {
        return single("POST /send");
    }

    public Map<String, Object> delete(Map<String, Object> body) {
        return single("DELETE /delete");
    }

    public Map<String, Object> adminNotification(Map<String, Object> body) {
        return todo("GET /admin-notification");
    }

    public Map<String, Object> readNotification(Map<String, Object> body) {
        return single("GET /read-notification");
    }

    public Map<String, Object> count(Map<String, Object> body) {
        return single("GET /count");
    }

    public Map<String, Object> scheduled(Map<String, String> query) {
        return todo("GET /scheduled");
    }

    public Map<String, Object> schedule(Map<String, Object> body) {
        return single("POST /schedule");
    }

    public Map<String, Object> cancelScheduled(Map<String, String> query) {
        return single("DELETE /scheduled/cancel");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT notification.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT notification.js #" + ref);
        return r;
    }
}