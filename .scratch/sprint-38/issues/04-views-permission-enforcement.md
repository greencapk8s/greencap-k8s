---
id: "38-04"
title: "Views: replace isViewer() with hasPermission() and disabled pattern"
status: open
labels: [feat, ui]
sprint: 38
---

## Goal

Update all views that currently use `SecurityUtils.isViewer()` to use granular permissions, and change `setVisible(false)` to `setEnabled(false)`.

## Affected files

- `ClustersView`: Add button (`SETTINGS_CLUSTERS_WRITE`), delete action (`SETTINGS_CLUSTERS_WRITE`)
- `DeploymentsView`: Scale and Restart buttons (`WORKLOADS_DEPLOYMENTS_SCALE`, `WORKLOADS_DEPLOYMENTS_RESTART`)
- `HorizontalScalerView`: write actions (`AUTOSCALING_HORIZONTALSCALER_WRITE`)

## Scope

- Replace every `setVisible(!SecurityUtils.isViewer())` with `setEnabled(SecurityUtils.hasPermission(X))`
- Replace every `boolean canWrite = !SecurityUtils.isViewer()` with permission-specific check

## Acceptance criteria

- Buttons without permission are visible but disabled with correct tooltip/style
- Layout does not shift when buttons are disabled vs enabled
