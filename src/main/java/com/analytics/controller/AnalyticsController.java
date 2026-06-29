package com.analytics.controller;

import com.analytics.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * GET /api/analytics/engagement?from=2024-01-01&to=2024-01-31
     */
    @GetMapping("/engagement")
    public ResponseEntity<Map<String, Object>> getEngagementMetrics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = (from != null ? from : LocalDate.now().minusDays(30))
                .atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = (to != null ? to : LocalDate.now())
                .atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();

        Map<String, Object> metrics = analyticsService.getEngagementMetrics(fromInstant, toInstant);
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/analytics/sales-trends?period=daily|weekly|monthly
     */
    @GetMapping("/sales-trends")
    public ResponseEntity<List<Map<String, Object>>> getSalesTrends(
            @RequestParam(defaultValue = "daily") String period) {

        List<Map<String, Object>> trends = analyticsService.getSalesTrends(period);
        return ResponseEntity.ok(trends);
    }

    /**
     * GET /api/analytics/channel-performance
     */
    @GetMapping("/channel-performance")
    public ResponseEntity<List<Map<String, Object>>> getChannelPerformance() {
        List<Map<String, Object>> performance = analyticsService.getChannelPerformance();
        return ResponseEntity.ok(performance);
    }

    /**
     * GET /api/analytics/top-products?limit=10
     */
    @GetMapping("/top-products")
    public ResponseEntity<List<Map<String, Object>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> products = analyticsService.getTopProducts(limit);
        return ResponseEntity.ok(products);
    }
}
