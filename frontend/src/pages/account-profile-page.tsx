import { useQuery } from '@tanstack/react-query'

import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { api } from '@/lib/api'

export function AccountProfilePage() {
  const profile = useQuery({
    queryKey: ['profile'],
    queryFn: () => api.getProfile(),
  })

  return (
    <section className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">Perfil</h1>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Datos de la cuenta</CardTitle>
        </CardHeader>
        <CardContent>
          {profile.isPending ? <p>Cargando perfil...</p> : null}
          {profile.isError ? (
            <p className="text-sm text-destructive">{(profile.error as Error).message}</p>
          ) : null}
          {profile.data ? (
            <div className="space-y-1 text-sm">
              <p>Email: {profile.data.email}</p>
              <p>API keys activas: {profile.data.totalKeys}</p>
              <p>Creada en: {new Date(profile.data.createdAt).toLocaleDateString('es-UY')}</p>
            </div>
          ) : null}
        </CardContent>
      </Card>
    </section>
  )
}
