import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { guest: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/change-password',
    name: 'ChangePassword',
    component: () => import('@/views/PasswordChangeView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mfa-setup',
    name: 'MfaSetup',
    component: () => import('@/views/MfaSetupView.vue'),
    meta: { requiresAuth: true },
  },
  // Phase 2 - Audio Library
  {
    path: '/audio',
    name: 'AudioLibrary',
    component: () => import('@/views/AudioLibraryView.vue'),
    meta: { requiresAuth: true },
  },
  // Phase 3 - Cooking Sessions
  {
    path: '/cooking',
    name: 'CookingSessions',
    component: () => import('@/views/CookingSessionsView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/cooking/:id',
    name: 'CookingSession',
    component: () => import('@/views/CookingSessionView.vue'),
    meta: { requiresAuth: true },
  },
  // Phase 4 - Study & Notebook
  {
    path: '/study',
    name: 'Study',
    component: () => import('@/views/StudyView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notebook',
    name: 'Notebook',
    component: () => import('@/views/NotebookView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/NotificationsView.vue'),
    meta: { requiresAuth: true },
  },
  // Phase 5 - Checkout
  {
    path: '/checkout',
    name: 'Checkout',
    component: () => import('@/views/CheckoutView.vue'),
    meta: { requiresAuth: true },
  },
  // Parent/Coach Review
  {
    path: '/review',
    name: 'Review',
    component: () => import('@/views/ReviewView.vue'),
    meta: { requiresAuth: true },
  },
  // Admin routes
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('@/views/admin/UserManagementView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/observability',
    name: 'Observability',
    component: () => import('@/views/admin/ObservabilityView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/tips',
    name: 'TipManagement',
    component: () => import('@/views/admin/TipManagementView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: '/admin/reconciliation',
    name: 'Reconciliation',
    component: () => import('@/views/admin/ReconciliationView.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    // Try to check session first
    await auth.checkAuth()
    if (!auth.isAuthenticated) {
      return { name: 'Login', query: { redirect: to.fullPath } }
    }
  }

  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'Dashboard' }
  }

  if (to.meta.guest && auth.isAuthenticated) {
    return { name: 'Dashboard' }
  }
})

export default router
