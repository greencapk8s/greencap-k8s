#!/bin/bash
# Script to install kubernetes dashboard

set -e

echo "=========================================="
echo "🔧 Installing kubernetes dashboard"
echo "=========================================="

echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 kubernetes-dashboard.greencap" >> /etc/hosts'

kubectl apply -f https://raw.githubusercontent.com/kubernetes/dashboard/v2.7.0/aio/deploy/recommended.yaml
kubectl apply -f ./infra-code-manifests/kubernetes-dashboard/dash-admin.yaml
kubectl -n kubernetes-dashboard create token admin-user; echo
kubectl apply -f ./infra-code-manifests/kubernetes-dashboard/dash-ing.yaml

echo "*************************"
echo "==> Token to access kubernetes dashboard."
echo "*************************"
# kubectl get secret admin-user -n kubernetes-dashboard -o jsonpath={".data.token"} | base64 -d >> ./kubernetes-dashboard/dash-token; echo
kubectl describe secrets admin-user -n kubernetes-dashboard >> ./infra-code-manifests/kubernetes-dashboard/dash-token; echo
cat ./infra-code-manifests/kubernetes-dashboard/dash-token; echo
echo "*************************"

echo ""
echo "=========================================="
echo "✅ Kubernetes dashboard installed successfully!"
echo "=========================================="