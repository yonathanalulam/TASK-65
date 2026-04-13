import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type {
  Question, AnswerResult, NotebookEntry, NotebookEntryDetail,
  DrillRun, Notification
} from '@/types/study'

export async function listQuestions(lessonId: number): Promise<PageResponse<Question>> {
  const { data } = await client.get<ApiResponse<PageResponse<Question>>>('/questions', {
    params: { lessonId },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list questions')
  }
  return data.data
}

export async function getQuestion(id: number): Promise<Question> {
  const { data } = await client.get<ApiResponse<Question>>(`/questions/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get question')
  }
  return data.data
}

export async function submitAnswer(
  questionId: number,
  answer: string,
  flagged = false
): Promise<AnswerResult> {
  const { data } = await client.post<ApiResponse<AnswerResult>>(`/questions/${questionId}/answer`, {
    userAnswer: answer,
    flagged,
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to submit answer')
  }
  return data.data
}

export async function listNotebookEntries(status?: string): Promise<PageResponse<NotebookEntry>> {
  const { data } = await client.get<ApiResponse<PageResponse<NotebookEntry>>>('/notebook/entries', {
    params: { status },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list notebook entries')
  }
  return data.data
}

export async function getNotebookEntry(id: number): Promise<NotebookEntryDetail> {
  const { data } = await client.get<ApiResponse<NotebookEntryDetail>>(`/notebook/entries/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get notebook entry')
  }
  return data.data
}

export async function addNote(entryId: number, noteText: string): Promise<NotebookEntryDetail> {
  const { data } = await client.post<ApiResponse<NotebookEntryDetail>>(
    `/notebook/entries/${entryId}/notes`,
    { noteText }
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to add note')
  }
  return data.data
}

export async function addTag(entryId: number, tagLabel: string): Promise<NotebookEntryDetail> {
  const { data } = await client.post<ApiResponse<NotebookEntryDetail>>(
    `/notebook/entries/${entryId}/tags`,
    { tagLabel }
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to add tag')
  }
  return data.data
}

export async function removeTag(entryId: number, tagId: number): Promise<NotebookEntryDetail> {
  const { data } = await client.delete<ApiResponse<NotebookEntryDetail>>(
    `/notebook/entries/${entryId}/tags/${tagId}`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to remove tag')
  }
  return data.data
}

export async function toggleFavorite(entryId: number): Promise<NotebookEntry> {
  const { data } = await client.post<ApiResponse<NotebookEntry>>(
    `/notebook/entries/${entryId}/favorite`
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to toggle favorite')
  }
  return data.data
}

export async function launchDrill(
  type: 'retry' | 'similar' | 'variant',
  entryId: number
): Promise<DrillRun> {
  const { data } = await client.post<ApiResponse<DrillRun>>(`/drills/${type}`, { entryId })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to launch drill')
  }
  return data.data
}

export async function listNotifications(): Promise<PageResponse<Notification>> {
  const { data } = await client.get<ApiResponse<PageResponse<Notification>>>('/notifications')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list notifications')
  }
  return data.data
}

export async function getUnreadCount(): Promise<number> {
  const { data } = await client.get<ApiResponse<{ unreadCount: number }>>('/notifications/unread-count')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get unread count')
  }
  return data.data.unreadCount
}

export async function markNotificationRead(id: number): Promise<Notification> {
  const { data } = await client.post<ApiResponse<Notification>>(`/notifications/${id}/read`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to mark notification read')
  }
  return data.data
}

export async function dismissNotification(id: number): Promise<Notification> {
  const { data } = await client.post<ApiResponse<Notification>>(`/notifications/${id}/dismiss`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to dismiss notification')
  }
  return data.data
}
