package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminEventService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminEventService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("list");
    }

    public Map<String, Object> sendMessages(Map<String, Object> body) {
        return one("sendMessages");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT event.js #" + ref);
        return r;
    }

    private Map<String, Object> one(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT event.js #" + ref);
        return r;
    }
}
