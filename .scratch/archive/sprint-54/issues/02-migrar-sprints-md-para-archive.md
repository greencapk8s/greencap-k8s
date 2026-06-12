---
id: "02"
title: "Migrar sprints 1-43 de sprints.md para sprints-archive.md"
status: done
labels: [docs, chore]
sprint: 54
---

## Contexto

A seção "Sprints Concluídas" de `docs/sprints.md` lista hoje (em ordem não cronológica): 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41, 40, 39, 37, 36, 35, 34, 33, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 22, 21, 20, 19, 18, 17, 16, 15, 14, 13, 12, 11, 23, 25, 24, 27, 26 — sem a sprint 38, que aparece na seção "Backlog" (sprints 28-32 também estão soltas no Backlog, fora de "Sprints Concluídas").

Decisão: manter na seção "Sprints Concluídas" apenas as últimas 10 sprints (44-53), em ordem decrescente. Mover o restante (1-43) para `docs/sprints-archive.md`, em ordem cronológica crescente.

## Entrega

- Criar `docs/sprints-archive.md`:
  - Cabeçalho explicando que o arquivo guarda o detalhamento de sprints concluídas fora da janela das últimas 10 (ver `docs/agents/sprint-archiving.md`)
  - Bullets das sprints 1 a 43 em ordem crescente (1, 2, 3, ... 43), incluindo as sprints atualmente soltas no "Backlog" (28, 29, 30, 31, 32)
  - Sprint 38: entrada com nota explícita "detalhamento não registrado em sprints.md na época — ver tabela Status Geral e git log"
- `docs/sprints.md`:
  - Seção "Sprints Concluídas" passa a conter apenas as sprints 44-53, em ordem decrescente (53→44) — conteúdo já existe, apenas remover as entradas 1-43 movidas
  - Seção "Backlog": remover as entradas das sprints 28-32 (já cobertas em "Sprints Concluídas"/archive); manter apenas itens realmente pendentes (ex: "RBAC + Polimento + Docker Final", se ainda válido — verificar se já foi entregue em sprints posteriores e remover se obsoleto)
  - Tabela "Status Geral" permanece inalterada

## Critérios de aceite

- `docs/sprints-archive.md` contém as sprints 1-43 em ordem crescente, com nota sobre a sprint 38
- `docs/sprints.md` "Sprints Concluídas" contém somente sprints 44-53 em ordem decrescente
- Nenhuma informação é perdida (apenas realocada)
- Tabela "Status Geral" inalterada
