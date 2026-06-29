package com.analytics.service;

import com.analytics.model.CustomerEvent;
import com.analytics.model.CustomerEvent.EventType;
import com.analytics.model.CustomerSegment;
import com.analytics.model.CustomerSegment.ChurnRisk;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SegmentService {

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public List<CustomerSegment> getAll() {
        return em.createQuery("SELECT s FROM CustomerSegment s ORDER BY s.customerCount DESC",
                CustomerSegment.class).getResultList();
    }

    /**
     * Recompute segments from live purchase data using RFM-like logic.
     * Segments: Champions, Loyal Customers, At Risk, Churned, New Customers
     */
    @Transactional
    public List<CustomerSegment> computeSegments() {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant ninetyDaysAgo = now.minus(90, ChronoUnit.DAYS);

        // Gather per-customer stats
        List<Object[]> rows = em.createQuery(
                "SELECT e.customerId, COUNT(e), SUM(e.amount), MAX(e.timestamp) " +
                "FROM CustomerEvent e WHERE e.eventType = :type " +
                "GROUP BY e.customerId", Object[].class)
                .setParameter("type", EventType.PURCHASE)
                .getResultList();

        // Buckets
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        buckets.put("Champions", new ArrayList<>());
        buckets.put("Loyal Customers", new ArrayList<>());
        buckets.put("At Risk", new ArrayList<>());
        buckets.put("Churned", new ArrayList<>());
        buckets.put("New Customers", new ArrayList<>());

        Map<String, BigDecimal> ltvBySegment = new LinkedHashMap<>();
        Map<String, Integer> countBySegment = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String customerId = (String) row[0];
            long frequency = (Long) row[1];
            BigDecimal ltv = row[2] == null ? BigDecimal.ZERO : (BigDecimal) row[2];
            Instant lastPurchase = (Instant) row[3];

            boolean recentBuyer = lastPurchase.isAfter(thirtyDaysAgo);
            boolean moderateBuyer = lastPurchase.isAfter(ninetyDaysAgo);

            String segment;
            if (recentBuyer && frequency >= 5 && ltv.compareTo(new BigDecimal("500")) >= 0) {
                segment = "Champions";
            } else if (recentBuyer && frequency >= 2) {
                segment = "Loyal Customers";
            } else if (moderateBuyer) {
                segment = "At Risk";
            } else if (!moderateBuyer) {
                segment = "Churned";
            } else {
                segment = "New Customers";
            }

            buckets.get(segment).add(customerId);
            ltvBySegment.merge(segment, ltv, BigDecimal::add);
        }

        // Also count customers with no purchases (signups without purchase) → New Customers
        Long signupOnlyCount = em.createQuery(
                "SELECT COUNT(DISTINCT e.customerId) FROM CustomerEvent e " +
                "WHERE e.customerId NOT IN " +
                "(SELECT DISTINCT e2.customerId FROM CustomerEvent e2 WHERE e2.eventType = :type)",
                Long.class)
                .setParameter("type", EventType.PURCHASE)
                .getSingleResult();
        for (int i = 0; i < signupOnlyCount; i++) {
            buckets.get("New Customers").add("__signup__" + i);
        }

        // Persist / update segments
        List<CustomerSegment> result = new ArrayList<>();

        Map<String, ChurnRisk> riskMap = Map.of(
                "Champions", ChurnRisk.LOW,
                "Loyal Customers", ChurnRisk.LOW,
                "At Risk", ChurnRisk.MEDIUM,
                "Churned", ChurnRisk.HIGH,
                "New Customers", ChurnRisk.LOW
        );

        for (Map.Entry<String, List<String>> entry : buckets.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue().size();
            BigDecimal totalLtv = ltvBySegment.getOrDefault(name, BigDecimal.ZERO);
            BigDecimal avgLtv = count == 0 ? BigDecimal.ZERO
                    : totalLtv.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

            List<CustomerSegment> existing = em.createQuery(
                    "SELECT s FROM CustomerSegment s WHERE s.segmentName = :name",
                    CustomerSegment.class)
                    .setParameter("name", name)
                    .getResultList();

            CustomerSegment seg;
            if (existing.isEmpty()) {
                seg = new CustomerSegment(name,
                        buildCriteriaJson(name), count, avgLtv, riskMap.get(name));
                em.persist(seg);
            } else {
                seg = existing.get(0);
                seg.setCustomerCount(count);
                seg.setAvgLTV(avgLtv);
            }
            result.add(seg);
        }

        return result;
    }

    private String buildCriteriaJson(String segment) {
        return switch (segment) {
            case "Champions"      -> "{\"minFrequency\":5,\"maxRecencyDays\":30,\"minLTV\":500}";
            case "Loyal Customers"-> "{\"minFrequency\":2,\"maxRecencyDays\":30}";
            case "At Risk"        -> "{\"maxRecencyDays\":90,\"minRecencyDays\":30}";
            case "Churned"        -> "{\"minRecencyDays\":90}";
            default               -> "{\"noPurchase\":true}";
        };
    }

    /**
     * Returns customer IDs belonging to a segment.
     */
    @Transactional(readOnly = true)
    public List<String> getSegmentCustomers(Long segmentId) {
        CustomerSegment seg = em.find(CustomerSegment.class, segmentId);
        if (seg == null) throw new NoSuchElementException("Segment not found: " + segmentId);

        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant ninetyDaysAgo = now.minus(90, ChronoUnit.DAYS);

        return switch (seg.getSegmentName()) {
            case "Champions" -> em.createQuery(
                    "SELECT DISTINCT e.customerId FROM CustomerEvent e WHERE e.eventType = :type " +
                    "GROUP BY e.customerId HAVING COUNT(e) >= 5 AND MAX(e.timestamp) > :cutoff",
                    String.class)
                    .setParameter("type", EventType.PURCHASE)
                    .setParameter("cutoff", thirtyDaysAgo)
                    .getResultList();
            case "Churned" -> em.createQuery(
                    "SELECT DISTINCT e.customerId FROM CustomerEvent e WHERE e.eventType = :type " +
                    "GROUP BY e.customerId HAVING MAX(e.timestamp) < :cutoff",
                    String.class)
                    .setParameter("type", EventType.PURCHASE)
                    .setParameter("cutoff", ninetyDaysAgo)
                    .getResultList();
            default -> em.createQuery(
                    "SELECT DISTINCT e.customerId FROM CustomerEvent e",
                    String.class)
                    .setMaxResults(1000)
                    .getResultList();
        };
    }

    /**
     * Returns customers with HIGH churn risk across all segments.
     */
    @Transactional(readOnly = true)
    public List<String> getChurnRiskCustomers() {
        Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
        return em.createQuery(
                "SELECT DISTINCT e.customerId FROM CustomerEvent e WHERE e.eventType = :type " +
                "GROUP BY e.customerId HAVING MAX(e.timestamp) < :cutoff",
                String.class)
                .setParameter("type", EventType.PURCHASE)
                .setParameter("cutoff", ninetyDaysAgo)
                .getResultList();
    }
}
