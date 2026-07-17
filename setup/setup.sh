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

# ─── Auto-install helpers (Linux and macOS) ────────────────────────────────────
OS="$(uname -s)"

SUDO=""
[ "$(id -u)" -ne 0 ] && SUDO="sudo"

# ─── macOS (Homebrew) ──────────────────────────────────────────────────────────
# kubectl/minikube/helm/openssl auto-install via brew. Docker is the exception: it
# must be installed and started manually on macOS (see install_docker_macos).
ensure_homebrew() {
  command -v brew &>/dev/null && return

  echo "    Installing Homebrew..."
  NONINTERACTIVE=1 /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

  if [ -x /opt/homebrew/bin/brew ]; then
    eval "$(/opt/homebrew/bin/brew shellenv)"
  elif [ -x /usr/local/bin/brew ]; then
    eval "$(/usr/local/bin/brew shellenv)"
  fi

  command -v brew &>/dev/null || fail "Homebrew installation failed — install manually from https://brew.sh and re-run"
  ok "Homebrew installed"
}

# Docker is a manual prerequisite on macOS — setup.sh does not auto-provision a
# daemon. Docker Desktop needs its GUI/EULA on first launch and a scripted Colima
# provision proved unreliable across environments, so the user installs and starts
# a Docker provider themselves (Docker Desktop, Colima or OrbStack).
install_docker_macos() {
  fail "Docker not found. On macOS you must install and start a Docker provider
       manually — Docker Desktop, Colima or OrbStack — then re-run setup.sh."
}

install_kubectl_macos() {
  ensure_homebrew
  echo "    Installing kubectl via brew..."
  brew install kubectl
  ok "kubectl installed"
}

install_minikube_macos() {
  ensure_homebrew
  echo "    Installing minikube via brew..."
  brew install minikube
  ok "minikube installed"
}

install_helm_macos() {
  ensure_homebrew
  echo "    Installing helm via brew..."
  brew install helm
  ok "helm installed"
}

# ─── Linux (apt/curl) ───────────────────────────────────────────────────────────
install_docker_linux() {
  echo "    Installing Docker via get.docker.com..."
  curl -fsSL https://get.docker.com | $SUDO sh
  $SUDO systemctl enable --now docker
  $SUDO usermod -aG docker "$USER" || true
  ok "Docker installed — restarting setup with docker group active..."
  exec sg docker -c "bash '$0'"
}

install_kubectl_linux() {
  echo "    Installing kubectl..."
  local version
  version="$(curl -Ls https://dl.k8s.io/release/stable.txt)"
  curl -fsSLo /tmp/kubectl "https://dl.k8s.io/release/${version}/bin/linux/amd64/kubectl"
  $SUDO install -o root -g root -m 0755 /tmp/kubectl /usr/local/bin/kubectl
  rm -f /tmp/kubectl
  ok "kubectl ${version} installed"
}

install_minikube_linux() {
  echo "    Installing minikube..."
  curl -fsSLo /tmp/minikube https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
  $SUDO install /tmp/minikube /usr/local/bin/minikube
  rm -f /tmp/minikube
  ok "minikube installed"
}

install_helm_linux() {
  echo "    Installing helm..."
  curl -fsSL https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | $SUDO bash
  ok "helm installed"
}

# ─── OS dispatch ────────────────────────────────────────────────────────────────
# Explicit if/else (not `&&`/`||` one-liners) — under `set -e`, a failure on the
# macOS branch of an `A && B || C` chain falls through to C instead of aborting.
install_docker() {
  if [ "$OS" = "Darwin" ]; then install_docker_macos; else install_docker_linux; fi
}
install_kubectl() {
  if [ "$OS" = "Darwin" ]; then install_kubectl_macos; else install_kubectl_linux; fi
}
install_minikube() {
  if [ "$OS" = "Darwin" ]; then install_minikube_macos; else install_minikube_linux; fi
}
install_helm() {
  if [ "$OS" = "Darwin" ]; then install_helm_macos; else install_helm_linux; fi
}

ensure_docker_accessible() {
  if [ "$OS" = "Darwin" ]; then
    if ! docker version &>/dev/null 2>&1; then
      fail "Docker daemon is not reachable. On macOS, start your Docker provider
       (Docker Desktop, Colima or OrbStack) and re-run setup.sh."
    fi
    return
  fi

  if ! docker version &>/dev/null 2>&1; then
    if ! groups | grep -qw docker; then
      warn "Docker found but user is not in the docker group — adding..."
      $SUDO usermod -aG docker "$USER"
    fi
    warn "Restarting setup with docker group active..."
    exec sg docker -c "bash '$0'"
  fi
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
check_cmd helm
check_cmd openssl

if [ "${#MISSING_TOOLS[@]}" -gt 0 ]; then
  echo ""
  echo -e "    ${YELLOW}Missing: ${MISSING_TOOLS[*]}${RESET}"
  echo ""

  if [[ "$OS" != "Linux" && "$OS" != "Darwin" ]]; then
    fail "Auto-install is only supported on Linux and macOS. Install the missing tools manually and re-run."
  fi

  # AUTO_INSTALL can be pre-set in the environment to skip this prompt (automation/CI)
  if [ -z "${AUTO_INSTALL:-}" ]; then
    read -rp "    Install missing tools automatically? [y/N]: " AUTO_INSTALL
  fi
  # Case-insensitive match without ${VAR,,} — that's Bash 4+ only and breaks under
  # macOS's system /bin/bash, stuck at 3.2 for licensing reasons (see ADR 0016 for
  # the broader context on macOS shipping ancient default tooling)
  case "$AUTO_INSTALL" in
    y|Y) ;;
    *) fail "Install the missing tools manually and re-run setup.sh" ;;
  esac

  echo ""
  for tool in "${MISSING_TOOLS[@]}"; do
    case "$tool" in
      docker)    install_docker   ;;
      kubectl)   install_kubectl  ;;
      minikube)  install_minikube ;;
      helm)      install_helm     ;;
      openssl)
        if [ "$OS" = "Darwin" ]; then
          ensure_homebrew
          brew install openssl && ok "openssl installed"
        else
          $SUDO apt-get install -y openssl && ok "openssl installed"
        fi
        ;;
    esac
  done

  echo ""
  echo "    Re-checking after install..."
  for tool in "${MISSING_TOOLS[@]}"; do
    command -v "$tool" &>/dev/null || fail "$tool installation failed — install manually and re-run"
    ok "$tool  →  $(command -v "$tool")"
  done
fi

# INSTALL_ONLY stops right after dependencies are in place, before touching Docker/
# minikube — used by CI to validate the installers on platforms where the runner
# itself can't provision a VM (e.g. GitHub-hosted macOS runners have no nested
# virtualization, so Colima/Docker can never actually start there; see ADR 0016)
if [ "${INSTALL_ONLY:-}" = "true" ]; then
  echo ""
  ok "INSTALL_ONLY set — dependencies installed, stopping before cluster provisioning"
  exit 0
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
# PROFILE_CHOICE can be pre-set in the environment to skip this prompt (automation/CI)
if [ -z "${PROFILE_CHOICE:-}" ]; then
  read -rp "    Your choice [1/2/3]: " PROFILE_CHOICE
fi
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
    # NODES/CPUS/MEMORY can be pre-set in the environment to skip these prompts
    if [ -z "${NODES:-}" ]; then read -rp "    Number of nodes [1]: " NODES; fi
    if [ -z "${CPUS:-}" ]; then read -rp "    CPUs per node   [2]: " CPUS; fi
    if [ -z "${MEMORY:-}" ]; then read -rp "    RAM per node MB [4096]: " MEMORY; fi
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
step "Step 4: Enabling addons (metrics-server, ingress, registry, olm)"

minikube addons enable metrics-server -p "$PROFILE"
minikube addons enable ingress        -p "$PROFILE"

# Older minikube binaries embed a stale kube-registry-proxy tag pinned to
# gcr.io (e.g. 0.0.8), which no longer exists after minikube's image
# migration to registry.k8s.io. Overriding it here decouples setup.sh from
# whatever addon defaults happen to be baked into the user's minikube
# version, matching what upstream currently ships.
minikube addons enable registry -p "$PROFILE" \
  --images="KubeRegistryProxy=minikube/kube-registry-proxy:v0.0.11" \
  --registries="KubeRegistryProxy=registry.k8s.io"

minikube addons enable olm -p "$PROFILE"

echo ""
echo "    Waiting for OLM operator..."
kubectl rollout status deployment/olm-operator \
  -n olm --timeout=180s
ok "OLM ready"

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

GENERATED_KEY=false
if [ -n "${GREENCAP_ENCRYPTION_KEY:-}" ]; then
  ok "Using GREENCAP_ENCRYPTION_KEY from environment"
else
  # Reuse the existing secret's GREENCAP_ENCRYPTION_KEY on reruns — the Postgres
  # volume persists across runs, so regenerating this key would leave any
  # kubeconfig already encrypted with the old key undecryptable.
  EXISTING_ENCRYPTION_KEY="$(kubectl get secret greencap-secrets -n "$NAMESPACE" -o jsonpath='{.data.GREENCAP_ENCRYPTION_KEY}' 2>/dev/null | base64 --decode 2>/dev/null || true)"
  if [ -n "$EXISTING_ENCRYPTION_KEY" ]; then
    GREENCAP_ENCRYPTION_KEY="$EXISTING_ENCRYPTION_KEY"
    warn "Secret 'greencap-secrets' already exists — reusing current GREENCAP_ENCRYPTION_KEY (previously encrypted kubeconfigs would be unreadable with a new one)"
  else
    GREENCAP_ENCRYPTION_KEY="$(openssl rand -hex 16)"
    GENERATED_KEY=true
  fi
fi

# Reuse the existing secret's DB_PASSWORD on reruns — Postgres only applies
# POSTGRES_PASSWORD on first initdb, so regenerating it here would desync
# the app from a password the already-provisioned PVC no longer recognizes.
if [ -z "${DB_PASSWORD:-}" ]; then
  EXISTING_DB_PASSWORD="$(kubectl get secret greencap-secrets -n "$NAMESPACE" -o jsonpath='{.data.DB_PASSWORD}' 2>/dev/null | base64 --decode 2>/dev/null || true)"
  if [ -n "$EXISTING_DB_PASSWORD" ]; then
    DB_PASSWORD="$EXISTING_DB_PASSWORD"
    warn "Secret 'greencap-secrets' already exists — reusing current DB_PASSWORD (the already-provisioned Postgres volume would reject a new one)"
  else
    DB_PASSWORD="$(openssl rand -hex 16)"
  fi
fi

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

HOSTS_ENTRY="$MINIKUBE_IP  greencap.local  # GreenCap K8s — managed by setup.sh"
if grep -q "greencap.local" /etc/hosts 2>/dev/null; then
  EXISTING_IP=$(grep "greencap.local" /etc/hosts | awk '{print $1}' | head -1)
  if [ "$EXISTING_IP" = "$MINIKUBE_IP" ]; then
    ok "/etc/hosts already contains greencap.local → $MINIKUBE_IP — skipping"
  else
    echo "    Updating greencap.local in /etc/hosts ($EXISTING_IP → $MINIKUBE_IP)..."
    # -i.bak (with a suffix) is the one sed -i form both GNU (Linux) and BSD (macOS) accept
    $SUDO sed -i.bak "/greencap.local/d" /etc/hosts
    $SUDO rm -f /etc/hosts.bak
    echo "$HOSTS_ENTRY" | $SUDO tee -a /etc/hosts > /dev/null
    ok "greencap.local updated: $EXISTING_IP → $MINIKUBE_IP"
  fi
else
  echo "    Adding greencap.local to /etc/hosts..."
  echo "$HOSTS_ENTRY" | $SUDO tee -a /etc/hosts > /dev/null
  ok "greencap.local → $MINIKUBE_IP added to /etc/hosts"
fi
echo ""
