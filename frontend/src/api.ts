import type { ApiError } from './types'

const TOKEN_KEY = 'jobmatch.token'

// Token is held in memory and mirrored to localStorage so a refresh keeps the session.
let inMemoryToken: string | null = localStorage.getItem(TOKEN_KEY)

export function getToken(): string | null {
  return inMemoryToken
}

export function setToken(token: string | null): void {
  inMemoryToken = token
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/** Raised on a 401 so callers/interceptors can force a re-login. */
export class UnauthorizedError extends Error {
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

/** Raised for any non-2xx response other than 401, carrying the parsed ApiError. */
export class ApiRequestError extends Error {
  status: number
  fieldErrors?: Record<string, string>

  constructor(apiError: ApiError) {
    super(apiError.message)
    this.name = 'ApiRequestError'
    this.status = apiError.status
    this.fieldErrors = apiError.fieldErrors
  }
}

// Callback invoked on any 401 (wired up by the auth context to redirect to /login).
let onUnauthorized: (() => void) | null = null
export function setUnauthorizedHandler(handler: (() => void) | null): void {
  onUnauthorized = handler
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(path, { ...options, headers })

  if (response.status === 401) {
    onUnauthorized?.()
    throw new UnauthorizedError()
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as ApiError | null
    throw new ApiRequestError(
      body ?? { timestamp: new Date().toISOString(), status: response.status, message: 'Request failed' },
    )
  }

  // 204 No Content -> nothing to parse.
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}
