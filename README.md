# GreenCap K8s

Plataforma web para gerenciamento e monitoramento de clusters Kubernetes.

## Quick Start

```bash
./setup/setup.sh
```

Um wizard interativo que provisiona um cluster Kubernetes real (via Minikube) e implanta o GreenCap dentro dele, pronto para uso — sem precisar instalar Java, Gradle ou configurar nada manualmente. Ao final, a aplicação estará disponível em `http://greencap.local`.

Pré-requisitos: apenas **Docker**. As demais ferramentas (`kubectl`, `minikube`, `helm`, `openssl`) são detectadas e, se ausentes, o próprio wizard oferece instalá-las (Linux e macOS).

### O que o wizard faz

O `setup.sh` conduz o provisionamento em 7 passos:

1. **Checking requirements** — verifica `docker`, `minikube`, `kubectl`, `helm` e `openssl`; oferece instalar as ferramentas ausentes automaticamente.
2. **Installation profile** — escolha do tamanho do cluster: `Minimal` (1 node, 2 CPUs, 4 GB), `Recommended` (3 nodes, 2 CPUs, 3 GB cada — padrão) ou `Custom` (você define nodes/CPUs/RAM).
3. **Starting minikube cluster** — sobe o cluster Minikube com o profile escolhido (reaproveita um cluster já existente, se houver).
4. **Enabling addons** — habilita `metrics-server`, `ingress`, `registry` (com PVC persistente de 8 Gi) e `olm`, aguardando cada um ficar pronto.
5. **Building and pushing GreenCap image** — builda a imagem do GreenCap a partir do `docker/Dockerfile` e publica no registry interno do cluster.
6. **Creating namespace and secrets** — cria o namespace `greencap-platform` e o Secret com a chave de encriptação, senha do banco e o kubeconfig do próprio cluster.
7. **Deploying Postgres and GreenCap** — aplica os manifests em `setup/manifests/`, aguarda o rollout do Postgres e do GreenCap.

Ao final, o script adiciona (ou atualiza) a entrada `greencap.local` em `/etc/hosts` apontando para o IP do Minikube.

Login padrão: `admin` / `admin` (altere após o primeiro acesso).

**Para desprovisionar:**

```bash
./setup/teardown.sh
```

## Vai alterar o código-fonte?

O fluxo acima é para rodar o GreenCap como usuário final. Para desenvolvimento (build local, Docker Compose, ambiente de demonstração), veja o [guia do desenvolvedor](.dev/README.md).
