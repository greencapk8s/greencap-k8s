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
- **Correção pós-implementação:** a premissa original desta ADR — de que rodar em hardware Apple real bastaria para a virtualização do Colima funcionar em CI — estava errada. Runners `macos-*` **hospedados padrão** do GitHub Actions rodam em hardware Apple real, mas o GitHub **desabilita virtualização aninhada** nesse tier (política/isolamento do runner compartilhado, independente do hardware por baixo) — `colima start` falha ali com `error starting vm: error at 'creating and starting': exit status 1` (driver `vz` sem entitlement de hypervisor). Confirmado empiricamente na primeira run real do `setup-script-validate.yml` (Sprint 100). Efeito prático: o suporte nativo a macOS no `setup.sh` continua funcionando normalmente num Mac real de um usuário; a validação de CI no matrix `macos-14` foi reduzida para cobrir só a instalação de dependências via Homebrew (`INSTALL_ONLY=true`), sem provisionar o cluster — documentado na issue 03 da Sprint 100. Runners macOS maiores (pagos) suportam virtualização aninhada e resolveriam isso, mas ficaram fora de escopo.
