# Project Clarification Questions

## Business Logic & Architecture Questions Log

This document records questions that arose from ambiguities in the original prompt and the solutions that were implemented in the codebase.

---

### 1. Authentication Mechanism

**Question:** What authentication mechanism should be used -- JWT tokens, Spring Security's built-in session management, or a custom session-based approach?
**My Understanding:** The prompt specifies "local username/password only" and "per-device session limits" but does not prescribe a specific session technology. Spring Security defaults to HTTP session-based auth, and JWTs are common in SPA architectures, but the requirement for per-device limits and signed requests suggests a custom approach.
**Solution:** Implement a fully custom session-based authentication system. A `SessionAuthenticationFilter` reads an `X-Session-Id` header from each request, validates the backing `AuthSession` database record (checking active status, absolute expiration of 12 hours, and idle timeout of 30 minutes), and populates the Spring `SecurityContext` with a `UserPrincipal`. Spring's built-in form login and HTTP Basic are disabled. Session creation only occurs through the explicit login endpoint (`POST /api/v1/auth/login`). Each session stores a unique HMAC-SHA256 signing key used for request signing.

---

### 2. User Registration Flow

**Question:** Should users be able to self-register, or is account creation restricted to administrators?
**My Understanding:** The prompt defines three roles (Regular User, Parent/Coach, Administrator) but does not specify whether there is a self-registration flow. The local/offline nature of the system and the mention of "Administrator (local staff for setup)" implies controlled enrollment.
**Solution:** User creation is admin-only. There is no self-registration endpoint. Administrators create users through `POST /api/v1/admin/users` with assigned roles. A default admin user is seeded during the V5 Flyway migration, with the password sourced from the `APP_BOOTSTRAP_ADMIN_PASSWORD` environment variable and a `force_password_change` flag set to `true`.

---

### 3. CAPTCHA Implementation Strategy

**Question:** What kind of CAPTCHA should be used given the system operates offline without internet connectivity?
**My Understanding:** The prompt says "local CAPTCHA challenge after repeated failures" but does not specify the implementation. Standard solutions like Google reCAPTCHA require internet. The offline requirement rules out all third-party CAPTCHA services.
**Solution:** Implement a fully local CAPTCHA generator using Java2D (`CaptchaGenerator.java`). It produces 6-character alphanumeric challenges with moderate visual distortion, rendered as Base64-encoded PNG images. Challenges are stored in a `captcha_challenges` table with BCrypt-hashed answers and a 5-minute expiry. CAPTCHA is triggered after 3 failed login attempts within a 10-minute window.

---

### 4. Request Signing Implementation

**Question:** How should "signed requests with nonce and 5-minute anti-replay windows" be implemented across the decoupled SPA frontend and REST backend?
**My Understanding:** The prompt mandates signed requests but does not specify which party computes signatures, what the canonical signing payload looks like, or how signing keys are distributed. In a decoupled SPA architecture, the frontend must have access to the signing key.
**Solution:** Implement client-side HMAC-SHA256 request signing using the Web Crypto API (`crypto.subtle.sign`). On login, the server generates a per-session signing key and returns it to the frontend in the `LoginResponse`. For every state-changing request (POST, PUT, PATCH, DELETE), the frontend constructs a canonical payload (`METHOD\nPATH\nTIMESTAMP\nNONCE`), signs it, and sends headers `X-Timestamp`, `X-Nonce`, and `X-Signature`. The backend `RequestSignatureFilter` validates the signature, checks the timestamp is within a 5-minute window, and ensures the nonce has not been previously used (via both Caffeine cache and MySQL `nonce_entries` table). Read-only methods (GET, HEAD, OPTIONS) are exempt.

---

### 5. Rate Limiting Architecture

**Question:** Should rate limiting be implemented with a distributed store (e.g., Redis) or in-memory, given the "same machine or local network" deployment model?
**My Understanding:** The prompt specifies "60 requests/minute/user" rate limiting but does not specify the backing store. The system is explicitly single-machine/LAN, so distributed coordination is unnecessary.
**Solution:** Use in-memory rate limiting with Bucket4j backed by Caffeine cache. Three tiers are defined: authenticated users (60 req/min), unauthenticated users (20 req/min), and admin users (30 req/min), with a burst allowance of 10 requests per 2-second window. Rate limit state resets on server restart. A `rate_limit_tracking` database table is reserved for future multi-instance support but is not actively used. The `RateLimitFilter` returns HTTP 429 with a `Retry-After` header when limits are exceeded.

---

### 6. Audio File Storage and Delivery

**Question:** How should audio files be stored and served -- via a streaming server, CDN, or direct filesystem access?
**My Understanding:** The prompt mentions "locally available cooking lesson audio" and "download segments for offline playback" but does not specify the storage backend. The offline/local deployment model rules out CDN or cloud storage.
**Solution:** Audio files are pre-provisioned at local filesystem paths referenced in the `audio_segments.file_path` column. The `AudioCacheService` copies from the source path to a user-specific cache directory (`./cache/{userId}/{segmentId}/`). When source files do not exist (e.g., during demo/seed data), placeholder files with metadata headers are created. Cached files are served via `GET /api/v1/audio/cache/{manifestId}/stream` as `application/octet-stream`. No streaming server or CDN is involved.

---

### 7. Cache Quota Enforcement Mechanism

**Question:** How exactly should the 2.0 GB offline cache limit be enforced -- at the filesystem level, application level, or both?
**My Understanding:** The prompt states "default 2.0 GB limit" with "LRU cleanup and user-visible storage metrics" but does not specify the enforcement mechanism.
**Solution:** Enforce the quota at the application level only. Before each download, the `AudioCacheService` sums `file_size_bytes` from the user's `audio_cache_manifests` records. If adding the new segment would exceed the 2 GB limit (`app.audio.cache-quota-bytes: 2147483648`), the download is rejected. LRU eviction is tracked via a `last_accessed_at` timestamp updated on each playback access. The `StorageMeterResponse` DTO exposes `usedBytes`, `totalQuotaBytes`, `percentUsed`, and `reclaimableBytes` to the frontend for the visible storage meter.

---

### 8. Degraded Mode Detection and Behavior

**Question:** Should degraded/offline mode detection happen on the backend, frontend, or both? What specific behaviors change in degraded mode?
**My Understanding:** The prompt says "degraded mode disables downloads and continues playback/drills when the network is poor" but does not specify the detection mechanism or which layer is responsible.
**Solution:** Degraded mode is implemented entirely in the frontend via the `degradedMode` Pinia store. The Axios response interceptor tracks consecutive network errors and 5xx responses. After 3 consecutive failures, degraded mode activates. On the next successful response, it clears. While degraded: audio downloads are disabled (button greyed out), cached playback remains available, study/drill flows continue, and a non-blocking banner appears on the Audio Library page. The backend has no degraded mode concept.

---

### 9. Cooking Session Step Source

**Question:** Where do cooking session steps/recipes come from -- a content management system, predefined templates, or user input?
**My Understanding:** The prompt says users start "a guided workflow" with steps and timers but does not specify whether recipe content is pre-authored or user-defined. There is no mention of a recipe authoring system.
**Solution:** Cooking session steps are provided by the client at session creation time via the `StartSessionRequest` DTO, which includes a `recipeTitle`, optional `lessonId`, and a list of `StepInput` objects (each with title, description, expectedDurationSeconds, hasTimer, timerDurationSeconds, and reminderText). Steps are snapshotted immutably into the `cooking_session_steps` table at session creation. There is no separate recipe content management system.

---

### 10. Timer Reconstruction After Restart

**Question:** How should timer state be reconstructed after "the app is closed or the device reboots" so the session "resumes to the last completed step with elapsed timer reconstruction"?
**My Understanding:** The prompt explicitly requires session resume with timer reconstruction but does not specify the precision or mechanism. Browser-side timers are lost on close, so persistent state is needed.
**Solution:** Wall-clock timestamps are stored in the database for each timer: `started_at`, `target_end_at`, `paused_at`, and `elapsed_before_pause_seconds`. On session resume (`POST /api/v1/cooking/sessions/{id}/resume`), the backend computes elapsed time from wall-clock differences. The `TimerResponse` DTO returns a calculated `remainingSeconds` field. Precision is limited to database timestamp resolution (millisecond). Sub-second timer accuracy is not guaranteed across restarts. The frontend polls for timer state updates; WebSocket-based live updates are deferred.

---

### 11. Question Evaluation Method

**Question:** Should answer evaluation use AI/NLP-based grading or deterministic string matching?
**My Understanding:** The prompt says users "answer practice questions tied to lessons" with wrong answers saved to a notebook, but does not specify the evaluation algorithm. AI-based grading would add complexity and external dependencies.
**Solution:** Answer evaluation uses deterministic string matching in the `QuestionService`: exact match of the user's answer against `canonical_answer` = CORRECT, substring match = PARTIAL, otherwise WRONG. The `AttemptEvaluation` entity stores the classification and evaluation details. Any WRONG, PARTIAL, or user-flagged attempt is automatically saved into the Wrong-Question Notebook. AI-based grading is explicitly out of scope.

---

### 12. Wrong-Question Notebook Entry Uniqueness

**Question:** Can a user have multiple notebook entries for the same question, or should entries be deduplicated?
**My Understanding:** The prompt describes the notebook as saving "wrong, partially wrong, or user-flagged" questions but does not specify whether repeated failures on the same question create new entries or update existing ones.
**Solution:** A unique constraint is enforced on `(user_id, question_id, status)` in the `wrong_notebook_entries` table. A user can have one ACTIVE entry and separately one ARCHIVED entry for the same question. Subsequent wrong attempts on a question that already has an ACTIVE entry increment the `fail_count` and update `last_attempt_at` rather than creating a duplicate. The entry tracks cumulative failure patterns.

---

### 13. Practice Reminder Scheduling

**Question:** How should "in-app reminders for due practice and overdue items" be generated and delivered?
**My Understanding:** The prompt requires reminders for due and overdue practice but does not specify the scheduling mechanism, frequency, or delivery method (push notifications, polling, etc.).
**Solution:** A Spring `@Scheduled` job runs every 15 minutes to generate reminder notifications. It checks for notebook entries that are due for review and creates `Notification` records with types `PRACTICE_DUE`, `PRACTICE_OVERDUE`, or `CACHE_EXPIRING`. Notifications use a `suppressionKey` to prevent duplicates. The frontend polls `GET /api/v1/notifications/unread-count` to display a badge. Reminders may be delayed up to 15 minutes from their theoretical due time.

---

### 14. Mock Checkout Implementation

**Question:** What does "mock checkout flow" mean concretely -- should it simulate a real payment gateway, use test credit card numbers, or simply record a purchase intent?
**My Understanding:** The prompt says "local mock checkout flow for paid content bundles with receipts" and "without requiring internet connectivity." This rules out any real payment gateway integration.
**Solution:** The mock checkout flow generates a locally-created UUID as the payment reference token (encrypted with AES-256). No real payment processing, credit card validation, tax calculation, shipping, or refund logic exists. The flow is: `POST /api/v1/checkout/initiate` (creates an INITIATED transaction with line items) then `POST /api/v1/checkout/complete/{transactionId}` (transitions to COMPLETED, generates a receipt, and grants `AudioBundleEntitlement` records). Administrators can void transactions via `POST /api/v1/admin/checkout/transactions/{id}/void`.

---

### 15. Receipt Number Generation

**Question:** What format and sequence strategy should receipt numbers follow?
**My Understanding:** The prompt mentions "receipts" but does not specify a numbering format or uniqueness strategy.
**Solution:** Receipt numbers follow the format `RCPT-YYYYMMDD-NNNNNN`, where the sequence number is generated by counting existing receipts for the same date prefix. A database unique constraint on `receipt_number` ensures no duplicates under concurrent scenarios. Receipts are stored in the `mock_receipts` table and returned in the `TransactionResponse` DTO.

---

### 16. Reconciliation Export Format

**Question:** What format should the "daily reconciliation export for bookkeeping" use -- CSV, JSON, XML, or a standard accounting format?
**My Understanding:** The prompt mentions reconciliation exports but does not specify the file format or content structure.
**Solution:** CSV format with a SHA-256 checksum for integrity verification. Exports are written to a configurable local directory (default: `./exports/reconciliation/`) with naming convention `reconciliation_YYYYMMDD_vN.csv`. Fields include: transaction ID, date, amount, status, and line items. Each export is recorded in the `reconciliation_exports` table with `transactionCount`, `totalCompletedAmount`, `totalVoidedAmount`, `fileChecksum`, and `generatedBy`. The reconciliation cron is disabled by default and enabled via the `app.checkout.reconciliation-cron` configuration property.

---

### 17. Observability Metrics Source

**Question:** Where should request throughput, latency, and error rate metrics come from -- a dedicated metrics agent (Micrometer/Prometheus) or derived from application data?
**My Understanding:** The prompt requires "success/latency/throughput monitoring" and "anomaly alerts based on threshold rules" but does not specify the metrics collection infrastructure.
**Solution:** In the current implementation, metrics are approximated from audit log and login attempt counts stored in the database. `MetricSnapshot` records capture time-series data with dimensional attributes (`metricName`, `metricValue`, `dimensionKey`, `dimensionValue`, `windowStart`, `windowEnd`). The `AdminDashboardController` exposes `/metrics`, `/kpis` (p50/p95 latency), and `/capacity` endpoints. A proper metrics collection agent (e.g., Micrometer + Prometheus) is recommended for production but not implemented.

---

### 18. Anomaly Detection Configuration

**Question:** How should anomaly alert thresholds be configured -- hardcoded, database-driven, or via a configuration UI?
**My Understanding:** The prompt gives an example of "error rate >2% over 10 minutes" but does not specify whether thresholds are configurable or how they are managed.
**Solution:** Default anomaly threshold is hardcoded in the `AnomalyEvaluationJob`: error rate >2% over a 10-minute window. Alerts are created with severity levels (INFO, WARNING, CRITICAL) and follow a lifecycle: OPEN -> ACKNOWLEDGED -> RESOLVED. Administrators manage alerts through `POST /api/v1/admin/dashboard/alerts/{id}/acknowledge` and `POST /api/v1/admin/dashboard/alerts/{id}/resolve`. A configuration UI for custom thresholds is deferred to a future phase.

---

### 19. Parent/Coach Access Scoping

**Question:** How should Parent/Coach users' access to student data be scoped -- can they see all users, or only explicitly assigned students?
**My Understanding:** The prompt says Parent/Coach "can review progress on the same device" and mentions "privacy access logs" but does not specify the assignment mechanism.
**Solution:** Parent/Coach access requires an explicit assignment record in the `parent_coach_assignments` table. Assignments are created by administrators via `POST /api/v1/admin/review/assignments`. Without an assignment, Parent/Coach users receive HTTP 403 on all student review endpoints. Every review action (viewing notebook, attempts, or cooking history) requires a `reason` query parameter and creates a `privacy_access_logs` entry recording the viewer, subject, resource type, and reason.

---

### 20. Frontend Architecture Approach

**Question:** Should the Vue.js frontend be a full single-page application (SPA) or use server-side rendering (SSR) / hybrid approach?
**My Understanding:** The prompt says "Vue.js web UI" and describes rich interactivity (concurrent timers, step completion, storage meter) but does not specify the rendering strategy.
**Solution:** Implement a full client-side SPA using Vue 3 with the Composition API and TypeScript. Vite serves as the build tool with a dev server proxy forwarding `/api` requests to the Spring Boot backend at `localhost:8080`. Vue Router provides client-side routing with navigation guards for authentication and role-based access. Pinia manages global state (auth credentials, degraded mode). The frontend is fully decoupled from the backend and communicates exclusively through the REST API.

---

### 21. Concurrent Timer Limit

**Question:** What is the maximum number of concurrent timers allowed per cooking session, and how should the limit be enforced?
**My Understanding:** The prompt says "multiple concurrent timers (for example, up to 6 active timers)" using parenthetical phrasing that could be interpreted as a suggestion or a hard limit.
**Solution:** Treat the 6-timer limit as a hard maximum enforced at the application level. The `CookingSessionTimer` entity and timer creation logic in `CookingSessionService` enforce that no more than 6 timers can be in a non-terminal state (RUNNING or PAUSED) per session. The frontend polls for timer state updates; each `TimerResponse` includes a computed `remainingSeconds` for display countdown.

---

### 22. Password History and Reuse Prevention

**Question:** How should password reuse be prevented beyond the stated "minimum 12 characters, complexity rules"?
**My Understanding:** The prompt specifies password complexity and lockout rules but does not mention password history or reuse prevention, which is a common security requirement.
**Solution:** Track the last 5 password hashes per user in the `password_history` table. The `PasswordHistoryService` checks new passwords against all stored hashes using BCrypt comparison before allowing a change. The `PasswordPolicyValidator` enforces: minimum 12 characters, at least one uppercase letter, one lowercase letter, one digit, and one special character. The `PasswordChangeRequest` DTO requires both `currentPassword` and `newPassword`.

---

### 23. CSRF Protection for SPA

**Question:** How should CSRF protection work with a decoupled SPA that makes API calls via Axios rather than submitting HTML forms?
**My Understanding:** The prompt mandates "CSRF protection" but standard CSRF token mechanisms (hidden form fields) do not apply to SPA architectures making XHR/fetch requests.
**Solution:** Use Spring Security's cookie-based CSRF token with `CookieCsrfTokenRepository` (HttpOnly disabled so JavaScript can read it). A dedicated `GET /api/v1/auth/csrf-token` endpoint is CSRF-exempt to allow the Vue SPA to obtain its initial token. The Axios request interceptor reads the `XSRF-TOKEN` cookie and sends it back as the `X-XSRF-TOKEN` header on every request. The `initCsrf()` function is called on application mount.

---

### 24. Database Migration Strategy

**Question:** How should the database schema evolve across development phases -- manual SQL scripts, ORM auto-generation, or a migration framework?
**My Understanding:** The prompt does not specify a database migration strategy. The schema spans many tables across multiple feature domains.
**Solution:** Use Flyway for versioned database migrations across 13 migration files (V1 through V13). Each migration is a SQL script in `src/main/resources/db/migration/`. Hibernate's `ddl-auto` is set to `validate` (not `create` or `update`), ensuring the schema is solely controlled by Flyway. Migrations cover schema creation (V1-V4, V6-V10, V13), data seeding (V5, V11, V12), and are applied automatically on application startup.

---

### 25. Default Admin Bootstrapping

**Question:** How should the first administrator account be created when the system is deployed for the first time?
**My Understanding:** The prompt mentions Administrator role for "setup and audits" but does not describe initial bootstrapping. Without an admin, no other users can be created (since registration is admin-only).
**Solution:** The V5 Flyway migration seeds a default admin user (`admin`) whose password is sourced from the `APP_BOOTSTRAP_ADMIN_PASSWORD` environment variable. If the variable is not set, no admin user is created and an error is logged. The admin account has `force_password_change = true`, requiring an immediate password change on first login. The password is never logged or hardcoded in the migration script.

---

### 26. Device Fingerprinting Approach

**Question:** How should "per-device session limits" be implemented -- what constitutes a "device" in a browser-based web application?
**My Understanding:** The prompt mentions "per-device session limits" but browsers do not expose hardware identifiers. A web-based system needs an alternative fingerprinting strategy.
**Solution:** Use a simple client-generated fingerprint combining User-Agent string, screen dimensions, and timezone, sent as a custom `X-Device-Fingerprint` header. The `DeviceRegistration` entity stores these fingerprints with `first_seen_at` and `last_seen_at` timestamps. Session limits are enforced per device (max 2 active sessions) and per user (max 3 active sessions). No advanced browser fingerprinting library is used.

---

### 27. Nonce Storage and Cleanup

**Question:** How should anti-replay nonces be stored for fast lookup while maintaining durability across restarts?
**My Understanding:** The prompt requires "5-minute anti-replay windows" using nonces but does not specify the storage mechanism. Pure in-memory storage loses nonces on restart; pure database lookups are slow for every request.
**Solution:** Implement dual-layer nonce storage: Caffeine in-memory cache as the primary fast-lookup layer, and MySQL `nonce_entries` table as the durable fallback and audit trail. Nonces expire after the signature validity window plus a 10-minute buffer. A scheduled cleanup job runs every 10 minutes to purge expired entries from both layers. Each nonce is bound to a specific session ID, preventing cross-session replay.

---

### 28. Tip Card Configuration Model

**Question:** How should the Administrator's ability to "enable/disable, short vs. detailed mode" for tip cards be modeled -- per tip, per lesson, or globally?
**My Understanding:** The prompt says tip cards "appear contextually per step and can be configured by the Administrator" but does not specify the granularity of configuration.
**Solution:** Implement a multi-scope configuration model. The `TipCard` entity has global properties (`enabled`, `shortText`, `detailedText`). The `TipCardConfiguration` entity provides scope-based display mode overrides with a unique constraint on `(scope, scope_id)`, where scope can be GLOBAL, LESSON, or STEP. Display modes are DISABLED, SHORT, or DETAILED. The `StepTipBinding` entity associates tips with specific cooking steps. Administrators manage configurations through `POST /api/v1/admin/tips/{id}/configure` and `POST /api/v1/admin/tips/{id}/toggle`.

---

### 29. Drill Types and Discovery

**Question:** How should "retry, similar-question drills, or variant drills" discover related questions?
**My Understanding:** The prompt describes three drill types launched from the notebook but does not specify how "similar" or "variant" questions are identified.
**Solution:** Implement three distinct discovery mechanisms in the `QuestionService`: **Retry** drills re-present the exact same question. **Similar** drills use a `question_similarity_links` table that stores a similarity graph between questions with a `similarity_score` (DECIMAL 0-1). **Variant** drills use the `question_variants` table that stores alternative forms of the same question with their own `question_text` and `canonical_answer`. Drill progress is tracked in the `drill_runs` table with type (RETRY, SIMILAR, VARIANT), status (IN_PROGRESS, COMPLETED), and score tracking.

---

### 30. Notification Types and Lifecycle

**Question:** What specific notification types should the system generate, and what lifecycle states should they have?
**My Understanding:** The prompt says "clear in-app reminders for due practice and overdue items" but does not specify the full notification taxonomy or state machine.
**Solution:** Three notification types are implemented: `PRACTICE_DUE` (upcoming review), `PRACTICE_OVERDUE` (past-due review), and `CACHE_EXPIRING` (audio cache about to expire). Notifications follow a lifecycle: GENERATED -> DELIVERED -> READ -> DISMISSED, with additional states EXPIRED and SUPPRESSED. Suppression keys prevent duplicate notifications. Priority levels allow UI ordering. The frontend `NotificationsView` displays notifications with type-specific styling and status-appropriate actions (mark read, dismiss).

---

### 31. Account Status Model

**Question:** What lifecycle states should user accounts support beyond simple active/inactive?
**My Understanding:** The prompt mentions locked accounts (after failed logins) but does not define a complete account lifecycle.
**Solution:** Five account statuses are implemented as the `AccountStatus` enum: `PENDING_SETUP` (newly created, requires password change), `ACTIVE` (fully operational), `LOCKED` (auto-locked after 5 failed login attempts, auto-unlocks after 15 minutes), `DISABLED` (admin-disabled, requires admin re-enablement), and `DELETED_SOFT` (soft-deleted, data retained). Status transitions are enforced in the `AuthService` and `UserController`.

---

### 32. Idempotency for State-Changing Requests

**Question:** How should duplicate form submissions or network retries be handled for state-changing operations?
**My Understanding:** The prompt does not mention idempotency, but in a system with signed requests and potential network issues (degraded mode), duplicate submissions are a real concern.
**Solution:** The Axios request interceptor generates a unique `X-Idempotency-Key` (UUID) for every POST, PUT, PATCH, and DELETE request. Idempotency key responses are cached in memory (Caffeine) for 24 hours. If the same idempotency key is received again, the cached response is returned without re-executing the operation. This prevents duplicate transactions, duplicate notebook entries, and duplicate timer creation.

---

### 33. Timezone Handling

**Question:** Should the system use a server-configured timezone or UTC for all stored timestamps?
**My Understanding:** The prompt does not specify timezone handling. The system supports local deployment across different regions, and consistent timestamp storage is critical for timer reconstruction and reconciliation exports.
**Solution:** Store all timestamps in UTC in the database. The frontend renders timestamps in the browser's local timezone using JavaScript's `Intl.DateTimeFormat`. A server-side configurable timezone override is deferred to a future phase. This ensures consistent timer calculations, correct reconciliation dates, and predictable audit log ordering regardless of deployment location.

---

### 34. Audio Content Types

**Question:** What audio file formats should the system support?
**My Understanding:** The prompt mentions "cooking lesson audio" but does not specify supported formats.
**Solution:** Four audio formats are supported, configured via `app.audio.allowed-audio-types`: MP3, AAC, M4A, and OGG. Maximum segment size is 250 MB (`app.audio.max-segment-size-bytes: 262144000`). Audio assets are divided into segments with checksums (SHA-256) for integrity verification. The cache streaming endpoint serves files as `application/octet-stream`.

---

### 35. Scheduled Job Infrastructure

**Question:** How should background jobs (reminders, cleanup, reconciliation, anomaly evaluation) be managed and monitored?
**My Understanding:** The prompt describes several periodic operations (reminders, reconciliation exports, anomaly alerts) but does not specify a job scheduling framework or monitoring approach.
**Solution:** Use Spring's `@Scheduled` annotation for periodic job execution. A `scheduled_jobs` registry table (seeded in V11 migration) tracks 6 default jobs: reminder generation, nonce cleanup, cache expiry, retention cleanup, reconciliation export, and anomaly evaluation. Each execution is recorded in the `job_runs` table with status (QUEUED, RUNNING, SUCCEEDED, FAILED, PARTIAL_SUCCESS, RETRY_QUEUED, TERMINAL_FAILED, CANCELLED), affected rows/files, error summaries, and checkpoint data for resumable jobs. The admin dashboard exposes job status and run history.

---

### 36. Privacy Access Logging Granularity

**Question:** What level of detail should "privacy access logs" capture when a Parent/Coach or Administrator views student data?
**My Understanding:** The prompt says "privacy access logs record when a Parent/Coach or Administrator views a user's notebook or history" but does not specify the log schema or what counts as an access event.
**Solution:** Every review endpoint call creates a `PrivacyAccessLog` entry recording: `viewerUserId`, `viewerRole`, `subjectUserId`, `resourceType` (e.g., "NOTEBOOK", "ATTEMPTS", "COOKING_HISTORY"), `resourceId`, `reasonCode` (required query parameter on all review endpoints), and `traceId` for correlation. Administrators can query access logs via `GET /api/v1/admin/privacy/access-logs` with optional `subjectUserId` filter. This provides a complete audit trail of who accessed what student data, when, and why.
