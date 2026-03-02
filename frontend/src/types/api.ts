export type ApiResponse<T> = {
  success: boolean
  message: string | null
  data: T | null
  timestamp: string
}

export type PaginationInfo = {
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export type DateRange = {
  from: string
  to: string
}

export type Category = {
  categoryId: number
  name: string
  createdAt?: string
}

export type CategoryListResponse = {
  categories: Category[]
  pagination: PaginationInfo
}

export type Product = {
  productId: number
  name: string
  specification?: string
  brand?: string
  category?: Category
}

export type ProductListResponse = {
  products: Product[]
  pagination: PaginationInfo
}

export type PricePoint = {
  date: string
  priceMin: number
  priceMax: number
  priceAvg: number
  priceMedian: number
  storeCount: number
  offerPercentage: number
}

export type PriceListResponse = {
  productId: number
  productName: string
  period: DateRange
  granularity: string
  prices: PricePoint[]
}

export type PriceSearchResponse = {
  prices: PricePoint[]
  pagination: PaginationInfo
}

export type LoginResponse = {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

export type RefreshResponse = {
  accessToken: string
  refreshToken: string
}

export type ProfileResponse = {
  clientId: number
  email: string
  totalKeys: number
  createdAt: string
}

export type ApiKeyListItem = {
  name: string
  keyPrefix: string
  isActive: boolean
  createdAt: string
}

export type ApiKeyCreateResponse = {
  name: string
  keyValue: string
  keyPrefix: string
  isActive: boolean
  createdAt: string
}

export type TrendResponse = {
  productId: number
  productName: string
  period: DateRange
  summary: {
    trend: string
    trendDirection: string
    variationPercentage: number
    variationAbsolute: number
    priceStart: number
    priceEnd: number
    priceAvg: number
    priceMin: number
    priceMax: number
    volatility: string
  }
  data: Array<{
    date: string
    priceAvg: number
    priceMin: number
    priceMax: number
  }>
}

export type InflationResponse = {
  categoryId: number
  categoryName: string
  period: DateRange
  summary: {
    totalInflationPercentage: number
    annualizedInflation: number
    productsCount: number
    periodType: string
  }
  data: Array<{
    yearMonth: string
    inflationPercentage: number
    avgPrice: number
    dataPoints: number
  }>
}

export type ComparisonResponse = {
  period: DateRange
  products: Array<{
    productId: number
    productName: string
    category: string
    avgPrice: number
    minPrice: number
    maxPrice: number
    variationPercentage: number
    dataPoints: number
    data?: Array<{
      date: string
      priceAvg: number
    }> | null
  }>
  comparison: {
    priceDifference: number
    priceRatio: number
    mostExpensiveProduct: string
    cheapestProduct: string
    mostVolatileProduct: string
  }
}

export type TopChangesResponse = {
  period: string
  dateRange: DateRange
  changes: Array<{
    productId: number
    productName: string
    category: string
    priceBefore: number
    priceAfter: number
    changePercentage: number
    changeAbsolute: number
    changeDirection: string
  }>
}

export type CategoryProductsResponse = {
  categoryId: number
  categoryName: string
  products: Array<{
    id: number
    name: string
    brand?: string
    specification?: string
  }>
  pagination: PaginationInfo
}

export type CategoryStatsResponse = {
  categoryId: number
  categoryName: string
  productCount: number
  period: DateRange
  stats: {
    avgPrice: number
    minPrice: number
    maxPrice: number
    medianPrice: number
  }
}
