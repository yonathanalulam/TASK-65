import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'

const client: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
})

function getCsrfToken(): string | null {
  const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : null
}

function generateUUID(): string {
  return crypto.randomUUID?.() ??
    'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0
      return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
    })
}

async function computeHmacSignature(payload: string, keyBase64: string): Promise<string> {
  const keyBytes = Uint8Array.from(atob(keyBase64), c => c.charCodeAt(0))
  const key = await crypto.subtle.importKey(
    'raw', keyBytes, { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
  )
  const data = new TextEncoder().encode(payload)
  const signature = await crypto.subtle.sign('HMAC', key, data)
  return btoa(String.fromCharCode(...new Uint8Array(signature)))
}

/**
 * Canonical signing path: ensures the path sent to HMAC always matches
 * the full request URI the backend sees (i.e., includes the /api/v1 prefix).
 * The backend's RequestSignatureFilter verifies against request.getRequestURI(),
 * which is always the absolute path including /api/v1.
 */
const API_BASE = '/api/v1'

export function canonicalSigningPath(url: string | undefined): string {
  const raw = url ?? ''
  if (raw.startsWith(API_BASE + '/') || raw === API_BASE) {
    return raw
  }
  return API_BASE + (raw.startsWith('/') ? '' : '/') + raw
}

// Request interceptor: CSRF, session ID, idempotency, signing
client.interceptors.request.use(async (config: InternalAxiosRequestConfig) => {
  // CSRF token
  const csrf = getCsrfToken()
  if (csrf) {
    config.headers['X-XSRF-TOKEN'] = csrf
  }

  // Session ID header (always send if we have one)
  const sessionId = sessionStorage.getItem('sessionId')
  if (sessionId) {
    config.headers['X-Session-Id'] = sessionId
  }

  // Idempotency key for state-changing methods
  const method = (config.method ?? 'get').toUpperCase()
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    config.headers['X-Idempotency-Key'] = generateUUID()

    // Signed request: compute real HMAC for state-changing methods
    const signingKey = sessionStorage.getItem('signingKey')
    if (signingKey && sessionId) {
      const timestamp = new Date().toISOString()
      const nonce = generateUUID()
      const path = canonicalSigningPath(config.url)
      const payload = `${method}\n${path}\n${timestamp}\n${nonce}`

      try {
        const signature = await computeHmacSignature(payload, signingKey)
        config.headers['X-Timestamp'] = timestamp
        config.headers['X-Nonce'] = nonce
        config.headers['X-Signature'] = signature
      } catch (e) {
        console.error('Failed to compute request signature', e)
      }
    }
  }

  return config
})

// Response interceptor: handle auth errors and degraded-mode tracking
client.interceptors.response.use(
  (response: AxiosResponse) => {
    // Successful response clears degraded-mode failure counter
    try {
      const { useDegradedModeStore } = require('@/stores/degradedMode')
      useDegradedModeStore().recordSuccess()
    } catch { /* store may not be initialized during early requests */ }
    return response
  },
  (error: any) => {
    if (error.response) {
      const status = error.response.status

      if (status === 401) {
        sessionStorage.clear()
        if (window.location.pathname !== '/login') {
          window.location.href = '/login'
        }
      }

      if (status === 429) {
        const retryAfter = error.response.headers['retry-after']
        console.warn(`Rate limited. Retry after ${retryAfter}s`)
      }
    }

    // Network errors (no response) or 5xx errors count toward degraded mode
    const isNetworkOrServerError = !error.response || (error.response.status >= 500)
    if (isNetworkOrServerError) {
      try {
        const { useDegradedModeStore } = require('@/stores/degradedMode')
        useDegradedModeStore().recordFailure()
      } catch { /* store may not be initialized */ }
    }

    return Promise.reject(error)
  }
)

export async function initCsrf(): Promise<void> {
  await client.get('/auth/csrf-token')
}

export default client
