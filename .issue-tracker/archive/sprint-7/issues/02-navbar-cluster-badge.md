# 02 — Exibir cluster atual na navbar com badge de status

Status: done

## Contexto

A navbar superior deve mostrar o cluster ativo da sessão (nome + badge de ConnectionStatus) para que o usuário saiba de relance qual cluster está em uso e se está acessível. Atualmente o nome do cluster aparece na sidebar abaixo do logo — deve migrar para a navbar.

## O que fazer

- Remover `clusterNameSpan` da sidebar (`buildLogoSection`)
- Adicionar na navbar (`buildNavbar`) um componente com:
  - Nome do cluster ativo
  - Badge de `ConnectionStatus` (success/error/contrast seguindo o padrão de `ClustersView`)
- Quando nenhum cluster estiver ativo: exibir texto muted "Nenhum cluster ativo"
- O componente atualiza em `afterNavigation`

## Critério de aceite

- Navbar mostra nome + badge de status do cluster ativo
- Quando sem cluster ativo, exibe "Nenhum cluster ativo" em estilo secundário
- Atualiza ao navegar entre páginas
