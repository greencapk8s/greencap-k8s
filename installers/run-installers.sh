#!/bin/bash

set -e

# [begin] Minimal setup.
./installers/configure-docker-daemon.sh
./installers/configure-hosts.sh
./installers/kind-install.sh $USER_NAME_INSTALL
./installers/kubectl-install.sh $USER_NAME_INSTALL
./installers/helm-install.sh
./installers/cilium-install.sh
./installers/kubectl-top-install.sh
./installers/ingress-controller-install.sh
./installers/kube-dash-install.sh
./installers/tech-docs-install.sh
./installers/apache-hello-install.sh
# [end] Minimal setup.

if [ -f ./greencap.ini ]; then
    echo "Reading greencap.ini file..."
    MONITORING_INSTALL=$(grep '^monitoring=' ./greencap.ini | cut -d'=' -f2)
    HARBOR_INSTALL=$(grep '^harbor=' ./greencap.ini | cut -d'=' -f2)
    GITLAB_INSTALL=$(grep '^gitlab=' ./greencap.ini | cut -d'=' -f2)
    POSTGRES_INSTALL=$(grep '^postgres=' ./greencap.ini | cut -d'=' -f2)
    ECOM_PYTHON_INSTALL=$(grep '^ecom-python=' ./greencap.ini | cut -d'=' -f2)
fi

if [[ "$SETUP_TYPE" == "full" ]] || [[ "$SETUP_TYPE" == "custom" && "$MONITORING_INSTALL" == "true" ]]; then
    ./installers/monitoring-install.sh
fi

if [[ "$SETUP_TYPE" == "full" ]] || [[ "$SETUP_TYPE" == "custom" && "$HARBOR_INSTALL" == "true" ]]; then
    ./installers/harbor-install.sh
fi

if [[ "$SETUP_TYPE" == "full" ]] || [[ "$SETUP_TYPE" == "custom" && "$GITLAB_INSTALL" == "true" ]]; then
    ./installers/gitlab-install.sh
fi

if [[ "$SETUP_TYPE" == "full" ]] || [[ "$SETUP_TYPE" == "custom" && "$POSTGRES_INSTALL" == "true" ]]; then
    ./installers/postgres-install.sh
fi

if [[ "$SETUP_TYPE" == "full" ]] || [[ "$SETUP_TYPE" == "custom" && "$ECOM_PYTHON_INSTALL" == "true" ]]; then
    ./installers/ecom-python-install.sh
fi

if [[ "$PROVIDER" != "local" ]]; then
    USER_NAME_INSTALL="$USER_NAME_INSTALL" MONITORING_INSTALL="$MONITORING_INSTALL" ./installers/configure-shortcuts.sh
fi