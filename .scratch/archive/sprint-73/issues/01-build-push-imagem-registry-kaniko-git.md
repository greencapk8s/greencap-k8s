---
id: "01"
title: "Container Registry — Build & push de imagem via Kaniko a partir de Git Repository público"
status: done
labels: [feature]
sprint: 73
---

## Contexto

Demanda do backlog (follow-up da Sprint 68), planejada via `/grill-with-docs`. Decisões registradas em `docs/adr/0007-build-via-kaniko-job-git-context.md` e nos termos **Build** e **Git Repository** do `CONTEXT.md`:

- Build = `Job` Kaniko criado pelo GreenCap via Fabric8 no Namespace `greencap-system` (criado sob demanda), mesmo padrão de criação de Job do `WorkloadService.triggerCronJob`.
- Contexto de build = Repositório Git **público**, via suporte nativo do Kaniko a contexto Git (`--context=git+https://...#refs/heads/<branch>`). Sem upload de arquivo, sem ConfigMap/PVC.
- Destino = Registry interno do Cluster ativo (`registry.kube-system.svc.cluster.local:5000`, ADR 0006), alcançável via DNS interno do cluster a partir do Job — sem port-forward.
- Job efêmero (`ttlSecondsAfterFinished`), sem histórico de builds persistido.
- Repositórios privados: fora de escopo — registrar em `docs/sprints.md` (backlog) como follow-up no fechamento da sprint.

## Entrega

### 1. `kubernetes/dto/BuildRequest.java` (novo)

```java
public record BuildRequest(String gitRepositoryUrl, String branch, String dockerfilePath, String repository, String tag) {}
```

Valores brutos do formulário (branch/dockerfilePath podem vir `null`/blank — defaults aplicados no `RegistryService`).

### 2. `kubernetes/dto/BuildProgress.java` (novo)

```java
public record BuildProgress(String podName, String status) {}
```

`status` ∈ `"Running" | "Complete" | "Failed"` (mesmos valores de `WorkloadService.deriveJobStatus`, sem `"Suspended"` — Build nunca é suspenso). `podName` é `null` enquanto o Pod do Job ainda não foi agendado.

### 3. `kubernetes/RegistryService.java`

Novas constantes:
- `BUILD_NAMESPACE = "greencap-system"`
- `REGISTRY_INTERNAL_HOST = "registry.kube-system.svc.cluster.local:5000"` (DNS interno do cluster — distinto do `REGISTRY_SERVICE_NAME`/port-forward usado para leitura)
- `KANIKO_IMAGE = "gcr.io/kaniko-project/executor:v1.23.2"` (conferir se há tag estável mais recente no momento da implementação)
- `BUILD_JOB_TTL_SECONDS = 600`
- `DEFAULT_BRANCH = "main"`, `DEFAULT_DOCKERFILE_PATH = "Dockerfile"`

Novos métodos públicos:

```java
public String startBuild(Cluster cluster, BuildRequest request)
```
- Abre `KubernetesClient` (try-with-resources, mesmo padrão dos demais métodos).
- `ensureBuildNamespaceExists(client)`: `client.namespaces().withName(BUILD_NAMESPACE).get() == null` → cria via `NamespaceBuilder` (`createOrReplace`/`create`).
- Gera `jobName = "kaniko-build-" + (System.currentTimeMillis() / 1000)` (mesmo padrão de `<cronjob>-manual-<epoch>`).
- Monta os args do container Kaniko:
  - `--dockerfile=<dockerfilePath ou DEFAULT_DOCKERFILE_PATH>`
  - `--context=<buildGitContext(gitRepositoryUrl, branch)>`
  - `--destination=<REGISTRY_INTERNAL_HOST>/<repository>:<tag>`
  - `--insecure` (registry interno sem TLS)
- Cria o `Job` (`JobBuilder`): `backoffLimit(0)`, `ttlSecondsAfterFinished(BUILD_JOB_TTL_SECONDS)`, `restartPolicy: Never`, container `kaniko` com a imagem/args acima e `resources` (requests `cpu=250m`/`memory=256Mi`, limits `cpu=1`/`memory=1Gi` — adequado aos nodes de 2 CPU/2GB do `greencap-demo`).
- `client.batch().v1().jobs().inNamespace(BUILD_NAMESPACE).resource(job).create()`.
- Loga `log.info` com cluster, contexto git e destino. Em erro, `KubernetesOperationException("Failed to start Build: " + e.getMessage(), e)`.
- Retorna `jobName`.

```java
public BuildProgress getBuildProgress(Cluster cluster, String jobName)
```
- Busca o `Job` em `BUILD_NAMESPACE`; se `null`, `KubernetesOperationException("Build job not found: " + jobName, null)`.
- Deriva `status` a partir de `job.getStatus().getConditions()` (mesma lógica de `deriveJobStatus`, sem ramo `Suspended`).
- Busca o Pod do Job via `client.pods().inNamespace(BUILD_NAMESPACE).withLabel("job-name", jobName).list()` — primeiro item (ou `null` se a lista estiver vazia).
- Retorna `new BuildProgress(podName, status)`.

Helpers privados (pure functions — extrair para testabilidade unitária):

```java
private String buildGitContext(String repositoryUrl, String branch) {
    String url = repositoryUrl.trim();
    if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
    if (!url.endsWith(".git")) url = url + ".git";
    String ref = (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch.trim();
    return "git+" + url + "#refs/heads/" + ref;
}

private String buildDestination(String repository, String tag) {
    return REGISTRY_INTERNAL_HOST + "/" + repository.trim() + ":" + tag.trim();
}

private String resolveDockerfilePath(String dockerfilePath) {
    return (dockerfilePath == null || dockerfilePath.isBlank()) ? DEFAULT_DOCKERFILE_PATH : dockerfilePath.trim();
}
```

### 4. `domain/user/Permission.java`

- Adicionar `GLOBAL_REGISTRY_BUILD` logo após `GLOBAL_REGISTRY_VIEW`.
- Incluir em `operatorPermissions()` (junto de `GLOBAL_REGISTRY_VIEW`). **Não** incluir em `viewerPermissions()` (write operation).

### 5. `src/main/resources/db/migration/V23__add_registry_build_permission.sql` (novo)

Seguir o padrão de `V20__add_statefulset_permissions.sql` (write permission por proxy de `GLOBAL_CLUSTERS_WRITE` = ADMIN/OPERATOR):

```sql
-- Grants GLOBAL_REGISTRY_BUILD to ADMIN and OPERATOR users
-- (identified by GLOBAL_CLUSTERS_WRITE, same pattern as V20).
INSERT INTO user_permissions (user_id, permissions)
SELECT user_id, 'GLOBAL_REGISTRY_BUILD'
FROM user_permissions
WHERE permissions = 'GLOBAL_CLUSTERS_WRITE'
ON CONFLICT DO NOTHING;
```

### 6. `ui/UserManagementView.java`

Trocar o grupo "Container Registry" (atualmente `Map.of("Container Registry", GLOBAL_REGISTRY_VIEW)`) para `LinkedHashMap`, mesmo padrão do grupo "Users":

```java
groups.add(buildGroup("Container Registry", new LinkedHashMap<>() {{
        put("Container Registry (View)", Permission.GLOBAL_REGISTRY_VIEW);
        put("Container Registry (Build)", Permission.GLOBAL_REGISTRY_BUILD);
}}, initial));
```

### 7. `ui/UiConstants.java`

Remover o `private` do overload `buildSectionHeader(String, BooleanSupplier, String, String, List<Button>)` (linha ~86) — torna-se package-private, reutilizável por `RegistryView` para adicionar o botão "Build Image" ao header sem duplicar a estrutura H3 + refresh + help. Nenhuma outra mudança nessa classe.

### 8. `ui/RegistryView.java`

- Novo botão `Build Image` (`VaadinIcon.HAMMER`, `ButtonVariant.LUMO_PRIMARY`), visível apenas com `SecurityUtils.hasPermission(Permission.GLOBAL_REGISTRY_BUILD)`, passado como `leadingButtons` para `UiConstants.buildSectionHeader(...)`.
- `openBuildDialog()`: `Dialog` com `FormLayout` (`ResponsiveStep("0", 1)`):
  - `TextField "Git Repository URL"` — obrigatório, placeholder `https://github.com/usuario/repo`.
  - `TextField "Branch"` — opcional, placeholder `main`.
  - `TextField "Dockerfile path"` — opcional, placeholder `Dockerfile`.
  - `TextField "Repository"` (destino no Registry) — obrigatório, placeholder `meu-grupo/minha-app`.
  - `TextField "Tag"` — obrigatório, placeholder `latest`.
- Validação no clique de "Build" (mesmo padrão de `nameField.isEmpty()` em `ClustersView`):
  - Git Repository URL: obrigatório, regex `^https?://\S+$`.
  - Repository: obrigatório, regex `^[a-z0-9]+((\.|_|__|-+)[a-z0-9]+)*(/[a-z0-9]+((\.|_|__|-+)[a-z0-9]+)*)*$`.
  - Tag: obrigatório, regex `^[a-zA-Z0-9_][a-zA-Z0-9._-]{0,127}$`.
  - Branch/Dockerfile path: sem validação de formato — em branco, `RegistryService` aplica defaults; erros (branch/arquivo inexistente) aparecem no log do build.
- Ao confirmar: `registryService.startBuild(cluster, new BuildRequest(...))` → fecha o diálogo → `UI.getCurrent().navigate("registry/build/" + jobName)`. Em `KubernetesOperationException`, `Notification` (`BOTTOM_END`, `LUMO_ERROR`).

### 9. `ui/BuildLogsView.java` (novo)

Rota `registry/build/:jobName`, mesmo esqueleto de `PodLogsView` mas simplificado (sem container select / tail select / previous checkbox — Job de container único e efêmero):

- Header: botão "Back" (→ `RegistryView`), título `"Build / " + jobName`, badge de status (`badge` theme + variante `success`/`error`/`contrast` para `Complete`/`Failed`/`Running`, conforme convenção de badges do projeto), botão Pause/Resume.
- `Pre logContent` com o mesmo styling de `PodLogsView`.
- Polling fixo de 3s via `ScheduledExecutorService` (mesmo padrão `Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())`, shutdown em `onDetach`).
- A cada tick: `registryService.getBuildProgress(cluster, jobName)` →
  - Atualiza badge conforme `status`.
  - Se `podName != null`: `observabilityService.fetchPodLogs(cluster, "greencap-system", podName, "kaniko", 1000, false)` → `logContent.setText(...)` + scroll to bottom.
  - Se `podName == null`: mantém mensagem "Waiting for build pod to start...".
  - Se `status != "Running"`: `stopPolling()` (build terminou — última leitura de log já capturada neste tick).
- `KubernetesOperationException` (ex: job não encontrado) → `Notification` de erro + `logContent.setText(message)`.
- `@Route` requer `Permission.GLOBAL_REGISTRY_BUILD` (mesmo padrão `forwardTo("")` se ausente).

### 10. Testes automatizados — `src/test/java/io/greencap/k8s/kubernetes/RegistryServiceTest.java` (novo)

Unit tests (sem `KubernetesClient`/mock — funções puras) para os helpers `buildGitContext`, `buildDestination` e `resolveDockerfilePath` (tornar package-private para teste):
- URL com/sem `.git`, com/sem `/` final → contexto normalizado corretamente.
- Branch em branco → `refs/heads/main`.
- Dockerfile path em branco → `Dockerfile`.
- `buildDestination` monta `registry.kube-system.svc.cluster.local:5000/<repo>:<tag>`.

## Fora de escopo (registrar no backlog no fechamento)

- Repositórios Git privados (credenciais/token) — `docs/sprints.md`, novo item em "Registry — follow-up da Sprint 73".
- Histórico de builds anteriores (Jobs em `greencap-system` são efêmeros, sem listagem dedicada).

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` passam (incluindo novo `RegistryServiceTest`).
- Migration `V23` aplica sem erro (`./gradlew flywayMigrate` ou no boot da app).
- Aceite manual no `greencap-demo`:
  1. Usuário ADMIN/OPERATOR vê o botão "Build Image" em Container Registry; usuário VIEWER não vê (permission treeview reflete `GLOBAL_REGISTRY_BUILD`).
  2. Build de um repositório público simples (ex: Dockerfile "hello world") com Branch/Dockerfile path em branco (defaults) → Job criado em `greencap-system`, navega para `registry/build/<job>`, log do Kaniko aparece em tempo real, badge muda para `Complete`, imagem aparece em "Container Registry" → Repository/Tag informados.
  3. Build com Repository URL inválida ou Dockerfile path inexistente → erro aparece no log do Kaniko (`Failed`), badge `Failed`, sem crash da UI.
  4. `kubectl get job -n greencap-system` some após `ttlSecondsAfterFinished` (10 min).
