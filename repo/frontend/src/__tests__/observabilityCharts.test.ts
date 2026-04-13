import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import ObservabilityView from '@/views/admin/ObservabilityView.vue'

// Mock axios client used by ObservabilityView
vi.mock('@/api/client', () => {
  const mockGet = vi.fn((url: string) => {
    if (url === '/admin/dashboard/kpis') {
      return Promise.resolve({ data: { success: true, data: { requestThroughput: 120, errorRate: 2.5, p50Latency: 45, p95Latency: 200, windowStart: null, windowEnd: null } } })
    }
    if (url === '/admin/dashboard/capacity') {
      return Promise.resolve({ data: { success: true, data: { totalUsers: 10, activeSessions: 3, totalAudioCacheBytes: 100000, totalTransactions: 50, pendingNotifications: 5, reportTime: '2026-01-01T00:00:00Z' } } })
    }
    if (url === '/admin/dashboard/alerts') {
      return Promise.resolve({ data: { success: true, data: { content: [] } } })
    }
    if (url === '/admin/dashboard/alerts/count') {
      return Promise.resolve({ data: { success: true, data: { openAlerts: 0 } } })
    }
    if (url === '/admin/dashboard/jobs') {
      return Promise.resolve({ data: { success: true, data: [] } })
    }
    if (url === '/admin/dashboard/metric-snapshots') {
      return Promise.resolve({
        data: {
          success: true,
          data: [
            { id: 1, metricName: 'error_rate', metricValue: 1.2, recordedAt: '2026-01-01T00:00:00Z' },
            { id: 2, metricName: 'error_rate', metricValue: 2.5, recordedAt: '2026-01-01T01:00:00Z' },
            { id: 3, metricName: 'error_rate', metricValue: 0.8, recordedAt: '2026-01-01T02:00:00Z' },
            { id: 4, metricName: 'request_throughput', metricValue: 100, recordedAt: '2026-01-01T00:00:00Z' },
            { id: 5, metricName: 'request_throughput', metricValue: 150, recordedAt: '2026-01-01T01:00:00Z' },
            { id: 6, metricName: 'request_throughput', metricValue: 120, recordedAt: '2026-01-01T02:00:00Z' },
          ],
        },
      })
    }
    return Promise.resolve({ data: { success: true, data: null } })
  })

  return {
    default: {
      get: mockGet,
      post: vi.fn().mockResolvedValue({ data: { success: true } }),
    },
  }
})

function createWrapper() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>Home</div>' } },
      { path: '/admin/observability', component: ObservabilityView },
    ],
  })
  return mount(ObservabilityView, { global: { plugins: [pinia, router] } })
}

describe('ObservabilityView — metrics charts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders chart SVG elements when metric snapshot data is available', async () => {
    const wrapper = createWrapper()
    await flushPromises()

    const svgs = wrapper.findAll('.trend-chart')
    expect(svgs.length).toBe(2) // Error Rate + Throughput
  })

  it('renders error rate trend chart with data points', async () => {
    const wrapper = createWrapper()
    await flushPromises()

    const circles = wrapper.findAll('circle[fill="#dc3545"]')
    expect(circles.length).toBe(3) // 3 error_rate data points
  })

  it('renders throughput trend chart with data points', async () => {
    const wrapper = createWrapper()
    await flushPromises()

    const circles = wrapper.findAll('circle[fill="#667eea"]')
    expect(circles.length).toBe(3) // 3 throughput data points
  })

  it('does not contain placeholder text', async () => {
    const wrapper = createWrapper()
    await flushPromises()

    const html = wrapper.html()
    expect(html).not.toContain('Charts will be rendered here')
    expect(html).not.toContain('Integrate with Chart.js')
  })

  it('shows empty state when no snapshots available', async () => {
    const client = await import('@/api/client')
    vi.mocked(client.default.get).mockImplementation((url: string) => {
      if (url === '/admin/dashboard/metric-snapshots') {
        return Promise.resolve({ data: { success: true, data: [] } })
      }
      if (url === '/admin/dashboard/kpis') {
        return Promise.resolve({ data: { success: true, data: null } })
      }
      if (url === '/admin/dashboard/capacity') {
        return Promise.resolve({ data: { success: true, data: null } })
      }
      if (url === '/admin/dashboard/alerts') {
        return Promise.resolve({ data: { success: true, data: { content: [] } } })
      }
      if (url === '/admin/dashboard/alerts/count') {
        return Promise.resolve({ data: { success: true, data: { openAlerts: 0 } } })
      }
      if (url === '/admin/dashboard/jobs') {
        return Promise.resolve({ data: { success: true, data: [] } })
      }
      return Promise.resolve({ data: { success: true, data: null } })
    })

    const wrapper = createWrapper()
    await flushPromises()

    expect(wrapper.text()).toContain('No metric snapshot data available')
  })
})
