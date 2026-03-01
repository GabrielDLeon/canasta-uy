import { AlertTriangle } from 'lucide-react'

import { getApiKeyValue } from '@/lib/storage'

export function ApiKeyBanner() {
  if (getApiKeyValue()) {
    return null
  }

  return (
    <div className="mb-4 flex items-start gap-2 rounded-md border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
      <AlertTriangle className="mt-0.5 h-4 w-4" />
      <span>
        No hay API key activa. Pega una API key en el header para consumir productos, categorias,
        precios y analytics.
      </span>
    </div>
  )
}
