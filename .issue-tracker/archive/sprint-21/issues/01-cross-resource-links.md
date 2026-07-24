---
id: 01
title: Links de navegação entre recursos relacionados
status: done
sprint: 21
---

## Objetivo

Tornar colunas de relacionamento clicáveis nas views de PersistentVolumes, ReplicaSets e HorizontalScaler, permitindo navegação direta para o recurso relacionado.

## Decisões de design

- Colunas clicáveis renderizadas como `Button` com variante `LUMO_TERTIARY` e `LUMO_SMALL` — aparência de link, sem caixa
- PV → PVC: troca namespace ativo (ClusterContext + UserService) antes de navegar, pois PVC é namespace-scoped
- ReplicaSet → Deployment e HPA → Deployment: navegação simples sem troca de namespace (já estão no mesmo namespace ativo)
- Valor `—` não é clicável — exibido como `Span` simples

## Arquivos a modificar

- `ui/PersistentVolumesView.java` — coluna Claim clicável
- `ui/ReplicaSetView.java` — coluna Owner clicável
- `ui/HorizontalScalerView.java` — coluna Target clicável

## Comportamento por view

### PersistentVolumesView — coluna Claim

- Valor: `namespace/nome` (ex: `greencap-demo/redis-data`) ou `—`
- Ao clicar: extrair namespace e nome do claim, chamar `clusterContext.setNamespace(namespace)` + `userService.updateActiveNamespace(username, namespace)`, navegar para `/storage/pvcs`

### ReplicaSetView — coluna Owner

- Valor: nome do Deployment pai ou `—`
- Ao clicar: navegar para `/workloads/deployments`

### HorizontalScalerView — coluna Target

- Valor: nome do Deployment alvo ou `—`
- Ao clicar: navegar para `/workloads/deployments`

## Critérios de aceite

- [ ] Clicar em Claim no PV troca o namespace e abre a view de PVCs
- [ ] Clicar em Owner no ReplicaSet abre a view de Deployments
- [ ] Clicar em Target no HPA abre a view de Deployments
- [ ] Valor `—` não é clicável em nenhuma das três views
- [ ] Sem regressão nas demais views
