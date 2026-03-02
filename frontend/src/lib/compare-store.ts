const COMPARE_PRODUCTS_KEY = 'canasta-compare-products'
const COMPARE_PRODUCTS_CHANGED = 'compare-products-changed'
const MAX_COMPARE_PRODUCTS = 5
const EMPTY_PRODUCTS: CompareProduct[] = []

let cachedRaw: string | null = null
let cachedProducts: CompareProduct[] = EMPTY_PRODUCTS

export type CompareProduct = {
  productId: number
  name: string
  brand?: string
}

type ProductCandidate = {
  productId?: number
  id?: number
  name: string
  brand?: string
}

function emitChange() {
  window.dispatchEvent(new Event(COMPARE_PRODUCTS_CHANGED))
}

function normalizeProduct(product: ProductCandidate): CompareProduct | null {
  const productId = product.productId ?? product.id
  if (!productId) {
    return null
  }

  return {
    productId,
    name: product.name,
    brand: product.brand,
  }
}

function parseProducts(raw: string | null): CompareProduct[] {
  if (!raw) {
    return EMPTY_PRODUCTS
  }

  try {
    const parsed = JSON.parse(raw) as ProductCandidate[]
    if (!Array.isArray(parsed)) {
      return EMPTY_PRODUCTS
    }

    const normalized = parsed
      .map(normalizeProduct)
      .filter((product): product is CompareProduct => product !== null)

    return normalized.length === 0 ? EMPTY_PRODUCTS : normalized
  } catch {
    localStorage.removeItem(COMPARE_PRODUCTS_KEY)
    return EMPTY_PRODUCTS
  }
}

function readProducts(): CompareProduct[] {
  const raw = localStorage.getItem(COMPARE_PRODUCTS_KEY)

  if (raw === cachedRaw) {
    return cachedProducts
  }

  cachedRaw = raw
  cachedProducts = parseProducts(raw)
  return cachedProducts
}

function writeProducts(products: CompareProduct[]) {
  const nextProducts = products.length === 0 ? EMPTY_PRODUCTS : products
  const nextRaw = nextProducts === EMPTY_PRODUCTS ? null : JSON.stringify(nextProducts)

  cachedProducts = nextProducts
  cachedRaw = nextRaw

  if (nextRaw === null) {
    localStorage.removeItem(COMPARE_PRODUCTS_KEY)
  } else {
    localStorage.setItem(COMPARE_PRODUCTS_KEY, nextRaw)
  }

  emitChange()
}

export function getCompareProducts(): CompareProduct[] {
  return readProducts()
}

export function hasCompareProduct(productId: number): boolean {
  return readProducts().some((product) => product.productId === productId)
}

export function addCompareProduct(product: ProductCandidate): 'added' | 'duplicate' | 'limit' | 'invalid' {
  const normalized = normalizeProduct(product)
  if (!normalized) {
    return 'invalid'
  }

  const products = readProducts()
  if (products.some((item) => item.productId === normalized.productId)) {
    return 'duplicate'
  }

  if (products.length >= MAX_COMPARE_PRODUCTS) {
    return 'limit'
  }

  writeProducts([...products, normalized])
  return 'added'
}

export function removeCompareProduct(productId: number): void {
  const next = readProducts().filter((product) => product.productId !== productId)
  writeProducts(next)
}

export function clearCompareProducts(): void {
  writeProducts(EMPTY_PRODUCTS)
}

export function subscribeCompareProducts(listener: () => void): () => void {
  const onStorage = (event: StorageEvent) => {
    if (event.key === COMPARE_PRODUCTS_KEY) {
      listener()
    }
  }

  window.addEventListener('storage', onStorage)
  window.addEventListener(COMPARE_PRODUCTS_CHANGED, listener)

  return () => {
    window.removeEventListener('storage', onStorage)
    window.removeEventListener(COMPARE_PRODUCTS_CHANGED, listener)
  }
}
