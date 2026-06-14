#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-demo"
NODES=3
CPUS=2
MEMORY=2048
DRIVER="docker"

echo "==> Provisioning minikube cluster: profile=$PROFILE, nodes=$NODES, cpus=$CPUS, memory=${MEMORY}MB, driver=$DRIVER"
echo ""

minikube start \
  --profile "$PROFILE" \
  --driver "$DRIVER" \
  --nodes "$NODES" \
  --cpus "$CPUS" \
  --memory "$MEMORY"

echo ""
echo "==> Enabling addons (metrics-server, ingress, registry)"
echo ""

minikube addons enable metrics-server -p "$PROFILE"
minikube addons enable ingress -p "$PROFILE"
minikube addons enable registry -p "$PROFILE"

echo ""
echo "==> Waiting for ingress-nginx controller to be ready..."
kubectl config use-context "$PROFILE"
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=120s

echo ""
echo "==> Cluster '$PROFILE' is ready!"
echo ""
echo "    Nodes:"
minikube node list -p "$PROFILE"
echo ""
echo "    Next step — deploy the demo workloads:"
echo "    bash $(dirname "$0")/create-demo.sh"
