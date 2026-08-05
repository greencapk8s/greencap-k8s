---
id: "01"
title: "UX: API compartilhada de seleção e barra de ações (UiConstants)"
status: done
labels: [feat, frontend]
sprint: 57
---

## Contexto

Sprint 57 reorganiza a UX das listagens: troca a ordem dos botões Refresh/Help, remove os botões Delete/Manifest/Events das colunas de ação das grids e os move para a barra de título, operando sobre o item selecionado. Esta issue cobre a infraestrutura compartilhada em `UiConstants` que as demais issues (02-06) vão consumir.

## Entrega

### 1. `UiConstants.buildSectionHeader` — reordenar Refresh/Help

A sobrecarga existente `buildSectionHeader(title, onRefresh, helpTitle, helpText)` passa a montar `[heading][...action buttons][Refresh][Help]` (hoje é `[heading][Help][Refresh]`). Usada sem botões extras por EventsView e MetricsView (sem coluna de ações).

### 2. `UiConstants.SelectionAction<T>` (record)

Record com: `icon` (VaadinIcon), `title` (String), `enabled` (boolean — permissão estática calculada uma vez), `destructive` (boolean — aplica `LUMO_ERROR`), `handler` (`Consumer<T>`). Factories: `of(icon, title, handler)`, `of(icon, title, enabled, handler)`, `destructive(icon, title, enabled, handler)`.

### 3. Nova sobrecarga `buildSectionHeader(title, onRefresh, helpTitle, helpText, grid, List<SelectionAction<T>>)`

Para cada `SelectionAction`, cria um `Button` ícone (tema `LUMO_TERTIARY` + `LUMO_ICON`, + `LUMO_ERROR` se `destructive`), `title` como atributo HTML (`title()`), click handler chama `grid.asSingleSelect().getOptionalValue().ifPresent(action.handler())`. Botão começa desabilitado se `!enabled` ou sem seleção. Um `ValueChangeListener` no `grid.asSingleSelect()` reabilita/desabilita os botões conforme há ou não item selecionado. Ordem final da barra: `[heading][...selectionActions na ordem da lista][Refresh][Help]` — para o padrão acordado `[Delete?][Manifest][Events?][Refresh][Help]`, a lista deve ser passada nessa ordem.

### 4. `UiConstants.configureSingleSelection(Grid<T>)`

`grid.setSelectionMode(SelectionMode.SINGLE)` + `((GridSingleSelectionModel<T>) grid.getSelectionModel()).setDeselectAllowed(false)` — impede deselecionar clicando na linha já selecionada.

### 5. `UiConstants.selectFirstOrPreserve(Grid<T>, ListDataProvider<T>, Function<T, String> nameExtractor)`

Após qualquer `dataProvider.refreshAll()` (load inicial, refresh manual/automático, mudança de filtro): tenta manter a seleção atual pelo nome (`nameExtractor`); se o item não existir mais na lista filtrada, seleciona o primeiro item visível; se a lista estiver vazia, `grid.deselectAll()`.

## Critérios de aceite

- `./gradlew compileJava` sem erros
- Ordem Refresh/Help trocada em todas as views que usam `buildSectionHeader` (incluindo EventsView/MetricsView)
- API nova documentada e pronta para consumo nas issues 02-06

## Comments
