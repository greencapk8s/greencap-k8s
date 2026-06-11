---
id: "01"
title: "Criar cluster-provision.sh para o perfil greencap-demo"
status: done
labels: [feat, samples]
sprint: 50
---

## Contexto

O `samples/greencap-demo/create.sh` aplica manifests mas pressupõe que o minikube já está rodando. Falta um script dedicado ao provisionamento do cluster local.

## Entrega

Criar `samples/greencap-demo/cluster-provision.sh` que:

- Inicia o minikube com perfil `greencap-demo`
- Driver: `virtualbox`
- 3 nodes
- Cada node: `--cpus=1 --memory=2048`
- Exibe mensagem de sucesso ao final com instruções para executar o `create.sh`

## Critérios de aceite

- Executar o script cria o cluster sem erros
- `minikube profile list` mostra o perfil `greencap-demo` com 3 nodes
- O `create.sh` continua funcionando normalmente após o provisionamento
