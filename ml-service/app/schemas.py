from datetime import date as DateType

from pydantic import BaseModel, Field


class PredictionRequest(BaseModel):
    """POST /predict のリクエストスキーマ。"""

    date: DateType = Field(..., description="予測対象の日付（YYYY-MM-DD 形式）。")
    gender: str = Field(..., description="顧客の性別（例: Male / Female）。")
    age: int = Field(..., ge=0, le=120, description="顧客の年齢。")
    product_category: str = Field(..., description="商品カテゴリ（例: Beauty / Electronics）。")
    price_per_unit: float = Field(..., gt=0, description="単価。")

    model_config = {
        "json_schema_extra": {
            "examples": [
                {
                    "date": "2023-11-24",
                    "gender": "Male",
                    "age": 34,
                    "product_category": "Beauty",
                    "price_per_unit": 50,
                }
            ]
        }
    }


class PredictionResponse(BaseModel):
    """POST /predict のレスポンススキーマ。"""

    predicted_quantity: float = Field(..., description="モデルが予測した需要数量。")