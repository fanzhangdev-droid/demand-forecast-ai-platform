# Demand Forecast AI Platform

A full-stack AI system that predicts product demand based on date, customer attributes, and pricing inputs.
Built as a microservice architecture connecting a React frontend, a Spring Boot API gateway, a FastAPI ML inference service, and PostgreSQL.

> **日本語版:** [README.md](./README.md)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [Tech Stack](#3-tech-stack)
4. [Features](#4-features)
5. [API Reference](#5-api-reference)
6. [Data Flow](#6-data-flow)
7. [Design Decisions](#7-design-decisions)
8. [V2: S3 Artifact Management](#8-v2-s3-artifact-management)
9. [Future Work (V3+)](#9-future-work-v3)
10. [AWS Deployment (Minimal Plan)](#10-aws-deployment-minimal-plan)
11. [Getting Started](#11-getting-started)
12. [Verification](#12-verification)
13. [Troubleshooting](#13-troubleshooting)
14. [Screenshots](#14-screenshots)

---

## 1. Project Overview

This platform addresses demand forecasting in the retail/e-commerce domain. A user submits customer and product attributes through a web UI; the backend routes the request to a trained ML model, returns the predicted quantity, and persists the result for historical analysis.

The project was built to demonstrate end-to-end engineering across:

- **Java backend**: REST API design, JPA persistence, service orchestration in Spring Boot
- **Python ML service**: FastAPI inference endpoint, feature engineering, scikit-learn model integration
- **API design**: Structured request/response contracts, input validation, consistent error handling
- **React frontend**: Async state management, controlled form inputs, layered feedback UX
- **Docker**: Multi-service containerization with Docker Compose

---

## 2. Architecture

```
Browser
  │
  ▼
┌──────────────────────┐
│  Frontend            │  React (Vite)
│  localhost:3000      │  Form input / Result display / History view
└──────────┬───────────┘
           │ REST / JSON
           ▼
┌──────────────────────┐
│  API Gateway         │  Spring Boot (Java)
│  localhost:8080      │  Request handling / ML delegation / DB persistence
└──────┬───────┬───────┘
       │       │
       │ REST  │ JPA
       ▼       ▼
┌──────────┐  ┌──────────────┐
│ML Service│  │  PostgreSQL  │
│:8000     │  │  :5432       │
│FastAPI   │  │  Forecast    │
│          │  │  History     │
└──────────┘  └──────────────┘
                    │
              (V2: S3 integration planned)
```

### Layer Responsibilities

| Service | Responsibilities |
|---|---|
| Frontend | User input, result display, history browsing, search filtering |
| API Gateway | Request validation, ML service invocation, history persistence and querying |
| ML Service | Feature engineering, model inference |
| PostgreSQL | Durable storage of prediction history |

---

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18 / Vite / JavaScript |
| API Gateway | Java 21 / Spring Boot 3 / Spring Data JPA |
| ML Service | Python 3.12 / FastAPI / scikit-learn / pandas |
| Database | PostgreSQL 16 |
| Containers | Docker / Docker Compose |

---

## 4. Features

### Demand Prediction
- Submit date, gender, age, product category, and unit price to run inference
- Product category is a controlled dropdown (Beauty / Clothing / Electronics)
- Predicted quantity is displayed as a rounded integer

### Prediction History
- Every prediction is automatically persisted to PostgreSQL
- History is displayed in reverse-chronological order (10 records per page, with Previous / Next navigation)

### History Search
- Filter history by product category and/or date
- All parameters are optional — omitting them returns the full history
- Changing search criteria resets to page 1

### Input Validation
- Frontend validates required fields and value ranges before sending requests (age: 1–120, pricePerUnit: > 0)
- Invalid input is rejected immediately without calling the API
- Backend enforces the same rules via Bean Validation as a second line of defence

### UI Feedback
- Loading indicator during API calls
- Distinct messages for success, input errors, server errors, and network failures

---

## 5. API Reference

### POST /api/forecast

Run a forecast and persist the result.

**Request Body**
```json
{
  "date": "2026-03-22",
  "gender": "Female",
  "age": 28,
  "product_category": "Beauty",
  "price_per_unit": 45.0
}
```

**Response**
```json
{
  "success": true,
  "data": {
    "predictedQuantity": 3.74
  }
}
```

---

### GET /api/forecast/history

Retrieve paginated prediction history. Filter parameters are optional.

**Query Parameters**

| Parameter | Type | Default | Description |
|---|---|---|---|
| productCategory | String | — | Optional filter by category |
| date | ISO date | — | Optional filter by date |
| page | int | `0` | 0-based page number |
| size | int | `10` | Records per page (max 50) |

**Response**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "date": "2026-03-22",
        "gender": "Female",
        "age": 28,
        "productCategory": "Beauty",
        "pricePerUnit": 45.0,
        "predictedQuantity": 3.74,
        "createdAt": "2026-03-22T14:30:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 23,
    "totalPages": 3,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

---

## 6. Data Flow

End-to-end flow for a single forecast request:

```
1. User fills in the form and clicks "Predict"

2. Frontend → POST /api/forecast
   - Converts camelCase form fields to snake_case JSON (forecastApi.js)

3. API Gateway (Spring Boot)
   - Deserializes ForecastRequest via Jackson (@JsonProperty bindings)
   - Calls ML Service POST /predict via RestTemplate

4. ML Service (FastAPI)
   - Applies feature engineering: date decomposition, category label encoding
   - Runs inference with the scikit-learn model
   - Returns predicted_quantity

5. API Gateway
   - Persists input + result to prediction_history table (JPA)
   - Returns { success: true, data: { predictedQuantity } }

6. Frontend
   - Displays Math.round(predictedQuantity) as an integer
   - Refreshes history list via GET /api/forecast/history
```

---

## 7. Design Decisions

### Separating the API Gateway and ML Service
The Java and Python services were kept separate rather than merged into a single backend. Spring Boot is well-suited for request validation, relational persistence, and transaction management. Python owns the ML ecosystem — scikit-learn, pandas, and the broader data science toolchain. Keeping them separate means the ML model can be retrained and redeployed without touching the API contract or requiring any Java changes.

### Controlled category input (dropdown vs. free text)
Free-text input for product category would allow arbitrary strings to reach the ML service, which would fail with a 400 error for any category not seen during training. Fixing the choices at the frontend eliminates the error at its source — the cheapest validation point in the stack. This is a deliberate trade-off: restrict input choices rather than build fallback logic in the backend or ML service.

### Displaying predicted quantity as an integer
The regression model returns a float (e.g., `3.74`), but demand quantity is inherently a count. The decimal component carries no business meaning in this context. The rounding is applied only in the display layer (`Math.round()` in JSX) — the raw float is preserved in the API response and database, keeping options open for future use cases that may care about precision.

### Adding search to the history view
As history accumulates, returning all records on every page load becomes impractical. The two most natural filter axes in this domain are product category and date, so those were implemented. The filtering is done via Spring Data JPA derived query methods with if-else branching in the service layer — no Specification or QueryDSL, deliberately minimal to keep the code readable.

### Adding pagination
As prediction records accumulate, returning all rows in a single response creates latency and UI scaling problems. Offset-based pagination was added using Spring Data JPA `Pageable`.

Key decisions: Spring's `Page<T>` is not returned directly — it is mapped to a custom `HistoryPageResponse` DTO to avoid leaking framework internals to the API contract. The frontend uses a minimal Previous / Next UI; numbered pages and arbitrary jumps are unnecessary for this use case. When a filter changes, the page resets to 0 to avoid landing on an empty page. Specification and QueryDSL were not introduced — the current two filter axes don't justify the added complexity.

### Adding input validation
The forecast API accepts external input that is passed directly to the ML service, so invalid values need to be caught before they cause downstream errors. Validation is applied at both layers: the frontend rejects bad input immediately for UX, and the backend enforces the same constraints via `@Valid` and Bean Validation annotations for API robustness. A single `@RestControllerAdvice` handler converts validation failures into readable 400 responses.

### Keeping V1 minimal
The goal for V1 was to have every layer working end-to-end: frontend form, API routing, ML inference, and DB persistence. Adding authentication, analytics, or S3 before that foundation was proven would increase complexity without verifying the core loop. Those concerns are scoped to V2.

---

## 8. V2: S3 Artifact Management

### What V1 got wrong at scale

V1 loaded `model.pkl` and `artifacts.pkl` directly from the local filesystem — fine for development, but problematic for anything beyond it:

- **Artifact volatility**: Rebuilding the container wipes the model files
- **No environment handoff**: There's no clear path to share trained artifacts from the training environment to the inference environment
- **Poor reproducibility**: No record of which model version is actually running

V2 addresses these by moving model artifact management to S3.

---

### Separating data by responsibility

The core design decision in V2 is not *how* to use S3 — it's *why* different kinds of data belong in different places. S3 is not a replacement for PostgreSQL. They serve fundamentally different roles:

| Data type | Storage | Reason |
|---|---|---|
| Prediction requests and history (transactional) | PostgreSQL | Structured, queryable, requires consistency |
| Model artifacts (`model.pkl`, etc.) | S3 | Binary blobs passed between training and inference environments |
| Datasets (raw / processed) | S3 | High-volume, unstructured — no reason to put them in a relational DB |

This separation keeps each storage layer focused on what it does well.

---

### S3 Storage Layout

```
s3://<bucket>/demand-forecast/
├── raw/                         ← Raw datasets (CSV, etc.)
├── processed/                   ← Preprocessed datasets
└── models/
    ├── model.pkl                ← Trained model
    └── artifacts.pkl            ← Feature engineering metadata
```

The `raw/` → `processed/` → `models/` path structure mirrors the train → store → distribute lifecycle. It also leaves room for versioning (`models/v1/`, `models/v2/`) without a structural change.

---

### STORAGE_MODE: balancing developer experience and cloud readiness

`STORAGE_MODE` controls where the ML service loads models from.

| Mode | Behavior | Optimizes for |
|---|---|---|
| `local` (default) | Load from `ml-service/models/` | Developer speed, zero-friction setup |
| `s3` | Download on startup → cache in `/tmp/` | Cross-environment reproducibility |

Without setting `STORAGE_MODE`, behaviour is identical to V1. The goal was to add cloud-aware artifact management without degrading the local development experience.

---

### Implementation decisions

**Why loading logic is centralized in `model_loader.py`**
`predictor.py` calls `load_model()` and `load_artifacts()` without knowing or caring where the files come from. The storage mode is an implementation detail, not part of the public interface. This means V3 changes — such as selecting a model by version at runtime — can be made inside `model_loader.py` without touching anything upstream.

**Why boto3 is a lazy import**
`STORAGE_MODE=local` is the default and covers all local development. Forcing boto3 as a hard dependency would add installation friction for a code path that most development sessions never hit. The import is deferred to `storage.py` at call time — if you never run in `s3` mode, you never need boto3 installed.

**Why startup download with `/tmp` caching**
Fetching from S3 on every inference request would introduce latency and cost. Caching in `/tmp` on startup keeps inference fast while avoiding a dependency on a persistent volume. It's the minimal optimization that solves the problem without overcomplicating the container setup.

---

### Lightweight artifact pipeline

V2 establishes a simple, explicit pipeline from training to deployment:

```
train.py
  ↓ generates model.pkl / artifacts.pkl
upload_artifacts_to_s3.py
  ↓ uploads to S3
s3://<bucket>/demand-forecast/models/
  ↓ fetched on ML service startup (s3 mode)
inference service
```

```bash
# Upload artifacts (from ml-service/ directory)
export S3_BUCKET_NAME=your-bucket-name
export AWS_REGION=ap-northeast-1
python scripts/upload_artifacts_to_s3.py

# Start ML service in s3 mode
export STORAGE_MODE=s3
uvicorn app.main:app --port 8000
```

---

### Why V2 stops here

Airflow, Step Functions, MLflow, and automated CI/CD pipelines are all valid extensions of this architecture. They were deliberately left out of scope.

The purpose of this project is to demonstrate system design and engineering judgment — not to catalogue every available tool. V2 delivers the minimum viable artifact management pipeline: trained models are stored durably, shared between environments, and loaded without manual file copying. Knowing where to stop is part of the design process. An unnecessarily complex solution is not a better solution.

---

## 9. Future Work (V3+)

| Area | Description |
|---|---|
| Authentication | API key or JWT-based auth via Spring Security |
| Model Versioning | Multi-version model registry with runtime selection |

---

## 10. AWS Deployment (Minimal Plan)

A minimal AWS deployment plan is documented in [`docs/deployment-aws.md`](./docs/deployment-aws.md).

**Target configuration:** single EC2 instance running all services via Docker Compose, with S3 for model artifact storage.

```
[User]
  ↓
[EC2 (single instance)]
├── Frontend  (React + Vite → nginx)
├── API Gateway (Spring Boot :8080)
├── ML Service  (FastAPI :8000)
└── PostgreSQL  (:5432)
  ↓
[S3]
└── demand-forecast/models/  ← model.pkl / artifacts.pkl
```

**Why this configuration:** The existing Docker Compose setup runs on EC2 with minimal changes. Complexity is kept low intentionally — this is a PoC project. See the document for trade-off analysis and a natural upgrade path toward RDS, ECS/Fargate, and ALB.

---

## 11. Getting Started

### Quick Start (Docker Compose)

**Prerequisite:** Docker Desktop must be running.

```bash
git clone <repo-url>
cd demand-forecast-ai-platform
docker compose up --build
```

Once all services are up:

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| ML Service (Swagger UI) | http://localhost:8000/docs |

To stop:

```bash
docker compose down          # stop containers
docker compose down -v       # stop and remove volumes (clears DB data)
```

---

### Manual Setup (local development)

**Prerequisites:** Node.js 18+, Java 21+, Python 3.12+, PostgreSQL 16

```bash
# Create the demand_forecast database in PostgreSQL first

# 1. ML Service
cd ml-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000

# 2. API Gateway
cd api-gateway
./mvnw spring-boot:run

# 3. Frontend
cd frontend
npm install
npm run dev       # http://localhost:5173
```

---

## 12. Verification

After `docker compose up --build`, follow these steps to confirm the system is working end-to-end.

### Run a prediction

1. Open http://localhost:3000
2. Fill in the form and click **Predict**:
   - Date: any date (e.g. `2026-03-22`)
   - Gender: `Female`
   - Age: `28`
   - Product Category: `Beauty`
   - Price per Unit: `45`
3. A predicted quantity (integer) should appear below the form.

### Check the history list

- After submitting, the prediction should appear in the history table at the bottom of the page.

### Test search filters

- Select `Beauty` from the Category dropdown → click **Search** → only Beauty records appear
- Enter a date in the Date field → click **Search** → only records for that date appear
- Click **Reset** → all records are shown again

### Test pagination

- With more than 10 records, click **Next** → the next page loads
- On the first page, **Previous** is disabled
- Change the search filter and click **Search** → page resets to 1

### Verify persistence across restarts

```bash
docker compose down
docker compose up --build
```

History data should still be present after restart, confirming PostgreSQL persistence is working.

---

## 13. Troubleshooting

Only issues that were actually encountered during development are listed here.

---

### Docker daemon is not running

**Symptom:**
```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock.
Is the docker daemon running?
```

**Fix:** Start Docker Desktop and retry.

---

### `version` field warning in docker-compose.yml

**Symptom:**
```
WARN[0000] .../docker-compose.yml: `version` is obsolete
```

**Explanation:** Docker Compose V2 no longer requires the `version` field. This is a non-fatal warning. Remove the `version: "3.9"` line from `docker-compose.yml` to suppress it.

---

### ML Service exits immediately on startup

**Symptom:**
```
FileNotFoundError: [Errno 2] No such file or directory: '/app/models/model.pkl'
```

**Cause:** The `ml-service` Docker image was built without the `models/` directory.

**Fix:** Ensure `ml-service/Dockerfile` includes the following line:

```dockerfile
COPY models/ ./models/
```

If missing, add it and rebuild with `docker compose up --build`.

---

## 14. Screenshots

### Forecast Form (empty)

![Forecast Form](docs/screenshots/forecast-form.png)

### Prediction Result

![Prediction Result](docs/screenshots/forecast-result.png)

### History List

![History List](docs/screenshots/history-list.png)

### History Search (category filter applied)

![History Search](docs/screenshots/history-search.png)
