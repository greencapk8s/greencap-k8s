---
id: "01"
title: "Container Registry — Remove Repository"
status: done
labels: [feature]
sprint: 74
---

## Contexto

Demanda do usuário, planejada via `/grill-with-docs`. Decisões registradas em `docs/adr/0008-registry-remove-via-manifest-delete-and-gc.md` e nos termos **Registry**, **Repository** e **Tag** do `CONTEXT.md`:

- Nova permissão `GLOBAL_REGISTRY_DELETE` (separada de `GLOBAL_REGISTRY_BUILD`), seguindo o padrão VIEW/DELETE de outros recursos.
- "Remove Repository" = deletar o manifest de cada digest único das Tags do repositório (`DELETE /v2/<repo>/manifests/<digest>`, idempotente — 404 também é sucesso) + rodar `registry garbage-collect /etc/distribution/config.yml --delete-untagged` no Pod do registry via Fabric8 `pods/exec`, para garantir que o repositório desapareça do catálogo.
- Confirmado em runtime no `greencap-demo`: `REGISTRY_STORAGE_DELETE_ENABLED=true` já está habilitado (imagem `registry:3.0.0`); RBAC `pods/exec` em `kube-system` já é permitido; Pod do registry tem label `actual-registry=true`; binário `/bin/registry`, config em `/etc/distribution/config.yml`.
- Novas operações destrutivas ficam em um novo `RegistryMaintenanceService` (pacote `kubernetes/`), separado do `RegistryService` (320 linhas, já acima do limite de ~200 do guia de convenções; `RegistryService` continua só com leitura + Build).
- "Remove Tags" (multi-seleção na `RegistryTagsView`) é a Issue 02, que reaproveita a permissão e o `RegistryMaintenanceService` criados aqui.

## Entrega

### 1. `domain/user/Permission.java`

- Adicionar `GLOBAL_REGISTRY_DELETE` logo após `GLOBAL_REGISTRY_BUILD`.
- Incluir em `operatorPermissions()` (junto de `GLOBAL_REGISTRY_VIEW`, `GLOBAL_REGISTRY_BUILD`). **Não** incluir em `viewerPermissions()` (write operation).

### 2. `src/main/resources/db/migration/V24__add_registry_delete_permission.sql` (novo)

Mesmo padrão de `V23__add_registry_build_permission.sql`:

```sql
-- Grants GLOBAL_REGISTRY_DELETE to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V23).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_REGISTRY_DELETE'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
```

### 3. `ui/UserManagementView.java`

No grupo "Container Registry" (já um `LinkedHashMap` desde a Sprint 73), adicionar:

```java
put("Container Registry (Delete)", Permission.GLOBAL_REGISTRY_DELETE);
```

### 4. `kubernetes/RegistryMaintenanceService.java` (novo)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryMaintenanceService {

    private static final String REGISTRY_NAMESPACE = "kube-system";
    private static final String REGISTRY_SERVICE_NAME = "registry";
    private static final int REGISTRY_CONTAINER_PORT = 5000;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final String REGISTRY_POD_LABEL = "actual-registry";
    private static final String REGISTRY_CONFIG_PATH = "/etc/distribution/config.yml";
    private static final Duration GARBAGE_COLLECT_TIMEOUT = Duration.ofSeconds(60);

    private final KubernetesClientFactory clientFactory;
    private final EncryptionService encryptionService;
    private final RegistryService registryService;

    public void deleteRepository(Cluster cluster, String repository) {
        List<TagInfo> tags = registryService.listTags(cluster, repository);

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

            runGarbageCollect(client);
        } catch (Exception e) {
            log.error("Failed to remove repository {} on cluster {}: {}", repository, cluster.getName(), e.getMessage());
            throw new KubernetesOperationException("Failed to remove Repository: " + e.getMessage(), e);
        }
    }

    private void deleteManifest(HttpClient httpClient, String baseUrl, String repository, String digest) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/v2/" + repository + "/manifests/" + digest))
                .timeout(HTTP_TIMEOUT)
                .DELETE()
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        // 202 = deleted, 404 = already gone (idempotent — safe to treat as success)
        if (response.statusCode() != 202 && response.statusCode() != 404) {
            throw new IllegalStateException("Unexpected status " + response.statusCode() + " deleting manifest " + digest);
        }
    }

    private void runGarbageCollect(KubernetesClient client) throws Exception {
        String podName = findRegistryPodName(client);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        try (ExecWatch watch = client.pods().inNamespace(REGISTRY_NAMESPACE).withName(podName)
                .writingOutput(stdout)
                .writingError(stderr)
                .exec("/bin/registry", "garbage-collect", REGISTRY_CONFIG_PATH, "--delete-untagged")) {

            Integer exitCode = watch.exitCode().get(GARBAGE_COLLECT_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            log.info("registry garbage-collect on {}: exit={}, output={}", podName, exitCode, stdout);
            if (exitCode == null || exitCode != 0) {
                throw new IllegalStateException("garbage-collect exited with code " + exitCode + ": " + stderr);
            }
        }
    }

    private String findRegistryPodName(KubernetesClient client) {
        return client.pods().inNamespace(REGISTRY_NAMESPACE)
                .withLabel(REGISTRY_POD_LABEL, "true")
                .list().getItems().stream()
                .findFirst()
                .map(pod -> pod.getMetadata().getName())
                .orElseThrow(() -> new IllegalStateException("Registry pod not found (label " + REGISTRY_POD_LABEL + "=true)"));
    }
}
```

Notas de implementação:
- API de exec confirmada no Fabric8 6.13.1 (`Execable.exec(String...)`, `TtyExecOutputErrorable.writingOutput/writingError`, `ExecWatch.exitCode(): CompletableFuture<Integer>`).
- `--delete-untagged` é proposital (ADR 0008) para maximizar a chance de o repositório sumir do catálogo — validar empiricamente no aceite manual; se necessário ajustar a flag, documentar o motivo na issue/ADR.
- Repositório com 0 tags (`tags` vazio): `deleteManifest` não é chamado para nenhum digest, mas o GC ainda roda — útil para "limpar" um repositório que já ficou fantasma.

### 5. `ui/UiConstants.java`

Estender o overload de `buildSectionHeader` que recebe `Grid<T>` + `selectionActions` para também aceitar botões extras (ex: "Build Image"), combinando os dois grupos de botões:

```java
static <T> HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh,
                                                String helpTitle, String helpText,
                                                Grid<T> grid, List<SelectionAction<T>> selectionActions) {
    return buildSectionHeader(title, onRefresh, helpTitle, helpText, grid, selectionActions, List.of());
}

static <T> HorizontalLayout buildSectionHeader(String title, BooleanSupplier onRefresh,
                                                String helpTitle, String helpText,
                                                Grid<T> grid, List<SelectionAction<T>> selectionActions,
                                                List<Button> extraLeadingButtons) {
    List<Button> buttons = new ArrayList<>(buildSelectionButtons(grid, selectionActions));
    buttons.addAll(extraLeadingButtons);
    return buildSectionHeader(title, onRefresh, helpTitle, helpText, buttons);
}
```

(o overload existente de 6 argumentos passa a delegar para o novo de 7 argumentos com `List.of()`; nenhuma outra mudança em `UiConstants`)

### 6. `ui/RegistryView.java`

- Injetar `RegistryMaintenanceService` no construtor.
- `canDelete = SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_DELETE)`.
- Trocar a chamada de `buildSectionHeader` para o novo overload de 7 argumentos:

```java
List<UiConstants.SelectionAction<RepositoryInfo>> selectionActions = List.of(
        UiConstants.SelectionAction.destructive(VaadinIcon.TRASH, "Remove Repository", canDelete, this::openDeleteRepositoryDialog)
);

add(UiConstants.buildSectionHeader("Container Registry", this::loadRepositories, HELP_TITLE, HELP_TEXT,
                grid, selectionActions, buildHeaderButtons()),
        noClusterMessage, emptyRegistryMessage, grid);
```

- Novo método `openDeleteRepositoryDialog(RepositoryInfo repository)`, mesmo padrão de `ConfigMapsView.openDeleteDialog`:

```java
private void openDeleteRepositoryDialog(RepositoryInfo repository) {
    ConfirmDialog dialog = new ConfirmDialog();
    dialog.setHeader("Remove Repository");
    dialog.setText("Removing Repository \"" + repository.name() + "\" will permanently delete all " + repository.tagCount()
            + " tag(s) and run garbage collection on the Registry. This action cannot be undone.");
    dialog.setCancelable(true);
    dialog.setConfirmText("Remove");
    dialog.setConfirmButtonTheme("error primary");
    dialog.addConfirmListener(e -> {
        Cluster cluster = clusterContext.getCluster();
        if (cluster == null) return;
        try {
            registryMaintenanceService.deleteRepository(cluster, repository.name());
            loadRepositories();
            notify("Repository " + repository.name() + " removed", NotificationVariant.LUMO_SUCCESS);
        } catch (KubernetesOperationException ex) {
            notify(ex.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    });
    dialog.open();
}
```

- Atualizar `HELP_TEXT` mencionando que repositórios podem ser removidos (com aviso de irreversibilidade e garbage collection).

## Fora de escopo (Issue 02)

- "Remove Tags" multi-seleção na `RegistryTagsView` (reaproveita `RegistryMaintenanceService` e `GLOBAL_REGISTRY_DELETE`).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` passam.
- Migration `V24` aplica sem erro.
- Aceite manual no `greencap-demo`:
  1. Usuário ADMIN/OPERATOR vê o botão "Remove Repository" (ícone TRASH) no header de Container Registry quando um repositório está selecionado; usuário VIEWER não vê (treeview reflete `GLOBAL_REGISTRY_DELETE`).
  2. Criar um repositório/tag descartável (via Build, ex: `sprint74-test/sample:v1`), selecioná-lo, clicar em "Remove Repository" → diálogo mostra nome + contagem de tags + aviso de GC.
  3. Confirmar → repositório desaparece da listagem de Container Registry (valida que `garbage-collect --delete-untagged` remove a entrada do catálogo). Se o repositório persistir com "0 tags", investigar e documentar o achado (ajustar flags do GC ou registrar caveat no backlog, conforme ADR 0008).
  4. Repositórios existentes do demo (`unifametro/flask-app`, `greencap-demo/*`, etc.) não são afetados pelo teste.
