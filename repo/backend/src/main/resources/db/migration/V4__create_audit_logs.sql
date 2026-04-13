-- V4: Audit logs and privacy access logs

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT NULL,
    username VARCHAR(64),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    details TEXT,
    trace_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_trace (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE privacy_access_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    viewer_user_id BIGINT NOT NULL,
    viewer_role VARCHAR(30) NOT NULL,
    subject_user_id BIGINT NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(100),
    reason_code VARCHAR(50) NOT NULL,
    trace_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_privacylog_viewer FOREIGN KEY (viewer_user_id) REFERENCES users(id),
    CONSTRAINT fk_privacylog_subject FOREIGN KEY (subject_user_id) REFERENCES users(id),
    INDEX idx_privacy_viewer (viewer_user_id),
    INDEX idx_privacy_subject (subject_user_id),
    INDEX idx_privacy_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
