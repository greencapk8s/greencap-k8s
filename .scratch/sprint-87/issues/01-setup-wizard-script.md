---
id: "87-01"
title: "Setup wizard: script principal e teardown"
status: done
priority: high
sprint: 87
---

O GreenCap precisa de um ponto de entrada único para usuários que querem rodar a plataforma localmente via minikube. Atualmente não existe nenhum mecanismo de instalação além do docker-compose (que não usa Kubernetes).

O `setup/setup.sh` deve ser autoexplicativo, declarativo por etapas e re-executável sem efeitos colaterais em caso de re-run. O `setup/teardown.sh` permite ao usuário desfazer tudo com um único comando.

**Escopo do `setup.sh`:**

Etapa 1 — verifica se `docker`, `minikube` e `kubectl` estão instalados no PATH; em caso de ausência, exibe link de instalação e encerra.

Etapa 2 — exibe menu de perfil de instalação e aguarda escolha do usuário:
- Minimal: 1 nó, 2 CPUs, 4 GB
- Recommended: 3 nós, 2 CPUs, 3 GB/nó
- Custom: usuário informa nós, CPUs e memória via `read`

Etapa 3 — inicia minikube com profile `greencap-platform` e o perfil escolhido (driver `docker`); idempotente — pula se o profile já existe.

Etapa 4 — habilita addons `metrics-server`, `ingress` e `registry`; aguarda o ingress-nginx controller ficar pronto; aplica PVC de 8 Gi para persistência do registry e faz patch no Deployment do registry (mesmo padrão do `cluster-provision.sh` do greencap-demo, com nodeSelector e volumeMount).

Etapa 5 — faz `docker build` do `docker/Dockerfile` e push para `localhost:5000/greencap-platform/platform:latest` via registry-proxy do minikube; idempotente — pula o push se a tag já existir no registry.

Etapa 6 — gera `GREENCAP_ENCRYPTION_KEY` e senha do Postgres via `openssl rand -hex 16` caso não estejam definidas como variáveis de ambiente; aplica o Secret `greencap-secrets` no namespace `greencap-platform`.

Etapa 7 — aplica os manifests em `setup/manifests/` (namespace, Postgres, GreenCap); aguarda rollout de todos os Deployments.

Etapa 8 — exibe resumo: URL (`http://greencap.local`), credenciais padrão (`admin`/`admin`), comando sudo para `/etc/hosts` e a `ENCRYPTION_KEY` gerada (com aviso de guarda).

**Escopo do `teardown.sh`:**

Exibe aviso de destruição e aguarda confirmação (`y/N`); deleta o minikube profile `greencap-platform` completamente.
