# ADR 0005 — Manifest "Apply" as a full replace, not a merge patch

**Status:** Accepted

## Context

The Manifest view (`/yaml/:resourceType/:namespace/:name`) becomes editable: an Edit toggle makes the displayed YAML editable, and an Apply action submits it back to the cluster.

The fetched YAML (`Serialization.asYaml`) includes server-managed fields — `metadata.resourceVersion`, `uid`, `creationTimestamp`, `generation`, `managedFields`, and `status`. For resource types whose `status` is reconciled continuously by controllers (Deployment, Pod, Job, ReplicaSet, HorizontalScaler...), `resourceVersion` advances every few seconds even with no `spec` changes. If Apply submitted that `resourceVersion` verbatim, Fabric8's `update()` would perform optimistic locking against a value that is almost always stale, causing near-constant `409 Conflict` errors unrelated to any real edit conflict.

## Decision

Apply performs a full replace (PUT) of the resource via `client.resource(editedYaml).inNamespace(namespace).update()`. Before submitting, GreenCap strips `resourceVersion`, `uid`, `creationTimestamp`, `generation`, `managedFields`, and `status` from the parsed object. Without a `resourceVersion`, Fabric8's `update()` fetches the latest value from the server before the PUT, so the replace targets current state — last-write-wins on `spec`/`metadata`, unaffected by `status` churn.

This makes GreenCap's "Apply" semantically closer to `kubectl replace`/`kubectl edit` (full replace of the object) than to `kubectl apply` (three-way merge that preserves fields owned by other appliers via `managedFields`). A field present in the live object but absent from the edited YAML — e.g., an annotation added by another controller after the page loaded — is removed by Apply, since a full replace overwrites the whole object.

## Alternatives considered

**Submit the YAML as-is, including `resourceVersion`:** simplest, but Apply would fail with 409 almost immediately for any resource type with actively-reconciled `status`, making the feature unusable for most Workloads.

**Three-way merge patch (`kubectl apply` semantics):** would avoid clobbering fields added by other actors, but requires tracking a "last applied configuration" per resource (or computing a merge patch against live state) — significant added complexity for a first iteration, and a different mental model than "edit this YAML and save it".

## Consequences

- Apply can silently drop fields that were added to the live resource out-of-band between page load and submission (e.g., labels/annotations set by another controller or user).
- Apply is rejected before submission if the edited `kind`, `metadata.name`, or `metadata.namespace` no longer match the page's resource — otherwise `client.resource(yaml)` would target a different object than the one displayed.
- If a real concurrent edit conflict occurs (someone else changes `spec` between fetch and Apply), the last Apply wins silently — there is no conflict warning to the user.
