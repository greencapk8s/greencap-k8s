---
id: 03
title: TopologiaView — view Vaadin e habilitação no sidebar
status: done
sprint: 23
---

## Objetivo

Implementar `TopologiaView` em Vaadin Flow que orquestra o `TopologyService` e o Web Component `topology-graph`, e habilitar o item de menu no sidebar.

## Escopo

- `ui/TopologiaView.java` — view Vaadin com rota `/topologia`
- `MainLayout.java` — substituir `disabledNavItem("Topologia", ...)` pelo item ativo

## Comportamento esperado

- Ao entrar na view: exibe spinner enquanto `TopologyService.buildGraph()` executa
- Após carregamento: renderiza o Web Component `topology-graph` com o JSON do grafo
- Clique em nó: navega para o `manifestUrl` recebido no evento `node-clicked`
- Estado vazio (namespace sem recursos): mensagem "Nenhum recurso encontrado neste namespace"
- Erro de conexão: `Notification` em `BOTTOM_END` com mensagem de erro

## Restrições

- Sem lógica de negócio na view — apenas orquestração de UI
- Sem injeção de repositories — apenas `TopologyService`

## Critérios de aceite

- [ ] Item "Topologia" no sidebar navega para a view
- [ ] Spinner visível durante carregamento
- [ ] Grafo renderizado corretamente após carregamento
- [ ] Clique em nó navega para Manifest
- [ ] Estado vazio exibe mensagem adequada
- [ ] Erro exibe notificação em BOTTOM_END
- [ ] Compilação e testes passando
