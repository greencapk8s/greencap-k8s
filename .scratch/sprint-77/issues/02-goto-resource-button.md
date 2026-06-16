---
id: 77-02
title: Botão "Go to resource" no painel lateral da Topologia
status: done
priority: high
---

## Descrição

Substituir botão "Ver YAML" no painel lateral da Topologia por "Go to resource" que navega direto para a view do recurso já filtrada pelo nome.

## Critérios de aceite

- Botão "Go to resource" substitui "Ver YAML" para todos os tipos exceto PodGroup
- PodGroup mantém botão "Go to Pods" navegando para `workloads/pods` sem filtro
- Cada tipo navega para sua view correta com `?name={resourceName}`
- `manifestUrl` no `TopologyNode` passa a conter a URL da view (não mais URL do YAML)

## Mapeamento de rotas

| Tipo | URL |
|---|---|
| Deployment | `workloads/deployments?name={name}` |
| ReplicaSet | `workloads/replicasets?name={name}` |
| Service | `networking/services?name={name}` |
| PVC | `storage/persistentvolumeclaims?name={name}` |
| Ingress | `networking/ingresses?name={name}` |
| PodGroup | `workloads/pods` |

## Escopo técnico

- `TopologyService`: refatorar `manifestUrl()` → `resourceViewUrl()` com switch por tipo
- `TopologyNodeDrawer`: renomear label do botão, remover dependência do `manifestUrl` como YAML URL
