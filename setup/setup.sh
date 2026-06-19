#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-platform"
NAMESPACE="greencap-platform"
IMAGE="localhost:5000/greencap-platform/platform:latest"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MANIFESTS_DIR="$SCRIPT_DIR/manifests"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ─── Output helpers ────────────────────────────────────────────────────────────
BOLD='\033[1m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
RESET='\033[0m'

step() { echo ""; echo -e "${BOLD}==> $*${RESET}"; echo ""; }
ok()   { echo -e "    ${GREEN}✓${RESET}  $*"; }
warn() { echo -e "    ${YELLOW}⚠${RESET}  $*"; }
fail() { echo -e "    ${RED}✗${RESET}  $*"; exit 1; }

# ─── Port-forward cleanup ──────────────────────────────────────────────────────
PF_PID=""
cleanup() { [ -n "$PF_PID" ] && kill "$PF_PID" 2>/dev/null || true; }
trap cleanup EXIT

echo ""
echo -e "${GREEN}${BOLD}  GreenCap K8s — Setup${RESET}"
echo ""

# ─── Auto-install helpers (Linux only) ────────────────────────────────────────
SUDO=""
[ "$(id -u)" -ne 0 ] && SUDO="sudo"

install_docker() {
  echo "    Installing Docker via get.docker.com..."
  curl -fsSL https://get.docker.com | $SUDO sh
  $SUDO systemctl enable --now docker
  $SUDO usermod -aG docker "$USER" || true
  ok "Docker installed — restarting setup with docker group active..."
  exec sg docker -c "bash '$0'"
}

ensure_docker_accessible() {
  if ! docker version &>/dev/null 2>&1; then
    if ! groups | grep -qw docker; then
      warn "Docker found but user is not in the docker group — adding..."
      $SUDO usermod -aG docker "$USER"
    fi
    warn "Restarting setup with docker group active..."
    exec sg docker -c "bash '$0'"
  fi
}

install_kubectl() {
  echo "    Installing kubectl..."
  local version
  version="$(curl -Ls https://dl.k8s.io/release/stable.txt)"
  curl -fsSLo /tmp/kubectl "https://dl.k8s.io/release/${version}/bin/linux/amd64/kubectl"
  $SUDO install -o root -g root -m 0755 /tmp/kubectl /usr/local/bin/kubectl
  rm -f /tmp/kubectl
  ok "kubectl ${version} installed"
}

install_minikube() {
  echo "    Installing minikube..."
  curl -fsSLo /tmp/minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
  $SUDO install /tmp/minikube /usr/local/bin/minikube
  rm -f /tmp/minikube
  ok "minikube installed"
}

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 1: Checking requirements"

MISSING_TOOLS=()

check_cmd() {
  if command -v "$1" &>/dev/null; then
    ok "$1  →  $(command -v "$1")"
  else
    echo -e "    ${RED}✗${RESET}  $1 not found"
    MISSING_TOOLS+=("$1")
  fi
}

check_cmd docker
check_cmd minikube
check_cmd kubectl
check_cmd openssl

if [ "${#MISSING_TOOLS[@]}" -gt 0 ]; then
  echo ""
  echo -e "    ${YELLOW}Missing: ${MISSING_TOOLS[*]}${RESET}"
  echo ""

  if [[ "$(uname -s)" != "Linux" ]]; then
    fail "Auto-install is only supported on Linux. Install the missing tools manually and re-run."
  fi

  read -rp "    Install missing tools automatically? [y/N]: " AUTO_INSTALL
  if [[ "${AUTO_INSTALL,,}" != "y" ]]; then
    fail "Install the missing tools manually and re-run setup.sh"
  fi

  echo ""
  for tool in "${MISSING_TOOLS[@]}"; do
    case "$tool" in
      docker)    install_docker   ;;
      kubectl)   install_kubectl  ;;
      minikube)  install_minikube ;;
      openssl)   $SUDO apt-get install -y openssl && ok "openssl installed" ;;
    esac
  done

  echo ""
  echo "    Re-checking after install..."
  for tool in "${MISSING_TOOLS[@]}"; do
    command -v "$tool" &>/dev/null || fail "$tool installation failed — install manually and re-run"
    ok "$tool  →  $(command -v "$tool")"
  done
fi

ensure_docker_accessible

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 2: Installation profile"

echo "    Select the resource profile for the GreenCap cluster:"
echo ""
echo "    [1] Minimal      —  1 node,  2 CPUs,  4 GB RAM"
echo "    [2] Recommended  —  3 nodes, 2 CPUs,  3 GB RAM each  (default)"
echo "    [3] Custom       —  you define nodes, CPUs and RAM"
echo ""
read -rp "    Your choice [1/2/3]: " PROFILE_CHOICE
PROFILE_CHOICE="${PROFILE_CHOICE:-2}"

case "$PROFILE_CHOICE" in
  1)
    NODES=1; CPUS=2; MEMORY=4096
    ok "Minimal  —  1 node, 2 CPUs, 4 GB"
    ;;
  2)
    NODES=3; CPUS=2; MEMORY=3072
    ok "Recommended  —  3 nodes, 2 CPUs, 3 GB each"
    ;;
  3)
    echo ""
    read -rp "    Number of nodes [1]: " NODES
    read -rp "    CPUs per node   [2]: " CPUS
    read -rp "    RAM per node MB [4096]: " MEMORY
    NODES="${NODES:-1}"; CPUS="${CPUS:-2}"; MEMORY="${MEMORY:-4096}"
    ok "Custom  —  $NODES node(s), $CPUS CPUs, ${MEMORY} MB each"
    ;;
  *)
    fail "Invalid choice. Re-run setup.sh and select 1, 2 or 3."
    ;;
esac

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 3: Starting minikube cluster (profile: $PROFILE)"

if minikube status -p "$PROFILE" --format='{{.Host}}' 2>/dev/null | grep -q "Running"; then
  ok "Cluster '$PROFILE' is already running — skipping creation"
  kubectl config use-context "$PROFILE"
else
  minikube start \
    --profile  "$PROFILE" \
    --driver   docker \
    --nodes    "$NODES" \
    --cpus     "$CPUS" \
    --memory   "$MEMORY"
  kubectl config use-context "$PROFILE"
  ok "Cluster '$PROFILE' started"
fi

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 4: Enabling addons (metrics-server, ingress, registry)"

minikube addons enable metrics-server -p "$PROFILE"
minikube addons enable ingress        -p "$PROFILE"
minikube addons enable registry       -p "$PROFILE"

echo ""
echo "    Waiting for ingress-nginx controller..."
kubectl rollout status deployment/ingress-nginx-controller \
  -n ingress-nginx --timeout=180s
ok "ingress-nginx ready"

echo ""
echo "    Persisting the internal registry with an 8 Gi PVC..."

kubectl apply -f - <<EOF
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: registry-storage
  namespace: kube-system
spec:
  accessModes:
    - ReadWriteOnce
  storageClassName: standard
  resources:
    requests:
      storage: 8Gi
EOF

# Pin the registry Pod to the control-plane node so the hostPath PV remains
# attached across pod restarts in multi-node clusters.
kubectl patch deployment registry -n kube-system --type=strategic \
  --patch-file /dev/stdin <<EOF
spec:
  template:
    spec:
      nodeSelector:
        kubernetes.io/hostname: $PROFILE
      containers:
      - name: registry
        volumeMounts:
        - name: registry-storage
          mountPath: /var/lib/registry
      volumes:
      - name: registry-storage
        persistentVolumeClaim:
          claimName: registry-storage
EOF

kubectl rollout status deployment/registry -n kube-system --timeout=120s
ok "Registry ready and persistent (8 Gi)"

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 5: Building and pushing GreenCap image"

echo "    Starting port-forward to registry at localhost:5000..."
kubectl port-forward -n kube-system service/registry 5000:80 &>/dev/null &
PF_PID=$!

# Wait until port 5000 accepts connections (up to 15 s)
for i in $(seq 1 15); do
  nc -z localhost 5000 2>/dev/null && break
  sleep 1
done
nc -z localhost 5000 2>/dev/null || fail "Registry port-forward did not become ready"
ok "Port-forward active (PID $PF_PID)"

echo "    Building from $PROJECT_ROOT/docker/Dockerfile..."
docker build -t "$IMAGE" -f "$PROJECT_ROOT/docker/Dockerfile" "$PROJECT_ROOT"
ok "Image built: $IMAGE"

echo "    Pushing to registry..."
docker push "$IMAGE"
ok "Image pushed"

kill "$PF_PID" 2>/dev/null || true
PF_PID=""

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 6: Creating namespace and secrets"

kubectl apply -f "$MANIFESTS_DIR/00-namespace.yaml"

if [ -z "${GREENCAP_ENCRYPTION_KEY:-}" ]; then
  GREENCAP_ENCRYPTION_KEY="$(openssl rand -hex 16)"
  GENERATED_KEY=true
else
  GENERATED_KEY=false
  ok "Using GREENCAP_ENCRYPTION_KEY from environment"
fi

DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 16)}"

echo "    Extracting kubeconfig for cluster '$PROFILE'..."
SELF_CLUSTER_KUBECONFIG="$(kubectl config view --minify --flatten --context "$PROFILE")"
ok "Kubeconfig extracted"

# --dry-run + apply makes the secret creation idempotent on re-runs
kubectl create secret generic greencap-secrets \
  --namespace "$NAMESPACE" \
  --from-literal=DB_PASSWORD="$DB_PASSWORD" \
  --from-literal=GREENCAP_ENCRYPTION_KEY="$GREENCAP_ENCRYPTION_KEY" \
  --from-literal=GREENCAP_SELF_CLUSTER_KUBECONFIG="$SELF_CLUSTER_KUBECONFIG" \
  --dry-run=client -o yaml | kubectl apply -f -

ok "Secret 'greencap-secrets' applied"

# ═══════════════════════════════════════════════════════════════════════════════
step "Step 7: Deploying Postgres and GreenCap"

for manifest in "$MANIFESTS_DIR"/*.yaml; do
  [ -f "$manifest" ] || continue
  echo "    applying $(basename "$manifest")..."
  kubectl apply -f "$manifest"
done

echo ""
echo "    Waiting for Postgres..."
kubectl rollout status deployment/postgres \
  -n "$NAMESPACE" --timeout=120s
ok "Postgres ready"

echo ""
echo "    Waiting for GreenCap (may take 2–3 min on first start)..."
kubectl rollout status deployment/greencap \
  -n "$NAMESPACE" --timeout=300s
ok "GreenCap ready"

# ═══════════════════════════════════════════════════════════════════════════════
MINIKUBE_IP="$(minikube ip -p "$PROFILE")"

echo ""
echo -e "${BOLD}${GREEN}════════════════════════════════════════════════════════${RESET}"
echo -e "${BOLD}${GREEN}  GreenCap is ready!${RESET}"
echo -e "${BOLD}${GREEN}════════════════════════════════════════════════════════${RESET}"
echo ""
echo "    URL      :  http://greencap.local"
echo "    Login    :  admin"
echo "    Password :  admin  (change after first login)"
echo ""

if [ "$GENERATED_KEY" = true ]; then
  echo -e "    ${YELLOW}ENCRYPTION KEY — save this, you will need it to recover cluster data:${RESET}"
  echo "    $GREENCAP_ENCRYPTION_KEY"
  echo ""
fi

echo "    Add to /etc/hosts (run once with sudo):"
echo "    echo \"$MINIKUBE_IP  greencap.local\" | sudo tee -a /etc/hosts"
echo ""
