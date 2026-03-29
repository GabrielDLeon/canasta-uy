# CanastaUY Frontend

Frontend web de CanastaUY construido con React + Vite + TypeScript.

Permite:
- autenticacion de usuario,
- exploracion de productos y categorias,
- visualizacion de analytics (tendencia, inflacion, comparativas, top cambios),
- gestion de API keys de cuenta.

## Stack

- React 19
- TypeScript
- Vite 7
- TanStack Query
- React Router
- Tailwind CSS

## Requisitos

- Node.js 20+
- pnpm

## Desarrollo local

Desde `frontend/`:

```bash
pnpm install
pnpm dev
```

App disponible en <http://localhost:5173>.

Por defecto, Vite proxea `/api` y `/v3/api-docs` a `http://localhost:8080`.

## Variables de entorno

- `VITE_API_BASE_URL` (default: `/api/v1`)
- `VITE_PROXY_TARGET` (default: `http://localhost:8080`)

Ejemplo:

```bash
VITE_PROXY_TARGET=http://localhost:8080 pnpm dev
```

## Docker (stack completo)

El flujo recomendado es usar el `docker-compose.yml` en la raiz del repositorio para levantar frontend + backend + PostgreSQL + Redis + pgAdmin.

Desde la raiz:

```bash
docker compose up --build
```

## Scripts

- `pnpm dev`: servidor de desarrollo
- `pnpm build`: build de produccion
- `pnpm preview`: previsualizar build
- `pnpm lint`: lint
