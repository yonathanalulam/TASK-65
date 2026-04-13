-- V11: Seed scheduled_jobs with all application-defined scheduled jobs (H2 compatible)

INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('reminder-generation', 'Generate practice due/overdue reminders for notebook entries', '0 */15 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('overdue-recalculation', 'Recalculate priority of unread overdue notifications', '0 */15 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('cache-cleanup', 'Hourly LRU cache cleanup for users over audio storage quota', '0 0 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('cache-expiry-sweep', 'Daily expiry sweep for audio cache segments past 30-day validity', '0 0 0 * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('session-cleanup', 'Abandon inactive cooking sessions and expire old abandoned ones', '0 */15 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('nonce-cleanup', 'Remove expired request replay nonces', '0 */10 * * * *', TRUE, 2);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('captcha-cleanup', 'Remove expired CAPTCHA challenges', '0 */10 * * * *', TRUE, 2);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('anomaly-evaluation', 'Evaluate error rates and job failure counts for anomaly alerts', '0 */10 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('capacity-report', 'Daily capacity report: users, sessions, cache, transactions, notifications', '0 0 2 * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('metrics-rollup', 'Aggregate request throughput, error counts, and latency metrics', '0 */10 * * * *', TRUE, 3);
INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('retention-cleanup', 'Purge auth logs >1yr, audit logs >2yr, job runs/metrics >180d', '0 0 3 * * *', TRUE, 3);
