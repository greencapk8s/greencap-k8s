---
id: 01
title: Navbar não acompanha o hide do drawer
status: done
---

## Problema

Ao fechar o drawer via `DrawerToggle`, o navbar mantinha o `left` fixo no valor da largura do drawer (ex: `240px`), em vez de voltar para `0px`. Isso causava um desalinhamento visual onde o navbar ficava deslocado para a direita mesmo com o drawer oculto.

## Causa raiz

Em `MainLayout.initResizableDrawer()`, a função `applyWidth()` definia `navbarPart.style.left = w + 'px'` de forma absoluta, sem observar o estado aberto/fechado do drawer. Nenhum listener reagia ao toggle do drawer para corrigir o offset.

## Solução

- `applyWidth()` agora verifica `appLayout.hasAttribute('drawer-opened')` antes de aplicar o offset: usa `w + 'px'` se aberto, `'0px'` se fechado.
- Adicionado `MutationObserver` no atributo `drawer-opened` do AppLayout: sempre que o drawer abre ou fecha, recalcula `navbarPart.style.left` e `contentPart.style.marginInlineStart`.
