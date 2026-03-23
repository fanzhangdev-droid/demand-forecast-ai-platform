"""
推論ロジックを担当するモジュール。

リクエストを学習時と同じ特徴量 DataFrame に変換し、モデルで予測値を返す。
モデルと成果物はモジュール読み込み時に一度だけロードする。
"""

from __future__ import annotations

import logging
from typing import Any

import pandas as pd

from app.model_loader import load_artifacts, load_model
from app.schemas import PredictionRequest

logger = logging.getLogger(__name__)

# 起動時に一度だけ読み込む（リクエストごとのディスク I/O を避けるため）
_model: Any = load_model()
_artifacts: dict[str, Any] = load_artifacts()


def _build_feature_dataframe(request: PredictionRequest) -> pd.DataFrame:
    """リクエストを学習時と同じ特徴量 DataFrame に変換する。

    変換ルール（訓練スクリプトと同一）:
    - date            -> year, month, day, day_of_week（月曜=0, 日曜=6）
    - gender          -> gender_encoded（artifacts["gender_classes"] のインデックス）
    - product_category-> category_encoded（artifacts["category_classes"] のインデックス）
    - age             -> Age
    - price_per_unit  -> Price per Unit

    最後に artifacts["feature_columns"] の順序で列を並び替える。

    Raises:
        ValueError: gender または product_category が未知のカテゴリの場合。
    """
    gender_classes: list[str] = _artifacts["gender_classes"]
    category_classes: list[str] = _artifacts["category_classes"]
    feature_columns: list[str] = _artifacts["feature_columns"]

    # gender のラベルエンコード
    if request.gender not in gender_classes:
        raise ValueError(
            f"不明な gender: '{request.gender}'。"
            f"有効な値: {gender_classes}"
        )
    gender_encoded = gender_classes.index(request.gender)

    # product_category のラベルエンコード
    if request.product_category not in category_classes:
        raise ValueError(
            f"不明な product_category: '{request.product_category}'。"
            f"有効な値: {category_classes}"
        )
    category_encoded = category_classes.index(request.product_category)

    # date を year / month / day / day_of_week に分解
    d = request.date
    row = {
        "year": d.year,
        "month": d.month,
        "day": d.day,
        "day_of_week": d.weekday(),
        "gender_encoded": gender_encoded,
        "category_encoded": category_encoded,
        "Age": request.age,
        "Price per Unit": request.price_per_unit,
    }

    df = pd.DataFrame([row])

    # 訓練時と同じ列順に揃える
    df = df[feature_columns]

    return df


def predict_quantity(request: PredictionRequest) -> float:
    """単一リクエストから需要数量の予測値を返す。

    Args:
        request: バリデーション済みの予測リクエスト。

    Returns:
        モデルが予測した需要数量（float）。

    Raises:
        ValueError: gender または product_category が未知のカテゴリの場合。
    """
    df = _build_feature_dataframe(request)
    prediction = _model.predict(df)
    result = float(prediction[0])

    logger.info(
        "推論完了: date=%s gender=%s category=%s -> %.4f",
        request.date,
        request.gender,
        request.product_category,
        result,
    )

    return result
