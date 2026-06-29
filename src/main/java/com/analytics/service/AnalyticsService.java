package com.analytics.service;

import com.analytics.model.CustomerEvent;
import com.analytics.model.CustomerEvent.EventType;
import com.analytics.model.CustomerEvent.Channel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns engagement metrics (DAU, WAU, MAU, avg session duration, bounce rate)
     * for the supplied date range.
     */
    public Map<String, Object> getEngagementMetrics(Instant from, Instant to) {
        // Daily Active Users — distinct customers in range
        Long dau = em.createQuery(
                "SELECT COUNT(DISTINCT e.customerId) FROM CustomerEvent e " +
                "WHERE e.timestamp BETWEEN :from AND :to", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        Instant weekAgo = to.minus(7, ChronoUnit.DAYS);
        Long wau = em.createQuery(
                "SELECT COUNT(DISTINCT e.customerId) FROM CustomerEvent e " +
                "WHERE e.timestamp BETWEEN :from AND :to", Long.class)
                .setParameter("from", weekAgo)
                .setParameter("to", to)
                .getSingleResult();

        Instant monthAgo = to.minus(30, ChronoUnit.DAYS);
        Long mau = em.createQuery(
                "SELECT COUNT(DISTINCT e.customerId) FROM CustomerEvent e " +
                "WHERE e.timestamp BETWEEN :from AND :to", Long.class)
                .setParameter("from", monthAgo)
                .setParameter("to", to)
                .getSingleResult();

        // Total events — used to estimate session duration proxy
        Long totalEvents = em.createQuery(
                "SELECT COUNT(e) FROM CustomerEvent e WHERE e.timestamp BETWEEN :from AND :to",
                Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        // Bounce rate = sessions with only one PAGE_VIEW / total sessions
        Long singlePageSessions = em.createQuery(
                "SELECT COUNT(e.sessionId) FROM CustomerEvent e " +
                "WHERE e.timestamp BETWEEN :from AND :to AND e.eventType = :type " +
                "GROUP BY e.sessionId HAVING COUNT(e) = 1", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .setParameter("type", EventType.PAGE_VIEW)
                .getResultList()
                .stream().mapToLong(Long::longValue).count();

        Long totalSessions = em.createQuery(
                "SELECT COUNT(DISTINCT e.sessionId) FROM CustomerEvent e " +
                "WHERE e.timestamp BETWEEN :from AND :to", Long.class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getSingleResult();

        double bounceRate = totalSessions == 0 ? 0.0
                : BigDecimal.valueOf((double) singlePageSessions / totalSessions * 100)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();

        // Approximate avg session duration: 3.5 seconds per event * events per session
        double avgSessionDurationSec = totalSessions == 0 ? 0
                : BigDecimal.valueOf((double) totalEvents / totalSessions * 3.5)
                    .setScale(1, RoundingMode.HALF_UP).doubleValue();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dau", dau);
        result.put("wau", wau);
        result.put("mau", mau);
        result.put("avgSessionDurationSeconds", avgSessionDurationSec);
        result.put("bounceRate", bounceRate);
        result.put("rangeFrom", from.toString());
        result.put("rangeTo", to.toString());
        return result;
    }

    /**
     * Returns daily or weekly revenue aggregation from the PURCHASE events.
     * period: "daily" | "weekly" | "monthly"
     */
    public List<Map<String, Object>> getSalesTrends(String period) {
        Instant now = Instant.now();
        Instant from;
        int bucketDays;

        switch (period.toLowerCase()) {
            case "weekly"  -> { from = now.minus(84, ChronoUnit.DAYS);  bucketDays = 7; }
            case "monthly" -> { from = now.minus(365, ChronoUnit.DAYS); bucketDays = 30; }
            default        -> { from = now.minus(30, ChronoUnit.DAYS);  bucketDays = 1; }
        }

        List<CustomerEvent> purchases = em.createQuery(
                "SELECT e FROM CustomerEvent e " +
                "WHERE e.eventType = :type AND e.timestamp >= :from AND e.amount IS NOT NULL " +
                "ORDER BY e.timestamp", CustomerEvent.class)
                .setParameter("type", EventType.PURCHASE)
                .setParameter("from", from)
                .getResultList();

        // Group into buckets
        TreeMap<LocalDate, BigDecimal> buckets = new TreeMap<>();
        for (CustomerEvent e : purchases) {
            LocalDate day = e.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
            // Align to bucket start
            long epochDay = day.toEpochDay();
            LocalDate bucketStart = LocalDate.ofEpochDay((epochDay / bucketDays) * bucketDays);
            buckets.merge(bucketStart, e.getAmount() == null ? BigDecimal.ZERO : e.getAmount(),
                    BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        buckets.forEach((date, revenue) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", date.toString());
            row.put("revenue", revenue.setScale(2, RoundingMode.HALF_UP));
            result.add(row);
        });
        return result;
    }

    /**
     * Returns events by channel with conversion rates
     * (conversion = PURCHASE / total events per channel).
     */
    public List<Map<String, Object>> getChannelPerformance() {
        // Total events per channel
        List<Object[]> totals = em.createQuery(
                "SELECT e.channel, COUNT(e) FROM CustomerEvent e GROUP BY e.channel",
                Object[].class).getResultList();

        // Purchases per channel
        List<Object[]> purchases = em.createQuery(
                "SELECT e.channel, COUNT(e) FROM CustomerEvent e " +
                "WHERE e.eventType = :type GROUP BY e.channel", Object[].class)
                .setParameter("type", EventType.PURCHASE)
                .getResultList();

        Map<Channel, Long> purchaseMap = new EnumMap<>(Channel.class);
        for (Object[] row : purchases) {
            purchaseMap.put((Channel) row[0], (Long) row[1]);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : totals) {
            Channel ch = (Channel) row[0];
            long total = (Long) row[1];
            long bought = purchaseMap.getOrDefault(ch, 0L);
            double convRate = total == 0 ? 0.0
                    : BigDecimal.valueOf((double) bought / total * 100)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("channel", ch.name());
            entry.put("totalEvents", total);
            entry.put("purchases", bought);
            entry.put("conversionRate", convRate);
            result.add(entry);
        }
        return result;
    }

    /**
     * Returns the top-N most purchased products by revenue.
     */
    public List<Map<String, Object>> getTopProducts(int limit) {
        List<Object[]> rows = em.createQuery(
                "SELECT e.productId, COUNT(e), SUM(e.amount) FROM CustomerEvent e " +
                "WHERE e.eventType = :type AND e.productId IS NOT NULL " +
                "GROUP BY e.productId ORDER BY SUM(e.amount) DESC", Object[].class)
                .setParameter("type", EventType.PURCHASE)
                .setMaxResults(limit)
                .getResultList();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("productId", row[0]);
            entry.put("orderCount", row[1]);
            entry.put("totalRevenue",
                    row[2] == null ? BigDecimal.ZERO
                            : ((BigDecimal) row[2]).setScale(2, RoundingMode.HALF_UP));
            result.add(entry);
        }
        return result;
    }
}
