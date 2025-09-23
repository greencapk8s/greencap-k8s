#!/bin/bash
# Script to install ecom-python.

set -e

# Check if the --local-debug parameter was passed
ECOM_PYTHON_DIR="./playground/projects/ecom-python"
for arg in "$@"; do
  if [ "$arg" == "--local-debug" ]; then
    ECOM_PYTHON_DIR="./projects/ecom-python"
    break
  fi
done

echo "=========================================="
echo "🔧 Installing ecom-python"
echo "=========================================="

# Add entries to /etc/hosts.
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 ecom-python.local" >> /etc/hosts'

# Build and load docker image.
echo "🚀 Building and loading docker image..."
docker build -t ecom-python-api:latest -f $ECOM_PYTHON_DIR/Dockerfile $ECOM_PYTHON_DIR
kind load docker-image ecom-python-api:latest --name k8s-nginx

# Create database.
echo "🔍 Creating database..."
export POSTGRES_PASSWORD=$(kubectl get secret --namespace postgresql postgres-17-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)
kubectl run postgres-17-postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:17.5.0 --env="PGPASSWORD=$POSTGRES_PASSWORD" --command -- psql --host postgres-17-postgresql -U postgres -d postgres -p 5432 -c "CREATE DATABASE ecom_python;"

# Deploy ecom-python.
echo "🚀 Deploying ecom-python..."
kubectl apply -f $ECOM_PYTHON_DIR/infra/namespace.yaml
envsubst < $ECOM_PYTHON_DIR/infra/deployment.yaml | kubectl apply -f -
kubectl apply -f $ECOM_PYTHON_DIR/infra/service.yaml
kubectl apply -f $ECOM_PYTHON_DIR/infra/ingress.yaml

echo ""
echo "=========================================="
echo "✅ Ecom-python installed successfully!"
echo "=========================================="