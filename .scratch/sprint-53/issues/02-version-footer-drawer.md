---
id: 02
title: Exibir versão no rodapé do drawer
status: done
---

## O que fazer

- Injetar `BuildProperties` no `MainLayout`
- Adicionar `Span` com a versão formatada (`v{version}`) no fundo do drawer, separado por um `Scroller` ou espaçador para empurrar para o fundo
- Estilo: `FontSize.XXSMALL`, `TextColor.TERTIARY`, alinhado ao centro horizontalmente

## Critério de aceite

A versão aparece no fundo do drawer em todas as páginas, legível e sem sobrepor os itens de navegação.
