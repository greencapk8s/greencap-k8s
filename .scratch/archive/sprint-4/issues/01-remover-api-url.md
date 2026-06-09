# 01 — Remover apiUrl do Cluster

Status: done

## Contexto

Durante o grilling de domínio (grill-with-docs), ficou decidido que o kubeconfig é a única fonte de verdade para acesso ao cluster (ADR-0001). O campo `apiUrl` na entidade `Cluster` é redundante — o server URL já está dentro do kubeconfig.

## O que fazer

- Remover o campo `apiUrl` da entidade `Cluster`
- Criar migration Flyway `V3__remove_api_url_from_clusters.sql` para dropar a coluna
- Remover `apiUrl` do `CreateClusterRequest` DTO
- Ajustar `ClustersView` para não exibir nem coletar `apiUrl` no formulário
- Garantir que nenhum outro ponto do código referencia `apiUrl`

## Critério de aceite

- App sobe sem erros
- É possível adicionar um cluster via kubeconfig sem campo de URL
- Grid de clusters não exibe coluna de URL
