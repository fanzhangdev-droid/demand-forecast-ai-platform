"""
学習済み成果物（model.pkl / artifacts.pkl）を S3 にアップロードするスクリプト。

使い方:
    # 環境変数を設定してから実行
    export S3_BUCKET_NAME=your-bucket-name
    export AWS_REGION=ap-northeast-1                        # 任意、デフォルト: ap-northeast-1
    export S3_MODEL_KEY=demand-forecast/models/model.pkl    # 任意、デフォルト値あり
    export S3_ARTIFACTS_KEY=demand-forecast/models/artifacts.pkl

    python scripts/upload_artifacts_to_s3.py   # ml-service/ 内から実行
    # または
    python ml-service/scripts/upload_artifacts_to_s3.py   # プロジェクトルートから実行

S3 パス設計:
    s3://<bucket>/demand-forecast/raw/          ← 原データ（将来用）
    s3://<bucket>/demand-forecast/processed/    ← 前処理済みデータ（将来用）
    s3://<bucket>/demand-forecast/models/       ← 学習済みモデル成果物（このスクリプトが対象）

注意:
    - このスクリプトはトランザクションデータ（PostgreSQL）は扱わない。
    - S3 はモデル成果物・データセットの管理専用。
    - AWS 認証情報は boto3 の標準チェーン（~/.aws/credentials, IAM Role 等）に従う。
"""

import os
import sys
from pathlib import Path

# スクリプトの位置（__file__）を基準に models/ ディレクトリを解決する。
# 実行ディレクトリに依存しないため、どこから起動しても同じパスが得られる。
_SCRIPT_DIR = Path(__file__).parent
_ML_SERVICE_DIR = _SCRIPT_DIR.parent
_MODELS_DIR = _ML_SERVICE_DIR / "models"

# S3 パスのデフォルト値（プロジェクトの設計方針に合わせた固定プレフィックス）
_DEFAULT_MODEL_KEY = "demand-forecast/models/model.pkl"
_DEFAULT_ARTIFACTS_KEY = "demand-forecast/models/artifacts.pkl"


def _require_env(name: str) -> str:
    """環境変数を読み取り、未設定の場合は終了する。"""
    value = os.environ.get(name)
    if not value:
        print(f"[ERROR] 環境変数 {name} が設定されていません。", file=sys.stderr)
        sys.exit(1)
    return value


def upload_file(s3_client, local_path: Path, bucket: str, s3_key: str) -> None:
    """単一ファイルを S3 にアップロードする。"""
    if not local_path.is_file():
        print(
            f"[ERROR] ファイルが見つかりません: {local_path}\n"
            "       先に学習スクリプトを実行してください。",
            file=sys.stderr,
        )
        sys.exit(1)

    file_size_kb = local_path.stat().st_size // 1024
    print(f"[INFO]  アップロード中: {local_path} ({file_size_kb} KB)")
    print(f"        → s3://{bucket}/{s3_key}")

    s3_client.upload_file(
        Filename=str(local_path),
        Bucket=bucket,
        Key=s3_key,
    )

    print(f"[OK]    完了: s3://{bucket}/{s3_key}")


def main() -> None:
    # boto3 のインポートは実行時のみ。
    # インストールされていない場合は分かりやすいエラーを出す。
    try:
        import boto3
        from botocore.exceptions import BotoCoreError, ClientError
    except ImportError:
        print(
            "[ERROR] boto3 がインストールされていません。\n"
            "        pip install boto3 を実行してください。",
            file=sys.stderr,
        )
        sys.exit(1)

    # 必須環境変数の読み取り
    bucket = _require_env("S3_BUCKET_NAME")
    region = os.environ.get("AWS_REGION", "ap-northeast-1")
    model_key = os.environ.get("S3_MODEL_KEY", _DEFAULT_MODEL_KEY)
    artifacts_key = os.environ.get("S3_ARTIFACTS_KEY", _DEFAULT_ARTIFACTS_KEY)

    print(f"[INFO]  バケット : s3://{bucket}")
    print(f"[INFO]  リージョン: {region}")
    print(f"[INFO]  ローカル成果物ディレクトリ: {_MODELS_DIR}")
    print()

    try:
        s3 = boto3.client("s3", region_name=region)

        upload_file(s3, _MODELS_DIR / "model.pkl", bucket, model_key)
        upload_file(s3, _MODELS_DIR / "artifacts.pkl", bucket, artifacts_key)

    except (BotoCoreError, ClientError) as e:
        print(f"[ERROR] S3 アップロードに失敗しました: {e}", file=sys.stderr)
        sys.exit(1)

    print()
    print("[INFO]  すべての成果物のアップロードが完了しました。")


if __name__ == "__main__":
    main()
