# 03 — DashboardView com cards de clusters

Status: done

## Contexto

A `DashboardView` atual é um placeholder vazio. O objetivo é exibir um resumo visual dos clusters registrados — quantos estão conectados, desconectados, com erro ou desconhecidos — sem precisar ir à view de clusters.

## O que fazer

- Substituir o placeholder por cards de resumo usando componentes Vaadin
- Exibir: total de clusters, contagem por `ConnectionStatus` (CONNECTED, DISCONNECTED, ERROR, UNKNOWN)
- Cada card com badge colorido conforme convenção do projeto (`success`, `error`, `contrast`)
- Clicar em um card navega para `ClustersView` (opcional, mas desejável)
- Dados carregados via `ClusterService.findAll()`

## Critério de aceite

- Dashboard exibe cards com contagens reais do banco
- Badges seguem a convenção de cores do projeto
- Sem lógica de negócio na view — apenas orquestração de UI via service
