# 01 — Refactor: extrair buildNoClusterMessage para UiConstants

Status: done

## Contexto

O método `buildNoClusterMessage()` está duplicado em 5 views (PodsView, DeploymentsView, ServicesView, ConfigMapsView, SecretsView) com código idêntico. A sprint 10 toca todas essas views para tradução — o momento certo para eliminar a duplicação.

## O que fazer

- Adicionar método estático `buildNoClusterMessage()` em `UiConstants`
- A mensagem deve estar em inglês: `"No active cluster. Select a cluster in Settings → Clusters."`
- O botão deve estar em inglês: `"Go to Clusters"`
- Remover o método `buildNoClusterMessage()` de cada uma das 5 views
- Substituir pela chamada `UiConstants.buildNoClusterMessage()`

## Critério de aceite

- Nenhuma view contém mais o método `buildNoClusterMessage()` definido localmente
- Comportamento visual idêntico ao anterior em todas as views
- Compilação sem erros
