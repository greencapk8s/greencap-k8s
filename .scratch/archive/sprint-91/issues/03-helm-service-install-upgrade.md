---
id: "91-03"
title: "HelmService — install, upgrade e gestão de repos"
status: done
priority: high
sprint: 91
---

`HelmService` recebe `ensureRepos(Path kubeconfig, List<HelmRepository> repos)` que executa `helm repo add` para cada repo e depois `helm repo update`.

`install(Cluster, namespace, repoName, chart, version, releaseName, values)` — executa `ensureRepos`, depois `helm install <release> <repo>/<chart> -n <ns> [-f values.yaml] [--version <ver>]`.

`upgrade(Cluster, namespace, releaseName, chart, version, values)` — executa `ensureRepos`, depois `helm upgrade <release> <chart> -n <ns> -f values.yaml [--version <ver>] --reuse-values`.

Values são escritos em tempfile quando não vazios; deletados em `finally`. `HelmRepositoryService` é injetado no `HelmService` para carregar os repos do cluster antes de cada operação.
