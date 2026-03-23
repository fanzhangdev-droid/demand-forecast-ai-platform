"""
Preprocessing utilities for retail demand model training.

This module:
1. loads raw CSV data
2. validates required columns
3. creates training features
4. encodes categorical variables
5. returns X, y and preprocessing artifacts
"""

from typing import Dict, List, Tuple

import pandas as pd
from sklearn.preprocessing import LabelEncoder


REQUIRED_COLUMNS = [
    "Date",
    "Gender",
    "Age",
    "Product Category",
    "Price per Unit",
    "Quantity",
]


def load_data(csv_path: str) -> pd.DataFrame:
    """
    Load raw CSV file into a DataFrame.
    """
    df = pd.read_csv(csv_path)
    return df


def validate_columns(df: pd.DataFrame) -> None:
    """
    Ensure the required columns exist in the dataset.
    """
    missing = [col for col in REQUIRED_COLUMNS if col not in df.columns]
    if missing:
        raise ValueError(f"Missing required columns: {missing}")


def build_features(
    df: pd.DataFrame,
) -> Tuple[pd.DataFrame, pd.Series, Dict]:
    """
    Build model features and target from raw retail sales data.

    Returns:
        X: training feature dataframe
        y: target series
        artifacts: preprocessing metadata for later inference
    """
    df = df.copy()
    validate_columns(df)

    # 1. Parse date column
    df["Date"] = pd.to_datetime(df["Date"])

    # 2. Create date-based features
    df["year"] = df["Date"].dt.year
    df["month"] = df["Date"].dt.month
    df["day"] = df["Date"].dt.day
    df["day_of_week"] = df["Date"].dt.dayofweek

    # 3. Encode categorical columns
    gender_encoder = LabelEncoder()
    category_encoder = LabelEncoder()

    df["gender_encoded"] = gender_encoder.fit_transform(df["Gender"])
    df["category_encoded"] = category_encoder.fit_transform(df["Product Category"])

    # 4. Select final feature columns
    feature_columns = [
        "gender_encoded",
        "category_encoded",
        "Age",
        "Price per Unit",
        "year",
        "month",
        "day",
        "day_of_week",
    ]

    X = df[feature_columns]
    y = df["Quantity"]

    # 5. Save preprocessing metadata
    artifacts = {
        "feature_columns": feature_columns,
        "gender_classes": list(gender_encoder.classes_),
        "category_classes": list(category_encoder.classes_),
    }

    return X, y, artifacts