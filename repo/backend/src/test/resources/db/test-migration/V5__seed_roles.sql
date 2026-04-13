-- V5: Seed default roles (H2 compatible)

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard self-learner with access to own data only');
INSERT INTO roles (name, description) VALUES
    ('ROLE_PARENT_COACH', 'Read-only scoped access to assigned reviewable users');
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'System administrator with full configuration and audit rights');
INSERT INTO roles (name, description) VALUES
    ('ROLE_SYSTEM_JOB', 'Non-human service role for background jobs only');
