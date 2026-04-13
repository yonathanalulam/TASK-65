-- V10: Observability tables (H2 compatible)

CREATE TABLE scheduled_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    cron_expression VARCHAR(50),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries INT NOT NULL DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE job_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    affected_rows INT NOT NULL DEFAULT 0,
    affected_files INT NOT NULL DEFAULT 0,
    error_summary CLOB,
    retry_count INT NOT NULL DEFAULT 0,
    trace_id VARCHAR(36),
    checkpoint_data CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (job_id) REFERENCES scheduled_jobs(id)
);

CREATE TABLE metric_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_name VARCHAR(100) NOT NULL,
    metric_value DECIMAL(18,4) NOT NULL,
    dimension_key VARCHAR(100),
    dimension_value VARCHAR(200),
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE anomaly_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_type VARCHAR(100) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    message CLOB NOT NULL,
    metric_name VARCHAR(100),
    threshold_value DECIMAL(18,4),
    actual_value DECIMAL(18,4),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    acknowledged_by VARCHAR(64),
    acknowledged_at TIMESTAMP NULL,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
