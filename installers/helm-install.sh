#!/bin/bash
# Script to install Helm

set -e

echo "=========================================="
echo "Installing Helm"
echo "=========================================="

# Install helm
echo "📦 Installing Helm..."
curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
chmod +x get_helm.sh
./get_helm.sh

# Verify installation
echo "🔍 Verifying Helm installation..."
helm version
rm get_helm.sh
echo "✅ Helm installed successfully!"

echo ""
echo "=========================================="
echo "✅ Helm installation completed!"
echo "==========================================" 