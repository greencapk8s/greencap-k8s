---
id: "91-01"
title: "HelmRepository — entidade JPA, service e migration"
status: done
priority: high
sprint: 91
---

Nova entidade `HelmRepository` com campos `id`, `cluster` (ManyToOne), `name` e `url`. `HelmRepositoryRepository` com `findByCluster` e `deleteByClusterAndName`. `HelmRepositoryService` com `addRepository`, `listRepositories` e `removeRepository`.

`V31__create_helm_repositories.sql` cria a tabela `helm_repositories` com FK para `clusters`.
