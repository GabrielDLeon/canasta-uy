import { useQuery } from '@tanstack/react-query'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'

function formatCurrency(value: number): string {
  return `$${Number(value).toFixed(2)}`
}

export function DashboardPage() {
  const dashboard = useQuery({
    queryKey: ['dashboard-summary', '30d', 5],
    queryFn: () => api.getDashboardSummary('30d', 5),
  })

  return (
    <section className="space-y-4">
      <ApiKeyBanner />
      <h1 className="mb-4 text-2xl font-semibold">Dashboard</h1>

      {dashboard.isPending ? <p>Cargando...</p> : null}
      {dashboard.isError ? (
        <p className="text-sm text-destructive">{(dashboard.error as Error).message}</p>
      ) : null}

      {dashboard.data ? (
        <div className="grid gap-4 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>Panorama mercado (30 dias)</CardTitle>
            </CardHeader>
            <CardContent className="space-y-1 text-sm">
              <p>
                Promedio actual: <strong>{formatCurrency(dashboard.data.marketSnapshot.avgPriceCurrent)}</strong>
              </p>
              <p>
                Promedio periodo anterior:{' '}
                <strong>{formatCurrency(dashboard.data.marketSnapshot.avgPricePrevious)}</strong>
              </p>
              <p>
                Cambio: <strong>{Number(dashboard.data.marketSnapshot.changePercentage).toFixed(2)}%</strong> (
                {formatCurrency(dashboard.data.marketSnapshot.changeAbsolute)})
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Top subas</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {dashboard.data.topIncreases.map((item) => (
                <div key={item.productId} className="flex items-center justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{item.productName}</p>
                    <p className="text-xs text-muted-foreground">{item.category}</p>
                  </div>
                  <p className="text-sm font-semibold text-green-700">
                    +{Number(item.changePercentage).toFixed(2)}%
                  </p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Top bajas</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {dashboard.data.topDecreases.map((item) => (
                <div key={item.productId} className="flex items-center justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{item.productName}</p>
                    <p className="text-xs text-muted-foreground">{item.category}</p>
                  </div>
                  <p className="text-sm font-semibold text-red-700">
                    {Number(item.changePercentage).toFixed(2)}%
                  </p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Categorias con mayor movimiento</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {dashboard.data.categoryChanges.map((item) => (
                <div key={item.category} className="flex items-center justify-between rounded-md border p-3">
                  <div>
                    <p className="font-medium">{item.category}</p>
                    <p className="text-xs text-muted-foreground">{item.productsCount} productos</p>
                  </div>
                  <p className="text-sm font-semibold">
                    {Number(item.avgChangePercentage).toFixed(2)}%
                  </p>
                </div>
              ))}
            </CardContent>
          </Card>

          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle>Volatilidad de productos</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 md:grid-cols-2">
              <div className="space-y-2">
                <p className="text-sm font-medium">Mas volatiles (CV %)</p>
                {dashboard.data.volatility.mostVolatile.map((item) => (
                  <div key={item.productId} className="flex items-center justify-between rounded-md border p-3">
                    <div>
                      <p className="font-medium">{item.productName}</p>
                      <p className="text-xs text-muted-foreground">{item.category}</p>
                    </div>
                    <p className="text-sm font-semibold">{Number(item.coefficientOfVariation).toFixed(2)}%</p>
                  </div>
                ))}
              </div>
              <div className="space-y-2">
                <p className="text-sm font-medium">Mas estables (CV %)</p>
                {dashboard.data.volatility.mostStable.map((item) => (
                  <div key={item.productId} className="flex items-center justify-between rounded-md border p-3">
                    <div>
                      <p className="font-medium">{item.productName}</p>
                      <p className="text-xs text-muted-foreground">{item.category}</p>
                    </div>
                    <p className="text-sm font-semibold">{Number(item.coefficientOfVariation).toFixed(2)}%</p>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      ) : null}
    </section>
  )
}
