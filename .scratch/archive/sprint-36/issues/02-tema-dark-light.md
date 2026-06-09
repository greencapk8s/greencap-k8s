# 02 — Tema dark/light

**Status:** done

## Contexto
O tema está fixo como `Lumo.DARK` (MainLayout linha 70). O objetivo é torná-lo configurável por usuário, persistido no banco, com controle em PlatformSettings.

## Entrega
- Migration `V10__add_theme_to_users.sql`: coluna `theme VARCHAR(10)` na tabela `users`, default `'DARK'`
- Campo `theme` (String) em `User.java`
- Métodos `findTheme(username)` e `updateTheme(username, String)` em `UserService`
- `MainLayout.onAttach()`: lê preferência do banco e aplica via `getElement().setAttribute("theme", ...)`
- `PlatformSettingsView`: card "Appearance" com `RadioButtonGroup` (Dark / Light)
- Ao mudar: persiste no banco + aplica na UI sem recarregar a página
- Default para usuários sem preferência: DARK

## Restrições
- Valores válidos: `"dark"` (Lumo.DARK) e `""` (Lumo default = light)
- Sem toggle na navbar — somente em PlatformSettings
