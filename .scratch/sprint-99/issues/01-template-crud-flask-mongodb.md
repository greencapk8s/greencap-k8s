# 01 — Novo Template: CRUD in Python (Flask) + MongoDB

Status: done

Novo Template no catálogo (repositório `greencap-templates`), análogo ao `crud-flask-postgres` existente, trocando o datastore relacional por um documental. Objetivo didático: comparar diretamente os dois templates — mesma aplicação, mesma entidade, bancos diferentes — pra evidenciar o que muda entre uma conexão relacional e uma documental dentro do mesmo padrão de Deployment stateless + storage stateful na Namespace.

Id/Namespace: `crud-flask-mongodb`. Entidade idêntica ao template Postgres (`items`: `name` e `description`), mesmas rotas de listagem/criação/edição/remoção, mesma interface HTML servida pelo próprio Flask. Persistência via `pymongo` (driver direto, sem ODM, espelhando o uso direto de `psycopg2` no template existente), com retry de conexão no boot (Mongo pode ainda estar subindo quando o container do backend inicia, mesmo raciocínio do `init_db()` atual).

MongoDB roda com autenticação obrigatória (usuário/senha root via Secret, mesmo padrão do `postgres-credentials`), imagem `mongo:7.0` — validar antes de fixar se há uma versão estável mais recente disponível — e uma PVC de 1Gi pra persistência (mesmo tamanho usado no Postgres do template atual). Backend buildado via Kaniko com a imagem-sentinela `__BUILD__backend`, mesmo mecanismo do template existente. Ingress fixo em `crud-flask-mongodb.greencap.local`.

Entrada em `catalog.json`: título "CRUD in Python (Flask) + MongoDB", descrição mencionando o template Postgres como comparação (mesmo padrão de Namespace, datastore documental em vez de relacional), technologies `["Python", "Flask", "MongoDB"]`.

Todo o trabalho é feito no repositório `greencap-templates` (fora do `greencap-k8s`) — o mecanismo de Deploy Template já é genérico (ADR 0015), então não é esperada nenhuma mudança de código em `greencap-k8s` pra esta entrega.
