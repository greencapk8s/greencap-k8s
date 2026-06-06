# ADR 0004 — Permission-based access control replacing Role

**Status:** Accepted

## Context

GreenCap started with a coarse three-tier Role model (VIEWER / OPERATOR / ADMIN) stored as a single enum column on the User entity. Access control in views was enforced via `SecurityUtils.isViewer()` / `isAdmin()`, which mapped to Spring Security authorities loaded from that column.

This model could not express per-user customisation: every OPERATOR had identical access, making it impossible to grant a user Deployment Scale without also giving them Cluster write access, for example.

## Decision

Replace the `role` column with a `Set<Permission>` stored via `@ElementCollection` in a `user_permissions` join table. Each `Permission` represents a named capability at one of two granularities:

- **View-level** — can the user navigate to this section? (e.g. `WORKLOADS_DEPLOYMENTS_VIEW`)
- **Action-level** — can the user perform a write operation within a section? (e.g. `WORKLOADS_DEPLOYMENTS_SCALE`, `WORKLOADS_DEPLOYMENTS_RESTART`)

Permissions are loaded as `SimpleGrantedAuthority` objects in `UserDetailsService.loadUserByUsername`, so Spring Security's `hasAuthority()` and `@PreAuthorize` remain the enforcement layer. `SecurityUtils` exposes a single `hasPermission(Permission)` method.

The UI for managing permissions is a TreeView with checkboxes (view nodes as group headers, action nodes as leaves), plus Select All / Deselect All shortcuts. New users start with no permissions; an Admin grants them explicitly.

ADMIN / OPERATOR / VIEWER survive only as Flyway migration labels: a one-time SQL block converts each existing user's `role` value to the equivalent Permission set before dropping the column.

## Alternatives considered

**Keep Role as a preset selector in the UI:** a dropdown would pre-populate the TreeView when creating a user. Rejected by the user — the TreeView alone is sufficient and the preset adds noise.

**Application-layer-only enforcement (no Spring Security authorities):** all users would carry `ROLE_USER` and views would use `BeforeEnterObserver` for access control. Rejected — it leaves direct URL access unprotected at the framework level.

## Consequences

- The `role` column is dropped; existing data is migrated automatically.
- `UserService.createUser` signature changes: accepts `Set<Permission>` instead of `Role`.
- All `isViewer()` / `isAdmin()` call sites are replaced by `hasPermission(Permission.X)` checks.
- Components the user lacks permission for are rendered **disabled**, not hidden, to preserve layout stability.
