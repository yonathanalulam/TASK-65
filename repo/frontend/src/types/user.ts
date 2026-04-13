export interface User {
  id: number
  username: string
  displayName: string | null
  email: string | null
  status: string
  mfaEnabled: boolean
  roles: string[]
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  password: string
  displayName?: string
  email?: string
  roles?: string[]
}
