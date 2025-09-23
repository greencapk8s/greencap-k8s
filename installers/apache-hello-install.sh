#!/bin/bash
# Script to install apache hello.

set -e

echo "=========================================="
echo "🔧 Installing apache hello"
echo "=========================================="

kubectl create namespace ns1
kubectl apply -f ./playground/apache-hello/hello-apache-cm.yaml
kubectl apply -f ./playground/apache-hello/hello-apache-dpl.yaml
kubectl apply -f ./playground/apache-hello/hello-apache-svc.yaml
kubectl apply -f ./playground/apache-hello/hello-apache-ing.yaml

echo ""
echo "=========================================="
echo "✅ Apache hello installed successfully!"
echo "=========================================="