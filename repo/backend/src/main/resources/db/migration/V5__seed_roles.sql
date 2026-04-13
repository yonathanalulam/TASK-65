-- V5: Seed default roles

INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard self-learner with access to own data only'),
    ('ROLE_PARENT_COACH', 'Read-only scoped access to assigned reviewable users'),
    ('ROLE_ADMIN', 'System administrator with full configuration and audit rights'),
    ('ROLE_SYSTEM_JOB', 'Non-human service role for background jobs only');
