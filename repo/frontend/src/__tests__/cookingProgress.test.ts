import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CookingSessionView from '@/views/CookingSessionView.vue'

function makeSession(lastCompletedStepOrder: number, totalSteps: number) {
  const steps = Array.from({ length: totalSteps }, (_, i) => ({
    id: i + 1,
    stepOrder: i,
    title: `Step ${i + 1}`,
    description: null,
    expectedDurationSeconds: null,
    hasTimer: false,
    timerDurationSeconds: null,
    reminderText: null,
    completed: i <= lastCompletedStepOrder,
    completedAt: i <= lastCompletedStepOrder ? '2026-01-01T00:01:00Z' : null,
    tips: [],
  }))

  return {
    id: 1,
    recipeTitle: 'Progress Test Recipe',
    lessonId: null,
    status: 'ACTIVE',
    totalSteps,
    lastCompletedStepOrder,
    startedAt: '2026-01-01T00:00:00Z',
    resumedAt: null,
    completedAt: null,
    abandonedAt: null,
    lastActivityAt: null,
    steps,
    timers: [],
  }
}

let currentSession = makeSession(-1, 5)

vi.mock('@/api/cooking', () => ({
  getSession: vi.fn(() => Promise.resolve(currentSession)),
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

describe('CookingSessionView — progress indicator', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('session start (lastCompletedStepOrder=-1) shows 0 completed and 0%', async () => {
    currentSession = makeSession(-1, 5)
    const wrapper = createWrapper()
    await flushPromises()

    const progressLabel = wrapper.find('.progress-label')
    expect(progressLabel.text()).toContain('0 / 5 steps')

    // Check percentage via progress fill width
    const progressFill = wrapper.find('.progress-fill')
    const style = progressFill.attributes('style')
    expect(style).toContain('width: 0%')
  })

  it('first completed step (lastCompletedStepOrder=0) shows 1 completed and 20%', async () => {
    currentSession = makeSession(0, 5)
    const wrapper = createWrapper()
    await flushPromises()

    const progressLabel = wrapper.find('.progress-label')
    expect(progressLabel.text()).toContain('1 / 5 steps')

    const progressFill = wrapper.find('.progress-fill')
    const style = progressFill.attributes('style')
    expect(style).toContain('width: 20%')
  })

  it('all steps completed shows 100%', async () => {
    currentSession = makeSession(4, 5)
    const wrapper = createWrapper()
    await flushPromises()

    const progressLabel = wrapper.find('.progress-label')
    expect(progressLabel.text()).toContain('5 / 5 steps')

    const progressFill = wrapper.find('.progress-fill')
    const style = progressFill.attributes('style')
    expect(style).toContain('width: 100%')
  })

  it('percentage never goes negative even with unexpected backend value', async () => {
    // Simulate an unexpected value lower than -1
    currentSession = makeSession(-2, 5)
    const wrapper = createWrapper()
    await flushPromises()

    const progressLabel = wrapper.find('.progress-label')
    // Math.max(0, -2 + 1) = 0
    expect(progressLabel.text()).toContain('0 / 5 steps')

    const progressFill = wrapper.find('.progress-fill')
    const style = progressFill.attributes('style')
    expect(style).toContain('width: 0%')
  })

  it('mid-session progress shows correct count and percentage', async () => {
    currentSession = makeSession(2, 4)
    const wrapper = createWrapper()
    await flushPromises()

    const progressLabel = wrapper.find('.progress-label')
    expect(progressLabel.text()).toContain('3 / 4 steps')

    const progressFill = wrapper.find('.progress-fill')
    const style = progressFill.attributes('style')
    expect(style).toContain('width: 75%')
  })
})
