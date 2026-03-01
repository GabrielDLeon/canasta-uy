import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { api } from '@/lib/api'

export function RegisterPage() {
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
      await api.register(email, password)
      navigate('/auth/login')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'No se pudo registrar el usuario')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/30 px-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Crear cuenta</CardTitle>
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
              minLength={8}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              placeholder="Minimo 8 caracteres"
              required
            />
            {error ? <p className="text-sm text-destructive">{error}</p> : null}
            <Button disabled={loading} className="w-full" type="submit">
              {loading ? 'Creando cuenta...' : 'Registrarme'}
            </Button>
          </form>
          <p className="mt-4 text-sm text-muted-foreground">
            Ya tenes cuenta?{' '}
            <Link to="/auth/login" className="font-medium text-primary">
              Inicia sesion
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
