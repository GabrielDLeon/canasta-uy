import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'

import { ThemeToggle } from '@/components/theme-toggle'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { api } from '@/lib/api'
import { setAuthState } from '@/lib/storage'

export function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [invalidCredentials, setInvalidCredentials] = useState(false)
  const [loading, setLoading] = useState(false)

  const onSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setInvalidCredentials(false)
    setLoading(true)
    try {
      const data = await api.login(email, password)
      setAuthState({ accessToken: data.accessToken, refreshToken: data.refreshToken })
      navigate('/app')
    } catch (e) {
      const message = e instanceof Error ? e.message : ''
      const isInvalidLogin = /validation failed|invalid|unauthorized|401/i.test(message)

      if (isInvalidLogin) {
        setInvalidCredentials(true)
      }

      toast.error(
        isInvalidLogin
          ? 'Credenciales invalidas. Verifica email y contrasena.'
          : message || 'No se pudo iniciar sesion',
      )
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-muted/30 px-4">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Iniciar sesion</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSubmit} className="space-y-3">
            <Input
              type="email"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value)
                setInvalidCredentials(false)
              }}
              placeholder="email@dominio.com"
              aria-invalid={invalidCredentials}
              required
            />
            <Input
              type="password"
              value={password}
              onChange={(event) => {
                setPassword(event.target.value)
                setInvalidCredentials(false)
              }}
              placeholder="Contrasena"
              aria-invalid={invalidCredentials}
              required
            />
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
