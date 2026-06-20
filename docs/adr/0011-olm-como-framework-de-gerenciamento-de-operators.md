# ADR 0011 — OLM como framework de gerenciamento de Kubernetes Operators

## Status
Accepted

## Context
GreenCap will offer a Developer Experience section with the ability to list, install, and uninstall Kubernetes Operators. Two approaches were considered:

**Option A — CRD discovery (no OLM):** detect installed operators by listing CRDs and inferring ownership from annotations. No external dependency, but no catalog, no lifecycle management, no channel or version awareness.

**Option B — OLM (Operator Lifecycle Manager):** use OLM's typed resources (`PackageManifest`, `Subscription`, `ClusterServiceVersion`, `InstallPlan`) as the foundation. Requires OLM to be present in the cluster.

## Decision
Use OLM (Option B).

The `openshift-client:6.13.1` dependency is already in the project and includes Fabric8 typed models for all OLM resources — no additional dependencies needed. OLM is the industry standard for operator lifecycle management, supported natively by OpenShift/OKD (already referenced in GreenCap's positioning), and installable on minikube via `minikube addons enable olm`.

When OLM is absent, GreenCap shows an informative empty state — no auto-installation of OLM and no degraded mode.

## Consequences
- The Kubernetes Operators feature is fully OLM-dependent. Clusters without OLM see only an empty state.
- `KubernetesClientFactory` will be extended to build an `OpenShiftClient` (via `adapt()`) for OLM resource access.
- Installation is always `AllNamespaces` mode with the `Subscription` created in the `operators` namespace — matching OLM's recommended default for this mode.
- CRDs are never deleted on uninstall — left to the user to avoid accidental data loss from lingering custom resources.
