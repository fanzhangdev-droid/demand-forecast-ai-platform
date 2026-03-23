# API 仕様

> プレースホルダー — 実装後は OpenAPI 仕様書または自動生成ドキュメントへのリンクに置き換えてください。

## api-gateway  (ポート 8080)

### `GET /api/v1/health`
サービスのヘルス状態を返します。

### `POST /api/v1/forecast`
新しい需要予測リクエストを送信します。

**リクエストボディ (JSON)**
```json
{
  "product_id": "string",
  "horizon_days": 30,
  "features": {}
}
```

**レスポンス**
```json
{
  "forecast_id": "uuid",
  "status": "pending"
}
```

---

## ml-service  (ポート 8000)

サービス起動中は `http://localhost:8000/docs` でインタラクティブなドキュメントを参照できます。

### `GET /health`
### `POST /predict`
### `GET /models`
