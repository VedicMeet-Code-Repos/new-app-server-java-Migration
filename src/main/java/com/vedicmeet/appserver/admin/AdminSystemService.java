package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminSystemService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminSystemService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> createCoupon(Map<String, Object> body) {
        return todoObject("POST /coupon/create");
    }

    public Map<String, Object> couponList(Map<String, String> query) {
        return todo("GET /coupon/list");
    }

    public Map<String, Object> notification(Map<String, String> query) {
        return todoObject("GET /notification");
    }

    public Map<String, Object> availability(Map<String, String> query) {
        return todoObject("GET /availability");
    }

    public Map<String, Object> masterGet(Map<String, String> query) {
        return todoObject("GET /master/get");
    }

    public Map<String, Object> masterUpdate(Map<String, Object> body) {
        return todoObject("POST /master/update");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT system.js #" + ref);
        return r;
    }

    private Map<String, Object> todoObject(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT system.js #" + ref);
        return r;
    }
}
