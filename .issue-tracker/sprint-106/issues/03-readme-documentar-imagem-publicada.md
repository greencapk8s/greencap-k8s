# 03 — README: documentar a imagem publicada e os overrides do setup

Status: done

Com o `setup.sh` passando a puxar uma imagem publicada por padrão (issue 02), o README precisa refletir o novo comportamento — hoje ele descreve (ou pressupõe) que a instalação constrói a imagem localmente. Esta entrega atualiza a documentação para explicar que a instalação agora baixa uma imagem pronta de `ghcr.io/greencapk8s/platform`, tornando o setup mais rápido, e expõe as chaves de override para quem precisa de outro comportamento.

O que documentar, de forma enxuta e sem prometer detalhes de implementação: que o `setup.sh` puxa a imagem publicada em `amd64` e constrói localmente em arm64 ou quando `BUILD_LOCAL=true`; que `PLATFORM_IMAGE_TAG` fixa uma versão específica (ex. `PLATFORM_IMAGE_TAG=0.7.7`) em vez do `latest` padrão; e que, se o pull falhar, a instalação constrói localmente de forma automática — não trava. Mencionar que a imagem é pública (nenhuma autenticação é necessária para puxá-la).

Manter o tom e o posicionamento já estabelecidos no README (setup-first, plug-and-play): a mensagem principal é "a instalação agora é mais rápida porque baixa uma imagem pronta", com os overrides como nota para desenvolvedores, não como fluxo principal.

Cobertura de teste: entrega puramente documental — sem código, sem CI novo. Validação por revisão do texto no aceite manual.

Fora de escopo: documentar a troca do `docker-compose.yml` para a imagem publicada (essa mudança está no backlog; o README só será ajustado nesse ponto quando o follow-up for implementado).
