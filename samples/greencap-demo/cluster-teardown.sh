#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-demo"

echo "==> Tearing down minikube cluster: profile=$PROFILE"
echo ""
echo "    This will permanently delete the cluster, all nodes, all volumes,"
echo "    and the service account used for GreenCap Token+URL registration."
echo ""
read -r -p "    Are you sure? (yes/N) " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted."
  exit 0
fi

echo ""
echo "==> Deleting minikube cluster '$PROFILE'..."
minikube delete -p "$PROFILE"

echo ""
echo "==> Done. Cluster '$PROFILE' has been removed."
echo ""
echo "    To recreate it:"
echo "    bash $(dirname "$0")/cluster-setup.sh"
