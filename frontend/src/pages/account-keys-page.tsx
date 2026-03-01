import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '@/lib/api'
import { setApiKeyValue } from '@/lib/storage'

export function AccountKeysPage() {
  const queryClient = useQueryClient()
  const [newKeyName, setNewKeyName] = useState('Frontend key')
  const [newKeyValue, setNewKeyValue] = useState('')
  const [open, setOpen] = useState(false)

  const apiKeys = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => api.listApiKeys(),
  })

  const createMutation = useMutation({
    mutationFn: () => api.createApiKey(newKeyName),
    onSuccess: async (data) => {
      setNewKeyValue(data.keyValue)
      setApiKeyValue(data.keyValue)
      setOpen(false)
      setNewKeyName('Frontend key')
      await queryClient.invalidateQueries({ queryKey: ['api-keys'] })
      await queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })

  const onCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await createMutation.mutateAsync()
  }

  return (
    <section className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">API Keys</h1>
        <Dialog open={open} onOpenChange={setOpen}>
          <DialogTrigger asChild>
            <Button variant="secondary" size="sm">
              Crear API Key
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Crear API Key</DialogTitle>
            </DialogHeader>
            <form onSubmit={onCreate} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="keyName">Nombre</Label>
                <Input
                  id="keyName"
                  value={newKeyName}
                  onChange={(event) => setNewKeyName(event.target.value)}
                  placeholder="Nombre de la API key"
                />
              </div>
              <Button type="submit" disabled={createMutation.isPending}>
                {createMutation.isPending ? 'Creando...' : 'Crear'}
              </Button>
            </form>
            {newKeyValue ? (
              <div className="mt-4 rounded-md border bg-muted p-3 text-sm">
                <p className="mb-1 font-semibold">API key generada (guardala ahora)</p>
                <code className="break-all">{newKeyValue}</code>
              </div>
            ) : null}
          </DialogContent>
        </Dialog>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>API keys actuales</CardTitle>
        </CardHeader>
        <CardContent>
          {apiKeys.isPending ? <p>Cargando API keys...</p> : null}
          {apiKeys.isError ? (
            <p className="text-sm text-destructive">{(apiKeys.error as Error).message}</p>
          ) : null}
          {apiKeys.data ? (
            <ul className="space-y-2 text-sm">
              {apiKeys.data.map((item) => (
                <li key={`${item.keyPrefix}-${item.createdAt}`} className="rounded-md border p-3">
                  <p className="font-medium">{item.name}</p>
                  <p className="text-muted-foreground">Prefijo: {item.keyPrefix}</p>
                </li>
              ))}
            </ul>
          ) : null}
        </CardContent>
      </Card>
    </section>
  )
}
