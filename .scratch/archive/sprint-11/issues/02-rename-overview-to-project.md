# 02 — Renomear seção "OVERVIEW" para "PROJECT" no menu principal

Status: done

## Contexto

A seção do menu lateral rotulada "OVERVIEW" agrupa Dashboard, Workloads, Networking, Parameters e Topologia — todas views relativas ao projeto/cluster ativo. O nome "PROJECT" reflete melhor a intenção do grupo: tudo que diz respeito a observar e operar dentro de um projeto Kubernetes.

## O que fazer

- Em `MainLayout.buildDrawer()`, alterar a string `"OVERVIEW"` para `"PROJECT"` na chamada `buildNavSection`

## Critério de aceite

- Menu lateral exibe "PROJECT" no lugar de "OVERVIEW"
- Nenhuma outra seção alterada
- Compilação sem erros
