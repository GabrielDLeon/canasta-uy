import { useMemo, useState } from "react";
import type { ChangeEvent, FormEvent } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { toast } from "sonner";

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
import { useCompareProducts } from "@/hooks/use-compare-products";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { api } from "@/lib/api";
import type { Product } from "@/types/api";

const COMPARE_SERIES_COLORS = [
  "#9c5a24",
  "#3c6e71",
  "#708238",
  "#b34f45",
  "#6e4f8a",
];

const DAY_IN_MS = 24 * 60 * 60 * 1000;

function toUtcTime(date: string): number {
  const [year, month, day] = date.split("-").map(Number);
  if (!year || !month || !day) {
    return Number.NaN;
  }

  return Date.UTC(year, month - 1, day);
}

export function ComparePage() {
  const { products: selectedCompareProducts, addProduct, removeProduct } =
    useCompareProducts();
  const [compareSearchInput, setCompareSearchInput] = useState("");
  const [isCompareComboboxOpen, setIsCompareComboboxOpen] = useState(false);
  const compareSearch = useDebouncedValue(compareSearchInput.trim(), 400);
  const [compareIds, setCompareIds] = useState("");
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

    const minTimestamp = maxTimestamp - 365 * DAY_IN_MS;

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
  }, [compareSeriesBase]);

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
    const result = addProduct(product);
    setCompareIds("");
    setCompareFormError("");

    if (result === "limit") {
      toast.error("Solo puedes comparar hasta 5 productos.");
    }
  };

  const onRemoveCompareProduct = (productId: number) => {
    removeProduct(productId);
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
            open={isCompareComboboxOpen}
            onOpenChange={(open, eventDetails) => {
              if (!open && eventDetails.reason === "item-press") {
                return;
              }

              setIsCompareComboboxOpen(open);
            }}
            items={availableCompareResults}
            itemToStringValue={(product: unknown) => (product as Product).name}
            onValueChange={(value: unknown) => {
              const selected = value as Product | null;
              if (!selected) {
                return;
              }
              onAddCompareProduct(selected);
              setIsCompareComboboxOpen(true);
            }}
          >
            <ComboboxInput
              value={compareSearchInput}
              onChange={(event: ChangeEvent<HTMLInputElement>) => {
                setCompareSearchInput(event.target.value);
                setIsCompareComboboxOpen(true);
              }}
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
                      <ProductItem
                        product={product}
                        showActions={false}
                        showCompareAction={false}
                      />
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
