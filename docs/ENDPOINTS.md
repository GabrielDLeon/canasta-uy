# Endpoints

API endpoints for CanastaUY.

---

## Authentication

All data endpoints require API Key authentication.
Header: `Authorization: Bearer {api_key}`

---

## Products

### List Products
`GET /api/v1/products`

Returns paginated list of all products.

**Query Parameters**:
- `page` (optional): Page number (0-based), default 0
- `size` (optional): Page size, default 20

**Response**: Paginated list of products

---

### Get Product
`GET /api/v1/products/{id}`

Returns details for a specific product.

**Response**: Product details with category info

---

### Search Products
`GET /api/v1/products/search?q={query}`

Search products by name.

**Query Parameters**:
- `q` (required): Search query string
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 20

**Response**: Paginated list of matching products

---

### Get Product Prices
`GET /api/v1/products/{id}/prices`

Returns price history for a specific product.

**Query Parameters**:
- `from` (optional): Start date (YYYY-MM-DD), default 365 days ago
- `to` (optional): End date (YYYY-MM-DD), default today
- `granularity` (optional): `daily` (default) or `monthly`

**Constraints**:
- Maximum date range: 365 days
- Returns 400 if range exceeds 365 days

**Response**: Price history with metadata

---

## Prices

### Search Prices
`GET /api/v1/prices`

Search prices across products with filters.

**Query Parameters**:
- `product_ids` (optional): Comma-separated product IDs (e.g., "1,2,3")
- `category_id` (optional): Filter by category
- `from` (optional): Start date, default 365 days ago
- `to` (optional): End date, default today
- `granularity` (optional): `daily` (default) or `monthly`
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 20, max 100

**Constraints**:
- Maximum date range: 365 days

**Response**: Paginated list of price records

---

## Categories

### List Categories
`GET /api/v1/categories`

Returns paginated list of all categories.

**Query Parameters**:
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 20

**Response**: Paginated list of categories

---

### Get Category
`GET /api/v1/categories/{id}`

Returns details for a specific category.

**Response**: Category details

---

### Get Category Products
`GET /api/v1/categories/{id}/products`

Returns products belonging to a category.

**Query Parameters**:
- `page` (optional): Page number, default 0
- `size` (optional): Page size, default 20, max 100

**Response**: Paginated list of products

---

### Get Category Stats
`GET /api/v1/categories/{id}/stats`

Returns aggregated price statistics for a category.

**Query Parameters**:
- `from` (optional): Start date, default 365 days ago
- `to` (optional): End date, default today

**Constraints**:
- Maximum date range: 365 days

**Response**: Aggregated statistics (avg, min, max, variation)

---

## Analytics

### Price Trend
`GET /api/v1/analytics/trend/{productId}`

Analyzes price trend for a product.

**Query Parameters**:
- `from` (optional): Start date, default 365 days ago
- `to` (optional): End date, default today
- `include_data` (optional): Include raw price series (`true`/`false`), default `false`

**Constraints**:
- Maximum date range: 365 days

**Response**:
```json
{
  "summary": {
    "trend": "increasing",
    "variation_percentage": 23.5,
    "price_start": 45.20,
    "price_end": 55.83,
    "volatility": "medium"
  },
  "data": [...]  // Only if include_data=true
}
```

---

### Category Inflation
`GET /api/v1/analytics/inflation/{categoryId}`

Calculates inflation rate for a category.

**Query Parameters**:
- `from` (optional): Start date, default 365 days ago
- `to` (optional): End date, default today
- `include_data` (optional): Include monthly breakdown, default `false`

**Constraints**:
- Maximum date range: 365 days

**Response**: Inflation statistics with optional monthly data

---

### Compare Products
`GET /api/v1/analytics/compare`

Compares prices across multiple products.

**Query Parameters**:
- `product_ids` (required): Comma-separated product IDs (2-5 products)
- `from` (optional): Start date, default 365 days ago
- `to` (optional): End date, default today

**Constraints**:
- Maximum 5 products
- Maximum date range: 365 days

**Response**: Side-by-side comparison with price differences

---

### Top Price Changes
`GET /api/v1/analytics/top-changes`

Returns products with highest price variations.

**Query Parameters**:
- `period` (optional): `7d`, `30d` (default), `90d`, `1y`
- `type` (optional): `increase`, `decrease`, `all` (default)
- `limit` (optional): Number of results, default 10, max 50
- `category_id` (optional): Filter by category

**Response**: List of products with price change details

---

## Common Response Format

### Pagination Info
Included in paginated responses:
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "total_elements": 379,
  "total_pages": 19,
  "has_next": true,
  "has_previous": false
}
```

### Date Range
```json
{
  "from": "2024-01-01",
  "to": "2024-12-31"
}
```

---

## Error Responses

All errors follow consistent format:
```json
{
  "message": "Error description",
  "timestamp": "2024-01-15T10:30:00",
  "path": "/api/v1/products/999/prices"
}
```

**Common Status Codes**:
- `400 Bad Request`: Invalid parameters, date range >365 days
- `401 Unauthorized`: Missing or invalid API key
- `404 Not Found`: Resource not found
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error

---

**Last Updated**: 2026-02-25
