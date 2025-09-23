#!/bin/bash
# Script to install ingress controller.

set -e

# Check if the --local-debug parameter was passed
INGRESS_PATH="./playground/ingress-nginx/ingress.yaml"
for arg in "$@"; do
  if [ "$arg" == "--local-debug" ]; then
    INGRESS_PATH="./ingress-nginx/ingress.yaml"
    break
  fi
done

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