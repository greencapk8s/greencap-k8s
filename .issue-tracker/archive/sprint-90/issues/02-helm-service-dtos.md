---
id: "90-02"
title: "HelmService — execução via CLI, DTOs e kubeconfig temporário"
status: done
priority: high
sprint: 90
---

`HelmService` executa operações Helm via `ProcessBuilder`, usando o Helm CLI instalado em `/usr/local/bin/helm`. Para cada operação, o kubeconfig descriptografado é escrito em um `Files.createTempFile` com permissão `600` e passado via `--kubeconfig`; o arquivo é deletado no `finally`.

DTOs: `HelmReleaseInfo` (nome, namespace, chart, appVersion, revisão, status, data) e `HelmReleaseDetails` (notes, values, manifest — todos como String).

Operações:
- `listReleases(Cluster, namespace)` — executa `helm list -n <ns> -o json` e faz parse do JSON com Jackson
- `getReleaseDetails(Cluster, namespace, name)` — executa `helm get notes/values/manifest` em paralelo e agrega em `HelmReleaseDetails`
- `uninstall(Cluster, namespace, name)` — executa `helm uninstall <name> -n <ns>`

Falhas de processo (exit code != 0 ou Helm CLI ausente) lançam `HelmOperationException` com a saída stderr como mensagem.
