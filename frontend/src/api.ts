import type { ApiError } from './types'

const TOKEN_KEY = 'jobmatch.token'

// Base URL for the API. Empty in local dev so requests go through the Vite proxy
// (same-origin). In production (Vercel) this is set to the backend's URL (Render),
// making requests cross-origin — which is exactly what the backend CORS config allows.
const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

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

function authHeader(base?: HeadersInit): Headers {
  const headers = new Headers(base)
  const token = getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  return headers
}

async function handleResponse<T>(response: Response): Promise<T> {
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

/** JSON request with the auth header attached. */
export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = authHeader(options.headers)
  headers.set('Content-Type', 'application/json')
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  return handleResponse<T>(response)
}

/**
 * Multipart upload. Deliberately does NOT set Content-Type — the browser sets it,
 * including the multipart boundary, which a manual header would clobber.
 */
export async function apiUpload<T>(path: string, formData: FormData): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: authHeader(),
    body: formData,
  })
  return handleResponse<T>(response)
}

export interface SseHandlers {
  onToken: (token: string) => void
  onError?: (message: string) => void
}

/**
 * Consume a Server-Sent Events stream with the auth header attached (EventSource can't set
 * headers). Reads the fetch body incrementally, splits on blank lines, and dispatches each frame.
 * Resolves when the stream ends; rejects on a non-2xx opening response.
 */
export async function apiStreamSse(path: string, handlers: SseHandlers, signal?: AbortSignal): Promise<void> {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: authHeader({ Accept: 'text/event-stream' }),
    signal,
  })
  if (response.status === 401) {
    onUnauthorized?.()
    throw new UnauthorizedError()
  }
  if (!response.ok || !response.body) {
    const body = (await response.json().catch(() => null)) as ApiError | null
    throw new ApiRequestError(
      body ?? { timestamp: new Date().toISOString(), status: response.status, message: 'Request failed' },
    )
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    let sep: number
    while ((sep = buffer.indexOf('\n\n')) !== -1) {
      const frame = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)
      dispatchSseFrame(frame, handlers)
    }
  }
}

function dispatchSseFrame(frame: string, handlers: SseHandlers): void {
  let event = 'message'
  const dataLines: string[] = []
  for (const line of frame.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
  }
  const data = dataLines.join('\n')
  if (event === 'error') {
    handlers.onError?.(data || 'Analysis failed')
    return
  }
  if (event === 'done') return
  // default "message" frames carry a JSON-encoded token string
  try {
    handlers.onToken(JSON.parse(data) as string)
  } catch {
    // ignore keep-alive / unparseable frames
  }
}
