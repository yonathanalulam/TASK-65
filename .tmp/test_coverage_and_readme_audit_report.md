# Test Coverage Audit

- Rerun status: refreshed via static inspection on 2026-04-16.
- Result delta vs prior run: no endpoint coverage regressions detected.

## Scope and Method

- Static inspection only (no execution).
- Inspected sources:
  - `backend/src/main/java/com/culinarycoach/web/controller/*.java`
  - `backend/src/test/java/com/culinarycoach/integration/*.java`
  - `backend/src/test/java/com/culinarycoach/security/**/*.java`
  - `backend/src/test/java/com/culinarycoach/integration/TestHelper.java`
  - `run_tests.sh`

## Backend Endpoint Inventory

Resolved method + full path (including controller prefixes).

1. `POST /api/v1/audio/cache/download/{segmentId}`
2. `GET /api/v1/audio/cache/status`
3. `DELETE /api/v1/audio/cache/{manifestId}`
4. `GET /api/v1/audio/cache/storage-meter`
5. `POST /api/v1/audio/cache/download-asset/{assetId}`
6. `GET /api/v1/audio/cache/{manifestId}/stream`
7. `GET /api/v1/audio/assets`
8. `GET /api/v1/audio/assets/{id}`
9. `GET /api/v1/audio/playlists`
10. `GET /api/v1/audio/playlists/{id}`
11. `POST /api/v1/audio/playlists`
12. `PUT /api/v1/audio/playlists/{id}`
13. `DELETE /api/v1/audio/playlists/{id}`
14. `POST /api/v1/audio/playlists/{id}/items`
15. `DELETE /api/v1/audio/playlists/{id}/items/{assetId}`
16. `PUT /api/v1/audio/playlists/{id}/items/reorder`
17. `GET /api/v1/audio/favorites`
18. `POST /api/v1/audio/favorites/{assetId}`
19. `DELETE /api/v1/audio/favorites/{assetId}`
20. `POST /api/v1/cooking/sessions`
21. `GET /api/v1/cooking/sessions`
22. `GET /api/v1/cooking/sessions/{id}`
23. `POST /api/v1/cooking/sessions/{id}/resume`
24. `POST /api/v1/cooking/sessions/{id}/pause`
25. `POST /api/v1/cooking/sessions/{id}/abandon`
26. `POST /api/v1/cooking/sessions/{id}/steps/{stepOrder}/complete`
27. `POST /api/v1/cooking/sessions/{sessionId}/timers`
28. `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/pause`
29. `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/resume`
30. `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/cancel`
31. `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/acknowledge`
32. `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/dismiss`
33. `GET /api/v1/questions`
34. `GET /api/v1/questions/{id}`
35. `POST /api/v1/questions/{id}/answer`
36. `GET /api/v1/notebook/entries`
37. `GET /api/v1/notebook/entries/{id}`
38. `POST /api/v1/notebook/entries/{id}/notes`
39. `POST /api/v1/notebook/entries/{id}/tags`
40. `DELETE /api/v1/notebook/entries/{id}/tags/{tagId}`
41. `POST /api/v1/notebook/entries/{id}/favorite`
42. `POST /api/v1/notebook/entries/{id}/resolve`
43. `POST /api/v1/notebook/entries/{id}/archive`
44. `POST /api/v1/notebook/entries/{id}/reactivate`
45. `POST /api/v1/drills/retry`
46. `POST /api/v1/drills/similar`
47. `POST /api/v1/drills/variant`
48. `POST /api/v1/drills/{drillId}/answer`
49. `POST /api/v1/drills/{drillId}/complete`
50. `GET /api/v1/notifications`
51. `GET /api/v1/notifications/unread-count`
52. `POST /api/v1/notifications/{id}/read`
53. `POST /api/v1/notifications/{id}/dismiss`
54. `GET /api/v1/checkout/bundles`
55. `POST /api/v1/checkout/initiate`
56. `POST /api/v1/checkout/complete/{transactionId}`
57. `GET /api/v1/checkout/transactions`
58. `GET /api/v1/checkout/transactions/{id}`
59. `POST /api/v1/admin/checkout/transactions/{id}/void`
60. `GET /api/v1/admin/checkout/reconciliation/export`
61. `GET /api/v1/admin/checkout/reconciliation/exports`
62. `GET /api/v1/admin/checkout/reconciliation/exports/{id}`
63. `GET /api/v1/admin/dashboard/jobs`
64. `GET /api/v1/admin/dashboard/jobs/{jobName}/runs`
65. `GET /api/v1/admin/dashboard/metrics`
66. `GET /api/v1/admin/dashboard/alerts`
67. `POST /api/v1/admin/dashboard/alerts/{id}/acknowledge`
68. `POST /api/v1/admin/dashboard/alerts/{id}/resolve`
69. `GET /api/v1/admin/dashboard/alerts/count`
70. `GET /api/v1/admin/dashboard/capacity`
71. `GET /api/v1/admin/dashboard/kpis`
72. `POST /api/v1/admin/review/assignments`
73. `DELETE /api/v1/admin/review/assignments`
74. `GET /api/v1/admin/review/assignments`
75. `GET /api/v1/review/students`
76. `GET /api/v1/review/students/{studentId}/notebook`
77. `GET /api/v1/review/students/{studentId}/attempts`
78. `GET /api/v1/review/students/{studentId}/cooking-history`
79. `POST /api/v1/admin/users`
80. `GET /api/v1/admin/users`
81. `GET /api/v1/admin/users/{id}`
82. `POST /api/v1/admin/users/{id}/disable`
83. `POST /api/v1/admin/users/{id}/enable`
84. `GET /api/v1/admin/tips`
85. `POST /api/v1/admin/tips/{id}/configure`
86. `POST /api/v1/admin/tips/{id}/toggle`
87. `GET /api/v1/auth/csrf-token`
88. `POST /api/v1/auth/login`
89. `POST /api/v1/auth/mfa-verify`
90. `POST /api/v1/auth/logout`
91. `POST /api/v1/auth/change-password`
92. `GET /api/v1/auth/me`
93. `POST /api/v1/mfa/setup`
94. `POST /api/v1/mfa/verify`
95. `POST /api/v1/mfa/disable`
96. `GET /api/v1/captcha/challenge`
97. `GET /api/v1/admin/privacy/access-logs`

Total endpoints: **97**.

## API Test Mapping Table

All endpoints below are covered by true no-mock HTTP tests (`@SpringBootTest` + `@AutoConfigureMockMvc`, no `@MockBean` in integration package).

| Endpoint | Covered | Test type | Test files | Evidence |
|---|---|---|---|---|
| `POST /api/v1/audio/cache/download/{segmentId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioCacheEndpointTest.java` | `downloadSegment_freeAssetSegment_returns200` |
| `GET /api/v1/audio/cache/status` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioCacheEndpointTest.java` | `getCacheStatus_returnsListResponse` |
| `DELETE /api/v1/audio/cache/{manifestId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ObjectLevelAuthorizationTest.java` | `audioCache_ownerCanDelete` |
| `GET /api/v1/audio/cache/storage-meter` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioCacheEndpointTest.java` | `getStorageMeter_returnsMeterWithQuota` |
| `POST /api/v1/audio/cache/download-asset/{assetId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioCacheEndpointTest.java` | `downloadAsset_freeAsset_returnsCacheEntries` |
| `GET /api/v1/audio/cache/{manifestId}/stream` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioCacheEndpointTest.java` | `streamCachedSegment_afterDownload_returnsAudioStream` |
| `GET /api/v1/audio/assets` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioLibraryEndpointTest.java` | `browseAssets_authenticated_returnsPaginatedList` |
| `GET /api/v1/audio/assets/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioLibraryEndpointTest.java` | `getAssetDetails_existingId_returnsAsset` |
| `GET /api/v1/audio/playlists` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `getUserPlaylists_initiallyEmpty` |
| `GET /api/v1/audio/playlists/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `getPlaylistDetail_afterCreate_returnsDetail` |
| `POST /api/v1/audio/playlists` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `createPlaylist_returnsCreatedPlaylist` |
| `PUT /api/v1/audio/playlists/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `updatePlaylist_changesName` |
| `DELETE /api/v1/audio/playlists/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `deletePlaylist_removesIt` |
| `POST /api/v1/audio/playlists/{id}/items` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `addItem_thenRemoveItem` |
| `DELETE /api/v1/audio/playlists/{id}/items/{assetId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `addItem_thenRemoveItem` |
| `PUT /api/v1/audio/playlists/{id}/items/reorder` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/PlaylistEndpointTest.java` | `reorderItems_changesOrder` |
| `GET /api/v1/audio/favorites` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioFavoritesEndpointTest.java` | `getFavorites_initiallyEmpty` |
| `POST /api/v1/audio/favorites/{assetId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioFavoritesEndpointTest.java` | `addFavorite_thenListContainsIt` |
| `DELETE /api/v1/audio/favorites/{assetId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AudioFavoritesEndpointTest.java` | `removeFavorite_thenListIsEmpty` |
| `POST /api/v1/cooking/sessions` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `createSession_returnsSessionResponse` |
| `GET /api/v1/cooking/sessions` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `listSessions_initiallyEmpty` |
| `GET /api/v1/cooking/sessions/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `getSessionDetail_hasSteps` |
| `POST /api/v1/cooking/sessions/{id}/resume` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `pauseAndResume_changesStatus` |
| `POST /api/v1/cooking/sessions/{id}/pause` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `pauseAndResume_changesStatus` |
| `POST /api/v1/cooking/sessions/{id}/abandon` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `abandonSession_changesStatus` |
| `POST /api/v1/cooking/sessions/{id}/steps/{stepOrder}/complete` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CookingSessionEndpointTest.java` | `completeStep_updatesSession` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `createTimer_returnsTimerResponse` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/pause` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `pauseTimer_changesStatusToPaused` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/resume` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `resumeTimer_changesStatusBackToRunning` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/cancel` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `cancelTimer_changesStatusToCancelled` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/acknowledge` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `acknowledgeTimer_elapsedTimer_changesStatusToAcknowledged` |
| `POST /api/v1/cooking/sessions/{sessionId}/timers/{timerId}/dismiss` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/TimerEndpointTest.java` | `dismissTimer_elapsedTimer_changesStatusToDismissed` |
| `GET /api/v1/questions` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/QuestionEndpointTest.java` | `listQuestions_authenticated_returnsList` |
| `GET /api/v1/questions/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/QuestionEndpointTest.java` | `getQuestion_existingId_returnsQuestion` |
| `POST /api/v1/questions/{id}/answer` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/QuestionEndpointTest.java` | `submitAnswer_correctAnswer_returnsCorrectClassification` |
| `GET /api/v1/notebook/entries` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `listEntries_afterWrongAnswer_hasContent` |
| `GET /api/v1/notebook/entries/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `getEntryDetail_returnsEntry` |
| `POST /api/v1/notebook/entries/{id}/notes` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `addNote_returnsUpdatedEntry` |
| `POST /api/v1/notebook/entries/{id}/tags` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `addTag_returnsUpdatedEntry` |
| `DELETE /api/v1/notebook/entries/{id}/tags/{tagId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `removeTag_afterAddingTag_returns200` |
| `POST /api/v1/notebook/entries/{id}/favorite` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `toggleFavorite_returns200` |
| `POST /api/v1/notebook/entries/{id}/resolve` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `resolveEntry_returns200` |
| `POST /api/v1/notebook/entries/{id}/archive` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `archiveEntry_returns200` |
| `POST /api/v1/notebook/entries/{id}/reactivate` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotebookEndpointTest.java` | `reactivateEntry_afterResolve_returns200` |
| `POST /api/v1/drills/retry` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/DrillEndpointTest.java` | `launchRetryDrill_returns200` |
| `POST /api/v1/drills/similar` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/DrillEndpointTest.java` | `launchSimilarDrill_returns200` |
| `POST /api/v1/drills/variant` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/DrillEndpointTest.java` | `launchVariantDrill_returns200` |
| `POST /api/v1/drills/{drillId}/answer` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/DrillEndpointTest.java` | `submitDrillAnswer_returns200` |
| `POST /api/v1/drills/{drillId}/complete` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/DrillEndpointTest.java` | `completeDrill_returns200` |
| `GET /api/v1/notifications` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotificationEndpointTest.java` | `listNotifications_authenticated_returns200` |
| `GET /api/v1/notifications/unread-count` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotificationEndpointTest.java` | `getUnreadCount_authenticated_returnsNumericValue` |
| `POST /api/v1/notifications/{id}/read` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotificationEndpointTest.java` | `markNotificationRead_returns200` |
| `POST /api/v1/notifications/{id}/dismiss` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/NotificationEndpointTest.java` | `dismissNotification_returns200` |
| `GET /api/v1/checkout/bundles` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CheckoutEndpointTest.java` | `listBundles_authenticated_returnsSeededBundles` |
| `POST /api/v1/checkout/initiate` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CheckoutEndpointTest.java` | `initiateCheckout_returns200WithTransaction` |
| `POST /api/v1/checkout/complete/{transactionId}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CheckoutEndpointTest.java` | `completeCheckout_returnsCompletedTransaction` |
| `GET /api/v1/checkout/transactions` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CheckoutEndpointTest.java` | `listTransactions_afterCheckout_hasContent` |
| `GET /api/v1/checkout/transactions/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/CheckoutEndpointTest.java` | `getTransactionDetail_returnsTransactionWithReceipt` |
| `POST /api/v1/admin/checkout/transactions/{id}/void` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ReconciliationEndpointTest.java` | `voidTransaction_admin_returns200` |
| `GET /api/v1/admin/checkout/reconciliation/export` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ReconciliationEndpointTest.java` | `generateExport_adminAllowed_withQueryParam` |
| `GET /api/v1/admin/checkout/reconciliation/exports` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ReconciliationEndpointTest.java` | `listExports_adminAllowed_returnsPage` |
| `GET /api/v1/admin/checkout/reconciliation/exports/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ReconciliationEndpointTest.java` | `getExportById_admin_returns200` |
| `GET /api/v1/admin/dashboard/jobs` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `listJobs_admin_returns200` |
| `GET /api/v1/admin/dashboard/jobs/{jobName}/runs` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `listJobRuns_admin_returns200` |
| `GET /api/v1/admin/dashboard/metrics` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `getMetrics_admin_returns200` |
| `GET /api/v1/admin/dashboard/alerts` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `listAlerts_admin_returns200` |
| `POST /api/v1/admin/dashboard/alerts/{id}/acknowledge` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `acknowledgeAlert_admin_returns200` |
| `POST /api/v1/admin/dashboard/alerts/{id}/resolve` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `resolveAlert_admin_returns200` |
| `GET /api/v1/admin/dashboard/alerts/count` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `getAlertCount_admin_returns200` |
| `GET /api/v1/admin/dashboard/capacity` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `getCapacityReport_admin_returns200` |
| `GET /api/v1/admin/dashboard/kpis` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `getKpiSummary_admin_returns200` |
| `POST /api/v1/admin/review/assignments` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `assignStudentToCoach` helper invocation |
| `DELETE /api/v1/admin/review/assignments` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `adminRevokeAssignment_returns200` |
| `GET /api/v1/admin/review/assignments` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `adminListAssignments_returns200` |
| `GET /api/v1/review/students` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `assignAndListStudents_coachSeesAssignedStudent` |
| `GET /api/v1/review/students/{studentId}/notebook` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `reviewStudentNotebook_coachCanReview` |
| `GET /api/v1/review/students/{studentId}/attempts` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `reviewStudentAttempts_coachCanReview` |
| `GET /api/v1/review/students/{studentId}/cooking-history` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/ParentCoachEndpointTest.java` | `reviewStudentCookingHistory_coachCanReview` |
| `POST /api/v1/admin/users` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/UserManagementEndpointTest.java` | `createUser_admin_returns201` |
| `GET /api/v1/admin/users` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/UserManagementEndpointTest.java` | `listUsers_admin_returns200` |
| `GET /api/v1/admin/users/{id}` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/UserManagementEndpointTest.java` | `getUser_admin_returns200` |
| `POST /api/v1/admin/users/{id}/disable` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/UserManagementEndpointTest.java` | `disableUser_admin_returns200` |
| `POST /api/v1/admin/users/{id}/enable` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/UserManagementEndpointTest.java` | `enableUser_admin_afterDisable_returns200` |
| `GET /api/v1/admin/tips` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `listTipCards_admin_returnsSeededTips` |
| `POST /api/v1/admin/tips/{id}/configure` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `configureTip_admin_returns200` |
| `POST /api/v1/admin/tips/{id}/toggle` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `toggleTip_admin_returns200` |
| `GET /api/v1/auth/csrf-token` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AuthCaptchaEndpointTest.java` | `getCsrfToken_returnsTokenSetStatus` |
| `POST /api/v1/auth/login` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AuthSessionIntegrationTest.java` | `login_validCredentials_returnsSessionId` |
| `POST /api/v1/auth/mfa-verify` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/MfaCaptchaIntegrationTest.java` | `mfaVerify_invalidCode_rejected` |
| `POST /api/v1/auth/logout` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AuthCaptchaEndpointTest.java` | `logout_validSession_returnsLoggedOut` |
| `POST /api/v1/auth/change-password` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/SignedRequestIntegrationTest.java` | `stateChangingRequest_missingAllSignatureHeaders_rejected` |
| `GET /api/v1/auth/me` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AuthSessionIntegrationTest.java` | `protectedRoute_withValidSession_returnsUserPrincipal` |
| `POST /api/v1/mfa/setup` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/MfaEndpointTest.java` | `setupMfa_authenticated_returnsQrCodeAndRecoveryCodes` |
| `POST /api/v1/mfa/verify` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/MfaEndpointTest.java` | `verifyMfa_withValidCode_returns200` |
| `POST /api/v1/mfa/disable` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/MfaEndpointTest.java` | `disableMfa_afterEnabling_returns200` |
| `GET /api/v1/captcha/challenge` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AuthCaptchaEndpointTest.java` | `getCaptchaChallenge_returnsImageAndChallengeId` |
| `GET /api/v1/admin/privacy/access-logs` | yes | true no-mock HTTP | `backend/src/test/java/com/culinarycoach/integration/AdminEndpointTest.java` | `listPrivacyAccessLogs_admin_returns200` |

## API Test Classification

1. **True No-Mock HTTP**
   - All endpoint-focused integration tests in `backend/src/test/java/com/culinarycoach/integration/*.java`.
   - Evidence pattern: `@SpringBootTest` + `@AutoConfigureMockMvc` + real request dispatch via `MockMvc` / `TestHelper`.

2. **HTTP with Mocking**
   - None detected.

3. **Non-HTTP (unit/integration without HTTP)**
   - `backend/src/test/java/com/culinarycoach/integration/AudioCacheIntegrationTest.java`
   - `backend/src/test/java/com/culinarycoach/security/mfa/TotpServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/captcha/CaptchaServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/nonce/NonceServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/ratelimit/RateLimitServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/auth/PasswordHistoryServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/auth/LoginAttemptServiceTest.java`
   - `backend/src/test/java/com/culinarycoach/security/auth/PasswordPolicyValidatorTest.java`

## Mock Detection Rules Check

- Integration/API tests: no `@MockBean`, no `@WebMvcTest`, no Mockito stubbing in request path.
- Unit tests with mocking detected:
  - `backend/src/test/java/com/culinarycoach/security/captcha/CaptchaServiceTest.java` (`@Mock CaptchaChallengeRepository`, `when(...)`)
  - `backend/src/test/java/com/culinarycoach/security/nonce/NonceServiceTest.java` (`@Mock NonceEntryRepository`, `when(...)`)
  - `backend/src/test/java/com/culinarycoach/security/auth/PasswordHistoryServiceTest.java` (`@Mock PasswordHistoryRepository`, `when(...)`)
  - `backend/src/test/java/com/culinarycoach/security/auth/LoginAttemptServiceTest.java` (`@Mock LoginAttemptRepository`, `@Mock UserRepository`, `@Mock AuditService`, `when(...)`)

## Coverage Summary

- Total endpoints: **97**
- Endpoints with HTTP tests: **97**
- Endpoints with true no-mock HTTP tests: **97**
- HTTP coverage: **100.00%**
- True API coverage: **100.00%**

## Unit Test Summary

- Unit/security test files:
  - `backend/src/test/java/com/culinarycoach/security/mfa/TotpServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/captcha/CaptchaServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/nonce/NonceServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/ratelimit/RateLimitServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/auth/PasswordHistoryServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/auth/LoginAttemptServiceTest.java`
  - `backend/src/test/java/com/culinarycoach/security/auth/PasswordPolicyValidatorTest.java`

- Modules covered:
  - Auth/password policy/history/login attempts
  - CAPTCHA, nonce, rate-limit, MFA crypto utility
  - Audio cache service integration (non-HTTP)

- Important modules still lacking dedicated unit tests:
  - `CheckoutService`, `ReconciliationService`, `ParentCoachService`, `NotebookService`, `DrillService`, `QuestionService`, `CookingSessionService`, `TimerService`, `NotificationService`, `TipCardService`, `UserService`, `AdminDashboard` service layer components.
  - Note: these are exercised via integration tests, but isolated unit-level failure localization remains limited.

## API Observability Check

- Endpoint visibility: strong (explicit method + path in tests).
- Request input visibility: strong (body/query/header values asserted in most endpoint tests).
- Response visibility: strong (status + key `jsonPath` assertions in most tests).
- Remaining weak spots:
  - Some negative-path tests use method-mismatched unauth checks (e.g., GET on POST-only endpoints) for auth denial checks; still useful for guard behavior but weaker for endpoint-contract specificity.

## Test Quality & Sufficiency

- Success paths: broad coverage across all endpoint groups.
- Failure/authorization paths: present for most endpoint groups.
- Validation/edge cases: present in several suites (signature, captcha, permissions, state transitions) but uneven depth across all endpoints.
- Assertions: generally meaningful; some remain status-heavy in denial scenarios.

## Tests Check

- `run_tests.sh` is Docker-based for both backend and frontend.
- Evidence: `docker build` + `docker run` for backend and frontend test images in `run_tests.sh`.
- Result against rule: **OK**.

## End-to-End Expectations

- Project is fullstack.
- Real FE↔BE E2E tests are not evident in inspected suite.
- Compensation: backend API coverage is complete at endpoint level, but this does not replace browser-level flow verification.

## Test Coverage Score (0-100)

**91/100**

## Score Rationale

- + Full endpoint inventory coverage with true no-mock HTTP tests.
- + Strong security-path testing (signature/captcha/session/authorization).
- - No true fullstack E2E coverage.
- - Unit tests are concentrated in security utilities; many core business services lack dedicated unit suites.
- - A few observability/negative-path tests are shallow or method-mismatched for strict contract semantics.

## Key Gaps

- No FE↔BE E2E/browser flow tests.
- Limited isolated unit tests for core business services outside security-centric modules.

## Confidence & Assumptions

- Confidence: **High** for endpoint mapping and direct coverage evidence.
- Assumptions:
  - Controller mappings in `web/controller` are the full API surface under audit.
  - Endpoint coverage is defined strictly by exact method+path invocation in tests.

---

# README Audit

- Rerun status: refreshed via static inspection on 2026-04-16.

## Project Type Detection

- Explicit declaration present at top: `fullstack` (`README.md:3`).

## README Location

- `repo/README.md` exists: **PASS**.

## Hard Gate Evaluation

### Formatting

- Markdown structure is clear and readable: **PASS**.

### Startup Instructions

- Includes required literal `docker-compose up` (`README.md:26`): **PASS**.

### Access Method

- Explicit URLs and ports provided for frontend/backend/adminer (`README.md:39-43`): **PASS**.

### Verification Method

- Deterministic backend curl verification with expected outputs and frontend flow steps provided (`README.md:98-137`): **PASS**.

### Environment Rules (strict Docker-contained rule)

- **PASS**.
- README now keeps required runtime path Docker-contained and moves local-dev guidance to optional external doc link (`README.md:154`).
- No local runtime install commands (`npm install`, `./mvnw`) remain in `README.md`.

### Demo Credentials (auth exists)

- Auth clearly exists.
- README provides admin credentials and explicit creation flow plus resulting credentials table for all roles (`README.md:45-97`): **PASS**.

## Engineering Quality

- Tech stack clarity: strong.
- Architecture explanation: strong.
- Testing instructions: strong for Docker path.
- Security/roles/workflow explanation: strong.
- Presentation quality: strong.

## High Priority Issues

- None.

## Medium Priority Issues

- Credential bootstrap script is shell-heavy and may be harder on non-Unix environments; not a hard-gate issue.

## Low Priority Issues

- Minor readability improvement opportunity: split credential bootstrap into a dedicated script file referenced by README.

## Hard Gate Failures

- None.

## README Verdict

**PASS**

## Final Verdicts

- **Test Coverage Audit Verdict:** PASS (endpoint-level coverage complete; quality gaps are non-blocking).
- **README Audit Verdict:** PASS (all strict hard gates satisfied in current README).
