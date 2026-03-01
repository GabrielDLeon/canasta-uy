const AUTH_KEY = 'canasta-auth'
const API_KEY = 'canasta-api-key'

export type AuthState = {
  accessToken: string
  refreshToken: string
}

export function getAuthState(): AuthState | null {
  const raw = localStorage.getItem(AUTH_KEY)
  if (!raw) {
    return null
  }

  try {
    return JSON.parse(raw) as AuthState
  } catch {
    localStorage.removeItem(AUTH_KEY)
    return null
  }
}

export function setAuthState(auth: AuthState | null): void {
  if (!auth) {
    localStorage.removeItem(AUTH_KEY)
    return
  }

  localStorage.setItem(AUTH_KEY, JSON.stringify(auth))
}

export function getApiKeyValue(): string {
  return localStorage.getItem(API_KEY) ?? ''
}

export function setApiKeyValue(value: string): void {
  const trimmed = value.trim()
  if (!trimmed) {
    localStorage.removeItem(API_KEY)
    return
  }

  localStorage.setItem(API_KEY, trimmed)
}
