-- V9: Mock checkout tables (H2 compatible)

CREATE TABLE product_bundles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description CLOB,
    price DECIMAL(12,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mock_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    total_amount DECIMAL(12,2) NOT NULL,
    payment_reference_token VARCHAR(255),
    operator_user_id BIGINT NULL,
    initiated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    voided_at TIMESTAMP NULL,
    void_reason CLOB,
    voided_by VARCHAR(64),
    trace_id VARCHAR(36),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE mock_transaction_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    bundle_id BIGINT NOT NULL,
    bundle_name VARCHAR(200) NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    line_total DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (transaction_id) REFERENCES mock_transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (bundle_id) REFERENCES product_bundles(id)
);

CREATE TABLE mock_receipts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL UNIQUE,
    receipt_number VARCHAR(30) NOT NULL UNIQUE,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES mock_transactions(id)
);

CREATE TABLE reconciliation_exports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    business_date DATE NOT NULL,
    export_version INT NOT NULL DEFAULT 1,
    file_path VARCHAR(500) NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,
    transaction_count INT NOT NULL DEFAULT 0,
    total_completed_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    total_voided_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    generated_by VARCHAR(64) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (business_date, export_version)
);
