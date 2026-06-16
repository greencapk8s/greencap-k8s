---
id: "02"
title: "Registry — view dedicada de Tags por Repository (digest, size, created)"
status: done
labels: [feat, backend, frontend]
sprint: 68
---

## Contexto

Depende da issue `01` (`RegistryService`, `RegistryView`, permission `GLOBAL_REGISTRY_VIEW`). Decisões via `/grill-with-docs` (ver `CONTEXT.md` — termo `Tag`):

- Selecionar um `Repository` na `RegistryView` navega para uma **view dedicada** (não dialog) listando suas `Tags`.
- Cada `Tag` mostra `digest`, `size` e `created`, obtidos via manifest + config blob (2 chamadas HTTP extras por tag — aceitável para o catálogo do demo).
- Nomes de `Repository` podem ter múltiplos segmentos (`/`) — a rota precisa de um parâmetro wildcard.

## Entrega

### `kubernetes/dto/TagInfo.java`

Novo record:

```java
package io.greencap.k8s.kubernetes.dto;

public record TagInfo(String name, String digest, String size, String createdAt) {
}
```

`size` formatado como string legível (ex.: `"12.4 MB"`), mesmo princípio de `NodeInfo::memory`/`NodeInfo::cpu`. `createdAt` formatado como `age`/timestamp legível, mesmo princípio de `NamespaceService.age(...)`.

### `kubernetes/RegistryService.java`

Novo método `public List<TagInfo> listTags(Cluster cluster, String repository)`:

- Mesmo padrão de conexão da issue `01` (KubernetesClient + LocalPortForward em try-with-resources).
- `GET /v2/<repository>/tags/list` → lista de nomes de tag.
- Para cada tag:
  1. `GET /v2/<repository>/manifests/<tag>` com header `Accept: application/vnd.docker.distribution.manifest.v2+json` → `digest` vem do header de resposta `Docker-Content-Digest`; `size` = soma de `config.size` + `layers[].size` do corpo do manifest (sem chamada extra).
  2. `GET /v2/<repository>/blobs/<config.digest>` (digest do campo `config` do manifest) → corpo é o JSON de config da imagem; campo `created` (timestamp ISO 8601) usado para `createdAt`.
- Mesma política de erro da issue `01`: qualquer exceção → `log.warn` + retorna `List.of()` (a view trata como "sem tags" — não deveria ocorrer já que o repository veio do catálogo, mas evita propagar erro).

### `ui/RegistryTagsView.java`

Nova view, seguindo o padrão de rota com parâmetro de `ManifestView`/`PodLogsView`:

- `@Route(value = "registry/:repository*", layout = MainLayout.class)` — `*` para capturar repositories com `/` no nome (ex.: `greencap-demo/backend`).
- `@PageTitle("Registry — GreenCap K8s")`, `@PermitAll`, permission `GLOBAL_REGISTRY_VIEW` (mesmo `beforeEnter` da issue `01`).
- Lê o nome do repository via `event.getRouteParameters()` (concatenando os segmentos do wildcard).
- `Grid<TagInfo>`: colunas `Tag`, `Digest` (truncado/com tooltip — digests são longos, ex. `sha256:abcd...`), `Size`, `Created`.
- Cabeçalho mostrando o nome do `Repository` atual (ex.: `H3` ou breadcrumb) + botão "Back" navegando para `RegistryView`.
- `noClusterMessage` (`UiConstants.buildNoClusterMessage()`) + estado vazio análogo à issue `01` caso `listTags` retorne `List.of()`.

### `ui/RegistryView.java` (ajuste da issue 01)

- Ação "View Tags" (ícone `VaadinIcon.LIST` ou similar) por linha/seleção, navegando para `registry/<repository>` (usar `UI.getCurrent().navigate(...)` com o nome do repository — atenção ao encoding de `/` na URL, usar `RouteParameters`/`getRouteParameters` análogo a `ManifestView`).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- A partir da `RegistryView`, "View Tags" em um repository navega para a view de Tags e lista corretamente nome, digest, size e created de cada tag.
- Repository com `/` no nome (ex.: `greencap-demo/backend`) funciona corretamente na navegação e na rota.
