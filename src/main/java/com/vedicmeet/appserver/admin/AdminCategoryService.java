package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCategoryService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminCategoryService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> listCategory(Map<String, String> query) {
        return todo("listCategory");
    }

    public Map<String, Object> addCategory(Map<String, Object> body) {
        return singleTodo("addCategory");
    }

    public Map<String, Object> updateCategory(Map<String, Object> body) {
        return singleTodo("updateCategory");
    }

    public Map<String, Object> blockUnblock(Map<String, Object> body) {
        return singleTodo("blockUnblock");
    }

    public Map<String, Object> addMusicOrMantra(Map<String, Object> body) {
        return singleTodo("addMusicOrMantra");
    }

    public Map<String, Object> updateMusicOrMantra(Map<String, Object> body) {
        return singleTodo("updateMusicOrMantra");
    }

    public Map<String, Object> listMediaCategory(Map<String, String> query) {
        return todo("listMediaCategory");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", List.of());
        r.put("total", 0);
        r.put("_todo", "PORT category.js #" + ref);
        return r;
    }

    private Map<String, Object> singleTodo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT category.js #" + ref);
        return r;
    }
}
