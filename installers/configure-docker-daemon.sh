#!/bin/bash
# Script to configure Docker daemon for insecure registries

set -e

echo "=========================================="
echo "Configuring Docker daemon for insecure registries."
echo "=========================================="

# Backup daemon.json if it exists and backup doesn't exist yet
if [ -f /etc/docker/daemon.json ] && [ ! -f /etc/docker/daemon.json.gcbck ]; then
    echo "💾 Creating backup of /etc/docker/daemon.json..."
    sudo cp /etc/docker/daemon.json /etc/docker/daemon.json.gcbck
    echo "Backup created: /etc/docker/daemon.json.gcbck"
fi

# Configure docker to use insecure registry
echo "📝 Creating Docker daemon configuration..."
sudo cat > /etc/docker/daemon.json << 'EOF'
{
  "insecure-registries": [
    "core.harbor.greencap:30001",
    "notary.harbor.greencap:30001",
    "harbor-core.harbor.svc.cluster.local"
  ]
}
EOF

# Set permissions and restart docker
echo "🔧 Setting permissions and restarting Docker..."
sudo chmod 644 /etc/docker/daemon.json
sudo systemctl restart docker

# Verify configuration
echo "Verifying Docker configuration..."
docker info | grep -A 10 "Insecure Registries"

echo ""
echo "=========================================="
echo "Docker daemon configured successfully."
echo "=========================================="