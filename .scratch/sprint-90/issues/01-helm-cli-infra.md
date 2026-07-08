---
id: "90-01"
title: "Dockerfile e setup.sh — instalação do Helm CLI"
status: done
priority: high
sprint: 90
---

O `Dockerfile` recebe a instalação do Helm no builder stage: download do binário oficial via script `get-helm-3` (ou via release direto), copiado para o runtime stage em `/usr/local/bin/helm`. Nenhuma dependência nova no runtime layer além do binário.

O `setup.sh` passa a verificar se `helm` está instalado no PATH e exibe link de instalação caso ausente, seguindo o mesmo padrão de verificação de `docker`, `kubectl` e `minikube`.
