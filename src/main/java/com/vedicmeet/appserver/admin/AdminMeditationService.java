package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminMeditationService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminMeditationService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> addMeditationCategory(Map<String, Object> body) {
        return single("addMeditationCategory");
    }

    public Map<String, Object> updateMeditationCategory(Map<String, Object> body) {
        return single("updateMeditationCategory");
    }

    public Map<String, Object> blockUnblockMeditationCategory(Map<String, Object> body) {
        return single("blockUnblockMeditationCategory");
    }

    public Map<String, Object> listMedidationCategory(Map<String, String> query) {
        return todo("listMedidationCategory");
    }

    public Map<String, Object> getMeditationCategory(Map<String, String> query) {
        return single("getMeditationCategory");
    }

    public Map<String, Object> addMeditationMedia(Map<String, Object> body) {
        return single("addMeditationMedia");
    }

    public Map<String, Object> editMeditationMedia(Map<String, Object> body) {
        return single("editMeditationMedia");
    }

    public Map<String, Object> blockMeditationMedia(Map<String, Object> body) {
        return single("blockMeditationMedia");
    }

    public Map<String, Object> getMeditationMedia(Map<String, String> query) {
        return single("getMeditationMedia");
    }

    public Map<String, Object> listMeditationMedia(Map<String, String> query) {
        return todo("listMeditationMedia");
    }

    public Map<String, Object> userListMeditationHistory(Map<String, Object> body) {
        return single("userListMeditationHistory");
    }

    public Map<String, Object> getMeditationInsights(Map<String, String> query) {
        return single("getMeditationInsights");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT meditation.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT meditation.js #" + ref);
        return r;
    }
}