---
id: "34-01"
title: "Mover auto-refresh para ao lado do combobox de namespace"
status: done
labels: [enhancement, ui]
---

## Contexto

O combobox de auto-refresh está atualmente no lado direito da navbar, separado do combobox de namespace. Isso quebra a coesão visual — os dois controles afetam a mesma experiência de visualização de dados.

## Entrega

- Mover `refreshIntervalCombo` para ao lado de `namespaceLayout` na navbar
- Adicionar label "Auto refresh:" ao lado esquerdo do combo
- O grupo auto-refresh deve ser **sempre visível** (independente de cluster ativo), pois a preferência persiste no localStorage

## Critério de aceite

- [ ] Label "Auto refresh:" visível ao lado do combobox
- [ ] Combobox posicionado imediatamente à direita do combobox de namespace
- [ ] Funcionalidade de auto-refresh preservada
