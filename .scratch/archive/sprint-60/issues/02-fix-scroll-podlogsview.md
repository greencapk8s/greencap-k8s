---
id: "02"
title: "Corrigir scroll horizontal nos logs do Pod (logContent)"
status: done
labels: [fix, frontend]
sprint: 60
---

## Contexto

A `PodLogsView` (`/logs/pod/:namespace/:name`) exibe os logs do Pod no componente `logContent` (`Pre`), com o mesmo padrão de estilo de `yamlContent` na `ManifestView`: `white-space: pre` + `overflow: auto`. Linhas de log muito longas (stack traces, URLs, mensagens JSON) criam scroll horizontal, com o mesmo problema de UX corrigido na issue 01.

## Entrega

### `PodLogsView.java`

No método `styleLogContent()`, aplicar o mesmo padrão definido na issue 01 para `yamlContent`:

- `white-space: pre` → `white-space: pre-wrap`.
- Adicionar `overflow-wrap: anywhere`.
- Manter a linha `.set("overflow", "auto")` existente (não remover/alterar) e adicionar `.set("overflow-x", "hidden")`.

## Critérios de aceite

- `./gradlew compileJava` sem erros.
- Abrir os logs de um Pod com pelo menos uma linha de log muito longa (ex.: stack trace ou mensagem JSON extensa) e confirmar que a linha quebra visualmente, sem scroll horizontal na caixa nem na página.
- `scrollToBottom()` (auto-scroll para o fim do log a cada poll) continua funcionando corretamente após a mudança.
- Scroll vertical continua funcionando normalmente.

## Comments
