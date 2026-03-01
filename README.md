# CanastaUY

![CanastaUY](public/canastauy-thumbnail.webp)

API REST para consulta y análisis histórico de precios de productos al consumidor en Uruguay. El proyecto recopila datos desde 2016 hasta 2025, procesándolos mediante técnicas de detección de outliers para garantizar la calidad de la información.

El objetivo es proporcionar una fuente de datos confiable para análisis de tendencias de precios, comparativas entre productos, y métricas de inflación por categoría. La API permite acceder a más de 774 mil registros de precios diarios mediante endpoints REST documentados con OpenAPI.

**Fuente de datos:** Los datos provienen del [Sistema de Información de Precios al Consumidor del Ministerio de Economía y Finanzas](https://catalogodatos.gub.uy/dataset/defensa-del-consumidor-sistema-de-informacion-de-precios-al-consumidor-2025), publicados en el Catálogo Nacional de Datos Abiertos de Uruguay.

**Datos disponibles:**

- **793,334 registros** de precios diarios
- **379 productos** en **193 categorías**
- **10 años** de datos históricos (2016-2025)
- Dataset limpio con detección de outliers mediante método IQR (Interquartile Range)

![Top Price Changes](outputs/top_price_changes.png)

![Rice Seasonality Analysis](outputs/rice_seasonality_analysis.png)

---

## Stack Tecnológico

| Componente    | Tecnología                  |
| ------------- | --------------------------- |
| Backend       | Spring Boot 4.0.2 + Java 21 |
| Base de datos | PostgreSQL 16               |
| Cache         | Redis 7                     |
| Migraciones   | Flyway                      |
| Autenticación | JWT + API Keys              |
| Documentación | OpenAPI/Swagger             |
| Data Science  | Python 3.12 + Pandas        |

---

## Estructura

```
canasta-uy/
├── backend/              # Spring Boot API
│   ├── src/              # Código fuente
│   ├── justfile          # Comandos de desarrollo
│   └── docker-compose.yml
├── scripts/              # Pipeline de datos Python
├── docs/                 # Documentación completa
└── bruno/                # Colección de API tests
```

---

## Preparación

### Requisitos

- Java 21
- Maven 3.9+
- Docker + Docker Compose
- Python 3.12+ (para scripts de datos)
- Just (runner de comandos)

### 1. Clonar Repositorio

```bash
git clone https://github.com/GabrielDLeon/canasta-uy.git canasta-uy
cd canasta-uy/backend
cp .env.example .env
```

### 2. Levantar infraestructura

```bash
cd backend
just infra-up
```

Se levantarán los siguientes servicios:

- PostgreSQL: http://localhost:5432
- Redis: http://localhost:6379
- pgAdmin: http://localhost:5050

### 3. Importar datos

Luego de levantar la infraestructura por primera vez, debe cargar los datos provistos para la base de datos ejecutando el siguiente comando:

```bash
just import-data
```

---

## Comandos Just

Todos los comandos se ejecutan desde el directorio `backend/`:

| Comando            | Descripción                                    |
| ------------------ | ---------------------------------------------- |
| `just infra-up`    | Levantar PostgreSQL, Redis y pgAdmin           |
| `just infra-down`  | Detener infraestructura                        |
| `just infra-logs`  | Ver logs de servicios                          |
| `just dev`         | Ejecutar Spring Boot en modo dev               |
| `just build`       | Compilar proyecto                              |
| `just test`        | Ejecutar tests (levanta infra automáticamente) |
| `just import-data` | Importar datos CSV a PostgreSQL                |
| `just setup`       | Crear archivo .env desde template              |
| `just clean`       | Limpiar contenedores, volúmenes y build        |
| `just cache-clear` | Limpiar cache de Redis                         |

---

## Desarrollo

Para iniciar el servidor de desarrollo ejecute el siguiente comando:

```bash
just dev
```

### Perfiles de Spring Boot

- **dev** (por defecto): Configuración local, logs verbose, CORS abierto
- **prod**: Configuración de producción (requiere variables adicionales)

### Acceso a Swagger UI

<http://localhost:8080/api/docs>

---

## Endpoints

Todos los endpoints de datos requieren autenticación con API Key.
Ver documentación completa en Swagger/OpenAPI.

---

## Scripts de Datos (Python)

Ubicados en `scripts/`. Requieren `uv` instalado.

```bash
# Instalar dependencias
uv sync

# Ejecutar script
uv run python scripts/processing/detect_outliers_v4_improved.py
```

**Scripts principales:**

| Script                                               | Descripción               |
| ---------------------------------------------------- | ------------------------- |
| `processing/detect_outliers_v4_improved.py`          | Crear dataset V4 (IQR)    |
| `visualization/visualize_rice_price_evolution_v4.py` | Generar gráficos          |
| `product_descriptive_statistics_v4.py`               | Estadísticas descriptivas |

---

## Documentación

- `docs/roadmap-backend.md` - Roadmap de desarrollo
- `docs/00-data-preparation/` - Pipeline de limpieza de datos
- `docs/01-backend-infrastructure/` - Infraestructura y DB
- `docs/02-backend-domain-persistence/` - Dominio y JPA
- `docs/03-backend-auth-security/` - Autenticación
- `docs/04-backend-business-logic/` - Lógica de negocio

---

## Licencia

Los datos provienen del Catálogo Nacional de Datos Abiertos de Uruguay. Su uso está sujeto a la licencia y condiciones publicadas por el proveedor. Este repositorio no redistribuye la fuente original, sino una versión procesada para análisis. Para uso público o comercial, revisar la licencia oficial del dataset y realizar la atribución correspondiente.
