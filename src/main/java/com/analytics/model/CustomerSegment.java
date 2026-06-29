package com.analytics.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "customer_segments")
public class CustomerSegment {

    public enum ChurnRisk {
        LOW, MEDIUM, HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "segment_id")
    private Long segmentId;

    @Column(nullable = false, unique = true, length = 100)
    private String segmentName;

    /**
     * JSON blob defining the segmentation criteria:
     *   e.g. {"minLTV": 500, "maxRecencyDays": 30, "minFrequency": 5}
     */
    @Column(columnDefinition = "TEXT")
    private String criteria;

    @Column(nullable = false)
    private Integer customerCount;

    @Column(precision = 12, scale = 2)
    private BigDecimal avgLTV;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ChurnRisk churnRisk;

    @Column(nullable = false)
    private Instant lastUpdated;

    @PrePersist
    @PreUpdate
    private void touch() {
        this.lastUpdated = Instant.now();
    }

    // Constructors
    public CustomerSegment() {}

    public CustomerSegment(String segmentName, String criteria,
                           Integer customerCount, BigDecimal avgLTV, ChurnRisk churnRisk) {
        this.segmentName = segmentName;
        this.criteria = criteria;
        this.customerCount = customerCount;
        this.avgLTV = avgLTV;
        this.churnRisk = churnRisk;
    }

    // Getters and Setters
    public Long getSegmentId() { return segmentId; }
    public void setSegmentId(Long segmentId) { this.segmentId = segmentId; }

    public String getSegmentName() { return segmentName; }
    public void setSegmentName(String segmentName) { this.segmentName = segmentName; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public Integer getCustomerCount() { return customerCount; }
    public void setCustomerCount(Integer customerCount) { this.customerCount = customerCount; }

    public BigDecimal getAvgLTV() { return avgLTV; }
    public void setAvgLTV(BigDecimal avgLTV) { this.avgLTV = avgLTV; }

    public ChurnRisk getChurnRisk() { return churnRisk; }
    public void setChurnRisk(ChurnRisk churnRisk) { this.churnRisk = churnRisk; }

    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}
