import { useQuery } from '@tanstack/react-query'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'

export function DashboardPage() {
  const topChanges = useQuery({
    queryKey: ['top-changes'],
    queryFn: () => api.getTopChanges('30d', 'all', 8),
  })

  return (
    <section>
      <ApiKeyBanner />
      <h1 className="mb-4 text-2xl font-semibold">Dashboard</h1>
      <Card>
        <CardHeader>
          <CardTitle>Top variaciones (30 dias)</CardTitle>
        </CardHeader>
        <CardContent>
          {topChanges.isPending ? <p>Cargando...</p> : null}
          {topChanges.isError ? (
            <p className="text-sm text-destructive">{(topChanges.error as Error).message}</p>
          ) : null}
          {topChanges.data ? (
            <div className="space-y-2">
              {topChanges.data.changes.map((item) => (
                <div
                  key={`${item.productId}-${item.changeDirection}`}
                  className="flex items-center justify-between rounded-md border p-3"
                >
                  <div>
                    <p className="font-medium">{item.productName}</p>
                    <p className="text-xs text-muted-foreground">{item.category}</p>
                  </div>
                  <p className="text-sm font-semibold">
                    {item.changePercentage > 0 ? '+' : ''}
                    {Number(item.changePercentage).toFixed(2)}%
                  </p>
                </div>
              ))}
            </div>
          ) : null}
        </CardContent>
      </Card>
    </section>
  )
}
