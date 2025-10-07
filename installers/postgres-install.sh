#!/bin/bash
# Script to install postgres.
set -e

# Check if the --local-debug parameter was passed
POSTGRES_DIR="./pgadmin"

echo "=========================================="
echo "🔧 Installing postgres"
echo "=========================================="

echo "🔍 Adding bitnami repository to helm..."
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo add runix https://helm.runix.net
helm repo update

# Install postgres.
echo "Creating namespace for postgres..."
kubectl create namespace postgresql

echo "🚀 Installing postgres..."
helm install postgres-17 bitnami/postgresql \
    --namespace postgresql \
    --set image.tag=17.5.0 \
    --wait \
    --timeout 10m

echo "*************************."
echo "==> Password to access postgres."
echo "*************************."
kubectl get secret --namespace postgresql postgres-17-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d; echo
echo "*************************."

# Add entries to /etc/hosts.
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 pgadmin.local" >> /etc/hosts'

# Install pgadmin.
echo "🚀 Installing pgadmin..."
helm install pgadmin runix/pgadmin4 --set env.email=admin@admin.com --set env.password=admin-user --set service.type=ClusterIP --namespace postgresql
kubectl apply -f $POSTGRES_DIR/pgadmin-ing.yaml

echo ""
echo "=========================================="
echo "✅ Postgres installed successfully!"
echo "=========================================="