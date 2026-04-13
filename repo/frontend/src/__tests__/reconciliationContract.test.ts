/**
 * Tests verifying the reconciliation API client calls the correct backend endpoints.
 *
 * These tests mock the axios client directly and import the real API functions,
 * ensuring the path, HTTP method, and param shape match the backend controller:
 *
 *   GET /api/v1/admin/checkout/reconciliation/exports  (pageable)
 *   GET /api/v1/admin/checkout/reconciliation/export   (query param: businessDate)
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'

const mockGet = vi.fn()
const mockPost = vi.fn()

vi.mock('@/api/client', () => ({
  default: {
    get: (...args: unknown[]) => mockGet(...args),
    post: (...args: unknown[]) => mockPost(...args),
  },
}))

import { listExports, triggerExport } from '@/api/reconciliation'

describe('reconciliation API client — contract alignment', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('listExports calls GET /admin/checkout/reconciliation/exports with page params', async () => {
    mockGet.mockResolvedValueOnce({
      data: { success: true, data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 } },
    })

    await listExports(2)

    expect(mockGet).toHaveBeenCalledTimes(1)
    expect(mockGet).toHaveBeenCalledWith(
      '/admin/checkout/reconciliation/exports',
      { params: { page: 2, size: 20 } },
    )
  })

  it('listExports defaults to page 0', async () => {
    mockGet.mockResolvedValueOnce({
      data: { success: true, data: { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 } },
    })

    await listExports()

    expect(mockGet).toHaveBeenCalledWith(
      '/admin/checkout/reconciliation/exports',
      { params: { page: 0, size: 20 } },
    )
  })

  it('triggerExport calls GET /admin/checkout/reconciliation/export with businessDate query param', async () => {
    mockGet.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, businessDate: '2026-03-01', exportVersion: 1 } },
    })

    await triggerExport('2026-03-01')

    expect(mockGet).toHaveBeenCalledTimes(1)
    expect(mockGet).toHaveBeenCalledWith(
      '/admin/checkout/reconciliation/export',
      { params: { businessDate: '2026-03-01' } },
    )
  })

  it('triggerExport uses GET, not POST', async () => {
    mockGet.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, businessDate: '2026-03-01', exportVersion: 1 } },
    })

    await triggerExport('2026-03-01')

    expect(mockPost).not.toHaveBeenCalled()
    expect(mockGet).toHaveBeenCalledTimes(1)
  })

  it('triggerExport passes businessDate as query param, not request body', async () => {
    mockGet.mockResolvedValueOnce({
      data: { success: true, data: { id: 1, businessDate: '2026-04-10', exportVersion: 2 } },
    })

    await triggerExport('2026-04-10')

    // Verify the second argument contains params (query params), not a body object
    const callArgs = mockGet.mock.calls[0]
    expect(callArgs[1]).toHaveProperty('params')
    expect(callArgs[1].params).toEqual({ businessDate: '2026-04-10' })
  })
})
