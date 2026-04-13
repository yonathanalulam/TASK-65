-- V11: Seed scheduled_jobs with all application-defined scheduled jobs

INSERT INTO scheduled_jobs (job_name, description, cron_expression, enabled, max_retries) VALUES
    ('reminder-generation', 'Generate practice due/overdue reminders for notebook entries', '0 */15 * * * *', TRUE, 3),
    ('overdue-recalculation', 'Recalculate priority of unread overdue notifications', '0 */15 * * * *', TRUE, 3),
    ('cache-cleanup', 'Hourly LRU cache cleanup for users over audio storage quota', '0 0 * * * *', TRUE, 3),
    ('cache-expiry-sweep', 'Daily expiry sweep for audio cache segments past 30-day validity', '0 0 0 * * *', TRUE, 3),
    ('session-cleanup', 'Abandon inactive cooking sessions and expire old abandoned ones', '0 */15 * * * *', TRUE, 3),
    ('nonce-cleanup', 'Remove expired request replay nonces', '0 */10 * * * *', TRUE, 2),
    ('captcha-cleanup', 'Remove expired CAPTCHA challenges', '0 */10 * * * *', TRUE, 2),
    ('anomaly-evaluation', 'Evaluate error rates and job failure counts for anomaly alerts', '0 */10 * * * *', TRUE, 3),
    ('capacity-report', 'Daily capacity report: users, sessions, cache, transactions, notifications', '0 0 2 * * *', TRUE, 3),
    ('metrics-rollup', 'Aggregate request throughput, error counts, and latency metrics', '0 */10 * * * *', TRUE, 3),
    ('retention-cleanup', 'Purge auth logs >1yr, audit logs >2yr, job runs/metrics >180d', '0 0 3 * * *', TRUE, 3);
