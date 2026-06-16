---
title: Add Reset Positions button to Topology view
status: done
sprint: 78
---

## Problem
If positions are saved while nodes are stacked at (0,0), the layout always restores those bad coordinates. No way for the user to clear them without database access.

## Fix
Add a "Reset Positions" button in the topology toolbar. On click:
1. Deletes the `TopologyLayout` record for current user + cluster + namespace
2. Navigates to the same route — beforeEnter reloads with no saved positions, triggering `randomize: true`
