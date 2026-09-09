package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminConcernRemedyService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminConcernRemedyService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> listAreaOfConcern(Map<String, String> query) {
        return todo("listAreaOfConcern");
    }

    public Map<String, Object> addAreaOfConcern(Map<String, Object> body) {
        return one("addAreaOfConcern");
    }

    public Map<String, Object> updateAreaOfConcern(Map<String, Object> body) {
        return one("updateAreaOfConcern");
    }

    public Map<String, Object> blockUnblockAreaOfConcern(Map<String, Object> body) {
        return one("blockUnblockAreaOfConcern");
    }

    public Map<String, Object> addAreaOfConcernRemedyMedia(Map<String, Object> body) {
        return one("addAreaOfConcernRemedyMedia");
    }

    public Map<String, Object> editAreaOfConcernRemedyMedia(Map<String, Object> body) {
        return one("editAreaOfConcernRemedyMedia");
    }

    public Map<String, Object> blockUnblockAreaOfConcernRemedyMedia(Map<String, Object> body) {
        return one("blockUnblockAreaOfConcernRemedyMedia");
    }

    public Map<String, Object> listAreaOfConcernRemedyMedia(Map<String, String> query) {
        return todo("listAreaOfConcernRemedyMedia");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT concern-remedy.js #" + ref);
        return r;
    }

    private Map<String, Object> one(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT concern-remedy.js #" + ref);
        return r;
    }
}
