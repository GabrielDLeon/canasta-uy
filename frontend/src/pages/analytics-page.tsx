import { useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import { useQuery } from '@tanstack/react-query'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { PriceChart } from '@/components/price-chart'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { api } from '@/lib/api'

export function AnalyticsPage() {
  const [trendProductInput, setTrendProductInput] = useState('15')
  const [trendProductId, setTrendProductId] = useState('15')

  const [inflationCategoryInput, setInflationCategoryInput] = useState('1')
  const [inflationCategoryId, setInflationCategoryId] = useState('1')

  const [compareIdsInput, setCompareIdsInput] = useState('15,16')
  const [compareIds, setCompareIds] = useState('15,16')

  const trend = useQuery({
    queryKey: ['analytics-trend', trendProductId],
    queryFn: () => api.getTrend(Number(trendProductId)),
    enabled: Boolean(trendProductId),
  })

  const inflation = useQuery({
    queryKey: ['analytics-inflation', inflationCategoryId],
    queryFn: () => api.getInflation(Number(inflationCategoryId)),
    enabled: Boolean(inflationCategoryId),
  })

  const compare = useQuery({
    queryKey: ['analytics-compare', compareIds],
    queryFn: () => api.compareProducts(compareIds),
    enabled: Boolean(compareIds),
  })

  const trendPoints = useMemo(
    () =>
      (trend.data?.data ?? []).map((item) => ({
        time: item.date,
        value: Number(item.priceAvg),
      })),
    [trend.data],
  )

  const inflationPoints = useMemo(
    () =>
      (inflation.data?.data ?? []).map((item) => ({
        time: `${item.yearMonth}-01`,
        value: Number(item.inflationPercentage),
      })),
    [inflation.data],
  )

  const onTrendSubmit = (event: FormEvent) => {
    event.preventDefault()
    setTrendProductId(trendProductInput.trim())
  }

  const onInflationSubmit = (event: FormEvent) => {
    event.preventDefault()
    setInflationCategoryId(inflationCategoryInput.trim())
  }

  const onCompareSubmit = (event: FormEvent) => {
    event.preventDefault()
    setCompareIds(compareIdsInput.trim())
  }

  return (
    <section className="space-y-4">
      <ApiKeyBanner />
      <h1 className="text-2xl font-semibold">Analytics</h1>

      <Tabs defaultValue="trend">
        <TabsList>
          <TabsTrigger value="trend">Trend</TabsTrigger>
          <TabsTrigger value="inflation">Inflation</TabsTrigger>
          <TabsTrigger value="compare">Compare</TabsTrigger>
        </TabsList>

        <TabsContent value="trend" className="space-y-2">
          <Card>
            <CardHeader>
              <CardTitle>Tendencia por producto</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="mb-2 text-sm text-muted-foreground">
                Ingresa el ID de un producto para ver su evolucion de precio promedio.
              </p>
              <form onSubmit={onTrendSubmit} className="flex flex-wrap gap-2">
                <Input
                  value={trendProductInput}
                  onChange={(event) => setTrendProductInput(event.target.value)}
                  placeholder="Ejemplo: 15"
                  className="max-w-xs"
                />
                <Button type="submit">Consultar trend</Button>
              </form>
            </CardContent>
          </Card>
          {trend.isError ? <p className="text-sm text-destructive">{(trend.error as Error).message}</p> : null}
          <PriceChart title="Tendencia de precio promedio" points={trendPoints} />
        </TabsContent>

        <TabsContent value="inflation" className="space-y-2">
          <Card>
            <CardHeader>
              <CardTitle>Inflacion por categoria</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="mb-2 text-sm text-muted-foreground">
                Ingresa el ID de una categoria para ver su serie de inflacion mensual.
              </p>
              <form onSubmit={onInflationSubmit} className="flex flex-wrap gap-2">
                <Input
                  value={inflationCategoryInput}
                  onChange={(event) => setInflationCategoryInput(event.target.value)}
                  placeholder="Ejemplo: 1"
                  className="max-w-xs"
                />
                <Button type="submit">Consultar inflation</Button>
              </form>
            </CardContent>
          </Card>
          {inflation.isError ? (
            <p className="text-sm text-destructive">{(inflation.error as Error).message}</p>
          ) : null}
          <PriceChart title="Inflacion mensual (%)" points={inflationPoints} />
        </TabsContent>

        <TabsContent value="compare" className="space-y-2">
          <Card>
            <CardHeader>
              <CardTitle>Comparar productos</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="mb-2 text-sm text-muted-foreground">
                Ingresa entre 2 y 5 IDs separados por coma para comparar precios y variacion.
              </p>
              <form onSubmit={onCompareSubmit} className="flex flex-wrap gap-2">
                <Input
                  value={compareIdsInput}
                  onChange={(event) => setCompareIdsInput(event.target.value)}
                  placeholder="Ejemplo: 15,16,20"
                  className="max-w-xs"
                />
                <Button type="submit">Comparar</Button>
              </form>
            </CardContent>
          </Card>
          {compare.isPending ? <p>Cargando comparacion...</p> : null}
          {compare.isError ? (
            <p className="text-sm text-destructive">{(compare.error as Error).message}</p>
          ) : null}
          {compare.data ? (
            <Card>
              <CardHeader>
                <CardTitle>Comparacion</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                {compare.data.products.map((item) => (
                  <div key={item.productId} className="rounded-md border p-2 text-sm">
                    <p className="font-medium">{item.productName}</p>
                    <p>Promedio: {Number(item.avgPrice).toFixed(2)}</p>
                    <p>Variacion: {Number(item.variationPercentage).toFixed(2)}%</p>
                  </div>
                ))}
              </CardContent>
            </Card>
          ) : null}
        </TabsContent>
      </Tabs>
    </section>
  )
}
