#!/usr/bin/env bash
set -euo pipefail

PROFILE="greencap-demo"
NODES=3
CPUS=2
MEMORY=2048
DRIVER="docker"

echo "==> Provisioning minikube cluster: profile=$PROFILE, nodes=$NODES, cpus=$CPUS, memory=${MEMORY}MB, driver=$DRIVER"
echo ""

minikube start \
  --profile "$PROFILE" \
  --driver "$DRIVER" \
  --nodes "$NODES" \
  --cpus "$CPUS" \
  --memory "$MEMORY"

echo ""
echo "==> Enabling addons (metrics-server, ingress, registry, olm)"
echo ""

minikube addons enable metrics-server -p "$PROFILE"
minikube addons enable ingress -p "$PROFILE"
minikube addons enable registry -p "$PROFILE"
minikube addons enable olm -p "$PROFILE"

kubectl config use-context "$PROFILE"

echo ""
echo "==> Installing local-path-provisioner and setting it as the default StorageClass"
echo ""

# The 'standard' StorageClass (minikube hostpath-provisioner) provisions PVs immediately,
# before the consuming Pod is scheduled — it has no way to know which node the Pod will
# land on, so it never sets nodeAffinity. In this multi-node cluster, a Pod (re)scheduled
# to a different node than the one holding its PV's hostPath directory mounts an empty
# directory instead of failing loudly. local-path-provisioner uses
# volumeBindingMode: WaitForFirstConsumer, so it provisions only after the Pod is already
# scheduled and correctly binds the PV to that node. See local-path-storage.yaml for
# provenance — vendored in-repo rather than applied from the upstream URL.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
kubectl apply -f "$SCRIPT_DIR/local-path-storage.yaml"
kubectl -n local-path-storage rollout status deployment/local-path-provisioner --timeout=90s

kubectl patch storageclass standard \
  -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"false"}}}'
kubectl patch storageclass local-path \
  -p '{"metadata": {"annotations":{"storageclass.kubernetes.io/is-default-class":"true"}}}'

echo ""
echo "==> Waiting for OLM operator..."
kubectl rollout status deployment/olm-operator -n olm --timeout=180s

echo ""
echo "==> Waiting for ingress-nginx controller to be ready..."
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=120s

echo ""
echo "==> Persisting the internal Container Registry..."
echo ""

# The minikube 'registry' addon Deployment ships with no storage, so /var/lib/registry
# is container-local and lost on every pod restart. Patching the addon's Deployment in
# place (instead of a custom registry manifest) keeps the Service and registry-proxy
# DaemonSet that come with the addon, and survives re-running 'minikube addons enable
# registry': kubectl apply's 3-way merge only reverts fields declared in the addon's
# own manifest, which never included volumes/volumeMounts/nodeSelector.
kubectl apply -f - <<'EOF'
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
      storage: 4Gi
EOF

# nodeSelector pins the registry Pod to the control-plane node. The 'standard'
# StorageClass (minikube hostpath-provisioner) creates PVs without nodeAffinity, so in
# a multi-node cluster a rescheduled Pod would mount an empty node-local directory on
# whatever node it lands on. Pinning to a fixed node keeps the hostPath data attached
# to the Pod across restarts. This PVC still targets 'standard' explicitly rather than
# the 'local-path' default installed above — left as-is (predates local-path-provisioner,
# already battle-tested); switching it to rely on local-path's own nodeAffinity handling
# and dropping this nodeSelector is a candidate cleanup, not done here to avoid touching
# a working path outside this fix's scope.
kubectl patch deployment registry -n kube-system --type=strategic --patch-file /dev/stdin <<EOF
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

# Create a cluster-admin service account for GreenCap Token+URL registration
SA_NAME="greencap-admin"
SA_NS="kube-system"
if ! kubectl get serviceaccount "$SA_NAME" -n "$SA_NS" &>/dev/null; then
  kubectl create serviceaccount "$SA_NAME" -n "$SA_NS"
  kubectl create clusterrolebinding "$SA_NAME" \
    --clusterrole=cluster-admin \
    --serviceaccount="$SA_NS:$SA_NAME"
fi
GREENCAP_TOKEN=$(kubectl create token "$SA_NAME" -n "$SA_NS" --duration=8760h)

echo ""
echo "==> Cluster '$PROFILE' is ready!"
echo ""
echo "    Nodes:"
minikube node list -p "$PROFILE"
echo ""
echo "    GreenCap — Token + URL registration:"
echo "    API Server URL : $(kubectl config view --minify -o jsonpath='{.clusters[0].cluster.server}')"
echo "    Bearer Token   : $GREENCAP_TOKEN"
echo ""
echo "    Next step — deploy the demo workloads:"
echo "    bash $(dirname "$0")/create-demo.sh"
