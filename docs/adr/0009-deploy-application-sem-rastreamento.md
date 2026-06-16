# ADR 0009 — Deploy Application sem rastreamento no GreenCap

**Status:** Accepted

## Context

A feature "Deploy Application" provisiona múltiplos recursos Kubernetes (Namespace, Deployment, Service, PVC, Ingress) em uma única operação wizard. Uma alternativa natural seria o GreenCap rastrear esses conjuntos como entidades próprias ("Applications") no banco de dados — o que habilitaria features futuras como listagem de "minhas aplicações", teardown completo, upgrade de imagem, etc.

## Decision

Os recursos criados pelo wizard são objetos Kubernetes padrão, sem nenhum rastreamento especial no GreenCap. Após a criação, são gerenciados individualmente pelas views existentes (Deployments, Services, PVCs, Ingresses) exatamente como qualquer outro recurso do cluster.

## Rationale

Introduzir rastreamento agora criaria acoplamento prematuro: o GreenCap precisaria manter sincronismo entre seu banco e o estado real do cluster (o usuário pode deletar o Deployment diretamente via kubectl ou pela view de Workloads, quebrando a consistência da entidade "Application" no banco). Esse problema de sincronismo é não trivial e não há requisito concreto que o justifique neste momento.

O catálogo futuro de soluções pré-prontas e o dashboard de "minhas aplicações" podem introduzir rastreamento quando a necessidade for real — e com um modelo de dados informado por casos de uso concretos, não antecipados.

## Consequences

- Não existe uma listagem "Applications" no GreenCap — os recursos são visíveis nas views existentes por Namespace.
- Teardown de uma aplicação completa requer deletar os recursos individualmente (ou deletar o Namespace inteiro).
- Adicionar rastreamento retroativamente exigirá uma estratégia de adoção para recursos já existentes nos clusters.
