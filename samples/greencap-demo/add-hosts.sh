#!/usr/bin/env bash
echo "$(minikube ip)  greencap-demo.local" | sudo tee -a /etc/hosts
