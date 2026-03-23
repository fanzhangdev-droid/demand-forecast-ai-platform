"""
Train a simple retail quantity prediction model and save artifacts
for later FastAPI inference.
"""

from pathlib import Path

import joblib
from sklearn.ensemble import RandomForestRegressor
from sklearn.metrics import mean_absolute_error
from sklearn.model_selection import train_test_split

from training.preprocess import load_data, build_features


def main() -> None:
    base_dir = Path(__file__).resolve().parent.parent
    data_path = base_dir / "data" / "retail_sales_dataset.csv"
    models_dir = base_dir / "models"
    model_path = models_dir / "model.pkl"
    artifacts_path = models_dir / "artifacts.pkl"

    models_dir.mkdir(parents=True, exist_ok=True)

    print("Step 1: loading raw dataset...")
    df = load_data(str(data_path))
    print(f"Dataset shape: {df.shape}")
    print(df.head())

    print("\nStep 2: building features...")
    X, y, artifacts = build_features(df)
    print("Feature columns:")
    print(artifacts["feature_columns"])
    print("\nFeature sample:")
    print(X.head())

    print("\nStep 3: splitting train and test data...")
    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=0.2,
        random_state=42
    )
    print(f"Train size: {len(X_train)}")
    print(f"Test size: {len(X_test)}")

    print("\nStep 4: training model...")
    model = RandomForestRegressor(
        n_estimators=100,
        random_state=42
    )
    model.fit(X_train, y_train)

    print("\nStep 5: evaluating model...")
    predictions = model.predict(X_test)
    mae = mean_absolute_error(y_test, predictions)
    print(f"MAE: {mae:.4f}")

    print("\nStep 6: saving model and preprocessing artifacts...")
    joblib.dump(model, model_path)
    joblib.dump(artifacts, artifacts_path)

    print(f"Model saved to: {model_path}")
    print(f"Artifacts saved to: {artifacts_path}")
    print("\nTraining completed successfully.")


if __name__ == "__main__":
    main()