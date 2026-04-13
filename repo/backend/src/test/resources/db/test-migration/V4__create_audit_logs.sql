-- V4: Audit logs (H2 compatible)

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    user_id BIGINT NULL,
    username VARCHAR(64),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    details CLOB,
    trace_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    FOREIGN KEY (viewer_user_id) REFERENCES users(id),
    FOREIGN KEY (subject_user_id) REFERENCES users(id)
);
