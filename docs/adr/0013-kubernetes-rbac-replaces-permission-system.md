# ADR 0013 — Kubernetes RBAC replaces GreenCap permission system

**Status:** Accepted
**Supersedes:** ADR-0004

GreenCap's internal permission system (`user_permissions` table, `Permission` enum, `SecurityUtils.hasPermission()` guards) is replaced by Kubernetes RBAC. Each non-admin User gets a ServiceAccount in the `greencap-system` namespace of their assigned Cluster, bound to a ClusterRole chosen by the PlatformAdmin. GreenCap uses that ServiceAccount's bearer token for all Kubernetes API calls made on behalf of the User — the API server enforces access and returns 403 for unauthorized operations.

## Why

The internal permission model duplicated access control that Kubernetes already provides. Maintaining two systems in parallel meant every new Kubernetes capability needed a new GreenCap permission, and the permission tree grew to ~50 entries with no clear ceiling. Delegating to K8s RBAC removes this duplication and lets cluster operators use standard Kubernetes tooling (kubectl, audit logs, existing ClusterRoles) to reason about access.

## Consequences

- All `SecurityUtils.hasPermission()` guards and `BeforeEnterObserver` checks are removed from views. The UI shows all features to all authenticated Users; unauthorized actions surface as 403 errors from the Kubernetes API.
- `UserManagementView` becomes admin-only (gated on `username == "admin"`).
- The cluster switcher in the header is hidden for non-admin Users — each User belongs to exactly one Cluster, fixed at creation time.
- `user_permissions` table, `Permission` enum, and `PermissionTreePanel` are deleted.
- User creation provisions a ServiceAccount + ClusterRoleBinding in the target Cluster; deactivation deletes them.

## Alternatives considered

**Keep internal permissions alongside RBAC:** Users would have both a SA token and a permission set. Rejected — two enforcement layers with no source of truth.

**Use SubjectAccessReview to reflect RBAC into the UI:** GreenCap would call `SubjectAccessReview` per verb/resource to show/hide buttons. Rejected for this sprint — complexity without clear UX benefit; a visible 403 is acceptable and more honest about what the system actually allows.
