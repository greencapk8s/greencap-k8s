---
id: "35-02"
title: "PlatformSettingsView — tela de configurações da plataforma"
status: done
labels: [feat, ui]
sprint: 35
---

## O que

Criar `PlatformSettingsView` acessível via menu Settings. Layout em cards por seção. Card "Refresh" com select de intervalo de auto-refresh.

## Critérios de aceite

- Rota `/settings` funcional, acessível pelo item "Settings" no sidebar
- Card "Refresh" com label e `ComboBox<RefreshInterval>`
- Ao entrar na tela, o select exibe o intervalo salvo para o usuário logado
- Ao alterar o select, persiste no banco via `UserService`
- Notificação de confirmação em `BOTTOM_END`
