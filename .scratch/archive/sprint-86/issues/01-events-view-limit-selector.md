---
title: EventsView — seletor de limite de Events exibidos
status: done
sprint: 86
---

## Contexto

A `EventsView` busca todos os Events do namespace ativo sem nenhuma restrição. Num cluster com atividade intensa, isso pode retornar centenas de registros, degradando a performance da listagem e dificultando a leitura. O objetivo é permitir que o usuário controle quantos Events quer ver, sempre ordenados do mais recente para o mais antigo.

## Comportamento esperado

Na `EventsView`, um seletor de limite aparece no section header (entre o título e o botão de refresh). As opções são **50, 100, 200, 500** e **All**, com **100 como padrão.

Ao selecionar um valor, a listagem recarrega imediatamente mostrando apenas os N Events mais recentes. O auto-refresh (quando ativo) respeita o limite selecionado no momento.

O `EventsDialog` (events por recurso específico) não é afetado — ele continua retornando todos os events do recurso sem limite.

## Decisões de design

- O limite é aplicado no `ObservabilityService.listEvents()`, após buscar e ordenar todos os eventos por `lastTimestamp` desc — garantindo que o usuário sempre veja os N mais recentes, não N arbitrários.
- O valor "All" equivale a ausência de limite (nenhum truncamento no stream).
- Nenhuma persistência do valor selecionado entre sessões — o padrão de 100 é restaurado a cada navegação para a view.
