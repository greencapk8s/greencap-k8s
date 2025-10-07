#!/bin/bash
# Script to install GitLab

set -e

GITLAB_DIR="./helm-values/gitlab"

echo "=========================================="
echo "Installing GitLab"
echo "=========================================="

echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 gitlab.local" >> /etc/hosts'

helm repo add gitlab https://charts.gitlab.io/
helm repo update

echo "Installing GitLab..."
helm upgrade --install gitlab gitlab/gitlab \
  --namespace gitlab --create-namespace \
  -f $GITLAB_DIR/values.yaml \
  --wait \
  --timeout 10m

kubectl wait --for=condition=Ready pod -l app=toolbox -n gitlab --timeout=5m || true

echo "*************************"
echo "==> GitLab root password:"
echo "*************************"
kubectl get secret gitlab-root-password -n gitlab -o jsonpath="{.data.password}" | base64 -d; echo
echo "*************************"

echo ""
echo "=========================================="
echo "✅ GitLab installation completed!"
echo "==========================================" 

# Run this to uninstall gitlab.
# helm uninstall gitlab -n gitlab
# kubectl delete pvc --all -n gitlab
# kubectl delete secrets --all -n gitlab
# kubectl delete ns gitlab
