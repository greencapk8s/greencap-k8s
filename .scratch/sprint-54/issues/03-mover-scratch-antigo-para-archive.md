---
id: "03"
title: "Mover .scratch/sprint-N (N < 44) para .scratch/archive/"
status: done
labels: [chore]
sprint: 54
---

## Contexto

`.scratch/` contém um diretório `sprint-N/` por sprint desde a sprint 4 (4, 7-27, 29-53), totalizando 49 diretórios. Seguindo o mesmo corte de "últimas 10 sprints" aplicado a `docs/sprints.md` (issue 02), os diretórios das sprints anteriores à 44 deixam de fazer parte do conjunto de trabalho ativo.

## Entrega

- Criar `.scratch/archive/`
- Mover todos os `.scratch/sprint-N/` com N < 44 (sprint-4, sprint-7 a sprint-27, sprint-29 a sprint-43) para `.scratch/archive/sprint-N/`, preservando a estrutura interna (`issues/*.md`)
- `.scratch/` (raiz) passa a conter apenas `sprint-44/` a `sprint-54/` + `archive/`

## Critérios de aceite

- `.scratch/sprint-N/` para N < 44 não existe mais na raiz de `.scratch/`
- `.scratch/archive/sprint-N/` contém o conteúdo original de cada diretório movido, sem perda de arquivos
- `git mv` usado para preservar o histórico dos arquivos
