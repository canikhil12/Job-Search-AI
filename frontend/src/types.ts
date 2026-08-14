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

export interface Resume {
  id: string
  fileName: string
  contentType: string
  sizeBytes: number
  createdAt: string
}

export interface ResumeDetail extends Resume {
  extractedText: string
}

export interface JobMatch {
  id: string
  title: string
  company: string | null
  location: string | null
  sourceUrl: string | null
  score: number
}

export interface Job {
  id: string
  title: string
  company: string | null
  location: string | null
  description: string
  source: string
  sourceUrl: string | null
  postedAt: string | null
  createdAt: string
}
