import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";

import { ApiKeyBanner } from "@/components/api-key-banner";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Item,
  ItemActions,
  ItemContent,
  ItemDescription,
  ItemMedia,
  ItemTitle,
} from "@/components/ui/item";
import { Skeleton } from "@/components/ui/skeleton";
import { ProductItem } from "@/components/product-item";
import { api } from "@/lib/api";

export function ProductsPage() {
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const search = useDebouncedValue(query.trim(), 500);

  const products = useQuery({
    queryKey: ["products", search, page],
    queryFn: () =>
      search ? api.searchProducts(search, page, 20) : api.getProducts(page, 20),
  });

  const pagination = products.data?.pagination;

  return (
    <section>
      <ApiKeyBanner />
      <h1 className="mb-4 text-2xl font-semibold">Productos</h1>

      <div className="mb-4">
        <div className="relative max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setPage(0);
            }}
            placeholder="Buscar por nombre"
            className="pl-9"
          />
        </div>
      </div>

      {products.isPending ? (
        <ul className="space-y-2">
          {Array.from({ length: 8 }).map((_, index) => (
            <li key={`product-skeleton-${index}`}>
              <Item variant="outline">
                <ItemMedia variant="image">
                  <Skeleton className="h-10 w-10 rounded-xl bg-muted/60" />
                </ItemMedia>
                <ItemContent>
                  <ItemTitle>
                    <Skeleton className="h-4 w-3/5 bg-muted/60" />
                  </ItemTitle>
                  <ItemDescription>
                    <Skeleton className="h-3 w-1/3 bg-muted/60" />
                  </ItemDescription>
                </ItemContent>
                <ItemActions>
                  <Skeleton className="h-8 w-10 bg-muted/60" />
                  <Skeleton className="h-8 w-16 bg-muted/60" />
                </ItemActions>
              </Item>
            </li>
          ))}
        </ul>
      ) : null}
      {products.isError ? (
        <p className="text-sm text-destructive">
          {(products.error as Error).message}
        </p>
      ) : null}
      {products.data ? (
        <ul className="space-y-2">
          {products.data.products.map((product) => (
            <li key={product.productId}>
              <ProductItem product={product} />
            </li>
          ))}
        </ul>
      ) : null}

      {pagination ? (
        <div className="mt-4 flex items-center gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!pagination.hasPrevious}
            onClick={() => setPage((current) => Math.max(current - 1, 0))}
          >
            Anterior
          </Button>
          <span className="text-sm text-muted-foreground">
            Pagina {pagination.page + 1} de {Math.max(pagination.totalPages, 1)}
          </span>
          <Button
            type="button"
            variant="outline"
            size="sm"
            disabled={!pagination.hasNext}
            onClick={() => setPage((current) => current + 1)}
          >
            Siguiente
          </Button>
        </div>
      ) : null}
    </section>
  );
}
