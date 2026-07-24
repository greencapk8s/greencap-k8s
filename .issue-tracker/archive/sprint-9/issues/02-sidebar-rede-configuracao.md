# 02 — Sidebar: grupos Rede e Configuração

Status: done

## Contexto

O sidebar já tem o padrão de menu colapsável com sub-itens (Workloads → Pods, Deployments). Precisamos adicionar dois novos grupos seguindo o mesmo padrão: Rede (Services) e Configuração (ConfigMaps, Secrets). O item "Topologia" permanece como placeholder desabilitado.

## O que fazer

Em `MainLayout`:

- Adicionar grupo colapsável **Rede** com sub-item: `Services` → rota `/networking/services`
- Adicionar grupo colapsável **Configuração** com sub-itens: `ConfigMaps` → `/config/configmaps` e `Secrets` → `/config/secrets`
- Manter **Topologia** como item desabilitado (pointer-events: none) abaixo dos novos grupos

## Critério de aceite

- Sidebar exibe os grupos Rede e Configuração colapsáveis, consistentes visualmente com Workloads
- Clicar em Services, ConfigMaps ou Secrets navega para a rota correspondente
- Topologia permanece visível e desabilitado
