import { useEffect, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Search } from 'lucide-react'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Item, ItemContent, ItemTitle, ItemActions } from '@/components/ui/item'
import { Skeleton } from '@/components/ui/skeleton'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { api } from '@/lib/api'

export function CategoriesPage() {
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const debouncedQuery = useDebouncedValue(query.trim().toLowerCase(), 500)

  useEffect(() => {
    setPage(0)
  }, [debouncedQuery])

  const categories = useQuery({
    queryKey: ['categories', debouncedQuery, page],
    queryFn: () =>
      debouncedQuery ? api.searchCategories(debouncedQuery, page, 20) : api.getCategories(page, 20),
  })

  const list = categories.data?.categories ?? []
  const pagination = categories.data?.pagination

  return (
    <section>
      <ApiKeyBanner />
      <h1 className="mb-4 text-2xl font-semibold">Categorias</h1>

      <div className="mb-4">
        <div className="relative max-w-sm">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Buscar categoria por nombre"
            className="pl-9"
          />
        </div>
      </div>

      {categories.isPending ? (
        <ul className="grid gap-2 md:grid-cols-2">
          {Array.from({ length: 8 }).map((_, index) => (
            <li key={`category-skeleton-${index}`}>
              <Item variant="outline">
                <ItemContent>
                  <Skeleton className="h-4 w-2/3 bg-muted/60" />
                </ItemContent>
                <ItemActions>
                  <Skeleton className="h-8 w-16 bg-muted/60" />
                </ItemActions>
              </Item>
            </li>
          ))}
        </ul>
      ) : null}
      {categories.isError ? (
        <p className="text-sm text-destructive">{(categories.error as Error).message}</p>
      ) : null}
      {categories.data ? (
        <ul className="grid gap-2 md:grid-cols-2">
          {list.map((category) => (
            <li key={category.categoryId}>
              <Item variant="outline">
                <ItemContent>
                  <ItemTitle>{category.name}</ItemTitle>
                </ItemContent>
                <ItemActions>
                  <Button asChild variant="secondary" size="sm">
                    <Link to={`/app/categories/${category.categoryId}`}>Ver</Link>
                  </Button>
                </ItemActions>
              </Item>
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

      {categories.data && list.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay categorias que coincidan con la busqueda.</p>
      ) : null}
    </section>
  )
}
