import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'

/**
 * Reconciliation export API client.
 *
 * Backend contract (AdminCheckoutController):
 *   GET /api/v1/admin/checkout/reconciliation/exports       — list exports (pageable)
 *   GET /api/v1/admin/checkout/reconciliation/export        — generate export (query param: businessDate)
 *   GET /api/v1/admin/checkout/reconciliation/exports/{id}  — get single export
 */

export interface ReconciliationExport {
  id: number
  businessDate: string
  exportVersion: number
  filePath: string
  fileChecksum: string
  transactionCount: number
  totalCompletedAmount: number
  totalVoidedAmount: number
  generatedBy: string
  generatedAt: string
}

export async function listExports(page = 0): Promise<PageResponse<ReconciliationExport>> {
  const { data } = await client.get<ApiResponse<PageResponse<ReconciliationExport>>>(
    '/admin/checkout/reconciliation/exports', { params: { page, size: 20 } }
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list exports')
  }
  return data.data
}

export async function triggerExport(businessDate: string): Promise<ReconciliationExport> {
  const { data } = await client.get<ApiResponse<ReconciliationExport>>(
    '/admin/checkout/reconciliation/export', { params: { businessDate } }
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to trigger export')
  }
  return data.data
}
