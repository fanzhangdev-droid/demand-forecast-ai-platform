"""
モデルと付随成果物を読み込むモジュール。

STORAGE_MODE 環境変数によって読み込み元を切り替える:

  local（デフォルト）:
    ml-service/models/ ディレクトリから直接読み込む。
    ローカル開発・CI 環境向け。boto3 不要。

  s3:
    起動時に S3 から /tmp/demand-forecast-models/ へダウンロードし、
    そのファイルを joblib で読み込む。
    ステージング・本番環境向け。

いずれのモードでも predictor.py から見たインターフェース（load_model / load_artifacts）
は変わらない。モード切り替えはこのファイル内に閉じている。
"""

from __future__ import annotations

import logging
import os
from pathlib import Path
from typing import Any

import joblib

logger = logging.getLogger(__name__)

# ── パス定義 ───────────────────────────────────────────────────────────────

# local モード: ml-service/models/ を参照する
_LOCAL_MODELS_DIR = Path(__file__).parent.parent / "models"

# s3 モード: ダウンロード先として /tmp を使う。
# コンテナ環境では /tmp が書き込み可能であることが保証されているため適切。
_S3_CACHE_DIR = Path("/tmp/demand-forecast-models")


def _get_storage_mode() -> str:
    """STORAGE_MODE 環境変数を読み取り、'local' または 's3' を返す。
    未設定・不正値の場合は 'local' にフォールバックする。
    """
    mode = os.environ.get("STORAGE_MODE", "local").lower().strip()
    if mode not in ("local", "s3"):
        logger.warning(
            "未知の STORAGE_MODE=%r。'local' にフォールバックします。", mode
        )
        return "local"
    return mode


def _resolve_path(filename: str) -> Path:
    """ストレージモードに応じてモデルファイルのローカルパスを解決する。

    s3 モードの場合は S3 からのダウンロードも行い、
    キャッシュ済みであれば再ダウンロードをスキップする。
    """
    mode = _get_storage_mode()
    logger.info("ストレージモード: %s", mode)

    if mode == "local":
        # ローカルモード: ml-service/models/<filename> をそのまま返す
        return _LOCAL_MODELS_DIR / filename

    # s3 モード: キャッシュディレクトリを確保してからパスを解決する。
    # storage.py 側にも mkdir はあるが、ここで明示的に作ることで
    # キャッシュ確認（cached_path.exists()）より前に確実に存在を保証する。
    _S3_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cached_path = _S3_CACHE_DIR / filename
    if cached_path.exists():
        logger.info("S3 キャッシュを使用: %s", cached_path)
        return cached_path

    # 対応する S3 キー環境変数を読み取る
    # model.pkl → S3_MODEL_KEY, artifacts.pkl → S3_ARTIFACTS_KEY
    key_env = {
        "model.pkl":     "S3_MODEL_KEY",
        "artifacts.pkl": "S3_ARTIFACTS_KEY",
    }
    env_name = key_env.get(filename)
    if not env_name:
        raise ValueError(f"未知のファイル名: {filename}")

    s3_key = os.environ.get(env_name)
    if not s3_key:
        raise EnvironmentError(
            f"環境変数 {env_name} が設定されていません。"
            f"STORAGE_MODE=s3 のときは {env_name} が必要です。"
        )

    # storage.py が S3 ダウンロードの責務を持つ。
    # boto3 の import はそこで行われるため、local モードでは不要。
    from app.storage import download_from_s3
    download_from_s3(s3_key=s3_key, local_path=cached_path)
    return cached_path


def load_model() -> Any:
    """学習済みモデル（model.pkl）を読み込んで返す。"""
    path = _resolve_path("model.pkl")
    logger.info("モデルを読み込み中: %s", path)
    return joblib.load(path)


def load_artifacts() -> dict[str, Any]:
    """学習時の付随成果物（artifacts.pkl）を読み込んで返す。

    artifacts には少なくとも以下のキーが含まれることを期待する:
    - feature_columns : 学習時の列順リスト
    - gender_classes  : gender のラベルリスト
    - category_classes: product_category のラベルリスト
    """
    path = _resolve_path("artifacts.pkl")
    logger.info("成果物を読み込み中: %s", path)
    return joblib.load(path)
