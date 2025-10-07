#!/bin/bash

# Script to install the tracing stack
# Jaeger

set -e

echo "=========================================="
echo "Tracing Stack Installation"
echo "=========================================="

# Add Helm repositories
echo "📦 Adding Helm repositories..."
helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
helm repo update

# Add entries to /etc/hosts
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 jaeger.local" >> /etc/hosts'

# Install Jaeger
echo "🔍 Installing Jaeger..."
kubectl create namespace jaeger --dry-run=client -o yaml | kubectl apply -f -
helm install jaeger jaegertracing/jaeger \
    --namespace jaeger \
    --values ./greencap/tracing/jaeger-values.yaml \
    --wait \
    --timeout 5m

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=jaeger -n jaeger --timeout=300s

# Check installation status
echo "🔍 Checking installation status..."
kubectl get pods -n jaeger

echo ""
echo "=========================================="
echo "✅ Tracing Stack installed successfully!"
echo "=========================================="
echo ""
echo "🌐 Access URLs:"
echo "  - Jaeger: http://jaeger.local:30001"
echo ""
echo "==========================================" 