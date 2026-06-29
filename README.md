# Cloud-Native Customer Analytics Platform

Analytics solution collecting customer interactions from multiple channels and transforming them into interactive dashboards. Business teams monitor customer engagement, predict sales trends, and generate executive reports on demand via automated Apache Airflow pipelines.

**Duration:** February 2024 – January 2025

## Technologies
Java 21 · Spring Boot 3.2 · Python · PostgreSQL · Apache Airflow · AWS Lambda · AWS S3 · Docker · Power BI · Prometheus

## Architecture

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Event Collection | Spring Boot REST API | Receive customer interaction events |
| Storage | PostgreSQL | Event and report metadata |
| Pipeline Orchestration | Apache Airflow | Scheduled ETL and report generation |
| Report Storage | AWS S3 | Compiled report files |
| Serverless Processing | AWS Lambda | On-demand aggregation functions |
| Visualization | Power BI Embedded | Executive dashboards |

## Features
- Multi-channel event collection (Web, Mobile, Email, SMS, Push)
- Customer segmentation: Champions, Loyal, At Risk, Churned
- Daily/Weekly/Monthly automated report generation via Airflow
- Presigned S3 URL download for generated reports
- Churn risk scoring and LTV computation
- Channel performance conversion rate analysis

## Setup
```bash
docker-compose up -d
# App: http://localhost:8080
# Airflow: http://localhost:8081 (admin/admin)
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| GET | /api/analytics/engagement | DAU, WAU, MAU metrics |
| GET | /api/analytics/sales-trends | Revenue by period |
| GET | /api/analytics/channel-performance | Channel conversion rates |
| POST | /api/reports/generate | Trigger report generation |
| GET | /api/dashboard | Full KPI summary |
