---
title: Fix side panel not opening when clicking group nodes
status: done
sprint: 78
---

## Problem
Tapping a compound group node (the box drawn around resources sharing `part-of`/`component` labels) fires `node-clicked` with missing data fields (`type`, `nodeLabel`, etc.), causing a server-side crash and the drawer never opening. This also prevents the panel from opening for real nodes in the same namespace.

## Fix
Add a guard at the start of the `tap` handler in `topology-graph.ts`:
```typescript
if (node.data('isGroup')) return;
```
Group nodes are visual containers, not Kubernetes resources — clicking them should have no effect.
