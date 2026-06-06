---
id: "38-01"
title: "Permission enum + refactor User entity"
status: open
labels: [feat, backend]
sprint: 38
---

## Goal

Replace the `role` field on `User` with a `Set<Permission>` that represents the user's explicit capabilities.

## Scope

- Create `Permission` enum in `domain/user/` with all view-level and action-level entries
- Remove `Role` enum and `role` field from `User` entity
- Add `@ElementCollection` `Set<Permission> permissions` to `User`
- Update `UserService.createUser` to accept `Set<Permission>` instead of `Role`
- Update `UserService.loadUserByUsername` to load permissions as `SimpleGrantedAuthority`
- Replace `SecurityUtils.isAdmin()` / `isViewer()` with `SecurityUtils.hasPermission(Permission)`
- Update `DataInitializer` to use permissions instead of roles

## Permission entries

View-level:
- `TOPOLOGY_VIEW`
- `WORKLOADS_DEPLOYMENTS_VIEW`, `WORKLOADS_REPLICASETS_VIEW`, `WORKLOADS_PODS_VIEW`
- `NETWORKING_SERVICES_VIEW`
- `PARAMETERS_CONFIGMAPS_VIEW`, `PARAMETERS_SECRETS_VIEW`
- `AUTOSCALING_HORIZONTALSCALER_VIEW`
- `STORAGE_PVC_VIEW`
- `OBSERVABILITY_DASHBOARD_VIEW`, `OBSERVABILITY_EVENTS_VIEW`, `OBSERVABILITY_METRICS_VIEW`
- `SETTINGS_CLUSTERS_VIEW`, `SETTINGS_INFRASTRUCTURE_VIEW`, `SETTINGS_USERS_VIEW`, `SETTINGS_PLATFORM_VIEW`

Action-level:
- `WORKLOADS_DEPLOYMENTS_SCALE`, `WORKLOADS_DEPLOYMENTS_RESTART`
- `AUTOSCALING_HORIZONTALSCALER_WRITE`
- `SETTINGS_CLUSTERS_WRITE`
- `SETTINGS_USERS_WRITE`

## Acceptance criteria

- `Role.java` deleted
- `User.role` column removed (via Flyway)
- `user_permissions` join table exists in DB
- `loadUserByUsername` returns permissions as authorities
- `SecurityUtils.hasPermission(Permission)` works correctly
