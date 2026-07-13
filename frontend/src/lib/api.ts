import type {
  ApiKeyCreateResponse,
  ApiKeyListItem,
  ApiResponse,
  CategoryListResponse,
  CategoryProductsResponse,
  CategoryStatsResponse,
  ComparisonResponse,
  DashboardResponse,
  InflationResponse,
  LoginResponse,
  PriceListResponse,
  PriceSearchResponse,
  Product,
  ProductListResponse,
  ProfileResponse,
  RefreshResponse,
  TopChangesResponse,
  TrendResponse,
} from '@/types/api'

type AuthMode = 'none' | 'session'

type RequestOptions = {
  method?: 'GET' | 'POST' | 'DELETE'
  auth?: AuthMode
  body?: unknown
  allowNullData?: boolean
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

// Token storage in memory + localStorage
let accessToken: string | null = null
let refreshToken: string | null = null

function setTokens(access: string, refresh: string) {
  accessToken = access
  refreshToken = refresh
  localStorage.setItem('canasta_access_token', access)
  localStorage.setItem('canasta_refresh_token', refresh)
}

function clearTokens() {
  accessToken = null
  refreshToken = null
  localStorage.removeItem('canasta_access_token')
  localStorage.removeItem('canasta_refresh_token')
}

function getAccessToken(): string | null {
  if (accessToken) return accessToken
  const stored = localStorage.getItem('canasta_access_token')
  if (stored) {
    accessToken = stored
    return stored
  }
  return null
}

function getRefreshToken(): string | null {
  if (refreshToken) return refreshToken
  const stored = localStorage.getItem('canasta_refresh_token')
  if (stored) {
    refreshToken = stored
    return stored
  }
  return null
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const data = await performRequest<T>(path, options, false)
  return data
}

async function performRequest<T>(
  path: string,
  options: RequestOptions,
  refreshAttempted: boolean,
): Promise<T> {
  const authMode = options.auth ?? 'none'
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }

  // Add Authorization header if auth is required
  if (authMode === 'session') {
    const token = getAccessToken()
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if ((response.status === 401 || response.status === 403) && authMode === 'session' && !refreshAttempted) {
    const refreshed = await tryRefreshToken()
    if (refreshed) {
      return performRequest(path, options, true)
    }
  }

  const parsed = await parseApiResponse<T>(response)

  if (!response.ok || !parsed.success) {
    throw new Error(parsed.message ?? `Error HTTP ${response.status}`)
  }

  if (parsed.data === null) {
    if (options.allowNullData) {
      return undefined as T
    }
    throw new Error(parsed.message ?? 'Response data is null')
  }

  return parsed.data
}

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const contentType = response.headers.get('content-type') ?? ''

  if (contentType.toLowerCase().includes('application/json')) {
    try {
      return (await response.json()) as ApiResponse<T>
    } catch {
      return {
        success: false,
        message: `Error HTTP ${response.status}`,
        data: null,
        timestamp: new Date().toISOString(),
      }
    }
  }

  const text = await response.text()
  const message = text.trim() || `Error HTTP ${response.status}`

  return {
    success: false,
    message,
    data: null,
    timestamp: new Date().toISOString(),
  }
}

async function tryRefreshToken(): Promise<boolean> {
  const currentRefreshToken = getRefreshToken()
  if (!currentRefreshToken) {
    return false
  }

  try {
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken: currentRefreshToken }),
    })

    if (!response.ok) {
      clearTokens()
      return false
    }

    const parsed = (await response.json()) as ApiResponse<RefreshResponse>
    if (!parsed.success || !parsed.data) {
      clearTokens()
      return false
    }

    setTokens(parsed.data.accessToken, parsed.data.refreshToken)
    return true
  } catch {
    clearTokens()
    return false
  }
}

function searchParams(
  params: Record<string, string | number | boolean | undefined | null>,
): string {
  const query = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return
    }

    query.set(key, String(value))
  })

  const asString = query.toString()
  return asString ? `?${asString}` : ''
}

export const api = {
  register: (email: string, password: string) =>
    request('/auth/register', { method: 'POST', body: { email, password } }),

  login: async (email: string, password: string) => {
    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password }),
    })

    const parsed = await parseApiResponse<LoginResponse>(response)
    if (!response.ok || !parsed.success) {
      throw new Error(parsed.message ?? `Error HTTP ${response.status}`)
    }

    if (parsed.data) {
      setTokens(parsed.data.accessToken, parsed.data.refreshToken)
    }
    return parsed.data
  },

  logout: async () => {
    try {
      await request('/auth/logout', { method: 'POST', auth: 'session' })
    } finally {
      clearTokens()
    }
  },

  getProfile: () => request<ProfileResponse>('/account/profile', { auth: 'session' }),

  listApiKeys: () => request<ApiKeyListItem[]>('/account/api-keys', { auth: 'session' }),

  createApiKey: (name: string) =>
    request<ApiKeyCreateResponse>('/account/api-keys', {
      method: 'POST',
      auth: 'session',
      body: { name },
    }),

  revokeApiKey: (id: number) =>
    request<void>(`/account/api-keys/${id}`, {
      method: 'DELETE',
      auth: 'session',
      allowNullData: true,
    }),

  getProducts: (page = 0, size = 20) =>
    request<ProductListResponse>(`/products${searchParams({ page, size })}`, {
      auth: 'session',
    }),

  searchProducts: (query: string, page = 0, size = 20) =>
    request<ProductListResponse>(`/products/search${searchParams({ query, page, size })}`, {
      auth: 'session',
    }),

  getProductById: (id: number) => request<Product>(`/products/${id}`, { auth: 'session' }),

  getProductPrices: (
    productId: number,
    from?: string,
    to?: string,
    granularity: 'daily' | 'weekly' | 'monthly' = 'daily',
  ) =>
    request<PriceListResponse>(
      `/products/${productId}/prices${searchParams({ from, to, granularity })}`,
      {
        auth: 'session',
      },
    ),

  searchPrices: (productIds?: string, from?: string, to?: string, page = 0, size = 20) =>
    request<PriceSearchResponse>(
      `/prices${searchParams({ productIds, from, to, page, size })}`,
      {
        auth: 'session',
      },
    ),

  getCategories: (page = 0, size = 20) =>
    request<CategoryListResponse>(`/categories${searchParams({ page, size })}`, {
      auth: 'session',
    }),

  searchCategories: (query: string, page = 0, size = 20) =>
    request<CategoryListResponse>(`/categories/search${searchParams({ query, page, size })}`, {
      auth: 'session',
    }),

  getCategoryProducts: (categoryId: number, page = 0, size = 20) =>
    request<CategoryProductsResponse>(
      `/categories/${categoryId}/products${searchParams({ categoryId, page, size })}`,
      {
        auth: 'session',
      },
    ),

  getCategoryStats: (categoryId: number, from?: string, to?: string) =>
    request<CategoryStatsResponse>(
      `/categories/${categoryId}/stats${searchParams({ categoryId, from, to })}`,
      {
        auth: 'session',
      },
    ),

  getTrend: (productId: number, from?: string, to?: string, includeData = true) =>
    request<TrendResponse>(
      `/analytics/trend/${productId}${searchParams({ from, to, includeData })}`,
      {
        auth: 'session',
      },
    ),

  getInflation: (categoryId: number, from?: string, to?: string, includeData = true) =>
    request<InflationResponse>(
      `/analytics/inflation/${categoryId}${searchParams({ from, to, includeData })}`,
      {
        auth: 'session',
      },
    ),

  compareProducts: (productIds: string, from?: string, to?: string, includeData = false) =>
    request<ComparisonResponse>(
      `/analytics/compare${searchParams({ productIds, from, to, includeData })}`,
      {
        auth: 'session',
      },
    ),

  getTopChanges: (
    period: '7d' | '30d' | '90d' | '1y' = '30d',
    type: 'increase' | 'decrease' | 'all' = 'all',
    limit = 10,
    categoryId?: number,
  ) =>
    request<TopChangesResponse>(
      `/analytics/top-changes${searchParams({ period, type, limit, categoryId })}`,
      {
        auth: 'session',
      },
    ),

  getDashboardSummary: (period: '7d' | '30d' | '90d' | '1y' = '30d', limit = 5) =>
    request<DashboardResponse>(
      `/analytics/dashboard${searchParams({ period, limit })}`,
      {
        auth: 'session',
      },
    ),
}
