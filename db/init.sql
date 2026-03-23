-- Bootstraps the demand_forecast database on first container start.
-- Actual schema is managed via versioned migrations in db/migrations/.

-- Enable useful extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";
