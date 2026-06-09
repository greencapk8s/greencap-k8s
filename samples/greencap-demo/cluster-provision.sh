#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-demo"
NODES=1
CPUS=2
MEMORY=4096
DRIVER="virtualbox"

echo "==> Provisioning minikube cluster: profile=$PROFILE, nodes=$NODES, cpus=$CPUS, memory=${MEMORY}MB, driver=$DRIVER"
echo ""

minikube start \
  --profile "$PROFILE" \
  --driver "$DRIVER" \
  --nodes "$NODES" \
  --cpus "$CPUS" \
  --memory "$MEMORY"

echo ""
echo "==> Cluster '$PROFILE' is ready!"
echo ""
echo "    Nodes:"
minikube node list -p "$PROFILE"
echo ""
echo "    Next step — deploy the demo workloads:"
echo "    bash $(dirname "$0")/create.sh"
