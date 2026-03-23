# infra

Infrastructure configuration for the demand forecast platform.

## Structure

```
infra/
├── docker/     # Supplementary Docker / Compose overrides
└── k8s/        # Kubernetes manifests
```

## Docker

The root `docker-compose.yml` covers local development. Service-specific overrides go in `infra/docker/`.

## Kubernetes

Kubernetes manifests go in `infra/k8s/`. Recommended sub-structure:

```
k8s/
├── base/           # Kustomize base manifests
└── overlays/
    ├── staging/
    └── production/
```
