import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import NotificationsView from '@/views/NotificationsView.vue'
import { isUnread, canMarkRead, canDismiss } from '@/api/notificationStatus'

// ── Unit tests for the centralized notification status mapping ───────

describe('notificationStatus helpers', () => {
  it('GENERATED is unread', () => {
    expect(isUnread('GENERATED')).toBe(true)
  })

  it('DELIVERED is unread', () => {
    expect(isUnread('DELIVERED')).toBe(true)
  })

  it('READ is not unread', () => {
    expect(isUnread('READ')).toBe(false)
  })

  it('DISMISSED is not unread', () => {
    expect(isUnread('DISMISSED')).toBe(false)
  })

  it('EXPIRED is not unread', () => {
    expect(isUnread('EXPIRED')).toBe(false)
  })

  it('SUPPRESSED is not unread', () => {
    expect(isUnread('SUPPRESSED')).toBe(false)
  })

  it('canMarkRead is true for GENERATED', () => {
    expect(canMarkRead('GENERATED')).toBe(true)
  })

  it('canMarkRead is true for DELIVERED', () => {
    expect(canMarkRead('DELIVERED')).toBe(true)
  })

  it('canMarkRead is false for READ', () => {
    expect(canMarkRead('READ')).toBe(false)
  })

  it('canMarkRead is false for DISMISSED', () => {
    expect(canMarkRead('DISMISSED')).toBe(false)
  })

  it('canDismiss is true for GENERATED', () => {
    expect(canDismiss('GENERATED')).toBe(true)
  })

  it('canDismiss is true for DELIVERED', () => {
    expect(canDismiss('DELIVERED')).toBe(true)
  })

  it('canDismiss is true for READ', () => {
    expect(canDismiss('READ')).toBe(true)
  })

  it('canDismiss is false for DISMISSED', () => {
    expect(canDismiss('DISMISSED')).toBe(false)
  })

  it('canDismiss is false for EXPIRED', () => {
    expect(canDismiss('EXPIRED')).toBe(false)
  })

  it('canDismiss is false for SUPPRESSED', () => {
    expect(canDismiss('SUPPRESSED')).toBe(false)
  })
})

// ── Component-level tests for NotificationsView ─────────────────────

vi.mock('@/api/questions', () => ({
  listNotifications: vi.fn().mockResolvedValue({
    content: [
      { id: 1, type: 'TIMER_ALERT', title: 'Timer done', message: null, status: 'GENERATED', priority: 1, createdAt: '2026-01-01T00:00:00Z' },
      { id: 2, type: 'DRILL_COMPLETE', title: 'Drill results', message: 'You scored 80%', status: 'DELIVERED', priority: 1, createdAt: '2026-01-01T01:00:00Z' },
      { id: 3, type: 'SYSTEM', title: 'Welcome', message: null, status: 'READ', priority: 1, createdAt: '2026-01-01T02:00:00Z' },
      { id: 4, type: 'SYSTEM', title: 'Old notice', message: null, status: 'DISMISSED', priority: 1, createdAt: '2026-01-01T03:00:00Z' },
    ],
  }),
  getUnreadCount: vi.fn().mockResolvedValue(2),
  markNotificationRead: vi.fn().mockResolvedValue({}),
  dismissNotification: vi.fn().mockResolvedValue({}),
}))

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/notifications', component: NotificationsView },
    ],
  })
  return mount(NotificationsView, { global: { plugins: [pinia, router] } })
}

describe('NotificationsView — backend status mapping', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('GENERATED notification renders as unread (has unread class)', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    // First item has status GENERATED — should have unread class
    expect(items[0].classes()).toContain('unread')
  })

  it('DELIVERED notification renders as unread (has unread class)', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    // Second item has status DELIVERED — should have unread class
    expect(items[1].classes()).toContain('unread')
  })

  it('READ notification renders as read (no unread class)', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    // Third item has status READ — should NOT have unread class
    expect(items[2].classes()).not.toContain('unread')
  })

  it('Mark Read button visible for GENERATED status', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    expect(items[0].find('.action-btn').text()).toBe('Mark Read')
  })

  it('Mark Read button visible for DELIVERED status', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    const btns = items[1].findAll('.action-btn')
    expect(btns.some(b => b.text() === 'Mark Read')).toBe(true)
  })

  it('Mark Read button NOT visible for READ status', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    const btns = items[2].findAll('.action-btn')
    expect(btns.some(b => b.text() === 'Mark Read')).toBe(false)
  })

  it('DISMISSED notification shows dismissed label', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const items = wrapper.findAll('.notification-item')
    expect(items[3].text()).toContain('Dismissed')
  })
})
