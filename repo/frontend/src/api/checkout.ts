import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { Bundle, Transaction } from '@/types/finance'

export async function listBundles(): Promise<Bundle[]> {
  const { data } = await client.get<ApiResponse<Bundle[]>>('/checkout/bundles')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list bundles')
  }
  return data.data
}

export async function initiateCheckout(bundleIds: number[]): Promise<Transaction> {
  const { data } = await client.post<ApiResponse<Transaction>>('/checkout/initiate', { bundleIds })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to initiate checkout')
  }
  return data.data
}

export async function completeCheckout(transactionId: number): Promise<Transaction> {
  const { data } = await client.post<ApiResponse<Transaction>>(`/checkout/complete/${transactionId}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to complete checkout')
  }
  return data.data
}

export async function listTransactions(): Promise<PageResponse<Transaction>> {
  const { data } = await client.get<ApiResponse<PageResponse<Transaction>>>('/checkout/transactions')
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list transactions')
  }
  return data.data
}

export async function getTransaction(id: number): Promise<Transaction> {
  const { data } = await client.get<ApiResponse<Transaction>>(`/checkout/transactions/${id}`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to get transaction')
  }
  return data.data
}
