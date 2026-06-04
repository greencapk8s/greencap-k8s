---
id: 02
title: Sidebar redimensionável com persistência em localStorage
status: done
sprint: 21
---

## Objetivo

Permitir ao usuário ajustar a largura do menu lateral arrastando a borda direita do drawer, com persistência da preferência em `localStorage`.

## Decisões de design

- Implementado via CSS + JavaScript injetado pelo `MainLayout` usando `UI.getCurrent().getPage().executeJs()`
- CSS custom property `--vaadin-app-layout-drawer-width` controla a largura do drawer
- Alça de resize: elemento `div` absoluto na borda direita do drawer, cursor `col-resize`
- Persistência: `localStorage` com chave `greencap-drawer-width`
- Limites: mínimo `180px`, padrão `240px`, máximo `400px`
- Largura restaurada no carregamento da página antes do primeiro render

## Arquivos a modificar

- `ui/MainLayout.java` — injetar script de resize após `addToDrawer()`

## Comportamento esperado

- Ao carregar: lê `localStorage.getItem('greencap-drawer-width')` e aplica via `--vaadin-app-layout-drawer-width`
- Ao arrastar: atualiza a CSS custom property em tempo real e persiste no `localStorage`
- Alça visível ao hover na borda direita do drawer
- Funciona em conjunto com o `DrawerToggle` existente (abrir/fechar não perde a largura salva)

## Critérios de aceite

- [ ] Arrastar a borda direita do drawer redimensiona o menu em tempo real
- [ ] Largura é persistida ao recarregar a página
- [ ] Limite mínimo de 180px respeitado
- [ ] Limite máximo de 400px respeitado
- [ ] DrawerToggle continua funcionando normalmente
- [ ] Sem regressão nas demais funcionalidades
