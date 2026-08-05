# 04 — ConfigMapsView

Status: done

## Contexto

Nova view para listar os ConfigMaps do namespace ativo, exibida sob o grupo Configuração no sidebar. Exibe apenas metadados — nomes das keys, nunca valores.

## O que fazer

- Criar `ConfigMapsView` em `ui/` com rota `@Route("config/configmaps")`
- Criar `ConfigMapDto` em `kubernetes/dto/` com campos: nome, quantidade de keys, namespace, idade (creationTimestamp formatada)
- Adicionar método `listConfigMaps(kubeconfig, namespace)` em `ConfigService` (ou `ConfigurationService`) em `kubernetes/` usando Fabric8 dentro de try-with-resources
- Grid com colunas: Nome, Keys, Namespace, Idade
- Comportamento idêntico às views existentes: aviso inline quando sem cluster ativo, recarrega via `BeforeEnterObserver`
- Lançar `KubernetesOperationException` em falhas de API

## Critério de aceite

- View exibe os ConfigMaps do namespace selecionado no cluster ativo
- Coluna Keys exibe apenas a contagem (ex: `3 keys`) — sem expor nomes ou valores das keys
- Sem cluster ativo: exibe aviso com botão de navegação para ClustersView
- Trocar namespace na navbar recarrega a lista automaticamente
