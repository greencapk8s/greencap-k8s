# ADR 0008 — Remove Repository via DELETE de manifests + garbage-collect via Fabric8 pod exec

**Status:** Accepted

## Context

A Sprint 74 adiciona "Remove Repository" ao Registry (ADR 0006). A API v2 do Docker Registry não tem um endpoint "delete repository": a única forma é fazer `DELETE /v2/<repo>/manifests/<digest>` para cada digest único referenciado pelas Tags do repositório. O registry interno do `greencap-demo` já roda com `REGISTRY_STORAGE_DELETE_ENABLED=true` (confirmado em runtime na imagem `registry:3.0.0` do addon `registry` do minikube), então esses DELETEs funcionam sem mudança de infraestrutura.

Após deletar todos os manifests de um repositório, ele pode continuar aparecendo no catálogo (`/v2/_catalog`) com 0 tags até que o registry rode garbage collection (`registry garbage-collect`). Esse comando só existe como subcomando do binário `registry` dentro do próprio container — não há endpoint HTTP equivalente na API v2.

## Decision

`RegistryMaintenanceService.deleteRepository` (1) deleta o manifest de cada digest único das Tags do repositório via `DELETE /v2/<repo>/manifests/<digest>`, reusando o mesmo port-forward de leitura do `RegistryService` (ADR 0006), e (2) executa `registry garbage-collect /etc/distribution/config.yml --delete-untagged` dentro do Pod do registry (label `actual-registry=true` em `kube-system`) via `client.pods().inNamespace("kube-system").withName(pod).exec(...)` do Fabric8 — o subrecurso `pods/exec` da API do Kubernetes, mesmo mecanismo usado por `kubectl exec`, mas sem dependência de um binário `kubectl` externo. É o primeiro uso de `pods/exec` no GreenCap (até aqui, toda integração via Fabric8 usava `port-forward`/`log`, nunca `exec`).

"Remove Tags" (multi-seleção, mesma sprint) não roda GC — apenas deleta os manifests das tags selecionadas via `DELETE /v2/<repo>/manifests/<digest>`, tratando `404` como sucesso (idempotente, cobre o caso de duas tags selecionadas apontarem para o mesmo digest).

## Alternatives considered

**Aceitar repositório "fantasma" (0 tags) sem GC**: mais simples e sem `pods/exec`, mas deixa uma entrada vazia e permanente no catálogo após "Remove Repository" — confuso para o usuário, que esperaria o repositório desaparecer da lista. Descartado a pedido do usuário durante o `/grill-with-docs`.

## Consequences

- Acopla `RegistryMaintenanceService` ao layout do Pod do registry (binário `/bin/registry`, config em `/etc/distribution/config.yml`, label `actual-registry=true`) — válido para a imagem `registry:3.0.0` usada pelo addon `registry` do minikube; uma mudança de imagem/addon pode exigir ajuste.
- Requer RBAC `pods/exec` em `kube-system` no Kubeconfig do Cluster (confirmado disponível com `kubectl auth can-i create pods/exec -n kube-system` no `greencap-demo`).
- `garbage-collect --delete-untagged` opera sobre todo o storage do registry, não apenas o repositório removido — pode liberar manifests/blobs órfãos de outros repositórios como efeito colateral (limpeza geral do registry, não escopada pela ação do usuário, mas considerada benéfica).
- "Remove Tags" não libera storage imediatamente — blobs órfãos das tags removidas só são reclamados na próxima execução de "Remove Repository" (que roda GC).
