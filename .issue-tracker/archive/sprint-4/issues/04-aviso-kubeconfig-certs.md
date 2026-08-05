# 04 — Aviso de kubeconfig com certs por caminho

Status: done

## Contexto

Kubeconfigs que referenciam certificados por caminho de arquivo (ex: `certificate-authority: /home/user/.minikube/ca.crt`) funcionam na máquina local do desenvolvedor mas falham no GreenCap — o servidor não tem acesso a esses caminhos.

A solução para o usuário é usar `kubectl config view --flatten --minify` antes de fazer upload, que incorpora os certificados inline no kubeconfig.

## O que fazer

- Exibir um aviso informativo no dialog de adição de cluster na `ClustersView`
- Texto sugerido: _"Se seu kubeconfig referencia certificados por caminho de arquivo, use `kubectl config view --flatten --minify` para gerar uma versão portável antes de fazer o upload."_
- Estilo: componente `Details` (acordeão) ou mensagem de `Tooltip` no campo de upload

## Critério de aceite

- Usuário vê o aviso ao abrir o dialog de adição de cluster
- Aviso não atrapalha o fluxo principal (não é bloqueante)

## Observação

Marcado como `ready-for-human` pois envolve decisão de UX sobre onde e como exibir o aviso.
