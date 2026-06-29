package com.analytics.controller;

import com.analytics.model.AnalyticsReport;
import com.analytics.model.AnalyticsReport.ReportType;
import com.analytics.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * POST /api/reports/generate
     * Body: { "reportName": "...", "reportType": "MONTHLY",
     *         "startDate": "2024-01-01", "endDate": "2024-01-31",
     *         "generatedBy": "admin" }
     */
    @PostMapping("/generate")
    public ResponseEntity<AnalyticsReport> generateReport(
            @RequestBody Map<String, String> body) {

        String reportName  = body.getOrDefault("reportName", "Analytics Report");
        ReportType type    = ReportType.valueOf(
                body.getOrDefault("reportType", "MONTHLY").toUpperCase());
        LocalDate startDate = LocalDate.parse(
                body.getOrDefault("startDate", LocalDate.now().minusDays(30).toString()));
        LocalDate endDate   = LocalDate.parse(
                body.getOrDefault("endDate", LocalDate.now().toString()));
        String generatedBy  = body.getOrDefault("generatedBy", "system");

        AnalyticsReport report = reportService.generateReport(
                reportName, type, startDate, endDate, generatedBy);

        return ResponseEntity
                .created(URI.create("/api/reports/" + report.getReportId()))
                .body(report);
    }

    /**
     * GET /api/reports
     */
    @GetMapping
    public ResponseEntity<List<AnalyticsReport>> listReports() {
        return ResponseEntity.ok(reportService.listReports());
    }

    /**
     * GET /api/reports/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnalyticsReport> getReport(@PathVariable UUID id) {
        return reportService.getReportById(id)
                .map(report -> {
                    // Also poll Airflow for latest status
                    return ResponseEntity.ok(reportService.pollStatus(id));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/reports/{id}/download — returns a presigned S3 URL
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> downloadReport(@PathVariable UUID id) {
        try {
            String presignedUrl = reportService.downloadReport(id);
            return ResponseEntity.ok(Map.of("downloadUrl", presignedUrl));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
