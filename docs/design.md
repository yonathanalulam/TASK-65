# Design Document

## Culinary Study & Cooking Coach Workstation

### System Overview

The Culinary Study & Cooking Coach Workstation is an offline-first, locally deployed application for home cooks and self-learners. It combines lesson audio playback, guided cooking sessions with concurrent timers, practice question drills with spaced-repetition-style wrong-question tracking, and a mock checkout system for paid content bundles. The system runs entirely on a single machine or local network with no internet dependency at runtime.

---

## 1. Architecture

### 1.1 High-Level Architecture

The system follows a **decoupled client-server architecture**:

```
+------------------+        REST/JSON         +-------------------+        JDBC         +----------+
|   Vue.js SPA     | <-------- /api/v1 ------> |  Spring Boot API  | <------------------> |  MySQL   |
|  (Vite, :5173)   |                           |     (:8080)       |                      |  (:3306) |
+------------------+                           +-------------------+                      +----------+
       |                                              |
  sessionStorage                               Local Filesystem
  (signingKey,                                 (audio files,
   sessionId)                                   cache, exports)
```

- **Frontend:** Vue 3 SPA served by Vite dev server (port 5173) or built as static assets
- **Backend:** Spring Boot 3.5.13 REST API (port 8080)
- **Database:** MySQL 8.0 on localhost or LAN (port 3306)
- **Communication:** JSON over HTTP, proxied through Vite in development (`/api` -> `localhost:8080`)

### 1.2 Design Principles

1. **Offline-first:** All data persisted locally in MySQL. No cloud dependencies at runtime.
2. **Decoupled layers:** Frontend and backend communicate exclusively through versioned REST APIs (`/api/v1`).
3. **Security in depth:** Six security layers (session auth, request signing, rate limiting, CSRF, CAPTCHA, MFA).
4. **Flyway-managed schema:** Database state is version-controlled through 13 SQL migrations. Hibernate validates but never modifies the schema (`ddl-auto: validate`).
5. **Immutable session snapshots:** Cooking session steps are snapshotted at creation and never modified.
6. **Wall-clock timer persistence:** Timer state uses absolute timestamps for reconstruction after restarts.

---

## 2. Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend Framework | Vue.js (Composition API) | 3.5.13 |
| Frontend Language | TypeScript | 5.6 |
| Frontend Build | Vite | 5.4.14 |
| State Management | Pinia | 2.3.0 |
| HTTP Client | Axios | 1.7.9 |
| Frontend Testing | Vitest + Happy DOM | 1.6.0 |
| Backend Framework | Spring Boot | 3.5.13 |
| Backend Language | Java | 23 |
| ORM | Spring Data JPA / Hibernate | - |
| Database | MySQL | 8.0 |
| Migrations | Flyway | - |
| Caching | Caffeine | - |
| Rate Limiting | Bucket4j | - |
| MFA | Dev Samstevens TOTP + ZXing QR | - |
| Test DB | H2 (MySQL compatibility mode) | - |
| Build Tool | Maven | - |
| Container | Docker Compose (MySQL + Adminer) | - |

---

## 3. Roles and Access Control

### 3.1 Role Definitions

| Role | Scope | Capabilities |
|------|-------|-------------|
| `ROLE_USER` | Own data only | Browse audio, manage playlists/favorites, run cooking sessions, answer questions, manage notebook, checkout bundles, view notifications |
| `ROLE_PARENT_COACH` | Assigned students | All ROLE_USER capabilities plus read-only review of assigned students' notebooks, attempts, and cooking history (requires explicit assignment + reason) |
| `ROLE_ADMIN` | System-wide | All capabilities plus user management, tip card configuration, reconciliation exports, observability dashboard, privacy/audit log access, coach-student assignment management |

### 3.2 Access Control Enforcement

- **Endpoint level:** `@PreAuthorize` annotations on controller classes and methods
- **Resource level:** Service methods verify the authenticated user owns the requested resource (session, notebook entry, transaction, etc.)
- **Admin paths:** All `/api/v1/admin/**` routes require `ROLE_ADMIN`
- **Review paths:** `/api/v1/review/**` routes require `ROLE_PARENT_COACH` or `ROLE_ADMIN` plus a valid assignment record
- **Privilege escalation:** Admin-only actions (user creation, void transaction, configuration changes) are protected by both role checks and audit logging

---

## 4. Security Architecture

### 4.1 Authentication Flow

```
Client                           Server
  |                                |
  |-- POST /auth/login ---------->|  1. Validate CAPTCHA (if required)
  |                                |  2. Look up user (case-insensitive)
  |                                |  3. Check account status (must be ACTIVE)
  |                                |  4. Check lockout (auto-unlock after 15 min)
  |                                |  5. Verify BCrypt password
  |                                |  6. If MFA enabled: return mfaToken
  |<-- {mfaRequired, mfaToken} ---|
  |                                |
  |-- POST /auth/mfa-verify ----->|  7. Validate TOTP code or recovery code
  |                                |  8. Register/update device
  |                                |  9. Enforce session limits (evict oldest)
  |                                | 10. Generate HMAC signing key
  |                                | 11. Create AuthSession record
  |<-- {sessionId, signingKey} ---|
  |                                |
  |   [store in sessionStorage]    |
```

### 4.2 Request Security Pipeline

The Spring Security filter chain processes requests in order:

```
Request
  |
  v
TraceIdFilter          -- Assigns X-Trace-Id UUID to every request
  |
  v
SessionAuthFilter      -- Reads X-Session-Id, validates session, populates SecurityContext
  |
  v
RequestSignatureFilter -- Validates HMAC-SHA256 on POST/PUT/PATCH/DELETE
  |                       (exempt: login, csrf-token, mfa-verify, captcha)
  v
RateLimitFilter        -- Bucket4j rate limiting (60/20/30 req/min by role)
  |
  v
CaptchaEnforcement     -- Escalates to CAPTCHA after repeated failures
  |
  v
Controller             -- Business logic execution
```

### 4.3 Request Signing

State-changing requests (POST, PUT, PATCH, DELETE) must include:

| Header | Value |
|--------|-------|
| `X-Session-Id` | Session identifier |
| `X-Timestamp` | ISO-8601 timestamp (must be within +-5 min) |
| `X-Nonce` | UUID (single-use per session) |
| `X-Signature` | HMAC-SHA256 of `METHOD\nPATH\nTIMESTAMP\nNONCE` |
| `X-Idempotency-Key` | UUID for duplicate suppression |

The signing key is the per-session key returned at login. The frontend computes signatures using the Web Crypto API (`crypto.subtle.sign`).

### 4.4 Password Security

- **Hashing:** BCrypt with cost factor 12
- **Minimum length:** 12 characters
- **Complexity:** Uppercase + lowercase + digit + special character
- **History:** Last 5 passwords tracked and checked for reuse
- **Lockout:** Account locked for 15 minutes after 5 failed attempts within a 30-minute window
- **Force change:** New accounts require password change on first login

### 4.5 Session Management

- **Absolute lifetime:** 12 hours
- **Idle timeout:** 30 minutes (tracked via `lastAccessedAt`)
- **Per-user limit:** 3 concurrent sessions (oldest evicted)
- **Per-device limit:** 2 concurrent sessions
- **Cleanup:** Scheduled task runs every 5 minutes to expire idle/absolute sessions

---

## 5. Data Model

### 5.1 Schema Overview

The database schema spans 13 Flyway migrations organized by feature domain:

| Migration | Domain | Key Tables |
|-----------|--------|------------|
| V1 | Identity | `users`, `roles`, `user_roles`, `password_history` |
| V2 | Sessions | `device_registrations`, `auth_sessions` |
| V3 | Security | `login_attempts`, `mfa_secrets`, `nonce_entries`, `captcha_challenges` |
| V4 | Audit | `audit_logs`, `privacy_access_logs` |
| V5 | Seed | Roles + default admin user |
| V6 | Audio | `audio_assets`, `audio_segments`, `audio_playlists`, `audio_playlist_items`, `audio_favorites`, `audio_cache_manifests`, `audio_bundle_entitlements` |
| V7 | Cooking | `cooking_sessions`, `cooking_session_steps`, `cooking_session_timers`, `step_completion_events`, `tip_cards`, `tip_card_configurations`, `step_tip_bindings`, `tip_card_audit_logs` |
| V8 | Study | `questions`, `question_variants`, `question_similarity_links`, `question_attempts`, `attempt_evaluations`, `wrong_notebook_entries`, `wrong_notebook_tags`, `wrong_notebook_entry_tags`, `wrong_notebook_notes`, `wrong_notebook_favorites`, `drill_runs`, `notifications` |
| V9 | Checkout | `product_bundles`, `mock_transactions`, `mock_transaction_items`, `mock_receipts`, `reconciliation_exports` |
| V10 | Observability | `scheduled_jobs`, `job_runs`, `metric_snapshots`, `anomaly_alerts` |
| V11 | Seed | Default scheduled jobs |
| V12 | Seed | Demo audio assets, questions, and product bundles |
| V13 | Coach | `parent_coach_assignments` |

### 5.2 Core Entity Relationships

```
users ──< user_roles >── roles
  |
  |──< auth_sessions
  |──< device_registrations
  |──< password_history
  |──< login_attempts
  |──< mfa_secrets (1:1)
  |
  |──< cooking_sessions ──< cooking_session_steps ──< step_completion_events
  |                     └──< cooking_session_timers
  |
  |──< question_attempts ──< attempt_evaluations (1:1)
  |
  |──< wrong_notebook_entries ──< wrong_notebook_notes
  |                           ──< wrong_notebook_entry_tags >── wrong_notebook_tags
  |                           ──< wrong_notebook_favorites
  |
  |──< drill_runs
  |──< notifications
  |
  |──< audio_playlists ──< audio_playlist_items
  |──< audio_favorites
  |──< audio_cache_manifests
  |──< audio_bundle_entitlements
  |
  |──< mock_transactions ──< mock_transaction_items
  |                       ──< mock_receipts (1:1)
  |
  |──< parent_coach_assignments (as coach or student)
  |──< privacy_access_logs (as viewer or subject)

audio_assets ──< audio_segments

questions ──< question_variants
          ──< question_similarity_links

tip_cards ──< tip_card_configurations
          ──< step_tip_bindings
          ──< tip_card_audit_logs

scheduled_jobs ──< job_runs
```

### 5.3 Key Design Patterns

- **Temporal tracking:** `createdAt` (immutable, set on persist) and `updatedAt` (auto-managed via `@PreUpdate`) on all entities
- **Optimistic locking:** `@Version` field on `CookingSession` and `User` for concurrent access protection
- **Soft deletes:** `DELETED_SOFT` status on users (data retained for audit)
- **Unique constraints:** Prevent duplicates on business keys (e.g., `user_roles`, `nonce`, `cache_manifest(userId, segmentId)`, `notebook_entry(userId, questionId, status)`)
- **Immutable snapshots:** Cooking session steps are copied from input at creation and never modified
- **Event sourcing (partial):** `step_completion_events` and `tip_card_audit_logs` record state changes as immutable events

---

## 6. Feature Design

### 6.1 Audio Library and Offline Cache

**Data flow:**
1. Admin pre-provisions audio files on the local filesystem
2. `audio_assets` and `audio_segments` reference file paths and sizes
3. Users browse assets via `GET /api/v1/audio/assets` (paginated, searchable)
4. Users download segments via `POST /api/v1/audio/cache/download/{segmentId}`
5. Cache service copies file to `./cache/{userId}/{segmentId}/`, computes SHA-256 checksum
6. `audio_cache_manifests` tracks cached state with `expires_at` (30-day TTL) and `last_accessed_at` (LRU)
7. Users stream cached audio via `GET /api/v1/audio/cache/{manifestId}/stream`

**Quota enforcement:**
- 2 GB per-user limit checked at download time
- Sum of `file_size_bytes` from user's cache manifests
- LRU eviction tracked via `last_accessed_at`
- Storage meter exposed at `GET /api/v1/audio/cache/storage-meter`

**Degraded mode:**
- Frontend tracks consecutive failures (3+ triggers degraded mode)
- Downloads disabled, cached playback continues
- Banner displayed on Audio Library page
- Clears on next successful API response

### 6.2 Cooking Sessions and Timer Management

**Session lifecycle:**
```
CREATED -> ACTIVE -> PAUSED -> ACTIVE -> COMPLETED
                  \-> ABANDONED
```

**Session creation:**
- Client provides `recipeTitle`, optional `lessonId`, and a list of steps
- Steps are immutably snapshotted into `cooking_session_steps`
- Each step may define a timer (duration, reminder text)

**Timer management:**
- Up to 6 concurrent non-terminal timers per session
- Timer states: `RUNNING -> PAUSED -> RUNNING -> ELAPSED_PENDING_ACK -> ACKNOWLEDGED/DISMISSED`
- Also: `RUNNING -> CANCELLED`
- Wall-clock timestamps (`started_at`, `target_end_at`, `paused_at`) enable reconstruction after restart
- `elapsed_before_pause_seconds` accumulates total paused time
- `remainingSeconds` computed by backend on each response

**Session resume:**
- `POST /api/v1/cooking/sessions/{id}/resume`
- Restores session to `ACTIVE` from `PAUSED`
- Timer remaining times recalculated from wall-clock state
- Last completed step order preserved

**Tip cards:**
- Contextual tips appear per step based on `step_tip_bindings`
- Display mode (SHORT, DETAILED, DISABLED) controlled by admin configuration
- Scope hierarchy: STEP > LESSON > GLOBAL

### 6.3 Study and Question Drills

**Question evaluation:**
- Deterministic string matching against `canonical_answer`
- Exact match = CORRECT, substring match = PARTIAL, otherwise = WRONG
- `AttemptEvaluation` stores classification details

**Wrong-Question Notebook:**
- Auto-populated from WRONG, PARTIAL, or user-flagged attempts
- Entry uniqueness: `(user_id, question_id, status)` -- one ACTIVE and one ARCHIVED per question
- Repeated failures increment `fail_count` on existing ACTIVE entry
- Entries support: error-cause tags, personal notes, favorites, status transitions (ACTIVE -> RESOLVED -> ARCHIVED, or ARCHIVED -> ACTIVE via reactivate)

**Drill types:**
- **Retry:** Re-presents the original question from the notebook entry
- **Similar:** Finds questions via `question_similarity_links` (scored 0-1)
- **Variant:** Uses `question_variants` for alternative forms of the same question

**Drill lifecycle:**
```
LaunchDrill -> DrillRun(IN_PROGRESS) -> SubmitAnswers -> CompleteDrill -> DrillRun(COMPLETED)
```

### 6.4 Mock Checkout

**Transaction flow:**
```
Initiate (select bundles) -> INITIATED -> Complete (mock payment) -> COMPLETED -> Receipt generated
                                                                \-> Admin void -> VOIDED
```

- Payment reference token: locally generated UUID, encrypted with AES-256
- Receipt format: `RCPT-YYYYMMDD-NNNNNN`
- Completion grants `AudioBundleEntitlement` records for purchased bundles
- No real payment processing, taxes, shipping, or refunds

**Reconciliation:**
- Daily CSV export with SHA-256 checksum
- File path: `./exports/reconciliation/reconciliation_YYYYMMDD_vN.csv`
- Tracks: transaction counts, completed amounts, voided amounts
- Cron disabled by default, enabled via configuration

### 6.5 Notifications

**Generation:**
- Spring `@Scheduled` job runs every 15 minutes
- Scans for: notebook entries due for practice, overdue items, expiring cache entries
- Creates `Notification` records with suppression keys to prevent duplicates

**Types:** `PRACTICE_DUE`, `PRACTICE_OVERDUE`, `CACHE_EXPIRING`

**Lifecycle:** `GENERATED -> DELIVERED -> READ -> DISMISSED` (also: `EXPIRED`, `SUPPRESSED`)

### 6.6 Parent/Coach Review

**Access model:**
1. Admin creates assignment: `POST /api/v1/admin/review/assignments` linking coach to student
2. Coach views assigned students: `GET /api/v1/review/students`
3. Coach reviews student data with mandatory `reason` parameter
4. Every review creates a `privacy_access_logs` entry

**Reviewable data:** Notebook entries, question attempts, cooking session history

---

## 7. Observability

### 7.1 Tracing

- `TraceIdFilter` assigns a UUID `X-Trace-Id` to every request
- `TraceContext` (ThreadLocal) propagates the ID through async operations
- Audit logs, job runs, privacy logs, and anomaly alerts all reference the trace ID

### 7.2 Audit Logging

- `AuditService` records events asynchronously (with sync fallback for critical events)
- Logged fields: `eventType`, `userId`, `username`, `ipAddress`, `userAgent`, `resourceType`, `resourceId`, `details`, `traceId`
- Admin access: `GET /api/v1/admin/privacy/audit-logs` with filters

### 7.3 Scheduled Jobs

Six default jobs seeded in V11 migration:

| Job | Purpose |
|-----|---------|
| Reminder Generation | Generate practice due/overdue notifications |
| Nonce Cleanup | Purge expired anti-replay nonces |
| Cache Expiry | Mark expired audio cache entries |
| Retention Cleanup | Delete records past retention period |
| Reconciliation Export | Generate daily transaction CSV |
| Anomaly Evaluation | Evaluate metric thresholds for alerts |

Each execution creates a `JobRun` record with status, duration, affected rows/files, and error summaries.

### 7.4 Metrics and Anomaly Detection

- `MetricSnapshot` records time-series data (throughput, latency, error rate)
- Metrics derived from audit logs and login attempt counts
- Default anomaly threshold: error rate >2% over 10-minute window
- Alerts follow lifecycle: OPEN -> ACKNOWLEDGED -> RESOLVED
- Severity levels: INFO, WARNING, CRITICAL

### 7.5 Admin Dashboard

Exposed via `/api/v1/admin/dashboard`:
- **Jobs:** Status and run history of all scheduled jobs
- **Metrics:** Time-series queries by name and date range
- **Alerts:** Anomaly alert management (acknowledge, resolve)
- **Capacity:** Snapshot of system usage (users, sessions, cache, transactions, notifications)
- **KPIs:** Request latency percentiles (p50, p95)

---

## 8. Frontend Architecture

### 8.1 Application Structure

```
frontend/src/
├── main.ts              -- App bootstrap (Vue + Pinia + Router)
├── App.vue              -- Root component with <router-view>
├── router/index.ts      -- Route definitions + navigation guards
├── stores/
│   ├── auth.ts          -- Authentication state + actions
│   └── degradedMode.ts  -- Network resilience tracking
├── api/
│   ├── client.ts        -- Axios instance + request signing + interceptors
│   ├── auth.ts          -- Auth endpoints
│   ├── audio.ts         -- Audio library endpoints
│   ├── cooking.ts       -- Cooking session endpoints
│   ├── questions.ts     -- Study, notebook, drills, notifications
│   ├── checkout.ts      -- Checkout endpoints
│   ├── review.ts        -- Parent/Coach review endpoints
│   ├── tips.ts          -- Admin tip card management
│   ├── users.ts         -- Admin user management
│   ├── reconciliation.ts-- Admin reconciliation exports
│   └── notificationStatus.ts -- Notification status helpers
├── types/               -- TypeScript interfaces mirroring backend DTOs
│   ├── api.ts, auth.ts, audio.ts, cooking.ts,
│   ├── study.ts, finance.ts, user.ts, enums.ts
├── views/               -- Page components
│   ├── LoginView.vue, DashboardView.vue,
│   ├── AudioLibraryView.vue, CookingSessionsView.vue,
│   ├── CookingSessionView.vue, StudyView.vue,
│   ├── NotebookView.vue, CheckoutView.vue,
│   ├── NotificationsView.vue, ReviewView.vue,
│   ├── PasswordChangeView.vue, MfaSetupView.vue,
│   └── admin/
│       ├── UserManagementView.vue, TipManagementView.vue,
│       ├── ReconciliationView.vue, ObservabilityView.vue
└── __tests__/           -- Vitest test files
```

### 8.2 Routing and Guards

| Path | Component | Auth | Role |
|------|-----------|------|------|
| `/login` | LoginView | Guest only | - |
| `/` | DashboardView | Required | Any |
| `/change-password` | PasswordChangeView | Required | Any |
| `/mfa-setup` | MfaSetupView | Required | Any |
| `/audio` | AudioLibraryView | Required | Any |
| `/cooking` | CookingSessionsView | Required | Any |
| `/cooking/:id` | CookingSessionView | Required | Any |
| `/study` | StudyView | Required | Any |
| `/notebook` | NotebookView | Required | Any |
| `/checkout` | CheckoutView | Required | Any |
| `/notifications` | NotificationsView | Required | Any |
| `/review` | ReviewView | Required | PARENT_COACH or ADMIN |
| `/admin/users` | UserManagementView | Required | ADMIN |
| `/admin/tips` | TipManagementView | Required | ADMIN |
| `/admin/reconciliation` | ReconciliationView | Required | ADMIN |
| `/admin/observability` | ObservabilityView | Required | ADMIN |

Navigation guard flow:
1. Check `requiresAuth` meta on route
2. If protected: call `authStore.checkAuth()` (verifies session with backend)
3. If unauthenticated: redirect to `/login?redirect=<originalPath>`
4. If admin route: check `authStore.isAdmin`
5. If guest route and authenticated: redirect to `/`

### 8.3 State Management

**Auth Store** (`stores/auth.ts`):
- Tracks user identity, roles, authentication status, MFA state
- Stores `sessionId` and `signingKey` in `sessionStorage` (lost on tab close)
- Computed properties: `isAdmin`, `isParentCoach`
- Actions: `login()`, `completeMfaVerify()`, `logout()`, `checkAuth()`

**Degraded Mode Store** (`stores/degradedMode.ts`):
- Tracks consecutive network/5xx failures
- Triggers degraded mode after 3 failures
- Clears on successful response
- Exposes `downloadsDisabled` getter

### 8.4 API Client Security

The Axios client (`api/client.ts`) implements all security requirements:

1. **CSRF:** Reads `XSRF-TOKEN` cookie, sends as `X-XSRF-TOKEN` header
2. **Session:** Sends `X-Session-Id` from sessionStorage
3. **Signing:** For POST/PUT/PATCH/DELETE, computes HMAC-SHA256 via Web Crypto API
4. **Idempotency:** Generates `X-Idempotency-Key` UUID per mutation
5. **Error handling:** 401 -> redirect to login; 429 -> log rate limit; network errors -> degraded mode tracking

---

## 9. Deployment

### 9.1 Prerequisites

- Java 23 (for backend)
- Node.js 18+ (for frontend)
- MySQL 8.0 (via Docker or native install)

### 9.2 Docker Compose

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: culinary_coach
      MYSQL_USER: cc_user
      MYSQL_PASSWORD: cc_password
      MYSQL_ROOT_PASSWORD: root
    ports: ["3306:3306"]

  adminer:
    image: adminer
    ports: ["8081:8080"]
    depends_on: [mysql]
```

### 9.3 Environment Variables

| Variable | Purpose | Required |
|----------|---------|----------|
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Initial admin password | Yes (first run) |
| `MFA_ENCRYPTION_KEY` | AES key for TOTP secrets (32+ chars) | Yes |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | Yes |
| `SPRING_DATASOURCE_USERNAME` | DB username | Yes |
| `SPRING_DATASOURCE_PASSWORD` | DB password | Yes |

### 9.4 Port Allocation

| Service | Port |
|---------|------|
| Frontend (Vite) | 5173 |
| Backend (Spring Boot) | 8080 |
| MySQL | 3306 |
| Adminer (DB browser) | 8081 |

---

## 10. Testing Strategy

- **Backend unit/integration tests:** JUnit 5 with H2 in MySQL compatibility mode
- **Frontend unit tests:** Vitest with Happy DOM for DOM simulation
- **Type checking:** `vue-tsc --noEmit` for TypeScript verification
- **Test runner script:** `run_tests.sh` executes backend test suite
- **Manual verification areas:** Authentication flows, timer reconstruction, degraded mode behavior, reconciliation export integrity
