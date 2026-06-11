---
id: "07"
title: "fix: preservar seleção da grid ao voltar do Manifest"
status: done
labels: [fix, frontend]
sprint: 57
---

## Contexto

Encontrado durante o aceite manual da sprint 57: ao selecionar um item na grid e abrir "View Manifest" (navegação para `yaml/...`), ao voltar (botão Back) a seleção volta para o primeiro item em vez de preservar o item que estava selecionado.

Causa: cada View é recriada pelo Vaadin a cada navegação (nova `Grid`, novo `ListDataProvider`). `selectFirstOrPreserve` consulta `grid.asSingleSelect().getValue()` da instância atual, que está sempre vazia logo após a recriação — por isso cai sempre no fallback "primeiro item".

## Entrega

- Novo bean `GridSelectionMemory` (`@VaadinSessionScope`), guarda o nome do último item selecionado por view (`Map<String, String>`, chave = `viewKey`).
- `UiConstants.configureSingleSelection(grid, selectionMemory, viewKey, nameExtractor)`: nova sobrecarga que, além de configurar `SelectionMode.SINGLE`, anexa `selectionMemory`/`viewKey` ao `Grid` (via `ComponentUtil`) e registra um listener que grava o nome do item selecionado na memória a cada mudança de seleção.
- `UiConstants.selectFirstOrPreserve(...)`: ao não encontrar seleção atual na instância da grid, consulta `GridSelectionMemory` (via dados anexados ao `Grid`) antes de cair no fallback "primeiro item".
- Todas as 14 views da sprint 57 passam a injetar `GridSelectionMemory` e usar a nova sobrecarga de `configureSingleSelection`, com `viewKey = getClass().getSimpleName()`.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros
- Selecionar um item, abrir "View Manifest", voltar (Back) → o mesmo item continua selecionado
- Se o item selecionado deixar de existir na lista (ex.: foi deletado), cai para o primeiro item
- Comportamento de seleção em refresh manual/automático e filtros (issue 06) continua funcionando

## Comments
