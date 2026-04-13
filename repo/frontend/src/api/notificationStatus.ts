/**
 * Centralized mapping between backend NotificationStatus enum and frontend display semantics.
 *
 * Backend statuses: GENERATED, DELIVERED, READ, DISMISSED, EXPIRED, SUPPRESSED
 * Frontend display: unread (actionable) vs read (non-actionable)
 *
 * GENERATED and DELIVERED are both "unread" — the notification has not been seen by the user.
 * READ means the user has explicitly marked it as read.
 * DISMISSED, EXPIRED, SUPPRESSED are terminal non-actionable states.
 */

const UNREAD_STATUSES = new Set(['GENERATED', 'DELIVERED'])

export function isUnread(status: string): boolean {
  return UNREAD_STATUSES.has(status)
}

export function isActionable(status: string): boolean {
  return status !== 'DISMISSED' && status !== 'EXPIRED' && status !== 'SUPPRESSED'
}

export function canMarkRead(status: string): boolean {
  return UNREAD_STATUSES.has(status)
}

export function canDismiss(status: string): boolean {
  return status !== 'DISMISSED' && status !== 'EXPIRED' && status !== 'SUPPRESSED'
}
