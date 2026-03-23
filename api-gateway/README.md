# api-gateway

Spring Boot REST API gateway. Sits between the frontend and the ML service, handles request routing, validation, and persistence.

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Data JPA
- PostgreSQL

## Local Development

```bash
./mvnw spring-boot:run
# API available at http://localhost:8080
```

Requires a running PostgreSQL instance. Use `docker-compose up db` to start only the database.

## Key Endpoints (placeholder)

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/health` | Health check |
| `POST` | `/api/v1/forecast` | Submit a forecast request |
| `GET` | `/api/v1/forecast/{id}` | Retrieve forecast result |
