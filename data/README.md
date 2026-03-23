# data

Local data storage for the demand forecast platform. **Do not commit raw or processed data files** — only the directory structure is tracked via `.gitkeep`.

## Structure

```
data/
├── raw/         # Source data as received (immutable)
├── processed/   # Cleaned and feature-engineered datasets
└── models/      # Trained model artifacts (.pkl, .onnx, etc.)
```

## Notes

- `data/raw/` and `data/processed/` are excluded from git (see root `.gitignore`).
- `data/models/` is also excluded; model artifacts should be stored in a model registry or object store (e.g. S3, MLflow).
- The `data/` directory is mounted read-only into `ml-service` via Docker volume.
