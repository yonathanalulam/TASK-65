import client from './client'
import type { ApiResponse } from '@/types/api'
import type { CookingSession, CookingSessionDetail, SessionTimer } from '@/types/cooking'

export async function listSessions(): Promise<CookingSession[]> {
  const { data } = await client.get<ApiResponse<CookingSession[]>>('/cooking/sessions')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list sessions')
  }
  return data.data
}

export async function startSession(
  recipeTitle: string,
  lessonId: number | null,
  steps: { title: string; description?: string; stepOrder: number; expectedDurationSeconds?: number; hasTimer?: boolean; timerDurationSeconds?: number }[]
): Promise<CookingSession> {
  const { data } = await client.post<ApiResponse<CookingSession>>('/cooking/sessions', {
    recipeTitle,
    lessonId,
    steps,
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to start session')
  }
  return data.data
}

export async function getSession(id: number): Promise<CookingSessionDetail> {
  const { data } = await client.get<ApiResponse<CookingSessionDetail>>(`/cooking/sessions/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get session')
  }
  return data.data
}

export async function resumeSession(id: number): Promise<CookingSessionDetail> {
  const { data } = await client.post<ApiResponse<CookingSessionDetail>>(`/cooking/sessions/${id}/resume`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to resume session')
  }
  return data.data
}

export async function pauseSession(id: number): Promise<void> {
  const { data } = await client.post<ApiResponse<void>>(`/cooking/sessions/${id}/pause`)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Failed to pause session')
  }
}

export async function abandonSession(id: number): Promise<void> {
  const { data } = await client.post<ApiResponse<void>>(`/cooking/sessions/${id}/abandon`)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Failed to abandon session')
  }
}

export async function completeStep(sessionId: number, stepOrder: number): Promise<CookingSessionDetail> {
  const { data } = await client.post<ApiResponse<CookingSessionDetail>>(
    `/cooking/sessions/${sessionId}/steps/${stepOrder}/complete`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to complete step')
  }
  return data.data
}

export async function createTimer(
  sessionId: number,
  stepId: number | null,
  label: string,
  durationSeconds: number
): Promise<SessionTimer> {
  const { data } = await client.post<ApiResponse<SessionTimer>>(
    `/cooking/sessions/${sessionId}/timers`,
    { stepId, label, durationSeconds }
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to create timer')
  }
  return data.data
}

export async function pauseTimer(sessionId: number, timerId: number): Promise<SessionTimer> {
  const { data } = await client.post<ApiResponse<SessionTimer>>(
    `/cooking/sessions/${sessionId}/timers/${timerId}/pause`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to pause timer')
  }
  return data.data
}

export async function resumeTimer(sessionId: number, timerId: number): Promise<SessionTimer> {
  const { data } = await client.post<ApiResponse<SessionTimer>>(
    `/cooking/sessions/${sessionId}/timers/${timerId}/resume`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to resume timer')
  }
  return data.data
}

export async function acknowledgeTimer(sessionId: number, timerId: number): Promise<SessionTimer> {
  const { data } = await client.post<ApiResponse<SessionTimer>>(
    `/cooking/sessions/${sessionId}/timers/${timerId}/acknowledge`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to acknowledge timer')
  }
  return data.data
}

export async function dismissTimer(sessionId: number, timerId: number): Promise<SessionTimer> {
  const { data } = await client.post<ApiResponse<SessionTimer>>(
    `/cooking/sessions/${sessionId}/timers/${timerId}/dismiss`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to dismiss timer')
  }
  return data.data
}
