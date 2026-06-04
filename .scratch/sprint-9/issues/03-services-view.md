# 03 — ServicesView

Status: done

## Contexto

Nova view para listar os Services do namespace ativo, exibida sob o grupo Rede no sidebar. Segue o mesmo padrão de PodsView e DeploymentsView — leitura via Fabric8, sem operações de escrita.

## O que fazer

- Criar `ServicesView` em `ui/` com rota `@Route("networking/services")`
- Criar `ServiceDto` em `kubernetes/dto/` com campos: nome, tipo (ClusterIP/NodePort/LoadBalancer/ExternalName), clusterIP, portas (porta:protocolo), namespace
- Criar `NetworkingService` em `kubernetes/` com método `listServices(kubeconfig, namespace)` usando Fabric8 dentro de try-with-resources
- Grid com colunas: Nome, Tipo, Cluster IP, Porta(s), Namespace
- Comportamento idêntico às views existentes: aviso inline quando sem cluster ativo, recarrega ao entrar na view via `BeforeEnterObserver`
- Lançar `KubernetesOperationException` em falhas de API

## Critério de aceite

- View exibe os Services do namespace selecionado no cluster ativo
- Coluna Tipo exibe badge com variante por tipo: `success` para LoadBalancer, `contrast` para ClusterIP, sem variante para NodePort
- Sem cluster ativo: exibe aviso com botão de navegação para ClustersView
- Trocar namespace na navbar recarrega a lista automaticamente
