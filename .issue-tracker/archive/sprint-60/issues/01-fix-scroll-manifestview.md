---
id: "01"
title: "Corrigir scroll horizontal no YAML do Manifest (yamlContent)"
status: done
labels: [fix, frontend]
sprint: 60
---

## Contexto

A `ManifestView` (`/yaml/:resourceType/:namespace/:name`) exibe o YAML do recurso no componente `yamlContent` (`Pre`), estilizado com `white-space: pre` + `overflow: auto`. Quando uma linha do YAML é mais larga que a tela (ex.: `image:` com registry longo, `value:` de variável de ambiente extensa, anotações longas), o `Pre` cria uma barra de rolagem horizontal, quebrando a experiência do usuário.

Decisão definida via `/grill-with-docs`: linhas longas devem **quebrar visualmente (wrap)** em vez de gerar scroll horizontal.

## Entrega

### `ManifestView.java`

No estilo de `yamlContent`:

- `white-space: pre` → `white-space: pre-wrap` (preserva espaços/indentação, mas permite quebra de linha).
- Adicionar `overflow-wrap: anywhere` — quebra tokens longos sem espaço (URLs de imagem, valores base64, hashes) que não quebrariam apenas com `pre-wrap`.
- Manter a linha `.set("overflow", "auto")` existente (não remover/alterar) e adicionar `.set("overflow-x", "hidden")` — elimina o scroll horizontal sem alterar o scroll vertical já existente.

`yamlEditor` (TextArea, modo edição) não recebe mudanças de código — o componente Vaadin já aplica `white-space: pre-wrap` e `min-width: 0` internamente no `<textarea>` slotado, então linhas longas já quebram sem scroll horizontal. O critério de aceite abaixo cobre a verificação manual desse modo.

## Critérios de aceite

- `./gradlew compileJava` sem erros.
- Abrir o Manifest de um recurso com pelo menos uma linha muito longa (ex.: `image:` de registry longo ou uma anotação extensa) e confirmar:
  - Em modo leitura (`yamlContent`): a linha quebra visualmente, sem scroll horizontal na caixa nem na página.
  - Em modo edição (`yamlEditor`, via Edit): a linha quebra visualmente, sem scroll horizontal.
- Scroll vertical continua funcionando normalmente para YAMLs longos (mais linhas que a altura disponível).

## Comments
