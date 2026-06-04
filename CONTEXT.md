# GreenCap K8s

A web platform for monitoring and managing external Kubernetes clusters. GreenCap does not provision clusters — it registers access credentials to clusters that exist outside the platform.

## Language

**Cluster**:
A registered access point to an external Kubernetes cluster. Stores an encrypted kubeconfig as the single credential needed to connect to it. GreenCap monitors and operates on Clusters but does not own or provision them.
_Avoid_: Connection, server, environment

**ConnectionStatus**:
The result of the last connection attempt to a Cluster. Not a persistent authoritative state — a snapshot of what was observed. Values: `UNKNOWN` (never tested), `CONNECTED` (last check succeeded), `DISCONNECTED` (cluster unreachable — timeout or no route), `ERROR` (cluster responded but rejected the request — invalid credentials or permission denied). A failed namespace fetch on login transitions a `CONNECTED` cluster to `DISCONNECTED`; clusters already in any other status are not updated by this path — only the explicit "Test Connection" action can move them back to `CONNECTED`.
_Avoid_: Status, health, availability

**User**:
A person with access to the GreenCap platform. Has a role that determines what they can do.
_Avoid_: Account, member, principal

**Role**:
The access level of a User. `VIEWER` — read-only access to clusters and workloads. `OPERATOR` — read + write access to clusters and workload operations. `ADMIN` — full access including user management (create, deactivate).
_Avoid_: Permission, privilege, group

**Namespace**:
A logical isolation unit within a Cluster that groups Workloads. Not a Workload itself.
_Avoid_: Environment, project, partition

**Workload**:
A deployable unit running inside a Namespace. In GreenCap, the concrete types are Pod, Deployment, and ReplicaSet.
_Avoid_: Resource, object, service

**Pod**:
The smallest Workload unit — one or more containers running together. Read-only in GreenCap (observed, not managed).
_Avoid_: Container, instance, process

**Deployment**:
A Workload that manages a set of replica Pods via one or more ReplicaSets. Exposes desired, ready, and available replica counts.
_Avoid_: app, service

**ReplicaSet**:
A Kubernetes resource that maintains a stable set of replica Pods. Almost always created and owned by a Deployment — each rollout produces a new ReplicaSet while the previous ones are retained for rollback. In GreenCap, displayed read-only under the Workloads section with an Owner column indicating the parent Deployment (or "—" for orphans).
_Avoid_: RS, replica controller

**ClusterProvider**:
Contextual metadata describing the Kubernetes distribution behind a Cluster (OKD, OpenShift, Kubernetes, Rancher). Does not alter GreenCap's behavior — used for display and identification only.
_Avoid_: Type, flavor, vendor

**Kubeconfig**:
The encrypted credential stored in a Cluster that contains everything needed to connect to the Kubernetes API (server URL, certificates, token). The single source of truth for cluster access — no separate URL field.
_Avoid_: Secret, token, certificate, credentials

**createdBy**:
Audit field on Cluster recording which User registered it. Does not imply ownership or restrict visibility — all Operators and Admins see all Clusters regardless of who created them.
_Avoid_: Owner, author, responsible

**Service**:
A Kubernetes network resource that exposes a set of Pods under a stable IP and port. Types: ClusterIP, NodePort, LoadBalancer, ExternalName. In GreenCap, displayed read-only under the Rede section. Never confused with "Workload" — a Service routes traffic, it does not run code.
_Avoid_: LoadBalancer (as a synonym for all Service types), endpoint, proxy

**ConfigMap**:
Key-value configuration data stored unencrypted in the cluster and injected into Workloads as environment variables or mounted files. In GreenCap, displayed read-only under the Parameters section — only metadata and key count are shown, not values.
_Avoid_: Config, settings, properties

**Secret**:
Sensitive key-value data (credentials, tokens, certificates) stored in the cluster. In GreenCap, only metadata is displayed (name, type, key count) — values are never decoded or shown.
_Avoid_: Kubeconfig (a Kubeconfig is a GreenCap concept; Secret is a Kubernetes object)

**Networking**:
UI section grouping network-related Kubernetes resources visible in GreenCap. Currently contains Services. Inspired by AWS Networking grouping. Future: Ingress.
_Avoid_: Rede, network, LoadBalancer

**Parameters**:
UI section grouping application-level parameter resources injected into Workloads within a Namespace. Currently contains ConfigMaps and Secrets. The name reflects that these resources configure applications, not the GreenCap platform itself. Inspired by AWS Parameter Store / Secrets Manager grouping.
_Avoid_: Configuração, Config, Settings

**PodMetric**:
A point-in-time resource usage sample for a Pod, collected by the metrics-server from the kubelet. Contains CPU usage (in millicores) and memory usage (in MiB) aggregated across all containers in the Pod. In GreenCap, displayed read-only under the Observability section as a top-pods listing, scoped to the active Namespace.
_Avoid_: Stats, usage, telemetry

**Event**:
A Kubernetes-native occurrence record emitted by the control plane or controllers when something happens to a resource. Has a `type` (always `Normal` or `Warning` — the only two values defined by the Kubernetes API spec), a `reason` (machine-readable cause), a `message` (human-readable description), an `involvedObject` (the resource that triggered it), and a `count` (how many times it repeated). In GreenCap, displayed read-only under the Observability section, scoped to the active Namespace.
_Avoid_: Log, alert, notification

**Manifest**:
The full YAML representation of a Kubernetes resource as returned by the API server. Read-only in GreenCap — displayed in a dedicated page per resource, reachable via an action icon in each listing view. The page URL encodes resource type, namespace, and name (e.g., `/yaml/pod/payments/my-pod`) to support deep-linking and future editing.
_Avoid_: Config, definition, spec

**HorizontalScaler**:
A Kubernetes `HorizontalPodAutoscaler` resource that automatically adjusts the replica count of a target Workload (typically a Deployment) based on observed metrics. In GreenCap, displayed read-only under the Auto Scaling section, scoped to the active Namespace.
_Avoid_: HPA, AutoScaler, HorizontalPodAutoscaler

**AutoScaling**:
UI section grouping scaling-related Kubernetes resources. Currently contains HorizontalScaler. Inspired by AWS Auto Scaling grouping.
_Avoid_: Scaling, autoscaling, scaler

**PersistentVolumeClaim**:
A request for persistent storage made by an application running in a Namespace. Namespaced. Kubernetes matches the claim against an available PersistentVolume. In GreenCap, displayed read-only under the Storage section. Status values: `Bound` (storage allocated and ready), `Pending` (awaiting a matching PersistentVolume), `Terminating` (deletion in progress — derived from `metadata.deletionTimestamp`), `Lost` (backing PersistentVolume disappeared).
_Avoid_: PVC, Volume, disk

**Storage**:
UI section grouping persistent storage resources visible in GreenCap. Currently contains PersistentVolumeClaims. Inspired by AWS Storage grouping.
_Avoid_: Volumes, persistent storage, disks

**PersistentVolume**:
A cluster-scoped storage resource representing a physical or virtual disk provisioned in the cluster. Not namespaced. Bound one-to-one to a PersistentVolumeClaim. In GreenCap, displayed read-only under Infrastructure in the Settings section. Status values: `Available` (free, no claim), `Bound` (allocated to a PVC), `Released` (PVC deleted, awaiting reclaim), `Terminating` (deletion in progress), `Failed` (provisioning error).
_Avoid_: PV, disk, volume

**StorageClass**:
A cluster-scoped Kubernetes resource that defines how PersistentVolumes are dynamically provisioned (provisioner, reclaim policy, binding mode, expansion support). Not namespaced. In GreenCap, displayed read-only under Infrastructure in the Settings section.
_Avoid_: SC, storage profile, storage tier

**Infrastructure**:
UI section within Settings grouping cluster-scoped infrastructure resources. Currently contains PersistentVolumes and StorageClasses. Distinct from Storage (which is namespace-scoped) and Settings (which is GreenCap platform configuration).
_Avoid_: Admin, cluster resources, system

**Topologia**:
UI view that renders an interactive graph of Kubernetes resources within a Namespace and the relationships between them. Node types: Deployment, ReplicaSet, Pod, Service, PersistentVolumeClaim. Edges derived from `ownerReferences` (Deployment→ReplicaSet→Pod), label selector matching (Service→Pod), and volume mounts (PodGroup→PersistentVolumeClaim via `spec.volumes[].persistentVolumeClaim.claimName`). Isolated nodes (no edges) are shown — they signal misconfiguration. Clicking a node navigates to its Manifest. Pan and zoom are enabled.
_Avoid_: Diagram, map, graph

**TopologyGraph**:
The data transfer object returned by `TopologyService` representing the full graph for a Namespace. Contains a flat list of `TopologyNode` and a flat list of `TopologyEdge`. Built server-side; the frontend only renders what it receives.
_Avoid_: Graph data, node map

**TopologyNode**:
A single resource in the `TopologyGraph`. Carries: a unique `id` (type + name), a display `label` (resource name), a `type` (Deployment, ReplicaSet, Pod, Service, PersistentVolumeClaim), a `status` (for badge coloring), and a `manifestUrl` (deep-link to the Manifest view). PersistentVolumeClaim nodes additionally carry `capacity`, `accessMode`, and `serviceType` (used for storageClass).
_Avoid_: Node, vertex, element

**TopologyEdge**:
A directed relationship in the `TopologyGraph` between two `TopologyNode` ids. Direction always flows from owner/controller to owned (Deployment→ReplicaSet→Pod) or from Service to its target Pods.
_Avoid_: Link, connection, arrow

