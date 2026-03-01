import { useMemo, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { CircleUserRound, LogOut, UserRound } from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Footer } from '@/components/footer'
import { Input } from '@/components/ui/input'
import { api } from '@/lib/api'
import { getApiKeyValue, getAuthState, setApiKeyValue, setAuthState } from '@/lib/storage'

const items = [
  { to: '/app', label: 'Dashboard' },
  { to: '/app/products', label: 'Productos' },
  { to: '/app/categories', label: 'Categorias' },
  { to: '/app/compare', label: 'Comparar productos' },
  { to: '/app/analytics', label: 'Analytics' },
]

export function AppLayout() {
  const navigate = useNavigate()
  const [apiKey, setApiKey] = useState(getApiKeyValue())
  const isLogged = useMemo(() => Boolean(getAuthState()?.accessToken), [])

  const saveApiKey = () => {
    setApiKeyValue(apiKey)
    navigate(0)
  }

  const clearSession = async () => {
    try {
      if (isLogged) {
        await api.logout()
      }
    } catch {
      // no-op
    } finally {
      setAuthState(null)
      navigate('/auth/login')
    }
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b bg-card/60">
        <div className="mx-auto flex w-full max-w-7xl flex-wrap items-center gap-3 px-4 py-3">
          <Link to="/app" className="mr-2 text-lg font-semibold">
            CanastaUY Frontend
          </Link>
          <div className="flex flex-1 flex-wrap items-center gap-2">
            <Input
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Api-Key activa"
              className="w-full min-w-60 max-w-md"
            />
            <Button onClick={saveApiKey} size="sm" variant="secondary">
              Guardar API key
            </Button>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="icon" aria-label="Abrir menu de cuenta">
                <CircleUserRound className="size-5" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onSelect={() => navigate('/account/profile')}>
                <UserRound className="size-4" />
                Cuenta
              </DropdownMenuItem>
              <DropdownMenuSeparator />
              <DropdownMenuItem variant="destructive" onSelect={clearSession}>
                <LogOut className="size-4" />
                Cerrar sesion
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      <div className="mx-auto grid w-full max-w-7xl grid-cols-1 gap-6 px-4 py-6 md:grid-cols-[220px_1fr]">
        <aside className="space-y-2">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm transition ${
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-muted text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </aside>

        <main>
          <Outlet />
        </main>
      </div>

      <Footer />
    </div>
  )
}
