#!/bin/bash
# Script to install harbor.

set -e

echo "=========================================="
echo "🔧 Installing Harbor"
echo "=========================================="

# Add entries to /etc/hosts.
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 core.harbor.domain" >> /etc/hosts'
sudo bash -c 'echo "127.0.0.1 notary.harbor.domain" >> /etc/hosts'

# Add harbor repository to helm.
echo "🔍 Adding harbor repository to helm..."
helm repo add harbor https://helm.goharbor.io
helm repo update

# Install harbor.
echo "🚀 Installing Harbor..."
kubectl create namespace harbor
helm install harbor harbor/harbor --namespace harbor --values ./playground/harbor/values.yaml

echo ""
echo "=========================================="
echo "✅ Harbor installed successfully!"
echo "=========================================="