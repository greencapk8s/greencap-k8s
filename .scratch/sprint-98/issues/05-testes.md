# 05 — Cobertura de testes: Karibu e integração

Status: done

Depende das issues 02, 03 e 04. Implementada após o aceite manual das entregas, conforme o fluxo de sprint do projeto.

Karibu (`KaribuTest`): guard de seleção — a ação de deploy fica desabilitada/oculta quando o Template já está "Installed"; o diálogo de preview abre em modo somente-leitura (sem campo editável) e só dispara o deploy após confirmação explícita.

Integração (`PostgresIntegrationTest` ou equivalente para o novo service): `SampleCatalogService` interpretando um `catalog.json`/`template.yaml` de fixture (incluindo YAML multi-documento e a substituição do valor-sentinela `__BUILD__<name>`), e o método de checagem de "Installed" contra uma Namespace existente/inexistente. Comportamento de abort-sem-rollback em caso de conflito coberto com um recurso pré-existente na fixture.
