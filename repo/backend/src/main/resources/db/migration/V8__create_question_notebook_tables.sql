-- V8: Questions, attempts, wrong-question notebook, notifications

CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lesson_id BIGINT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'SINGLE_CHOICE',
    difficulty VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    canonical_answer TEXT NOT NULL,
    explanation TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_question_lesson (lesson_id),
    INDEX idx_question_type (question_type),
    INDEX idx_question_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    original_question_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    canonical_answer TEXT NOT NULL,
    explanation TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_variant_question FOREIGN KEY (original_question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_variant_original (original_question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_similarity_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id_a BIGINT NOT NULL,
    question_id_b BIGINT NOT NULL,
    similarity_score DECIMAL(5,4) NOT NULL DEFAULT 1.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_similarity_pair (question_id_a, question_id_b),
    CONSTRAINT fk_sim_a FOREIGN KEY (question_id_a) REFERENCES questions(id) ON DELETE CASCADE,
    CONSTRAINT fk_sim_b FOREIGN KEY (question_id_b) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE question_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    variant_id BIGINT NULL,
    user_answer TEXT NOT NULL,
    classification VARCHAR(20) NOT NULL,
    score DECIMAL(5,2),
    flagged_by_user BOOLEAN NOT NULL DEFAULT FALSE,
    drill_run_id BIGINT NULL,
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attempt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_attempt_user (user_id),
    INDEX idx_attempt_question (question_id),
    INDEX idx_attempt_classification (classification)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attempt_evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL UNIQUE,
    classification VARCHAR(20) NOT NULL,
    details TEXT,
    evaluated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_attempt FOREIGN KEY (attempt_id) REFERENCES question_attempts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_notebook_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fail_count INT NOT NULL DEFAULT 1,
    last_attempt_at TIMESTAMP NOT NULL,
    latest_note TEXT,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_question_active (user_id, question_id, status),
    CONSTRAINT fk_notebook_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notebook_question FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    INDEX idx_notebook_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_notebook_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    label VARCHAR(50) NOT NULL UNIQUE,
    is_admin_seeded BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_notebook_entry_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_entry_tag (entry_id, tag_id),
    CONSTRAINT fk_entrytag_entry FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_entrytag_tag FOREIGN KEY (tag_id) REFERENCES wrong_notebook_tags(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_notebook_notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    note_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_note_entry FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE,
    INDEX idx_note_entry (entry_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE wrong_notebook_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entry_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_entry_fav (user_id, entry_id),
    CONSTRAINT fk_nbfav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_nbfav_entry FOREIGN KEY (entry_id) REFERENCES wrong_notebook_entries(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    CONSTRAINT fk_drill_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_drill_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
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
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user_status (user_id, status),
    INDEX idx_notif_suppression (suppression_key),
    INDEX idx_notif_due (next_due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
