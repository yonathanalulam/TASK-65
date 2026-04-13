-- V7: Cooking session tables - sessions, steps, timers, step completions, tip bindings, tip cards

CREATE TABLE tip_cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    short_text VARCHAR(500),
    detailed_text TEXT,
    scope VARCHAR(50) NOT NULL DEFAULT 'GLOBAL',
    lesson_id BIGINT NULL,
    step_context VARCHAR(200),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tip_scope (scope),
    INDEX idx_tip_lesson (lesson_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tip_card_configurations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope VARCHAR(50) NOT NULL,
    scope_id BIGINT NULL,
    display_mode VARCHAR(20) NOT NULL DEFAULT 'SHORT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(64),
    UNIQUE KEY uk_tip_config_scope (scope, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE tip_card_audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tip_card_id BIGINT NULL,
    config_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    changed_by VARCHAR(64) NOT NULL,
    trace_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cooking_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    recipe_title VARCHAR(255) NOT NULL,
    lesson_id BIGINT NULL,
    workflow_version VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    total_steps INT NOT NULL DEFAULT 0,
    last_completed_step_order INT NOT NULL DEFAULT -1,
    started_at TIMESTAMP NULL,
    resumed_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    abandoned_at TIMESTAMP NULL,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_csession_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_csession_user_status (user_id, status),
    INDEX idx_csession_activity (last_activity_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cooking_session_steps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    expected_duration_seconds INT,
    has_timer BOOLEAN NOT NULL DEFAULT FALSE,
    timer_duration_seconds INT,
    reminder_text VARCHAR(500),
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP NULL,
    UNIQUE KEY uk_session_step_order (session_id, step_order),
    CONSTRAINT fk_cstep_session FOREIGN KEY (session_id) REFERENCES cooking_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cooking_session_timers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    step_id BIGINT NULL,
    label VARCHAR(200),
    timer_type VARCHAR(30) NOT NULL DEFAULT 'STEP',
    status VARCHAR(30) NOT NULL DEFAULT 'RUNNING',
    duration_seconds INT NOT NULL,
    started_at TIMESTAMP NOT NULL,
    target_end_at TIMESTAMP NOT NULL,
    paused_at TIMESTAMP NULL,
    elapsed_before_pause_seconds INT NOT NULL DEFAULT 0,
    acknowledged_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctimer_session FOREIGN KEY (session_id) REFERENCES cooking_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctimer_step FOREIGN KEY (step_id) REFERENCES cooking_session_steps(id) ON DELETE SET NULL,
    INDEX idx_ctimer_session_status (session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE step_completion_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    step_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stepevent_session FOREIGN KEY (session_id) REFERENCES cooking_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_stepevent_step FOREIGN KEY (step_id) REFERENCES cooking_session_steps(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE step_tip_bindings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    step_id BIGINT NOT NULL,
    tip_card_id BIGINT NOT NULL,
    display_mode VARCHAR(20) NOT NULL DEFAULT 'SHORT',
    UNIQUE KEY uk_step_tip (step_id, tip_card_id),
    CONSTRAINT fk_steptip_step FOREIGN KEY (step_id) REFERENCES cooking_session_steps(id) ON DELETE CASCADE,
    CONSTRAINT fk_steptip_tip FOREIGN KEY (tip_card_id) REFERENCES tip_cards(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
