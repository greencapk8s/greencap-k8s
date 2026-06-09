# 06 — Traduzir mensagens de exceção e validação

Status: done

## Contexto

Mensagens de `KubernetesOperationException` são propagadas diretamente para notificações na UI. `KubeconfigValidator` também exibe mensagens de erro ao usuário. Todas precisam estar em inglês.

## O que fazer

WorkloadService:
- `"Erro ao listar pods: "` → `"Failed to list pods: "`
- `"Erro ao listar deployments: "` → `"Failed to list deployments: "`

NetworkingService:
- `"Erro ao listar services: "` → `"Failed to list services: "`

ConfigurationService:
- `"Erro ao listar configmaps: "` → `"Failed to list configmaps: "`
- `"Erro ao listar secrets: "` → `"Failed to list secrets: "`

NamespaceService:
- `"Erro ao listar namespaces: "` → `"Failed to list namespaces: "`

ClusterService (verificar se há mensagens em português):
- Traduzir qualquer mensagem de exceção voltada ao usuário

KubeconfigValidator:
- Traduzir todas as mensagens de validação exibidas ao usuário

## Critério de aceite

- Nenhuma string em português nos métodos que lançam `KubernetesOperationException`
- Mensagens de validação do `KubeconfigValidator` em inglês
- Compilação sem erros
