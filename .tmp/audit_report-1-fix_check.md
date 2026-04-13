# Fix Check Report (Against Previous Static Audit)
Static-only recheck of issues from `.tmp/static-audit-report.md`.
## Summary
- Total prior issues checked: **11**
- **Fixed:** 11
- **Partially Fixed:** 0
- **Still Open (not fixed):** 0
## Issue-by-Issue Status
1) **Broken authenticated-user identity resolution across core APIs**  
**Status: Fixed**
- Controllers now use centralized resolver instead of casting domain `User` from principal.
- `backend/src/main/java/com/culinarycoach/security/auth/AuthenticatedUserResolver.java:13`
- `backend/src/main/java/com/culinarycoach/web/controller/CookingSessionController.java:4`
- `backend/src/main/java/com/culinarycoach/web/controller/NotebookController.java:4`
2) **Authentication/session model not integrated with security context**  
**Status: Fixed**
- Session auth filter validates `X-Session-Id` and sets `SecurityContext` with `UserPrincipal`.
- `backend/src/main/java/com/culinarycoach/security/filter/SessionAuthenticationFilter.java:126`
- Registered in security chain.
- `backend/src/main/java/com/culinarycoach/config/SecurityConfig.java:68`
3) **Signed-request anti-replay control bypassable by design**  
**Status: Fixed**
- Backend now requires all signing headers for state-changing requests and validates active session + nonce + HMAC.
- `backend/src/main/java/com/culinarycoach/security/filter/RequestSignatureFilter.java:87`
- `backend/src/main/java/com/culinarycoach/security/filter/RequestSignatureFilter.java:111`
- `backend/src/main/java/com/culinarycoach/security/filter/RequestSignatureFilter.java:118`
- Frontend now computes real WebCrypto HMAC (placeholder removed).
- `frontend/src/api/client.ts:25`
- Frontend now uses canonical signing path aligned to backend request URI contract (`/api/v1` prefix included).
- `frontend/src/api/client.ts:43`
- `frontend/src/api/client.ts:75`
- `backend/src/main/java/com/culinarycoach/security/filter/RequestSignatureFilter.java:130`
4) **MFA login flow incomplete (server/client placeholders)**  
**Status: Fixed**
- Server implements `/auth/mfa-verify` completion path.
- `backend/src/main/java/com/culinarycoach/web/controller/AuthController.java:51`
- `backend/src/main/java/com/culinarycoach/service/AuthService.java:137`
- Frontend now performs MFA verify and routes after success.
- `frontend/src/views/LoginView.vue:144`
- `frontend/src/stores/auth.ts:35`
5) **CAPTCHA after repeated failures not enforced server-side**  
**Status: Fixed (login flow)**
- Login now enforces CAPTCHA when threshold reached.
- `backend/src/main/java/com/culinarycoach/service/AuthService.java:87`
- `backend/src/main/java/com/culinarycoach/security/auth/LoginAttemptService.java:101`
6) **Object-level authorization gap in checkout transaction retrieval (IDOR)**  
**Status: Fixed**
- Controller passes authenticated userId.
- `backend/src/main/java/com/culinarycoach/web/controller/CheckoutController.java:69`
- Service now enforces transaction ownership.
- `backend/src/main/java/com/culinarycoach/service/CheckoutService.java:219`
7) **Parent/Coach role semantics missing in exposed API/UI**  
**Status: Fixed**
- Added review endpoints with role guards.
- `backend/src/main/java/com/culinarycoach/web/controller/ParentCoachController.java:16`
- Added admin assignment APIs.
- `backend/src/main/java/com/culinarycoach/web/controller/AdminReviewController.java:19`
- Added assignment model + migration.
- `backend/src/main/java/com/culinarycoach/domain/entity/ParentCoachAssignment.java:7`
- `backend/src/main/resources/db/migration/V13__create_parent_coach_assignments.sql:3`
- Added frontend review view + API.
- `frontend/src/views/ReviewView.vue:1`
- `frontend/src/api/review.ts:38`
8) **Offline audio download/playback flow incomplete**  
**Status: Fixed**
- Backend now performs real cache directory/file creation + checksum computation.
- `backend/src/main/java/com/culinarycoach/service/AudioCacheService.java:92`
- `backend/src/main/java/com/culinarycoach/service/AudioCacheService.java:149`
- Frontend now exposes download buttons, cache list with expiry labels, and cached playback controls.
- `frontend/src/views/AudioLibraryView.vue:113`
- `frontend/src/views/AudioLibraryView.vue:133`
- `frontend/src/views/AudioLibraryView.vue:161`
- `frontend/src/views/AudioLibraryView.vue:318`
9) **Frontend/backend cooking enum mismatch breaks UX**  
**Status: Fixed**
- Timer action comparisons updated to backend-aligned statuses.
- `frontend/src/views/CookingSessionView.vue:54`
- `frontend/src/views/CookingSessionView.vue:58`
- Active-state gate now uses shared backend-aligned enum constant.
- `frontend/src/views/CookingSessionView.vue:136`
10) **No project-specific setup/run/test documentation**  
**Status: Fixed**
- Root README now includes startup, env, tests, architecture, API groups.
- `README.md:19`
- `README.md:69`
- `README.md:94`
- `.env.example` added.
- `.env.example:1`
11) **Default admin credentials hardcoded and logged**  
**Status: Fixed**
- Bootstrap admin password now read from config/env, not hardcoded literal.
- `backend/src/main/java/com/culinarycoach/config/DataInitializer.java:28`
- No plaintext credential logging.
- `backend/src/main/java/com/culinarycoach/config/DataInitializer.java:69`
## Additional Observations Relevant to Prior Gaps
- Integration/API test coverage added for key previous risk areas (auth sessions, authorization, signed requests, MFA/CAPTCHA, audio cache):
  - `backend/src/test/java/com/culinarycoach/integration/AuthSessionIntegrationTest.java:37`
  - `backend/src/test/java/com/culinarycoach/integration/AuthorizationIntegrationTest.java:31`
  - `backend/src/test/java/com/culinarycoach/integration/SignedRequestIntegrationTest.java:41`
  - `backend/src/test/java/com/culinarycoach/integration/MfaCaptchaIntegrationTest.java:35`
  - `backend/src/test/java/com/culinarycoach/integration/AudioCacheIntegrationTest.java:28`
## Final Recheck Conclusion
- Most previously reported blockers/high issues are resolved in code structure and API implementation.
- The three previously partial issues are now resolved based on static evidence; all 11 tracked items are marked fixed in this recheck.
