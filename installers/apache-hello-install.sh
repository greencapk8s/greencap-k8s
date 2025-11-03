#!/bin/bash
# Script to install apache hello.

set -e

APACHE_HELLO_DIR="./infra-code-manifests/apache-hello"

echo "=========================================="
echo "🔧 Installing apache hello"
echo "=========================================="

kubectl create namespace ns1
kubectl apply -f $APACHE_HELLO_DIR/hello-apache-cm.yaml

export APACHE_IMAGE_VERSION=${APACHE_IMAGE_VERSION:-httpd:2.4-alpine}
envsubst < $APACHE_HELLO_DIR/hello-apache-dpl.yaml | kubectl apply -f -

kubectl apply -f $APACHE_HELLO_DIR/hello-apache-svc.yaml
kubectl apply -f $APACHE_HELLO_DIR/hello-apache-ing.yaml

echo ""
echo "=========================================="
echo "✅ Apache hello installed successfully!"
echo "=========================================="