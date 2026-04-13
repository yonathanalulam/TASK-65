import client from './client'
import type { ApiResponse } from '@/types/api'

export interface ReviewStudent {
  userId: number
  username: string
  displayName: string | null
}

export interface ReviewNotebookEntry {
  id: number
  questionText: string
  status: string
  failCount: number
  isFavorite: boolean
  latestNote: string | null
  lastAttemptAt: string
}

export interface ReviewAttempt {
  id: number
  questionText: string
  userAnswer: string
  classification: string
  attemptedAt: string
}

export interface ReviewCookingSession {
  id: number
  recipeTitle: string
  status: string
  totalSteps: number
  lastCompletedStepOrder: number
  startedAt: string | null
  completedAt: string | null
}

export async function listAssignedStudents(): Promise<ReviewStudent[]> {
  const { data } = await client.get<ApiResponse<ReviewStudent[]>>('/review/students')
  if (!data.success || !data.data) throw new Error(data.error?.message ?? 'Failed to load students')
  return data.data
}

export async function reviewNotebook(studentId: number, reason: string): Promise<ReviewNotebookEntry[]> {
  const { data } = await client.get<ApiResponse<ReviewNotebookEntry[]>>(
    `/review/students/${studentId}/notebook`, { params: { reason } })
  if (!data.success || !data.data) throw new Error(data.error?.message ?? 'Failed to load notebook')
  return data.data
}

export async function reviewAttempts(studentId: number, reason: string): Promise<ReviewAttempt[]> {
  const { data } = await client.get<ApiResponse<ReviewAttempt[]>>(
    `/review/students/${studentId}/attempts`, { params: { reason } })
  if (!data.success || !data.data) throw new Error(data.error?.message ?? 'Failed to load attempts')
  return data.data
}

export async function reviewCookingHistory(studentId: number, reason: string): Promise<ReviewCookingSession[]> {
  const { data } = await client.get<ApiResponse<ReviewCookingSession[]>>(
    `/review/students/${studentId}/cooking-history`, { params: { reason } })
  if (!data.success || !data.data) throw new Error(data.error?.message ?? 'Failed to load cooking history')
  return data.data
}
