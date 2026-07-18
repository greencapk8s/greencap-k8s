# Contributing to GreenCap K8s

First off — thank you for taking the time to contribute! 💚

GreenCap is an open, community-friendly project, and contributions of all kinds are
welcome: bug reports, feature ideas, documentation fixes, and code.

By participating, you agree to abide by our [Code of Conduct](CODE_OF_CONDUCT.md).

## Ways to contribute

- 🐛 **Report a bug** — open a [bug report](https://github.com/greencapk8s/greencap-k8s/issues/new/choose)
- 💡 **Suggest a feature** — open a [feature request](https://github.com/greencapk8s/greencap-k8s/issues/new/choose)
- 📖 **Improve the docs** — typos, clarifications, examples
- 🧑‍💻 **Submit code** — see [Submitting a pull request](#submitting-a-pull-request)

For anything non-trivial, please **open an issue to discuss it first** so we can align on
the approach before you invest time.

> ⚠️ Do **not** open public issues for security vulnerabilities — see [SECURITY.md](SECURITY.md).

## Development setup

GreenCap is a Spring Boot + Vaadin monolith. To run it locally you only need **Java 21+**
(the Gradle Wrapper handles Gradle itself).

```bash
docker compose -f docker-compose.dev.yml up -d   # PostgreSQL only
./gradlew bootRun                                 # app at http://localhost:8080
```

Default login: `admin` / `admin`. The dev defaults in `application.yaml` work without a `.env`.

Full details — production Docker Compose build, the demo environment, and registering a
cluster — are in the [developer guide](.dev/README.md).

## Project conventions

The complete coding standard lives in [`CLAUDE.md`](CLAUDE.md). The essentials:

**Language**
- Code, comments, and identifiers are in **English**. Comments explain the *why*, not the *what*.
- Log via `@Slf4j` (`log.info/debug/error`) — never `System.out.println`.

**Code style**
- Use Lombok (`@RequiredArgsConstructor`, `@Getter`, `@Slf4j`, …).
- Services: `@Transactional(readOnly = true)` on the class, `@Transactional` on write methods.
  Wrap Fabric8 calls in try-with-resources and throw `KubernetesOperationException` on API failures.
- Vaadin views orchestrate UI only — no business logic; inject services, never repositories.
- Favor intention-revealing names, small methods (≤ 3 params), early returns, and no magic numbers.

**Database**
- No native SQL — use Spring Data JPA / JPQL.
- Flyway migrations are named `V{n}__{snake_case}.sql`. Never edit an applied migration — add a new one.

**Commits**
- Format: `type: short description` — types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
- **English is preferred; Portuguese is accepted.** Keep each commit focused.

**Branches**
- `develop` is the active development branch — **base your work on `develop` and target your PR at it.**
- `staging` holds release candidates; `main` is the stable line, updated only via release PRs.
  Don't target `main` directly.

**Tests**
- Compile after changes: `./gradlew compileJava` (and `./gradlew compileTestJava` for tests).
- Run the suite before submitting: `./gradlew test`.
- Add coverage where it applies: Vaadin view behavior via Karibu (extend `KaribuTest`);
  service/repository behavior via `PostgresIntegrationTest`.

## Submitting a pull request

1. Fork the repo and create a branch off `develop` (e.g. `feat/topology-legend`).
2. Make your change, following the conventions above.
3. Make sure `./gradlew compileJava` and `./gradlew test` pass.
4. Keep the PR focused, fill in the PR template, and link the related issue (`Closes #123`).
5. For UI changes, include before/after screenshots.
6. Open the PR **against `develop`**.

A maintainer will review it as soon as possible. Thanks again for contributing! 💚

## License

By contributing, you agree that your contributions will be licensed under the project's
[Apache License 2.0](LICENSE).
