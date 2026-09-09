package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminMembershipDiscountService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminMembershipDiscountService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> list(Map<String, String> query) {
        return todo("list");
    }

    public Map<String, Object> add(Map<String, Object> body) {
        return one("add");
    }

    public Map<String, Object> addMasterCoupon(Map<String, Object> body) {
        return one("addMster");
    }

    public Map<String, Object> update(Map<String, Object> body) {
        return one("update");
    }

    public Map<String, Object> blockUnblock(Map<String, Object> body) {
        return one("blockUnblock");
    }

    public Map<String, Object> details(Map<String, String> query) {
        return one("details");
    }

    public Map<String, Object> listOffers(Map<String, String> query) {
        return todo("listOffers");
    }

    public Map<String, Object> addOffers(Map<String, Object> body) {
        return one("addOffers");
    }

    public Map<String, Object> editOffers(Map<String, Object> body) {
        return one("editOffers");
    }

    public Map<String, Object> blockUnblockOffers(Map<String, String> query) {
        return one("blockUnblockOffers");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT membership-discount.js #" + ref);
        return r;
    }

    private Map<String, Object> one(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT membership-discount.js #" + ref);
        return r;
    }
}
