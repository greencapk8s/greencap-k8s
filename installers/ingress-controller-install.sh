#!/bin/bash

set -e

INGRESS_PATH="./infra-code-manifests/ingress-nginx/ingress.yaml"

echo "=========================================="
echo "Installing ingress controller"
echo "=========================================="

kubectl apply -f "$INGRESS_PATH"
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

echo "==> Ingress controller installed successfully!"