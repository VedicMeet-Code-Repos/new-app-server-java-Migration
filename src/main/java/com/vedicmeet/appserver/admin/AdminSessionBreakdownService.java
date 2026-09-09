package com.vedicmeet.appserver.admin;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminSessionBreakdownService {

    private final MongoTemplate mongo;
    private final AdminMongoSupport support;

    public AdminSessionBreakdownService(MongoTemplate mongo, AdminMongoSupport support) {
        this.mongo = mongo;
        this.support = support;
    }

    public Map<String, Object> breakdown(String id) {
        return single("GET /:id/breakdown");
    }

    public Map<String, Object> couponOfferAnalytics(Map<String, String> query) {
        return single("GET /analytics/coupon-offer");
    }

    public Map<String, Object> consultantCouponAnalytics(Map<String, String> query) {
        return single("GET /analytics/consultant-coupon");
    }

    private Map<String, Object> todo(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("list", java.util.List.of());
        r.put("total", 0);
        r.put("_todo", "PORT session-breakdown.js #" + ref);
        return r;
    }

    private Map<String, Object> single(String ref) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("_todo", "PORT session-breakdown.js #" + ref);
        return r;
    }
}