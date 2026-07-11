# 03 — Workflow GitHub Actions validando setup.sh em Linux e macOS

Status: in-progress

Novo workflow (`.github/workflows/setup-script-validate.yml`, seguindo o mesmo estilo de `docker-compose-validate.yml`: jobs nomeados, `timeout-minutes`, trigger por push/PR em `main`/`staging`/`develop` filtrado por path) que executa o setup completo do GreenCap via `setup/setup.sh` de ponta a ponta, numa matrix cobrindo `ubuntu-24.04` e `macos-14`. Depende das issues 01 (suporte macOS) e 02 (modo não-interativo) — sem elas o script não completa sem prompts nem falha explicitamente fora do Linux.

Versões de runner fixas, não `ubuntu-latest`/`macos-latest` — essas tags são realocadas pelo GitHub para uma imagem mais nova sem aviso, o que arrisca invalidar silenciosamente as premissas específicas de Apple Silicon que este workflow valida (virtualização acelerada do Colima, `TARGETARCH=arm64` no `docker/Dockerfile`). Pelo mesmo motivo, `docker-compose-validate.yml` (pré-existente, fora do escopo formal desta sprint) também migrou de `ubuntu-latest` para `ubuntu-24.04` nos dois jobs.

O job roda `setup.sh` com `AUTO_INSTALL=y` e `PROFILE_CHOICE=1` (profile Minimal — 1 node, 2 CPUs, 4GB — escolhido para caber com folga nos runners padrão e reduzir tempo/flakiness; não testa o caminho multi-node). O auto-install não é contornado pré-instalando dependências no workflow — o próprio `setup.sh` deve instalar tudo que falta (Docker/Colima, kubectl, minikube, helm), exercitando de fato os instaladores em cada plataforma, que é o objetivo central desta sprint. No Linux, o runner já vem com Docker pronto (só kubectl/minikube/helm serão instalados pelo script); no macOS, nada vem pronto, então o branch inteiro do instalador é exercitado.

Após o setup completar, o workflow valida que a aplicação está de fato servindo tráfego — `curl --fail -L http://greencap.local`, reaproveitando a entrada em `/etc/hosts` que o próprio `setup.sh` grava, mesmo caminho que um usuário real percorre. Em caso de falha, um step de diagnóstico (`if: failure()`) despeja logs relevantes (`kubectl get pods -A`, `kubectl logs`, `minikube logs`) antes de encerrar o job, para tornar falhas de CI depuráveis sem precisar reproduzir localmente.

Como último step, o workflow chama `teardown.sh` (com `CONFIRM=yes`, via issue 02) para validar que ele também funciona nas duas plataformas — mesmo o runner sendo destruído ao fim do job de qualquer forma, isso garante que o script de teardown em si não está quebrado.
