import { describe, it, expect } from 'vitest'
import { canonicalSigningPath } from '@/api/client'

describe('canonicalSigningPath', () => {
  it('prepends /api/v1 to a relative path starting with /', () => {
    expect(canonicalSigningPath('/auth/change-password')).toBe('/api/v1/auth/change-password')
  })

  it('prepends /api/v1/ to a relative path without leading /', () => {
    expect(canonicalSigningPath('auth/login')).toBe('/api/v1/auth/login')
  })

  it('does not double-prefix when path already starts with /api/v1/', () => {
    expect(canonicalSigningPath('/api/v1/cooking/sessions')).toBe('/api/v1/cooking/sessions')
  })

  it('handles undefined url gracefully', () => {
    expect(canonicalSigningPath(undefined)).toBe('/api/v1/')
  })

  it('handles empty string', () => {
    expect(canonicalSigningPath('')).toBe('/api/v1/')
  })

  it('preserves the exact /api/v1 base without trailing path', () => {
    expect(canonicalSigningPath('/api/v1')).toBe('/api/v1')
  })

  // Regression: the old code signed "/auth/..." without /api/v1
  it('regression: /auth/change-password must include /api/v1 prefix', () => {
    const signed = canonicalSigningPath('/auth/change-password')
    expect(signed).toBe('/api/v1/auth/change-password')
    expect(signed).not.toBe('/auth/change-password')
  })
})
