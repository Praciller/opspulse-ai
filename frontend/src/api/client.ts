import { API_BASE_URL } from '../config'
import type { ApiError, AuthResponse } from '../types'

let accessToken: string | null = null
let refreshPromise: Promise<boolean> | null = null
export const authToken = { get: () => accessToken, set: (value: string | null) => { accessToken = value } }
export const SESSION_EXPIRED_EVENT = 'opspulse:session-expired'
export const SESSION_REFRESHED_EVENT = 'opspulse:session-refreshed'

const sanitizeMessage = (value: string) => {
  const bounded = value.replace(/\s+/g, ' ').trim().slice(0, 240)
  return bounded.replace(/(api[_ -]?key|authorization|bearer|password|secret|token)\s*[:=]\s*[^,; ]+/gi, '$1=[redacted]')
}

export class ApiRequestError extends Error {
  constructor(public readonly status: number, public readonly payload: ApiError, public readonly requestId?: string) {
    super(sanitizeMessage(payload.message ?? payload.code ?? 'Request failed'))
    this.name = 'ApiRequestError'
  }
}

export const requestId = () => `web-${crypto.randomUUID()}`

const parse = async (response: Response) => {
  const text = await response.text()
  if (!text) return undefined
  try { return JSON.parse(text) as unknown } catch { return { message: text } }
}

const refresh = async () => {
  const refreshToken = sessionStorage.getItem('opspulse.refreshToken')
  if (!refreshToken) return false
  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST', headers: { 'Content-Type': 'application/json', 'X-Request-Id': requestId() },
      body: JSON.stringify({ refreshToken }),
    }).then(async response => {
      if (!response.ok) return false
      const tokens = await response.json() as AuthResponse
      authToken.set(tokens.accessToken)
      sessionStorage.setItem('opspulse.refreshToken', tokens.refreshToken)
      sessionStorage.setItem('opspulse.user', JSON.stringify(tokens.user))
      window.dispatchEvent(new CustomEvent(SESSION_REFRESHED_EVENT, { detail: tokens.user }))
      return true
    }).catch(() => false).finally(() => { refreshPromise = null })
  }
  return refreshPromise
}

export async function apiFetch<T>(path: string, init: RequestInit = {}, retry = true, transientRetries = 2): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  headers.set('X-Request-Id', headers.get('X-Request-Id') ?? requestId())
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  const response = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  if (response.status === 401 && retry) {
    if (await refresh()) return apiFetch<T>(path, init, false, transientRetries)
    authToken.set(null)
    sessionStorage.removeItem('opspulse.refreshToken')
    sessionStorage.removeItem('opspulse.user')
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  } else if (response.status === 401) {
    authToken.set(null)
    sessionStorage.removeItem('opspulse.refreshToken')
    sessionStorage.removeItem('opspulse.user')
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
  }
  if ([503, 504].includes(response.status) && transientRetries > 0) {
    await new Promise(resolve => window.setTimeout(resolve, transientRetries === 2 ? 300 : 900))
    return apiFetch<T>(path, init, retry, transientRetries - 1)
  }
  const payload = await parse(response)
  if (!response.ok) {
    const errorPayload = (payload ?? {}) as ApiError
    const safePayload = { ...errorPayload, message: errorPayload.message ? sanitizeMessage(errorPayload.message) : errorPayload.message }
    throw new ApiRequestError(response.status, safePayload, response.headers.get('X-Request-Id') ?? errorPayload.requestId)
  }
  return payload as T
}
