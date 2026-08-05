---
title: "Help: componente HelpDialog e extensão do header compartilhado"
status: done
sprint: 46
---

## O que
- Criar `HelpDialog` (classe estática, mesmo padrão de `EventsDialog`): `Dialog` modal com `setHeaderTitle(title)`, corpo com o texto explicativo e botão "Close"
- Estender `UiConstants.buildSectionHeader` com um novo parâmetro de conteúdo de ajuda (título + texto), adicionando um botão `VaadinIcon.QUESTION_CIRCLE` (`LUMO_TERTIARY` + `LUMO_ICON`, `title="Help"`) posicionado **antes** do botão de refresh
- Header final: `Título — [Help] — [Refresh]`

## Por quê
- Base compartilhada para os botões de Help em todas as 16 views que usam `buildSectionHeader` — evita duplicar a lógica de abertura do diálogo em cada view

## Critérios
- `HelpDialog.open(title, text)` abre um modal com título e texto formatado, fechável pelo botão "Close"
- `buildSectionHeader` aceita o novo parâmetro e renderiza o botão de Help à esquerda do refresh, com mesmo estilo visual dos demais botões do header
- Compila sem quebrar nenhuma das 16 views que já usam `buildSectionHeader` (issue seguinte cuida de migrar cada uma)
