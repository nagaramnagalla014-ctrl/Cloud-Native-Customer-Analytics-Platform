package com.analytics.service;

import com.analytics.model.AnalyticsReport;
import com.analytics.model.AnalyticsReport.ReportStatus;
import com.analytics.model.AnalyticsReport.ReportType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

@Service
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    @Value("${airflow.base-url}")
    private String airflowBaseUrl;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    private final RestTemplate restTemplate = new RestTemplate();
    private final S3Presigner s3Presigner;

    public ReportService(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    /**
     * Persist a new report record and trigger an Apache Airflow DAG run.
     */
    @Transactional
    public AnalyticsReport generateReport(String reportName, ReportType reportType,
                                           LocalDate startDate, LocalDate endDate,
                                           String generatedBy) {
        AnalyticsReport report = new AnalyticsReport(reportName, reportType,
                startDate, endDate, generatedBy);
        em.persist(report);
        em.flush(); // obtain reportId before calling Airflow

        triggerAirflowDag(report.getReportId().toString(), reportType, startDate, endDate);
        return report;
    }

    /**
     * Fire-and-forget POST to Airflow REST API to trigger the analytics DAG.
     */
    private void triggerAirflowDag(String reportId, ReportType reportType,
                                    LocalDate startDate, LocalDate endDate) {
        String url = airflowBaseUrl + "/api/v1/dags/customer_analytics_report/dagRuns";

        Map<String, Object> conf = new LinkedHashMap<>();
        conf.put("report_id", reportId);
        conf.put("report_type", reportType.name());
        conf.put("start_date", startDate.toString());
        conf.put("end_date", endDate.toString());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dag_run_id", "report_" + reportId);
        body.put("conf", conf);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception ex) {
            // Log and continue — report status will remain QUEUED until Airflow callback
            System.err.println("[ReportService] Airflow trigger failed for report "
                    + reportId + ": " + ex.getMessage());
        }
    }

    /**
     * Poll Airflow for the current DAG run status and sync back to DB.
     */
    @Transactional
    public AnalyticsReport pollStatus(UUID reportId) {
        AnalyticsReport report = em.find(AnalyticsReport.class, reportId);
        if (report == null) throw new NoSuchElementException("Report not found: " + reportId);

        if (report.getStatus() == ReportStatus.COMPLETE
                || report.getStatus() == ReportStatus.FAILED) {
            return report;
        }

        String url = airflowBaseUrl + "/api/v1/dags/customer_analytics_report/dagRuns/report_" + reportId;
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                String state = (String) resp.getBody().get("state");
                report.setStatus(mapAirflowState(state));
            }
        } catch (Exception ex) {
            System.err.println("[ReportService] Airflow poll failed: " + ex.getMessage());
        }
        return report;
    }

    private ReportStatus mapAirflowState(String state) {
        if (state == null) return ReportStatus.QUEUED;
        return switch (state.toLowerCase()) {
            case "success" -> ReportStatus.COMPLETE;
            case "failed"  -> ReportStatus.FAILED;
            case "running" -> ReportStatus.PROCESSING;
            default        -> ReportStatus.QUEUED;
        };
    }

    @Transactional(readOnly = true)
    public Optional<AnalyticsReport> getReportById(UUID reportId) {
        return Optional.ofNullable(em.find(AnalyticsReport.class, reportId));
    }

    @Transactional(readOnly = true)
    public List<AnalyticsReport> listReports() {
        return em.createQuery(
                "SELECT r FROM AnalyticsReport r ORDER BY r.createdAt DESC",
                AnalyticsReport.class)
                .getResultList();
    }

    /**
     * Generate a pre-signed S3 URL valid for 15 minutes.
     */
    public String downloadReport(UUID reportId) {
        AnalyticsReport report = em.find(AnalyticsReport.class, reportId);
        if (report == null) throw new NoSuchElementException("Report not found: " + reportId);
        if (report.getStatus() != ReportStatus.COMPLETE || report.getS3Path() == null) {
            throw new IllegalStateException("Report is not ready for download.");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Bucket)
                .key(report.getS3Path())
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
