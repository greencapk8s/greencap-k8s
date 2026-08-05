---
id: "01"
title: "Auto refresh nas listing views"
status: done
labels: [feat, ui]
sprint: 30
---

## Contexto

As listing views exibem dados do Kubernetes API que mudam constantemente (pods reiniciando, deployments escalando, etc.). Hoje o usuário precisa clicar manualmente no botão de refresh. Um auto refresh configurável reduz o atrito no monitoramento.

## Entrega

Adicionar um combobox de intervalo de auto refresh na navbar global (`MainLayout`). Quando ativo, a view corrente é atualizada automaticamente no intervalo selecionado.

## Opções do combobox

- Sem auto refresh (padrão)
- 5 segundos
- 10 segundos
- 30 segundos
- 1 minuto

## Decisões de design

- **Localização**: navbar global — um único controle vale para todas as views
- **Contrato**: interface `Refreshable` com método `refresh()` — `MainLayout` verifica se `getContent()` implementa e chama
- **Views afetadas (12)**: `PodsView`, `DeploymentsView`, `ReplicaSetView`, `ServicesView`, `ConfigMapsView`, `SecretsView`, `EventsView`, `MetricsView`, `HorizontalScalerView`, `PersistentVolumeClaimsView`, `PersistentVolumesView`, `StorageClassesView`
- **Views excluídas**: `LoginView`, `ClustersView`, `ManifestView`, `DashboardView`, `TopologiaView`
- **Persistência**: `localStorage` (chave `greencap-auto-refresh-interval`), seguindo padrão do drawer width
- **Erros**: silenciados no auto refresh — grid mantém dados anteriores, sem notificação de erro
- **Indicador visual**: nenhum — refresh silencioso

## Implementação

1. Criar interface `Refreshable` em `io.greencap.k8s.ui`
2. As 12 listing views implementam `Refreshable` expondo `refresh()` (delega para o `loadXxx()` existente)
3. `MainLayout`: adicionar `ComboBox<RefreshInterval>` na navbar com enum `RefreshInterval` (NONE, 5s, 10s, 30s, 1m)
4. `MainLayout`: ao mudar o intervalo, cancelar timer anterior e agendar novo com `ScheduledExecutorService` que chama `ui.access(() -> ((Refreshable) getContent()).refresh())` — só executa se `getContent()` instanceof `Refreshable`
5. `MainLayout.afterNavigation()`: cancelar e rescheduler o timer para a nova view
6. Persistir/restaurar via `Page.executeJs` + `localStorage` (mesmo padrão do drawer)
7. Cancelar timer no `DetachEvent` do `MainLayout`

## Critério de aceite

- [ ] Combobox aparece na navbar com as 5 opções
- [ ] Selecionando um intervalo, o grid da view ativa atualiza automaticamente
- [ ] Navegando entre views, o timer reinicia para a nova view
- [ ] Selecionando "Sem auto refresh", o timer para imediatamente
- [ ] O intervalo selecionado persiste ao recarregar o browser
- [ ] Erros de API durante auto refresh não geram notificação
- [ ] Views sem suporte (Dashboard, Topology, etc.) não quebram ao auto refresh estar ativo
