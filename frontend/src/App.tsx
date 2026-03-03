import { Navigate, Route, Routes } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useQuery } from '@tanstack/react-query'

import { AccountLayout } from '@/components/account-layout'
import { AppLayout } from '@/components/app-layout'
import { api } from '@/lib/api'
import { AccountKeysPage } from '@/pages/account-keys-page'
import { AccountProfilePage } from '@/pages/account-profile-page'
import { AnalyticsPage } from '@/pages/analytics-page'
import { CategoriesPage } from '@/pages/categories-page'
import { CategoryDetailPage } from '@/pages/category-detail-page'
import { ComparePage } from '@/pages/compare-page'
import { DashboardPage } from '@/pages/dashboard-page'
import { LandingPage } from '@/pages/landing-page'
import { LoginPage } from '@/pages/login-page'
import { ProductDetailPage } from '@/pages/product-detail-page'
import { ProductsPage } from '@/pages/products-page'
import { RegisterPage } from '@/pages/register-page'

function RequireAuth({ children }: { children: ReactNode }) {
  const profile = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.getProfile(),
    retry: false,
  })

  if (profile.isPending) {
    return <p className="p-4 text-sm text-muted-foreground">Verificando sesion...</p>
  }

  if (profile.isError) {
    return <Navigate to="/auth/login" replace />
  }

  return children
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/auth/login" element={<LoginPage />} />
      <Route path="/auth/register" element={<RegisterPage />} />

      <Route
        path="/app"
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="products" element={<ProductsPage />} />
        <Route path="products/:id" element={<ProductDetailPage />} />
        <Route path="categories" element={<CategoriesPage />} />
        <Route path="categories/:id" element={<CategoryDetailPage />} />
        <Route path="analytics" element={<AnalyticsPage />} />
        <Route path="compare" element={<ComparePage />} />
      </Route>

      <Route
        path="/account"
        element={
          <RequireAuth>
            <AccountLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="profile" replace />} />
        <Route path="profile" element={<AccountProfilePage />} />
        <Route path="keys" element={<AccountKeysPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
