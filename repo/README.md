# Culinary Study & Cooking Coach Workstation

> **Project type:** `fullstack`

An offline-first local learning and cooking execution platform for home cooks and self-learners. Combines lesson audio, guided cooking sessions, practice drills, wrong-question remediation, and local bookkeeping capabilities.

## Stack

- **Backend:** Spring Boot 3.5.13 (Java 23)
- **Frontend:** Vue.js 3 + TypeScript + Vite
- **Database:** MySQL 8.0
- **Deployment:** Local machine or LAN only (no cloud dependencies)

## Prerequisites

- Docker and Docker Compose (no local Java, Node.js, or npm required for runtime)
- Git

## Quick Start (Docker-contained)

All services run inside Docker. No local Java, Node.js, or npm installation is required.

### 1. Start All Services

```bash
docker-compose up -d
```

This starts:
- **MySQL 8.0** on port 3306
- **Backend (Spring Boot)** on port 8080
- **Frontend (Vue.js)** on port 80
- **Adminer (DB browser)** on port 8081

The backend waits for MySQL to be healthy before starting, and the frontend waits for the backend. On first run, Flyway migrations run automatically and the admin user is bootstrapped.

### 2. Access the Application

| Service | URL |
|---|---|
| Frontend (UI) | http://localhost |
| Backend API | http://localhost:8080 |
| Adminer (DB browser) | http://localhost:8081 |

### 3. Demo Credentials

The following credentials are available out of the box with `docker-compose up`:

| Role | Username | Password | Notes |
|---|---|---|---|
| `ROLE_ADMIN` | `admin` | `Admin@12345678!` | Bootstrapped on first startup |

To create additional demo users for `ROLE_USER` and `ROLE_PARENT_COACH`, use the admin API after startup:

```bash
# Wait for the backend to be healthy
until curl -sf http://localhost:8080/api/v1/auth/csrf-token > /dev/null; do sleep 2; done

# Login as admin to get session credentials
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@12345678!"}')

SESSION_ID=$(echo "$LOGIN_RESPONSE" | grep -o '"sessionId":"[^"]*"' | cut -d'"' -f4)
SIGNING_KEY=$(echo "$LOGIN_RESPONSE" | grep -o '"signingKey":"[^"]*"' | cut -d'"' -f4)

# Helper function to create signed requests
sign_request() {
  local METHOD=$1 PATH=$2
  local TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  local NONCE=$(cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen)
  local PAYLOAD="${METHOD}\n${PATH}\n${TIMESTAMP}\n${NONCE}"
  local SIGNATURE=$(printf '%b' "$PAYLOAD" | openssl dgst -sha256 -hmac "$(echo "$SIGNING_KEY" | base64 -d)" -binary | base64)
  echo "-H 'X-Session-Id: ${SESSION_ID}' -H 'X-Timestamp: ${TIMESTAMP}' -H 'X-Nonce: ${NONCE}' -H 'X-Signature: ${SIGNATURE}'"
}

# Create a ROLE_USER demo user
eval curl -s -X POST http://localhost:8080/api/v1/admin/users \
  -H "Content-Type: application/json" \
  $(sign_request POST /api/v1/admin/users) \
  -d '{"username":"demouser","password":"DemoUser12345!@","role":"ROLE_USER"}'

# Create a ROLE_PARENT_COACH demo user
eval curl -s -X POST http://localhost:8080/api/v1/admin/users \
  -H "Content-Type: application/json" \
  $(sign_request POST /api/v1/admin/users) \
  -d '{"username":"democoach","password":"DemoCoach12345!@","role":"ROLE_PARENT_COACH"}'
```

After running the above, the following credentials are available:

| Role | Username | Password |
|---|---|---|
| `ROLE_ADMIN` | `admin` | `Admin@12345678!` |
| `ROLE_USER` | `demouser` | `DemoUser12345!@` |
| `ROLE_PARENT_COACH` | `democoach` | `DemoCoach12345!@` |

## Verification

### Backend API Verification

```bash
# 1. Health check — expect {"success":true,"data":{"status":"csrf_token_set"}}
curl -s http://localhost:8080/api/v1/auth/csrf-token
# Expected: JSON with "success":true

# 2. Login as admin — expect sessionId and signingKey in response
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"Admin@12345678!"}'
# Expected: {"success":true,"data":{"sessionId":"...","signingKey":"...","userId":1,"mfaRequired":false}}

# 3. Access protected endpoint — expect user principal data
# (Use the sessionId from step 2)
curl -s http://localhost:8080/api/v1/auth/me \
  -H "X-Session-Id: <sessionId-from-step-2>"
# Expected: {"success":true,"data":{"userId":1,"username":"admin","authorities":["ROLE_ADMIN"],...}}

# 4. List audio assets — expect non-empty array of seeded audio content
curl -s http://localhost:8080/api/v1/audio/assets \
  -H "X-Session-Id: <sessionId-from-step-2>"
# Expected: {"success":true,"data":{"content":[...]}} with 12 seeded assets

# 5. List questions — expect seeded question bank
curl -s http://localhost:8080/api/v1/questions \
  -H "X-Session-Id: <sessionId-from-step-2>"
# Expected: {"success":true,"data":{"content":[...]}} with 12 seeded questions
```

### Frontend Verification

1. Open http://localhost in a browser
2. Login with `admin` / `Admin@12345678!`
3. Verify the dashboard loads with navigation links to Audio Library, Cooking Sessions, Questions, Notebook, Drills, Notifications, and Admin panels
4. Navigate to "Audio Library" and verify 12 audio assets are listed
5. Navigate to "Questions" and verify the question bank loads

## Running Tests

All tests run inside Docker. No local toolchain required.

```bash
./run_tests.sh
```

This builds and runs both backend (Spring Boot + H2 in-memory) and frontend (vitest + happy-dom) test suites in isolated Docker containers.

### Test Details

- **Backend tests:** `@SpringBootTest` integration tests with H2 in-memory database (MySQL compatibility mode). No external services required.
- **Frontend tests:** Vitest with happy-dom environment.
- Test logs written to `/tmp/cc_backend_test.log` and `/tmp/cc_frontend_test.log`.

> **Local development** (running services outside Docker) is optional and documented separately in [`docs/local-development.md`](docs/local-development.md).

## Architecture Overview

```
repo/
├── backend/                    # Spring Boot REST API
│   ├── src/main/java/com/culinarycoach/
│   │   ├── config/             # Security, async, data init
│   │   ├── security/           # Auth filters, CAPTCHA, MFA, rate limiting
│   │   │   ├── auth/           # UserPrincipal, session filter, resolvers
│   │   │   ├── filter/         # TraceId, RateLimit, Signature, Session filters
│   │   │   ├── mfa/            # TOTP, encryption
│   │   │   ├── captcha/        # Local captcha generator
│   │   │   └── nonce/          # Anti-replay nonce service
│   │   ├── domain/             # JPA entities, enums, repositories
│   │   ├── service/            # Business logic
│   │   ├── web/                # REST controllers, DTOs
│   │   └── audit/              # Trace context, audit events
│   └── src/main/resources/
│       └── db/migration/       # Flyway migrations (V1-V13)
└── frontend/                   # Vue.js SPA
    └── src/
        ├── api/                # Typed API clients with HMAC signing
        ├── stores/             # Pinia state management
        ├── router/             # Route definitions with auth guards
        ├── views/              # Page components
        └── types/              # TypeScript type definitions + shared enums
```

## Authentication Model

1. **Login** (`POST /api/v1/auth/login`): validates credentials, optional CAPTCHA after repeated failures
2. **MFA** (optional): if TOTP enabled, returns MFA challenge token; verified via `/api/v1/auth/mfa-verify`
3. **Session**: on successful auth, server creates `AuthSession` and returns `sessionId` + `signingKey`
4. **Subsequent requests**: frontend sends `X-Session-Id` header; `SessionAuthenticationFilter` validates and populates `SecurityContext` with `UserPrincipal`
5. **Signed mutations**: all POST/PUT/PATCH/DELETE require `X-Timestamp`, `X-Nonce`, `X-Signature` (HMAC-SHA256)

## Roles

| Role | Access |
|---|---|
| `ROLE_USER` | Own data: sessions, notebook, audio, checkout |
| `ROLE_PARENT_COACH` | Read-only review of assigned students (with privacy logging) |
| `ROLE_ADMIN` | Full system: user management, config, audits, exports |

## Offline/Local-Only Behavior

- No internet required at runtime
- Audio segments cached locally with 2GB quota and 30-day expiry
- All data persisted in local MySQL
- CAPTCHA generated locally via Java2D (no reCAPTCHA)
- No external payment gateway (mock checkout only)
- Degraded mode: disables downloads, continues playback of valid cached content

## Configuration

Key properties in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `app.security.bcrypt-strength` | 12 | BCrypt cost factor |
| `app.security.max-sessions-per-user` | 3 | Max concurrent sessions |
| `app.security.lockout-duration-minutes` | 15 | Lock after 5 failures |
| `app.security.signature-validity-minutes` | 5 | Anti-replay window |
| `app.audio.cache-quota-bytes` | 2147483648 | 2GB per user |
| `app.audio.cache-validity-days` | 30 | Cache expiry |
| `app.bootstrap.admin-password` | (none) | Initial admin password |

## Reconciliation Exports

Daily reconciliation exports can be run in two ways:

1. **Admin UI:** Navigate to `/admin/reconciliation` (or click "Reconciliation Exports" on the
   Dashboard). Select a business date and click "Run Export". The export history table shows all
   past exports with transaction counts and totals.

2. **Scheduled (cron):** By default the scheduled job is disabled. To enable automatic daily
   exports, set the cron expression in `application.yml`:
   ```yaml
   app:
     checkout:
       reconciliation-cron: "0 0 0 * * *"  # midnight daily
   ```
   When enabled, the job runs `ReconciliationExportJob.dailyClose()` which exports the previous
   day's transactions to `./exports/reconciliation/`.

Export files are CSV with SHA-256 checksum, stored under `./exports/reconciliation/` with naming
convention `reconciliation_YYYYMMDD_vN.csv`.

## API Documentation

All REST endpoints are under `/api/v1`. Standard response envelope:

```json
{
  "traceId": "uuid",
  "success": true,
  "data": {},
  "error": null
}
```

Key endpoint groups:
- `/api/v1/auth/*` — login, logout, MFA, password change
- `/api/v1/audio/*` — library, playlists, favorites, cache
- `/api/v1/cooking/*` — sessions, steps, timers
- `/api/v1/questions/*` — question bank, answer submission
- `/api/v1/notebook/*` — wrong-question notebook
- `/api/v1/drills/*` — retry, similar, variant drills
- `/api/v1/notifications/*` — reminders and notifications
- `/api/v1/checkout/*` — mock purchase flow
- `/api/v1/review/*` — Parent/Coach student review
- `/api/v1/admin/*` — user management, tips, exports, dashboards
