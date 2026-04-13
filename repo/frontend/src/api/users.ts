import client from './client'
import type { ApiResponse, PageResponse } from '@/types/api'
import type { User, CreateUserRequest } from '@/types/user'

export async function createUser(request: CreateUserRequest): Promise<User> {
  const { data } = await client.post<ApiResponse<User>>('/admin/users', request)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to create user')
  }
  return data.data
}

export async function listUsers(page = 0, size = 20): Promise<PageResponse<User>> {
  const { data } = await client.get<ApiResponse<PageResponse<User>>>('/admin/users', {
    params: { page, size },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to list users')
  }
  return data.data
}

export async function disableUser(userId: number): Promise<User> {
  const { data } = await client.post<ApiResponse<User>>(`/admin/users/${userId}/disable`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to disable user')
  }
  return data.data
}

export async function enableUser(userId: number): Promise<User> {
  const { data } = await client.post<ApiResponse<User>>(`/admin/users/${userId}/enable`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to enable user')
  }
  return data.data
}
