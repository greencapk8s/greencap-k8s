# 02 — Traduzir MainLayout: sidebar e navbar

Status: done

## Contexto

O MainLayout concentra os labels de seção do sidebar, itens do menu e textos da navbar — todos em português.

## O que fazer

Sidebar — seções:
- `"VISÃO GERAL"` → `"OVERVIEW"`
- `"OBSERVABILIDADE"` → `"OBSERVABILITY"`
- `"CONFIGURAÇÃO"` (seção de settings) → `"SETTINGS"`

Sidebar — itens colapsável e desabilitados:
- `"Configuração"` (menu colapsável de ConfigMaps/Secrets) → `"Parameters"`
- `"Métricas"` → `"Metrics"`
- `"Usuários"` → `"Users"`
- `"Configurações"` → `"Settings"`

Navbar:
- `"Nenhum cluster ativo"` → `"No active cluster"`
- `"Namespace:"` (label) → `"Namespace:"` (manter — é termo de domínio)
- `"Selecionar..."` (placeholder) → `"Select..."`
- `"Erro ao carregar namespaces: "` → `"Failed to load namespaces: "`

## Critério de aceite

- Sidebar exibe todas as seções e itens em inglês
- Menu colapsável aparece como "Parameters" com sub-itens ConfigMaps e Secrets
- Navbar exibe "No active cluster" quando sem cluster ativo
