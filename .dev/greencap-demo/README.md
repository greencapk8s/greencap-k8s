# greencap-demo

Cluster minikube local com uma aplicação de exemplo (frontend + backend + redis), usada para validar o GreenCap K8s ponta a ponta.

## Requisitos

- `minikube`, `kubectl` instalados
- `docker` instalado e em execução (driver recomendado — ver abaixo)
- ~5GB de RAM livre (cluster de 3 nodes, 2GB cada)

## Quick start

```bash
./cluster-setup.sh       # cria o cluster minikube (3 nodes, driver docker) e habilita os addons
./create-demo.sh         # aplica os manifests do demo
./add-hosts.sh           # adiciona "greencap-demo.local" ao /etc/hosts (pede sudo)
```

Acesse `http://greencap-demo.local` no navegador.

Para remover os recursos da aplicação (mantendo o cluster):

```bash
./delete-demo.sh
```

Para remover o cluster por completo:

```bash
./cluster-teardown.sh
```

## Driver recomendado

`cluster-setup.sh` usa `--driver=docker`. No Linux, é o driver que o minikube já auto-detecta por padrão quando o Docker está instalado.

| | `docker` (recomendado) | `virtualbox` | `kvm2` |
|---|---|---|---|
| Networking multi-node | Rede bridge própria do Docker (`minikube`), gerenciada pelo `dockerd`/systemd | Rede host-only com DHCP próprio (`vboxnetdhcp`) | Rede do libvirt (`virbr*`), gerenciada por systemd |
| Sobrevive a reboot do host | Sim | Não — ver troubleshooting abaixo | Sim |
| Isolamento | Compartilha kernel do host (containers) | VM completa | VM completa |
| Dependências extras | Nenhuma (Docker já é requisito do projeto) | VirtualBox | libvirt + driver kvm2 |

`kvm2` é uma alternativa válida (isolamento de VM real, sem o bug de DHCP do virtualbox), mas não foi validado nesta sprint — exige instalar `libvirt` e o driver `kvm2`, sem ganho claro sobre `docker` para este demo.

## Container Registry

O addon `registry` do minikube é habilitado pelo `cluster-setup.sh` e persiste as imagens enviadas via uma `PersistentVolumeClaim` (`registry-storage`, 4Gi, `kube-system`), montada em `/var/lib/registry`. Sem essa PVC, o conteúdo do registry é perdido a cada vez que o pod `registry` é recriado.

O pod do `registry` é fixado (`nodeSelector`) no node control-plane (`greencap-demo`). A `StorageClass` `standard` (hostpath-provisioner do minikube) cria volumes sem `nodeAffinity` — em um cluster multi-node, se o pod fosse reagendado para outro node, ele montaria um diretório local vazio nesse node e "perderia" os dados, mesmo com a PVC permanecendo `Bound`. Fixar o pod em um node estável garante que os dados do hostPath sejam preservados entre recriações do pod.

Como o control-plane sempre existe no demo de 3 nodes, essa fixação não traz problemas práticos. As imagens só são perdidas com `minikube delete -p greencap-demo` (destruição completa do cluster).

## Troubleshooting

### virtualbox: cluster multi-node não volta após reboot do host

Driver `virtualbox` no Linux tem um bug conhecido: a rede host-only usada pelo minikube depende de um servidor DHCP próprio (`vboxnetdhcp`) que não reinicia de forma confiável após o reboot do host. Em clusters multi-node, os nodes além do primeiro ficam sem IP e o cluster não volta saudável automaticamente — é necessário recriar o cluster manualmente.

Esse problema não ocorre com o driver `docker` (recomendado), pois não há VMs nem rede host-only — os nodes são containers na rede `minikube`, gerenciada pelo `dockerd` via systemd.

### docker: cluster não responde após reboot do host

Com o driver `docker`, os containers dos nodes ficam parados após o reboot (não voltam automaticamente). O cluster volta saudável sem reprovisionar, mas é necessário religar os nodes manualmente:

```bash
minikube start -p greencap-demo
```

Após esse comando, `minikube status -p greencap-demo` mostra os 3 nodes `Running`/`OK` e `http://greencap-demo.local` volta a responder sem passos adicionais.
