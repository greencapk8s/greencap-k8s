#!/bin/bash

set -e

echo "=========================================="
echo "Installing Kind and Creating Cluster"
echo "=========================================="

echo "📦 Installing kubectl top..."

# Install metrics-server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Patch metrics-server to allow insecure TLS to kind.
kubectl patch deployment metrics-server -n kube-system --type='json' -p='[{"op": "add", "path": "/spec/template/spec/containers/0/args/-", "value": "--kubelet-insecure-tls"}]'

echo "✅ kubectl top installed successfully!"
