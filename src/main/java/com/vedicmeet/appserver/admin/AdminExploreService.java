package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminExploreService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminExploreService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("list");
    }

    public Map<String, Object> add(Map<String, Object> body) {
        return single("add");
    }

    public Map<String, Object> addDetailVideo(Map<String, Object> body) {
        return single("add_detail_video");
    }

    public Map<String, Object> update(Map<String, Object> body) {
        return single("update");
    }

    public Map<String, Object> blockUnblock(Map<String, Object> body) {
        return single("blockUnblock");
    }

    public Map<String, Object> getDetails(Map<String, String> query) {
        return single("getDetails");
    }

    public Map<String, Object> getLikesAndComments(Map<String, String> query) {
        return todo("getLikesAndComments");
    }

    public Map<String, Object> updateCommentVisibility(Map<String, Object> body) {
        return single("updateCommentVisibility");
    }

    public Map<String, Object> getExploreInsights(Map<String, String> query) {
        return single("getExploreInsights");
    }

    public Map<String, Object> getExploreActivityMetrics(Map<String, String> query) {
        return todo("getExploreActivityMetrics");
    }

    public Map<String, Object> getUserActivityLog(Map<String, String> query) {
        return todo("getUserActivityLog");
    }

    public Map<String, Object> publishScheduledPosts() {
        return single("publishScheduledPosts");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT explore.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT explore.js #" + ref);
        return r;
    }
}