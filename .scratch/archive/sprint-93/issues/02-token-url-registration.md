---
title: "Registro de cluster via Token + URL"
status: done
priority: high
sprint: 93
---

Adicionar suporte a registro de cluster via API server URL e bearer token de service account, como alternativa ao kubeconfig.

Internamente, GreenCap sintetiza um kubeconfig mínimo a partir da URL e do token — sem alteração de schema de credencial. Se o usuário fornecer um CA certificate (PEM ou base64), ele é incorporado ao kubeconfig sintetizado via `certificate-authority-data`; caso contrário, `insecure-skip-tls-verify: true`.

O dialog "Add Cluster" na `ClustersView` ganha um `TabSheet` com duas abas: "Kubeconfig" (fluxo atual) e "Token + URL" (novo). Ambas levam ao mesmo resultado: um cluster registrado com kubeconfig encriptado.
