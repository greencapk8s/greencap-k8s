---
id: "01"
title: "Infraestrutura de Demo: migrar greencap-demo para driver docker multi-node"
status: done
labels: [chore, samples]
sprint: 65
depends_on: []
---

## Contexto

`samples/greencap-demo/cluster-provision.sh` usa `--driver=virtualbox`, herdado do spec original da sprint 50 (3 nodes, 1cpu/2048MB cada). O candidato registrado em `docs/sprints.md` (seção "🔧 Infraestrutura de Demo") aponta um bug conhecido do virtualbox no Linux: o DHCP da rede host-only não atribui IP às VMs extras após reboot do host, então clusters multi-node não voltam saudáveis automaticamente.

Hoje o script já está reduzido para `NODES=1` (contorno informal, fora do spec original), e o profile `greencap-demo` ativo no host roda 1 node com virtualbox (4096MB/2cpu reservados, VM em execução).

Decisões via `/grill-with-docs`:

- **Driver**: trocar para `docker`. No Linux com Docker instalado, `docker` já é o driver auto-detectado por padrão pelo minikube — `virtualbox` só está em uso porque o script o especifica explicitamente. O driver `docker` elimina a classe de bug (sem VM, sem rede host-only/DHCP própria; a rede `minikube` é gerida pelo `dockerd`/systemd e sobrevive a reboot). `kvm2` foi avaliado como alternativa mas descartado para esta sprint — exigiria instalar libvirt, sem ganho claro sobre `docker` neste host.
- **Nodes/sizing**: 3 nodes (control-plane + 2 workers) — fiel ao spec original e necessário para validar a classe de bug multi-node (1 node não a exercitaria). `2 cpu / 2048MB` por node (~6GB total) — `1cpu`/`1536MB` planejados originalmente bateram nos mínimos do minikube (`RSRC_INSUFFICIENT_CORES` exige ≥2 cpu, `RSRC_INSUFFICIENT_REQ_MEMORY` exige ≥1800MB), ajustado durante a implementação. Antes de provisionar, deletar o profile virtualbox atual (`minikube delete -p greencap-demo`) para liberar os 4096MB/2cpu reservados pela VM em execução.
- **Fix relacionado**: a mensagem final do script aponta para `create.sh`, mas o arquivo real é `create-demo.sh` — corrigir nesta mesma edição. Também encontrado durante a validação: `add-hosts.sh` chama `minikube ip` sem `-p greencap-demo`, falhando se `greencap-demo` não for o profile default do minikube — corrigido para `minikube ip -p greencap-demo`, mesmo padrão de `cluster-provision.sh`/`create-demo.sh`.
- **Escopo de validação**: não só o provisionamento — o fluxo completo do demo (`cluster-provision.sh` → `create-demo.sh` → `add-hosts.sh` → acesso via browser em `greencap-demo.local`) precisa continuar funcionando com driver docker + 3 nodes. Risco conhecido: o `ingress-nginx-controller` pode ser escalonado em qualquer um dos 3 nodes (cada um é um container Docker com IP próprio na rede `minikube`), o que pode afetar o acesso via `minikube ip`. Se isso se manifestar, documentar o workaround no README.
- **Teste de reboot**: não é executado durante a implementação (reiniciar o host é disruptivo). Fica como critério de **aceite manual** — o usuário reinicia o host e confirma que o cluster (3 nodes) e o demo voltam saudáveis automaticamente, sem reprovisionar.
- **Documentação**: novo `samples/greencap-demo/README.md` com quick start, driver recomendado (tabela de trade-offs docker/virtualbox/kvm2), troubleshooting do bug histórico do virtualbox, e requisitos (docker, minikube, kubectl, ~5GB livres).
- **Fora de escopo**: `CONTEXT.md`/ADR — decisão de infraestrutura de `samples/`, não de domínio do produto, facilmente reversível.
- Atualizar a referência de issue quebrada no candidato "Infraestrutura de Demo" em `docs/sprints.md` (apontava para `.scratch/sprint-50/issues/03-minikube-multinode-driver-validation.md`, que nunca existiu) para esta issue.

## Entrega

### `samples/greencap-demo/cluster-provision.sh`

- `DRIVER="docker"` (era `virtualbox`)
- `NODES=3` (era `1`)
- `CPUS=2`, `MEMORY=2048` (era `2`/`4096`)
- Corrigir `create.sh` → `create-demo.sh` na mensagem final

### `samples/greencap-demo/add-hosts.sh`

- `minikube ip` → `minikube ip -p greencap-demo`
- Endurecido: `set -euo pipefail` + valida que a saída de `minikube ip` é um IPv4 antes de gravar em `/etc/hosts` — encontrado no aceite manual: uma corrida com `create-demo.sh` ainda em andamento fez `minikube ip` imprimir uma mensagem de erro no stdout, que foi gravada literalmente em `/etc/hosts`

### `samples/greencap-demo/README.md` (novo)

- Quick start: ordem `cluster-provision.sh` → `create-demo.sh` → `add-hosts.sh` → acessar `http://greencap-demo.local` → `delete-demo.sh` para limpar
- Driver recomendado: `docker`, com tabela de trade-offs (docker / virtualbox / kvm2) — networking multi-node, sobrevivência a reboot, isolamento, disponibilidade no host
- Troubleshooting: bug do virtualbox (DHCP host-only após reboot em multi-node)
- Requisitos: docker, minikube, kubectl; ~5GB de RAM livre para os 3 nodes

### `docs/sprints.md`

- Atualizar a referência de issue do candidato "Infraestrutura de Demo" para `.scratch/sprint-65/issues/01-migrar-driver-docker-multinode.md`

## Validação (implementação)

- [x] `minikube delete -p greencap-demo` (remove profile virtualbox atual) — feito, 4096MB/2cpu liberados
- [x] Rodar `cluster-provision.sh` novo → `minikube profile list` mostra `greencap-demo`, driver `docker`, 3 nodes, status `OK`
- [x] Rodar `create-demo.sh` → rollout de `redis`/`backend`/`frontend` e addon `ingress` OK; `ingress-nginx-controller` escalonado no control-plane (`greencap-demo`, mesmo node de `minikube ip`) — risco do ingress cair em worker node não se manifestou
- [x] Rodar `add-hosts.sh` (após fix do `-p greencap-demo`) — requer `sudo`, não disponível no sandbox de implementação; validado via `curl --resolve greencap-demo.local:80:192.168.49.2 http://greencap-demo.local/` → `HTTP 200`. Usuário deve rodar `add-hosts.sh` manualmente para popular `/etc/hosts`
- [x] Confirmar app acessível em `http://greencap-demo.local` no browser — confirmado no aceite manual

## Critérios de aceite manual

- [x] Rodar `./add-hosts.sh` (atualiza `/etc/hosts` com o IP do novo cluster — `192.168.49.2`; remover manualmente a entrada antiga de `greencap-demo.local` se houver, ex.: `192.168.59.131` do virtualbox)
- [x] Confirmar `http://greencap-demo.local` acessível no browser
- [x] Reiniciar o host
- [x] Após o boot, cluster (3 nodes) volta `Running`/`OK` sem rodar `cluster-provision.sh` novamente — requer `minikube start -p greencap-demo` manual (driver `docker` não religa os containers automaticamente no boot); documentado no README. Considerado aceitável pelo usuário — bem diferente do bug do virtualbox, que exigia reprovisionar o cluster inteiro
- [x] `http://greencap-demo.local` continua acessível após o `minikube start -p greencap-demo`
