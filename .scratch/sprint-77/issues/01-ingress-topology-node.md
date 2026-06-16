---
id: 77-01
title: Ingress node na Topologia
status: done
priority: high
---

## Descrição

Adicionar nós Ingress ao grafo de Topologia, com edges conectando ao(s) Service(s) que roteiam.

## Critérios de aceite

- Ingresses do namespace aparecem como nós na Topologia com cor ciano (#06B6D4)
- Edge Ingress→Service criado para cada backend service existente no namespace
- Ingresses sem Services correspondentes aparecem como nós isolados (sinaliza misconfiguration)
- Painel lateral exibe: Hosts, TLS badge (Secure/Plain), IngressClass

## Escopo técnico

- `TopologyService`: listar Ingresses, método `ingressNode()`, edges Ingress→Service
- `topology-graph.ts`: adicionar `Ingress: '#06B6D4'` em `NODE_COLORS`
- `TopologyNodeDrawer`: adicionar bloco `isIngress` no `buildBody()`
- `TopologyNode` record: reusar `serviceType`=ingressClass, `capacity`=hosts, `accessMode`=TLS string
