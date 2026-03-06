import { useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { api } from '@/lib/api'
import { getApiKeyValue, setApiKeyValue } from '@/lib/storage'
import type { ApiKeyListItem } from '@/types/api'

const keyDateFormatter = new Intl.DateTimeFormat('es-UY', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export function AccountKeysPage() {
  const queryClient = useQueryClient()
  const [newKeyName, setNewKeyName] = useState('Frontend key')
  const [newKeyValue, setNewKeyValue] = useState('')
  const [activeApiKey, setActiveApiKey] = useState(getApiKeyValue())
  const [open, setOpen] = useState(false)
  const [selectedKey, setSelectedKey] = useState<ApiKeyListItem | null>(null)

  const apiKeys = useQuery({
    queryKey: ['api-keys'],
    queryFn: () => api.listApiKeys(),
  })

  const createMutation = useMutation({
    mutationFn: () => api.createApiKey(newKeyName),
    onSuccess: async (data) => {
      setNewKeyValue(data.keyValue)
      setApiKeyValue(data.keyValue)
      setActiveApiKey(data.keyValue)
      setOpen(false)
      setNewKeyName('Frontend key')
      await queryClient.invalidateQueries({ queryKey: ['api-keys'] })
      await queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })

  const revokeMutation = useMutation({
    mutationFn: async (item: ApiKeyListItem) => {
      await api.revokeApiKey(item.id)
      return item
    },
    onSuccess: async (item) => {
      const keyStart = item.keyPrefix.split('...')[0]
      if (activeApiKey && keyStart && activeApiKey.startsWith(keyStart)) {
        setApiKeyValue('')
        setActiveApiKey('')
      }
      setSelectedKey(null)
      await queryClient.invalidateQueries({ queryKey: ['api-keys'] })
      await queryClient.invalidateQueries({ queryKey: ['profile'] })
    },
  })

  const onCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await createMutation.mutateAsync()
  }

  const onSaveActiveApiKey = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setApiKeyValue(activeApiKey)
    setActiveApiKey(getApiKeyValue())
  }

  const onClearActiveApiKey = () => {
    setApiKeyValue('')
    setActiveApiKey('')
  }

  const closeRevokeDialog = () => {
    if (revokeMutation.isPending) {
      return
    }
    setSelectedKey(null)
  }

  const onConfirmRevoke = async () => {
    if (!selectedKey) {
      return
    }
    await revokeMutation.mutateAsync(selectedKey)
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
          <CardTitle>API key activa en frontend</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onSaveActiveApiKey} className="flex flex-wrap items-center gap-2">
            <Input
              value={activeApiKey}
              onChange={(event) => setActiveApiKey(event.target.value)}
              placeholder="Pega aqui la API key activa"
              className="min-w-60 flex-1"
            />
            <Button type="submit" variant="secondary" size="sm">
              Guardar
            </Button>
            <Button type="button" variant="outline" size="sm" onClick={onClearActiveApiKey}>
              Limpiar
            </Button>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>API keys actuales</CardTitle>
        </CardHeader>
        <CardContent>
          {apiKeys.isPending ? <p>Cargando API keys...</p> : null}
          {apiKeys.isError ? (
            <p className="text-sm text-destructive">{(apiKeys.error as Error).message}</p>
          ) : null}
          {revokeMutation.isError ? (
            <p className="text-sm text-destructive">
              {(revokeMutation.error as Error).message}
            </p>
          ) : null}
          {apiKeys.data ? (
            <ul className="space-y-2 text-sm">
              {apiKeys.data.map((item) => (
                <li
                  key={item.id}
                  className="flex items-center justify-between gap-3 rounded-md border p-3"
                >
                  <div>
                    <p className="font-medium">{item.name}</p>
                    <p className="text-muted-foreground">Prefijo: {item.keyPrefix}</p>
                    <p className="text-muted-foreground">
                      Generada: {keyDateFormatter.format(new Date(item.createdAt))}
                    </p>
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    variant="destructive"
                    onClick={() => {
                      revokeMutation.reset()
                      setSelectedKey(item)
                    }}
                    disabled={revokeMutation.isPending}
                  >
                    Eliminar
                  </Button>
                </li>
              ))}
            </ul>
          ) : null}
        </CardContent>
      </Card>

      <Dialog
        open={Boolean(selectedKey)}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            closeRevokeDialog()
          }
        }}
      >
        <DialogContent showCloseButton={!revokeMutation.isPending}>
          <DialogHeader>
            <DialogTitle>Eliminar API key</DialogTitle>
            <DialogDescription>
              Esta accion revocara la key{' '}
              <span className="font-medium">{selectedKey?.name}</span>. No se puede deshacer.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button type="button" variant="outline" disabled={revokeMutation.isPending}>
                Cancelar
              </Button>
            </DialogClose>
            <Button
              type="button"
              variant="destructive"
              onClick={onConfirmRevoke}
              disabled={revokeMutation.isPending}
            >
              {revokeMutation.isPending ? 'Eliminando...' : 'Confirmar eliminacion'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </section>
  )
}
