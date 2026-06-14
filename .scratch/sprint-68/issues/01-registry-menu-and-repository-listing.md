---
id: "01"
title: "Registry — novo menu Global, RegistryService (port-forward) e listagem de Repositories"
status: done
labels: [feat, backend, frontend]
sprint: 68
---

## Contexto

Decisões definidas via `/grill-with-docs` (ver `CONTEXT.md` — termos `Registry`, `Repository`, `Tag`, `Global`; `docs/adr/0006-registry-via-port-forward.md`):

- `Registry` é uma capacidade **derivada** de um `Cluster` — **não** é uma entidade persistida (sem migration, sem repository, sem formulário de configuração).
- GreenCap alcança o registry via **port-forward da API do Kubernetes** para o `Service` `registry` no Namespace `kube-system`, porta `80` (convenção do addon `registry` do minikube) — reaproveita o `Kubeconfig` do `Cluster`, sem credenciais novas.
- Novo item **top-level** na navegação `Global`, irmão de `Clusters` e `Infrastructure` — "Registry". Nova permission `GLOBAL_REGISTRY_VIEW`.
- Quando o `Service` não existe, ou o port-forward falha, ou o catálogo está vazio: grid vazia + mensagem explicativa única (sem distinguir os casos), sem banner de erro/notificação. Mensagem não deve assumir minikube — apenas descrever o que o GreenCap procura (`Service "registry" no Namespace "kube-system"`).
- Esta issue cobre a listagem de **Repositories** (grid principal). A listagem de **Tags** por Repository é a issue `02`.

## Entrega

### `kubernetes/dto/RepositoryInfo.java`

Novo record:

```java
package io.greencap.k8s.kubernetes.dto;

public record RepositoryInfo(String name, int tagCount) {
}
```

### `kubernetes/RegistryService.java`

Novo `@Service` (`@Slf4j`, `@RequiredArgsConstructor`), seguindo o padrão de `NetworkingService`:

- Constantes: `REGISTRY_NAMESPACE = "kube-system"`, `REGISTRY_SERVICE_NAME = "registry"`, `REGISTRY_SERVICE_PORT = 80`.
- Injeta `KubernetesClientFactory` e `EncryptionService`.
- `public List<RepositoryInfo> listRepositories(Cluster cluster)`:
  - Abre `KubernetesClient` (via `clientFactory.buildClient(encryptionService.decrypt(...))`) e, dentro do mesmo try-with-resources, `LocalPortForward` via `client.services().inNamespace(REGISTRY_NAMESPACE).withName(REGISTRY_SERVICE_NAME).portForward(REGISTRY_SERVICE_PORT)`.
  - Usa `java.net.http.HttpClient` (built-in, sem nova dependência) contra `http://localhost:<portForward.getLocalPort()>`.
  - `GET /v2/_catalog` → lista de nomes de repository (campo `repositories` do JSON).
  - Para cada repository, `GET /v2/<repository>/tags/list` → `tagCount` = tamanho do array `tags` (`0` se `tags` for `null`).
  - **Qualquer exceção** (Service inexistente, timeout de port-forward, erro HTTP) é capturada e logada em `log.warn` (não `log.error` — é um estado esperado, não uma falha operacional) e o método retorna `List.of()`. **Não** relançar `KubernetesOperationException` — diferente das demais operações Fabric8, a ausência do Registry não é um erro de cluster.
- Métodos privados auxiliares para parse do JSON (`_catalog` e `tags/list`) — usar `com.fasterxml.jackson.databind.ObjectMapper` (já é dependência do Spring Boot) para o parse, evitando dependência nova.

### `domain/user/Permission.java`

- Adicionar `GLOBAL_REGISTRY_VIEW` ao enum, na seção `// Global`, junto de `GLOBAL_INFRASTRUCTURE_VIEW`/`GLOBAL_INFRASTRUCTURE_CORDON`.
- Adicionar a `operatorPermissions()` e `viewerPermissions()` (mesmo grupo que `GLOBAL_INFRASTRUCTURE_VIEW`).

### Migration Flyway

Nova migration `V{n}__add_registry_permission.sql` — seguir o padrão das migrations anteriores que adicionaram permissions a usuários existentes (`ADMIN`/`OPERATOR`/`VIEWER` conforme `operatorPermissions()`/`viewerPermissions()`).

### `ui/RegistryView.java`

Nova view, seguindo o padrão de `NodesView` (Global, `BeforeEnterObserver`):

- `@Route(value = "registry", layout = MainLayout.class)`, `@PageTitle("Registry — GreenCap K8s")`, `@PermitAll`.
- Permission: `GLOBAL_REGISTRY_VIEW` (mesmo padrão de `beforeEnter` do `NodesView` — `event.forwardTo("")` se não tiver a permission).
- `Grid<RepositoryInfo>`: coluna `Repository` (`RepositoryInfo::name`, sortable, flexGrow maior) + coluna `Tags` (`RepositoryInfo::tagCount`, largura fixa).
- Sem ações de seleção (sem delete/edit — read-only). Ação "View Tags" (ícone) navega para a rota da issue `02` passando o nome do repository.
- `noClusterMessage` via `UiConstants.buildNoClusterMessage()` (mesmo padrão das demais views Global).
- Estado vazio: quando `allItems` está vazio **e** há cluster ativo, mostrar (em vez da grid, ou como placeholder da grid) o texto: *"No repositories found. Make sure the Service \"registry\" in the \"kube-system\" namespace is available on this Cluster."* — sem `Notification`/erro, já que `RegistryService.listRepositories` nunca lança exceção.
- Filtro de texto por `Repository` (mesmo padrão `TextField` + `HeaderRow` de `NodesView`).
- `HELP_TITLE`/`HELP_TEXT` explicando o conceito `Registry`/`Repository`/`Tag` (resumo do `CONTEXT.md`).

### `ui/MainLayout.java`

- Em `buildGlobalNav()`, adicionar novo `SideNavItem` "Registry" (ícone sugerido: `VaadinIcon.ARCHIVE` ou `VaadinIcon.PACKAGE`) apontando para `RegistryView.class`, condicionado a `SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_VIEW)`, como item irmão de `clustersItem` e do retorno de `buildInfrastructureNavItem()`.
- Adicionar à lista `clusterDependentNavItems` (mesmo tratamento de `Infrastructure`).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Com o addon `registry` habilitado no cluster `greencap-demo` (`minikube addons enable registry -p greencap-demo`) e ao menos uma imagem com tag pushada manualmente (via port-forward + `docker push`, para validação manual), o menu "Registry" lista o repository com a contagem correta de tags.
- Sem o addon habilitado, o menu "Registry" mostra a grid vazia com a mensagem explicativa, sem notificação de erro.
