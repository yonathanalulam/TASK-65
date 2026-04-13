import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { LoginRequest, LoginResponse } from '@/types/auth'

export const useAuthStore = defineStore('auth', () => {
  const userId = ref<number | null>(null)
  const username = ref<string | null>(null)
  const displayName = ref<string | null>(null)
  const roles = ref<string[]>([])
  const isAuthenticated = ref(false)
  const mfaRequired = ref(false)
  const mfaToken = ref<string | null>(null)
  const forcePasswordChange = ref(false)
  const loading = ref(false)
  const error = ref<string | null>(null)

  const isAdmin = computed(() => roles.value.includes('ROLE_ADMIN'))
  const isParentCoach = computed(() => roles.value.includes('ROLE_PARENT_COACH'))

  async function login(request: LoginRequest) {
    loading.value = true
    error.value = null
    try {
      const response = await authApi.login(request)
      handleLoginResponse(response)
    } catch (e: any) {
      error.value = e.response?.data?.error?.message ?? e.message ?? 'Login failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function completeMfaVerify(code: string) {
    if (!mfaToken.value) throw new Error('No MFA token')
    loading.value = true
    error.value = null
    try {
      const response = await authApi.verifyMfaLogin({
        code,
        mfaToken: mfaToken.value,
      })
      handleLoginResponse(response)
    } catch (e: any) {
      error.value = e.response?.data?.error?.message ?? e.message ?? 'MFA verification failed'
      throw e
    } finally {
      loading.value = false
    }
  }

  function handleLoginResponse(response: LoginResponse) {
    if (response.mfaRequired) {
      mfaRequired.value = true
      mfaToken.value = response.mfaToken
      return
    }

    userId.value = response.userId
    username.value = response.username
    displayName.value = response.displayName
    roles.value = response.roles ?? []
    isAuthenticated.value = true
    forcePasswordChange.value = response.forcePasswordChange
    mfaRequired.value = false
    mfaToken.value = null

    // Store session credentials for API signing
    if (response.signingKey) {
      sessionStorage.setItem('signingKey', response.signingKey)
    }
    if (response.sessionId) {
      sessionStorage.setItem('sessionId', response.sessionId)
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      $reset()
    }
  }

  async function checkAuth() {
    try {
      const user = await authApi.getCurrentUser()
      username.value = user.username
      userId.value = user.userId
      roles.value = user.authorities
      isAuthenticated.value = true
    } catch {
      $reset()
    }
  }

  function $reset() {
    userId.value = null
    username.value = null
    displayName.value = null
    roles.value = []
    isAuthenticated.value = false
    mfaRequired.value = false
    mfaToken.value = null
    forcePasswordChange.value = false
    error.value = null
    sessionStorage.removeItem('signingKey')
    sessionStorage.removeItem('sessionId')
  }

  return {
    userId, username, displayName, roles,
    isAuthenticated, mfaRequired, mfaToken,
    forcePasswordChange, loading, error,
    isAdmin, isParentCoach,
    login, completeMfaVerify, logout, checkAuth, handleLoginResponse, $reset,
  }
})
