import logging

from fastapi import FastAPI, HTTPException

from app.predictor import predict_quantity
from app.schemas import PredictionRequest, PredictionResponse

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="Demand Forecast ML Service",
    version="0.1.0",
    description="需要予測モデルの推論 API。",
)


@app.get("/health", tags=["ops"], summary="ヘルスチェック")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post(
    "/predict",
    response_model=PredictionResponse,
    tags=["inference"],
    summary="需要数量を予測する",
)
def predict_endpoint(request: PredictionRequest) -> PredictionResponse:
    logger.info(
        "予測リクエスト受信: date=%s gender=%s category=%s",
        request.date,
        request.gender,
        request.product_category,
    )

    try:
        quantity = predict_quantity(request)
    except ValueError as exc:
        # 未知のカテゴリ値など、入力起因のエラー
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:
        # モデル内部エラーなど、予期しない例外
        logger.exception("推論中にエラーが発生しました")
        raise HTTPException(status_code=500, detail="推論中にエラーが発生しました。") from exc

    return PredictionResponse(predicted_quantity=quantity)
