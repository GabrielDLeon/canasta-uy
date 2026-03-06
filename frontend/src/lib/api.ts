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
import { getApiKeyValue } from '@/lib/storage'

type AuthMode = 'none' | 'session' | 'api-key'

type RequestOptions = {
  method?: 'GET' | 'POST' | 'DELETE'
  auth?: AuthMode
  body?: unknown
}

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? '/api/v1'

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const data = await performRequest<T>(path, options)
  return data
}

async function performRequest<T>(path: string, options: RequestOptions): Promise<T> {
  const authMode = options.auth ?? 'none'
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  }

  if (authMode === 'api-key') {
    const apiKey = getApiKeyValue()
    if (!apiKey) {
      throw new Error('No hay API key activa. Pegala en el header de la app.')
    }

    headers['Api-Key'] = apiKey
  }

  const response = await fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    headers,
    credentials: 'include',
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (response.status === 401 && authMode === 'session') {
    const refreshed = await tryRefreshToken()
    if (refreshed) {
      return performRequest(path, options)
    }
  }

  const parsed = (await response.json()) as ApiResponse<T>

  if (!response.ok || !parsed.success || parsed.data === null) {
    throw new Error(parsed.message ?? `Error HTTP ${response.status}`)
  }

  return parsed.data
}

async function tryRefreshToken(): Promise<boolean> {
  try {
    const response = await fetch(`${API_BASE}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    })

    if (!response.ok) {
      return false
    }

    const parsed = (await response.json()) as ApiResponse<RefreshResponse>
    if (!parsed.success || !parsed.data) {
      return false
    }

    return true
  } catch {
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

  login: (email: string, password: string) =>
    request<LoginResponse>('/auth/login', { method: 'POST', body: { email, password } }),

  logout: () => request('/auth/logout', { method: 'POST', auth: 'session' }),

  getProfile: () => request<ProfileResponse>('/account/profile', { auth: 'session' }),

  listApiKeys: () => request<ApiKeyListItem[]>('/account/api-keys', { auth: 'session' }),

  createApiKey: (name: string) =>
    request<ApiKeyCreateResponse>('/account/api-keys', {
      method: 'POST',
      auth: 'session',
      body: { name },
    }),

  revokeApiKey: (id: number) =>
    request<void>(`/account/api-keys/${id}`, { method: 'DELETE', auth: 'session' }),

  getProducts: (page = 0, size = 20) =>
    request<ProductListResponse>(`/products${searchParams({ page, size })}`, {
      auth: 'api-key',
    }),

  searchProducts: (query: string, page = 0, size = 20) =>
    request<ProductListResponse>(`/products/search${searchParams({ query, page, size })}`, {
      auth: 'api-key',
    }),

  getProductById: (id: number) => request<Product>(`/products/${id}`, { auth: 'api-key' }),

  getProductPrices: (
    productId: number,
    from?: string,
    to?: string,
    granularity: 'daily' | 'weekly' | 'monthly' = 'daily',
  ) =>
    request<PriceListResponse>(
      `/products/${productId}/prices${searchParams({ from, to, granularity })}`,
      {
        auth: 'api-key',
      },
    ),

  searchPrices: (productIds?: string, from?: string, to?: string, page = 0, size = 20) =>
    request<PriceSearchResponse>(
      `/prices${searchParams({ productIds, from, to, page, size })}`,
      {
        auth: 'api-key',
      },
    ),

  getCategories: (page = 0, size = 20) =>
    request<CategoryListResponse>(`/categories${searchParams({ page, size })}`, {
      auth: 'api-key',
    }),

  searchCategories: (query: string, page = 0, size = 20) =>
    request<CategoryListResponse>(`/categories/search${searchParams({ query, page, size })}`, {
      auth: 'api-key',
    }),

  getCategoryProducts: (categoryId: number, page = 0, size = 20) =>
    request<CategoryProductsResponse>(
      `/categories/${categoryId}/products${searchParams({ categoryId, page, size })}`,
      {
        auth: 'api-key',
      },
    ),

  getCategoryStats: (categoryId: number, from?: string, to?: string) =>
    request<CategoryStatsResponse>(
      `/categories/${categoryId}/stats${searchParams({ categoryId, from, to })}`,
      {
        auth: 'api-key',
      },
    ),

  getTrend: (productId: number, from?: string, to?: string, includeData = true) =>
    request<TrendResponse>(
      `/analytics/trend/${productId}${searchParams({ from, to, includeData })}`,
      {
        auth: 'api-key',
      },
    ),

  getInflation: (categoryId: number, from?: string, to?: string, includeData = true) =>
    request<InflationResponse>(
      `/analytics/inflation/${categoryId}${searchParams({ from, to, includeData })}`,
      {
        auth: 'api-key',
      },
    ),

  compareProducts: (productIds: string, from?: string, to?: string, includeData = false) =>
    request<ComparisonResponse>(
      `/analytics/compare${searchParams({ productIds, from, to, includeData })}`,
      {
        auth: 'api-key',
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
        auth: 'api-key',
      },
    ),

  getDashboardSummary: (period: '7d' | '30d' | '90d' | '1y' = '30d', limit = 5) =>
    request<DashboardResponse>(
      `/analytics/dashboard${searchParams({ period, limit })}`,
      {
        auth: 'api-key',
      },
    ),
}
