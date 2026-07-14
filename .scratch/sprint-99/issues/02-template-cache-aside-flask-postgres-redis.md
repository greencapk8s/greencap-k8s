# 02 — Novo Template: Cache-aside in Python (Flask) + PostgreSQL + Redis

Status: done

Novo Template no catálogo (repositório `greencap-templates`) demonstrando o padrão cache-aside — não um CRUD "Redis puro". Decisão registrada no `CONTEXT.md` do `greencap-k8s` (verbete Template): Redis não deve ser modelado como datastore primário de um CRUD, é usado pelo seu papel idiomático (cache), com PostgreSQL como fonte de verdade.

Id/Namespace: `cache-aside-flask-postgres-redis`. Reaproveita a mesma aplicação/entidade do `crud-flask-postgres` (`items`: `name` e `description`) como base, adicionando uma camada de cache no endpoint de listagem (`GET /`, o mais acessado): leitura tenta a chave `items:all` no Redis antes de consultar o Postgres; na ausência (miss), consulta o Postgres e popula a chave com TTL de 60 segundos. Qualquer escrita (create/update/delete) invalida ativamente a chave `items:all` no Redis, além do TTL — as duas estratégias combinadas evitam tanto a janela de dado desatualizado pós-escrita quanto a dependência de uma invalidação perfeita em todo caminho de escrita.

Redis roda com autenticação obrigatória (`requirepass` via Secret, mesmo padrão do `postgres-credentials`), imagem `redis:7.4-alpine` — validar antes de fixar se há uma versão estável mais recente disponível — sem PVC: é um cache descartável, perder o conteúdo num restart de Pod é esperado e a próxima leitura repopula sozinha a partir do Postgres (dar persistência ao Redis reforçaria a confusão que motivou reformular este template). Backend buildado via Kaniko com a imagem-sentinela `__BUILD__backend`, mesmo mecanismo do template existente. Ingress fixo em `cache-aside-flask-postgres-redis.greencap.local`.

Entrada em `catalog.json`: título "Cache-aside in Python (Flask) + PostgreSQL + Redis", descrição mencionando o padrão cache-aside e a Namespace com mais de um componente stateful, technologies `["Python", "Flask", "PostgreSQL", "Redis"]`.

Todo o trabalho é feito no repositório `greencap-templates` (fora do `greencap-k8s`) — o mecanismo de Deploy Template já é genérico (ADR 0015), então não é esperada nenhuma mudança de código em `greencap-k8s` pra esta entrega.
