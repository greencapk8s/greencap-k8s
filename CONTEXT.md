# GreenCap K8s

A web platform for monitoring and managing external Kubernetes clusters. GreenCap does not provision clusters — it registers access credentials to clusters that exist outside the platform. GreenCap is not a read-only tool — it actively manages Kubernetes resources (create, delete, scale, restart, suspend, trigger) within registered clusters, subject to the Kubernetes RBAC permissions of the acting User.

## Purpose & Audience

GreenCap targets individuals and small/medium teams who study, develop, and test against Kubernetes — an approachable alternative to heavyweight platforms like OpenShift or Rancher for contexts that don't need their operational complexity. This shapes the product's priorities (simplicity, low setup cost, clear UI over exhaustive feature coverage) but does not change GreenCap's core responsibility: it remains a management layer over Clusters the user already has (minikube, kind, managed, on-prem, ...), not a cluster lifecycle/provisioning tool (see Cluster).
_Avoid_: Enterprise platform, cloud provider, cluster provisioner

## Language

**Cluster**:
A registered access point to an external Kubernetes cluster. Internally stores an encrypted kubeconfig as the credential used for all operations. Users may register a Cluster by pasting a kubeconfig directly, or by providing an API server URL and a bearer token (which GreenCap synthesizes into a kubeconfig at registration time). GreenCap monitors and operates on Clusters but does not own or provision them.
_Avoid_: Connection, server, environment

**ConnectionStatus**:
The result of the last connection attempt to a Cluster. Not a persistent authoritative state — a snapshot of what was observed. Values: `UNKNOWN` (never tested), `CONNECTED` (last check succeeded), `DISCONNECTED` (cluster unreachable — timeout or no route), `ERROR` (cluster responded but rejected the request — invalid credentials or permission denied). A failed namespace fetch on login transitions a `CONNECTED` cluster to `DISCONNECTED`; clusters already in any other status are not updated by this path — only the explicit "Test Connection" action can move them back to `CONNECTED`.
_Avoid_: Status, health, availability

**User**:
A person with access to the GreenCap platform. Non-admin Users are represented in their assigned Cluster by a UserServiceAccount, and their access to Kubernetes resources is governed entirely by Kubernetes RBAC. Each non-admin User belongs to exactly one Cluster.
_Avoid_: Account, member, principal

**PlatformAdmin**:
The single administrative User of the GreenCap platform, identified by the fixed username `admin`. Uses the Cluster's kubeconfig directly for all operations, giving full cluster access. The only User who can register Clusters, create/deactivate other Users, and switch between Clusters. Not represented by a UserServiceAccount.
_Avoid_: Admin user, superuser, root

**UserServiceAccount**:
A Kubernetes ServiceAccount created in the `greencap-system` namespace of a Cluster when a non-admin User is created. Its bearer token is stored encrypted on the User record and used for all Kubernetes API calls made on that User's behalf. Bound to a ClusterRole via a ClusterRoleBinding. Deleted from the cluster when the User is deactivated.
_Avoid_: SA, service account, user credentials

**ClusterRole**:
A Kubernetes ClusterRole assigned to a User's UserServiceAccount via a ClusterRoleBinding, determining the User's access level across all namespaces in the Cluster. Selected by the PlatformAdmin at User creation time and editable afterwards. GreenCap populates the selector from the ClusterRoles available in the target Cluster.
_Avoid_: Role, permission level, access level

**Namespace**:
A logical isolation unit within a Cluster that groups Workloads. Cluster-scoped — not itself a Workload. In GreenCap, displayed in the Global section with resource counts (Pods, Deployments, Services) to give a quick overview of what lives inside. Supports two write operations: Create and Delete Namespace. System namespaces (`kube-system`, `kube-public`, `kube-node-lease`, `default`) are protected from deletion in the UI — the Delete action is disabled when one of these is selected.
_Avoid_: Environment, project, partition

**Create Namespace**:
A write operation that provisions a new Namespace in the active Cluster. Requires only a name (validated against Kubernetes DNS subdomain rules).
_Avoid_: New namespace, add namespace

**Delete Namespace**:
A write operation that permanently removes a Namespace and **all namespaced resources inside it** — Pods, Deployments, Services, ConfigMaps, Secrets, PersistentVolumeClaims, Ingresses, and more — via Kubernetes cascading deletion. The Namespace transitions to `Terminating` until all contained resources are garbage-collected. In GreenCap, requires the user to type the Namespace name in the confirmation dialog before proceeding, reducing the risk of accidental destruction. Not available for system namespaces.
_Avoid_: Remove namespace, drop namespace

**Workload**:
A deployable unit running inside a Namespace. In GreenCap, the concrete types are Pod, Deployment, ReplicaSet, StatefulSet, Job, and CronJob.
_Avoid_: Resource, object, service

**Pod**:
The smallest Workload unit — one or more containers running together. In GreenCap, supports one write operation: Delete (removes the Pod from the cluster; the owning ReplicaSet or controller will recreate it if applicable). The Pods listing hides Pods belonging to a Job that have completed successfully (`Succeeded`) by default, via a toggle that is on by default — reduces clutter from recurring CronJobs. Pods filtered to a specific Job (via `?job=`) are shown regardless of phase.
_Avoid_: Container, instance, process

**Deployment**:
A Workload that manages a set of replica Pods via one or more ReplicaSets. Exposes desired, ready, and available replica counts. Supports two write operations in GreenCap: Scale (change desired replica count) and Restart (rolling restart — replaces pods one by one without downtime). In GreenCap, the listing also shows a Nodes column — the distinct Nodes currently running its Pods (or "—" if none).
_Avoid_: app, service

**Scale**:
A write operation on a Deployment that changes the desired replica count. Takes effect immediately via the Kubernetes API. The user sets the new count via a dialog pre-populated with the current desired value.
_Avoid_: resize, update replicas

**Restart**:
A write operation on a Deployment that triggers a rolling restart — Kubernetes replaces each Pod one by one, causing momentary traffic shift but no hard downtime. Implemented via a patch to `spec.template.metadata.annotations["kubectl.kubernetes.io/restartedAt"]`.
_Avoid_: redeploy, bounce, kill

**ReplicaSet**:
A Kubernetes resource that maintains a stable set of replica Pods. Almost always created and owned by a Deployment — each rollout produces a new ReplicaSet while the previous ones are retained for rollback. In GreenCap, displayed under the Workloads section with an Owner column indicating the parent Deployment (or "—" for orphans), and a Nodes column listing the distinct Nodes currently running its Pods (or "—" if none). Supports one write operation: Delete.
_Avoid_: RS, replica controller

**StatefulSet**:
A Workload that manages a set of replica Pods with stable, unique network identities and stable storage. Pods follow an ordinal naming scheme (`<name>-0`, `<name>-1`, ...) and are created, scaled, and deleted in order. Associated with a headless Service (via `spec.serviceName`) that provides per-pod DNS resolution, and may define `volumeClaimTemplates` that provision a dedicated PersistentVolumeClaim per Pod. Supports four write operations in GreenCap: Scale, Restart, Rollback, and Delete — same mechanisms as Deployment. In GreenCap, the listing also shows a Nodes column — the distinct Nodes currently running its Pods (or "—" if none).
_Avoid_: STS, stateful app, sharded service, replica set

**Job**:
A Workload that runs a finite task to completion. Tracks how many Pods succeeded (`completions`) out of how many were desired. In GreenCap, displayed under the Workloads section, scoped to the active Namespace. Status derived from `.status.conditions`: `Complete`, `Failed`, `Running`, or `Suspended`. Also shows a Nodes column listing the distinct Nodes that ran/are running its Pods (or "—" if none). Supports one write operation: Delete.
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
Contextual metadata describing the Kubernetes distribution behind a Cluster. Current values: `MinikubeDocker` (displayed as "Minikube (Docker)") and `OpenShift` (planned, not yet supported). Does not alter GreenCap's behavior — used for display and identification only.
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
A namespaced Kubernetes resource that routes external HTTP/HTTPS traffic to Services based on host and path rules. Has an optional `ingressClassName` identifying which IngressController handles it — displayed as `"—"` when absent. Hosts are collapsed into a comma-separated string for grid display. TLS is shown as a badge: `success` when any TLS block is configured, `contrast` ("Plain") otherwise. In GreenCap, displayed read-only under the Networking section, scoped to the active Namespace.
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
The full YAML representation of a Kubernetes resource as returned by the API server. Displayed in a dedicated page per resource, reachable via an action icon in each listing view. The page URL encodes resource type, namespace, and name (e.g., `/yaml/pod/payments/my-pod`) to support deep-linking. For the 12 namespaced resource types covered by GreenCap's listing views (Pod, Deployment, StatefulSet, ReplicaSet, Job, CronJob, Service, Ingress, ConfigMap, Secret, HorizontalScaler, PersistentVolumeClaim), the Manifest supports the Apply write operation. Node, PersistentVolume and StorageClass (cluster-scoped) remain read-only.
_Avoid_: Config, definition, spec

**Apply**:
A write operation on a Manifest that submits user-edited YAML back to the cluster as a full replace (PUT) of the resource — closer to `kubectl replace`/`kubectl edit` than to `kubectl apply`'s three-way merge. An Edit toggle makes the Manifest's YAML editable and focuses the editor; Apply then submits the edited content and, on success, returns to the read-only view with the refreshed Manifest. Before submitting, GreenCap strips server-managed fields (`resourceVersion`, `uid`, `creationTimestamp`, `generation`, `managedFields`, `status`) so the replace targets the latest server state — last-write-wins on `spec`/`metadata`, unaffected by `status` churn from controllers. Rejected before submission if the edited `kind`, `metadata.name`, or `metadata.namespace` no longer match the page's resource.
_Avoid_: kubectl apply (different semantics — three-way merge, not full replace), Save, Update, Patch

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
A cluster-scoped storage resource representing a physical or virtual disk provisioned in the cluster. Not namespaced. Bound one-to-one to a PersistentVolumeClaim. In GreenCap, displayed under Infrastructure in the Global section. Supports one write operation: Delete. Status values: `Available` (free, no claim), `Bound` (allocated to a PVC), `Released` (PVC deleted, awaiting reclaim), `Terminating` (deletion in progress), `Failed` (provisioning error). Delete is blocked when the PV is `Bound` — the PVC must be deleted first to avoid orphaning it.
_Avoid_: PV, disk, volume

**Delete PersistentVolume**:
A write operation that permanently removes a PersistentVolume from the cluster. Only available when the PV status is not `Bound` — attempting to delete a Bound PV is blocked in the UI to prevent orphaning the associated PersistentVolumeClaim. Confirmed via a simple confirmation dialog (no type-to-confirm).
_Avoid_: Remove PV, drop volume

**StorageClass**:
A cluster-scoped Kubernetes resource that defines how PersistentVolumes are dynamically provisioned (provisioner, reclaim policy, binding mode, expansion support). Not namespaced. In GreenCap, displayed read-only under Infrastructure in the Global section.
_Avoid_: SC, storage profile, storage tier

**Node**:
A cluster-scoped Kubernetes resource representing a physical or virtual machine that runs Workloads. Not namespaced. Each Node exposes allocatable CPU and memory (the capacity available for scheduling, after system overhead). Role is derived from labels: a Node with label `node-role.kubernetes.io/control-plane` or `node-role.kubernetes.io/master` is a Control Plane node; all others are Workers. Status is determined by the `Ready` condition: `Ready` (node is healthy and accepting Pods), `NotReady` (node is not accepting Pods), `Unknown` (node controller lost contact). In GreenCap, displayed under the Infrastructure section. Supports one write operation: Cordon/Uncordon.
_Avoid_: Machine, host, server, instance

**Cordon**:
A write operation on a Node that marks it as unschedulable by patching `spec.unschedulable: true`, preventing new Pods from being placed on it without affecting Pods already running. The inverse operation, Uncordon, patches `spec.unschedulable: false` and makes the Node eligible for scheduling again. In GreenCap both are represented by a single toggling button that reflects the current state (Cordon when schedulable, Uncordon when cordoned).
_Avoid_: Pause, disable, drain

**Infrastructure**:
UI section within Global grouping cluster-scoped infrastructure resources. Currently contains PersistentVolumes, StorageClasses, and Nodes. Distinct from Storage (which is namespace-scoped) and Settings (which is GreenCap platform configuration).
_Avoid_: Admin, cluster resources, system

**Project**:
UI section in the sidebar grouping all views and operations scoped to the active Namespace within a Cluster. Contains Topology, Observability (Dashboard, Events, Metrics), Workloads, Networking, Parameters, Auto Scaling, Storage, and Helm. Distinct from Global (cluster-scoped, independent of Namespace) and Settings (GreenCap platform/user configuration).
_Avoid_: Namespace view, Workspace

**Helm**:
UI subsection within Project grouping Helm-related views scoped to the active Namespace. Contains Releases and Repositories. Positioned below Storage in the sidebar. Helm operations are executed via the Helm CLI binary embedded in the GreenCap runtime image, with the active Cluster's Kubeconfig written to a temporary file for each operation and deleted immediately after. Before install or upgrade operations, GreenCap re-adds all HelmRepositories configured for the active Cluster to the Helm CLI environment.
_Avoid_: Package manager, charts section

**HelmRelease**:
A Helm release installed in the active Namespace — a named instance of a chart deployed to the cluster. Carries: name, chart name and version, app version, revision number, status, and installation timestamp. In GreenCap, displayed in a grid under Helm → Releases, scoped to the active Namespace. Selecting a release opens a detail drawer with three tabs: Notes (chart's post-install instructions), Values (user-supplied overrides as YAML), and Manifest (rendered Kubernetes resources as YAML). Supports two write operations: Uninstall and Upgrade.
_Avoid_: Chart, deployment, Helm chart

**HelmRepository**:
A named Helm chart repository configured for a specific Cluster in GreenCap. Carries a name (used as the Helm repo alias, e.g. `bitnami`) and a URL (e.g. `https://charts.bitnami.com/bitnami`). Persisted per Cluster in the GreenCap database. Before any install or upgrade operation, GreenCap re-adds all configured HelmRepositories to the Helm CLI environment (`helm repo add`) and updates them (`helm repo update`). Managed via Helm → Repositories in the sidebar. Supports two operations: Add (registers a new repo) and Remove (deletes it from GreenCap and the Helm CLI environment).
_Avoid_: Helm repo, chart source, registry

**Deploy from Helm**:
A wizard-guided write operation that installs a Helm chart as a new HelmRelease in the active Namespace. One of four deploy modes alongside Deploy Application, Import Compose, and Deploy from Dockerfile, accessible via the mode selector in the New Application section. The wizard runs in three steps: (1) Chart — select a configured HelmRepository, enter chart name and optional version (empty = latest); (2) Config — release name (auto-suggested from chart name) and namespace (pre-filled with active Namespace, editable); (3) Values & Install — optional YAML values override textarea, followed by inline output of `helm install` after confirmation.
_Avoid_: Helm install, chart deploy

**Upgrade (Helm)**:
A write operation on a HelmRelease that updates the release with new values and/or a new chart version (`helm upgrade`). Triggered from Helm → Releases via a SelectionAction. Opens a dialog pre-filled with the release's current user-supplied values (from `helm get values`); the user edits the YAML and optionally specifies a new chart version. On confirmation, executes `helm upgrade <release> <chart> -f <values>`.
_Avoid_: Update release, redeploy, helm update

**Uninstall (Helm)**:
A write operation on a HelmRelease that removes all Kubernetes resources created by the release from the cluster (`helm uninstall`). Requires the user to type the release name in a confirmation dialog before proceeding — the blast radius is equivalent to deleting all resources the chart provisioned. Does not delete PersistentVolumeClaims by default (Helm's standard behavior).
_Avoid_: Delete release, remove chart, helm delete

**Observability**:
UI subsection within Project grouping namespace-scoped monitoring views. Currently contains Dashboard (health overview), Events, and Metrics (PodMetric listing).
_Avoid_: Monitoring, Telemetry

**Global**:
UI section in the sidebar grouping views related to Clusters as a whole rather than to resources inside a Namespace. Currently contains Clusters (the list of registered Clusters), Namespaces, Infrastructure, and Registry. Distinct from Project (scoped to the active Namespace, which includes the Observability subsection), Developer Experience (tooling and learning aids), and Settings (GreenCap platform/user configuration, unrelated to any Cluster).
_Avoid_: Cluster-wide, Admin

**Developer Experience**:
Top-level UI section in the sidebar grouping capabilities aimed at developers studying, building, and experimenting with Kubernetes. Peer to Global, Project, and Settings. Currently contains Kubernetes Operators. In progress: Templates Catalog. Distinct from Global (cluster infrastructure resources) and Project (workloads scoped to a Namespace).
_Avoid_: DevEx, tooling, learning

**Templates Catalog**:
A view in Developer Experience listing every Template available for one-click deployment, sourced from `catalog.json` at the root of an external Git repository curated by the GreenCap team (`greencapk8s/greencap-templates`, public — not user-supplied, unlike the Git repository fields in Deploy from Dockerfile/Import Compose). Fetched via plain HTTP (raw content), with no caching layer and no Git client library — re-fetched on every visit. Displayed as a card list — one card per Template, showing title, description, and technologies involved, plus an "Installed" badge when the Template's Namespace already exists in the active Cluster (see Deploy Template). Distinct from Manifest/Apply (which edits a single existing resource already in the cluster) — the Templates Catalog provisions a full new set of resources from a Template.
_Avoid_: Sample Manifests, Template Gallery, Marketplace

**Template**:
A complete example application definition inside the Git repository backing the Templates Catalog — one directory per Template, containing its own manifest (`template.yaml`) plus the Kubernetes resource files and, when a component needs a custom-built image, its source code and Dockerfile. `template.yaml` lists `resources` (resource files, applied in order, each possibly a multi-document YAML) and optionally `builds` (name, build context path, Dockerfile path, and target image name, for components not available as a ready-made public image). One of the resource files defines the Template's own Namespace — not the Cluster's active Namespace — with a fixed name declared in `catalog.json`, making a Template's installed state a property of the Cluster rather than of the deploying User. See ADR 0015.
_Avoid_: Sample, Blueprint, Stack, Recipe

**Deploy Template**:
A write operation in the Templates Catalog that provisions a Template's resources into the Cluster. Shows a read-only preview of the concatenated resource manifests before confirming — no editing, since Templates are curated, not user-authored. On confirmation: applies the Template's Namespace resource file first; runs a Kaniko Build per entry in `builds`, using the Template's own Git repository as context and pushing to the Cluster's internal Registry (same mechanism as Deploy from Dockerfile/Import Compose, not an external registry); substitutes each built image's sentinel placeholder (`__BUILD__<name>`) in the corresponding resource file with the pushed reference; then applies the remaining resource files via a generic dynamic client (resource types are not known ahead of time, unlike the rest of GreenCap). Aborts on the first conflict (a resource — commonly the Namespace, from a prior deploy of the same Template — already exists) with no rollback of resources already applied, consistent with Import Compose and Deploy from Dockerfile. Disabled from the card once the Template is already Installed in the active Cluster.
_Avoid_: Install template, apply template, run template

**Kubernetes Operator**:
A controller deployed to a Kubernetes cluster that extends the API with custom resources (CRDs) and automates the lifecycle of complex applications — databases, message queues, certificate managers, monitoring stacks, and more. In GreenCap, managed exclusively via OLM (Operator Lifecycle Manager). Displayed in Developer Experience under a dedicated view with two tabs: Installed (lists operators whose `ClusterServiceVersion` is present in the cluster) and Catalog (lists packages available across all `CatalogSource`s for installation). Installation is triggered by creating a `Subscription` via a simple dialog (channel selector, AllNamespaces install mode by default); OLM handles the rest asynchronously. Each installed operator shows a status badge derived from its `ClusterServiceVersion` phase: `Installing`, `Succeeded`, or `Failed`; a `Failed` badge carries a tooltip with the `status.message` from the CSV. If OLM is not installed in the cluster, the view shows an informative empty state. Not to be confused with the `OPERATOR` label used in Flyway migrations to seed legacy user permissions — that is a GreenCap preset, not a Kubernetes resource.
_Avoid_: Cluster Operator, addon, plugin, extension

**Install Operator**:
A write operation in the Catalog tab of the Kubernetes Operators view that provisions a Kubernetes Operator in the active Cluster by creating a `Subscription` (and an `OperatorGroup` if absent) in the `operators` namespace. The user selects a channel from the channels available in the operator's `PackageManifest`; install mode is fixed at `AllNamespaces`.
_Avoid_: Deploy operator, add operator, enable operator

**Uninstall Operator**:
A write operation in the Installed tab of the Kubernetes Operators view that removes a Kubernetes Operator from the active Cluster by deleting its `Subscription` and `ClusterServiceVersion`. Does not delete the CRDs created by the operator — leaving CRD cleanup to the user avoids accidental data loss from custom resources still present in the cluster. Requires the user to type the operator name in a confirmation dialog before proceeding.
_Avoid_: Remove operator, delete operator, disable operator

**Topologia**:
UI view that renders an interactive graph of Kubernetes resources within a Namespace and the relationships between them. Node types: Deployment, ReplicaSet, Pod, Service, PersistentVolumeClaim, Ingress. Edges derived from `ownerReferences` (Deployment→ReplicaSet→Pod), label selector matching (Service→Pod), volume mounts (PodGroup→PersistentVolumeClaim via `spec.volumes[].persistentVolumeClaim.claimName`), and backend service references (Ingress→Service via `spec.rules[].http.paths[].backend.service.name` — only when the target Service exists in the namespace). Isolated nodes (no edges) are shown — they signal misconfiguration. Pods owned by a Job (directly or via a CronJob's Job) are deliberately excluded — they represent finite task executions, not the long-running service topology this view is meant to map. Clicking a node opens a detail panel; the "Go to resource" button navigates to the resource's listing view pre-filtered by name. Pan and zoom are enabled. Optionally renders `TopologyGroup` containers around nodes sharing `app.kubernetes.io/part-of`/`app.kubernetes.io/component` labels, toggleable via a control that is on by default.
_Avoid_: Diagram, map, graph

**TopologyGraph**:
The data transfer object returned by `TopologyService` representing the full graph for a Namespace. Contains a flat list of `TopologyNode` and a flat list of `TopologyEdge`. Built server-side; the frontend only renders what it receives.
_Avoid_: Graph data, node map

**TopologyNode**:
A single resource in the `TopologyGraph`. Carries: a unique `id` (type + name), a display `label` (resource name), a `type` (Deployment, ReplicaSet, Pod, Service, PersistentVolumeClaim, Ingress), a `status` (for badge coloring), and a resource view URL (used by the "Go to resource" button in the detail panel to navigate to the resource's listing view pre-filtered by name). PersistentVolumeClaim nodes additionally carry capacity, access mode, and storage class. Ingress nodes additionally carry hosts, TLS status ("Secure"/"Plain"), and IngressClass.
_Avoid_: Node, vertex, element

**TopologyEdge**:
A directed relationship in the `TopologyGraph` between two `TopologyNode` ids. Direction always flows from owner/controller to owned (Deployment→ReplicaSet→Pod) or from Service to its target Pods.
_Avoid_: Link, connection, arrow

**TopologyGroup**:
A visual container drawn around `TopologyNode`s that share the same `app.kubernetes.io/part-of` and/or `app.kubernetes.io/component` label value, rendered as a Cytoscape compound node. Grouping is nested: an outer group by `part-of` value, with an inner group by `component` value for nodes that also carry it. A node carrying only `component` (no `part-of`) forms its own outer-level group. Nodes without either label are not grouped — they render normally outside any container. Labels for synthetic nodes (PodGroup) are derived from the first Pod in the group; PersistentVolumeClaim nodes use their own metadata labels. Purely visual — groups do not collapse or expand. Toggled on `Topologia` via a control that is on by default; group boxes are labeled `key: value` (e.g. `part-of: payments`).
_Avoid_: Cluster, container, namespace grouping, folder

**PlatformSettings**:
User-scoped preferences that control GreenCap's behavior across sessions. Not related to Kubernetes resources — these are settings about the platform itself. Persisted per User in the database so they follow the user across devices. Currently contains: auto-refresh interval, ranging from "no auto-refresh" up to 1 minute. For a User with no saved preference (new accounts, or accounts that never opened Platform Settings), the default is 3 seconds — chosen for responsiveness given GreenCap's target of small dev/test clusters; the User can change or disable it at any time. Accessed via the Settings menu item in the sidebar Settings section.
_Avoid_: Preferences, config, global settings

**TopologyLayout**:
A persisted snapshot of the visual state of the Topologia view for a specific User + Cluster + Namespace combination. Stores the pixel positions of all currently visible TopologyNodes (as a complete JSON snapshot, not incremental) and the `groupingEnabled` toggle state. Scoped per user+cluster+namespace so each combination has an independent layout. Saved automatically after each node drag (replacing the previous snapshot entirely — stale nodes from prior graph states are dropped on the next save). On load, saved positions are applied to matching nodes; new nodes with no saved position are placed by fcose. On first access (no saved layout), fcose positions all nodes normally.
_Avoid_: Saved graph, layout cache, position state

**PodLog**:
A snapshot of the stdout/stderr output of a container within a Pod, fetched via the Kubernetes API with a configurable line limit (tail). In GreenCap, displayed in a dedicated read-only page (`logs/pod/{namespace}/{name}`) with auto-poll every 3 seconds. Supports two modes: current (active container) and previous (last terminated instance of the container — useful for CrashLoopBackOff diagnosis). When no previous log exists, the page shows an informative message instead of an error.
_Avoid_: Output, stdout, console, terminal

**Registry**:
The container registry running inside a Cluster, exposed as the `Service` named `registry` in the `kube-system` Namespace — the convention used by the minikube `registry` addon. Not a persisted entity: GreenCap reaches it on demand via a Kubernetes API port-forward to that Service, reusing the Cluster's Kubeconfig (no separate credentials). In GreenCap, displayed under the Global section as its own top-level item, scoped to the active Cluster, listing the Registry's Repositories. Supports write operations: Build, Remove Repository, and Remove Tag(s). Shows an empty state when the Service is absent or unreachable, without distinguishing between the two.
_Avoid_: Docker registry, Image registry

**Repository**:
A named collection of image versions stored in a Registry (e.g. `greencap-demo/backend`), returned by the Registry's catalog. In GreenCap, displayed in the Registry view with its Tag count; selecting one navigates to its Tags. Can be permanently removed (Remove Repository), which deletes all of its Tags and triggers the Registry's garbage collection; irreversible.
_Avoid_: Image (ambiguous — may mean a Repository, a Tag, or a `repository:tag` pair)

**Tag**:
A named reference to a specific image version within a Repository (e.g. `latest`, `v1.2.3`). In GreenCap, displayed in a dedicated view per Repository with its `digest` (content hash of the image manifest), `size`, and `created` timestamp. One or more Tags can be permanently removed via multi-selection (Remove Tags); irreversible.
_Avoid_: Version, Image (see Repository)

**Build**:
A write operation on the Registry that builds an image from a Git Repository's source and pushes it to the Cluster's Registry, under a Repository and Tag chosen by the user. Triggered from the Registry view; progress is shown as a live log, similar to PodLog. GreenCap does not persist a history of past Builds — once finished, a Build leaves no trace in GreenCap.
_Avoid_: Deploy, publish

**Git Repository**:
The source of a Build — a publicly accessible Git repository identified by URL and branch, plus a Context path (the subdirectory used as the build's root, defaults to the repository root) and the path to a Dockerfile within that Context. GreenCap does not store or transfer its contents, only the reference; the Build clones it directly. Only public repositories are supported — private repositories would require credentials, not yet supported.
_Avoid_: Source code, project, "repository" alone (ambiguous with Repository, the Registry concept)

**Deploy Application**:
A wizard-guided write operation that creates a new Namespace and provisions the Kubernetes resources needed to run an application from a container image: a Deployment (always), a Service ClusterIP (when a container port is specified), a PersistentVolumeClaim (optional), and an Ingress (optional, requires a port). The image source is a free-text field (`repository:tag`) with suggestions from the active Cluster's internal Registry. Ingress host is auto-suggested as `<namespace>.greencap.local` and editable; IngressClassName is selected from IngressClasses available in the cluster. PVC StorageClass is selected from StorageClasses available in the cluster, with the cluster default pre-selected; access mode is always ReadWriteOnce. On partial failure, GreenCap reports which resources were created and which failed — no rollback is attempted. On success, navigates to the Topologia view of the newly created Namespace. The resulting resources are standard Kubernetes objects with no special GreenCap tracking; they are managed individually through the existing views after creation. One of three deploy modes alongside Import Compose and Deploy from Dockerfile.
_Avoid_: New project, create project, provision application

**Import Compose**:
A wizard-guided write operation that translates a `docker-compose.yml` from a public Git Repository into Kubernetes resources and provisions them in a single Namespace. One of three deploy modes alongside Deploy Application and Deploy from Dockerfile. The wizard runs in three steps: (1) Git Repository URL + branch + optional path to the Compose file + Namespace name — GreenCap fetches and parses the file; (2) review screen showing all resources to be created, editable PVC and Secret fields, warnings for unsupported or ignored directives, and pending Builds for services with `build:`; (3) execution — Kaniko Builds run first (one per service with `build:`, live log), then all Kubernetes resources are created, and a result summary is shown. Translation rules: each Compose `service` becomes a Deployment; `ports:` becomes a ClusterIP Service; `environment:` keys matching `PASSWORD`, `SECRET`, `TOKEN`, `KEY`, or `CREDENTIAL` (case-insensitive) go to a Secret (`<service>-secret`), the rest to a ConfigMap (`<service>-config`); named `volumes:` become PersistentVolumeClaims (`<service>-pvc`, defaulting to 1Gi and the cluster's default StorageClass, editable); bind-mount volumes are ignored with a warning; `depends_on:` is ignored with an informative warning; `build:` triggers a Kaniko Build using the same Git Repository as context, pushing to the Cluster's Registry — image name taken from `image:` if declared alongside `build:`, otherwise defaults to `<service-name>:latest` (editable); `image:` without `build:` is used as-is. All generated resources carry `app.kubernetes.io/part-of: <namespace>` and `app.kubernetes.io/component: <service-name>` labels so they appear grouped in Topologia. On full success, navigates to Topologia of the new Namespace; on partial failure, stays on an inline result screen. No rollback on failure.
_Avoid_: Compose deploy, stack import, docker-compose import

**Deploy from Dockerfile**:
A wizard-guided write operation that builds a container image from a Dockerfile in a public Git Repository via Kaniko, then provisions Kubernetes resources to run that image — combining Build and Deploy Application in a single uninterrupted flow. One of three deploy modes alongside Deploy Application and Import Compose. The wizard runs in six steps: (1) Source + Name — Git Repository URL, branch, Dockerfile path (defaults to `Dockerfile`), context path (defaults to repository root), and application name (used as the Namespace); (2) Image & Port — image tag auto-suggested as `<namespace>/<namespace>:latest` (editable) and the container port the application listens on; (3) Resources — replica count, CPU request/limit, memory request/limit; (4) Volume (optional) — StorageClass, size in Gi, mount path; (5) External Access (optional) — Ingress host (auto-suggested as `<namespace>.greencap.local`) and IngressClass; (6) Review — summary of all resources to be created. On Review confirmation, a Kaniko Build runs first with inline logs (same polling mechanism as Import Compose); on build success the deployment phase provisions a Namespace, Deployment, and optionally Service/PVC/Ingress using the freshly built image (`localhost:5000/<namespace>/<namespace>:<tag>`). On build or deploy failure, an inline error is shown — no rollback. On full success, navigates to Topologia of the new Namespace.
_Avoid_: Build and deploy, Dockerfile deploy, Docker build

