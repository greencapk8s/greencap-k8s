---
id: 77-03
title: Pré-filtro ?name= nas views de destino da Topologia
status: done
priority: high
---

## Descrição

Adicionar suporte ao query param `?name=` nas views de listagem para que a navegação "Go to resource" da Topologia chegue com o recurso já filtrado.

## Critérios de aceite

- Navegar para `workloads/deployments?name=my-app` filtra a grid pelo nome "my-app"
- O filtro é aplicado ao campo `nameFilter` existente (TextField no header da grid)
- O filtro fica visível e editável — usuário pode limpá-lo manualmente
- Padrão idêntico ao `?job=` já implementado em `PodsView`

## Views afetadas

- `DeploymentsView`
- `ReplicaSetView`
- `ServicesView`
- `PersistentVolumeClaimsView`
- `IngressView`
