import client from './client'
import type { ApiResponse } from '@/types/api'

export interface TipCard {
  id: number
  title: string
  shortText: string | null
  detailedText: string | null
  displayMode: string | null
  enabled: boolean
}

export async function listTipCards(): Promise<TipCard[]> {
  const { data } = await client.get<ApiResponse<TipCard[]>>('/admin/tips')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list tip cards')
  }
  return data.data
}

export async function toggleTipCard(id: number): Promise<TipCard> {
  const { data } = await client.post<ApiResponse<TipCard>>(`/admin/tips/${id}/toggle`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to toggle tip card')
  }
  return data.data
}

export async function configureTipDisplayMode(
  id: number,
  scope: string,
  scopeId: number | null,
  displayMode: string
): Promise<TipCard> {
  const { data } = await client.post<ApiResponse<TipCard>>(`/admin/tips/${id}/configure`, {
    scope,
    scopeId,
    displayMode,
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to configure tip display mode')
  }
  return data.data
}
