---
title: ImportComposeService — tradução do modelo intermediário para recursos Kubernetes
status: done
sprint: 83
---

O `ImportComposeService` recebe o modelo intermediário produzido pelo parser (issue 01) e um request de configuração editado pelo usuário na tela de revisão, e provisiona todos os recursos Kubernetes no cluster ativo.

Cada serviço do Compose resulta em um Deployment. Quando o serviço declara `ports:`, um Service ClusterIP é criado. Quando há variáveis de ambiente, um ConfigMap (`<service>-config`) e/ou um Secret (`<service>-secret`) são criados conforme a classificação do parser. Quando há named volumes, um PersistentVolumeClaim (`<service>-pvc`) é criado com o StorageClass e tamanho informados pelo usuário (default: StorageClass default do cluster, 1Gi).

Todos os recursos recebem as labels `app.kubernetes.io/part-of: <namespace>` e `app.kubernetes.io/component: <service-name>`.

Serviços com `build:` exigem que o Build Kaniko correspondente (issue 03) já tenha sido executado com sucesso antes da criação dos recursos — a imagem resultante é a referência usada no Deployment.

O serviço tenta criar os recursos de todos os serviços antes de retornar, mesmo que algum falhe. O resultado acumula os recursos criados com sucesso e os que falharam, sem rollback. A lógica de falha parcial segue o padrão de `DeployApplicationResult`.

O Namespace é criado antes de qualquer outro recurso, como pré-condição.
