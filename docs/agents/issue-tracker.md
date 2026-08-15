# Issue tracker: Local Markdown

Issues and specs for this repo live as markdown files in `.issue-tracker/`.

## Conventions

- One feature per directory: `.issue-tracker/<feature-slug>/` — in practice, one sprint per directory (`sprint-N`)
- The spec is `.issue-tracker/<feature-slug>/spec.md`
- Implementation issues are one file per ticket at `.issue-tracker/<feature-slug>/issues/<NN>-<slug>.md`, numbered from `01` — never a single combined tickets file
- Triage state is recorded as a `Status:` line near the top of each issue file (see `triage-labels.md` for the role strings)
- Comments and conversation history append to the bottom of the file under a `## Comments` heading

## When a skill says "publish to the issue tracker"

Create a new file under `.issue-tracker/<feature-slug>/` (creating the directory if needed).

## When a skill says "fetch the relevant ticket"

Read the file at the referenced path. The user will normally pass the path or the issue number directly.

## Wayfinding operations

Used by `/wayfinder`. The **map** is a file with one **child** file per ticket.

- **Map**: `.issue-tracker/<effort>/map.md` — the Notes / Decisions-so-far / Fog body.
- **Child ticket**: `.issue-tracker/<effort>/issues/NN-<slug>.md`, numbered from `01`, with the question in the body. A `Type:` line records the ticket type (`research`/`prototype`/`grilling`/`task`); a `Status:` line records `claimed`/`resolved`.
- **Blocking**: a `Blocked by: NN, NN` line near the top. A ticket is unblocked when every file it lists is `resolved`.
- **Frontier**: scan `.issue-tracker/<effort>/issues/` for files that are open, unblocked, and unclaimed; first by number wins.
- **Claim**: set `Status: claimed` and save before any work.
- **Resolve**: append the answer under an `## Answer` heading, set `Status: resolved`, then append a context pointer (gist + link) to the map's Decisions-so-far in `map.md`.

## Archived sprints

`.issue-tracker/sprint-N/` directories for sprints outside the active window are moved to `.issue-tracker/archive/sprint-N/`. They keep the same structure and remain greppable, just out of the active working set. See `docs/agents/sprint-archiving.md`.
