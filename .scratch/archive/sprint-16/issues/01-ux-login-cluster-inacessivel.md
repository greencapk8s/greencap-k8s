---
id: 01
title: UX pós-login com cluster inacessível
status: done
sprint: 16
---

## Problema

Após o login, o `MainLayout` chama `loadNamespacesForCluster()` de forma síncrona na UI thread. O `KubernetesClientFactory` não tem timeout configurado, fazendo a UI travar por até 2 minutos quando o cluster ativo está inacessível.

## Decisões de design (ver ADR-0002)

- Estratégia: timeout curto + falha rápida (síncrono)
- `KubernetesClientFactory`: connection timeout 5s, request timeout 10s — hardcoded com constantes legíveis
- Em timeout: se `ConnectionStatus` atual for `CONNECTED`, transitar para `DISCONNECTED`
- Seletor de namespace: esconder quando cluster inacessível
- Notificação de erro exibida no `BOTTOM_END`

## Arquivos a modificar

- `kubernetes/KubernetesClientFactory.java` — adicionar constantes de timeout
- `domain/cluster/ClusterService.java` — novo método `markAsDisconnectedIfConnected()`
- `ui/MainLayout.java` — tratar `KubernetesOperationException` em `loadNamespacesForCluster()`

## Critérios de aceite

- [x] Com minikube parado: login responde em ≤ 10s com notificação de erro
- [x] Cluster que estava CONNECTED passa a DISCONNECTED no dashboard
- [x] Seletor de namespace some da navbar
- [x] Com minikube rodando: fluxo normal sem regressão
- [x] Itens de menu desabilitados (exceto Settings > Clusters) quando cluster inacessível
- [x] Faixa de aviso na navbar orientando verificar conexão
