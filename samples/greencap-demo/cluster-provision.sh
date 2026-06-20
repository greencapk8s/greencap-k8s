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
# to the Pod across restarts.
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

echo ""
echo "==> Cluster '$PROFILE' is ready!"
echo ""
echo "    Nodes:"
minikube node list -p "$PROFILE"
echo ""
echo "    Next step — deploy the demo workloads:"
echo "    bash $(dirname "$0")/create-demo.sh"
