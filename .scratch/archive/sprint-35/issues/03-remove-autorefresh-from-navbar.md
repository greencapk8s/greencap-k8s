---
id: "35-03"
title: "Remover auto-refresh da navbar e ler preferência do banco"
status: done
labels: [feat, ui]
sprint: 35
---

## O que

Remover o combo de auto-refresh da navbar do `MainLayout`. O intervalo passa a ser lido do banco (via `UserService`) no carregamento do layout.

## Critérios de aceite

- `refreshIntervalCombo` e label "Auto refresh:" removidos da navbar
- `onAttach` lê intervalo do banco em vez de `localStorage`
- Timer de refresh inicializado com o valor do banco
- Lógica de localStorage (`AUTO_REFRESH_STORAGE_KEY`) removida
