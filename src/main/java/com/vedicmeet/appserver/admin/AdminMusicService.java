package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminMusicService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminMusicService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> addMusicCategory(Map<String, Object> body) {
        return single("addMusicCategory");
    }

    public Map<String, Object> blockMusicCategory(Map<String, Object> body) {
        return single("blockMusicCategory");
    }

    public Map<String, Object> listMusicCategory(Map<String, String> query) {
        return todo("listMusicCategory");
    }

    public Map<String, Object> getDetailMusicCategory(Map<String, String> query) {
        return single("getDetailMusicCategory");
    }

    public Map<String, Object> addMusicMedia(Map<String, Object> body) {
        return single("addMusicMedia");
    }

    public Map<String, Object> editMusicMedia(Map<String, Object> body) {
        return single("editMusicMedia");
    }

    public Map<String, Object> getDetailMusicMedia(Map<String, String> query) {
        return single("getDetailMusicMedia");
    }

    public Map<String, Object> blockMusicMedia(Map<String, Object> body) {
        return single("blockMusicMedia");
    }

    public Map<String, Object> listMusicMedia(Map<String, String> query) {
        return todo("listMusicMedia");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT music.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT music.js #" + ref);
        return r;
    }
}