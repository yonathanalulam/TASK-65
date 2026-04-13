/**
 * Shared enum constants mirroring backend Java enums.
 * Keep in sync with backend/src/main/java/com/culinarycoach/domain/enums/
 */

export const CookingSessionStatus = {
  CREATED: 'CREATED',
  ACTIVE: 'ACTIVE',
  PAUSED: 'PAUSED',
  COMPLETED: 'COMPLETED',
  ABANDONED: 'ABANDONED',
  EXPIRED: 'EXPIRED',
} as const

export type CookingSessionStatusType = typeof CookingSessionStatus[keyof typeof CookingSessionStatus]

export const TimerStatus = {
  RUNNING: 'RUNNING',
  PAUSED: 'PAUSED',
  ELAPSED_PENDING_ACK: 'ELAPSED_PENDING_ACK',
  ACKNOWLEDGED: 'ACKNOWLEDGED',
  DISMISSED: 'DISMISSED',
  CANCELLED: 'CANCELLED',
} as const

export type TimerStatusType = typeof TimerStatus[keyof typeof TimerStatus]

export const CacheEntryStatus = {
  NOT_CACHED: 'NOT_CACHED',
  DOWNLOADING: 'DOWNLOADING',
  CACHED_VALID: 'CACHED_VALID',
  EXPIRED: 'EXPIRED',
  CORRUPT: 'CORRUPT',
  DELETED: 'DELETED',
} as const

export type CacheEntryStatusType = typeof CacheEntryStatus[keyof typeof CacheEntryStatus]

export const NotebookEntryStatus = {
  ACTIVE: 'ACTIVE',
  FAVORITED: 'FAVORITED',
  RESOLVED: 'RESOLVED',
  ARCHIVED: 'ARCHIVED',
} as const

export type NotebookEntryStatusType = typeof NotebookEntryStatus[keyof typeof NotebookEntryStatus]

export const TransactionStatus = {
  INITIATED: 'INITIATED',
  COMPLETED: 'COMPLETED',
  VOIDED: 'VOIDED',
  FAILED: 'FAILED',
} as const

export type TransactionStatusType = typeof TransactionStatus[keyof typeof TransactionStatus]

export const AccountStatus = {
  PENDING_SETUP: 'PENDING_SETUP',
  ACTIVE: 'ACTIVE',
  LOCKED: 'LOCKED',
  DISABLED: 'DISABLED',
  DELETED_SOFT: 'DELETED_SOFT',
} as const

export type AccountStatusType = typeof AccountStatus[keyof typeof AccountStatus]

export const AttemptClassification = {
  CORRECT: 'CORRECT',
  PARTIAL: 'PARTIAL',
  WRONG: 'WRONG',
  FLAGGED_BY_USER: 'FLAGGED_BY_USER',
} as const

export type AttemptClassificationType = typeof AttemptClassification[keyof typeof AttemptClassification]
