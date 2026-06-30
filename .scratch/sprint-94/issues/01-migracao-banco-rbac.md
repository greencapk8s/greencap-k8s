# 01 — Migração de banco para modelo RBAC

Status: done

A tabela `user_permissions` e todas as permissões granulares associadas são removidas do banco de dados. Em substituição, a tabela `users` recebe três novas colunas para representar a identidade Kubernetes do usuário: o nome da ServiceAccount criada no cluster, o nome do ClusterRole atribuído e o token da ServiceAccount armazenado de forma encriptada.

A migration deve ser destrutiva e irreversível — não há dados de permissões a migrar, pois o modelo de autorização passa a ser o Kubernetes RBAC. O usuário admin não terá valores nessas novas colunas (nullable), já que continua usando o kubeconfig do cluster diretamente.
