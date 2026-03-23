# db

Database schema management for the demand forecast platform (PostgreSQL).

## Structure

```
db/
├── init.sql        # Bootstraps the database on first container start
├── migrations/     # Versioned schema migrations (Flyway-compatible naming)
└── seeds/          # Optional seed data for local development
```

## Migrations

Migration files follow the Flyway naming convention:

```
V{version}__{description}.sql
# e.g. V1__create_forecast_table.sql
```

Run migrations locally via the API gateway on startup (Flyway is configured in Spring Boot), or apply them manually:

```bash
psql -h localhost -U app -d demand_forecast -f db/migrations/V1__create_forecast_table.sql
```
