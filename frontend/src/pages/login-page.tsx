import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { api } from '@/lib/api'
import { setAuthState } from '@/lib/storage'

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const data = await api.login(email, password)
      setAuthState({ accessToken: data.accessToken, refreshToken: data.refreshToken })
      navigate('/app')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo iniciar sesion')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Iniciar sesion</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-3">
            <Input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="email@dominio.com"
              required
            />
            <Input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Contrasena"
              required
            />
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button disabled={loading} className="w-full" type="submit">
              {loading ? 'Ingresando...' : 'Ingresar'}
            </Button>
          </form>
          <p className="mt-4 text-sm text-muted-foreground">
            No tenes cuenta?{' '}
            <Link to="/auth/register" className="font-medium text-primary">
              Registrate
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
