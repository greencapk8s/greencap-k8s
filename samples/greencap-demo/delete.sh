#!/usr/bin/env bash
set -euo pipefail

NAMESPACE="greencap-demo"

echo "==> Deleting namespace '$NAMESPACE' and all its resources..."
kubectl delete namespace "$NAMESPACE" --ignore-not-found

echo ""
echo "==> Done. All greencap-demo resources have been removed."
