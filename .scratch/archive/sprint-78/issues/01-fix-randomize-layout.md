---
title: Fix nodes stacking on first render
status: done
sprint: 78
---

## Problem
On the first visit to a namespace (no saved positions), all nodes start at (0,0) because `randomize: false` gives fcose no spread to work from. The layout fails to separate nodes visually.

## Fix
Make `randomize` dynamic: `true` when `positionMap` is empty (no saved positions), `false` otherwise. This allows fcose to start from random positions on first render and converge to a good layout, while still respecting saved positions on subsequent visits.

## Side effect
Fixes positions not being saved: once nodes are spread, the user can drag them, which fires `dragfree` and persists the layout.
