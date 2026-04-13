-- V3: Security tables (H2 compatible)

CREATE TABLE login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    device_fingerprint VARCHAR(255),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    trace_id VARCHAR(36)
);

CREATE TABLE mfa_secrets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    encrypted_secret CLOB NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_codes CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE nonce_registry (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nonce VARCHAR(36) NOT NULL UNIQUE,
    session_id VARCHAR(128) NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

CREATE TABLE captcha_challenges (
    id VARCHAR(36) PRIMARY KEY,
    answer_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    ip_address VARCHAR(45)
);
