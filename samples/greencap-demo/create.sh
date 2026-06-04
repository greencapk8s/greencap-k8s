#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFESTS_DIR="$SCRIPT_DIR/manifests"
NAMESPACE="greencap-demo"

echo "==> Enabling metrics-server addon (required for HPA)..."
minikube addons enable metrics-server

echo ""
echo "==> Applying greencap-demo manifests..."

for manifest in "$MANIFESTS_DIR"/*.yaml; do
  echo "    applying $(basename "$manifest")..."
  kubectl apply -f "$manifest"
done

echo ""
echo "==> Waiting for deployments to be ready..."
kubectl rollout status deployment/redis    -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/backend  -n "$NAMESPACE" --timeout=120s
kubectl rollout status deployment/frontend -n "$NAMESPACE" --timeout=120s

echo ""
echo "==> greencap-demo is ready!"
echo ""
echo "    Namespace : $NAMESPACE"
echo "    Resources :"
kubectl get all,configmap,secret,pvc,hpa -n "$NAMESPACE" --ignore-not-found
echo ""
echo "    To access the frontend:"
echo "    minikube service frontend -n $NAMESPACE"
