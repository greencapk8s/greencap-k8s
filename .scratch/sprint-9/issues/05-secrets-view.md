# 05 — SecretsView

Status: done

## Contexto

Nova view para listar os Secrets do namespace ativo, exibida sob o grupo Configuração no sidebar. Por segurança, apenas metadados são exibidos — valores nunca são decodificados nem exibidos.

## O que fazer

- Criar `SecretsView` em `ui/` com rota `@Route("config/secrets")`
- Criar `SecretDto` em `kubernetes/dto/` com campos: nome, tipo (Opaque, kubernetes.io/tls, etc.), quantidade de keys, namespace, idade (creationTimestamp formatada)
- Adicionar método `listSecrets(kubeconfig, namespace)` ao mesmo service de ConfigMaps (`ConfigurationService`) em `kubernetes/` usando Fabric8 dentro de try-with-resources
- Grid com colunas: Nome, Tipo, Keys, Namespace, Idade
- Comportamento idêntico às views existentes: aviso inline quando sem cluster ativo, recarrega via `BeforeEnterObserver`
- Lançar `KubernetesOperationException` em falhas de API

## Critério de aceite

- View exibe os Secrets do namespace selecionado no cluster ativo
- Valores dos Secrets nunca são decodificados nem exibidos em nenhuma camada
- Coluna Keys exibe apenas a contagem — sem expor nomes das keys
- Coluna Tipo exibe badge `contrast` para `Opaque`, sem variante para outros tipos
- Sem cluster ativo: exibe aviso com botão de navegação para ClustersView
- Trocar namespace na navbar recarrega a lista automaticamente
