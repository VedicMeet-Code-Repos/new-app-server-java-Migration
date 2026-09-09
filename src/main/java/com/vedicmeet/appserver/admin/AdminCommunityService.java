package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminCommunityService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminCommunityService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, Object> body) {
        return todo("list");
    }

    public Map<String, Object> add(Map<String, Object> body) {
        return one("add");
    }

    public Map<String, Object> memberList(Map<String, String> query) {
        return todo("memberList");
    }

    public Map<String, Object> memberChatList(Map<String, Object> body) {
        return todo("memberChatList");
    }

    public Map<String, Object> changeCommunityUserStatus(Map<String, Object> body) {
        return one("changeCommunityUserStatus");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT community.js #" + ref);
        return r;
    }

    private Map<String, Object> one(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT community.js #" + ref);
        return r;
    }
}
