-- V13: Parent/Coach assignment table (H2 compatible)

CREATE TABLE parent_coach_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coach_user_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    assigned_by VARCHAR(64) NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT uk_coach_student UNIQUE (coach_user_id, student_user_id),
    CONSTRAINT fk_pca_coach FOREIGN KEY (coach_user_id) REFERENCES users(id),
    CONSTRAINT fk_pca_student FOREIGN KEY (student_user_id) REFERENCES users(id)
);
