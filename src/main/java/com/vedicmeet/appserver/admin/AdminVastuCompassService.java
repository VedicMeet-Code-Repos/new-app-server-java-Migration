package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminVastuCompassService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminVastuCompassService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> listCategory(Map<String, String> query) {
        return todo("listCategory");
    }

    public Map<String, Object> addVastuCompassCategory(Map<String, Object> body) {
        return singleTodo("addVastuCompassCategory");
    }

    public Map<String, Object> updateVastuCategory(Map<String, Object> body) {
        return singleTodo("updateVastuCategory");
    }

    public Map<String, Object> blockUnblockCategory(Map<String, Object> body) {
        return singleTodo("blockUnblockCategory");
    }

    public Map<String, Object> addEditZone(Map<String, Object> body) {
        return singleTodo("addEditZone");
    }

    public Map<String, Object> getZone(Map<String, String> query) {
        return singleTodo("getZone");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", List.of());
        r.put("total", 0);
        r.put("_todo", "PORT vastu-compass.js #" + ref);
        return r;
    }

    private Map<String, Object> singleTodo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT vastu-compass.js #" + ref);
        return r;
    }
}
