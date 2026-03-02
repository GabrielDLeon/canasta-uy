import { useSyncExternalStore } from 'react'

import {
  addCompareProduct,
  clearCompareProducts,
  getCompareProducts,
  hasCompareProduct,
  removeCompareProduct,
  subscribeCompareProducts,
} from '@/lib/compare-store'

const getSnapshot = () => getCompareProducts()

export function useCompareProducts() {
  const products = useSyncExternalStore(subscribeCompareProducts, getSnapshot, getSnapshot)

  return {
    products,
    count: products.length,
    addProduct: addCompareProduct,
    removeProduct: removeCompareProduct,
    clearProducts: clearCompareProducts,
    hasProduct: hasCompareProduct,
  }
}
