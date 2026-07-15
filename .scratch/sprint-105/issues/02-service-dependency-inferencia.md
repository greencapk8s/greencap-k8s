# 02 — Inferência de ServiceDependency (backend)

Status: in-progress

Esta entrega implementa a inferência de dependência entre Workloads, o núcleo da sprint: quando um backend aponta para o Service de outro objeto (ex: um banco de dados) através de uma variável de ambiente, a Topologia passa a desenhar essa relação — sem exigir que o usuário abra o manifest para descobrir isso sozinho. Decisões de escopo fechadas na sessão de `/grill-with-docs` (ver ADR 0018 e a entrada `ServiceDependency` do `CONTEXT.md`, que documentam o raciocínio completo por trás de cada corte abaixo).

`TopologyEdge` ganha um campo `type`, distinguindo as arestas estruturais já existentes (ownership, selector, volume mount, backend de Ingress — todas passam a ser criadas com `type: STRUCTURAL`) do novo tipo `SERVICE_DEPENDENCY`. Uma aresta `SERVICE_DEPENDENCY` liga o Workload consumidor ao Service alvo (direção invertida da aresta estrutural Service→Pod).

A detecção varre `spec.containers` e `spec.initContainers` de cada grupo de Pods (e Pods órfãos) já coletado por `buildGraph`, olhando: (a) `env[].value` com valor hardcoded; (b) `env[].valueFrom.configMapKeyRef`/`secretKeyRef`, resolvendo a chave referenciada; (c) `envFrom.configMapRef`/`secretRef`, resolvendo todas as chaves do ConfigMap/Secret referenciado. Isso exige buscar ConfigMaps e Secrets do namespace ativo — hoje não lidos pelo `TopologyService` — mas eles não viram nós no grafo: servem só para resolver o valor final da env var, permanecendo invisíveis (decisão registrada na ADR 0018, para não expandir o escopo com um novo tipo de nó).

O valor resolvido de cada env var é comparado contra os nomes dos Services já carregados no namespace ativo por substring com word-boundary (delimitado por caracteres não-alfanuméricos), cobrindo tanto hostname puro quanto connection strings completas (`jdbc:postgresql://postgres-service:5432/db`). Quando o valor tem o formato `<service>.<namespace>.svc.cluster.local`, o segmento de namespace extraído precisa bater com o namespace ativo — caso contrário, nenhuma aresta é criada (evita atribuir erroneamente a um Service local de mesmo nome quando a intenção era um Service de outro namespace; ver nota de direção futura na entrada `ServiceDependency` do `CONTEXT.md`).

Múltiplas env vars do mesmo Workload que casam com o mesmo Service colapsam em uma única aresta `SERVICE_DEPENDENCY` — sem duplicar. A env var e o valor que geraram cada match precisam ficar acessíveis para a entrega seguinte (evidência no drawer do nó de origem) — avaliar se isso vai como campos adicionais no próprio `TopologyEdge` ou uma estrutura auxiliar retornada junto do `TopologyGraph`.

Fora de escopo: parse de conteúdo de arquivo montado via volume (ConfigMap/Secret projetado como arquivo de config, ex: `application.yaml`) — decisão já registrada na ADR 0018. ConfigMap/Secret como nós de primeira classe — mesma ADR.

Cobertura de teste: avaliar no momento da cobertura da sprint um teste de integração cobrindo os três formatos de valor (hostname puro, host:porta, connection string completa), o caso de dedup (duas env vars, mesmo Service, uma aresta só), e o caso negativo de namespace diferente no FQDN (sem aresta).
