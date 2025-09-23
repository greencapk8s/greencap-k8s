#!/bin/bash
# Script to configure Docker daemon for insecure registries

set -e

echo "=========================================="
echo "Configuring Docker Daemon for Insecure Registries"
echo "=========================================="

# Configure docker to use insecure registry
echo "📝 Creating Docker daemon configuration..."
sudo cat > /etc/docker/daemon.json << 'EOF'
{
  "insecure-registries": [
    "core.harbor.domain:30001",
    "notary.harbor.domain:30001",
    "harbor-core.harbor.svc.cluster.local"
  ]
}
EOF

# Set permissions and restart docker
echo "🔧 Setting permissions and restarting Docker..."
sudo chmod 644 /etc/docker/daemon.json
sudo systemctl restart docker

# Verify configuration
echo "✅ Verifying Docker configuration..."
docker info | grep -A 10 "Insecure Registries"

echo ""
echo "=========================================="
echo "✅ Docker daemon configured successfully!"
echo "=========================================="
