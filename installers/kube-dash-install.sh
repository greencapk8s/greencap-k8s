#!/bin/bash
# Script to install kubernetes dashboard.

set -e

echo "=========================================="
echo "🔧 Installing kubernetes dashboard"
echo "=========================================="

kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
kubectl apply -f ./playground/kubernetes-dashboard/dash-admin.yaml
kubectl -n kubernetes-dashboard create token admin-user; echo
kubectl apply -f ./playground/kubernetes-dashboard/dash-ing.yaml

echo "*************************."
echo "==> Token to access kubernetes dashboard."
echo "*************************."
kubectl get secret admin-user -n kubernetes-dashboard -o jsonpath={".data.token"} | base64 -d >> ./playground/dash-token; echo
cat ./playground/dash-token; echo
echo "*************************."

echo ""
echo "=========================================="
echo "✅ Kubernetes dashboard installed successfully!"
echo "=========================================="