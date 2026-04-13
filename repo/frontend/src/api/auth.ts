import client from './client'
import type { ApiResponse } from '@/types/api'
import type { LoginRequest, LoginResponse, PasswordChangeRequest, MfaSetupResponse, MfaVerifyRequest } from '@/types/auth'

export async function login(request: LoginRequest): Promise<LoginResponse> {
  const { data } = await client.post<ApiResponse<LoginResponse>>('/auth/login', request)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Login failed')
  }
  return data.data
}

export async function verifyMfaLogin(request: MfaVerifyRequest): Promise<LoginResponse> {
  const { data } = await client.post<ApiResponse<LoginResponse>>('/auth/mfa-verify', request)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'MFA verification failed')
  }
  return data.data
}

export async function logout(): Promise<void> {
  await client.post('/auth/logout')
}

export async function changePassword(request: PasswordChangeRequest): Promise<void> {
  const { data } = await client.post<ApiResponse<void>>('/auth/change-password', request)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Password change failed')
  }
}

export async function getCurrentUser(): Promise<{ userId: number; username: string; authorities: string[]; sessionId: string }> {
  const { data } = await client.get<ApiResponse<{ userId: number; username: string; authorities: string[]; sessionId: string }>>('/auth/me')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get current user')
  }
  return data.data
}

export async function setupMfa(): Promise<MfaSetupResponse> {
  const { data } = await client.post<ApiResponse<MfaSetupResponse>>('/mfa/setup')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'MFA setup failed')
  }
  return data.data
}

export async function verifyMfa(request: MfaVerifyRequest): Promise<boolean> {
  const { data } = await client.post<ApiResponse<{ verified: boolean }>>('/mfa/verify', request)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'MFA verification failed')
  }
  return data.data.verified
}

export async function disableMfa(): Promise<void> {
  await client.post('/mfa/disable')
}
