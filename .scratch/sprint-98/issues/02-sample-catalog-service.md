# 02 — SampleCatalogService: parser do índice e do manifest de Template

Status: done

Depende da issue 01 para ter conteúdo real para validar contra, mas pode ser implementada e testada com fixtures locais em paralelo.

Novo service em `kubernetes/` (ou pacote próprio, a definir na implementação) responsável por buscar e interpretar o catálogo de Templates a partir do `greencap-templates`. Busca `catalog.json` via HTTP simples (raw content, mesmo padrão de URL já usado em `DockerfileParser`/`ComposeParser` para GitHub), sem nenhum cliente Git e sem camada de cache — refeito a cada chamada. Interpreta cada entrada em um DTO com id, title, description, technologies, path e namespace.

Para um Template específico, busca também o `template.yaml` (lista de `resources` e, opcionalmente, `builds`) e os arquivos de recurso referenciados, concatenando-os para exibir no preview de deploy (issue 03/04). Deve suportar YAML multi-documento (arquivos com múltiplos recursos separados por `---`).

Inclui um método para checar se um Template já está "Installed" no Cluster ativo — um `GET` no Namespace cujo nome vem do campo `namespace` do `catalog.json`; existência da Namespace determina o badge (é uma condição por Cluster, não por usuário).

URL do repositório (`https://raw.githubusercontent.com/greencapk8s/greencap-templates/main`) fixa no código, sem variável de ambiente de override.
