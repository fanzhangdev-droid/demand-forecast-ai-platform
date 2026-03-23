# API Specification

> Placeholder — replace with OpenAPI spec or link to generated docs.

## api-gateway  (port 8080)

### `GET /api/v1/health`
Returns service health status.

### `POST /api/v1/forecast`
Submit a new demand forecast request.

**Request body (JSON)**
```json
{
  "product_id": "string",
  "horizon_days": 30,
  "features": {}
}
```

**Response**
```json
{
  "forecast_id": "uuid",
  "status": "pending"
}
```

---

## ml-service  (port 8000)

Interactive docs available at `http://localhost:8000/docs` when the service is running.

### `GET /health`
### `POST /predict`
### `GET /models`
