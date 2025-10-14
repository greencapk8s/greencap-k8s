#!/bin/bash
# Script to install postgres.
set -e

POSTGRES_DIR="./infra-code-manifests/pgadmin"
POSTGRES_HELM_VALUES_DIR="./helm-values/postgres"

echo "=========================================="
echo "🔧 Installing postgres"
echo "=========================================="

echo "🔍 Adding bitnami repository to helm..."
helm repo add groundhog2k https://groundhog2k.github.io/helm-charts/
helm repo add runix https://helm.runix.net
helm repo update

# Install postgres.
echo "🚀 Installing postgres..."
helm install postgres-17 groundhog2k/postgres \
    --namespace postgresql \
    --set image.tag=17.6 \
    -f $POSTGRES_HELM_VALUES_DIR/values.yaml \
    --create-namespace \
    --wait \
    --timeout 10m

echo "*************************."
echo "==> Password to access postgres."
echo "*************************."
kubectl get secret postgres-17 -n postgresql -o jsonpath="{.data.POSTGRES_PASSWORD}" | base64 -d; echo
echo "*************************."

# Add entries to /etc/hosts.
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 pgadmin.greencap" >> /etc/hosts'

# Install pgadmin.
echo "🚀 Installing pgadmin..."
helm install pgadmin runix/pgadmin4 --set env.email=admin@admin.com --set env.password=admin-user --set service.type=ClusterIP --namespace postgresql
kubectl apply -f $POSTGRES_DIR/pgadmin-ing.yaml

echo ""
echo "=========================================="
echo "✅ Postgres installed successfully!"
echo "=========================================="