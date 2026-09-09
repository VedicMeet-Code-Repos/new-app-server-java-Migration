package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/** MONEY-MOVING: intentionally not implemented; must not move funds; requires human sign-off before real porting. */
@Service
public class AdminPayoutService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminPayoutService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> export(Map<String, String> query) {
        return todo("GET /export");
    }

    public Map<String, Object> exportWithPeriod(Map<String, String> query) {
        return todo("GET /export-with-period");
    }

    public Map<String, Object> uploadWithSettlement(Map<String, Object> body) {
        return todo("POST /upload-with-settlement");
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("GET /");
    }

    public Map<String, Object> specificMonth(Map<String, String> query) {
        return todo("GET /specific-month");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("_todo", "PORT payout.js #" + ref);
        return r;
    }
}
