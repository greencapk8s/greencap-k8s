# 03 — SampleCatalogView: lista de cards em Developer Experience

Status: done

Depende da issue 02.

Nova view Vaadin em Developer Experience, ao lado de Kubernetes Operators no sidebar, seguindo a rota já anotada no `CONTEXT.md` (`Templates Catalog`). Exibe os Templates retornados por `SampleCatalogService` como uma lista de cards — título, descrição e badges de tecnologia por card — sem paginação ou filtro nesta primeira entrega (catálogo pequeno, um único Template seed).

Cada card mostra um badge "Installed" (tema `badge` + variante `contrast`, seguindo a convenção do projeto) quando o Template já está implantado no Cluster ativo, e desabilita a ação de deploy nesse caso. Sem gate de permissão na view — consistente com a ADR 0013 (RBAC substituindo o sistema de permissões interno): a view fica visível a qualquer usuário autenticado, e a autorização real acontece na chamada Kubernetes durante o deploy (issue 04).

Carregamento da lista de cards via `AsyncTasks.execute(...)` (padrão já consolidado no projeto desde a Sprint 96), com o banner de "cluster inacessível" já usado em outras views quando a busca falhar.
