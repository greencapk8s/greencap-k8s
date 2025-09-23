#!/bin/bash
# Script to install kubectl and configure for Kind cluster

set -e

echo "=========================================="
echo "Installing kubectl and Configuring for Kind Cluster"
echo "=========================================="

# Install kubectl
echo "📦 Installing kubectl..."
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv ./kubectl /usr/local/bin/kubectl
kubectl version
echo "✅ kubectl installed successfully!"

# Configure kubectl to use kind cluster
echo "🔧 Configuring kubectl to use Kind cluster..."
mkdir -p /home/vagrant/.kube/
sudo chown -R vagrant:vagrant /home/vagrant/.kube/
kind get kubeconfig --name k8s-nginx > /home/vagrant/.kube/config
ls -la /home/vagrant/.kube
echo "✅ kubectl configured for Kind cluster!"

# Validate kubectl installation
echo "🔍 Validating kubectl installation..."
kubectl cluster-info
kubectl get nodes
echo "✅ kubectl validation completed!"

echo ""
echo "=========================================="
echo "✅ kubectl installation and configuration completed!"
echo "==========================================" 