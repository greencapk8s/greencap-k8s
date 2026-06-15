---
id: "01"
title: "Infraestrutura de Demo — PVC para persistir o Container Registry interno"
status: done
labels: [chore, infra]
sprint: 71
---

## Contexto

O addon `registry` do minikube (habilitado na Sprint 68 em `cluster-provision.sh`) não tem armazenamento persistente — o `Deployment registry` em `kube-system` não declara `volumes`. Quando o pod é recriado (restart, reschedule), o conteúdo de `/var/lib/registry` é perdido, e o catálogo (`/v2/_catalog`) volta vazio. Confirmado em produção do demo: imagens enviadas em sessão anterior (Sprint 68) não existiam mais no dia seguinte.

Decisões da sessão `/grill-with-docs`:

- **Abordagem**: manter `minikube addons enable registry` como hoje; após habilitar, criar uma `PersistentVolumeClaim` em `kube-system` (StorageClass `standard`, **4Gi**) e usar `kubectl patch` (strategic merge) no `Deployment registry` para montar essa PVC em `/var/lib/registry`.
- **Validado empiricamente no cluster `greencap-demo`**:
  - Não existe `kube-addon-manager` rodando neste cluster (versões recentes do minikube não fazem reconcile contínuo) — o label `addonmanager.kubernetes.io/mode: Reconcile` é vestigial.
  - `volumes`/`volumeMounts`/`nodeSelector` adicionados via `kubectl patch` sobrevivem a uma reexecução de `minikube addons enable registry` (merge de 3 vias do `kubectl apply` não remove campos que nunca fizeram parte do manifest do addon).
- **Achado crítico**: a StorageClass `standard` (hostpath-provisioner) cria PVs **sem `nodeAffinity`**. Em cluster multi-node, se o pod do registry for reagendado para outro node, o diretório hostPath local fica vazio e os dados "somem" (mesmo a PVC continuando `Bound`). Fix validado: adicionar `nodeSelector: kubernetes.io/hostname: greencap-demo` (control-plane, nome estável = nome do profile) — o pod sempre retorna ao mesmo node e os dados persistem.
- Problema geral de `nodeAffinity` da StorageClass (afeta qualquer PVC, não só o registry) registrado como candidato de backlog (`docs/sprints.md`, "Infraestrutura de Demo — follow-up da Sprint 71") para avaliação futura de `local-path-provisioner`. ODF/Ceph avaliado e descartado — over-engineering para o posicionamento "plataforma leve" do GreenCap.
- Tudo inline em `cluster-provision.sh` (PVC via heredoc + `kubectl patch --patch-file` via heredoc) — sem novo diretório de manifests, mesmo padrão autocontido do script atual.
- Sem alterações em `CONTEXT.md`/`src/` — escopo é puramente infraestrutura de demo.

## Entrega

### `samples/greencap-demo/cluster-provision.sh`

- Após `minikube addons enable registry`:
  - Aplicar (via heredoc) uma `PersistentVolumeClaim` `registry-storage` em `kube-system`, `storageClassName: standard`, `accessModes: [ReadWriteOnce]`, `requests.storage: 4Gi`.
  - Aplicar (via `kubectl patch deployment registry -n kube-system --type=strategic --patch-file -` com heredoc) um patch adicionando:
    - `spec.template.spec.volumes`: `registry-storage` → `persistentVolumeClaim.claimName: registry-storage`
    - `spec.template.spec.containers[name=registry].volumeMounts`: `registry-storage` em `/var/lib/registry`
    - `spec.template.spec.nodeSelector`: `kubernetes.io/hostname: greencap-demo`
  - `kubectl rollout status deployment/registry -n kube-system --timeout=120s` (mesmo padrão do wait do `ingress-nginx-controller`).
  - Comentários no script (em inglês, por que — não o quê) explicando: (1) por que o patch é aplicado após o `addons enable` em vez de um manifest próprio, e (2) por que o `nodeSelector` é necessário (hostpath sem `nodeAffinity` em multi-node).

### `samples/greencap-demo/README.md`

- Nova seção `## Container Registry` (após "Driver recomendado" ou em "Troubleshooting"):
  - Registry agora persiste imagens via PVC `registry-storage` (4Gi, `kube-system`).
  - Caveat do `nodeSelector`: pod fixado no control-plane (`greencap-demo`) para preservar dados do hostPath entre recriações; aceitável pois o control-plane sempre existe no demo de 3 nodes.
  - Dados são perdidos apenas com `minikube delete -p greencap-demo` (destruição completa do cluster — esperado).

## Critérios de aceite

- `cluster-provision.sh` roda do zero (cluster novo) e em um cluster já existente (idempotente — sem erro, sem recriar PVC já existente, patch não duplica volumes).
- `kubectl get pvc registry-storage -n kube-system` → `Bound`, `4Gi`.
- `kubectl get deployment registry -n kube-system -o yaml` → contém `volumes`/`volumeMounts` para `registry-storage` e `nodeSelector.kubernetes.io/hostname: greencap-demo`.
- Aceite manual:
  1. Build + push de uma imagem de teste via port-forward (mesmo fluxo da Sprint 68).
  2. Imagem aparece em "Container Registry" na UI do GreenCap.
  3. `kubectl delete pod -n kube-system -l actual-registry=true` (força recriação).
  4. Imagem continua aparecendo na UI após o pod voltar.
