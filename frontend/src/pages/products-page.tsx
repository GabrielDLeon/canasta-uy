import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";

import { ApiKeyBanner } from "@/components/api-key-banner";
import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { ProductItem } from "@/components/product-item";
import { api } from "@/lib/api";

export function ProductsPage() {
  const [query, setQuery] = useState("");
  const search = useDebouncedValue(query.trim(), 500);

  const products = useQuery({
    queryKey: ["products", search],
    queryFn: () => (search ? api.searchProducts(search) : api.getProducts()),
  });

  return (
    <section>
      <ApiKeyBanner />
      <h1 className="mb-4 text-2xl font-semibold">Productos</h1>

      <div className="mb-4">
        <div className="relative max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Buscar por nombre"
            className="pl-9"
          />
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Resultados</CardTitle>
        </CardHeader>
        <CardContent>
          {products.isPending ? <p>Cargando productos...</p> : null}
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
        </CardContent>
      </Card>
    </section>
  );
}
