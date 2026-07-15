# 03 — Reescrita de UserManagementView para modelo RBAC

Status: done

A tela de gerenciamento de usuários deve ser acessível apenas ao admin (username `admin`). Qualquer outro usuário autenticado que tente acessar a rota diretamente deve ser redirecionado para a página inicial.

O formulário de criação de usuário perde o painel de permissões e ganha dois campos novos: seleção do cluster ao qual o usuário pertencerá, e seleção do ClusterRole a ser atribuído. O seletor de ClusterRole deve ser populado dinamicamente com os ClusterRoles disponíveis no cluster selecionado.

A grid de usuários substitui a coluna "Permissions" por uma coluna "Role" exibindo o ClusterRole atribuído. A ação "Edit Permissions" é substituída por "Edit Role", que permite ao admin trocar o ClusterRole de um usuário existente — o que implica removar o ClusterRoleBinding antigo e criar um novo no cluster.

O seletor de cluster no header da aplicação deve ser ocultado para usuários não-admin, já que cada usuário regular pertence a um único cluster fixado na criação.
