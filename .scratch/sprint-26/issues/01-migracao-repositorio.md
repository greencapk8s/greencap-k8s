---
id: "26-01"
title: "Migração do repositório para greencapk8s/greencap-k8s"
status: done
labels: [chore, migration]
sprint: 26
---

## Objetivo

Migrar todos os arquivos do repositório `joseafilho/greencap-k8s-platform` para o repositório oficial da organização `greencapk8s/greencap-k8s`, que passará a ser o repositório principal da plataforma.

## Escopo

- **Fonte**: `/home/araujo/projects/greencap-k8s-forked/greencap-k8s-platform`
- **Destino**: `/home/araujo/projects/greencap-k8s/greencap-k8s`

## Decisões

- Histórico git do destino preservado (commit de migração adicionado em cima)
- Branch `infra-legacy` criada no destino antes da limpeza (backup dos arquivos de infra)
- Excluídos da cópia: `.git/`, `node_modules/`, `build/`
- Incluídos: todo o resto (`.claude/`, `docs/`, `CONTEXT.md`, `CLAUDE.md`, etc.)

## Critério de aceite

- [x] Destino limpo (arquivos de infra removidos)
- [x] Arquivos da plataforma copiados para o destino
- [x] Commit de migração criado no destino com mensagem destacada
- [x] Push para `origin/main` do destino realizado
