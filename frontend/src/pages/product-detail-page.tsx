import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Scale } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'

import { PriceChart } from '@/components/price-chart'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { useCompareProducts } from '@/hooks/use-compare-products'
import { api } from '@/lib/api'

export function ProductDetailPage() {
  const params = useParams()
  const productId = Number(params.id)
  const { addProduct } = useCompareProducts()

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

  const onAddToCompare = () => {
    if (!product.data) {
      return
    }

    const result = addProduct(product.data)

    if (result === 'added') {
      toast.success('Producto agregado a comparacion.')
      return
    }

    if (result === 'duplicate') {
      toast('Este producto ya esta en comparacion.')
      return
    }

    if (result === 'limit') {
      toast.error('Solo puedes comparar hasta 5 productos.')
      return
    }

    toast.error('No se pudo agregar el producto a comparacion.')
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <Button asChild variant="outline" size="sm">
          <Link to="/app/products">
            <ArrowLeft className="size-4" />
            Volver a productos
          </Link>
        </Button>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={onAddToCompare}
          disabled={product.isPending || !product.data}
        >
          <Scale className="size-4" />
          Agregar a comparacion
        </Button>
      </div>

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
        <CardContent className="flex flex-wrap items-end gap-3">
          <div className="space-y-2">
            <Label htmlFor="prices-from">Desde</Label>
            <Input id="prices-from" type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="prices-to">Hasta</Label>
            <Input id="prices-to" type="date" value={to} onChange={(event) => setTo(event.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="prices-granularity">Granularidad</Label>
          <Select value={granularity} onValueChange={(value) => setGranularity(value as typeof granularity)}>
            <SelectTrigger id="prices-granularity" className="w-[180px]">
              <SelectValue placeholder="Granularidad" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="daily">Diaria</SelectItem>
              <SelectItem value="weekly">Semanal</SelectItem>
              <SelectItem value="monthly">Mensual</SelectItem>
            </SelectContent>
          </Select>
          </div>
        </CardContent>
      </Card>

      {prices.isError ? <p className="text-sm text-destructive">{(prices.error as Error).message}</p> : null}
      <PriceChart title="Evolucion de precio promedio" points={points} />
    </section>
  )
}
