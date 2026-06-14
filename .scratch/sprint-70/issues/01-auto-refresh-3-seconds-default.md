---
id: "01"
title: "Platform Settings — adicionar opção '3 seconds' ao auto-refresh e torná-la o default"
status: done
labels: [feat, frontend]
sprint: 70
---

## Contexto

`PlatformSettings` (`CONTEXT.md`) inclui hoje a preferência de auto-refresh, com opções `RefreshInterval`: NONE(0), FIVE_SECONDS(5), TEN_SECONDS(10), THIRTY_SECONDS(30), ONE_MINUTE(60). Quando o usuário nunca salvou uma preferência (`UserService.findRefreshInterval` vazio), o fallback hoje é `NONE` — ou seja, auto-refresh desligado por padrão.

Decisão da sessão `/grill-with-docs`:
- Adicionar uma nova opção "3 seconds" ao enum.
- Trocar o **fallback** usado quando não há preferência salva de `NONE` para `THREE_SECONDS` — isto é, "default global" = valor de fallback para quem nunca configurou, não uma config que sobrepõe escolhas já salvas (quem já escolheu explicitamente "No auto refresh" ou outro valor continua com sua escolha).
- Aplicado uniformemente a todas as views `Refreshable` (sem exceção por view).
- Sem migration Flyway — `users.refresh_interval_seconds` já é `INTEGER` nullable; o fallback é hardcoded no Java, mesmo padrão do tema `"DARK"`.
- `CONTEXT.md` já atualizado (entrada `PlatformSettings`) com o novo default e a justificativa (responsividade para o público-alvo de clusters pequenos de dev/teste).

## Entrega

### `ui/RefreshInterval.java`

- Novo valor `THREE_SECONDS("3 seconds", 3)`, posicionado como o menor intervalo ativo (entre `NONE` e `FIVE_SECONDS`).

### `ui/PlatformSettingsView.java`

- `buildRefreshCard()`: quando `intervalSelect.getValue() == null` (sem preferência salva), usar `RefreshInterval.THREE_SECONDS` em vez de `RefreshInterval.NONE`.

### `ui/MainLayout.java`

- `onAttach()`: quando `userService.findRefreshInterval(username)` vier vazio, aplicar `RefreshInterval.THREE_SECONDS` (em vez de manter `currentRefreshInterval` no valor padrão `NONE` do field e não iniciar nenhum timer).
- Ajustar o valor inicial do field `currentRefreshInterval` se necessário para refletir o novo default.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Platform Settings: ComboBox de "Auto-refresh interval" lista "3 seconds" entre "No auto refresh" e "5 seconds".
- Usuário sem preferência salva (conta nova, ou conta existente que nunca abriu Platform Settings): ao entrar em qualquer view `Refreshable`, o auto-refresh já está ativo a cada 3s e o ComboBox em Platform Settings mostra "3 seconds" selecionado.
- Usuário que já salvou explicitamente "No auto refresh" (0) ou qualquer outro valor continua com esse valor — sem alteração de comportamento.
- Trocar manualmente o intervalo em Platform Settings continua persistindo e aplicando normalmente (incluindo voltar para "No auto refresh").

## Comments

- Aceite manual confirmado pelo usuário.
