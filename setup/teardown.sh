#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-platform"

BOLD='\033[1m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
RESET='\033[0m'

echo ""
echo -e "${BOLD}${RED}  WARNING: This will permanently delete the '$PROFILE' minikube cluster${RESET}"
echo -e "${RED}  and all data inside it (Postgres database, registry images, etc).${RESET}"
echo ""
read -rp "    Type 'yes' to confirm: " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
  echo -e "    ${YELLOW}Teardown cancelled.${RESET}"
  exit 0
fi

echo ""
echo -e "${BOLD}==> Deleting minikube profile '$PROFILE'...${RESET}"
echo ""

minikube delete -p "$PROFILE"

echo ""
echo "    Cluster '$PROFILE' deleted."
echo ""
