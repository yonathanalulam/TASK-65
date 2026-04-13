export interface LoginRequest {
  username: string
  password: string
  deviceFingerprint?: string
  captchaId?: string
  captchaAnswer?: string
}

export interface LoginResponse {
  userId: number | null
  username: string | null
  displayName: string | null
  roles: string[] | null
  mfaRequired: boolean
  mfaToken: string | null
  forcePasswordChange: boolean
  signingKey: string | null
  sessionId: string | null
}

export interface PasswordChangeRequest {
  currentPassword: string
  newPassword: string
}

export interface MfaSetupResponse {
  qrCodeDataUri: string
  secretKey: string
  recoveryCodes: string[]
}

export interface MfaVerifyRequest {
  code: string
  mfaToken?: string
}
