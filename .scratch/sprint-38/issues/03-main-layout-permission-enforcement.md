---
id: "38-03"
title: "MainLayout: enforce permissions on nav items and actions"
status: open
labels: [feat, ui]
sprint: 38
---

## Goal

Replace role-based visibility logic in MainLayout with permission-based disabled/enabled state.

## Scope

- Replace `if (SecurityUtils.isAdmin())` guard on Users nav item with `hasPermission(SETTINGS_USERS_VIEW)`
- Nav items without permission: rendered but `disabled` (not removed from DOM)
- Replace any `setVisible(false)` patterns driven by role with `setEnabled(false)` driven by permission
- Update `@RolesAllowed("ADMIN")` on `UserManagementView` to `@PreAuthorize("hasAuthority('SETTINGS_USERS_VIEW')")`

## Acceptance criteria

- Users nav item is disabled (not hidden) for users without SETTINGS_USERS_VIEW
- All layout sections preserve their shape regardless of permissions
- Navigating directly to `/users` without permission redirects to login/access-denied
