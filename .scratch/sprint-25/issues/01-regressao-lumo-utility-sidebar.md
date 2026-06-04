# 01 — Regressão: labels PROJECT/OBSERVABILITY/SETTINGS sem formatação no sidebar

Status: done

## Causa raiz

`LumoUtility` classes (FontSize, FontWeight, Padding, TextColor) usadas em `buildNavSection`
dependem de CSS definido em `utility-global.js`. Esse módulo nunca foi importado
explicitamente no `MainLayout` — ao contrário de `badge-global.js` que foi adicionado
na sprint 22. A reconstrução do bundle Vite na sprint 24 (adição do Cytoscape.js) parou
de resolver o módulo implicitamente, tornando a regressão visível.

## Correção

Adicionar `@JsModule("@vaadin/vaadin-lumo-styles/utility-global.js")` ao `MainLayout`,
seguindo o mesmo padrão do `badge-global.js`.

## Critério de aceite

Labels "PROJECT", "OBSERVABILITY" e "SETTINGS" voltam a exibir fonte pequena em bold,
cor secundária e padding correto no sidebar.

## Comments
