---
title: "Help: textos e botão nas views de Networking, Parameters, Auto Scaling e Storage"
status: done
sprint: 46
---

## O que
- Adicionar constantes privadas `HELP_TITLE` / `HELP_TEXT` e passar para `buildSectionHeader` em:
  - `ServicesView` — o que é um Service, tipos (ClusterIP, NodePort, LoadBalancer, ExternalName), somente leitura
  - `IngressView` — o que é um Ingress, roteamento por host/path, badge de TLS, somente leitura
  - `ConfigMapsView` — o que é um ConfigMap, somente metadados exibidos, somente leitura
  - `SecretsView` — o que é um Secret, valores nunca exibidos/decodificados, somente leitura
  - `HorizontalScalerView` — o que é um Horizontal Pod Autoscaler e como ele ajusta réplicas
  - `PersistentVolumeClaimsView` — o que é uma PVC e sua relação com Storage Classes e volumes

## Por quê
- Orientar usuários iniciantes sobre o propósito de cada tela de Networking/Parameters/Auto Scaling/Storage e quais ações podem realizar nela

## Critérios
- Cada view exibe o botão de Help no header e abre o `HelpDialog` com o texto correspondente
- Texto baseado nas definições do glossário em `CONTEXT.md` (complementando com conceitos ainda não documentados, como HPA e PVC, em linguagem acessível)
