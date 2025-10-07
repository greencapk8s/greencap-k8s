#!/bin/bash
# Script to install ingress controller.

set -e

# Check if the --local-debug parameter was passed
INGRESS_PATH="./ingress-nginx/ingress.yaml"

echo "=========================================="
echo "🔧 Installing ingress controller"
echo "=========================================="

kubectl apply -f "$INGRESS_PATH"
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

echo ""
echo "=========================================="
echo "✅ Ingress controller installed successfully!"
echo "=========================================="