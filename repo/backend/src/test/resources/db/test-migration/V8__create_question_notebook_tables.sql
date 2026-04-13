-- V8: Questions, attempts, wrong-question notebook, notifications (H2 compatible)

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NULL,
    question_text CLOB NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_CHOICE',
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    canonical_answer CLOB NOT NULL,
    explanation CLOB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE question_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_question_id BIGINT NOT NULL,
    question_text CLOB NOT NULL,
    canonical_answer CLOB NOT NULL,
    explanation CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (original_question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE question_similarity_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id_a BIGINT NOT NULL,
    question_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (question_id_a, question_id_b),
    FOREIGN KEY (question_id_a) REFERENCES questions(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id_b) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE question_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    user_answer CLOB NOT NULL,
    classification VARCHAR(20) NOT NULL,
    score DECIMAL(5,2),
    flagged_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    drill_run_id BIGINT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE attempt_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL UNIQUE,
    classification VARCHAR(20) NOT NULL,
    details CLOB,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attempt_id) REFERENCES question_attempts(id) ON DELETE CASCADE
);

CREATE TABLE wrong_notebook_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fail_count INT NOT NULL DEFAULT 1,
    last_attempt_at TIMESTAMP NOT NULL,
    latest_note CLOB,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

CREATE TABLE wrong_notebook_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    label VARCHAR(50) NOT NULL UNIQUE,
    is_admin_seeded BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wrong_notebook_entry_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE (entry_id, tag_id),
    FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES wrong_notebook_tags(id) ON DELETE CASCADE
);

CREATE TABLE wrong_notebook_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    note_text CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE
);

CREATE TABLE wrong_notebook_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, entry_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE
);

CREATE TABLE drill_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    drill_type VARCHAR(30) NOT NULL,
    source_entry_id BIGINT NULL,
    source_question_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    total_questions INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message CLOB,
    reference_type VARCHAR(50),
    reference_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'GENERATED',
    priority INT NOT NULL DEFAULT 0,
    next_due_at TIMESTAMP NULL,
    suppression_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL,
    dismissed_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
