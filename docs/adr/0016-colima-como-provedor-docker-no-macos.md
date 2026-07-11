# Colima como provedor de Docker no macOS, em vez de Docker Desktop

**Status:** Accepted

`setup/setup.sh` ganha instaladores nativos para macOS (Sprint 100), incluindo Docker — pré-requisito do `minikube --driver docker`. A instalação usa **Colima** (`brew install colima docker` + `colima start`), não Docker Desktop.

## Why

Docker Desktop é um app GUI: mesmo instalado via `brew install --cask docker`, o daemon só sobe depois de abrir o app manualmente e aceitar os termos de uso na primeira execução — inviável tanto para o workflow de CI (runner sem interação humana) quanto para a promessa de "plug and play" que `setup.sh` já entrega no Linux (`curl | bash` sem intervenção). Colima sobe um daemon Docker headless (VM Lima + Virtualization.framework) via linha de comando, sem esse atrito, e expõe o mesmo socket Docker — `minikube --driver docker` continua funcionando sem mudança de driver entre Linux e macOS.

## Considered Options

- **Docker Desktop** — rejeitado: requer interação GUI na primeira execução, não scriptável, inviável em CI.
- **Driver nativo do minikube sem daemon Docker** (`qemu2`/`vfkit`) — rejeitado: divergiria do driver usado no Linux (`docker`), duplicando lógica de addons/registry testada apenas contra o driver Docker.

## Consequences

- Usuários macOS passam a ter o Colima instalado como dependência do `setup.sh`, não o Docker Desktop que a maioria já conhece — vale uma nota no README/output do script explicando a escolha.
- Runners `macos-latest` do GitHub Actions rodam em hardware Apple real (não VMs aninhadas), então a virtualização acelerada do Colima funciona tanto em CI quanto na máquina de um usuário real, sem tratamento especial.
