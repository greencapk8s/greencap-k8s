#!/bin/bash

./installers/configure-docker-daemon.sh
./installers/configure-hosts.sh
./installers/kind-install.sh $USER_NAME_INSTALL
./installers/kubectl-install.sh $USER_NAME_INSTALL
./installers/helm-install.sh
./installers/cilium-install.sh
./installers/kubectl-top-install.sh
./installers/ingress-controller-install.sh --local-debug
./installers/kube-dash-install.sh --local-debug
./installers/monitoring-install.sh --local-debug
./installers/apache-hello-install.sh --local-debug
./installers/harbor-install.sh --local-debug
./installers/postgres-install.sh --local-debug
./installers/ecom-python-install.sh --local-debug