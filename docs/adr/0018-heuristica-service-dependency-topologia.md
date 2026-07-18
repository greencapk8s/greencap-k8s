# Heurística de inferência para ServiceDependency na Topologia

A Topologia passa a inferir arestas `ServiceDependency` (Workload→Service) a partir de env vars e ConfigMap/Secret referenciados no PodSpec, usando matching por substring/word-boundary contra os nomes de Service do namespace ativo — em vez de match exato (que perderia quase todos os casos reais, já que env vars costumam ser connection strings completas, não o nome puro do Service) ou parse do conteúdo de arquivos montados via ConfigMap/Secret (multi-formato, mais frágil, escopo maior). ConfigMap/Secret permanecem invisíveis no grafo — usados só para resolver o valor da env var, não viram nós — evitando introduzir dois novos tipos de nó/aresta estrutural numa sprint focada em dependências entre serviços.

## Considered Options

- **Match exato** do valor da env var contra o nome do Service — descartado por não cobrir connection strings reais (`jdbc:postgresql://postgres-service:5432/db`).
- **Parse de conteúdo de arquivos de config montados** (YAML/properties/.env) — descartado nesta sprint por exigir parser multi-formato e ter maior risco de falso positivo/negativo; candidato a sprint futura.
- **ConfigMap/Secret como nós de primeira classe** no grafo — descartado por ampliar o escopo (novo tipo de nó + nova aresta estrutural) sem servir diretamente ao objetivo da sprint.

## Consequences

- A heurística pode gerar falsos positivos com nomes de Service curtos ou genéricos (`db`, `api`, `cache`) — mitigado mostrando a env var e o valor que geraram o match no drawer do nó de origem, e por uma diferenciação visual (aresta tracejada) que sinaliza "isso é inferido, não garantido".
- Trocar o algoritmo de matching no futuro muda quais arestas aparecem no grafo sem aviso — usuários já acostumados ao grafo atual veem uma mudança de comportamento silenciosa.
