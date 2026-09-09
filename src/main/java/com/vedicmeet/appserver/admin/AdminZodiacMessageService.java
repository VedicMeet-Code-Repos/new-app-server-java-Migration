package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminZodiacMessageService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminZodiacMessageService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> create(Map<String, Object> body, String adminId) {
        return todoObject("POST /create");
    }

    public Map<String, Object> update(Map<String, Object> body, String adminId) {
        return todoObject("PUT /update");
    }

    public Map<String, Object> get(Map<String, String> query) {
        return todoObject("GET /get");
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("GET /list");
    }

    public Map<String, Object> delete(Map<String, Object> body, String adminId) {
        return todoObject("DELETE /delete");
    }

    public Map<String, Object> bulkUpdate(Map<String, Object> body, String adminId) {
        return todo("POST /bulk-update");
    }

    public Map<String, Object> personalized(Map<String, String> query) {
        return todoObject("GET /personalized");
    }

    public Map<String, Object> userPersonalized(Map<String, String> query) {
        return todoObject("GET /user-personalized");
    }

    public Map<String, Object> seed(Map<String, Object> body, String adminId) {
        return todoObject("POST /seed");
    }

    public Map<String, Object> stats(Map<String, String> query) {
        return todoObject("GET /stats");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT zodiac-message.js #" + ref);
        return r;
    }

    private Map<String, Object> todoObject(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT zodiac-message.js #" + ref);
        return r;
    }
}
