import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ThemeToggle } from '@/components/theme-toggle'

export function LandingPage() {
  return (
    <main className="min-h-screen bg-muted/20 px-4 py-14">
      <section className="mx-auto w-full max-w-4xl space-y-10">
        <div className="flex justify-end">
          <ThemeToggle />
        </div>
        <div className="space-y-3 text-center">
          <p className="text-sm font-medium uppercase tracking-wide text-primary">CanastaUY</p>
          <h1 className="text-4xl font-semibold tracking-tight">Visualizador de precios en Uruguay</h1>
          <p className="mx-auto max-w-2xl text-muted-foreground">
            Explora productos, categorias y analytics con datos historicos del backend de CanastaUY.
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <Button asChild size="lg">
            <Link to="/auth/login">Iniciar sesion</Link>
          </Button>
          <Button asChild size="lg" variant="outline">
            <Link to="/auth/register">Crear cuenta</Link>
          </Button>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>Que incluye la app</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 text-sm md:grid-cols-3">
            <div className="rounded-lg border p-3">
              <p className="font-medium">Catalogo</p>
              <p className="text-muted-foreground">Busqueda de productos y detalle historico de precios.</p>
            </div>
            <div className="rounded-lg border p-3">
              <p className="font-medium">Categorias</p>
              <p className="text-muted-foreground">Estadisticas agregadas y listado de productos por categoria.</p>
            </div>
            <div className="rounded-lg border p-3">
              <p className="font-medium">Analytics</p>
              <p className="text-muted-foreground">Trend, inflation y comparacion con graficos interactivos.</p>
            </div>
          </CardContent>
        </Card>
      </section>
    </main>
  )
}
