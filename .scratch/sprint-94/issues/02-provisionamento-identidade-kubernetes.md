# 02 — Provisionamento de identidade Kubernetes por usuário

Status: done

Ao criar um usuário no GreenCap, o sistema deve provisionar automaticamente no cluster designado uma ServiceAccount no namespace `greencap-system`, um ClusterRoleBinding que vincula essa ServiceAccount ao ClusterRole escolhido pelo admin, e um token de longa duração para essa ServiceAccount. O token é encriptado e armazenado no registro do usuário no banco.

Ao desativar um usuário, o sistema deve remover do cluster a ServiceAccount e o ClusterRoleBinding correspondentes, revogando o acesso de forma imediata e real — não apenas no nível da plataforma.

O cliente Fabric8 utilizado para as operações de qualquer usuário logado deve ser construído com as credenciais daquele usuário: kubeconfig do cluster para o admin, token da ServiceAccount para os demais. Isso garante que o Kubernetes RBAC seja o único guardião de acesso para operações sobre recursos do cluster.
