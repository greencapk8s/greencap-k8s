#!/bin/bash
# Script to install kubernetes dashboard.

set -e

# Check if the --local-debug parameter was passed
KUBE_DASH_DIR="./playground/kubernetes-dashboard"
for arg in "$@"; do
  if [ "$arg" == "--local-debug" ]; then
    KUBE_DASH_DIR="./kubernetes-dashboard"
    break
  fi
done

echo "=========================================="
echo "🔧 Installing kubernetes dashboard"
echo "=========================================="

kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
kubectl apply -f $KUBE_DASH_DIR/dash-admin.yaml
kubectl -n kubernetes-dashboard create token admin-user; echo
kubectl apply -f $KUBE_DASH_DIR/dash-ing.yaml

echo "*************************."
echo "==> Token to access kubernetes dashboard."
echo "*************************."
kubectl get secret admin-user -n kubernetes-dashboard -o jsonpath={".data.token"} | base64 -d >> ./$KUBE_DASH_DIR/dash-token ; echo
cat ./$KUBE_DASH_DIR/dash-token; echo
echo "*************************."

echo ""
echo "=========================================="
echo "✅ Kubernetes dashboard installed successfully!"
echo "=========================================="