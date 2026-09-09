package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminRequestService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminRequestService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("list");
    }

    public Map<String, Object> details(Map<String, String> query) {
        return one("details");
    }

    public Map<String, Object> acceptAndReject(Map<String, Object> body) {
        return one("acceptAndReject");
    }

    public Map<String, Object> add(Map<String, Object> body) {
        return one("add");
    }

    public Map<String, Object> update(Map<String, Object> body) {
        return one("update");
    }

    public Map<String, Object> getByConsultantId(String id) {
        return one("getByConsultantId");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT request.js #" + ref);
        return r;
    }

    private Map<String, Object> one(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT request.js #" + ref);
        return r;
    }
}
