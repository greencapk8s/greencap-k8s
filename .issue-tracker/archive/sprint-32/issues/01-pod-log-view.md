---
id: 32-01
title: "Troubleshooting: visualização de PodLog em página dedicada"
status: done
priority: high
type: feat
sprint: 32
---

## Contexto

Usuários precisam inspecionar logs de Pods para troubleshooting. A feature deve ser acessível diretamente da `PodsView` e permitir acompanhamento em tempo real via poll periódico.

## Escopo

### Serviço

- `ObservabilityService.fetchPodLogs(cluster, namespace, podName, container, tailLines, previous)` → `String`
  - Fabric8: `client.pods().inNamespace(ns).withName(name).inContainer(container).tailingLines(n).getLog()`
  - Flag `previous=true` busca o container terminado anteriormente
  - Quando `previous=true` e não há log anterior, retornar `Optional.empty()` (não lançar exceção)
  - Container pode ser `null` quando o pod tem apenas um container

### View — `PodLogsView`

- Rota: `logs/pod/:namespace/:name` (padrão `ManifestView`)
- Toolbar:
  - `Select<String>` de container — **condicional**: só exibe quando o pod tem > 1 container
  - `Select<Integer>` de linhas: opções `100 / 500 / 1000`, padrão `100`
  - Toggle **"Previous container"** — desabilitado por padrão; quando ativo, busca o log do container terminado
  - Botão **Pause / Resume** — controla o auto-poll
- Área de log: elemento `Pre` (ou `TextArea` readonly) com fonte monospace, tamanho cheio
- Auto-poll a cada 3 segundos via `ScheduledExecutorService` + `ui.access()` — mesmo padrão da sprint 30
- Auto-scroll para o fim a cada atualização via JS (`element.scrollTop = element.scrollHeight`)
- Quando `previous=true` e não há log anterior: exibe mensagem `"No previous log available for this container."`
- Poll cancelado no `DetachEvent`

### Ponto de entrada

- `PodsView`: adicionar botão Logs (ícone `ALIGN_JUSTIFY`) na coluna de ações, após o botão Events
- Clique navega para `logs/pod/{namespace}/{name}`

### Termo canônico

- `PodLog` adicionado ao `CONTEXT.md`

## Critérios de aceite

- [ ] Botão Logs aparece na coluna de ações de cada Pod
- [ ] Página abre com os últimos 100 logs do container principal
- [ ] Auto-poll atualiza a área de log a cada 3s sem perder scroll
- [ ] Pause interrompe o poll; Resume o retoma
- [ ] Select de linhas recarrega imediatamente ao mudar
- [ ] Select de container aparece somente em pods com > 1 container; mudar recarrega
- [ ] Toggle "Previous container" exibe log anterior quando disponível
- [ ] Toggle "Previous container" exibe mensagem informativa quando não há log anterior
- [ ] Compilação sem erros e testes passando

## Fora do escopo

- Download do log como arquivo
- Busca/filtro dentro do log
- Streaming contínuo (WebSocket / SSE)
- Logs de Deployments ou outros Workloads
