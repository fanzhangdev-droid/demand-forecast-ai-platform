from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_predict_returns_forecast():
    payload = {
        "product_id": "SKU-001",
        "horizon_days": 7,
        "features": {},
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200

    body = response.json()
    assert body["product_id"] == "SKU-001"
    assert body["horizon_days"] == 7
    assert len(body["forecast"]) == 7
    assert all(isinstance(v, float) for v in body["forecast"])


def test_predict_validates_horizon_bounds():
    payload = {"product_id": "SKU-001", "horizon_days": 0}
    response = client.post("/predict", json=payload)
    assert response.status_code == 422  # Pydantic validation error
