# 01 — Username do usuário logado no header

Status: done

Hoje o header (`MainLayout.buildNavbar`) mostra apenas o bloco "Cluster: nome [badge]" (`clusterInfoLayout`, atualizado em `updateClusterInfo()`), sem nenhuma indicação de qual usuário está autenticado na sessão. Esta entrega adiciona um bloco "User: `<username>`" à esquerda do bloco de Cluster, na mesma navbar.

O username exibido é o login já usado pelo restante do `MainLayout` (`SecurityContextHolder.getContext().getAuthentication().getName()`) — não requer novo campo em `User` nem migration, já que a entidade hoje só tem `username`/`email` e não há um "nome de exibição" separado.

Formato visual: mesmo padrão do bloco de Cluster — um `Span` label "User:" (`FontSize.SMALL` + `TextColor.SECONDARY`) seguido do `Span` com o username (`FontSize.SMALL` + `FontWeight.MEDIUM`). Fica em um layout próprio, construído uma vez (não dentro de `clusterInfoLayout`, que é limpo e reconstruído a cada `updateClusterInfo()`), posicionado antes de `clusterInfoLayout` na `HorizontalLayout` da navbar.

Visibilidade: o bloco de usuário é sempre visível, independente do cluster ativo — inclusive no estado "No active cluster", já que a identidade do usuário logado não depende do cluster selecionado.

Fora de escopo: qualquer alteração em `User`, `UserService` ou telas de gestão de usuário; o valor exibido é só o username de login.

Cobertura de teste (Karibu, estendendo o teste existente de `MainLayout` se houver, ou criando um novo): renderizar o header autenticado como um usuário conhecido e verificar que o texto do username aparece no bloco à esquerda do bloco de Cluster; verificar que o bloco permanece visível quando não há cluster ativo.
