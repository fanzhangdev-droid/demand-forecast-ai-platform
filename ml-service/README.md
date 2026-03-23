# ml-service

FastAPI service that exposes demand forecasting model inference via a REST API.

## Tech Stack

- Python 3.12
- FastAPI
- Uvicorn
- Pydantic v2

## Local Development

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 8000
# Docs at http://localhost:8000/docs
```

## Key Endpoints (placeholder)

| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Health check |
| `POST` | `/predict` | Run model inference |
| `GET` | `/models` | List available models |

## Running Tests

```bash
pytest
```
