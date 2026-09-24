# Sprint Flow

A sprint delivers a feature, a behavior change or a product bug fix. It lives on one ephemeral `sprint-N` branch and reaches `main` through a single PR: one PR per sprint, not per commit.

A bug fix whose cause and solution are evident skips steps 1–2: the backlog item or the user's report is its spec. Ask the user whether a fix outside the app itself (tooling, CI, `setup.sh`) runs as a sprint.

`docs/sprints.md` is written in Portuguese, so its section names and status values are quoted here verbatim.

## Commits

The sprint is committed as it goes: each commit is a cohesive delivery, pushed right away. Right after the first push (`git push -u origin sprint-N`), open the sprint's draft PR with `gh pr create --draft`, titled `Sprint N: <theme>` and with a body following `.github/pull_request_template.md`. From then on the work has a backup, and CI runs on every push, including while the user validates. Acceptance gates only two things: declaring the sprint done (issues `done`, the ✅ in `docs/sprints.md`) and the merge.

A commit carries only the sprint's work. When `git status` shows changes from before the session, ask the user whether to commit them separately first (recommended) or fold them into the sprint.

## Steps

0. **Branch**: from an up-to-date `main`, `git pull && git checkout -b sprint-N`.
1. **Planning**: invoke the `grilling` skill together with `domain-modeling` to settle the sprint's scope, requirements and constraints. This is what `/grill-with-docs` runs; that wrapper only works when the user types it. Before the first round, verify in the code every factual claim the backlog item makes: backlog prose goes stale, and Sprint 112 settled its scope on one such claim before anyone opened the file.
2. **Issues**: one issue per delivery at `.issue-tracker/sprint-N/issues/NN-slug.md` (format in `docs/agents/issue-tracker.md`), plus the sprint's row in the "Status Geral" table of `docs/sprints.md` as 🔄 Em andamento. An issue describes behavior in prose (scenarios, inputs, expected outputs, constraints) and leaves the how to the implementation: no class snippets, mock setup or prescribed method names. Done when the plan is committed.
3. **Implementation**: implement following `docs/agents/code-standards.md`. Each issue turns `in-progress` when started and is committed once it compiles, with a dated entry under its `## Comments` recording what was built and any reading that departs from the issue's text.
4. **Review**: run `/code-review` against `main`, with `docs/agents/code-standards.md` as the standards and the sprint's issues as the spec (for a bug fix, its backlog item or report). Every finding ends either fixed and committed or reported to the user with the reason it stays.
5. **Manual acceptance**: ask the user to validate the flows in the browser and confirm, then wait. Each adjustment that comes out of acceptance is its own commit. Done only when the user explicitly confirms acceptance; green automated tests are not acceptance.
6. **Tests**: for each delivery, add coverage following the Testing section of `docs/agents/code-standards.md`, run `./gradlew compileTestJava` and commit; the push runs the suite in CI. This step may run while the user validates, and so may `./gradlew test` when a CI failure needs diagnosing locally: the suite is isolated (Testcontainers Postgres, Fabric8 mock client, `MOCK` web environment) and touches neither the dev database, minikube nor port 8080.
7. **Closing**: starts only once the user has explicitly accepted and the PR's `test` check is green.
   1. Tick every acceptance criterion that a green test or the manual acceptance verified, and mark the sprint's issues `done`. A criterion without that evidence is reported to the user, and the reason it stays unticked is recorded under the issue's `## Comments`.
   2. Update `docs/sprints.md`: the sprint's row in "Status Geral" as ✅ Concluído, its entry in "Sprints Concluídas", and every backlog item it delivered deleted outright, not struck through (a still-open bullet under a deleted item moves to a heading of its own).
   3. If "Sprints Concluídas" now holds more than 10 entries, apply `docs/agents/sprint-archiving.md`.
   4. Commit these changes as the closing commit.
8. **Merge**: rewrite the PR body to describe what the sprint delivered, still following the template, then `gh pr ready` and `gh pr merge --auto --merge`. The PR merges once the required checks pass, and the branch is deleted on merge.

## Resuming

A sprint's state lives in git and in its issues, not in the session, so a new session (after a `/clear` or a restart) rebuilds it from there before anything else: `git log main..HEAD` and `git status` for what is committed and pending, `gh pr view` for the draft PR and its checks, and the `Status:` line and `## Comments` of each issue in `.issue-tracker/sprint-N/issues/`. Tell the user which step the sprint is on, then continue from it.
