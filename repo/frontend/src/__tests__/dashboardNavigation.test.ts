import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import { useAuthStore } from '@/stores/auth'

// Mock the auth API
vi.mock('@/api/auth', () => ({
  logout: vi.fn().mockResolvedValue(undefined),
  getCurrentUser: vi.fn().mockResolvedValue({
    userId: 1, username: 'test', authorities: ['ROLE_USER'],
  }),
}))

function createMountedDashboard(roles: string[]) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: DashboardView },
      { path: '/review', component: { template: '<div>Review</div>' } },
      { path: '/admin/tips', component: { template: '<div>Tips</div>' } },
      { path: '/admin/users', component: { template: '<div>Users</div>' } },
      { path: '/admin/observability', component: { template: '<div>Observability</div>' } },
      { path: '/login', component: { template: '<div>Login</div>' } },
      { path: '/notifications', component: { template: '<div>Notifications</div>' } },
      { path: '/mfa-setup', component: { template: '<div>MFA</div>' } },
      { path: '/change-password', component: { template: '<div>Password</div>' } },
    ],
  })

  const auth = useAuthStore()
  auth.isAuthenticated = true
  auth.username = 'testuser'
  auth.displayName = 'Test User'
  auth.roles = roles

  return mount(DashboardView, {
    global: {
      plugins: [pinia, router],
    },
  })
}

describe('DashboardView — role-based navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows Student Review card for ROLE_PARENT_COACH', () => {
    const wrapper = createMountedDashboard(['ROLE_USER', 'ROLE_PARENT_COACH'])
    const html = wrapper.html()
    expect(html).toContain('Student Review')
    expect(html).toContain('/review')
  })

  it('shows Student Review card for ROLE_ADMIN', () => {
    const wrapper = createMountedDashboard(['ROLE_USER', 'ROLE_ADMIN'])
    const html = wrapper.html()
    expect(html).toContain('Student Review')
    expect(html).toContain('/review')
  })

  it('hides Student Review card for regular ROLE_USER', () => {
    const wrapper = createMountedDashboard(['ROLE_USER'])
    const html = wrapper.html()
    expect(html).not.toContain('Student Review')
  })

  it('shows Tip Card Configuration for ROLE_ADMIN', () => {
    const wrapper = createMountedDashboard(['ROLE_USER', 'ROLE_ADMIN'])
    const html = wrapper.html()
    expect(html).toContain('Tip Card Configuration')
    expect(html).toContain('/admin/tips')
  })

  it('hides Tip Card Configuration for non-admin users', () => {
    const wrapper = createMountedDashboard(['ROLE_USER'])
    const html = wrapper.html()
    expect(html).not.toContain('Tip Card Configuration')
  })

  it('hides admin section entirely for ROLE_PARENT_COACH', () => {
    const wrapper = createMountedDashboard(['ROLE_USER', 'ROLE_PARENT_COACH'])
    const html = wrapper.html()
    expect(html).not.toContain('Tip Card Configuration')
    expect(html).not.toContain('User Management')
  })
})
