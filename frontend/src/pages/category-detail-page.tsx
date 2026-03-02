import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { ProductItem } from '@/components/product-item'
import { api } from '@/lib/api'

export function CategoryDetailPage() {
  const params = useParams()
  const categoryId = Number(params.id)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const stats = useQuery({
    queryKey: ['category-stats', categoryId, from, to],
    queryFn: () => api.getCategoryStats(categoryId, from || undefined, to || undefined),
    enabled: Number.isFinite(categoryId),
  })

  const products = useQuery({
    queryKey: ['category-products', categoryId],
    queryFn: () => api.getCategoryProducts(categoryId),
    enabled: Number.isFinite(categoryId),
  })

  return (
    <section className="space-y-4">
      <ApiKeyBanner />
      <div className="flex items-center gap-2">
        <Button asChild variant="outline" size="sm">
          <Link to="/app/categories">
            <ArrowLeft className="size-4" />
            Volver a categorias
          </Link>
        </Button>
      </div>
      <h1 className="text-2xl font-semibold">Categoria #{categoryId}</h1>

      <Card>
        <CardHeader>
          <CardTitle>Filtro de fechas para estadisticas</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-wrap gap-2">
          <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
          <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Estadisticas</CardTitle>
        </CardHeader>
        <CardContent>
          {stats.isPending ? <p>Cargando...</p> : null}
          {stats.isError ? (
            <p className="text-sm text-destructive">{(stats.error as Error).message}</p>
          ) : null}
          {stats.data ? (
            <ul className="grid gap-2 text-sm md:grid-cols-2">
              <li>Categoria: {stats.data.categoryName}</li>
              <li>Productos: {stats.data.productCount}</li>
              <li>Precio promedio: {Number(stats.data.stats.avgPrice).toFixed(2)}</li>
              <li>Precio mediano: {Number(stats.data.stats.medianPrice).toFixed(2)}</li>
              <li>Min: {Number(stats.data.stats.minPrice).toFixed(2)}</li>
              <li>Max: {Number(stats.data.stats.maxPrice).toFixed(2)}</li>
            </ul>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Productos de la categoria</CardTitle>
        </CardHeader>
        <CardContent>
          {products.isPending ? <p>Cargando...</p> : null}
          {products.isError ? (
            <p className="text-sm text-destructive">{(products.error as Error).message}</p>
          ) : null}
          {products.data ? (
            <ul className="space-y-2">
              {products.data.products.map((product) => (
                <li key={product.id}>
                  <ProductItem product={product} showImage={false} />
                </li>
              ))}
            </ul>
          ) : null}
        </CardContent>
      </Card>
    </section>
  )
}
