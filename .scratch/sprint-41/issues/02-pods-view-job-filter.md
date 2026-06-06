---
title: PodsView — filtro por Job via query param
status: done
---

## O que

`PodsView` lê o query param `?job=<name>` e filtra os pods cujo `jobName` bate com o valor.
Exibe um banner dismissível acima do grid enquanto o filtro estiver ativo.

## Critérios de aceite

- `beforeEnter()` lê `?job=<name>` e armazena em campo `jobFilter`
- Banner "Showing pods for Job: `<name>` ×" visível acima do grid quando filtro ativo
- Clicar em × limpa `jobFilter`, esconde o banner e reaplica o predicado (exibe todos os pods)
- `dataProvider.setFilter()` combina filtro de nome existente AND `jobFilter`
- Sem o query param: comportamento idêntico ao atual
