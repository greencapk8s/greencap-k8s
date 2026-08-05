---
id: "02"
title: "Container Registry — Remove Tags (multi-seleção)"
status: done
labels: [feature]
sprint: 74
---

## Contexto

Depende da Issue 01 (`GLOBAL_REGISTRY_DELETE`, `RegistryMaintenanceService`). Decisões registradas em `docs/adr/0008-registry-remove-via-manifest-delete-and-gc.md` e no termo **Tag** do `CONTEXT.md`:

- "Remove Tags" = deletar o manifest de cada digest único entre as Tags selecionadas (`DELETE /v2/<repo>/manifests/<digest>`, idempotente — 404 também é sucesso, cobre o caso de duas tags selecionadas apontarem para o mesmo digest).
- **Sem garbage-collect** — operação leve; storage órfão das tags removidas só é reclamado na próxima execução de "Remove Repository" (Issue 01).
- Multi-seleção implementada localmente na `RegistryTagsView` (primeira ocorrência desse padrão no codebase) — sem alterações em `UiConstants` além das já feitas na Issue 01.

## Entrega

### 1. `kubernetes/RegistryMaintenanceService.java`

Adicionar `deleteTags`, reaproveitando o helper privado `deleteManifest` já criado na Issue 01:

```java
public void deleteTags(Cluster cluster, String repository, List<TagInfo> tags) {
    try (KubernetesClient client = clientFactory.buildClient(
            encryptionService.decrypt(cluster.getKubeconfigContent()));
         LocalPortForward portForward = client.services()
                 .inNamespace(REGISTRY_NAMESPACE)
                 .withName(REGISTRY_SERVICE_NAME)
                 .portForward(REGISTRY_CONTAINER_PORT)) {

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();
        String baseUrl = "http://localhost:" + portForward.getLocalPort();

        Set<String> digests = tags.stream().map(TagInfo::digest).collect(Collectors.toSet());
        for (String digest : digests) {
            deleteManifest(httpClient, baseUrl, repository, digest);
        }
    } catch (Exception e) {
        log.error("Failed to remove tags from repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
        throw new KubernetesOperationException("Failed to remove Tags: " + e.getMessage(), e);
    }
}
```

(considerar extrair a abertura do `KubernetesClient`/`LocalPortForward`/`HttpClient` para um helper privado comum entre `deleteRepository` e `deleteTags`, já que passa a se repetir)

### 2. `ui/RegistryTagsView.java`

- Injetar `RegistryMaintenanceService` no construtor.
- `canDelete = SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_DELETE)`.
- Se `canDelete`: `grid.setSelectionMode(Grid.SelectionMode.MULTI)` (sem isso, grid permanece sem seleção como hoje — não há motivo para mostrar checkboxes a quem não pode remover).
- Novo botão "Remove Tags" no `buildHeader()` (mesmo estilo dos botões existentes — `VaadinIcon.TRASH`, `ButtonVariant.LUMO_TERTIARY` + `LUMO_ICON` + `LUMO_ERROR`, `title="Remove Tags"`), visível apenas se `canDelete`, inicialmente `setEnabled(false)`.
- Listener de seleção habilita/desabilita o botão:

```java
grid.asMultiSelect().addValueChangeListener(e -> removeTagsBtn.setEnabled(!e.getValue().isEmpty()));
```

- Novo método `openDeleteTagsDialog(Set<TagInfo> selected)`:

```java
private void openDeleteTagsDialog(Set<TagInfo> selected) {
    String tagNames = selected.stream().map(TagInfo::name).sorted().collect(Collectors.joining(", "));

    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Remove Tags");
    dialog.setText("Removing " + selected.size() + " tag(s) from \"" + repository + "\" (" + tagNames
            + ") is permanent and cannot be undone.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Remove");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(e -> {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            registryMaintenanceService.deleteTags(cluster, repository, List.copyOf(selected));
            loadTags();
            notify(selected.size() + " tag(s) removed", NotificationVariant.LUMO_SUCCESS);
        } catch (KubernetesOperationException ex) {
            notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    });
    dialog.open();
}
```

- Atualizar `HELP_TEXT` mencionando a remoção de Tags via multi-seleção (irreversível, sem liberar storage imediatamente — ver Registry/Repository no `CONTEXT.md`).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` passam.
- Aceite manual no `greencap-demo`:
  1. Usuário ADMIN/OPERATOR vê checkboxes de seleção múltipla na grid de Tags e o botão "Remove Tags" (desabilitado sem seleção); usuário VIEWER não vê nem checkboxes nem o botão.
  2. Selecionar 2+ tags de um repositório descartável (ex: criado na Issue 01) → botão habilita mostrando a ação disponível; diálogo lista os nomes das tags selecionadas.
  3. Confirmar → tags somem da grid (`/v2/<repo>/tags/list` não as retorna mais).
  4. Caso existam duas tags apontando para o mesmo digest, selecionar ambas e remover → sem erro (segundo DELETE recebe 404, tratado como sucesso).
  5. Selecionar todas as tags de um repositório e remover → repositório permanece em Container Registry com "0 tags" até uma execução de "Remove Repository" (Issue 01) — comportamento esperado (ADR 0008), não é bug.
