---
id: "01"
title: "Documentar rotina de archiving de sprints (sprints.md + .scratch)"
status: done
labels: [docs, chore]
sprint: 54
---

## Contexto

`docs/sprints.md` e `.scratch/` crescem a cada sprint sem limite. `docs/sprints.md` já está com 598 linhas e a seção "Sprints Concluídas" está fora de ordem (53→44, depois 1→10, depois 22→11, etc.) e sem a sprint 38. `.scratch/` acumula um diretório `sprint-N/` por sprint desde a sprint 4.

Decisão (sessão `/grill-with-docs`): manter visível apenas as últimas 10 sprints na seção "Sprints Concluídas" de `docs/sprints.md`, arquivando o restante em `docs/sprints-archive.md`; e mover diretórios `.scratch/sprint-N/` antigos para `.scratch/archive/sprint-N/`.

## Entrega

- Criar `docs/agents/sprint-archiving.md` descrevendo:
  - Regra: a seção "Sprints Concluídas" de `docs/sprints.md` mantém no máximo as últimas 10 sprints (a tabela "Status Geral" permanece completa, sem corte)
  - Quando o fechamento de uma sprint faz a seção ultrapassar 10 entradas, a(s) entrada(s) mais antiga(s) são movidas para `docs/sprints-archive.md`, em ordem cronológica crescente
  - O diretório `.scratch/sprint-N/` correspondente à sprint arquivada é movido para `.scratch/archive/sprint-N/`
  - Quando executar: como parte da etapa 6 (Fechamento) do fluxo de sprint, no mesmo commit de fechamento
- Atualizar `CLAUDE.md`:
  - Adicionar referência a `docs/agents/sprint-archiving.md` na seção "Agent skills", seguindo o padrão de `issue-tracker.md`/`triage-labels.md`/`domain.md`
  - Ajustar a etapa 6 (Fechamento) do "Fluxo de Sprint" para citar a verificação de archiving
- Atualizar `docs/agents/issue-tracker.md` com uma nota sobre `.scratch/archive/sprint-N/` — diretórios arquivados, ainda pesquisáveis via grep, fora do fluxo ativo

## Critérios de aceite

- `docs/agents/sprint-archiving.md` existe e descreve a regra de forma autossuficiente (não depende de contexto desta issue)
- `CLAUDE.md` referencia o novo doc e a etapa 6 menciona a verificação de archiving
- `docs/agents/issue-tracker.md` menciona `.scratch/archive/`
