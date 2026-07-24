# Wizard Deploy from Dockerfile — 6 passos de configuração

Status: done

Criar `DeployFromDockerfileView` na rota `/deploy/dockerfile`, protegida por `PROJECT_DEPLOY_APPLICATION`. O wizard segue o mesmo padrão visual de `DeployApplicationView` (step indicator, botões Back/Next/Build & Deploy, max-width 820px).

Os seis passos são:

**Step 1 — Source + Name**: Git Repository URL, branch (padrão `main`), Dockerfile path (padrão `Dockerfile`), context path (padrão vazio = raiz do repositório), e nome da aplicação (Namespace). O nome da aplicação segue o mesmo padrão de validação: lowercase, números e hífens, máximo 63 chars.

**Step 2 — Image & Port**: tag da imagem, auto-sugerida como `<namespace>/<namespace>:latest` a partir do nome preenchido no Step 1 (editável pelo usuário). Porta do container (opcional, inteiro 1–65535).

**Step 3 — Resources**: replicas (mínimo 1, padrão 1), CPU request/limit e memória request/limit com os mesmos defaults de `DeployApplicationView`.

**Step 4 — Volume**: checkbox "Add persistent storage" com StorageClass (carregada do cluster, default pré-selecionado), tamanho em Gi (mínimo 1, padrão 1) e mount path (padrão `/data`). Campos só visíveis quando checkbox marcado.

**Step 5 — External Access**: checkbox "Expose application externally (Ingress)" com host (auto-sugerido como `<namespace>.greencap.local`) e IngressClass (carregado do cluster). O host é auto-sugerido a partir do nome no Step 1, seguindo o mesmo comportamento de `DeployApplicationView`. Campos só visíveis quando checkbox marcado.

**Step 6 — Review**: lista todos os recursos que serão criados (Namespace, Deployment, Service se porta informada, PVC se volume habilitado, Ingress se external access habilitado). Botão "Build & Deploy" no lugar de "Next". A fase de execução (build + deploy) é coberta pela issue 03.
