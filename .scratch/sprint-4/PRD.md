# Sprint 4 — Estabilização + Ambiente Local

## Objetivo

Garantir que tudo implementado nas sprints 1–3 roda corretamente e pode ser validado visualmente. Foco exclusivo em integração read-only. Sem novas features de escrita.

## Escopo

- Remover campo `apiUrl` da entidade `Cluster` (ADR-0001)
- Montar ambiente local com docker-compose para desenvolvimento
- Implementar `DashboardView` com cards de resumo dos clusters
- Documentar aviso de kubeconfig com certs por caminho na UI

## Critério de conclusão

O desenvolvedor consegue rodar o app localmente, fazer login, registrar um cluster via kubeconfig, e visualizar pods, deployments e namespaces sem erros.

## Issues

- [01 — Remover apiUrl do Cluster](./issues/01-remover-api-url.md)
- [02 — Docker Compose local](./issues/02-docker-compose-local.md)
- [03 — DashboardView com cards de clusters](./issues/03-dashboard-view-cards.md)
- [04 — Aviso de kubeconfig com certs por caminho](./issues/04-aviso-kubeconfig-certs.md)
