---
title: "Topologia: tabela topology_layouts e camada de domínio"
status: done
sprint: 49
---

## Problema

O estado visual da Topologia (posições dos nós e toggle "Group by labels") é perdido a cada navegação. Não existe estrutura no banco para persistir TopologyLayout por user+cluster+namespace.

## Solução

Criar a tabela `topology_layouts` via Flyway e a camada de domínio correspondente.

## Entregáveis

- [ ] Migration `V16__add_topology_layouts.sql` com colunas: `id`, `user_id`, `cluster_id`, `namespace`, `node_positions` (TEXT), `grouping_enabled` (BOOLEAN), `updated_at`
- [ ] Entidade JPA `TopologyLayout` com chave única em `(user_id, cluster_id, namespace)`
- [ ] `TopologyLayoutRepository` com método `findByUserIdAndClusterIdAndNamespace`
- [ ] `TopologyLayoutService` com método de upsert: salva ou atualiza o layout existente
