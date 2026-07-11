import { useMemo, useState } from "react";
import type { FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";

import { usePageTitle } from "@/hooks/use-page-title";
import { PriceChart } from "@/components/price-chart";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api } from "@/lib/api";

export function AnalyticsPage() {
  usePageTitle("Analytics");

  const [trendProductInput, setTrendProductInput] = useState("15");
  const [trendProductId, setTrendProductId] = useState("15");

  const trend = useQuery({
    queryKey: ["analytics-trend", trendProductId],
    queryFn: () => api.getTrend(Number(trendProductId)),
    enabled: Boolean(trendProductId),
  });

  const trendPoints = useMemo(
    () =>
      (trend.data?.data ?? []).map((item) => ({
        time: item.date,
        value: Number(item.priceAvg),
      })),
    [trend.data],
  );

  const onTrendSubmit = (event: FormEvent) => {
    event.preventDefault();
    setTrendProductId(trendProductInput.trim());
  };

  return (
    <section className="space-y-4">
      <h1 className="text-2xl font-semibold">Analytics</h1>

      <Card>
        <CardHeader>
          <CardTitle>Tendencia por producto</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-2 text-sm text-muted-foreground">
            Ingresa el ID de un producto para ver su evolucion de precio
            promedio.
          </p>
          <form onSubmit={onTrendSubmit} className="flex flex-wrap gap-2">
            <div className="space-y-2">
              <Label htmlFor="trend-product-id">ID de producto</Label>
                <Input
                  id="trend-product-id"
                  value={trendProductInput}
                  onChange={(event) => setTrendProductInput(event.target.value)}
                  placeholder="Ejemplo: 15"
                  className="max-w-xs"
                />
            </div>
            <Button type="submit">Consultar trend</Button>
          </form>
        </CardContent>
      </Card>
      {trend.isError ? (
        <p role="alert" className="text-sm text-destructive">
          {(trend.error as Error).message}
        </p>
      ) : null}
      <PriceChart title="Tendencia de precio promedio" points={trendPoints} />
    </section>
  );
}
