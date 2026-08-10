export interface User {
  id: string
  email: string
  fullName: string
}

export interface AuthResponse {
  token: string
  expiresAt: string
  user: User
}

export interface ApiError {
  timestamp: string
  status: number
  message: string
  fieldErrors?: Record<string, string>
}
