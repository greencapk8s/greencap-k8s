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
A person with access to the GreenCap platform. Access is determined by the explicit set of Permissions granted to them — there is no Role field.
_Avoid_: Account, member, principal

**Permission**:
A named capability granted explicitly to a User. Persisted as a `Set<Permission>` on the User entity (`@ElementCollection`). Covers two levels: view-level (can the user navigate to a section?) and action-level (can the user perform a write operation within that section?). Examples: `WORKLOADS_DEPLOYMENTS_VIEW`, `WORKLOADS_DEPLOYMENTS_SCALE`, `SETTINGS_USERS_VIEW`. Loaded as Spring Security `GrantedAuthority` objects at login so that `hasAuthority()` checks work at the framework level. ADMIN, OPERATOR and VIEWER are preset labels used only in Flyway migration to seed permissions for existing users — they are not a persistent concept.
_Avoid_: Role, privilege, group

**Namespace**:
A logical isolation unit within a Cluster that groups Workloads. Not a Workload itself.
_Avoid_: Environment, project, partition

**Workload**:
A deployable unit running inside a Namespace. In GreenCap, the concrete types are Pod, Deployment, ReplicaSet, Job, and CronJob.
_Avoid_: Resource, object, service

**Pod**:
The smallest Workload unit — one or more containers running together. Read-only in GreenCap (observed, not managed).
_Avoid_: Container, instance, process

**Deployment**:
A Workload that manages a set of replica Pods via one or more ReplicaSets. Exposes desired, ready, and available replica counts. Supports two write operations in GreenCap: Scale (change desired replica count) and Restart (rolling restart — replaces pods one by one without downtime).
_Avoid_: app, service

**Scale**:
A write operation on a Deployment that changes the desired replica count. Takes effect immediately via the Kubernetes API. The user sets the new count via a dialog pre-populated with the current desired value.
_Avoid_: resize, update replicas

**Restart**:
A write operation on a Deployment that triggers a rolling restart — Kubernetes replaces each Pod one by one, causing momentary traffic shift but no hard downtime. Implemented via a patch to `spec.template.metadata.annotations["kubectl.kubernetes.io/restartedAt"]`.
_Avoid_: redeploy, bounce, kill

**ReplicaSet**:
A Kubernetes resource that maintains a stable set of replica Pods. Almost always created and owned by a Deployment — each rollout produces a new ReplicaSet while the previous ones are retained for rollback. In GreenCap, displayed read-only under the Workloads section with an Owner column indicating the parent Deployment (or "—" for orphans).
_Avoid_: RS, replica controller

**Job**:
A Workload that runs a finite task to completion. Tracks how many Pods succeeded (`completions`) out of how many were desired. In GreenCap, displayed under the Workloads section, scoped to the active Namespace. Status derived from `.status.conditions`: `Complete`, `Failed`, `Running`, or `Suspended`. Supports one write operation: Delete.
_Avoid_: Task, batch job, process

**CronJob**:
A Workload that creates Jobs on a recurring schedule defined by a cron expression. Owns zero or more active Jobs at any moment. Can be suspended — pausing new Job creation without deleting existing ones. In GreenCap, displayed under the Workloads section, scoped to the active Namespace. Supports three write operations: Trigger, Suspend/Resume, and Delete.
_Avoid_: Scheduled job, scheduler, task

**Trigger**:
A write operation on a CronJob that immediately creates a new Job from the CronJob's `spec.jobTemplate`, bypassing the schedule. The generated Job name follows the pattern `<cronjob-name>-manual-<epoch-seconds>` to guarantee uniqueness. After triggering, GreenCap navigates to JobsView filtered by the parent CronJob so the user can observe the new Job.
_Avoid_: Run now, execute, start, dispatch

**Suspend**:
A write operation on a CronJob that pauses new Job creation by patching `spec.suspend: true`. Does not affect Jobs already running at the time of suspension. The inverse operation, Resume, patches `spec.suspend: false` and resumes the schedule. In GreenCap both are represented by a single toggling button that reflects the current state (Suspend when active, Resume when suspended).
_Avoid_: Pause, stop, disable

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

**Ingress**:
A namespaced Kubernetes resource that routes external HTTP/HTTPS traffic to Services based on host and path rules. Has an optional `ingressClassName` identifying which IngressController handles it — displayed as `"—"` when absent. Hosts are collapsed into a comma-separated string for grid display. TLS is shown as a badge: `success` when any TLS block is configured, `contrast` ("Plain") otherwise. In GreenCap, displayed read-only under the Networking section, scoped to the active Namespace, protected by `NETWORKING_INGRESS_VIEW`.
_Avoid_: Route, proxy, gateway, LoadBalancer

**Networking**:
UI section grouping network-related Kubernetes resources visible in GreenCap. Currently contains Services and Ingresses. Inspired by AWS Networking grouping.
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

**Node**:
A cluster-scoped Kubernetes resource representing a physical or virtual machine that runs Workloads. Not namespaced. Each Node exposes allocatable CPU and memory (the capacity available for scheduling, after system overhead). Role is derived from labels: a Node with label `node-role.kubernetes.io/control-plane` or `node-role.kubernetes.io/master` is a Control Plane node; all others are Workers. Status is determined by the `Ready` condition: `Ready` (node is healthy and accepting Pods), `NotReady` (node is not accepting Pods), `Unknown` (node controller lost contact). In GreenCap, displayed read-only under the Infrastructure section, protected by `SETTINGS_INFRASTRUCTURE_VIEW`.
_Avoid_: Machine, host, server, instance

**Infrastructure**:
UI section within Settings grouping cluster-scoped infrastructure resources. Currently contains PersistentVolumes, StorageClasses, and Nodes. Distinct from Storage (which is namespace-scoped) and Settings (which is GreenCap platform configuration).
_Avoid_: Admin, cluster resources, system

**Topologia**:
UI view that renders an interactive graph of Kubernetes resources within a Namespace and the relationships between them. Node types: Deployment, ReplicaSet, Pod, Service, PersistentVolumeClaim. Edges derived from `ownerReferences` (Deployment→ReplicaSet→Pod), label selector matching (Service→Pod), and volume mounts (PodGroup→PersistentVolumeClaim via `spec.volumes[].persistentVolumeClaim.claimName`). Isolated nodes (no edges) are shown — they signal misconfiguration. Pods owned by a Job (directly or via a CronJob's Job) are deliberately excluded — they represent finite task executions, not the long-running service topology this view is meant to map. Clicking a node navigates to its Manifest. Pan and zoom are enabled. Optionally renders `TopologyGroup` containers around nodes sharing `app.kubernetes.io/part-of`/`app.kubernetes.io/component` labels, toggleable via a control that is on by default.
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

**TopologyGroup**:
A visual container drawn around `TopologyNode`s that share the same `app.kubernetes.io/part-of` and/or `app.kubernetes.io/component` label value, rendered as a Cytoscape compound node. Grouping is nested: an outer group by `part-of` value, with an inner group by `component` value for nodes that also carry it. A node carrying only `component` (no `part-of`) forms its own outer-level group. Nodes without either label are not grouped — they render normally outside any container. Labels for synthetic nodes (PodGroup) are derived from the first Pod in the group; PersistentVolumeClaim nodes use their own metadata labels. Purely visual — groups do not collapse or expand. Toggled on `Topologia` via a control that is on by default; group boxes are labeled `key: value` (e.g. `part-of: payments`).
_Avoid_: Cluster, container, namespace grouping, folder

**PlatformSettings**:
User-scoped preferences that control GreenCap's behavior across sessions. Not related to Kubernetes resources — these are settings about the platform itself. Persisted per User in the database so they follow the user across devices. Currently contains: auto-refresh interval. Accessed via the Settings menu item in the sidebar Settings section.
_Avoid_: Preferences, config, global settings

**TopologyLayout**:
A persisted snapshot of the visual state of the Topologia view for a specific User + Cluster + Namespace combination. Stores the pixel positions of all currently visible TopologyNodes (as a complete JSON snapshot, not incremental) and the `groupingEnabled` toggle state. Scoped per user+cluster+namespace so each combination has an independent layout. Saved automatically after each node drag (replacing the previous snapshot entirely — stale nodes from prior graph states are dropped on the next save). On load, saved positions are applied to matching nodes; new nodes with no saved position are placed by fcose. On first access (no saved layout), fcose positions all nodes normally.
_Avoid_: Saved graph, layout cache, position state

**PodLog**:
A snapshot of the stdout/stderr output of a container within a Pod, fetched via the Kubernetes API with a configurable line limit (tail). In GreenCap, displayed in a dedicated read-only page (`logs/pod/{namespace}/{name}`) with auto-poll every 3 seconds. Supports two modes: current (active container) and previous (last terminated instance of the container — useful for CrashLoopBackOff diagnosis). When no previous log exists, the page shows an informative message instead of an error.
_Avoid_: Output, stdout, console, terminal

