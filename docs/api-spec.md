# API Specification

## Culinary Study & Cooking Coach Workstation

**Base URL:** `/api/v1`
**Content-Type:** `application/json`
**Authentication:** Custom session-based (see [Authentication](#authentication-apiv1auth))

---

## Response Envelope

All endpoints return a standard `ApiResponse<T>` wrapper:

```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "success": true,
  "data": { ... },
  "error": null,
  "meta": { }
}
```

**Error response:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Password must be at least 12 characters",
    "details": { }
  },
  "meta": { }
}
```

**Paginated responses** use `ApiResponse<Page<T>>`:
```json
{
  "traceId": "...",
  "success": true,
  "data": {
    "content": [ ... ],
    "totalElements": 42,
    "totalPages": 3,
    "number": 0,
    "size": 20
  }
}
```

---

## Security Headers

### Required on All Requests

| Header | Value | Notes |
|--------|-------|-------|
| `X-XSRF-TOKEN` | CSRF token | Read from `XSRF-TOKEN` cookie |

### Required on Authenticated Requests

| Header | Value | Notes |
|--------|-------|-------|
| `X-Session-Id` | Session identifier | Returned at login |

### Required on State-Changing Requests (POST/PUT/PATCH/DELETE)

| Header | Value | Notes |
|--------|-------|-------|
| `X-Timestamp` | ISO-8601 timestamp | Must be within +-5 minutes of server time |
| `X-Nonce` | UUID | Single-use per session |
| `X-Signature` | HMAC-SHA256 | Signature of `METHOD\nPATH\nTIMESTAMP\nNONCE` |
| `X-Idempotency-Key` | UUID | Prevents duplicate processing |

---

## Authentication (`/api/v1/auth`)

### `GET /auth/csrf-token`

Obtain initial CSRF token. CSRF-exempt.

**Response:** `ApiResponse<Map>`
```json
{ "data": { "status": "csrf_token_set" } }
```

Sets `XSRF-TOKEN` cookie.

---

### `POST /auth/login`

Authenticate with username and password.

**Request Body:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `username` | string | Yes | Case-insensitive |
| `password` | string | Yes | |
| `deviceFingerprint` | string | No | User-Agent + screen + timezone |
| `captchaId` | string | No | Required after 3 failed attempts |
| `captchaAnswer` | string | No | Required with captchaId |

**Response:** `ApiResponse<LoginResponse>`
```json
{
  "data": {
    "userId": 1,
    "username": "admin",
    "displayName": "Administrator",
    "roles": ["ROLE_ADMIN"],
    "mfaRequired": false,
    "mfaToken": null,
    "forcePasswordChange": false,
    "signingKey": "base64-encoded-hmac-key",
    "sessionId": "uuid-session-id"
  }
}
```

If `mfaRequired` is `true`, `signingKey` and `sessionId` are null. Use the returned `mfaToken` with `/auth/mfa-verify`.

**Error Codes:** `INVALID_CREDENTIALS`, `ACCOUNT_LOCKED`, `ACCOUNT_DISABLED`, `CAPTCHA_REQUIRED`, `CAPTCHA_INVALID`

---

### `POST /auth/mfa-verify`

Complete MFA login with TOTP code.

**Request Body:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `code` | string | Yes | 6-digit TOTP code |
| `mfaToken` | string | Yes | Token from login response |

**Response:** `ApiResponse<LoginResponse>` (same as login, with `signingKey` and `sessionId` populated)

---

### `POST /auth/logout`

Invalidate current session. Requires authentication.

**Response:** `ApiResponse<String>`
```json
{ "data": "Logged out" }
```

---

### `POST /auth/change-password`

Change the authenticated user's password. Requires authentication.

**Request Body:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `currentPassword` | string | Yes | |
| `newPassword` | string | Yes | Min 12 chars, complexity rules, no reuse of last 5 |

**Response:** `ApiResponse<Void>`

**Error Codes:** `INVALID_CURRENT_PASSWORD`, `PASSWORD_TOO_WEAK`, `PASSWORD_RECENTLY_USED`

---

### `GET /auth/me`

Get current authenticated user info.

**Response:** `ApiResponse<Map>`
```json
{
  "data": {
    "userId": 1,
    "username": "admin",
    "authorities": ["ROLE_ADMIN"],
    "sessionId": "uuid-session-id"
  }
}
```

---

## MFA (`/api/v1/mfa`)

Requires authentication.

### `POST /mfa/setup`

Generate TOTP secret and QR code.

**Response:** `ApiResponse<MfaSetupResponse>`
```json
{
  "data": {
    "qrCodeDataUri": "data:image/png;base64,...",
    "secretKey": "BASE32SECRET",
    "recoveryCodes": ["code1", "code2", "code3", "code4", "code5", "code6", "code7", "code8"]
  }
}
```

---

### `POST /mfa/verify`

Verify TOTP code and enable MFA.

**Request Body:**
| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `code` | string | Yes | 6-digit TOTP code |
| `mfaToken` | string | No | Not needed during setup verification |

**Response:** `ApiResponse<Map>`
```json
{ "data": { "verified": true } }
```

---

### `POST /mfa/disable`

Disable MFA for the authenticated user.

**Response:** `ApiResponse<Void>`

---

## CAPTCHA (`/api/v1/captcha`)

Public endpoint.

### `GET /captcha/challenge`

Generate a new CAPTCHA challenge.

**Response:** `ApiResponse<Map>`
```json
{
  "data": {
    "challengeId": "uuid",
    "imageBase64": "data:image/png;base64,..."
  }
}
```

Challenge expires in 5 minutes.

---

## Audio Library (`/api/v1/audio`)

Requires authentication.

### `GET /audio/assets`

Browse/search audio assets (paginated).

**Query Parameters:**
| Param | Type | Default | Notes |
|-------|------|---------|-------|
| `search` | string | - | Filter by title/description |
| `page` | int | 0 | Page number |
| `size` | int | 20 | Page size |

**Response:** `ApiResponse<Page<AudioAssetResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Knife Skills Fundamentals",
        "description": "Learn essential knife techniques",
        "coverArtPath": "/covers/knife-skills.jpg",
        "durationSeconds": 1800,
        "category": "Techniques",
        "difficulty": "BEGINNER",
        "isFavorite": false
      }
    ],
    "totalElements": 15,
    "totalPages": 1,
    "number": 0,
    "size": 20
  }
}
```

---

### `GET /audio/assets/{id}`

Get audio asset details.

**Response:** `ApiResponse<AudioAssetResponse>`

---

## Audio Cache (`/api/v1/audio/cache`)

Requires authentication.

### `POST /audio/cache/download/{segmentId}`

Download a single audio segment to local cache.

**Response:** `ApiResponse<CacheEntryResponse>`
```json
{
  "data": {
    "id": 1,
    "segmentId": 5,
    "assetTitle": "Knife Skills Fundamentals",
    "status": "CACHED_VALID",
    "fileSizeBytes": 15728640,
    "downloadedAt": "2026-04-13T10:30:00Z",
    "expiresAt": "2026-05-13T10:30:00Z",
    "expiresInLabel": "30 days"
  }
}
```

**Error Codes:** `CACHE_QUOTA_EXCEEDED`, `SEGMENT_NOT_FOUND`

---

### `POST /audio/cache/download-asset/{assetId}`

Download all segments of an asset.

**Response:** `ApiResponse<List<CacheEntryResponse>>`

---

### `GET /audio/cache/status`

List all cached segments for the authenticated user.

**Response:** `ApiResponse<List<CacheEntryResponse>>`

---

### `GET /audio/cache/storage-meter`

Get user's cache storage usage.

**Response:** `ApiResponse<StorageMeterResponse>`
```json
{
  "data": {
    "usedBytes": 524288000,
    "totalQuotaBytes": 2147483648,
    "percentUsed": 24.41,
    "reclaimableBytes": 104857600
  }
}
```

---

### `DELETE /audio/cache/{manifestId}`

Remove a cached segment.

**Response:** `ApiResponse<Void>`

---

### `GET /audio/cache/{manifestId}/stream`

Stream a cached audio file.

**Response:** Binary file stream (`application/octet-stream`)

---

## Playlists (`/api/v1/audio/playlists`)

Requires authentication.

### `GET /audio/playlists`

List user's playlists.

**Response:** `ApiResponse<List<PlaylistResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "name": "Morning Prep",
      "description": "Quick morning cooking lessons",
      "itemCount": 5,
      "createdAt": "2026-04-01T08:00:00Z"
    }
  ]
}
```

---

### `GET /audio/playlists/{id}`

Get playlist with items.

**Response:** `ApiResponse<PlaylistDetailResponse>`
```json
{
  "data": {
    "id": 1,
    "name": "Morning Prep",
    "description": "Quick morning cooking lessons",
    "items": [ { ... AudioAssetResponse ... } ]
  }
}
```

---

### `POST /audio/playlists`

Create a new playlist.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | Yes | Max 200 chars |
| `description` | string | No | |

**Response:** `ApiResponse<PlaylistResponse>`

---

### `PUT /audio/playlists/{id}`

Update playlist metadata.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | No | Max 200 chars |
| `description` | string | No | |

**Response:** `ApiResponse<PlaylistResponse>`

---

### `DELETE /audio/playlists/{id}`

Delete a playlist.

**Response:** `ApiResponse<Void>`

---

### `POST /audio/playlists/{id}/items`

Add an audio asset to a playlist.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `audioAssetId` | long | Yes |

**Response:** `ApiResponse<Void>`

---

### `DELETE /audio/playlists/{id}/items/{assetId}`

Remove an audio asset from a playlist.

**Response:** `ApiResponse<Void>`

---

### `PUT /audio/playlists/{id}/items/reorder`

Reorder playlist items.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `orderedAssetIds` | long[] | Yes |

**Response:** `ApiResponse<Void>`

---

## Favorites (`/api/v1/audio/favorites`)

Requires authentication.

### `GET /audio/favorites`

List favorited audio assets.

**Response:** `ApiResponse<List<AudioAssetResponse>>`

---

### `POST /audio/favorites/{assetId}`

Add an asset to favorites.

**Response:** `ApiResponse<Map>`
```json
{ "data": { "favorited": true } }
```

---

### `DELETE /audio/favorites/{assetId}`

Remove an asset from favorites.

**Response:** `ApiResponse<Map>`
```json
{ "data": { "favorited": false } }
```

---

## Cooking Sessions (`/api/v1/cooking/sessions`)

Requires authentication.

### `POST /cooking/sessions`

Start a new cooking session.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `recipeTitle` | string | Yes | Max 255 chars |
| `lessonId` | long | No | |
| `steps` | StepInput[] | Yes | Non-empty |

**StepInput:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `title` | string | Yes | Max 255 chars |
| `description` | string | No | |
| `expectedDurationSeconds` | int | No | |
| `hasTimer` | boolean | No | |
| `timerDurationSeconds` | int | No | |
| `reminderText` | string | No | Max 500 chars |

**Response:** `ApiResponse<CookingSessionResponse>`
```json
{
  "data": {
    "id": 1,
    "recipeTitle": "Pasta Carbonara",
    "lessonId": 3,
    "status": "CREATED",
    "totalSteps": 5,
    "lastCompletedStepOrder": 0,
    "startedAt": "2026-04-13T12:00:00Z",
    "completedAt": null,
    "lastActivityAt": "2026-04-13T12:00:00Z"
  }
}
```

---

### `GET /cooking/sessions`

List user's cooking sessions.

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `statuses` | CookingSessionStatus[] | Optional filter (e.g., `?statuses=ACTIVE&statuses=PAUSED`) |

**Response:** `ApiResponse<List<CookingSessionResponse>>`

---

### `GET /cooking/sessions/{id}`

Get session detail with steps and timers.

**Response:** `ApiResponse<CookingSessionDetailResponse>`
```json
{
  "data": {
    "id": 1,
    "recipeTitle": "Pasta Carbonara",
    "lessonId": 3,
    "status": "ACTIVE",
    "totalSteps": 5,
    "lastCompletedStepOrder": 2,
    "startedAt": "2026-04-13T12:00:00Z",
    "resumedAt": null,
    "completedAt": null,
    "abandonedAt": null,
    "lastActivityAt": "2026-04-13T12:15:00Z",
    "steps": [
      {
        "id": 1,
        "stepOrder": 1,
        "title": "Boil water",
        "description": "Fill pot and bring to rolling boil",
        "expectedDurationSeconds": 600,
        "hasTimer": true,
        "timerDurationSeconds": 600,
        "reminderText": "Add salt to the water",
        "completed": true,
        "completedAt": "2026-04-13T12:10:00Z",
        "tips": [
          {
            "id": 1,
            "title": "Water Temperature",
            "shortText": "Use cold water for better taste",
            "detailedText": "Starting with cold water allows...",
            "displayMode": "SHORT",
            "enabled": true
          }
        ]
      }
    ],
    "timers": [
      {
        "id": 1,
        "stepId": 2,
        "label": "Pasta cooking timer",
        "timerType": "STEP",
        "status": "RUNNING",
        "durationSeconds": 480,
        "remainingSeconds": 320,
        "startedAt": "2026-04-13T12:12:00Z",
        "targetEndAt": "2026-04-13T12:20:00Z",
        "pausedAt": null,
        "acknowledgedAt": null
      }
    ]
  }
}
```

---

### `POST /cooking/sessions/{id}/resume`

Resume a paused session. Recalculates timer states from wall-clock timestamps.

**Response:** `ApiResponse<CookingSessionDetailResponse>`

---

### `POST /cooking/sessions/{id}/pause`

Pause an active session. Pauses all running timers.

**Response:** `ApiResponse<CookingSessionResponse>`

---

### `POST /cooking/sessions/{id}/abandon`

Abandon a session.

**Response:** `ApiResponse<CookingSessionResponse>`

---

### `POST /cooking/sessions/{id}/steps/{stepOrder}/complete`

Mark a step as completed. If the session is in CREATED status, transitions to ACTIVE. If all steps are completed, transitions to COMPLETED.

**Response:** `ApiResponse<CookingSessionDetailResponse>`

---

## Timers (`/api/v1/cooking/sessions/{sessionId}/timers`)

Requires authentication. Maximum 6 non-terminal timers per session.

### `POST /cooking/sessions/{sessionId}/timers`

Create a new timer.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `stepId` | long | No | |
| `label` | string | No | Max 200 chars |
| `durationSeconds` | int | Yes | 1 - 86400 |

**Response:** `ApiResponse<TimerResponse>`

---

### `POST /cooking/sessions/{sessionId}/timers/{timerId}/pause`

Pause a running timer. Records `paused_at` timestamp.

**Response:** `ApiResponse<TimerResponse>`

---

### `POST /cooking/sessions/{sessionId}/timers/{timerId}/resume`

Resume a paused timer. Recalculates `target_end_at` based on elapsed pause time.

**Response:** `ApiResponse<TimerResponse>`

---

### `POST /cooking/sessions/{sessionId}/timers/{timerId}/cancel`

Cancel a timer.

**Response:** `ApiResponse<TimerResponse>`

---

### `POST /cooking/sessions/{sessionId}/timers/{timerId}/acknowledge`

Acknowledge an elapsed timer.

**Response:** `ApiResponse<TimerResponse>`

---

### `POST /cooking/sessions/{sessionId}/timers/{timerId}/dismiss`

Dismiss an elapsed timer alert.

**Response:** `ApiResponse<TimerResponse>`

---

## Questions (`/api/v1/questions`)

### `GET /questions`

List questions by lesson (paginated).

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `lessonId` | long | Yes |
| `page` | int | No (default 0) |
| `size` | int | No (default 20) |

**Response:** `ApiResponse<Page<QuestionResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "questionText": "What is the proper technique for dicing an onion?",
        "questionType": "SINGLE_CHOICE",
        "difficulty": "MEDIUM",
        "lessonId": 1
      }
    ]
  }
}
```

---

### `GET /questions/{id}`

Get question detail.

**Response:** `ApiResponse<QuestionResponse>`

---

### `POST /questions/{id}/answer`

Submit an answer. Requires authentication. Automatically evaluates and creates a wrong-notebook entry if incorrect or flagged.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `userAnswer` | string | Yes | Max 5000 chars |
| `flagged` | boolean | No | Flag for review regardless of result |

**Response:** `ApiResponse<SubmitAnswerResponse>`
```json
{
  "data": {
    "classification": "WRONG",
    "correct": false,
    "explanation": "The correct technique involves...",
    "notebookEntryCreated": true
  }
}
```

**Classification values:** `CORRECT`, `PARTIAL`, `WRONG`, `FLAGGED_BY_USER`

---

## Drills (`/api/v1/drills`)

Requires authentication.

### `POST /drills/retry`

Launch a retry drill on a wrong notebook entry.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `entryId` | long | Yes |

**Response:** `ApiResponse<DrillRunResponse>`
```json
{
  "data": {
    "id": 1,
    "drillType": "RETRY",
    "status": "IN_PROGRESS",
    "totalQuestions": 1,
    "correctCount": 0,
    "startedAt": "2026-04-13T14:00:00Z"
  }
}
```

---

### `POST /drills/similar`

Launch a similar-question drill. Finds questions via the similarity graph.

**Request Body:** Same as retry.

**Response:** `ApiResponse<DrillRunResponse>`

---

### `POST /drills/variant`

Launch a variant drill. Uses alternative forms of the question.

**Request Body:** Same as retry.

**Response:** `ApiResponse<DrillRunResponse>`

---

### `POST /drills/{drillId}/answer`

Submit an answer within a drill.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `questionId` | long | Yes | |
| `answer` | string | Yes | Max 5000 chars |

**Response:** `ApiResponse<SubmitAnswerResponse>`

---

### `POST /drills/{drillId}/complete`

Complete a drill run.

**Response:** `ApiResponse<DrillRunResponse>`

---

## Wrong-Question Notebook (`/api/v1/notebook/entries`)

Requires authentication.

### `GET /notebook/entries`

List notebook entries (paginated).

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `status` | NotebookEntryStatus | ACTIVE, FAVORITED, RESOLVED, ARCHIVED |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

**Response:** `ApiResponse<Page<NotebookEntryResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "questionText": "What is the proper technique for dicing an onion?",
        "status": "ACTIVE",
        "failCount": 3,
        "isFavorite": true,
        "tags": ["technique-error", "knife-skills"],
        "latestNote": "Need to practice the claw grip",
        "lastAttemptAt": "2026-04-12T16:00:00Z"
      }
    ]
  }
}
```

---

### `GET /notebook/entries/{id}`

Get entry detail with notes and tags.

**Response:** `ApiResponse<NotebookEntryDetailResponse>`
```json
{
  "data": {
    "id": 1,
    "questionText": "What is the proper technique for dicing an onion?",
    "status": "ACTIVE",
    "failCount": 3,
    "isFavorite": true,
    "tags": ["technique-error"],
    "latestNote": "Need to practice the claw grip",
    "lastAttemptAt": "2026-04-12T16:00:00Z",
    "notes": [
      {
        "id": 1,
        "noteText": "Need to practice the claw grip",
        "createdAt": "2026-04-12T16:05:00Z"
      }
    ],
    "questionExplanation": "The proper technique involves..."
  }
}
```

---

### `POST /notebook/entries/{id}/notes`

Add a note to an entry.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `noteText` | string | Yes | Max 5000 chars |

**Response:** `ApiResponse<NotebookEntryDetailResponse>`

---

### `POST /notebook/entries/{id}/tags`

Add an error-cause tag.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `tagLabel` | string | Yes | Max 50 chars |

**Response:** `ApiResponse<NotebookEntryDetailResponse>`

---

### `DELETE /notebook/entries/{id}/tags/{tagId}`

Remove a tag from an entry.

**Response:** `ApiResponse<NotebookEntryDetailResponse>`

---

### `POST /notebook/entries/{id}/favorite`

Toggle favorite status.

**Response:** `ApiResponse<NotebookEntryResponse>`

---

### `POST /notebook/entries/{id}/resolve`

Mark entry as resolved.

**Response:** `ApiResponse<NotebookEntryResponse>`

---

### `POST /notebook/entries/{id}/archive`

Archive an entry.

**Response:** `ApiResponse<NotebookEntryResponse>`

---

### `POST /notebook/entries/{id}/reactivate`

Reactivate an archived entry.

**Response:** `ApiResponse<NotebookEntryResponse>`

---

## Notifications (`/api/v1/notifications`)

Requires authentication.

### `GET /notifications`

List notifications (paginated).

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `status` | NotificationStatus | GENERATED, DELIVERED, READ, DISMISSED, EXPIRED, SUPPRESSED |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

**Response:** `ApiResponse<Page<NotificationResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "type": "PRACTICE_OVERDUE",
        "title": "Overdue Practice",
        "message": "You have 3 overdue practice items",
        "status": "GENERATED",
        "priority": 2,
        "createdAt": "2026-04-13T09:00:00Z"
      }
    ]
  }
}
```

---

### `GET /notifications/unread-count`

Get count of unread notifications.

**Response:** `ApiResponse<Map>`
```json
{ "data": { "unreadCount": 5 } }
```

---

### `POST /notifications/{id}/read`

Mark a notification as read.

**Response:** `ApiResponse<NotificationResponse>`

---

### `POST /notifications/{id}/dismiss`

Dismiss a notification.

**Response:** `ApiResponse<NotificationResponse>`

---

## Checkout (`/api/v1/checkout`)

Requires authentication.

### `GET /checkout/bundles`

List available product bundles.

**Response:** `ApiResponse<List<ProductBundleResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "name": "Essential Cooking Techniques",
      "description": "Complete audio course on fundamental techniques",
      "price": 29.99
    }
  ]
}
```

---

### `POST /checkout/initiate`

Initiate a checkout transaction.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `bundleIds` | long[] | Yes (non-empty) |

**Response:** `ApiResponse<TransactionResponse>`
```json
{
  "data": {
    "id": 1,
    "status": "INITIATED",
    "totalAmount": 49.98,
    "receiptNumber": null,
    "initiatedAt": "2026-04-13T15:00:00Z",
    "completedAt": null,
    "items": [
      {
        "bundleId": 1,
        "bundleName": "Essential Cooking Techniques",
        "unitPrice": 29.99,
        "quantity": 1,
        "lineTotal": 29.99
      }
    ],
    "paymentRefMasked": null
  }
}
```

---

### `POST /checkout/complete/{transactionId}`

Complete a transaction (mock payment). Generates receipt and grants bundle entitlements.

**Response:** `ApiResponse<TransactionResponse>`

The response includes `receiptNumber` (format: `RCPT-YYYYMMDD-NNNNNN`) and `paymentRefMasked` (masked UUID).

---

### `GET /checkout/transactions`

List user's transactions (paginated).

**Query Parameters:**
| Param | Type | Default |
|-------|------|---------|
| `page` | int | 0 |
| `size` | int | 20 |

**Response:** `ApiResponse<Page<TransactionResponse>>`

---

### `GET /checkout/transactions/{id}`

Get transaction detail.

**Response:** `ApiResponse<TransactionResponse>`

---

## Parent/Coach Review (`/api/v1/review`)

Requires `ROLE_PARENT_COACH` or `ROLE_ADMIN`. All endpoints create privacy access log entries.

### `GET /review/students`

List students assigned to the authenticated coach.

**Response:** `ApiResponse<List<ReviewStudentResponse>>`
```json
{
  "data": [
    {
      "userId": 5,
      "username": "student1",
      "displayName": "Alice Johnson"
    }
  ]
}
```

---

### `GET /review/students/{studentId}/notebook`

View a student's wrong-question notebook. Privacy logged.

**Query Parameters:**
| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `reason` | string | Yes | Reason for accessing data |

**Response:** `ApiResponse<List<NotebookEntryResponse>>`

---

### `GET /review/students/{studentId}/attempts`

View a student's question attempt history. Privacy logged.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `reason` | string | Yes |

**Response:** `ApiResponse<List<AttemptHistoryResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "questionText": "What temperature should oil be for deep frying?",
      "userAnswer": "350 degrees",
      "classification": "CORRECT",
      "attemptedAt": "2026-04-12T14:30:00Z"
    }
  ]
}
```

---

### `GET /review/students/{studentId}/cooking-history`

View a student's cooking session history. Privacy logged.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `reason` | string | Yes |

**Response:** `ApiResponse<List<CookingSessionResponse>>`

---

## Admin: User Management (`/api/v1/admin/users`)

Requires `ROLE_ADMIN`.

### `POST /admin/users`

Create a new user. Returns HTTP 201.

**Request Body:**
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `username` | string | Yes | 3-64 chars, alphanumeric + `._-` |
| `password` | string | Yes | Must meet password policy |
| `displayName` | string | No | |
| `email` | string | No | |
| `roles` | string[] | No | e.g., `["ROLE_USER"]` |

**Response:** `ApiResponse<UserResponse>` (HTTP 201)
```json
{
  "data": {
    "id": 5,
    "username": "newuser",
    "displayName": "New User",
    "email": "new@example.com",
    "status": "PENDING_SETUP",
    "mfaEnabled": false,
    "roles": ["ROLE_USER"],
    "createdAt": "2026-04-13T16:00:00Z"
  }
}
```

---

### `GET /admin/users`

List all users (paginated, sorted by `createdAt` descending).

**Query Parameters:**
| Param | Type | Default |
|-------|------|---------|
| `page` | int | 0 |
| `size` | int | 20 |

**Response:** `ApiResponse<Page<UserResponse>>`

---

### `GET /admin/users/{id}`

Get user detail.

**Response:** `ApiResponse<UserResponse>`

---

### `POST /admin/users/{id}/disable`

Disable a user account.

**Response:** `ApiResponse<UserResponse>`

---

### `POST /admin/users/{id}/enable`

Re-enable a disabled user account.

**Response:** `ApiResponse<UserResponse>`

---

## Admin: Tip Card Management (`/api/v1/admin/tips`)

Requires `ROLE_ADMIN`.

### `GET /admin/tips`

List all tip cards.

**Response:** `ApiResponse<List<TipCardResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "title": "Knife Safety",
      "shortText": "Always curl your fingers",
      "detailedText": "The claw grip technique involves...",
      "displayMode": "SHORT",
      "enabled": true
    }
  ]
}
```

---

### `POST /admin/tips/{id}/configure`

Set display mode for a tip card.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `scope` | string | Yes |
| `scopeId` | long | No |
| `displayMode` | string | Yes |

**Display modes:** `DISABLED`, `SHORT`, `DETAILED`

**Response:** `ApiResponse<TipCardResponse>`

---

### `POST /admin/tips/{id}/toggle`

Toggle tip card enabled/disabled state.

**Response:** `ApiResponse<TipCardResponse>`

---

## Admin: Checkout Management (`/api/v1/admin/checkout`)

Requires `ROLE_ADMIN`.

### `POST /admin/checkout/transactions/{id}/void`

Void a transaction.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `reason` | string | Yes |

**Response:** `ApiResponse<TransactionResponse>`

---

### `GET /admin/checkout/reconciliation/export`

Generate a reconciliation export for a business date.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `businessDate` | date (YYYY-MM-DD) | Yes |

**Response:** `ApiResponse<ReconciliationExportResponse>`
```json
{
  "data": {
    "id": 1,
    "businessDate": "2026-04-12",
    "exportVersion": 1,
    "filePath": "./exports/reconciliation/reconciliation_20260412_v1.csv",
    "fileChecksum": "sha256:abc123...",
    "transactionCount": 15,
    "totalCompletedAmount": 449.85,
    "totalVoidedAmount": 29.99,
    "generatedBy": "admin",
    "generatedAt": "2026-04-13T17:00:00Z"
  }
}
```

---

### `GET /admin/checkout/reconciliation/exports`

List reconciliation export history (paginated).

**Response:** `ApiResponse<Page<ReconciliationExportResponse>>`

---

### `GET /admin/checkout/reconciliation/exports/{id}`

Get export detail.

**Response:** `ApiResponse<ReconciliationExportResponse>`

---

## Admin: Coach Assignment Management (`/api/v1/admin/review`)

Requires `ROLE_ADMIN`.

### `POST /admin/review/assignments`

Assign a student to a coach.

**Request Body:**
| Field | Type | Required |
|-------|------|----------|
| `coachUserId` | long | Yes |
| `studentUserId` | long | Yes |

**Response:** `ApiResponse<Void>`

---

### `DELETE /admin/review/assignments`

Remove a coach-student assignment.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `coachUserId` | long | Yes |
| `studentUserId` | long | Yes |

**Response:** `ApiResponse<Void>`

---

### `GET /admin/review/assignments`

List students assigned to a specific coach.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `coachUserId` | long | Yes |

**Response:** `ApiResponse<List<ReviewStudentResponse>>`

---

## Admin: Observability Dashboard (`/api/v1/admin/dashboard`)

Requires `ROLE_ADMIN`.

### `GET /admin/dashboard/jobs`

List all scheduled jobs and their status.

**Response:** `ApiResponse<List<ScheduledJobResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "jobName": "reminder-generation",
      "description": "Generate practice due/overdue notifications",
      "enabled": true,
      "latestRunStatus": "SUCCEEDED",
      "latestRunAt": "2026-04-13T17:00:00Z"
    }
  ]
}
```

---

### `GET /admin/dashboard/jobs/{jobName}/runs`

Get execution history for a specific job (paginated, sorted by `startedAt` descending).

**Response:** `ApiResponse<Page<JobRunResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 42,
        "jobName": "reminder-generation",
        "status": "SUCCEEDED",
        "startedAt": "2026-04-13T17:00:00Z",
        "endedAt": "2026-04-13T17:00:02Z",
        "affectedRows": 5,
        "affectedFiles": 0,
        "errorSummary": null,
        "retryCount": 0,
        "traceId": "uuid"
      }
    ]
  }
}
```

---

### `GET /admin/dashboard/metrics`

Query time-series metrics.

**Query Parameters:**
| Param | Type | Required |
|-------|------|----------|
| `name` | string | Yes |
| `from` | ISO-8601 datetime | Yes |
| `to` | ISO-8601 datetime | Yes |

**Response:** `ApiResponse<List<MetricSnapshotResponse>>`
```json
{
  "data": [
    {
      "id": 1,
      "metricName": "request_throughput",
      "metricValue": 45.2,
      "dimensionKey": "endpoint",
      "dimensionValue": "/api/v1/cooking/sessions",
      "windowStart": "2026-04-13T16:00:00Z",
      "windowEnd": "2026-04-13T16:10:00Z"
    }
  ]
}
```

---

### `GET /admin/dashboard/alerts`

List anomaly alerts.

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `status` | AlertStatus | OPEN, ACKNOWLEDGED, RESOLVED |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

**Response:** `ApiResponse<Page<AnomalyAlertResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "alertType": "ERROR_RATE_HIGH",
        "severity": "WARNING",
        "message": "Error rate exceeded 2% over 10 minutes",
        "metricName": "error_rate",
        "thresholdValue": 2.0,
        "actualValue": 3.5,
        "status": "OPEN",
        "acknowledgedBy": null,
        "acknowledgedAt": null,
        "resolvedAt": null,
        "createdAt": "2026-04-13T16:30:00Z"
      }
    ]
  }
}
```

---

### `POST /admin/dashboard/alerts/{id}/acknowledge`

Acknowledge an alert.

**Response:** `ApiResponse<AnomalyAlertResponse>`

---

### `POST /admin/dashboard/alerts/{id}/resolve`

Resolve an alert.

**Response:** `ApiResponse<AnomalyAlertResponse>`

---

### `GET /admin/dashboard/alerts/count`

Get count of open alerts.

**Response:** `ApiResponse<Map>`
```json
{ "data": { "openAlerts": 2 } }
```

---

### `GET /admin/dashboard/capacity`

Get system capacity report.

**Response:** `ApiResponse<CapacityReportResponse>`
```json
{
  "data": {
    "totalUsers": 25,
    "activeSessions": 8,
    "totalAudioCacheBytes": 3221225472,
    "totalTransactions": 142,
    "pendingNotifications": 12,
    "reportTime": "2026-04-13T17:00:00Z"
  }
}
```

---

### `GET /admin/dashboard/kpis`

Get latency KPI summary.

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `from` | ISO-8601 datetime | Optional |
| `to` | ISO-8601 datetime | Optional |

**Response:** `ApiResponse<KpiSummaryResponse>`
```json
{
  "data": {
    "requestThroughput": 125.5,
    "errorRate": 0.8,
    "p50Latency": 45.0,
    "p95Latency": 250.0,
    "windowStart": "2026-04-13T16:00:00Z",
    "windowEnd": "2026-04-13T17:00:00Z"
  }
}
```

---

## Admin: Privacy Logs (`/api/v1/admin/privacy`)

Requires `ROLE_ADMIN`.

### `GET /admin/privacy/access-logs`

Query privacy access logs.

**Query Parameters:**
| Param | Type | Notes |
|-------|------|-------|
| `subjectUserId` | long | Optional filter |
| `page` | int | Default 0 |
| `size` | int | Default 20 |

**Response:** `ApiResponse<Page<PrivacyAccessLogResponse>>`
```json
{
  "data": {
    "content": [
      {
        "id": 1,
        "viewerUsername": "coach1",
        "viewerRole": "ROLE_PARENT_COACH",
        "subjectUsername": "student1",
        "resourceType": "NOTEBOOK",
        "resourceId": "5",
        "reasonCode": "Progress Check",
        "traceId": "uuid",
        "createdAt": "2026-04-13T10:00:00Z"
      }
    ]
  }
}
```

---

## HTTP Status Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created (user creation) |
| 400 | Validation error / bad request |
| 401 | Unauthenticated (invalid/expired session) |
| 403 | Forbidden (insufficient role or not resource owner) |
| 404 | Resource not found |
| 409 | Conflict (duplicate resource) |
| 429 | Rate limited (includes `Retry-After` header) |
| 500 | Internal server error |

---

## Rate Limits

| Context | Limit |
|---------|-------|
| Authenticated users | 60 requests/minute |
| Unauthenticated users | 20 requests/minute |
| Admin users | 30 requests/minute |
| Burst | 10 requests/2 seconds |

When rate limited, the response includes a `Retry-After` header with the number of seconds to wait.

---

## Enumerations Reference

| Enum | Values |
|------|--------|
| AccountStatus | `PENDING_SETUP`, `ACTIVE`, `LOCKED`, `DISABLED`, `DELETED_SOFT` |
| CookingSessionStatus | `CREATED`, `ACTIVE`, `PAUSED`, `COMPLETED`, `ABANDONED`, `EXPIRED` |
| TimerStatus | `RUNNING`, `PAUSED`, `ELAPSED_PENDING_ACK`, `ACKNOWLEDGED`, `DISMISSED`, `CANCELLED` |
| CacheEntryStatus | `NOT_CACHED`, `DOWNLOADING`, `CACHED_VALID`, `EXPIRED`, `CORRUPT`, `DELETED` |
| AttemptClassification | `CORRECT`, `PARTIAL`, `WRONG`, `FLAGGED_BY_USER` |
| NotebookEntryStatus | `ACTIVE`, `FAVORITED`, `RESOLVED`, `ARCHIVED` |
| DrillType | `RETRY`, `SIMILAR`, `VARIANT` |
| NotificationStatus | `GENERATED`, `DELIVERED`, `READ`, `DISMISSED`, `EXPIRED`, `SUPPRESSED` |
| NotificationType | `PRACTICE_DUE`, `PRACTICE_OVERDUE`, `CACHE_EXPIRING` |
| TransactionStatus | `INITIATED`, `COMPLETED`, `VOIDED`, `FAILED` |
| TipDisplayMode | `DISABLED`, `SHORT`, `DETAILED` |
| AlertStatus | `OPEN`, `ACKNOWLEDGED`, `RESOLVED` |
| AlertSeverity | `INFO`, `WARNING`, `CRITICAL` |
| JobRunStatus | `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `PARTIAL_SUCCESS`, `RETRY_QUEUED`, `TERMINAL_FAILED`, `CANCELLED` |
