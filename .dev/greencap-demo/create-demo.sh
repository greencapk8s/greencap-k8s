#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFESTS_DIR="$SCRIPT_DIR/manifests"
NAMESPACE="greencap-demo"
PROFILE="greencap-demo"

echo "==> Switching kubectl context to $PROFILE..."
kubectl config use-context "$PROFILE"

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
kubectl get all,configmap,secret,pvc,hpa,ingress -n "$NAMESPACE" --ignore-not-found
echo ""
echo ""
echo "    Ingress host: greencap-demo.local"
echo "    Add to /etc/hosts (run with sudo):"
echo "    echo \"\$(minikube ip -p $PROFILE)  greencap-demo.local\" | sudo tee -a /etc/hosts"
