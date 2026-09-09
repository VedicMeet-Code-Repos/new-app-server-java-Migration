package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminGrowthTrainingService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminGrowthTrainingService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> addGrowthTrainingCategory(Map<String, Object> body) {
        return single("addGrowthTrainingCategory");
    }

    public Map<String, Object> updateGrowthTrainingCategory(Map<String, Object> body) {
        return single("updateGrowthTrainingCategory");
    }

    public Map<String, Object> blockUnblockCategory(Map<String, Object> body) {
        return single("blockUnblockCategory");
    }

    public Map<String, Object> listMedidationCategory(Map<String, String> query) {
        return todo("listMedidationCategory");
    }

    public Map<String, Object> getDetailsCategory(Map<String, String> query) {
        return single("getDetailsCategory");
    }

    public Map<String, Object> addMedia(Map<String, Object> body) {
        return single("addMedia");
    }

    public Map<String, Object> editMedia(Map<String, Object> body) {
        return single("editMedia");
    }

    public Map<String, Object> blockUnblockMedia(Map<String, Object> body) {
        return single("blockUnblockMedia");
    }

    public Map<String, Object> detailMedia(Map<String, String> query) {
        return single("detailMedia");
    }

    public Map<String, Object> listMedia(Map<String, String> query) {
        return todo("listMedia");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT growth-training.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT growth-training.js #" + ref);
        return r;
    }
}