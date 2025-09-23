#!/bin/bash
# Script to install Cilium

set -e

echo "=========================================="
echo "Installing Cilium"
echo "=========================================="

# Install Cilium CLI
echo "📦 Installing Cilium CLI..."
CILIUM_CLI_VERSION=$(curl -s https://raw.githubusercontent.com/cilium/cilium-cli/main/stable.txt)
CLI_ARCH=amd64
if [ "$(uname -m)" = "aarch64" ]; then CLI_ARCH=arm64; fi
curl -L --fail --remote-name-all https://github.com/cilium/cilium-cli/releases/download/${CILIUM_CLI_VERSION}/cilium-linux-${CLI_ARCH}.tar.gz{,.sha256sum}
sha256sum --check cilium-linux-${CLI_ARCH}.tar.gz.sha256sum
sudo tar xzvfC cilium-linux-${CLI_ARCH}.tar.gz /usr/local/bin
rm cilium-linux-${CLI_ARCH}.tar.gz{,.sha256sum}
echo "✅ Cilium CLI installed successfully!"

# Install Cilium in cluster
echo "🚀 Installing Cilium in cluster..."
cilium install --version=$(curl -s https://raw.githubusercontent.com/cilium/cilium/refs/heads/main/stable.txt)
kubectl get pods -A
cilium version
cilium status --wait
echo "✅ Cilium installed in cluster successfully!"

echo ""
echo "=========================================="
echo "✅ Cilium installation completed!"
echo "==========================================" 