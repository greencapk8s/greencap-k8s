# Sprint Flow

A sprint delivers a feature, a behavior change or a product bug fix. It lives on one ephemeral `sprint-N` branch and reaches `main` through a single PR: one PR per sprint, not per commit.

A bug fix whose cause and solution are evident skips steps 1–2: the backlog item or the user's report is its spec. Ask the user whether a fix outside the app itself (tooling, CI, `setup.sh`) runs as a sprint.

## Steps

0. **Branch**: from an up-to-date `main`, `git pull && git checkout -b sprint-N`.
1. **Planning**: remind the user to set effort to high (`/effort high`), then run `/grill-with-docs` to settle the sprint's scope, requirements and constraints.
2. **Issues**: one issue per delivery at `.issue-tracker/sprint-N/issues/NN-slug.md` (format in `docs/agents/issue-tracker.md`), plus the sprint's row in the "Status Geral" table of `docs/sprints.md` as 🔄 Em andamento. An issue describes behavior in prose (scenarios, inputs, expected outputs, constraints) and leaves the how to the implementation: no class snippets, mock setup or prescribed method names.
3. **Implementation**: remind the user to set effort to medium (`/effort medium`), then implement following `docs/agents/code-standards.md`, marking each issue `in-progress`.
4. **Compilation**: `./gradlew compileJava` after each relevant change; report errors immediately.
5. **Manual acceptance**: ask the user to validate the flows in the browser and confirm, then wait. Done only when the user explicitly confirms acceptance; green automated tests are not acceptance.
6. **Tests**: for each delivery, add coverage following the Testing section of `docs/agents/code-standards.md`, then `./gradlew compileTestJava`. This step and `./gradlew test` may run while the user validates: the suite is isolated (Testcontainers Postgres, Fabric8 mock client, `MOCK` web environment) and touches neither the dev database, minikube nor port 8080.
7. **Closing**: starts only once the user has explicitly accepted and `./gradlew test` is green.
   1. If `git status` shows unrelated changes from before the session, ask the user whether to commit them separately first (recommended) or fold them into the closing commit.
   2. Mark the sprint's issues `done`.
   3. Update `docs/sprints.md`: the sprint's row in "Status Geral" as ✅ Concluído, its entry in "Sprints Concluídas", and every backlog item it delivered deleted outright, not struck through (a still-open bullet under a deleted item moves to a heading of its own).
   4. If "Sprints Concluídas" now holds more than 10 entries, apply `docs/agents/sprint-archiving.md`.
   5. Commit everything in one closing commit.
8. **Merge**: `git push -u origin sprint-N`, `gh pr create --fill`, then `gh pr merge --auto --merge`. The PR merges once CI passes, and the branch is deleted on merge.
