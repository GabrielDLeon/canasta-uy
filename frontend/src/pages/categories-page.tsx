import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { Search } from 'lucide-react'

import { ApiKeyBanner } from '@/components/api-key-banner'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Item, ItemContent, ItemTitle, ItemActions } from '@/components/ui/item'
import { useDebouncedValue } from '@/hooks/use-debounced-value'
import { api } from '@/lib/api'

export function CategoriesPage() {
  const [query, setQuery] = useState('')
  const debouncedQuery = useDebouncedValue(query.trim().toLowerCase(), 500)

  const categories = useQuery({
    queryKey: ['categories'],
    queryFn: () => api.getCategories(),
  })

  const filtered = useMemo(() => {
    if (!categories.data) {
      return []
    }

    if (!debouncedQuery) {
      return categories.data
    }

    return categories.data.filter((category) => category.name.toLowerCase().includes(debouncedQuery))
  }, [categories.data, debouncedQuery])

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

      {categories.isPending ? <p>Cargando categorias...</p> : null}
      {categories.isError ? (
        <p className="text-sm text-destructive">{(categories.error as Error).message}</p>
      ) : null}
      {categories.data ? (
        <ul className="grid gap-2 md:grid-cols-2">
          {filtered.map((category) => (
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

      {categories.data && filtered.length === 0 ? (
        <p className="text-sm text-muted-foreground">No hay categorias que coincidan con la busqueda.</p>
      ) : null}
    </section>
  )
}
