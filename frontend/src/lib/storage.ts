const API_KEY = 'canasta-api-key'

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
