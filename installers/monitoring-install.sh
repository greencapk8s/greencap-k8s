#!/bin/bash

# Script to install the monitoring stack
# Prometheus + Grafana

set -e

MONITORING_DIR="./helm-values/monitoring"

echo "=========================================="
echo "Monitoring Stack Installation"
echo "=========================================="

# Add Helm repositories
echo "📦 Adding Helm repositories..."
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

# Add entries to /etc/hosts
echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 grafana.greencap" >> /etc/hosts'
sudo bash -c 'echo "127.0.0.1 prometheus.greencap" >> /etc/hosts'

# 1. Install Prometheus Stack
PROMETHEUS_CHART_VERSION="79.0.1"
echo "🚀 Installing Prometheus Stack..."
kubectl create namespace monitoring --dry-run=client -o yaml | kubectl apply -f -
helm install prometheus prometheus-community/kube-prometheus-stack \
    --version=${PROMETHEUS_CHART_VERSION} \
    --namespace monitoring \
    --values $MONITORING_DIR/prometheus-values.yaml \
    --wait \
    --timeout 10m

# Wait for pods to be ready
echo "⏳ Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app.kubernetes.io/name=grafana -n monitoring --timeout=300s

# Check installation status
echo "🔍 Checking installation status..."
kubectl get pods -n monitoring

echo ""
echo "=========================================="
echo "✅ Monitoring Stack installed successfully!"
echo "=========================================="
echo ""
echo "🌐 Access URLs:"
echo "  - Grafana: http://grafana.greencap:30001"
echo "    User: admin, Password: prom-operator"
echo ""
echo "  - Prometheus: http://prometheus.greencap:30001"
echo ""
echo "==========================================" 