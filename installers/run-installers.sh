#!/bin/bash

set -e

./installers/configure-docker-daemon.sh
./installers/configure-hosts.sh
./installers/kind-install.sh $USER_NAME_INSTALL
./installers/kubectl-install.sh $USER_NAME_INSTALL
./installers/helm-install.sh
./installers/cilium-install.sh
./installers/kubectl-top-install.sh
./installers/ingress-controller-install.sh
./installers/apache-hello-install.sh
./installers/kube-dash-install.sh
./installers/monitoring-install.sh
./installers/harbor-install.sh
./installers/gitlab-install.sh
./installers/postgres-install.sh
./installers/ecom-python-install.sh