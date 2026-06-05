# Cards de contagem de recursos por namespace

Status: done

## Descrição

Substituir os atuais cards de cluster (Total, Connected, Disconnected, Error, Unknown) por 7 cards de contagem de recursos Kubernetes escopados ao namespace ativo no ClusterContext.

## Recursos a exibir

| Card | Service | Navega para |
|---|---|---|
| Deployments | WorkloadService | DeploymentsView |
| Pods | WorkloadService | PodsView |
| Services | NetworkingService | ServicesView |
| ConfigMaps | ConfigurationService | ConfigMapsView |
| Secrets | ConfigurationService | SecretsView |
| Volume Claims | StorageService | PersistentVolumeClaimsView |
| Horizontal Scalers | AutoScalingService | HorizontalScalerView |

## Comportamento

- Sem cluster ativo: exibir mensagem de orientação ("Selecione um cluster...") com botão para ClustersView
- Cada card é clicável e navega para a view correspondente
- DashboardView implementa Refreshable

## Critérios de aceite

- Cards exibem contagem correta para o namespace selecionado
- Trocar namespace no seletor recarrega os cards
- Clicar em cada card navega para a view correta
