---
id: "38-05"
title: "UserManagementView: TreeView permission editor"
status: open
labels: [feat, ui]
sprint: 38
---

## Goal

Redesign UserManagementView to support creating and editing users with granular permissions via a TreeView with checkboxes.

## Scope

### Grid changes
- Remove Role column; add a summary column showing permission count (e.g. "14 / 19 permissions")
- Add Edit (pencil icon) button alongside existing Deactivate button

### Create User dialog
- Remove Role selector
- Add TreeView with checkboxes (all unchecked by default)
- Select All / Deselect All buttons above the TreeView
- TreeView structure mirrors the sidebar sections (PROJECT, OBSERVABILITY, SETTINGS)
- Parent nodes are group headers (cascade check/uncheck, indeterminate state when partial)
- Dialog wider to accommodate the TreeView (~560px)

### Edit Permissions dialog
- Same TreeView component, pre-populated with current user permissions
- Select All / Deselect All buttons
- Save persists the updated Set<Permission>

### TreeView structure
```
PROJECT
  Topology
  Workloads
    Deployments
      Scale
      Restart
    ReplicaSets
    Pods
  Networking
    Services
  Parameters
    ConfigMaps
    Secrets
  Auto Scaling
    Horizontal Scaler
      [Write]
  Storage
    Volume Claims
OBSERVABILITY
  Dashboard
  Events
  Metrics
SETTINGS
  Clusters
    [Write]
  Infrastructure
  Users
    [Write]
  Platform Settings
```

## Acceptance criteria

- Creating a user with zero permissions works
- Creating a user with all permissions works
- Editing permissions of existing user persists correctly
- Select All checks every leaf; Deselect All unchecks every leaf
- Parent indeterminate state renders correctly when children are partially checked
