# Code Standards

How Java code is written in this repo, on top of the always-on rules in `CLAUDE.md`. `/code-review` checks its Standards axis against both files.

## Layers

- `ui/` — Vaadin views and `MainLayout`. A view orchestrates the UI and nothing else: business logic lives in services, and data is reached through a service, never a repository.
- `domain/` — one package per aggregate, holding its JPA entity, repository and service.
- `kubernetes/` — stateless services over the Fabric8 client, with their DTOs in `kubernetes/dto/`.
- `config/` — configuration and infrastructure beans: security, encryption, data initialization.

## General

- Lombok for boilerplate: `@RequiredArgsConstructor` (constructor injection), `@Getter`, `@Setter`, `@Slf4j`.
- Logging through `log.info/debug/error` from `@Slf4j`, never `System.out.println`.

## Services

- `@Transactional(readOnly = true)` on the class; `@Transactional` on each write method.
- Every Fabric8 call runs inside try-with-resources, so the client is always closed: `try (KubernetesClient client = clientFactory.buildClient(cluster)) { … }`.
- A Kubernetes API failure is thrown as `KubernetesOperationException`.

## Vaadin views

- Notifications at `Notification.Position.BOTTOM_END`.
- Status badges through the `badge` theme plus a variant: `success`, `error` or `contrast`.
- Forms in a `FormLayout` with `ResponsiveStep("0", 1)`.

### Gotchas

- **`Notification.show()` followed by `forwardTo()` or `rerouteTo()` in the same `beforeEnter()` never reaches the browser.** The server creates the component (a Karibu test finds it), but the reroute restarts navigation before the diff is pushed. Schedule the notification as its own task with `ui.access(() -> …)`, before or after the forward, as `UserManagementView.beforeEnter` does.
- **A custom upload through `StreamReceiver` must name its resource `upload`.** Vaadin recognizes only `VAADIN/dynamic/resource/<n>/<id>/upload` as an internal request, and only that path is exempt from CSRF in `VaadinWebSecurity`; any other name hits Spring's `CsrfFilter` and the POST returns 403. The stock `Upload` component does this internally; `BuildContextPicker` is the custom case.
- **A detached component loses its server and client state.** A wizard that calls `container.removeAll()` on each step fires the children's `onDetach` (and any cleanup in it) and recreates their elements in the browser from scratch. A component that must survive across steps is built once, outside the replaced container, and toggled with `setVisible(false)`, which keeps it in the DOM.

## Database

- Queries through Spring Data JPA or JPQL, never native SQL.
- Flyway migrations named `V{n}__{snake_case_description}.sql`.
- An applied migration is immutable: a schema change is always a new migration. Flyway validates checksums at startup, so editing an applied one breaks every database that already ran it.

## Security

- Kubeconfig is encrypted with `EncryptionService` before it is persisted.
- In production, `GREENCAP_ENCRYPTION_KEY` comes from an environment variable.

## Code quality

### Naming

- Names reveal intent: `findActiveClustersByUser()`, not `getData()`.
- Words spelled out: `kubernetesClient`, not `kc`; `namespace`, not `ns`.
- Booleans read as assertions: `isActive`, `hasConnection`, `isEmpty`.
- Constants in `UPPER_SNAKE_CASE`.

### Methods

- A method does one thing: a name that needs "and" (`validateAndSave`) is two methods.
- At most 3 parameters; beyond that, a request object or DTO.
- No hidden side effects: a `get*` method does not change state.
- A result that can be absent is an `Optional`, never `null`.

### Classes

- Single responsibility: a class has one reason to change.
- At most ~200 lines; beyond that, evaluate extraction.

### Readability

- No magic numbers: `int MAX_REPLICAS = 10`, not `if (replicas > 10)`.
- Complex conditions extracted into methods with descriptive names.
- No double negation: `if (isActive)`, not `if (!isInactive)`.
- Early return to reduce nesting; no `else` after `return`.

### What to avoid

- Dead code: commented-out code and unused methods are removed.
- Duplication: before creating something, check whether an equivalent already exists.
- Over-engineering: no abstraction before the second real occurrence.
- Anemic objects: entities carry behavior, not just getters and setters.

## Testing

- Vaadin views with Karibu, extending `KaribuTest`: permission guards, destructive dialogs (type-to-confirm, selection guard) and form validation.
- New service methods and repository behavior not yet covered, with integration tests extending `PostgresIntegrationTest`.
- `./gradlew compileTestJava` after writing tests.
