---
title: Add Cluster dialog — UX improvements (provider list, OpenShift warning, copyable command)
status: done
sprint: 80
---

## Problem
The Add Cluster dialog has three UX gaps:
1. `ClusterProvider.Kubernetes` is semantically wrong — the only supported local provider is Minikube (Docker driver). The enum value misleads users into thinking any generic Kubernetes cluster is the target.
2. Selecting OpenShift gives no feedback that it is not yet supported.
3. The kubeconfig placeholder mentions `kubectl config view --flatten --minify` but it is not copyable — users have to type it manually.

## Expected

### 1 — ClusterProvider rename
- Rename enum value `Kubernetes` → `MinikubeDocker`, displayed as `"Minikube (Docker)"` in the Select via `ItemLabelGenerator`.
- Flyway migration `V{n}__rename_provider_kubernetes_to_minikube_docker.sql`:
  - Drop CHECK constraint on `clusters.provider`
  - UPDATE existing rows: `'Kubernetes'` → `'MinikubeDocker'`
  - Re-add CHECK constraint with new values `('MinikubeDocker', 'OpenShift')`
- Default value in the dialog Select updated to `ClusterProvider.MinikubeDocker`.

### 2 — OpenShift "coming soon"
- When the user selects `OpenShift` in the provider Select, show an inline warning message below the field: `"OpenShift support is coming in a future release."`.
- The Save button is disabled while OpenShift is selected.
- Selecting any other provider hides the warning and re-enables Save.

### 3 — Copyable kubectl command
- Below the kubeconfig TextArea, add a styled code block (dark background) displaying `kubectl config view --flatten --minify`.
- The block has a copy icon button on the right that copies the command to the clipboard via `UI.getCurrent().getPage().executeJs(...)`.
- The TextArea placeholder is simplified to reference the block below rather than repeating the command inline.
