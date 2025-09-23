#!/bin/bash
# Script to install Kind and create cluster

set -e

echo "=========================================="
echo "Installing Kind and Creating Cluster"
echo "=========================================="

# Install kind
echo "📦 Installing Kind..."
curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.29.0/kind-linux-amd64
chmod +x ./kind
sudo mv ./kind /usr/local/bin/kind
kind --version
echo "✅ Kind installed successfully!"

# Create kind configuration file
echo "📝 Creating Kind configuration file..."
cat > kind-config.yaml <<EOF
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
name: cluster1
nodes:
- role: control-plane
  extraPortMappings: # Ports to be exposed from the cluster
  - containerPort: 30001
    hostPort: 30001
  - containerPort: 30002
    hostPort: 30002
- role: worker
- role: worker
networking:
  disableDefaultCNI: true # Disable the default CNI plugin
  podSubnet: "10.244.0.0/16"
  serviceSubnet: "10.245.0.0/16"
EOF
sudo chown vagrant:vagrant kind-config.yaml
echo "✅ Kind configuration file created!"

# Create cluster
echo "🚀 Creating Kind cluster..."
kind create cluster --name k8s-nginx --config kind-config.yaml
echo "✅ Kind cluster created successfully!"

echo ""
echo "=========================================="
echo "✅ Kind installation and cluster creation completed!"
echo "=========================================="
