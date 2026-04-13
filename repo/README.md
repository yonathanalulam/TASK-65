# Culinary Study & Cooking Coach Workstation

An offline-first local learning and cooking execution platform for home cooks and self-learners. Combines lesson audio, guided cooking sessions, practice drills, wrong-question remediation, and local bookkeeping capabilities.

## Stack

- **Backend:** Spring Boot 3.5.13 (Java 23)
- **Frontend:** Vue.js 3 + TypeScript + Vite
- **Database:** MySQL 8.0
- **Deployment:** Local machine or LAN only (no cloud dependencies)

## Prerequisites

- Java 23+
- Node.js 18+ / npm 9+
- Docker (for MySQL via Docker Compose)
- Git

## Quick Start

### 1. Start MySQL

```bash
docker compose up -d
```

This starts MySQL 8.0 on port 3306 and Adminer (DB browser) on port 8081.

### 2. Set Environment Variables

```bash
# Required: bootstrap admin password (only needed on first run)
export APP_BOOTSTRAP_ADMIN_PASSWORD="YourSecureAdminPassword1!"

# Required: MFA encryption key (32+ characters)
export MFA_ENCRYPTION_KEY="your-production-encryption-key-32ch"
```

See `.env.example` for all configuration options.

### 3. Start the Backend

```bash
cd backend
chmod +x mvnw
./mvnw spring-boot:run
```

The backend starts on `http://localhost:8080`. On first run, it applies all Flyway migrations and creates the admin user if `APP_BOOTSTRAP_ADMIN_PASSWORD` is set.

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts on `http://localhost:5173` with a proxy to the backend API.

### 5. Login

Open `http://localhost:5173` and login with:
- Username: `admin`
- Password: the value you set in `APP_BOOTSTRAP_ADMIN_PASSWORD`

You will be prompted to change the password on first login.

## Running Tests

### Backend Tests

```bash
cd backend
./mvnw test
```

Tests use H2 in-memory database (MySQL compatibility mode). No external services required.

### Frontend Type Check

```bash
cd frontend
npx vue-tsc --noEmit
```

### Frontend Build

```bash
cd frontend
npm run build
```

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

## Manual Verification Areas

Some features require runtime testing with a running MySQL instance:

- Audio segment download (creates local cache files)
- Cooking session timer reconstruction after restart
- Reconciliation CSV export (writes to `./exports/`)
- Scheduled job execution (reminder generation, cache cleanup)
- Frontend MFA QR code display

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
