---
id: "38-02"
title: "Flyway migration: role → user_permissions"
status: done
labels: [feat, database]
sprint: 38
---

## Goal

Migrate existing users from the `role` column to the `user_permissions` join table without data loss.

## Scope

- Create `V{n}__permission_based_access_control.sql`
- Create `user_permissions(user_id, permissions)` table
- Insert permission rows for each existing user based on their current `role`:
  - `ADMIN` → all permissions
  - `OPERATOR` → all except `SETTINGS_USERS_VIEW` and `SETTINGS_USERS_WRITE`
  - `VIEWER` → only `*_VIEW` permissions
- Drop `role` column from `users` table

## Acceptance criteria

- Migration runs cleanly on existing DB
- All existing users retain equivalent access after migration
- `role` column no longer exists in `users` table
