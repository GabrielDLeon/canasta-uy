import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams } from 'react-router-dom'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { PriceChart } from '@/components/price-chart'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { api } from '@/lib/api'

export function ProductDetailPage() {
  const params = useParams()
  const productId = Number(params.id)

  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [granularity, setGranularity] = useState<'daily' | 'weekly' | 'monthly'>('daily')

  const product = useQuery({
    queryKey: ['product', productId],
    queryFn: () => api.getProductById(productId),
    enabled: Number.isFinite(productId),
  })

  const prices = useQuery({
    queryKey: ['product-prices', productId, from, to, granularity],
    queryFn: () => api.getProductPrices(productId, from || undefined, to || undefined, granularity),
    enabled: Number.isFinite(productId),
  })

  const points = useMemo(
    () =>
      (prices.data?.prices ?? []).map((item) => ({
        time: item.date,
        value: Number(item.priceAvg),
      })),
    [prices.data],
  )

  return (
    <section className="space-y-4">
      <ApiKeyBanner />
      <h1 className="text-2xl font-semibold">Detalle de producto</h1>

      <Card>
        <CardHeader>
          <CardTitle>{product.data?.name ?? `Producto #${productId}`}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-1 text-sm">
          {product.isError ? (
            <p className="text-destructive">{(product.error as Error).message}</p>
          ) : null}
          <p>Marca: {product.data?.brand ?? 'N/A'}</p>
          <p>Especificacion: {product.data?.specification ?? 'N/A'}</p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Filtros de precios</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap items-center gap-2">
          <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
          <Select value={granularity} onValueChange={(value) => setGranularity(value as typeof granularity)}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Granularidad" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="daily">Diaria</SelectItem>
              <SelectItem value="weekly">Semanal</SelectItem>
              <SelectItem value="monthly">Mensual</SelectItem>
            </SelectContent>
          </Select>
        </CardContent>
      </Card>

      {prices.isError ? <p className="text-sm text-destructive">{(prices.error as Error).message}</p> : null}
      <PriceChart title="Evolucion de precio promedio" points={points} />
    </section>
  )
}
