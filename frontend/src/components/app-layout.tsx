import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import {
  BarChart3,
  CircleUserRound,
  GitCompareArrows,
  LayoutDashboard,
  LogOut,
  Shapes,
  ShoppingBasket,
  UserRound,
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Footer } from '@/components/footer'
import { ThemeToggle } from '@/components/theme-toggle'
import { useCompareProducts } from '@/hooks/use-compare-products'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarMenuBadge,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  SidebarSeparator,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { api } from '@/lib/api'

const items = [
  { to: '/app', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/app/products', label: 'Productos', icon: ShoppingBasket },
  { to: '/app/categories', label: 'Categorias', icon: Shapes },
  { to: '/app/compare', label: 'Comparar productos', icon: GitCompareArrows },
  { to: '/app/analytics', label: 'Analytics', icon: BarChart3 },
]

export function AppLayout() {
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { count: compareCount } = useCompareProducts()

  const clearSession = async () => {
    try {
      await api.logout()
    } catch {
      // no-op
    } finally {
      queryClient.removeQueries({ queryKey: ['profile'] })
      navigate('/auth/login')
    }
  }

  const isItemActive = (to: string) => {
    if (to === '/app') {
      return location.pathname === '/app'
    }

    return location.pathname.startsWith(to)
  }

  return (
    <SidebarProvider>
      <Sidebar variant="inset" collapsible="icon">
        <SidebarHeader>
          <div className="flex items-center gap-2 px-2 py-1">
            <div className="flex size-8 items-center justify-center rounded-md bg-sidebar-primary text-sidebar-primary-foreground">
              <ShoppingBasket className="size-4" />
            </div>
            <div className="grid flex-1 text-sm leading-tight group-data-[collapsible=icon]:hidden">
              <span className="truncate font-semibold">CanastaUY</span>
              <span className="truncate text-xs text-sidebar-foreground/70">Frontend</span>
            </div>
          </div>
        </SidebarHeader>
        <SidebarSeparator />
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel>Navegacion</SidebarGroupLabel>
            <SidebarGroupContent>
              <SidebarMenu>
                {items.map((item) => (
                  <SidebarMenuItem key={item.to}>
                    <SidebarMenuButton asChild isActive={isItemActive(item.to)} tooltip={item.label}>
                      <Link to={item.to}>
                        <item.icon />
                        <span>{item.label}</span>
                      </Link>
                    </SidebarMenuButton>
                    {item.to === '/app/compare' && compareCount > 0 ? (
                      <SidebarMenuBadge>{compareCount}</SidebarMenuBadge>
                    ) : null}
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>
        <SidebarRail />
      </Sidebar>

      <SidebarInset>
        <header className="sticky top-0 z-20 border-b bg-card/80 backdrop-blur">
          <div className="flex h-14 items-center justify-between px-4 md:px-6">
            <div className="flex items-center gap-2">
              <SidebarTrigger className="-ml-1" />
              <h1 className="text-sm font-semibold md:text-base">CanastaUY Frontend</h1>
            </div>
            <div className="flex items-center gap-2">
              <ThemeToggle />
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
          </div>
        </header>

        <div className="flex-1 p-4 md:p-6">
          <Outlet />
        </div>
        <Footer />
      </SidebarInset>
    </SidebarProvider>
  )
}
