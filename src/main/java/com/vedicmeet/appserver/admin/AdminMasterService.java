package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminMasterService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminMasterService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, String> query, String adminId) {
        return todo("GET /");
    }

    public Map<String, Object> addEdit(Map<String, Object> body, String adminId) {
        return todoObject("POST /add_edit");
    }

    public Map<String, Object> addVimshotri(Map<String, Object> body, String adminId) {
        return todoObject("POST /add_vimshotri");
    }

    public Map<String, Object> clear(Map<String, String> query, String adminId) {
        return todoObject("GET /clear");
    }

    public Map<String, Object> getCategory(Map<String, String> query, String adminId) {
        return todo("GET /getCategory");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT master.js #" + ref);
        return r;
    }

    private Map<String, Object> todoObject(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT master.js #" + ref);
        return r;
    }
}
