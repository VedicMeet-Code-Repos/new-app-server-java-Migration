package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminCmsService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminCmsService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> getCmsDetails(Map<String, String> query) {
        return singleTodo("getCmsDetails");
    }

    public Map<String, Object> addAndUpdateCms(Map<String, Object> body) {
        return singleTodo("addAndUpdateCms");
    }

    public Map<String, Object> addContent(Map<String, Object> body) {
        return singleTodo("addContent");
    }

    public Map<String, Object> editContent(Map<String, Object> body) {
        return singleTodo("editContent");
    }

    public Map<String, Object> listContent(Map<String, String> query) {
        return todo("listContent");
    }

    public Map<String, Object> statusChangeContent(Map<String, Object> body) {
        return singleTodo("statusChageContent");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("list", List.of());
        r.put("total", 0);
        r.put("_todo", "PORT cms.js #" + ref);
        return r;
    }

    private Map<String, Object> singleTodo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT cms.js #" + ref);
        return r;
    }
}
