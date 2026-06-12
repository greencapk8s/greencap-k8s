#!/usr/bin/env bash
set -euo pipefail

IP="$(minikube ip -p greencap-demo)"

if ! [[ "$IP" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: could not determine the IP for profile 'greencap-demo':" >&2
  echo "$IP" >&2
  exit 1
fi

echo "$IP  greencap-demo.local" | sudo tee -a /etc/hosts
