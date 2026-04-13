import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CookingSessionView from '@/views/CookingSessionView.vue'
import { CookingSessionStatus, TimerStatus } from '@/types/enums'

function makeSession(status: string) {
  return {
    id: 1,
    recipeTitle: 'Test Recipe',
    lessonId: null,
    status,
    totalSteps: 3,
    lastCompletedStepOrder: 1,
    startedAt: '2026-01-01T00:00:00Z',
    resumedAt: null,
    completedAt: null,
    abandonedAt: null,
    lastActivityAt: null,
    steps: [
      { id: 1, stepOrder: 1, title: 'Step 1', description: null, expectedDurationSeconds: null, hasTimer: false, timerDurationSeconds: null, reminderText: null, completed: true, completedAt: '2026-01-01T00:01:00Z', tips: [] },
      { id: 2, stepOrder: 2, title: 'Step 2', description: 'Do the thing', expectedDurationSeconds: 60, hasTimer: true, timerDurationSeconds: 60, reminderText: null, completed: false, completedAt: null, tips: [] },
      { id: 3, stepOrder: 3, title: 'Step 3', description: null, expectedDurationSeconds: null, hasTimer: false, timerDurationSeconds: null, reminderText: null, completed: false, completedAt: null, tips: [] },
    ],
    timers: [],
  }
}

let mockSessionStatus = 'ACTIVE'

vi.mock('@/api/cooking', () => ({
  getSession: vi.fn(() => Promise.resolve(makeSession(mockSessionStatus))),
  completeStep: vi.fn(),
  resumeSession: vi.fn(),
  pauseSession: vi.fn(),
  abandonSession: vi.fn(),
  createTimer: vi.fn(),
  pauseTimer: vi.fn(),
  resumeTimer: vi.fn(),
  acknowledgeTimer: vi.fn(),
  dismissTimer: vi.fn(),
}))

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/cooking/:id', component: CookingSessionView },
    ],
  })
  router.push('/cooking/1')
  return mount(CookingSessionView, { global: { plugins: [pinia, router] } })
}

// ── Enum-level tests ──────────────────────────────────────────────

describe('CookingSessionStatus enum', () => {
  it('has ACTIVE status matching backend enum', () => {
    expect(CookingSessionStatus.ACTIVE).toBe('ACTIVE')
  })

  it('does not contain IN_PROGRESS (old incorrect value)', () => {
    expect(Object.values(CookingSessionStatus)).not.toContain('IN_PROGRESS')
  })

  it('has all expected backend statuses', () => {
    expect(CookingSessionStatus.CREATED).toBe('CREATED')
    expect(CookingSessionStatus.PAUSED).toBe('PAUSED')
    expect(CookingSessionStatus.COMPLETED).toBe('COMPLETED')
    expect(CookingSessionStatus.ABANDONED).toBe('ABANDONED')
    expect(CookingSessionStatus.EXPIRED).toBe('EXPIRED')
  })
})

describe('TimerStatus enum', () => {
  it('uses ELAPSED_PENDING_ACK, not FIRED', () => {
    expect(TimerStatus.ELAPSED_PENDING_ACK).toBe('ELAPSED_PENDING_ACK')
    expect(Object.values(TimerStatus)).not.toContain('FIRED')
  })
})

// ── Component-level tests ─────────────────────────────────────────

describe('CookingSessionView — ACTIVE session controls', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStatus = 'ACTIVE'
  })

  it('renders "Complete Step" button when status is ACTIVE', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).toContain('Complete Step')
  })

  it('renders "Start Timer" button for current step with timer when ACTIVE', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).toContain('Start Timer')
  })

  it('renders "Add Timer" section when ACTIVE', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).toContain('Add Timer')
  })

  it('renders Pause button when ACTIVE', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const pauseBtn = wrapper.findAll('button').find(b => b.text() === 'Pause')
    expect(pauseBtn).toBeTruthy()
  })
})

describe('CookingSessionView — COMPLETED session hides active controls', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStatus = 'COMPLETED'
  })

  it('does not render "Complete Step" button when COMPLETED', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).not.toContain('Complete Step')
  })

  it('does not render "Add Timer" section when COMPLETED', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).not.toContain('Add Timer')
  })
})

describe('CookingSessionView — IN_PROGRESS no longer controls visibility', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStatus = 'IN_PROGRESS'
  })

  it('IN_PROGRESS status does NOT show active controls (regression)', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    // IN_PROGRESS is not a valid backend status — active controls must NOT appear
    expect(wrapper.html()).not.toContain('Complete Step')
    expect(wrapper.html()).not.toContain('Add Timer')
  })
})

describe('CookingSessionView — PAUSED session controls', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockSessionStatus = 'PAUSED'
  })

  it('renders Resume button when PAUSED', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    const resumeBtn = wrapper.findAll('button').find(b => b.text() === 'Resume')
    expect(resumeBtn).toBeTruthy()
  })

  it('does not render step completion controls when PAUSED', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.html()).not.toContain('Complete Step')
  })
})
