#!/bin/bash
# Script to install GitLab

set -e

GITLAB_DIR="./helm-values/gitlab"

echo "=========================================="
echo "Installing GitLab"
echo ""
echo "⚠️  Attention: Installing GitLab may take several minutes (up to 20 minutes or more)."
echo "Please be patient!"
echo ""
echo "=========================================="

echo "📝 Adding entries to /etc/hosts..."
sudo bash -c 'echo "127.0.0.1 gitlab.greencap" >> /etc/hosts'

helm repo add gitlab https://charts.gitlab.io/
helm repo update

GITLAB_CHART_VERSION="9.5.1"
echo "Installing GitLab..."
helm upgrade --install gitlab gitlab/gitlab \
  --version=${GITLAB_CHART_VERSION} \
  --namespace gitlab --create-namespace \
  -f $GITLAB_DIR/values.yaml \
  --wait \
  --timeout 20m

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
