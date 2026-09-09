package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scaffold service for admin analytics.js routes. */
@Service
public class AdminAnalyticsService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminAnalyticsService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> logs(Map<String, String> query) { return todo("/logs"); }

    public Map<String, Object> summary(Map<String, String> query) { return todo("/summary"); }

    public Map<String, Object> log(String id) { return objectTodo("/logs/{id}"); }

    public Map<String, Object> uniqueDevices(Map<String, String> query) { return todo("/unique-devices"); }

    public Map<String, Object> uniqueUsers(Map<String, String> query) { return todo("/unique-users"); }

    public Map<String, Object> device(String deviceId, Map<String, String> query) { return todo("/device/{device_id}"); }

    public Map<String, Object> user(String userId, Map<String, String> query) { return todo("/user/{user_id}"); }

    public String export(Map<String, String> query) { return ""; }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", List.of());
        result.put("total", 0);
        result.put("_todo", "PORT analytics.js #" + ref);
        return result;
    }

    private Map<String, Object> objectTodo(String ref) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("_todo", "PORT analytics.js #" + ref);
        return result;
    }
}
