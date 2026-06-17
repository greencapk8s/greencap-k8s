---
title: ImportComposeView — wizard 3 passos integrado ao DeployApplicationView
status: open
sprint: 83
---

A `DeployApplicationView` existente ganha um segundo modo de entrada: "Deploy from image" (fluxo atual) e "Import Compose" (novo). A seleção entre os dois modos é feita no topo da view, antes de qualquer passo do wizard.

O wizard de Import Compose tem três passos:

**Passo 1 — Fonte e destino:** o usuário informa a URL do Git Repository público, o branch (default: `main`) e o path opcional para o `docker-compose.yml` dentro do repositório (default: `docker-compose.yml`). Também informa o nome do Namespace de destino. Ao avançar, o GreenCap busca e faz o parse do arquivo; erros de fetch ou parse são exibidos inline sem avançar.

**Passo 2 — Revisão:** a tela mostra um painel por serviço com os recursos que serão criados (Deployment, Service, ConfigMap, Secret, PVC), com campos editáveis para: nome da imagem (serviços com `build:`), tamanho e StorageClass dos PVCs, e classificação sensível/não-sensível das variáveis de ambiente. Uma seção consolidada "Diretivas ignoradas" lista o que foi encontrado no Compose mas não traduzido (bind mounts, `depends_on:`, `networks:`, etc.) com aviso visual mas sem bloquear o avanço. Serviços com `build:` exibem o nome da imagem que será produzida, editável.

**Passo 3 — Execução:** para serviços com `build:`, os Jobs Kaniko são criados e acompanhados com log live sequencialmente. Após todos os builds concluírem (com sucesso ou falha), os recursos Kubernetes são criados para os serviços cujos builds tiveram sucesso. Um resumo final mostra o resultado por serviço e por recurso. Em sucesso total, navega automaticamente para a Topologia do Namespace criado. Em falha parcial, permanece na tela de resultado com botão "Ver Topologia" para navegação manual.

Notificações seguem `Notification.Position.BOTTOM_END`. A view é protegida por `PROJECT_DEPLOY_APPLICATION`.
