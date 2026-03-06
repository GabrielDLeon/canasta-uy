import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { useDebouncedValue } from "@/hooks/use-debounced-value";
import { api } from "@/lib/api";
import type { Category, Product } from "@/types/api";
import { Button } from "@/components/ui/button";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { useSidebar } from "@/components/ui/sidebar";

const MIN_QUERY_LENGTH = 2;
const RESULT_LIMIT = 8;

export function SidebarSearch() {
  const navigate = useNavigate();
  const { isMobile, state } = useSidebar();
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");

  const searchQuery = useDebouncedValue(input.trim(), 450);
  const shouldSearch = searchQuery.length >= MIN_QUERY_LENGTH;
  const isIconCollapsed = !isMobile && state === "collapsed";

  const productsSearch = useQuery({
    queryKey: ["sidebar-search-products", searchQuery],
    queryFn: () => api.searchProducts(searchQuery, 0, RESULT_LIMIT),
    enabled: shouldSearch,
  });

  const categoriesSearch = useQuery({
    queryKey: ["sidebar-search-categories", searchQuery],
    queryFn: () => api.searchCategories(searchQuery, 0, RESULT_LIMIT),
    enabled: shouldSearch,
  });

  const products = productsSearch.data?.products ?? [];
  const categories = categoriesSearch.data?.categories ?? [];

  const hasError = productsSearch.isError || categoriesSearch.isError;

  const hasResults = useMemo(
    () => products.length > 0 || categories.length > 0,
    [products.length, categories.length],
  );

  const closeAndReset = () => {
    setOpen(false);
    setInput("");
  };

  const onSelectProduct = (productId: number) => {
    closeAndReset();
    navigate(`/app/products/${productId}`);
  };

  const onSelectCategory = (categoryId: number) => {
    closeAndReset();
    navigate(`/app/categories/${categoryId}`);
  };

  return (
    <Popover
      open={open}
      onOpenChange={(nextOpen) => {
        setOpen(nextOpen);
        if (!nextOpen) {
          setInput("");
        }
      }}
    >
      <PopoverTrigger asChild>
        {isIconCollapsed ? (
          <Button variant="ghost" size="icon" aria-label="Buscar">
            <Search className="size-4" />
          </Button>
        ) : (
          <Button
            variant="outline"
            className="h-8 w-full justify-start text-muted-foreground"
          >
            <Search className="size-4" />
            <span className="text-xs">Buscar...</span>
          </Button>
        )}
      </PopoverTrigger>
      <PopoverContent
        align="start"
        className="w-80 p-0"
        side={isIconCollapsed ? "right" : "bottom"}
      >
        <Command shouldFilter={false}>
          <CommandInput
            value={input}
            onValueChange={setInput}
            placeholder="Buscar producto o categoria..."
          />
          <CommandList>
            {shouldSearch && hasError ? (
              <CommandGroup>
                <CommandItem disabled>Error al buscar resultados.</CommandItem>
              </CommandGroup>
            ) : null}
            {shouldSearch && !hasError ? (
              <>
                {!hasResults ? (
                  <CommandEmpty>No se encontraron resultados.</CommandEmpty>
                ) : null}
                {products.length > 0 ? (
                  <CommandGroup heading="Productos">
                    {products.map((product: Product) => (
                      <CommandItem
                        key={`product-${product.productId}`}
                        value={`product-${product.productId}-${product.name}`}
                        onSelect={() => onSelectProduct(product.productId)}
                      >
                        <div className="flex min-w-0 flex-col">
                          <span className="truncate">{product.name}</span>
                          <span className="text-xs text-muted-foreground">
                            {product.brand ?? "Sin marca"}
                          </span>
                        </div>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
                {products.length > 0 && categories.length > 0 ? (
                  <CommandSeparator />
                ) : null}
                {categories.length > 0 ? (
                  <CommandGroup heading="Categorias">
                    {categories.map((category: Category) => (
                      <CommandItem
                        key={`category-${category.categoryId}`}
                        value={`category-${category.categoryId}-${category.name}`}
                        onSelect={() => onSelectCategory(category.categoryId)}
                      >
                        <span className="truncate">{category.name}</span>
                      </CommandItem>
                    ))}
                  </CommandGroup>
                ) : null}
              </>
            ) : null}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  );
}
