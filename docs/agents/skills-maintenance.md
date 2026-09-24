# Skills Maintenance

The skills in `.claude/skills/` are copies of [mattpocock/skills](https://github.com/mattpocock/skills), versioned here and recorded in `skills-lock.json`. Two deviations from upstream are kept by hand:

- `greencap-run` and `greencap-stop` belong to this project and are absent from `skills-lock.json`.
- `code-review`, `to-tickets` and `ask-matt` say `.issue-tracker/` where upstream says `.scratch/`, since this repo's issue tracker lives there (`docs/agents/issue-tracker.md`).

## Updating

1. Run `npx skills@latest update`.
2. Re-apply the `.scratch/` → `.issue-tracker/` replacement in `code-review`, `to-tickets` and `ask-matt`; the update restores upstream's text.
3. Done when `grep -rln '\.scratch' .claude/skills/` lists only files under `setup-matt-pocock-skills/`, whose seed templates keep upstream's path.
