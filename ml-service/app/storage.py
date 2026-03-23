"""
S3 からファイルをダウンロードするユーティリティモジュール。

ML Service の起動時に STORAGE_MODE=s3 が設定されている場合のみ使用される。
ローカル開発では呼び出されないため、boto3 が未インストールでも
STORAGE_MODE=local であれば問題なく動作する。
"""

from __future__ import annotations

import logging
import os
from pathlib import Path

logger = logging.getLogger(__name__)


def download_from_s3(s3_key: str, local_path: Path) -> None:
    """S3 上のオブジェクトを local_path にダウンロードする。

    Args:
        s3_key:     ダウンロード対象の S3 キー（例: demand-forecast/models/model.pkl）
        local_path: 保存先のローカルパス

    Raises:
        RuntimeError: 必要な環境変数が未設定の場合、または S3 ダウンロードに失敗した場合
    """
    # boto3 は S3 モード時のみ必要なため、ここで遅延インポートする。
    # STORAGE_MODE=local では import されないためインストール不要。
    try:
        import boto3
        from botocore.exceptions import BotoCoreError, ClientError
    except ImportError as e:
        raise RuntimeError(
            "boto3 がインストールされていません。"
            "S3 モードを使用するには: pip install boto3"
        ) from e

    bucket = os.environ.get("S3_BUCKET_NAME")
    region = os.environ.get("AWS_REGION", "ap-northeast-1")

    if not bucket:
        raise RuntimeError(
            "環境変数 S3_BUCKET_NAME が設定されていません。"
            ".env または環境変数を確認してください。"
        )

    logger.info("S3 設定: region=%s, bucket=%s", region, bucket)

    # ダウンロード先のディレクトリが存在しない場合は作成する。
    local_path.parent.mkdir(parents=True, exist_ok=True)

    logger.info(
        "S3 からダウンロード中: s3://%s/%s → %s", bucket, s3_key, local_path
    )

    try:
        s3 = boto3.client("s3", region_name=region)
        s3.download_file(Bucket=bucket, Key=s3_key, Filename=str(local_path))
    except (BotoCoreError, ClientError) as e:
        raise RuntimeError(
            f"S3 ダウンロードに失敗しました: s3://{bucket}/{s3_key}\n"
            f"詳細: {e}"
        ) from e

    logger.info("ダウンロード完了: %s", local_path)
