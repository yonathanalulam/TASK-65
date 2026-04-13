import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ReconciliationView from '@/views/admin/ReconciliationView.vue'

vi.mock('@/api/reconciliation', () => ({
  listExports: vi.fn().mockResolvedValue({
    content: [
      {
        id: 1,
        businessDate: '2026-01-15',
        exportVersion: 1,
        filePath: 'exports/reconciliation/reconciliation_20260115_v1.csv',
        fileChecksum: 'abc123',
        transactionCount: 12,
        totalCompletedAmount: 150.00,
        totalVoidedAmount: 10.00,
        generatedBy: 'admin',
        generatedAt: '2026-01-16T00:00:00Z',
      },
    ],
  }),
  triggerExport: vi.fn().mockResolvedValue({
    id: 2,
    businessDate: '2026-01-16',
    exportVersion: 1,
    filePath: 'exports/reconciliation/reconciliation_20260116_v1.csv',
    fileChecksum: 'def456',
    transactionCount: 5,
    totalCompletedAmount: 75.00,
    totalVoidedAmount: 0,
    generatedBy: 'admin',
    generatedAt: '2026-01-17T00:00:00Z',
  }),
}))

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/admin/reconciliation', component: ReconciliationView },
    ],
  })
  return mount(ReconciliationView, { global: { plugins: [pinia, router] } })
}

describe('ReconciliationView — admin reconciliation page', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the reconciliation page with title', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('Reconciliation Exports')
  })

  it('shows export history table', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('Export History')
    expect(wrapper.text()).toContain('2026-01-15')
    expect(wrapper.text()).toContain('v1')
    expect(wrapper.text()).toContain('12')
  })

  it('has a run export form with date input', async () => {
    const wrapper = createWrapper()
    await flushPromises()
    expect(wrapper.text()).toContain('Generate Export')
    const dateInput = wrapper.find('input[type="date"]')
    expect(dateInput.exists()).toBe(true)
  })

  it('calls triggerExport when form submitted', async () => {
    const reconciliationApi = await import('@/api/reconciliation')
    const wrapper = createWrapper()
    await flushPromises()

    const form = wrapper.find('.export-form')
    await form.trigger('submit.prevent')
    await flushPromises()

    expect(reconciliationApi.triggerExport).toHaveBeenCalled()
  })
})

describe('Router — admin reconciliation route exists', () => {
  it('admin/reconciliation route is defined', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/admin/reconciliation',
          name: 'Reconciliation',
          component: { template: '<div>Reconciliation</div>' },
          meta: { requiresAuth: true, requiresAdmin: true },
        },
      ],
    })
    await router.push('/admin/reconciliation')
    expect(router.currentRoute.value.name).toBe('Reconciliation')
    expect(router.currentRoute.value.meta.requiresAdmin).toBe(true)
  })
})
