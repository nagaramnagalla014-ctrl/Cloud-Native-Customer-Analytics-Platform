package com.analytics.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "analytics_reports")
public class AnalyticsReport {

    public enum ReportType {
        DAILY, WEEKLY, MONTHLY, CUSTOM
    }

    public enum ReportStatus {
        QUEUED, PROCESSING, COMPLETE, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "report_id", updatable = false, nullable = false)
    private UUID reportId;

    @Column(nullable = false, length = 255)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportType reportType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 128)
    private String generatedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ReportStatus status;

    @Column(length = 512)
    private String s3Path;

    /**
     * JSON blob containing:
     *   totalRevenue, totalOrders, uniqueCustomers,
     *   conversionRate, avgOrderValue, churnRate
     */
    @Column(columnDefinition = "TEXT")
    private String metrics;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = ReportStatus.QUEUED;
        }
    }

    // Constructors
    public AnalyticsReport() {}

    public AnalyticsReport(String reportName, ReportType reportType,
                            LocalDate startDate, LocalDate endDate, String generatedBy) {
        this.reportName = reportName;
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.generatedBy = generatedBy;
        this.status = ReportStatus.QUEUED;
    }

    // Getters and Setters
    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public String getS3Path() { return s3Path; }
    public void setS3Path(String s3Path) { this.s3Path = s3Path; }

    public String getMetrics() { return metrics; }
    public void setMetrics(String metrics) { this.metrics = metrics; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
