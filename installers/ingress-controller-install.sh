#!/bin/bash
# Script to install ingress controller.

set -e

echo "=========================================="
echo "🔧 Installing ingress controller"
echo "=========================================="

kubectl apply -f ./playground/ingress-nginx/ingress.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

echo ""
echo "=========================================="
echo "✅ Ingress controller installed successfully!"
echo "=========================================="