---
id: "01"
title: "Tornar o YAML do Manifest editável (Edit + Apply)"
status: done
labels: [feat, backend, frontend]
sprint: 59
---

## Contexto

A `ManifestView` (`/yaml/:resourceType/:namespace/:name`) hoje exibe o YAML do recurso (`Serialization.asYaml`) num `Pre` somente leitura. Esta sprint torna o YAML editável diretamente nessa página, com dois botões: **Edit** (entra em modo edição, foca o editor) e **Apply** (envia a modificação ao cluster via Fabric8).

Decisões de escopo definidas via `/grill-with-docs` (ver `CONTEXT.md` — termos `Manifest` e `Apply` — e `docs/adr/0005-manifest-apply-as-full-replace.md`):

- Editável apenas para os **11 tipos namespaced**: `pod, deployment, replicaset, job, cronjob, service, ingress, configmap, secret, horizontalscaler, persistentvolumeclaim`. `node`, `persistentvolume`, `storageclass` permanecem read-only (sem botões Edit/Apply).
- Nova permissão transversal `MANIFEST_EDIT` controla os botões Edit/Apply (concedida a Operator/Admin, não a Viewer).
- Editor: `TextArea` do Vaadin (sem novas dependências), reaproveitando o estilo monoespaçado do `Pre` atual.
- Botão **Edit** alterna para **Cancelar** durante a edição (descarta alterações e volta ao YAML original); **Apply** só fica visível/habilitado em modo edição.
- **Apply** abre `ConfirmDialog` antes de enviar.
- **Apply** = replace completo (PUT) via `client.resource(yaml).inNamespace(namespace).update()`, removendo antes `metadata.resourceVersion`, `uid`, `creationTimestamp`, `generation`, `managedFields` e o nó `status` — evita 409 espúrio por churn de status (ver ADR 0005).
- Validação prévia: bloquear Apply (sem chamar a API) se o YAML for inválido, ou se `kind`/`metadata.name`/`metadata.namespace` divergirem dos parâmetros da URL.
- Pós-Apply: sucesso → re-`fetchYaml`, volta ao modo leitura, notificação de sucesso (`LUMO_SUCCESS`, `BOTTOM_END`). Falha → notificação de erro (`LUMO_ERROR`, `BOTTOM_END`), permanece em edição com o texto do usuário intacto.

## Entrega

### 1. `Permission.java`
- Novo grupo `// Project — Manifest` com `MANIFEST_EDIT`.
- Incluir em `operatorPermissions()`. Não incluir em `viewerPermissions()`.

### 2. `V19__add_manifest_edit_permission.sql`
- Conceder `MANIFEST_EDIT` a usuários que possuem `SETTINGS_CLUSTERS_WRITE` (mesmo identificador de "operator/admin" usado em `V17`/`V18`), seguindo o padrão `INSERT INTO user_permissions ... ON CONFLICT DO NOTHING`.

### 3. `ManifestService.java`
- Novo método `applyYaml(Cluster cluster, String resourceType, String namespace, String name, String yamlContent)`:
  - Parsear `yamlContent` com `Serialization.yamlMapper().readTree(...)` para um `JsonNode`/`ObjectNode`. Erro de parsing → `KubernetesOperationException("Invalid YAML: " + ...)`.
  - Validar `kind` contra um mapa `resourceType → kind` esperado (ex.: `horizontalscaler → HorizontalPodAutoscaler`, `persistentvolumeclaim → PersistentVolumeClaim`, etc., cobrindo os 11 tipos). Validar `metadata.name == name` e `metadata.namespace == namespace`. Qualquer divergência → `KubernetesOperationException` com mensagem amigável (sem chamar a API).
  - Remover do `ObjectNode`: `status` (nó raiz) e, em `metadata`, `resourceVersion`, `uid`, `creationTimestamp`, `generation`, `managedFields`.
  - Serializar de volta (`yamlMapper().writeValueAsString(...)`) e chamar `client.resource(cleanedYaml).inNamespace(namespace).update()`.
  - Envolver `KubernetesClientException` em `KubernetesOperationException`, repassando a mensagem da API (ex.: conflitos reais de `spec`, valores inválidos).
  - Operação dentro de `try-with-resources` com o `KubernetesClient`, seguindo o padrão de `fetchYaml`.

### 4. `ManifestView.java`
- Adicionar dois botões no header (`buildHeader()`): **Edit** (`VaadinIcon.EDIT`) e **Apply** (`VaadinIcon.CHECK`), ambos `LUMO_TERTIARY`.
- `Edit`:
  - Visível apenas para os 11 tipos editáveis (resourceType lowercase no Set de tipos suportados); `setEnabled(SecurityUtils.hasPermission(Permission.MANIFEST_EDIT))` com tooltip quando desabilitado.
  - Clique entra em modo edição: mostra `TextArea` (pré-preenchido com o YAML atual, mesmo estilo monoespaçado do `Pre`), oculta o `Pre`, foca o `TextArea`, troca o ícone/texto para "Cancelar", exibe/habilita `Apply`.
  - Clique em modo edição (agora "Cancelar"): descarta o texto editado, restaura `Pre` com o YAML original, oculta `TextArea`, volta para "Edit", oculta/desabilita `Apply`.
- `Apply`:
  - Oculto/desabilitado fora do modo edição.
  - Clique abre `ConfirmDialog` ("Apply changes to {resourceType} \"{name}\"? This updates the resource directly in the cluster.").
  - Ao confirmar: chama `manifestService.applyYaml(...)` com o conteúdo do `TextArea`.
    - Sucesso: re-`fetchYaml`, atualiza `Pre`, sai do modo edição (volta para "Edit"/oculta `Apply`/`TextArea`), notificação de sucesso.
    - Falha (`KubernetesOperationException`): notificação de erro, permanece em modo edição com o texto do `TextArea` intacto.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Para os 11 tipos namespaced, com `MANIFEST_EDIT`: botões Edit/Apply visíveis e funcionais conforme fluxo acima.
- Para `node`, `persistentvolume`, `storageclass`: sem botões Edit/Apply (comportamento read-only inalterado).
- Sem `MANIFEST_EDIT`: botão Edit visível porém desabilitado (tooltip explicativo).
- YAML inválido ou `kind`/`name`/`namespace` divergentes bloqueiam o Apply com notificação de erro, sem chamar a API.
- Apply bem-sucedido reflete a alteração ao recarregar a página (ex.: editar `spec.replicas` de um Deployment e confirmar a mudança no cluster).
- Edição de um Deployment com `status` mudando continuamente (réplicas/condições) não gera 409 ao aplicar.

## Comments
