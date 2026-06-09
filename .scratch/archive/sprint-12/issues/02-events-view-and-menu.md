# 02 — EventsView e atualização do menu

Status: done

## O que fazer

- Criar `EventsView` em `ui/` com rota `observability/events`
- Grid: Type (badge Normal=success/Warning=error), Reason, Object, Message, Count, Age
- Menu: substituir `disabledNavItem("Logs")` por item ativo apontando para `EventsView`
- Atualizar `CONTEXT.md` com o termo `Event`

## Critério de aceite

- Item "Events" navegável no menu OBSERVABILITY
- Grid exibe eventos do namespace selecionado
- Badge verde para Normal, badge vermelho para Warning
- Compilação sem erros
