# Architecture

This document describes the system design of the Demand Forecast AI Platform: what each component does, how they communicate, and why the architecture is structured this way.

---

## Table of Contents

- [System Diagram](#system-diagram)
- [Component Roles](#component-roles)
  - [Frontend](#frontend)
  - [API Gateway](#api-gateway-spring-boot)
  - [ML Service](#ml-service-fastapi)
  - [PostgreSQL](#postgresql)
  - [S3](#s3-object-storage)
- [Prediction Request Flow](#prediction-request-flow)
- [Separation of Concerns](#separation-of-concerns)

---

## System Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser                                                        │
└────────────────────────┬────────────────────────────────────────┘
                         │ HTTPS
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  frontend  (React + TypeScript, port 3000)                      │
│                                                                 │
│  • Forecast submission form                                     │
│  • Results dashboard and charts                                 │
│  • Communicates only with api-gateway                           │
└────────────────────────┬────────────────────────────────────────┘
                         │ REST / JSON
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│  api-gateway  (Spring Boot, port 8080)                          │
│                                                                 │
│  • Validates and routes incoming requests                       │
│  • Persists requests and results to PostgreSQL                  │
│  • Calls ml-service for inference                               │
│  • Returns structured responses to the frontend                 │
└──────────────┬──────────────────────────┬───────────────────────┘
               │ REST / JSON              │ JDBC
               ▼                          ▼
┌──────────────────────────┐   ┌──────────────────────────────────┐
│  ml-service              │   │  PostgreSQL  (port 5432)         │
│  (FastAPI, port 8000)    │   │                                  │
│                          │   │  • forecast_requests             │
│  • Loads model artifacts │   │  • forecast_results              │
│  • Runs inference        │   │  • product / SKU metadata        │
│  • Returns predictions   │   └──────────────────────────────────┘
└──────────────┬───────────┘
               │ AWS SDK / HTTP
               ▼
┌──────────────────────────────────────────────────────────────────┐
│  S3  (or S3-compatible object store)                             │
│                                                                  │
│  • Trained model artifacts  (e.g. model_v3.pkl, model.onnx)     │
│  • Raw input datasets                                            │
│  • Processed feature snapshots                                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Component Roles

### Frontend

**Technology:** React 18, TypeScript, Vite, React Query, Recharts

The frontend is a single-page application running in the browser. Its sole responsibility is presenting data and capturing user input. It has no direct knowledge of the database, the ML model, or how predictions are computed.

Key behaviours (planned):
- A form for submitting a forecast request (product ID, horizon, optional overrides)
- A results view that polls for or receives the forecast and renders it as a chart
- Error and loading states communicated clearly to the user

The frontend talks only to the api-gateway. This means the entire backend can change topology without any frontend modification, as long as the REST contract is stable.

---

### API Gateway (Spring Boot)

**Technology:** Java 21, Spring Boot 3, Spring Data JPA, Spring Validation, Flyway

The gateway is the single entry point for all client requests. It owns the public API contract and is responsible for everything that needs to happen before and after the ML inference call.

Responsibilities:
- **Validation** — Rejects malformed requests before they reach downstream services. Bean Validation annotations keep this declarative and auditable.
- **Persistence** — Writes forecast requests and their results to PostgreSQL via JPA. This gives the system a full audit log and allows results to be retrieved later without re-running inference.
- **Orchestration** — Calls the ml-service, handles the response, and assembles the final reply to the client. If the ml-service is slow or unavailable, the gateway is the right place to apply timeouts, retries, or fallback logic.
- **Schema management** — Flyway migrations in `db/migrations/` are applied at startup, keeping the schema versioned alongside the code.

The gateway does not contain ML logic. It treats the ml-service as an internal dependency with a well-defined HTTP interface.

---

### ML Service (FastAPI)

**Technology:** Python 3.12, FastAPI, scikit-learn, pandas, numpy, Uvicorn

The ML service is a stateless inference engine. It receives a prediction request, loads the appropriate model, runs inference against the provided features, and returns a prediction. It does not write to the database and has no knowledge of the frontend.

Responsibilities:
- **Model loading** — Loads serialised model artifacts from the local filesystem or S3 on startup (or lazily on first request, depending on model size).
- **Inference** — Applies feature preprocessing and runs the model to produce a forecast.
- **Model management** — Exposes a `/models` endpoint listing available versions, enabling the gateway to request a specific model by name or version tag.

Keeping inference in Python means the ML team can swap model implementations (ARIMA → LightGBM → a neural network) without any changes outside this service. The FastAPI auto-generated docs at `/docs` also serve as a live contract for the gateway team.

---

### PostgreSQL

**Technology:** PostgreSQL 16

PostgreSQL is the system of record for all forecast activity. The api-gateway is the only service that reads from or writes to it directly.

Planned schema (subject to migration files in `db/migrations/`):

| Table | Purpose |
|---|---|
| `forecast_requests` | One row per submitted forecast request; includes input parameters and status |
| `forecast_results` | Stores the prediction values returned by ml-service, linked to a request |
| `products` | SKU / product metadata referenced by forecast requests |

Storing results in the database means the system can return previous forecasts instantly without re-invoking the model, and provides a foundation for audit, analytics, and retraining pipelines.

---

### S3 (Object Storage)

**Technology:** AWS S3 or an S3-compatible store (e.g. MinIO for local development)

S3 is used for large binary assets that do not belong in a relational database or a git repository.

Contents:
- **Model artifacts** — Serialised model files (`.pkl`, `.joblib`, `.onnx`, etc.) produced by training runs. The ml-service downloads or streams these at startup.
- **Raw data** — Source CSV / Parquet files ingested from upstream systems, kept immutable and versioned by prefix.
- **Processed features** — Feature-engineered datasets produced by the preprocessing pipeline, ready for training or batch inference.

Using S3 rather than a Docker volume means model artifacts and datasets are accessible to all environments (local, staging, production) from a single location, and can be versioned independently of the application code.

---

## Prediction Request Flow

The following describes the end-to-end path of a single synchronous forecast request.

```
Frontend          api-gateway           ml-service          PostgreSQL       S3
   │                   │                     │                   │            │
   │── POST /forecast ▶│                     │                   │            │
   │                   │── INSERT request ──▶│                   │            │
   │                   │                     │                   │            │
   │                   │── POST /predict ───▶│                   │            │
   │                   │                     │── load model ────────────────▶│
   │                   │                     │◀─ model artifact ─────────────│
   │                   │                     │                   │            │
   │                   │                     │  run inference    │            │
   │                   │                     │                   │            │
   │                   │◀─ predictions ──────│                   │            │
   │                   │── INSERT result ───────────────────────▶│            │
   │                   │                     │                   │            │
   │◀─ 200 forecast ───│                     │                   │            │
   │                   │                     │                   │            │
```

**Step-by-step:**

1. The user fills in the forecast form and submits it. The frontend sends a `POST /api/v1/forecast` to the api-gateway with product ID, horizon, and any feature overrides.

2. The gateway validates the request payload. Invalid requests are rejected with a `400` before any downstream calls are made.

3. The gateway persists a `forecast_request` record to PostgreSQL with status `PENDING`.

4. The gateway calls `POST /predict` on the ml-service, passing the relevant features.

5. The ml-service loads the model artifact from S3 (or a warm in-memory cache) and runs inference. It returns a prediction array.

6. The gateway updates the `forecast_request` status to `COMPLETE` and writes a `forecast_result` record to PostgreSQL.

7. The gateway returns the forecast to the frontend in the HTTP response.

8. The frontend renders the result as a time-series chart.

---

## Separation of Concerns

Each service owns a clearly bounded responsibility. The table below maps concerns to their owners.

| Concern | Owner | Rationale |
|---|---|---|
| User interface and data presentation | `frontend` | UI changes should never require backend deployments |
| API contract and input validation | `api-gateway` | A single stable interface for all clients |
| Business data persistence | `api-gateway` → PostgreSQL | Transactional writes belong close to the domain model |
| ML inference logic | `ml-service` | Isolates the Python ML ecosystem from the JVM |
| Model artifact storage | S3 | Binary assets are decoupled from both code and the DB |
| Schema versioning | `db/migrations/` (Flyway) | Schema changes are code-reviewed and applied deterministically |

**Why not put everything in one service?**

A single-service approach is simpler to start but creates coupling that becomes expensive as the project grows:

- If inference and the REST API share a process, a memory spike from a large model load can affect API latency for all other requests.
- Updating the ML model requires redeploying the entire application, including the frontend-facing API.
- Java and Python ML libraries cannot be used in the same runtime without awkward bridging layers (e.g. Jython, GraalPy). The results are slower, harder to maintain, and cut the team off from the Python ML ecosystem.

**The cost of this architecture** is the operational overhead of running multiple services and an internal HTTP call between the gateway and the ML service. For a forecasting workload — where predictions are not latency-critical at the millisecond level — this is an acceptable and standard trade-off used widely in production ML systems.
