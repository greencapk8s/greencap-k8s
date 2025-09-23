#!/bin/bash
# Script to install ecom-python.

set -e

echo "=========================================="
echo "🔧 Installing ecom-python"
echo "=========================================="

# Add entries to /etc/hosts.
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 ecom-python.local" >> /etc/hosts'

# Build and load docker image.
echo "🚀 Building and loading docker image..."
docker build -t ecom-python-api:latest -f ./playground/projects/ecom-python/Dockerfile ./playground/projects/ecom-python
kind load docker-image ecom-python-api:latest --name k8s-nginx

# Create database.
echo "🔍 Creating database..."
export POSTGRES_PASSWORD=$(kubectl get secret --namespace postgresql postgres-17-postgresql -o jsonpath="{.data.postgres-password}" | base64 -d)
kubectl run postgres-17-postgresql-client --rm --tty -i --restart='Never' --namespace postgresql --image docker.io/bitnami/postgresql:17.5.0 --env="PGPASSWORD=$POSTGRES_PASSWORD" --command -- psql --host postgres-17-postgresql -U postgres -d postgres -p 5432 -c "CREATE DATABASE ecom_python;"

# Deploy ecom-python.
echo "🚀 Deploying ecom-python..."
kubectl apply -f ./playground/projects/ecom-python/infra/namespace.yaml
envsubst < ./playground/projects/ecom-python/infra/deployment.yaml | kubectl apply -f -
kubectl apply -f ./playground/projects/ecom-python/infra/service.yaml
kubectl apply -f ./playground/projects/ecom-python/infra/ingress.yaml

echo ""
echo "=========================================="
echo "✅ Ecom-python installed successfully!"
echo "=========================================="