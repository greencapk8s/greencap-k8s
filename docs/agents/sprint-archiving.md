# Sprint Archiving

`docs/sprints.md` and `.scratch/` accumulate one entry per sprint indefinitely. To keep both navigable, only the most recent sprints stay in the active files — older ones move to dedicated archives.

## The rule

- **`docs/sprints.md`** — the "Status Geral" table keeps the full history (1 line per sprint, never trimmed). The "Sprints Concluídas" section keeps detailed bullets for the **last 10 sprints only**, in descending order (most recent first).
- **`.scratch/`** — keeps `sprint-N/` directories for the same last 10 sprints. Older directories live under `.scratch/archive/sprint-N/`.

## When to run it

As part of step 6 (Fechamento) of the sprint flow, in the same closing commit:

1. After adding the new sprint's entry to "Sprints Concluídas", check if the section now has more than 10 entries.
2. If so, move the oldest entry (or entries, if more than one sprint was closed since the last run) to `docs/sprints-archive.md`, appended in chronological order (oldest archived sprints come first).
3. Move the corresponding `.scratch/sprint-N/` directory to `.scratch/archive/sprint-N/` via `git mv`, preserving its internal structure (`issues/*.md`).

## docs/sprints-archive.md

A single file, sprints in ascending order. If a sprint has no detailed bullets recorded at the time (gap in `sprints.md`), add an entry noting that and pointing to the "Status Geral" table / git history instead of leaving the sprint out entirely.

## .scratch/archive/

Plain markdown, same structure as the active `.scratch/sprint-N/` directories — still greppable, just out of the active working set.
