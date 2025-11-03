#!/bin/bash
# Script to install Kind and create cluster

set -e

kind_install_user=$1

if [ -z "$kind_install_user" ]; then
    echo "Erro: Parameter user to install the kind not informed."
    echo "Example: ./kind-install.sh ubuntu"
    exit 1
fi

echo "=========================================="
echo "Installing Kind and Creating Cluster"
echo "=========================================="

# Install kind
KIND_VERSION="v0.29.0"
echo "📦 Installing Kind..."
curl -Lo ./kind https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-linux-amd64
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
sudo chown $kind_install_user:$kind_install_user kind-config.yaml
echo "✅ Kind configuration file created!"

# Create cluster
echo "🚀 Creating Kind cluster..."
kind create cluster --name greencap-k8s --config kind-config.yaml
echo "✅ Kind cluster created successfully!"

echo ""
echo "=========================================="
echo "✅ Kind installation and cluster creation completed!"
echo "=========================================="
