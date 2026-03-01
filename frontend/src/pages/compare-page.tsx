import { useMemo, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";

import { ApiKeyBanner } from "@/components/api-key-banner";
import { PriceChart } from "@/components/price-chart";
import { ProductItem } from "@/components/product-item";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { api } from "@/lib/api";
import type { Product } from "@/types/api";

type CompareRangePreset = "30d" | "90d" | "1y";

const COMPARE_SERIES_COLORS = [
  "#d97706",
  "#0ea5e9",
  "#22c55e",
  "#ef4444",
  "#a855f7",
];

const DAY_IN_MS = 24 * 60 * 60 * 1000;

function toUtcTime(date: string): number {
  const [year, month, day] = date.split("-").map(Number);
  if (!year || !month || !day) {
    return Number.NaN;
  }

  return Date.UTC(year, month - 1, day);
}

function getPresetDays(preset: CompareRangePreset): number {
  if (preset === "30d") {
    return 30;
  }
  if (preset === "90d") {
    return 90;
  }
  return 365;
}

export function ComparePage() {
  const [compareSearchInput, setCompareSearchInput] = useState("");
  const compareSearch = useDebouncedValue(compareSearchInput.trim(), 400);
  const [selectedCompareProducts, setSelectedCompareProducts] = useState<
    Product[]
  >([]);
  const [compareIds, setCompareIds] = useState("");
  const [compareRangePreset, setCompareRangePreset] =
    useState<CompareRangePreset>("1y");
  const [compareFormError, setCompareFormError] = useState("");

  const compare = useQuery({
    queryKey: ["analytics-compare", compareIds, "with-data"],
    queryFn: () => api.compareProducts(compareIds, undefined, undefined, true),
    enabled: Boolean(compareIds),
  });

  const compareSearchResults = useQuery({
    queryKey: ["analytics-compare-search", compareSearch],
    queryFn: () => api.searchProducts(compareSearch, 0, 10),
    enabled: compareSearch.length >= 2,
  });

  const compareSeriesBase = useMemo(
    () =>
      (compare.data?.products ?? [])
        .map((item, index) => ({
          name: item.productName,
          color: COMPARE_SERIES_COLORS[index % COMPARE_SERIES_COLORS.length],
          points: (item.data ?? []).map((point) => ({
            time: point.date,
            value: Number(point.priceAvg),
          })),
        }))
        .filter((item) => item.points.length > 0),
    [compare.data],
  );

  const compareSeries = useMemo(() => {
    if (compareSeriesBase.length === 0) {
      return [];
    }

    const maxTimestamp = Math.max(
      ...compareSeriesBase.flatMap((item) =>
        item.points.map((point) => toUtcTime(point.time)),
      ),
    );

    if (!Number.isFinite(maxTimestamp)) {
      return compareSeriesBase;
    }

    const days = getPresetDays(compareRangePreset);
    const minTimestamp = maxTimestamp - days * DAY_IN_MS;

    return compareSeriesBase
      .map((item) => ({
        ...item,
        points: item.points.filter((point) => {
          const timestamp = toUtcTime(point.time);
          return (
            Number.isFinite(timestamp) &&
            timestamp >= minTimestamp &&
            timestamp <= maxTimestamp
          );
        }),
      }))
      .filter((item) => item.points.length > 0);
  }, [compareRangePreset, compareSeriesBase]);

  const onCompareSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (
      selectedCompareProducts.length < 2 ||
      selectedCompareProducts.length > 5
    ) {
      setCompareFormError("Selecciona entre 2 y 5 productos para comparar.");
      setCompareIds("");
      return;
    }

    const ids = selectedCompareProducts.map((item) => item.productId).join(",");
    setCompareIds(ids);
    setCompareFormError("");
  };

  const onAddCompareProduct = (product: Product) => {
    setSelectedCompareProducts((current) => {
      if (
        current.some((item) => item.productId === product.productId) ||
        current.length >= 5
      ) {
        return current;
      }
      return [...current, product];
    });
    setCompareIds("");
    setCompareFormError("");
  };

  const onRemoveCompareProduct = (productId: number) => {
    setSelectedCompareProducts((current) =>
      current.filter((item) => item.productId !== productId),
    );
    setCompareIds("");
    setCompareFormError("");
  };

  const availableCompareResults = useMemo(
    () =>
      (compareSearchResults.data?.products ?? []).filter(
        (product) =>
          !selectedCompareProducts.some(
            (selected) => selected.productId === product.productId,
          ),
      ),
    [compareSearchResults.data, selectedCompareProducts],
  );

  return (
    <section className="space-y-4">
      <ApiKeyBanner />
      <h1 className="text-2xl font-semibold">Comparacion de productos</h1>

      <Card>
        <CardHeader>
          <CardTitle>Comparar productos</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="mb-2 text-sm text-muted-foreground">
            Busca productos por nombre, selecciona entre 2 y 5, y compara su
            evolucion de precio.
          </p>
          <Combobox
            items={availableCompareResults}
            itemToStringValue={(product: unknown) => (product as Product).name}
            onValueChange={(value: unknown) => {
              const selected = value as Product | null;
              if (!selected) {
                return;
              }
              onAddCompareProduct(selected);
              setCompareSearchInput("");
            }}
          >
            <ComboboxInput
              value={compareSearchInput}
              onChange={(event: ChangeEvent<HTMLInputElement>) =>
                setCompareSearchInput(event.target.value)
              }
              placeholder="Buscar producto por nombre"
              showClear
            >
              <Search className="size-4 text-muted-foreground" />
            </ComboboxInput>
            <ComboboxContent>
              <ComboboxEmpty>No se encontraron productos.</ComboboxEmpty>
              <ComboboxList>
                {(product: unknown) => (
                  <ComboboxItem
                    key={(product as Product).productId}
                    value={product as Product}
                  >
                    <div className="flex min-w-0 flex-col">
                      <span className="truncate">{(product as Product).name}</span>
                      <span className="text-xs text-muted-foreground">
                        {(product as Product).brand ?? "Sin marca"}
                      </span>
                    </div>
                  </ComboboxItem>
                )}
              </ComboboxList>
            </ComboboxContent>
          </Combobox>
          <div className="mt-2">
            {compareSearchResults.isError ? (
              <p className="text-sm text-destructive">
                {(compareSearchResults.error as Error).message}
              </p>
            ) : null}
          </div>
          <div className="mt-4 space-y-2">
            <p className="text-sm font-medium">
              Items seleccionados ({selectedCompareProducts.length}/5)
            </p>
            {selectedCompareProducts.length === 0 ? (
              <p className="text-sm text-muted-foreground">
                Todavia no seleccionaste productos.
              </p>
            ) : (
              <ul className="space-y-2">
                {selectedCompareProducts.map((product) => (
                  <li key={product.productId} className="flex items-center gap-2">
                    <div className="min-w-0 flex-1">
                      <ProductItem product={product} showActions={false} />
                    </div>
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      onClick={() => onRemoveCompareProduct(product.productId)}
                    >
                      Quitar
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </div>
          <form onSubmit={onCompareSubmit} className="flex flex-wrap gap-2">
            <Button type="submit" disabled={selectedCompareProducts.length < 2}>
              Comparar seleccionados
            </Button>
          </form>
          {compareFormError ? (
            <p className="mt-2 text-sm text-destructive">{compareFormError}</p>
          ) : null}
          <div className="mt-3 flex flex-wrap gap-2">
            <Button
              type="button"
              variant={compareRangePreset === "30d" ? "default" : "outline"}
              onClick={() => setCompareRangePreset("30d")}
            >
              30d
            </Button>
            <Button
              type="button"
              variant={compareRangePreset === "90d" ? "default" : "outline"}
              onClick={() => setCompareRangePreset("90d")}
            >
              90d
            </Button>
            <Button
              type="button"
              variant={compareRangePreset === "1y" ? "default" : "outline"}
              onClick={() => setCompareRangePreset("1y")}
            >
              1y
            </Button>
          </div>
        </CardContent>
      </Card>

      {compare.isPending ? <p>Cargando comparacion...</p> : null}
      {compare.isError ? (
        <p className="text-sm text-destructive">{(compare.error as Error).message}</p>
      ) : null}
      {compareIds ? (
        <PriceChart title="Comparacion de precio promedio" series={compareSeries} />
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
    </section>
  );
}
